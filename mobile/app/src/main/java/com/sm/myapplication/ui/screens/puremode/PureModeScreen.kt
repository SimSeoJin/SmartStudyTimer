package com.sm.myapplication.ui.screens.puremode

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.Context
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.camera.view.PreviewView
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.repository.AppRepository
import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.ui.components.NavBar
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.screens.timer.ResponsiveTimerText
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.DarkStatusBarEffect
import com.sm.myapplication.ui.theme.FocusRed
import com.sm.myapplication.ui.theme.FocusCameraTop
import com.sm.myapplication.ui.theme.FocusCameraBottom
import com.sm.myapplication.ui.theme.FocusBg
import com.sm.myapplication.ui.theme.FocusSurface
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.FocusGreen
import com.sm.myapplication.vision.PureModeCameraService
import com.sm.myapplication.vision.PureModeUserSettings
import com.sm.myapplication.vision.CameraStatusPolicy

@Composable
fun PureModeScreen(onBack: () -> Unit) {
    DarkStatusBarEffect()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var displayActive by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    val displayStates = remember { pureModeDisplayStates(TimerController.state) }
    val state by displayStates.collectAsStateWithLifecycle(
        initialValue = pureModeDisplayState(TimerController.state.value),
    )
    var cameraPreviewVisible by remember { mutableStateOf(true) }
    var lowLightModeEnabled by remember { mutableStateOf(false) }
    var cameraStatus by remember { mutableStateOf("카메라 권한을 확인하는 중") }
    var cameraPermissionRequested by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraService by remember { mutableStateOf<PureModeCameraService?>(null) }
    var sessionEnded by remember { mutableStateOf(false) }
    var pureModeSettings by remember { mutableStateOf(PureModeUserSettings.load(context)) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val statusUi = remember(state.isRunning, state.pauseCause, state.message, cameraStatus) {
        pureModeStatusUi(state.isRunning, state.pauseCause, state.message, cameraStatus)
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val cameraSession = remember(context, hasCameraPermission) { PureModeSessionGate() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        cameraStatus = if (granted) "카메라 시작 중" else "카메라 권한이 필요합니다"
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        // Session decisions need the REAL elapsed time, not the UI projection.
        val initialState = TimerController.state.value
        if (cameraSession.isOpen && (initialState.mode != StudyMode.PURE || (!initialState.isRunning && initialState.elapsedMs == 0L))) {
            TimerController.start(StudyMode.PURE, autoRun = false)
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission && !cameraPermissionRequested) {
            cameraPermissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // The service owns analysis for BOTH visible and hidden camera states.
    // Toggling the UI changes only its Preview surface, never its lifecycle/model.
    DisposableEffect(cameraSession) {
        var bound = false
        var serviceStarted = false
        var connectedService: PureModeCameraService? = null
        val serviceIntent = Intent(context, PureModeCameraService::class.java)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = (binder as? PureModeCameraService.LocalBinder)?.getService() ?: return
                cameraSession.runIfOpen {
                    connectedService = service
                    cameraService = service
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                cameraSession.runIfOpen {
                    if (cameraService === connectedService) cameraService = null
                    connectedService = null
                    TimerController.onCameraOff()
                    cameraStatus = "카메라 오류"
                }
            }
        }
        cameraSession.setReleaseAction {
            // Only this effect's captured binding is released. Never consult a
            // later screen's mutable service when an old transition is disposed.
            val ownedService = connectedService
            connectedService = null
            try {
                ownedService?.setPreviewSurfaceProvider(null)
                ownedService?.stopSession()
            } finally {
                if (cameraService === ownedService) cameraService = null
                try {
                    if (serviceStarted) {
                        serviceStarted = false
                        context.stopService(serviceIntent)
                    }
                } finally {
                    if (bound) {
                        bound = false
                        context.unbindService(connection)
                    }
                    TimerController.onCameraOff()
                }
            }
        }
        if (hasCameraPermission && !sessionEnded) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, PureModeCameraService::class.java).apply {
                    action = PureModeCameraService.ACTION_START
                    putExtra(PureModeCameraService.EXTRA_LOW_LIGHT_ENABLED, lowLightModeEnabled)
                })
                serviceStarted = true
                bound = context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
                if (!bound) {
                    cameraStatus = "카메라 서비스 연결 실패"
                    cameraSession.close()
                }
            } catch (error: RuntimeException) {
                cameraStatus = "카메라 시작 실패"
                cameraSession.close()
            }
        }
        onDispose { cameraSession.close() }
    }

    LaunchedEffect(cameraSession, cameraService) {
        cameraService?.cameraStatusFlow?.collect { message ->
            cameraSession.runIfOpen { cameraStatus = message }
        }
    }
    LaunchedEffect(cameraSession, cameraService, pureModeSettings) {
        cameraSession.runIfOpen { cameraService?.updateSettings(pureModeSettings) }
    }
    LaunchedEffect(cameraSession, cameraService, lowLightModeEnabled) {
        cameraSession.runIfOpen { cameraService?.setLowLightEnabled(lowLightModeEnabled) }
    }
    DisposableEffect(lifecycleOwner, cameraSession) {
        cameraSession.runIfOpen {
            // A reattached owner may already be STARTED and will not replay ON_START.
            displayActive = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        val observer = LifecycleEventObserver { _, event ->
            cameraSession.runIfOpen {
                when (event) {
                    Lifecycle.Event.ON_START -> displayActive = true
                    Lifecycle.Event.ON_STOP -> {
                        displayActive = false
                        cameraService?.setPreviewSurfaceProvider(null)
                    }
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(cameraSession, cameraService, cameraPreviewVisible, previewView, displayActive) {
        cameraSession.runIfOpen {
            cameraService?.setPreviewSurfaceProvider(if (cameraPreviewVisible && displayActive) previewView?.surfaceProvider else null)
        }
    }
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val finish: () -> Unit = {
        if (!sessionEnded) {
            sessionEnded = true
            // Unbind now, not after the navigation transition. A new visit must
            // not reuse a service still owned by this outgoing screen.
            cameraSession.close()
            TimerController.stop(AppRepository.get(context.applicationContext as Application))
            onBack()
        }
    }
    BackHandler(onBack = finish)

    Column(
        modifier = Modifier.fillMaxSize().background(FocusBg)
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            NavBar(title = "순공모드", titleColor = CardWhite)
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "순공모드 설정", tint = CardWhite)
            }
        }

        if (!hasCameraPermission) {
            CameraPermissionNotice(
                modifier = Modifier.weight(1f),
                onRequestPermission = {
                    cameraPermissionRequested = true
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
            )
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                if (cameraPreviewVisible) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(FocusCameraTop, FocusCameraBottom)))
                            .border(0.5.dp, CardWhite.copy(alpha = 0.14f), RoundedCornerShape(22.dp)),
                    ) {
                        AndroidView(
                            factory = { viewContext ->
                                PreviewView(viewContext).apply {
                                    // Prefer a dedicated SurfaceView. CameraX retains
                                    // its TextureView fallback on unsupported devices.
                                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }.also { previewView = it }
                            },
                            onRelease = { released ->
                                cameraSession.runIfOpen {
                                    if (previewView === released) {
                                        cameraService?.setPreviewSurfaceProvider(null)
                                        previewView = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Keep the latest UI's alignment guide, without debug boxes or text.
                        Box(
                            modifier = Modifier.align(Alignment.Center).size(width = 150.dp, height = 195.dp)
                                .border(
                                    1.5.dp,
                                    if (state.isRunning) FocusGreen.copy(alpha = 0.75f) else CardWhite.copy(alpha = 0.3f),
                                    RoundedCornerShape(percent = 48),
                                ),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }

                TimerLine(big = !cameraPreviewVisible)
                Spacer(Modifier.height(8.dp))
                StatusLine(statusUi)
                if (!cameraPreviewVisible) Spacer(Modifier.weight(1f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PureModeActionButton(
                icon = if (cameraPreviewVisible) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                label = if (cameraPreviewVisible) "숨기기" else "보기",
                contentDescription = if (cameraPreviewVisible) "카메라 화면 숨기기" else "카메라 화면 보기",
                selected = !cameraPreviewVisible,
                modifier = Modifier.weight(1f),
                onClick = { cameraPreviewVisible = !cameraPreviewVisible },
            )
            PureModeActionButton(
                icon = Icons.Filled.DarkMode,
                label = "저조도",
                contentDescription = if (lowLightModeEnabled) "저조도 모드 끄기" else "저조도 모드 켜기",
                selected = lowLightModeEnabled,
                modifier = Modifier.weight(1f),
                onClick = { lowLightModeEnabled = !lowLightModeEnabled },
            )
            PureModeActionButton(
                icon = Icons.Filled.Stop,
                label = "종료",
                contentDescription = "순공모드 종료",
                selected = false,
                modifier = Modifier.weight(1f),
                onClick = finish,
            )
        }
    }

    if (showSettingsDialog) {
        PureModeSettingsDialog(
            settings = pureModeSettings,
            onDismiss = { showSettingsDialog = false },
            onFaceAwayTimeoutSelected = { timeoutMs ->
                pureModeSettings = PureModeUserSettings.saveFaceAwayTimeout(context, timeoutMs)
            },
            onGestureRecognitionChanged = { enabled ->
                pureModeSettings = PureModeUserSettings.saveGestureRecognitionEnabled(context, enabled)
            },
            onRecognitionModeSelected = { mode ->
                pureModeSettings = PureModeUserSettings.saveRecognitionMode(context, mode)
            },
        )
    }
}

@Composable
private fun PureModeSettingsDialog(
    settings: PureModeUserSettings.Snapshot,
    onDismiss: () -> Unit,
    onFaceAwayTimeoutSelected: (Long) -> Unit,
    onGestureRecognitionChanged: (Boolean) -> Unit,
    onRecognitionModeSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("순공모드 설정", color = LabelPrimary, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("얼굴 이탈 후 일시정지", color = LabelPrimary, fontWeight = FontWeight.SemiBold)
                    listOf(
                        PureModeUserSettings.FACE_AWAY_TIMEOUT_3S to "3초",
                        PureModeUserSettings.FACE_AWAY_TIMEOUT_5S to "5초",
                        PureModeUserSettings.FACE_AWAY_TIMEOUT_10S to "10초",
                    ).forEach { (timeoutMs, label) ->
                        PureModeRadioRow(
                            label = label,
                            selected = settings.faceAwayTimeoutMs == timeoutMs,
                            onClick = { onFaceAwayTimeoutSelected(timeoutMs) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("손 제스처", color = LabelPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.gestureRecognitionEnabled,
                        onCheckedChange = onGestureRecognitionChanged,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("인식 방식", color = LabelPrimary, fontWeight = FontWeight.SemiBold)
                    PureModeRadioRow(
                        label = "배터리 절약",
                        selected = settings.recognitionMode == PureModeUserSettings.MODE_BATTERY_SAVER,
                        onClick = { onRecognitionModeSelected(PureModeUserSettings.MODE_BATTERY_SAVER) },
                    )
                    PureModeRadioRow(
                        label = "인식 정확도 우선",
                        selected = settings.recognitionMode == PureModeUserSettings.MODE_ACCURACY,
                        onClick = { onRecognitionModeSelected(PureModeUserSettings.MODE_ACCURACY) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = LabelPrimary)
            }
        },
    )
}

@Composable
private fun PureModeRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(label, color = LabelPrimary, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(description, color = LabelPrimary.copy(alpha = 0.68f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CameraPermissionNotice(modifier: Modifier = Modifier, onRequestPermission: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "카메라 권한이 필요합니다",
            color = CardWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "순공모드에서 얼굴과 제스처를 인식하려면\n카메라 사용을 허용해 주세요.",
            color = CardWhite.copy(alpha = 0.68f),
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        PillButton(
            label = "카메라 권한 허용",
            modifier = Modifier.fillMaxWidth(),
            onClick = onRequestPermission,
        )
    }
}

internal data class PureModeStatusUi(
    val icon: ImageVector?,
    val label: String,
    val activeDot: Boolean = false,
)

internal fun pureModeStatusUi(
    isRunning: Boolean,
    pauseCause: TimerPauseCause,
    message: String,
    cameraStatus: String,
): PureModeStatusUi {
    if (!isRunning && CameraStatusPolicy.isConnectionFailure(cameraStatus)) {
        return PureModeStatusUi(Icons.Filled.VideocamOff, "카메라 오류")
    }

    return when {
        pauseCause == TimerPauseCause.CAMERA_OFF -> PureModeStatusUi(Icons.Filled.VideocamOff, "카메라 오류")
        !isRunning && pauseCause == TimerPauseCause.AUTO_FACE_LOST -> PureModeStatusUi(Icons.Filled.Pause, "얼굴 미감지")
        !isRunning && pauseCause == TimerPauseCause.MANUAL_GESTURE -> PureModeStatusUi(Icons.Filled.PanTool, "수동 정지")
        !isRunning && pauseCause == TimerPauseCause.WAITING_FACE -> PureModeStatusUi(Icons.Filled.Person, "얼굴이 보이지 않아요")
        message.contains("손바닥") || message.contains("주먹") -> {
            PureModeStatusUi(Icons.Filled.PanTool, if (isRunning) "다시 시작" else "일시정지")
        }
        message.contains("다시") || message.contains("시작") || message.contains("재개") -> {
            PureModeStatusUi(Icons.Filled.PlayArrow, "다시 시작")
        }
        isRunning -> PureModeStatusUi(null, "집중 중", activeDot = true)
        else -> PureModeStatusUi(Icons.Filled.Pause, "일시정지")
    }
}

@Composable
private fun StatusLine(status: PureModeStatusUi) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (status.activeDot) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(FocusGreen),
            )
        } else if (status.icon != null) {
            Icon(
                imageVector = status.icon,
                contentDescription = null,
                tint = CardWhite,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = status.label,
            color = CardWhite,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun TimerLine(big: Boolean = false) {
    val elapsedSeconds = remember { pureModeElapsedSeconds(TimerController.state) }
    val seconds by elapsedSeconds.collectAsStateWithLifecycle(
        initialValue = TimerController.state.value.elapsedMs / 1_000L,
    )
    ResponsiveTimerText(
        text = formatElapsedDigits(seconds * 1_000L),
        maxFontSize = if (big) 56f else 48f,
        minFontSize = 24f,
        color = CardWhite,
    )
}

@Composable
private fun PillButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(FocusSurface)
            .border(1.dp, CardWhite.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
            .pressable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = CardWhite, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
private fun PureModeActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val destructive = icon == Icons.Filled.Stop
    val background = when {
        destructive -> FocusRed.copy(alpha = 0.16f)
        selected -> FocusGreen.copy(alpha = 0.18f)
        else -> FocusSurface
    }
    val tint = if (destructive) FocusRed else if (selected) FocusGreen else CardWhite

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .border(1.dp, CardWhite.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .pressable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(label, color = CardWhite, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

private fun formatElapsedDigits(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec / 60) % 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
