package com.sm.myapplication.vision

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import android.os.Binder
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.camera.core.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.sm.myapplication.R
import com.sm.myapplication.ui.screens.puremode.PureModeDetectionController
import com.sm.myapplication.ui.screens.timer.TimerController
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * 화면을 가리거나 앱이 백그라운드에 있어도 순공모드 카메라 분석을 유지한다.
 * Android 정책상 카메라 사용 중에는 포그라운드 서비스 알림이 반드시 유지된다.
 */
class PureModeCameraService : LifecycleService() {
    private var cameraController: CameraDetectionView? = null
    private var previewSurface: Preview.SurfaceProvider? = null
    private val detectionController = PureModeDetectionController()
    private var lowLightModeEnabled = false
    private var detectionCount = 0L
    private var lastDetectionAt = 0L
    private var lastFaceDetected = false
    private var cameraStatus = "starting"
    private lateinit var pureModeSettings: PureModeUserSettings.Snapshot
    private val binder = LocalBinder()
    private val status = MutableStateFlow("카메라 시작 중")
    val cameraStatusFlow = status.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private val watchdog = object : Runnable {
        override fun run() {
            detectionController.checkTimeout()
            handler.postDelayed(this, 250L)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PureModeCameraService = this@PureModeCameraService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun setPreviewSurfaceProvider(provider: Preview.SurfaceProvider?) {
        previewSurface = provider
        cameraController?.setPreviewSurfaceProvider(provider)
    }

    fun setLowLightEnabled(enabled: Boolean) {
        lowLightModeEnabled = enabled
        cameraController?.setLowLightModeEnabled(enabled)
        updateNotification()
    }

    fun updateSettings(settings: PureModeUserSettings.Snapshot) = applyPureModeSettings(settings)

    fun stopSession() = stopCamera()

    fun analyzerSessionId(): Int = cameraController?.analyzerSessionId ?: 0
    fun detectionFrames(): Long = detectionCount
    fun analysisDiagnostics(): String = cameraController?.lowLightDiagnostics ?: "camera=stopped"

    override fun onCreate() {
        super.onCreate()
        applyPureModeSettings(PureModeUserSettings.load(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // LifecycleService가 ON_START를 전달해야 CameraX가 프레임을 공급한다.
        // 이 호출 없이 bindToLifecycle만 하면 서비스는 CREATED에 머문다.
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopCamera()
                stopSelf()
                return Service.START_NOT_STICKY
            }

            ACTION_UPDATE_LOW_LIGHT -> {
                lowLightModeEnabled = intent.getBooleanExtra(EXTRA_LOW_LIGHT_ENABLED, false)
                cameraController?.setLowLightModeEnabled(lowLightModeEnabled)
                updateNotification()
            }

            ACTION_UPDATE_SETTINGS -> {
                applyPureModeSettings(PureModeUserSettings.load(this))
            }

            ACTION_START, null -> {
                lowLightModeEnabled = intent?.getBooleanExtra(EXTRA_LOW_LIGHT_ENABLED, false) ?: false
                applyPureModeSettings(PureModeUserSettings.load(this))
                showForegroundNotification()
                startCamera()
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        stopCamera()
        TimerController.onCameraOff()
        super.onDestroy()
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            TimerController.onCameraOff()
            stopSelf()
            return
        }

        cameraController?.let {
            it.setLowLightModeEnabled(lowLightModeEnabled)
            it.setPureModeSettings(pureModeSettings)
            return
        }

        // 숨김 모드에서는 Preview UseCase를 만들지 않고 ImageAnalysis만 유지한다.
        cameraController = CameraDetectionView(applicationContext, false).also { controller ->
            controller.setPreviewSurfaceProvider(previewSurface)
            controller.setLowLightModeEnabled(lowLightModeEnabled)
            controller.setPureModeSettings(pureModeSettings)
            controller.start(
                this,
                object : CameraDetectionView.Listener {
                    override fun onDetection(result: DetectionResult) {
                        detectionCount++
                        if (result.faceAnalyzed) {
                            lastDetectionAt = SystemClock.elapsedRealtime()
                            lastFaceDetected = result.faceDetected
                            if (CameraStatusPolicy.isFreshAnalysisHeartbeat(result, lastDetectionAt, pureModeSettings.faceAwayTimeoutMs)) {
                                // Actual fresh frames supersede stale display/bind warnings.
                                cameraStatus = "분석 중"
                                status.value = ""
                                TimerController.onCameraOn()
                            }
                        }
                        detectionController.handle(result)
                    }

                    override fun onStatus(message: String) {
                        cameraStatus = message
                        status.value = CameraStatusPolicy.forDisplay(message)
                    }

                    override fun onCameraConnectionChanged(connected: Boolean) {
                        if (connected) TimerController.onCameraOn() else TimerController.onCameraOff()
                    }
                },
            )
        }
        handler.removeCallbacks(watchdog)
        handler.post(watchdog)
    }

    private fun stopCamera() {
        handler.removeCallbacks(watchdog)
        cameraController?.release()
        cameraController = null
        detectionController.reset()
        // Only exiting Pure Mode stops analysis. Display toggles never reach here.
    }

    private fun applyPureModeSettings(settings: PureModeUserSettings.Snapshot) {
        pureModeSettings = settings
        TimerController.onGestureRecognitionChanged(settings.gestureRecognitionEnabled)
        detectionController.setPureModeSettings(settings)
        cameraController?.setPureModeSettings(settings)
    }

    // 화면에 디버그 정보를 표시하거나 영상을 저장하지 않고 adb로 동작을 검증한다.
    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<out String>?) {
        super.dump(fd, writer, args)
        writer.println("lifecycle=${lifecycle.currentState} camera=$cameraStatus")
        writer.println("analyzerSession=${analyzerSessionId()} previewAttached=${previewSurface != null}")
        writer.println("detections=$detectionCount lastDetectionAgeMs=${if (lastDetectionAt == 0L) -1 else SystemClock.elapsedRealtime() - lastDetectionAt}")
        writer.println("faceDetected=$lastFaceDetected lowLight=$lowLightModeEnabled")
        writer.println(
            "pureSettings faceAwayMs=${pureModeSettings.faceAwayTimeoutMs} gesture=${pureModeSettings.gestureRecognitionEnabled} mode=${pureModeSettings.recognitionMode} faceInterval=${pureModeSettings.faceAnalyzeIntervalMs} gestureInterval=${pureModeSettings.gestureAnalyzeIntervalMs}",
        )
        writer.println(cameraController?.lowLightDiagnostics ?: "camera=stopped")
        val timer = TimerController.state.value
        writer.println("timerRunning=${timer.isRunning} elapsedMs=${timer.elapsedMs} pauseCause=${timer.pauseCause}")
    }

    private fun showForegroundNotification() {
        createNotificationChannel()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_pure_mode_notification)
        .setContentTitle("순공모드 실행 중")
        .setContentText(if (lowLightModeEnabled) "학습 타이머 · 저조도 ON" else "학습 타이머 작동 중")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "순공모드 카메라",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "순공모드 카메라 인식이 백그라운드에서 실행 중임을 알립니다"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.sm.myapplication.action.START_PURE_MODE_CAMERA"
        const val ACTION_STOP = "com.sm.myapplication.action.STOP_PURE_MODE_CAMERA"
        const val ACTION_UPDATE_LOW_LIGHT = "com.sm.myapplication.action.UPDATE_PURE_MODE_LOW_LIGHT"
        const val ACTION_UPDATE_SETTINGS = "com.sm.myapplication.action.UPDATE_PURE_MODE_SETTINGS"
        const val EXTRA_LOW_LIGHT_ENABLED = "low_light_enabled"
        private const val CHANNEL_ID = "pure_mode_camera"
        private const val NOTIFICATION_ID = 2301
    }
}
