package com.sm.myapplication.vision;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Camera;
import androidx.camera.core.ExposureState;
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
        // Connection state must not depend on words in a display message.
        default void onCameraConnectionChanged(boolean connected) { }
    }

    private static final String TAG = "CameraDetectionView";

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private Camera boundCamera;
    private int exposureRequestGeneration = 0;
    private int requestedExposureIndex = Integer.MIN_VALUE;
    private String exposureDiagnostics = "exposure=unbound";
    private PreviewView previewView;
    private Preview preview;
    private Preview.SurfaceProvider externalPreviewProvider;
    private CameraSelector activeSelector;
    private ImageAnalysis imageAnalysis;
    private FrameAnalyzer frameAnalyzer;
    private LifecycleOwner lifecycleOwner;
    private Listener listener;
    private boolean started = false;
    private int sessionGeneration = 0;
    private boolean lowLightModeEnabled = false;
    private PureModeUserSettings.Snapshot pureModeSettings;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CameraDetectionView(@NonNull Context context) {
        this(context, true);
    }

    /**
     * @param showPreview true이면 사용자에게 카메라 영상을 보여 주고, false이면 분석만 수행한다.
     */
    public CameraDetectionView(@NonNull Context context, boolean showPreview) {
        this(context, null, 0, showPreview);
    }

    public CameraDetectionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraDetectionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, true);
    }

    private CameraDetectionView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            boolean showPreview
    ) {
        super(context, attrs, defStyleAttr);

        if (showPreview) {
            previewView = new PreviewView(context);
            previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
            previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
            addView(previewView, new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            ));
        }
    }

    public void start(@NonNull LifecycleOwner owner, @Nullable Listener listener) {
        this.lifecycleOwner = owner;
        this.listener = listener;

        // Compose 재구성이나 설정 변경으로 start가 다시 호출돼도 기존 카메라 세션을 중복 바인딩하지 않는다.
        if (started) {
            return;
        }
        this.started = true;
        final int generation = ++sessionGeneration;

        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }

        if (frameAnalyzer == null) {
            frameAnalyzer = new FrameAnalyzer(getContext(), result -> mainHandler.post(() -> {
                if (started && sessionGeneration == generation && this.listener != null) {
                    this.listener.onDetection(result);
                }
            }));
            frameAnalyzer.setLowLightModeEnabled(lowLightModeEnabled);
            frameAnalyzer.setPureModeSettings(currentPureModeSettings());
        }

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(getContext());
        future.addListener(() -> {
            if (!started || sessionGeneration != generation) return;
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Throwable t) {
                Log.e(TAG, "카메라 시작 실패", t);
                notifyConnection(false);
                notifyStatus("카메라 시작 실패: " + t.getClass().getSimpleName() + " / " + t.getMessage());
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    public void stop() {
        started = false;
        sessionGeneration++;
        exposureRequestGeneration++;
        boundCamera = null;
        requestedExposureIndex = Integer.MIN_VALUE;

        try {
            if (imageAnalysis != null) {
                imageAnalysis.clearAnalyzer();
            }

            unbindOwnedUseCases();
            preview = null;
            imageAnalysis = null;
            notifyStatus("카메라 정지");
        } catch (Throwable t) {
            Log.e(TAG, "카메라 정지 실패", t);
        }
        // 진행 중인 분석이 끝난 뒤 같은 실행 스레드에서 모델을 닫는다.
        // 메인 스레드에서 즉시 close하면 얼굴 분석 Tasks.await와 충돌한다.
        FrameAnalyzer analyzerToClose = frameAnalyzer;
        ExecutorService executorToClose = cameraExecutor;
        frameAnalyzer = null;
        cameraExecutor = null;
        if (executorToClose != null) {
            if (analyzerToClose != null) executorToClose.execute(analyzerToClose::shutdown);
            executorToClose.shutdown();
        } else if (analyzerToClose != null) {
            analyzerToClose.shutdown();
        }
    }

    public void release() {
        stop();

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

    public void setLowLightModeEnabled(boolean enabled) {
        boolean changed = lowLightModeEnabled != enabled;
        lowLightModeEnabled = enabled;
        if (frameAnalyzer != null) {
            frameAnalyzer.setLowLightModeEnabled(enabled);
        }
        if (changed) applyLowLightExposure();
    }

    public void setPureModeSettings(@Nullable PureModeUserSettings.Snapshot settings) {
        pureModeSettings = settings != null ? settings : PureModeUserSettings.load(getContext());
        if (frameAnalyzer != null) {
            frameAnalyzer.setPureModeSettings(pureModeSettings);
        }
    }

    public String getLowLightDiagnostics() {
        return exposureDiagnostics + " " + (frameAnalyzer == null ? "analyzer=stopped" : frameAnalyzer.getLowLightDiagnostics());
    }

    public int getAnalyzerSessionId() { return frameAnalyzer == null ? 0 : System.identityHashCode(frameAnalyzer); }

    /** Main thread only. Change display use case without replacing ImageAnalysis or its models. */
    public void setPreviewSurfaceProvider(@Nullable Preview.SurfaceProvider provider) {
        // A previous optional Preview bind may have failed. The same provider
        // must be allowed to retry while there is still no display use case.
        if (externalPreviewProvider == provider && (provider == null || preview != null)) return;
        externalPreviewProvider = provider;
        if (!started || cameraProvider == null || imageAnalysis == null || boundCamera == null) return;
        try {
            if (provider == null) {
                if (preview != null) cameraProvider.unbind(preview);
                preview = null;
            } else {
                if (preview == null) {
                    Preview display = new Preview.Builder().setTargetResolution(new Size(640, 480)).build();
                    display.setSurfaceProvider(provider);
                    try {
                        cameraProvider.bindToLifecycle(lifecycleOwner, activeSelector, display);
                        preview = display;
                    } catch (IllegalArgumentException incompatibleSurfaces) {
                        // A solo YUV analysis stream can negotiate a MAXIMUM
                        // size that cannot coexist with Preview (even when its
                        // target was 640x480). Renegotiate the owned pair, not
                        // the analyzer/model or another owner's camera session.
                        Log.w(TAG, "Renegotiating analysis resolution for Preview", incompatibleSurfaces);
                        cameraProvider.unbind(display);
                        try {
                            cameraProvider.unbind(imageAnalysis);
                            boundCamera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner, activeSelector, display, imageAnalysis);
                            preview = display;
                            requestedExposureIndex = Integer.MIN_VALUE;
                            applyLowLightExposure();
                        } catch (Throwable displayError) {
                            Log.w(TAG, "Combined display bind failed; restoring analysis only", displayError);
                            try {
                                cameraProvider.unbind(display);
                                cameraProvider.unbind(imageAnalysis);
                                boundCamera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner, activeSelector, imageAnalysis);
                                requestedExposureIndex = Integer.MIN_VALUE;
                                applyLowLightExposure();
                            } catch (Throwable restoreError) {
                                boundCamera = null;
                                Log.e(TAG, "Analysis restoration failed", restoreError);
                                notifyConnection(false);
                                notifyStatus("카메라 연결 실패: " + restoreError.getClass().getSimpleName());
                                return;
                            }
                            throw displayError;
                        }
                    } catch (Throwable error) {
                        cameraProvider.unbind(display);
                        throw error;
                    }
                } else preview.setSurfaceProvider(provider);
            }
        } catch (Throwable error) {
            Log.e(TAG, "Preview display change failed (analysis remains bound)", error);
            notifyStatus("카메라 화면 표시 실패");
        }
    }

    private PureModeUserSettings.Snapshot currentPureModeSettings() {
        if (pureModeSettings == null) {
            pureModeSettings = PureModeUserSettings.load(getContext());
        }
        return pureModeSettings;
    }

    private void applyLowLightExposure() {
        if (boundCamera == null || !started) return;
        try {
            ExposureState state = boundCamera.getCameraInfo().getExposureState();
            float step = state.getExposureCompensationStep().floatValue();
            if (!state.isExposureCompensationSupported() || step <= 0) {
                exposureDiagnostics = "exposure=unsupported (software fallback)";
                return;
            }
            // Preserve automatic exposure in both modes. Blind +1 EV can wash out
            // a readable study scene and increase motion blur. Software rescue is adaptive.
            int index = 0;
            if (requestedExposureIndex == index) return;
            requestedExposureIndex = index;
            final int request = ++exposureRequestGeneration;
            exposureDiagnostics = "exposure=pending index=" + index;
            ListenableFuture<Integer> future = boundCamera.getCameraControl().setExposureCompensationIndex(index);
            future.addListener(() -> {
                if (!started || request != exposureRequestGeneration) return;
                try {
                    exposureDiagnostics = "exposureEV=" + future.get() * step;
                } catch (Exception error) {
                    // An unsupported/rejected camera setting must never stop software analysis.
                    exposureDiagnostics = "exposure=rejected (software fallback)";
                    requestedExposureIndex = Integer.MIN_VALUE;
                    Log.w(TAG, "Low-light exposure adjustment unavailable", error);
                }
            }, ContextCompat.getMainExecutor(getContext()));
        } catch (Exception error) {
            // Exposure is optional, including synchronous hardware/API rejection.
            // It must not make an already-bound, analyzing camera look disconnected.
            exposureDiagnostics = "exposure=rejected (software fallback)";
            requestedExposureIndex = Integer.MIN_VALUE;
            Log.w(TAG, "Exposure control unavailable; camera analysis retained", error);
        }
    }

    private void bindCameraUseCases() {
        if (!started || cameraProvider == null || lifecycleOwner == null || frameAnalyzer == null) {
            return;
        }

        try {
            unbindOwnedUseCases();

            if (imageAnalysis != null) {
                imageAnalysis.clearAnalyzer();
            }

            // 기존 순공모드와 동일한 해상도. 고해상도 원본을 매 프레임 Bitmap으로 변환하면
            // 얼굴/제스처 분석이 밀려 타이머 제어 이벤트도 늦어질 수 있다.
            ImageAnalysis.Builder analysisBuilder = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

            // Feed ML Kit the camera's native YUV image, including its rotation.
            // Only gesture/low-light processing needs a Bitmap conversion.
            analysisBuilder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888);

            imageAnalysis = analysisBuilder.build();
            imageAnalysis.setAnalyzer(cameraExecutor, frameAnalyzer);

            Preview.SurfaceProvider surfaceProvider = previewView != null ? previewView.getSurfaceProvider() : externalPreviewProvider;
            if (surfaceProvider != null) {
                preview = new Preview.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .build();
                preview.setSurfaceProvider(surfaceProvider);
            }

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
            activeSelector = selector;
            if (preview != null) {
                boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis);
            } else {
                boundCamera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, imageAnalysis);
            }
            applyLowLightExposure();
            notifyConnection(true);
            notifyStatus(selectedFrontCamera ? "전면 카메라 연결 완료" : "후면 카메라 연결 완료");
        } catch (Throwable firstError) {
            Log.e(TAG, "카메라 바인딩 실패, 후면 fallback 시도", firstError);
            bindBackCameraFallback(firstError);
        }
    }

    private void bindBackCameraFallback(@NonNull Throwable firstError) {
        if (cameraProvider == null || lifecycleOwner == null || imageAnalysis == null) {
            notifyConnection(false);
            notifyStatus("카메라 연결 실패: " + firstError.getClass().getSimpleName());
            return;
        }

        try {
            unbindOwnedUseCases();
            activeSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            if (frameAnalyzer != null) {
                frameAnalyzer.setFrontCamera(false);
            }
            if (preview != null) {
                boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );
            } else {
                boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        imageAnalysis
                );
            }
            requestedExposureIndex = Integer.MIN_VALUE;
            applyLowLightExposure();
            notifyConnection(true);
            notifyStatus("후면 카메라 연결 완료");
        } catch (Throwable secondError) {
            Log.e(TAG, "후면 fallback도 실패", secondError);
            notifyConnection(false);
            notifyStatus("카메라 연결 실패: "
                    + secondError.getClass().getSimpleName()
                    + " / " + secondError.getMessage());
        }
    }

    private void notifyConnection(boolean connected) {
        if (started && listener != null) listener.onCameraConnectionChanged(connected);
    }

    private void notifyStatus(@NonNull String message) {
        if (listener != null) {
            listener.onStatus(message);
        }
    }

    private void unbindOwnedUseCases() {
        if (cameraProvider == null) return;
        // ProcessCameraProvider는 앱 전체에서 공유된다. 이전 화면/서비스의 정리가
        // 새 소유자의 카메라까지 끊지 않도록 이 인스턴스의 UseCase만 해제한다.
        if (preview != null) cameraProvider.unbind(preview);
        if (imageAnalysis != null) cameraProvider.unbind(imageAnalysis);
    }
}
