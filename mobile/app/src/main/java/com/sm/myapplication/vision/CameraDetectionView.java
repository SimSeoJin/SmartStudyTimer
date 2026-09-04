package com.sm.myapplication.vision;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Compose/Kotlin 화면 안에 Java CameraX + MLKit + MediaPipe 분석 기능을 붙이기 위한 View.
 *
 * Kotlin UI는 유지하고, 이 View만 AndroidView로 감싸서 PureModeScreen의 카메라 영역에 넣으면 됩니다.
 */
public class CameraDetectionView extends FrameLayout {

    public interface Listener {
        void onDetection(@NonNull DetectionResult result);
        void onStatus(@NonNull String message);
    }

    private static final String TAG = "CameraDetectionView";

    private final PreviewView previewView;
    private final FaceOverlayView overlayView;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private FrameAnalyzer frameAnalyzer;
    private LifecycleOwner lifecycleOwner;
    private Listener listener;
    private boolean started = false;

    public CameraDetectionView(@NonNull Context context) {
        this(context, null);
    }

    public CameraDetectionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraDetectionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        previewView = new PreviewView(context);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        addView(previewView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        overlayView = new FaceOverlayView(context);
        addView(overlayView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    public void start(@NonNull LifecycleOwner owner, @Nullable Listener listener) {
        this.lifecycleOwner = owner;
        this.listener = listener;
        this.started = true;

        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }

        if (frameAnalyzer == null) {
            frameAnalyzer = new FrameAnalyzer(getContext(), result -> post(() -> {
                overlayView.setFaces(
                        result.faces,
                        result.imageWidth,
                        result.imageHeight,
                        result.frontCamera,
                        result.debugText
                );

                if (this.listener != null) {
                    this.listener.onDetection(result);
                }
            }));
        }

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(getContext());
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Throwable t) {
                Log.e(TAG, "카메라 시작 실패", t);
                notifyStatus("카메라 시작 실패: " + t.getClass().getSimpleName() + " / " + t.getMessage());
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    public void stop() {
        started = false;

        try {
            if (imageAnalysis != null) {
                imageAnalysis.clearAnalyzer();
            }
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
            overlayView.clearFaces();
            notifyStatus("카메라 정지");
        } catch (Throwable t) {
            Log.e(TAG, "카메라 정지 실패", t);
        }
    }

    public void release() {
        stop();

        try {
            if (frameAnalyzer != null) {
                frameAnalyzer.shutdown();
                frameAnalyzer = null;
            }
        } catch (Throwable t) {
            Log.e(TAG, "FrameAnalyzer 종료 실패", t);
        }

        try {
            if (cameraExecutor != null) {
                cameraExecutor.shutdown();
                cameraExecutor = null;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Executor 종료 실패", t);
        }

        listener = null;
        lifecycleOwner = null;
    }

    public void triggerStartGestureForDebug() {
        if (frameAnalyzer != null) {
            frameAnalyzer.triggerStartGesture();
        }
    }

    public void triggerPauseGestureForDebug() {
        if (frameAnalyzer != null) {
            frameAnalyzer.triggerPauseGesture();
        }
    }

    private void bindCameraUseCases() {
        if (!started || cameraProvider == null || lifecycleOwner == null || frameAnalyzer == null) {
            return;
        }

        try {
            cameraProvider.unbindAll();

            if (imageAnalysis != null) {
                imageAnalysis.clearAnalyzer();
            }

            preview = new Preview.Builder()
                    .setTargetResolution(new Size(640, 480))
                    .build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            ImageAnalysis.Builder analysisBuilder = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

            // CameraX 1.3.x 이상이면 RGBA 출력이 가능해서 MediaPipe Bitmap 변환 안정성이 좋아집니다.
            try {
                analysisBuilder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888);
            } catch (Throwable ignored) {
                // 낮은 CameraX 버전에서는 FrameAnalyzer의 YUV fallback을 사용합니다.
            }

            imageAnalysis = analysisBuilder.build();
            imageAnalysis.setAnalyzer(cameraExecutor, frameAnalyzer);

            CameraSelector selector;
            boolean selectedFrontCamera;
            try {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    selector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    selectedFrontCamera = true;
                } else {
                    selector = CameraSelector.DEFAULT_BACK_CAMERA;
                    selectedFrontCamera = false;
                }
            } catch (Throwable t) {
                Log.e(TAG, "카메라 확인 실패, 후면 카메라로 fallback", t);
                selector = CameraSelector.DEFAULT_BACK_CAMERA;
                selectedFrontCamera = false;
            }

            frameAnalyzer.setFrontCamera(selectedFrontCamera);
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis);
            notifyStatus(selectedFrontCamera ? "전면 카메라 연결 완료" : "후면 카메라 연결 완료");
        } catch (Throwable firstError) {
            Log.e(TAG, "카메라 바인딩 실패, 후면 fallback 시도", firstError);
            bindBackCameraFallback(firstError);
        }
    }

    private void bindBackCameraFallback(@NonNull Throwable firstError) {
        if (cameraProvider == null || lifecycleOwner == null || preview == null || imageAnalysis == null) {
            notifyStatus("카메라 연결 실패: " + firstError.getClass().getSimpleName());
            return;
        }

        try {
            cameraProvider.unbindAll();
            if (frameAnalyzer != null) {
                frameAnalyzer.setFrontCamera(false);
            }
            cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
            );
            notifyStatus("전면 실패 → 후면 카메라 연결 완료");
        } catch (Throwable secondError) {
            Log.e(TAG, "후면 fallback도 실패", secondError);
            notifyStatus("카메라 연결 실패: "
                    + secondError.getClass().getSimpleName()
                    + " / " + secondError.getMessage());
        }
    }

    private void notifyStatus(@NonNull String message) {
        if (listener != null) {
            listener.onStatus(message);
        }
    }
}
