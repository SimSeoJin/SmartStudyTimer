package com.sm.myapplication.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.media.ImageReader
import android.media.ImageWriter
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.ui.screens.puremode.PureModeDetectionController
import com.sm.myapplication.ui.screens.timer.TimerController
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

/** Real bundled ML Kit + native YUV + FrameAnalyzer + timer; no webcam or user images. */
class FacePipelineInstrumentedTest {
    @Test fun normalFaceStartsTimer() = checkFacePipeline(lowLight = false, rotation = 0)
    @Test fun rotatedFaceStartsTimer() = checkFacePipeline(lowLight = false, rotation = 90)
    @Test fun lowLightFaceStartsTimer() = checkFacePipeline(lowLight = true, rotation = 0)
    @Test fun gestureEnabledDoesNotBlockFirstFace() = checkFacePipeline(false, 0, gestures = true)
    @Test fun emptySceneDoesNotStartTimer() = checkFacePipeline(false, 0, blank = true)
    @Test fun lowLightEmptySceneDoesNotStartTimer() = checkFacePipeline(true, 0, blank = true)
    @Test fun lowLightToggleDoesNotLoseAlreadyDetectedFace() = checkFacePipeline(false, 0, toggleAfterFirstFace = true)
    @Test fun halfFaceRescueRepeatsAndBlankStillStopsTimer() = checkFacePipeline(false, 0, gestures = true, partialAfterFirstFace = true)

    private fun checkFacePipeline(lowLight: Boolean, rotation: Int, gestures: Boolean = false, blank: Boolean = false, toggleAfterFirstFace: Boolean = false, partialAfterFirstFace: Boolean = false) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        var portrait = instrumentation.context.assets.open("portrait.jpg").use {
            BitmapFactory.decodeStream(it)
        }
        val resized = Bitmap.createScaledBitmap(portrait, 480, 640, true)
        val scaled = checkNotNull(resized.copy(Bitmap.Config.ARGB_8888, true))
        if (resized !== portrait) resized.recycle()
        portrait.recycle()
        portrait = scaled
        if (blank) portrait.eraseColor(0xff000000.toInt())
        if (lowLight) {
            val pixels = IntArray(portrait.width * portrait.height)
            portrait.getPixels(pixels, 0, portrait.width, 0, 0, portrait.width, portrait.height)
            for (i in pixels.indices) {
                val p = pixels[i]
                pixels[i] = (0xff shl 24) or (((p shr 16 and 255) * 0.4).toInt() shl 16) or
                    (((p shr 8 and 255) * 0.4).toInt() shl 8) or ((p and 255) * 0.4).toInt()
            }
            portrait.setPixels(pixels, 0, portrait.width, 0, 0, portrait.width, portrait.height)
        }
        if (rotation != 0) {
            val transformed = Bitmap.createBitmap(portrait, 0, 0, portrait.width, portrait.height,
                Matrix().apply { postRotate(-rotation.toFloat()) }, false)
            portrait.recycle()
            portrait = transformed
        }
        val reader = ImageReader.newInstance(portrait.width, portrait.height, ImageFormat.YUV_420_888, 2)
        val writer = ImageWriter.newInstance(reader.surface, 2)
        val image = writer.dequeueInputImage()
        fillYuv(image, portrait)
        var closed = false
        val imageInfo = proxy<ImageInfo> { name ->
            when (name) { "getRotationDegrees" -> rotation; "getTimestamp" -> 1L; else -> null }
        }
        val imageProxy = proxy<ImageProxy> { name ->
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
                "close" -> { closed = true; null }
                else -> null
            }
        }
        // Settings loading uses isolated diagnostic prefs, not the user's preferences.
        val settings = PureModeUserSettings.saveGestureRecognitionEnabled(context, gestures)
        // Deterministic timer clock: cold model loading is tested independently
        // from observation-age guards (covered by JVM tests).
        var testNow = 0L
        val detector = PureModeDetectionController { testNow }
        detector.setPureModeSettings(settings)
        var faceSeen = false
        var startedBeforeGestureInitialization = false
        var lastFaceSeen = false
        val faceCallbacks = AtomicInteger()
        instrumentation.runOnMainSync { TimerController.start(StudyMode.PURE, false) }
        val analyzer = FrameAnalyzer(context) { result ->
            instrumentation.runOnMainSync {
                if (result.faceAnalyzed) lastFaceSeen = result.faceDetected
                detector.handle(DetectionResult(result.faceDetected, result.startGestureDetected,
                    result.pauseGestureDetected, result.faces, result.imageWidth, result.imageHeight,
                    result.frontCamera, result.lowLightApplied, result.faceAnalyzed, -1L))
                if (result.faceDetected) {
                    faceSeen = true
                    startedBeforeGestureInitialization = TimerController.state.value.isRunning
                }
                if (result.faceAnalyzed) faceCallbacks.incrementAndGet()
            }
        }
        try {
            analyzer.setPureModeSettings(settings)
            analyzer.setLowLightModeEnabled(lowLight)
            analyzer.analyze(imageProxy)
            assertEquals("Actual model result for fixture", !blank, faceSeen)
            assertEquals("Only a detected face may start the timer", !blank, startedBeforeGestureInitialization)
            assertTrue("ImageProxy must always be closed", closed)
            if (partialAfterFirstFace) {
                val warmDeadline = SystemClock.elapsedRealtime() + 15_000L
                while (!analyzer.lowLightDiagnostics.contains("lowLightReady=true") && SystemClock.elapsedRealtime() < warmDeadline) {
                    Thread.sleep(100L)
                }
                assertTrue("Optional accurate model must warm independently", analyzer.lowLightDiagnostics.contains("lowLightReady=true"))
                val boxDetector = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
                val box = try {
                    Tasks.await(boxDetector.process(InputImage.fromBitmap(portrait, 0))).first().boundingBox
                } finally { boxDetector.close() }
                val halfFace = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
                val empty = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
                halfFace.eraseColor(Color.GRAY)
                empty.eraseColor(Color.BLACK)
                Canvas(halfFace).drawBitmap(portrait, -(box.right - box.width() * .50f), 0f, null)
                try {
                    fillYuv(image, halfFace)
                    var detections = 0
                    fun handFrames() = Regex("gestureFrames=(\\d+)").find(analyzer.lowLightDiagnostics)!!.groupValues[1].toLong()
                    val initialHandFrames = handFrames()
                    repeat(4) {
                        Thread.sleep(1_100L)
                        testNow += 2_000L
                        val previousCallbacks = faceCallbacks.get()
                        analyzer.analyze(imageProxy)
                        // Feed additional camera frames while accurate face work
                        // is pending: the hand worker must continue receiving input.
                        repeat(2) {
                            Thread.sleep(180L)
                            if (analyzer.lowLightDiagnostics.contains("faceRescueBusy=true")) analyzer.analyze(imageProxy)
                        }
                        val deadline = SystemClock.elapsedRealtime() + 15_000L
                        while (analyzer.lowLightDiagnostics.contains("faceRescueBusy=true") && SystemClock.elapsedRealtime() < deadline) Thread.sleep(50L)
                        assertFalse("Rescue input must be released after completion", analyzer.lowLightDiagnostics.contains("faceRescueBusy=true"))
                        assertTrue("A pending rescue must eventually publish its face result", faceCallbacks.get() > previousCallbacks)
                        if (lastFaceSeen) detections++
                        Log.i("FaceVisibilityProbe", "pipeline-half-$it detected=$lastFaceSeen ${analyzer.lowLightDiagnostics}")
                        assertTrue("Half-face presence must keep the timer running across repeated frames", TimerController.state.value.isRunning)
                    }
                    assertTrue("A slow first rescue must not disable later half-face rescue", detections >= 2)
                    assertTrue("Hand inference must continue while optional face work is pending", handFrames() > initialHandFrames + 1)
                    fillYuv(image, empty)
                    Thread.sleep(600L)
                    testNow += 1_000L
                    val previousCallbacks = faceCallbacks.get()
                    analyzer.analyze(imageProxy)
                    val deadline = SystemClock.elapsedRealtime() + 15_000L
                    while (analyzer.lowLightDiagnostics.contains("faceRescueBusy=true") && SystemClock.elapsedRealtime() < deadline) Thread.sleep(50L)
                    assertFalse("Blank rescue must complete", analyzer.lowLightDiagnostics.contains("faceRescueBusy=true"))
                    assertTrue("Blank analysis must publish a negative result", faceCallbacks.get() > previousCallbacks)
                    assertFalse("No cached half-face may count as a current face", lastFaceSeen)
                    instrumentation.runOnMainSync { testNow += 5_000L; detector.checkTimeout() }
                    assertFalse("Blank scene must still stop after 5 seconds", TimerController.state.value.isRunning)
                } finally { halfFace.recycle(); empty.recycle() }
            }
            if (lowLight) {
                val diagnostics = analyzer.lowLightDiagnostics
                assertTrue("Low-light must preserve original detection or use an enhanced rescue",
                    // A cold/slow original negative may exhaust the optional
                    // rescue budget before any enhancement classification runs.
                    if (blank) diagnostics.contains("insufficient-detail") || diagnostics.contains("rescuePath=budget-skip") else
                        diagnostics.contains("software=original-face") || diagnostics.contains("software=enhanced"))
            }
            if (toggleAfterFirstFace) {
                analyzer.setLowLightModeEnabled(true)
                testNow = 1_000L
                Thread.sleep(600L)
                analyzer.analyze(imageProxy)
                assertTrue("Low-light ON must keep the same previously detected face", lastFaceSeen)
                assertTrue("A good original result must not be replaced", analyzer.lowLightDiagnostics.contains("software=original-face"))
                analyzer.setLowLightModeEnabled(false)
                testNow = 2_000L
                Thread.sleep(600L)
                analyzer.analyze(imageProxy)
                assertTrue("Low-light OFF must keep normal detection", lastFaceSeen)
            }
            if (!partialAfterFirstFace && (gestures || (lowLight && !blank))) {
                // A slow optional hand-model initialization must not prevent a
                // subsequent real negative face frame, loss timeout, or recovery.
                val blankBitmap = Bitmap.createBitmap(portrait.width, portrait.height, Bitmap.Config.ARGB_8888)
                blankBitmap.eraseColor(0xff000000.toInt())
                try {
                    testNow = 1_000L
                    fillYuv(image, blankBitmap)
                    Thread.sleep(600L)
                    analyzer.analyze(imageProxy)
                    instrumentation.runOnMainSync { testNow = 6_000L; detector.checkTimeout() }
                    assertFalse("Face loss must pause while hand initialization is in flight", TimerController.state.value.isRunning)
                    fillYuv(image, portrait)
                    Thread.sleep(600L)
                    analyzer.analyze(imageProxy)
                    assertTrue("A fresh face must resume after automatic loss", TimerController.state.value.isRunning)
                } finally { blankBitmap.recycle() }
            }
        } finally {
            analyzer.shutdown()
            instrumentation.runOnMainSync { TimerController.start(StudyMode.BASIC, false) }
            image.close()
            writer.close()
            reader.close()
            portrait.recycle()
        }
    }

    private inline fun <reified T> proxy(crossinline get: (String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            get(method.name)
        } as T

    private fun fillYuv(image: Image, bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        fun put(planeIndex: Int, x: Int, y: Int, value: Double) {
            val plane = image.planes[planeIndex]
            plane.buffer.put(y * plane.rowStride + x * plane.pixelStride,
                value.toInt().coerceIn(0, 255).toByte())
        }
        for (y in 0 until height) for (x in 0 until width) {
            val p = pixels[y * width + x]
            val r = p shr 16 and 255; val g = p shr 8 and 255; val b = p and 255
            put(0, x, y, 16 + 0.257 * r + 0.504 * g + 0.098 * b)
            if (x % 2 == 0 && y % 2 == 0) {
                put(1, x / 2, y / 2, 128 - 0.148 * r - 0.291 * g + 0.439 * b)
                put(2, x / 2, y / 2, 128 + 0.439 * r - 0.368 * g - 0.071 * b)
            }
        }
    }
}
