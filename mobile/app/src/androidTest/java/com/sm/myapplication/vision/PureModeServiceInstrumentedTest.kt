package com.sm.myapplication.vision

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.media.ImageReader
import android.graphics.ImageFormat
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.ui.screens.puremode.PureModeSessionGate
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real CameraX + foreground LifecycleService, isolated from the installed user app. */
class PureModeServiceInstrumentedTest {
    @Test fun immediateReentryGetsFreshPreviewAndIgnoresOldCleanup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".diagnostic"))
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} ${Manifest.permission.CAMERA}")
            .use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        PureModeUserSettings.saveGestureRecognitionEnabled(context, false)
        val intent = Intent(context, PureModeCameraService::class.java).setAction(PureModeCameraService.ACTION_START)
        var previousGate: PureModeSessionGate? = null
        var previousService: PureModeCameraService? = null
        var previousAnalyzer = 0
        var activeGate: PureModeSessionGate? = null
        try {
            repeat(3) { cycle ->
                val gate = PureModeSessionGate()
                activeGate = gate
                val connected = CountDownLatch(1)
                var running: PureModeCameraService? = null
                var bound = false
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        gate.runIfOpen {
                            running = (binder as PureModeCameraService.LocalBinder).getService()
                            connected.countDown()
                        }
                    }
                    override fun onServiceDisconnected(name: ComponentName?) {
                        gate.runIfOpen { running = null }
                    }
                }
                instrumentation.runOnMainSync {
                    gate.setReleaseAction {
                        running?.setPreviewSurfaceProvider(null)
                        running?.stopSession()
                        if (bound) context.unbindService(connection)
                        context.stopService(intent)
                    }
                    TimerController.start(StudyMode.PURE, false)
                    ContextCompat.startForegroundService(context, intent)
                    bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                }
                assertTrue(bound)
                assertTrue("Cycle $cycle must reconnect", connected.await(20L, TimeUnit.SECONDS))
                val service = checkNotNull(running)
                val previewRequests = AtomicInteger()
                val previewFrames = AtomicInteger()
                val display = Preview.SurfaceProvider { request ->
                    previewRequests.incrementAndGet()
                    val size = request.resolution
                    val sink = ImageReader.newInstance(size.width, size.height, ImageFormat.PRIVATE, 2)
                    sink.setOnImageAvailableListener({ reader ->
                        reader.acquireLatestImage()?.let { it.close(); previewFrames.incrementAndGet() }
                    }, Handler(Looper.getMainLooper()))
                    request.provideSurface(sink.surface, ContextCompat.getMainExecutor(context)) { sink.close() }
                }
                var framesBefore = 0L
                instrumentation.runOnMainSync {
                    framesBefore = service.detectionFrames()
                    service.setPreviewSurfaceProvider(display)
                    // Mimic old NavHost disposal, released PreviewView, and a
                    // delayed old callback AFTER the new camera has attached.
                    previousGate?.let { old ->
                        assertFalse(old.close())
                        assertFalse(old.runIfOpen {
                            previousService?.setPreviewSurfaceProvider(null)
                            previousService?.stopSession()
                            context.stopService(intent)
                            TimerController.onCameraOff()
                        })
                    }
                }
                var analyzer = 0
                var detections = framesBefore
                val deadline = SystemClock.elapsedRealtime() + 45_000L
                do {
                    instrumentation.runOnMainSync {
                        analyzer = service.analyzerSessionId()
                        detections = service.detectionFrames()
                    }
                    if (previewRequests.get() > 0 && previewFrames.get() > 0 && detections > framesBefore) break
                    Thread.sleep(150L)
                } while (SystemClock.elapsedRealtime() < deadline)
                assertTrue("Cycle $cycle requires a NEW Preview SurfaceRequest", previewRequests.get() > 0)
                assertTrue("Cycle $cycle preview must actually deliver frames", previewFrames.get() > 0)
                assertTrue("Cycle $cycle analysis must continue", detections > framesBefore)
                assertTrue(analyzer != 0)
                if (previousAnalyzer != 0) assertNotEquals("Each ended session needs a fresh analyzer", previousAnalyzer, analyzer)
                previousGate = gate; previousService = service; previousAnalyzer = analyzer
                // Synchronous exit before navigation, then next iteration starts
                // immediately without waiting for the old transition/dispose.
                instrumentation.runOnMainSync { assertTrue(gate.close()) }
            }
        } finally {
            instrumentation.runOnMainSync {
                activeGate?.close()
                TimerController.start(StudyMode.BASIC, false)
            }
        }
    }

    @Test fun displayToggleKeepsOneAnalyzerAndLowLightUpdatesWhileHidden() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".diagnostic")) { "Never run against the user's package" }
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} ${Manifest.permission.CAMERA}")
            .use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        PureModeUserSettings.saveGestureRecognitionEnabled(context, false)
        val latch = CountDownLatch(1)
        var service: PureModeCameraService? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as PureModeCameraService.LocalBinder).getService()
                latch.countDown()
            }
            override fun onServiceDisconnected(name: ComponentName?) { service = null }
        }
        var bound = false
        val intent = Intent(context, PureModeCameraService::class.java).setAction(PureModeCameraService.ACTION_START)
        try {
            instrumentation.runOnMainSync {
                TimerController.start(StudyMode.PURE, false)
                ContextCompat.startForegroundService(context, intent)
                bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
            assertTrue(bound)
            assertTrue(latch.await(20L, TimeUnit.SECONDS))
            val running = checkNotNull(service)
            fun snapshot(): Pair<Int, Long> {
                var snapshot = 0 to 0L
                instrumentation.runOnMainSync { snapshot = running.analyzerSessionId() to running.detectionFrames() }
                return snapshot
            }
            fun awaitFramesAfter(count: Long) {
                val deadline = SystemClock.elapsedRealtime() + 45_000L
                while (snapshot().second <= count && SystemClock.elapsedRealtime() < deadline) Thread.sleep(250L)
                assertTrue("Camera analysis must keep producing fresh results", snapshot().second > count)
            }
            awaitFramesAfter(0L)
            val initial = snapshot()
            assertTrue(initial.first != 0)
            val previewRequests = AtomicInteger()
            val previewFrames = AtomicInteger()
            fun awaitPreviewFramesAfter(count: Int) {
                val deadline = SystemClock.elapsedRealtime() + 45_000L
                while (previewFrames.get() <= count && SystemClock.elapsedRealtime() < deadline) Thread.sleep(150L)
                assertTrue("Preview must deliver frames, not just keep analysis alive", previewFrames.get() > count)
            }
            // A real, drained Preview surface. No webcam frame is read or saved.
            val display = Preview.SurfaceProvider { request ->
                previewRequests.incrementAndGet()
                val size = request.resolution
                val sink = ImageReader.newInstance(size.width, size.height, ImageFormat.PRIVATE, 2)
                sink.setOnImageAvailableListener({ reader ->
                    reader.acquireLatestImage()?.let { it.close(); previewFrames.incrementAndGet() }
                }, Handler(Looper.getMainLooper()))
                request.provideSurface(sink.surface, ContextCompat.getMainExecutor(context)) { sink.close() }
            }
            instrumentation.runOnMainSync { running.setPreviewSurfaceProvider(display) }
            awaitPreviewFramesAfter(0)
            awaitFramesAfter(initial.second)
            val visible = snapshot()
            assertEquals(initial.first, visible.first)
            instrumentation.runOnMainSync {
                running.setPreviewSurfaceProvider(null)
                running.setLowLightEnabled(true)
            }
            awaitFramesAfter(visible.second)
            val hidden = snapshot()
            assertEquals("Hiding must not recreate the analyzer", initial.first, hidden.first)
            var diagnostics = ""
            val lowLightDeadline = SystemClock.elapsedRealtime() + 45_000L
            do {
                instrumentation.runOnMainSync { diagnostics = running.analysisDiagnostics() }
                if (!diagnostics.contains("software=off")) break
                Thread.sleep(250L)
            } while (SystemClock.elapsedRealtime() < lowLightDeadline)
            assertFalse("Low-light must reach the hidden analyzer", diagnostics.contains("software=off"))
            val framesBeforeRestoring = previewFrames.get()
            instrumentation.runOnMainSync { running.setLowLightEnabled(false); running.setPreviewSurfaceProvider(display) }
            awaitPreviewFramesAfter(framesBeforeRestoring)
            assertTrue("Showing again needs a fresh Preview SurfaceRequest", previewRequests.get() >= 2)
            awaitFramesAfter(hidden.second)
            assertEquals(initial.first, snapshot().first)
        } finally {
            instrumentation.runOnMainSync {
                service?.stopSession()
                if (bound) context.unbindService(connection)
                context.stopService(intent)
                TimerController.start(StudyMode.BASIC, false)
            }
        }
    }
}
