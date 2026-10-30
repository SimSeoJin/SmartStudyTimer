package com.sm.myapplication.vision

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.sm.myapplication.MainActivity
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.ui.screens.timer.TimerController
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Tests an actual attached PreviewView, not only an ImageReader SurfaceProvider. */
class PreviewPerformanceInstrumentedTest {
    @Test fun performancePreviewStreamsAgainAfterHiddenViewIsReplaced() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".diagnostic")) { "Never run against the user's app" }
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${context.packageName} ${Manifest.permission.CAMERA}",
        ).use { ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        // This changes only the isolated diagnostic package's preferences.
        PureModeUserSettings.saveGestureRecognitionEnabled(context, false)
        val intent = Intent(context, PureModeCameraService::class.java)
            .setAction(PureModeCameraService.ACTION_START)
        val connected = CountDownLatch(1)
        var service: PureModeCameraService? = null
        var activity: MainActivity? = null
        var host: FrameLayout? = null
        var bound = false
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as PureModeCameraService.LocalBinder).getService()
                connected.countDown()
            }
            override fun onServiceDisconnected(name: ComponentName?) { service = null }
        }

        fun awaitMainCondition(label: String, condition: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 45_000L
            var met: Boolean
            do {
                met = false
                instrumentation.runOnMainSync { met = condition() }
                if (met) return
                Thread.sleep(150L)
            } while (SystemClock.elapsedRealtime() < deadline)
            var diagnostics = ""
            instrumentation.runOnMainSync {
                diagnostics = "detections=${service?.detectionFrames()} ${service?.analysisDiagnostics()}"
            }
            Log.e("PreviewPerformanceTest", "$label: $diagnostics")
            fail("$label: $diagnostics")
        }

        fun descendants(view: View): List<View> = if (view is ViewGroup) {
            listOf(view) + (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
        } else listOf(view)

        fun attachFreshPreview(running: PureModeCameraService): PreviewView {
            var display: PreviewView? = null
            instrumentation.runOnMainSync {
                val owner = checkNotNull(activity)
                display = PreviewView(owner).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                checkNotNull(host).addView(
                    display, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                running.setPreviewSurfaceProvider(checkNotNull(display).surfaceProvider)
            }
            return checkNotNull(display)
        }

        try {
            // MainActivity belongs to .diagnostic; no test-only production Activity is required.
            activity = instrumentation.startActivitySync(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            ) as MainActivity
            instrumentation.runOnMainSync {
                val owner = checkNotNull(activity)
                owner.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                host = FrameLayout(owner).apply {
                    // Exercise a real rounded display container without reading/saving frames.
                    background = GradientDrawable().apply {
                        setColor(Color.BLACK)
                        cornerRadius = 22f * resources.displayMetrics.density
                    }
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                    clipToOutline = true
                }
                owner.setContentView(checkNotNull(host))
            }
            awaitMainCondition("Diagnostic Activity must be visible before starting camera") {
                checkNotNull(activity).hasWindowFocus() && checkNotNull(host).isAttachedToWindow
            }
            instrumentation.runOnMainSync {
                TimerController.start(StudyMode.PURE, false)
                ContextCompat.startForegroundService(context, intent)
                bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
            assertTrue(bound)
            assertTrue("Camera service must connect", connected.await(20L, TimeUnit.SECONDS))
            val running = checkNotNull(service)
            awaitMainCondition("Hidden analysis must begin") { running.detectionFrames() > 0L }
            var analyzer = 0
            var previousFrames = 0L
            instrumentation.runOnMainSync {
                analyzer = running.analyzerSessionId()
                previousFrames = running.detectionFrames()
            }
            assertTrue(analyzer != 0)

            repeat(2) { cycle ->
                val display = attachFreshPreview(running)
                awaitMainCondition("PERFORMANCE preview $cycle must reach STREAMING") {
                    display.previewStreamState.value == PreviewView.StreamState.STREAMING
                }
                awaitMainCondition("Analysis must continue with visible preview $cycle") {
                    running.detectionFrames() > previousFrames
                }
                instrumentation.runOnMainSync {
                    assertEquals(PreviewView.ImplementationMode.PERFORMANCE, display.implementationMode)
                    assertEquals("Display changes must retain the analysis model", analyzer, running.analyzerSessionId())
                    val renderers = descendants(display).filter { it is SurfaceView || it is TextureView }
                    assertTrue("Preview needs a real SurfaceView or CameraX fallback TextureView", renderers.isNotEmpty())
                    // PERFORMANCE may legitimately fall back on legacy/incompatible devices.
                    Log.i("PreviewPerformanceTest", "cycle=$cycle renderer=${renderers.map { it.javaClass.simpleName }} ${running.analysisDiagnostics()}")
                    previousFrames = running.detectionFrames()
                    running.setPreviewSurfaceProvider(null)
                    checkNotNull(host).removeView(display)
                }
                awaitMainCondition("Analysis must continue after removing preview $cycle") {
                    running.detectionFrames() > previousFrames
                }
                instrumentation.runOnMainSync {
                    assertEquals("Hiding must not recreate the analysis model", analyzer, running.analyzerSessionId())
                    Log.i("PreviewPerformanceTest", "cycle=$cycle hidden ${running.analysisDiagnostics()}")
                    previousFrames = running.detectionFrames()
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                service?.setPreviewSurfaceProvider(null)
                service?.stopSession()
                if (bound) context.unbindService(connection)
                context.stopService(intent)
                TimerController.start(StudyMode.BASIC, false)
                host?.removeAllViews()
                activity?.finish()
            }
        }
    }
}
