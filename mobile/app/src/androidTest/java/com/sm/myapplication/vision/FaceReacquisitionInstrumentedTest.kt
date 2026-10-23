package com.sm.myapplication.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.media.ImageWriter
import android.os.SystemClock
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.test.platform.app.InstrumentationRegistry
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.ui.screens.puremode.PureModeDetectionController
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Real FAST detection must not wait for optional work on an older empty scene. */
class FaceReacquisitionInstrumentedTest {
    @Test fun stationaryFaceReturnsBeforeOldRescueCompletes() = checkReturn(lowLight = false)
    @Test fun lowLightStationaryFaceReturnsBeforeOldRescueCompletes() = checkReturn(lowLight = true)

    private fun checkReturn(lowLight: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val decoded = instrumentation.context.assets.open("portrait.jpg").use(BitmapFactory::decodeStream)
        val resized = Bitmap.createScaledBitmap(decoded, 480, 640, true)
        val portrait = checkNotNull(resized.copy(Bitmap.Config.ARGB_8888, false))
        if (resized !== decoded) resized.recycle()
        decoded.recycle()
        val blank = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        val reader = ImageReader.newInstance(480, 640, ImageFormat.YUV_420_888, 2)
        val writer = ImageWriter.newInstance(reader.surface, 2)
        val image = writer.dequeueInputImage()
        val closeCount = AtomicLong()
        val imageInfo = proxy<ImageInfo> { name ->
            when (name) { "getRotationDegrees" -> 0; "getTimestamp" -> 1L; else -> null }
        }
        // The native image remains owned by this fixture. Each analyzed proxy
        // closes normally, without freeing the buffer reused for later frames.
        val frame = proxy<ImageProxy> { name ->
            when (name) {
                "getWidth" -> image.width
                "getHeight" -> image.height
                "getFormat" -> image.format
                "getImage" -> image
                "getImageInfo" -> imageInfo
                "getPlanes" -> image.planes.map { plane -> proxy<ImageProxy.PlaneProxy> { method ->
                    when (method) {
                        "getBuffer" -> plane.buffer.duplicate()
                        "getPixelStride" -> plane.pixelStride
                        "getRowStride" -> plane.rowStride
                        else -> null
                    }
                } }.toTypedArray()
                "close" -> { closeCount.incrementAndGet(); null }
                else -> null
            }
        }
        val settingsConstructor = PureModeUserSettings.Snapshot::class.java.getDeclaredConstructor(
            java.lang.Long.TYPE, java.lang.Boolean.TYPE, String::class.java,
        ).apply { isAccessible = true }
        val settings = settingsConstructor.newInstance(5_000L, false, PureModeUserSettings.MODE_ACCURACY)
        val clock = AtomicLong(SystemClock.elapsedRealtime())
        val detector = PureModeDetectionController { clock.get() }
        detector.setPureModeSettings(settings)
        val observations = CopyOnWriteArrayList<DetectionResult>()
        instrumentation.runOnMainSync { TimerController.start(StudyMode.PURE, false) }
        val analyzer = FrameAnalyzer(instrumentation.targetContext) { result ->
            instrumentation.runOnMainSync {
                clock.set(SystemClock.elapsedRealtime())
                detector.handle(result) // Keep the actual production capture timestamp.
                if (result.faceAnalyzed) observations.add(result)
            }
        }
        val rescueExecutor = field(analyzer, "faceRescueExecutor") as ExecutorService
        val rescueBusy = field(analyzer, "faceRescueBusy") as AtomicBoolean
        val blockerStarted = CountDownLatch(1)
        val releaseBlocker = CountDownLatch(1)
        try {
            analyzer.setPureModeSettings(settings)
            analyzer.setLowLightModeEnabled(lowLight)
            fillYuv(image, portrait)
            analyzer.analyze(frame)
            assertTrue("Warm the real FAST model on the stationary portrait", observations.last().faceDetected)
            assertTrue(TimerController.state.value.isRunning)

            // Delay the optional worker, not the FAST model, camera or timer.
            // This deterministically recreates an old empty-scene rescue still
            // pending when the unchanged portrait comes back into the camera.
            rescueExecutor.execute {
                blockerStarted.countDown()
                try { releaseBlocker.await(30L, TimeUnit.SECONDS) }
                catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            }
            assertTrue("Optional worker blocker must start", blockerStarted.await(10L, TimeUnit.SECONDS))
            Thread.sleep(350L)
            fillYuv(image, blank)
            analyzer.analyze(frame)
            assertTrue("Empty scene must enqueue real optional rescue", rescueBusy.get())
            val blankObservation = observations.last()
            assertFalse("Publish the initial FAST miss before rescue", blankObservation.faceDetected)
            instrumentation.runOnMainSync { TimerController.onFaceLost(5_000L) }
            assertEquals(TimerPauseCause.AUTO_FACE_LOST, TimerController.state.value.pauseCause)

            Thread.sleep(350L)
            fillYuv(image, portrait) // Exactly the original pixels: no movement required.
            val callbacksBeforeReturn = observations.size
            analyzer.analyze(frame)
            assertTrue("Return must be analyzed while old rescue remains blocked", rescueBusy.get())
            assertTrue("FAST must publish a current result before optional rescue completes",
                observations.size > callbacksBeforeReturn)
            val returnedObservation = observations.last()
            assertTrue("The unchanged portrait must be detected without movement", returnedObservation.faceDetected)
            assertTrue(returnedObservation.observationElapsedMs > blankObservation.observationElapsedMs)
            assertTrue("The first fresh face must resume an automatic pause immediately", TimerController.state.value.isRunning)
            val returnedAt = clock.get()

            releaseBlocker.countDown()
            rescueExecutor.submit {}.get(20L, TimeUnit.SECONDS)
            assertFalse("Old rescue must release its owned input", rescueBusy.get())
            assertTrue("The older empty-scene result must really arrive after recovery",
                observations.drop(callbacksBeforeReturn + 1).any {
                    !it.faceDetected && it.observationElapsedMs == blankObservation.observationElapsedMs
                })
            instrumentation.runOnMainSync {
                assertTrue("A delayed old negative must not re-pause the timer", TimerController.state.value.isRunning)
                assertNull("A delayed old negative must not start a new absence window", field(detector, "faceMissingStartedAt"))
                assertEquals("Retain the newer positive capture ordering",
                    returnedObservation.observationElapsedMs, field(detector, "lastFaceObservationAt"))
                clock.set(returnedAt + 4_999L)
                detector.checkTimeout()
                assertTrue("Recovery retains the complete existing watchdog grace", TimerController.state.value.isRunning)
                clock.set(returnedAt + 5_000L)
                detector.checkTimeout()
                assertFalse("A genuinely stalled stream must still stop at 5 seconds", TimerController.state.value.isRunning)
            }
            assertEquals("Every admitted camera frame must close", 3L, closeCount.get())
        } finally {
            releaseBlocker.countDown()
            analyzer.shutdown()
            // Shutdown enqueues model cleanup on the same owned worker.
            rescueExecutor.awaitTermination(20L, TimeUnit.SECONDS)
            instrumentation.runOnMainSync { TimerController.start(StudyMode.BASIC, false) }
            image.close()
            writer.close()
            reader.close()
            blank.recycle()
            portrait.recycle()
        }
    }

    private fun field(owner: Any, name: String): Any? =
        owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(owner)

    private inline fun <reified T> proxy(crossinline get: (String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            get(method.name)
        } as T

    private fun fillYuv(image: Image, bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        fun put(planeIndex: Int, x: Int, y: Int, value: Double) {
            val plane = image.planes[planeIndex]
            plane.buffer.put(y * plane.rowStride + x * plane.pixelStride,
                value.toInt().coerceIn(0, 255).toByte())
        }
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val p = pixels[y * bitmap.width + x]
            val r = p shr 16 and 255; val g = p shr 8 and 255; val b = p and 255
            put(0, x, y, 16 + .257 * r + .504 * g + .098 * b)
            if (x % 2 == 0 && y % 2 == 0) {
                put(1, x / 2, y / 2, 128 - .148 * r - .291 * g + .439 * b)
                put(2, x / 2, y / 2, 128 + .439 * r - .368 * g - .071 * b)
            }
        }
    }
}
