package com.sm.myapplication.ui.screens.puremode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.vision.CameraDetectionView
import com.sm.myapplication.vision.DetectionResult

@Composable
fun FrontCameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var statusText by remember { mutableStateOf("카메라 준비 중") }
    val cameraViewRef = remember { mutableStateOf<CameraDetectionView?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    val detectionController = remember {
        PureModeDetectionController(onStatus = { statusText = it })
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraViewRef.value?.release()
            cameraViewRef.value = null
            detectionController.reset()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    CameraDetectionView(ctx).also { view ->
                        cameraViewRef.value = view
                        view.start(
                            lifecycleOwner,
                            object : CameraDetectionView.Listener {
                                override fun onDetection(result: DetectionResult) {
                                    detectionController.handle(result)
                                }

                                override fun onStatus(message: String) {
                                    statusText = message
                                }
                            },
                        )
                    }
                },
                update = { view ->
                    if (cameraViewRef.value !== view) cameraViewRef.value = view
                },
            )

            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 4,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "카메라 권한이 필요합니다",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { launcher.launch(Manifest.permission.CAMERA) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("권한 허용", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

private class PureModeDetectionController(
    private val onStatus: (String) -> Unit,
) {
    private companion object {
        private const val FACE_LOST_TIMEOUT_MS = 5_000L
    }

    private var faceMissingStartedAt = 0L

    fun reset() {
        faceMissingStartedAt = 0L
    }

    fun handle(result: DetectionResult) {
        val now = System.currentTimeMillis()
        val debug = result.debugText.take(180)

        if (result.pauseGestureDetected) {
            faceMissingStartedAt = 0L
            TimerController.onPauseGesture()
            onStatus("주먹 제스처 → 수동 일시중지\n$debug")
            return
        }

        if (result.startGestureDetected) {
            faceMissingStartedAt = 0L
            TimerController.onStartGesture()
            onStatus("손바닥 제스처 → 시작/재개\n$debug")
            return
        }

        if (result.faceDetected) {
            faceMissingStartedAt = 0L
            TimerController.onFaceDetected()
            onStatus("얼굴 감지 중\n$debug")
            return
        }

        if (faceMissingStartedAt == 0L) {
            faceMissingStartedAt = now
        }

        val lostDuration = now - faceMissingStartedAt
        if (lostDuration >= FACE_LOST_TIMEOUT_MS) {
            TimerController.onFaceLost()
            onStatus("5초 이상 얼굴 미감지 → 자동 일시중지\n$debug")
        } else {
            onStatus("얼굴 미감지 ${(lostDuration / 1000.0).format1()}초\n$debug")
        }
    }
}

private fun Double.format1(): String = "%.1f".format(this)
