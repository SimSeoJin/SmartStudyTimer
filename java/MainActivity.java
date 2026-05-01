package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final long FACE_LOST_TIMEOUT_MS = 5000L;

    private ActivityMainBinding binding;
    private ExecutorService cameraExecutor;

    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private boolean cameraEnabled = true;

    private FrameAnalyzer frameAnalyzer;
    private DebugTimer debugTimer;
    private TimerStateManager timerStateManager;

    private long lastFaceSeenAt = 0L;
    private boolean autoPauseTriggeredByFaceLoss = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    binding.tvLog.setText("카메라 권한이 거부되었습니다.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cameraExecutor = Executors.newSingleThreadExecutor();

        debugTimer = new DebugTimer(elapsedMillis ->
                runOnUiThread(() -> binding.tvTimer.setText(DebugTimer.formatTime(elapsedMillis)))
        );

        timerStateManager = new TimerStateManager(debugTimer, (newState, reason) ->
                runOnUiThread(() -> {
                    binding.tvState.setText("STATE: " + newState.name());
                    binding.tvLog.setText(reason);
                })
        );

        frameAnalyzer = new FrameAnalyzer(
                this,
                result -> runOnUiThread(() -> handleDetectionResult(result))
        );

        setupButtons();
        checkCameraPermissionAndStart();
    }

    private void setupButtons() {
        binding.btnStartGesture.setOnClickListener(v -> {
            frameAnalyzer.triggerStartGesture();
            binding.tvLog.setText("디버그: 시작 제스처 예약");
        });

        binding.btnPauseGesture.setOnClickListener(v -> {
            frameAnalyzer.triggerPauseGesture();
            binding.tvLog.setText("디버그: 일시중지 제스처 예약");
        });

        binding.btnCameraToggle.setOnClickListener(v -> {
            cameraEnabled = !cameraEnabled;

            if (cameraEnabled) {
                binding.btnCameraToggle.setText("카메라 OFF");
                startCamera();
                binding.tvLog.setText("카메라 ON");
            } else {
                binding.btnCameraToggle.setText("카메라 ON");
                stopCamera();
                binding.faceOverlayView.clearFaces();
                timerStateManager.onCameraOff();
                binding.tvLog.setText("카메라 OFF");
            }
        });
    }

    private void handleDetectionResult(DetectionResult result) {
        long now = System.currentTimeMillis();

        Log.d(TAG, result.debugText);

        binding.faceOverlayView.setFaces(
                result.faces,
                result.imageWidth,
                result.imageHeight,
                result.frontCamera,
                result.debugText
        );

        if (result.pauseGestureDetected) {
            timerStateManager.onPauseGesture();
            autoPauseTriggeredByFaceLoss = false;
            binding.tvLog.setText(result.debugText + " / 제스처: 일시중지");
            return;
        }

        if (result.startGestureDetected) {
            timerStateManager.onStartGesture();
            autoPauseTriggeredByFaceLoss = false;
            binding.tvLog.setText(result.debugText + " / 제스처: 시작");
            return;
        }

        if (result.faceDetected) {
            lastFaceSeenAt = now;

            if (timerStateManager.getCurrentState() == TimerStateManager.TimerState.AUTO_PAUSED) {
                timerStateManager.onFaceDetected();
                autoPauseTriggeredByFaceLoss = false;
            }

            binding.tvLog.setText(
                    result.debugText + " / 얼굴 감지 / state=" +
                            timerStateManager.getCurrentState().name()
            );
        } else {
            if (lastFaceSeenAt == 0L) {
                binding.tvLog.setText(result.debugText + " / 아직 얼굴 첫 감지 전");
                return;
            }

            long lostDuration = now - lastFaceSeenAt;

            if (lostDuration >= FACE_LOST_TIMEOUT_MS && !autoPauseTriggeredByFaceLoss) {
                timerStateManager.onFaceLost();
                autoPauseTriggeredByFaceLoss = true;
            }

            binding.tvLog.setText(
                    result.debugText
                            + " / 미감지 " + String.format("%.1f", lostDuration / 1000.0) + "초"
                            + " / 기준 " + (FACE_LOST_TIMEOUT_MS / 1000) + "초"
                            + " / state=" + timerStateManager.getCurrentState().name()
            );
        }
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "카메라 시작 실패", e);
                binding.tvLog.setText("카메라 시작 실패: " + e.getClass().getSimpleName()
                        + " / " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
        }

        preview = new Preview.Builder()
                .setTargetResolution(new Size(640, 480))
                .build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                // MediaPipe에는 Bitmap을 넘기므로 RGBA로 받으면 YUV 수동 변환 오류를 줄일 수 있다.
                // 사용하는 CameraX 버전이 너무 낮아 컴파일이 안 되면 이 줄을 제거해도 FrameAnalyzer가 YUV fallback을 수행한다.
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, frameAnalyzer);

        CameraSelector cameraSelector;
        boolean selectedFrontCamera;
        try {
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                selectedFrontCamera = true;
                Log.d(TAG, "Using FRONT camera");
            } else {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                selectedFrontCamera = false;
                Log.d(TAG, "FRONT unavailable, using BACK camera");
            }
        } catch (Exception e) {
            Log.e(TAG, "Camera availability check failed", e);
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            selectedFrontCamera = false;
        }

        try {
            frameAnalyzer.setFrontCamera(selectedFrontCamera);
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            lastFaceSeenAt = 0L;
            autoPauseTriggeredByFaceLoss = false;

            binding.tvLog.setText("카메라 바인딩 완료");
        } catch (Exception e) {
            Log.e(TAG, "bindToLifecycle 실패", e);

            try {
                CameraSelector fallback = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                frameAnalyzer.setFrontCamera(false);
                cameraProvider.bindToLifecycle(this, fallback, preview, imageAnalysis);

                lastFaceSeenAt = 0L;
                autoPauseTriggeredByFaceLoss = false;

                binding.tvLog.setText("전면 실패 → 후면 카메라 바인딩 완료");
            } catch (Exception e2) {
                Log.e(TAG, "후면 fallback도 실패", e2);
                binding.tvLog.setText("카메라 시작 실패: "
                        + e2.getClass().getSimpleName() + " / " + e2.getMessage());
            }
        }
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
        }

        lastFaceSeenAt = 0L;
        autoPauseTriggeredByFaceLoss = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (frameAnalyzer != null) {
            frameAnalyzer.shutdown();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
