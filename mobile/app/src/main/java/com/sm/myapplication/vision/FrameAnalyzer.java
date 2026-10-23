package com.sm.myapplication.vision;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mediapipe.tasks.components.containers.Category;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    public interface OnDetectionListener {
        void onDetection(DetectionResult result);
    }

    private static final String TAG = "FrameAnalyzer";
    // A study session needs face presence, not a frontal pose, visible mouth,
    // eye-open classification, or identity recognition.
    private static final float MIN_FACE_SIZE = 0.03f;


    private final Context appContext;
    private final OnDetectionListener listener;
    private final FaceDetector faceDetector;
    private volatile FaceDetector lowLightFaceDetector;
    private final Object lowLightModelLock = new Object();
    private final ExecutorService lowLightWarmupExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean lowLightWarmupStarted = new AtomicBoolean(false);
    private final LowLightRescueBudget lowLightRescueBudget = new LowLightRescueBudget();
    private final StudyFaceRescueBudget studyFaceRescueBudget = new StudyFaceRescueBudget();
    private final ExecutorService faceRescueExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean faceRescueBusy = new AtomicBoolean(false);
    // Created, used, and closed exclusively on faceRescueExecutor.
    private volatile PartialFaceDetector partialFaceDetector;
    private long lastPartialFaceInitFailureAt = Long.MIN_VALUE;
    private final LowLightEnhancer lowLightEnhancer = new LowLightEnhancer();
    private int[] enhancementPixels = new int[0];
    private volatile long lastFrameAnalysisMs;
    private volatile long lastConversionMs;
    private volatile long lastEnhancementMs;
    private volatile long lastRescueMs;
    private volatile String lastRescuePath = "none";
    private volatile String lowLightDiagnostics = "software=off";
    private long enhancedFrameCount = 0;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicInteger gestureSettingsGeneration = new AtomicInteger(0);

    private final ExecutorService gestureExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean gestureBusy = new AtomicBoolean(false);
    private final GestureEventFilter gestureFilter = new GestureEventFilter();
    private volatile boolean closed;
    private volatile long gestureAnalysisCount;
    private int filteredGestureGeneration = -1;
    private GestureRecognizerHelper gestureHelper;
    private final GestureModelRecovery gestureRecovery = new GestureModelRecovery();
    private boolean gestureAvailable = false;

    private long lastFaceAnalyzeAt = Long.MIN_VALUE;
    private long lastFaceLogAt = 0L;
    private volatile long faceAnalysisCount = 0L;
    private long lastGestureAnalyzeAt = 0L;

    private volatile boolean simulatedStartGesture = false;
    private volatile boolean simulatedPauseGesture = false;
    private volatile boolean frontCamera = true;
    private volatile boolean lowLightModeEnabled = false;
    private volatile long faceAnalyzeIntervalMs = 300L;
    private volatile long gestureAnalyzeIntervalMs = 100L;
    private volatile boolean gestureRecognitionEnabled = true;

    private int lastImageWidth = 0;
    private int lastImageHeight = 0;
    // Only a tone-metering hint, never a substitute for a current face result.
    private Rect recentFaceRegion;
    private long recentFaceRegionAt = 0L;
    private int recentFaceWidth = 0;
    private int recentFaceHeight = 0;
    private static final long FACE_METERING_MEMORY_MS = 2_000L;
    private volatile FaceSearchHint faceSearchHint;

    private static final class FaceSearchHint {
        final Rect box;
        final int width, height;
        final long capturedAt;
        FaceSearchHint(Rect box, int width, int height, long capturedAt) {
            this.box = new Rect(box);
            this.width = width;
            this.height = height;
            this.capturedAt = capturedAt;
        }
    }

    private Rect currentSearchHint(int width, int height, long capturedAt) {
        FaceSearchHint hint = faceSearchHint;
        if (hint == null || hint.width <= 0 || hint.height <= 0
                || capturedAt < hint.capturedAt || capturedAt - hint.capturedAt > 5_000L) return null;
        return new Rect(hint.box.left * width / hint.width, hint.box.top * height / hint.height,
                hint.box.right * width / hint.width, hint.box.bottom * height / hint.height);
    }


    private volatile String lastGestureLabelForDebug = "not_run";
    private volatile float lastGestureScoreForDebug = -1f;
    private volatile String lastGestureSource = "none";
    private volatile String lastGestureErrorForDebug = "none";
    private String lastGestureAssetForDebug = "not checked";

    public FrameAnalyzer(Context context, OnDetectionListener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;

        faceDetector = FaceDetection.getClient(faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST));
    }

    static FaceDetectorOptions faceOptions(int performanceMode) {
        return new FaceDetectorOptions.Builder().setPerformanceMode(performanceMode)
                .setMinFaceSize(MIN_FACE_SIZE).enableTracking().build();
    }

    public void setFrontCamera(boolean frontCamera) {
        this.frontCamera = frontCamera;
    }

    /**
     * 저조도 모드에서는 어두운 프레임만 밝게 보정한 뒤 얼굴·제스처 모델에 전달합니다.
     * 밝은 환경에서는 원본 프레임을 그대로 사용합니다.
     */
    public void setLowLightModeEnabled(boolean enabled) {
        lowLightModeEnabled = enabled;
        if (enabled && faceAnalysisCount > 0) startFaceRescueWarmup();
    }

    public void setPureModeSettings(PureModeUserSettings.Snapshot settings) {
        if (settings == null) return;
        faceAnalyzeIntervalMs = settings.faceAnalyzeIntervalMs;
        gestureAnalyzeIntervalMs = settings.gestureAnalyzeIntervalMs;
        boolean gestureEnabledChanged = gestureRecognitionEnabled != settings.gestureRecognitionEnabled;
        gestureRecognitionEnabled = settings.gestureRecognitionEnabled;
        if (gestureEnabledChanged) gestureSettingsGeneration.incrementAndGet();
    }

    public String getLowLightDiagnostics() {
        return lowLightDiagnostics + " faceFrames=" + faceAnalysisCount
                + " gestureFrames=" + gestureAnalysisCount + " gestureBusy=" + gestureBusy.get()
                + " gesture=" + lastGestureLabelForDebug + " score=" + lastGestureScoreForDebug
                + " gestureSource=" + lastGestureSource
                + " gestureError=" + lastGestureErrorForDebug
                + " gestureEnabled=" + gestureRecognitionEnabled
                + " gestureModelReady=" + gestureRecovery.isReady()
                + " gestureInitAttempts=" + gestureRecovery.getInitializationAttempts()
                + " faceRescueBusy=" + faceRescueBusy.get()
                + " partialFaceReady=" + (partialFaceDetector != null)
                + " lowLightReady=" + (lowLightFaceDetector != null)
                + " frameMs=" + lastFrameAnalysisMs + " conversionMs=" + lastConversionMs
                + " enhancementMs=" + lastEnhancementMs + " rescueMs=" + lastRescueMs
                + " rescuePath=" + lastRescuePath;
    }

    public long getLastFrameAnalysisMs() { return lastFrameAnalysisMs; }

    private void startFaceRescueWarmup() {
        // Share one warmed ACCURATE model between normal pose/edge rescue and
        // low-light rescue. Never block the first FAST face result on cold init.
        if (closed || !lowLightWarmupStarted.compareAndSet(false, true)) return;
        try {
            faceRescueExecutor.execute(() -> {
                // Prepare while the original FAST detector is already working,
                // not only after the user first moves partly out of the frame.
                ensurePartialFaceDetector();
            });
            lowLightWarmupExecutor.execute(() -> {
                Bitmap ownedWarmFrame = null;
                FaceDetector candidate = null;
                try {
                    if (closed) return;
                    candidate = FaceDetection.getClient(faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE));
                    ownedWarmFrame = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888);
                    ownedWarmFrame.eraseColor(0xff000000);
                    // Own input until completion; never retain a camera ImageProxy here.
                    Tasks.await(candidate.process(InputImage.fromBitmap(ownedWarmFrame, 0)));
                    synchronized (lowLightModelLock) {
                        if (!closed) {
                            lowLightFaceDetector = candidate;
                            candidate = null; // Transfer ownership to analyzer shutdown.
                        }
                    }
                } catch (Exception error) {
                    Log.w(TAG, "Face rescue prewarm unavailable; FAST remains active", error);
                } finally {
                    if (ownedWarmFrame != null) ownedWarmFrame.recycle();
                    if (candidate != null) {
                        try { candidate.close(); } catch (Exception error) { Log.w(TAG, "Warm detector close failed", error); }
                    }
                }
            });
        } catch (RejectedExecutionException error) {
            // Shutdown won the race. No owned frame/client was created.
            Log.d(TAG, "Low-light prewarm skipped after shutdown");
        }
    }

    /** faceRescueExecutor only; optional model failure never removes ML Kit. */
    private void ensurePartialFaceDetector() {
        if (closed || partialFaceDetector != null) return;
        long current = SystemClock.elapsedRealtime();
        if (lastPartialFaceInitFailureAt != Long.MIN_VALUE
                && current - lastPartialFaceInitFailureAt < 5_000L) return;
        try {
            partialFaceDetector = new PartialFaceDetector(appContext);
        } catch (Exception error) {
            lastPartialFaceInitFailureAt = SystemClock.elapsedRealtime();
            Log.w(TAG, "Partial-face model unavailable; ML Kit retained", error);
        }
    }

    public void triggerStartGesture() {
        simulatedStartGesture = true;
    }

    public void triggerPauseGesture() {
        simulatedPauseGesture = true;
    }

    private void ensureGestureHelper() {
        if (closed || !gestureRecognitionEnabled || gestureAvailable
                || !gestureRecovery.canInitialize(SystemClock.elapsedRealtime())) return;

        try {
            gestureHelper = new GestureRecognizerHelper();
            gestureAvailable = gestureHelper.init(appContext);
            lastGestureErrorForDebug = gestureHelper.getLastErrorMessage();
            lastGestureAssetForDebug = gestureHelper.getLastAssetCheckMessage();
        } catch (Throwable t) {
            Log.e(TAG, "Gesture helper init crashed", t);
            gestureAvailable = false;
            gestureHelper = null;
            lastGestureErrorForDebug = t.getClass().getSimpleName() + " / " + String.valueOf(t.getMessage());
        }
        gestureRecovery.onInitializationResult(gestureAvailable, SystemClock.elapsedRealtime());
        if (!gestureAvailable && gestureHelper != null) {
            gestureHelper.close();
            gestureHelper = null;
        }
    }

    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        long now = SystemClock.elapsedRealtime();

        boolean gestureEnabledThisFrame = gestureRecognitionEnabled;
        int gestureGenerationThisFrame = gestureSettingsGeneration.get();
        if (!gestureEnabledThisFrame) resetGestureState("disabled");
        // While an optional current-frame rescue is pending, keep admitting hand
        // frames without publishing newer FAST negatives over that face result.
        boolean doFace = !faceRescueBusy.get()
                && (lastFaceAnalyzeAt == Long.MIN_VALUE || now - lastFaceAnalyzeAt >= faceAnalyzeIntervalMs);
        boolean doGesture = gestureEnabledThisFrame && !gestureBusy.get() &&
                (now - lastGestureAnalyzeAt >= gestureAnalyzeIntervalMs);

        if (closed || (!doFace && !doGesture)) {
            imageProxy.close();
            return;
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        boolean faceDetected = false;
        boolean faceResultDeferred = false;
        List<Rect> faceRects = new ArrayList<>();
        Bitmap frameBitmap = null;
        Bitmap analysisBitmap = null;
        boolean lowLightApplied = false;
        final boolean lowLightThisFrame = lowLightModeEnabled;
        lastConversionMs = lastEnhancementMs = lastRescueMs = 0L;
        lastRescuePath = lowLightThisFrame ? "original" : "off";
        if (!lowLightThisFrame) {
            lowLightEnhancer.reset();
            lowLightDiagnostics = "software=off enhancedFrames=" + enhancedFrameCount;
        }

        int outputWidth = lastImageWidth > 0 ? lastImageWidth : imageProxy.getWidth();
        int outputHeight = lastImageHeight > 0 ? lastImageHeight : imageProxy.getHeight();

        try {
            // Capture the hand input before waiting for ML Kit, so neither model's
            // inference/initialization holds up the other's scheduling.
            if (doGesture) {
                try {
                    lastGestureAnalyzeAt = now;
                    frameBitmap = imageProxyToBitmap(imageProxy, lowLightThisFrame);
                    if (frameBitmap != null) {
                        analysisBitmap = prepareAnalysisBitmap(frameBitmap, lowLightThisFrame);
                        lowLightApplied = analysisBitmap != frameBitmap;
                        submitGesture(analysisBitmap, lowLightApplied, now, gestureGenerationThisFrame);
                    }
                } catch (Exception error) {
                    Log.w(TAG, "Optional gesture preprocessing failed; continue face analysis", error);
                }
            }
            if (doFace) {
                lastFaceAnalyzeAt = now;
                int rotation = imageProxy.getImageInfo().getRotationDegrees();
                outputWidth = rotation % 180 == 0 ? imageProxy.getWidth() : imageProxy.getHeight();
                outputHeight = rotation % 180 == 0 ? imageProxy.getHeight() : imageProxy.getWidth();
                try {
                    InputImage originalImage = null;
                    if (imageProxy.getFormat() == ImageFormat.YUV_420_888 && imageProxy.getImage() != null) {
                        originalImage = InputImage.fromMediaImage(imageProxy.getImage(), rotation);
                    }
                    if (originalImage == null) {
                        if (frameBitmap == null) frameBitmap = imageProxyToBitmap(imageProxy, lowLightThisFrame);
                        if (frameBitmap != null) {
                            originalImage = InputImage.fromBitmap(frameBitmap, 0);
                            outputWidth = frameBitmap.getWidth();
                            outputHeight = frameBitmap.getHeight();
                        }
                    }
                    if (originalImage == null) throw new IllegalStateException("No valid face image");
                    // Low-light ON must retain the exact normal-mode detector/input.
                    // Enhancement is an optional rescue, not a replacement for a good result.
                    List<Face> faces = Tasks.await(faceDetector.process(originalImage));
                    lowLightApplied = false;
                    if (lowLightThisFrame && !faces.isEmpty()) {
                        lowLightDiagnostics = "software=original-face enhancedFrames=" + enhancedFrameCount;
                    } else if (lowLightThisFrame) {
                        try {
                            if (!lowLightRescueBudget.canAttempt(SystemClock.elapsedRealtime(), now)) {
                                lastRescuePath = "budget-skip";
                                lowLightDiagnostics = "software=budget-skip enhancedFrames=" + enhancedFrameCount;
                            } else {
                                if (frameBitmap == null) frameBitmap = imageProxyToBitmap(imageProxy, true);
                                if (frameBitmap != null) {
                                    if (analysisBitmap == null) analysisBitmap = prepareAnalysisBitmap(frameBitmap, true);
                                    if (analysisBitmap != frameBitmap) {
                                        long rescueStarted = SystemClock.elapsedRealtime();
                                        if (!lowLightRescueBudget.canAttempt(rescueStarted, now)) {
                                            lastRescuePath = "budget-skip";
                                        } else {
                                            lastRescuePath = "enhanced-fast";
                                            List<Face> rescued = Tasks.await(faceDetector.process(InputImage.fromBitmap(analysisBitmap, 0)));
                                            lastRescueMs = SystemClock.elapsedRealtime() - rescueStarted;
                                            // Slow ACCURATE work runs only AFTER the new
                                            // partial-face model on its independent worker.
                                            if (!rescued.isEmpty()) {
                                                faces = rescued;
                                                outputWidth = analysisBitmap.getWidth();
                                                outputHeight = analysisBitmap.getHeight();
                                                lowLightApplied = true;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception rescueError) {
                            // Optional enhancement/model failure cannot discard the normal result.
                            Log.w(TAG, "Low-light rescue unavailable; normal result retained", rescueError);
                        }
                    }
                    // Also rescue normal-light profile/small/edge faces. Accept
                    // only a real result from THIS frame, never a cached box.
                    // Existing low-light processing/positive results take priority.
                    if (faces.isEmpty()) {
                        FaceDetector ready = lowLightFaceDetector;
                        long rescueStarted = SystemClock.elapsedRealtime();
                        // A cheap independent model must not be starved by a
                        // slower enhanced FAST pass. Captured time is retained;
                        // the controller still rejects stale returned evidence.
                        if (rescueStarted >= now && rescueStarted - now < 3_000L) {
                            if (frameBitmap == null) frameBitmap = imageProxyToBitmap(imageProxy, false);
                            if (frameBitmap != null) {
                                // Start absence grace NOW, not after slow rescue.
                                // Send this before enqueuing: a quick rescue must
                                // never publish positive before its FAST negative.
                                if (listener != null) listener.onDetection(new DetectionResult(
                                        false, false, false, Collections.emptyList(), outputWidth, outputHeight,
                                        frontCamera, false, true, now));
                                Bitmap enhanced = analysisBitmap != frameBitmap ? analysisBitmap : null;
                                faceResultDeferred = submitOriginalFaceRescue(frameBitmap, enhanced, ready, now);
                            }
                        }
                    }
                    faceRects.clear();
                    for (Face face : faces) faceRects.add(face.getBoundingBox());
                    faceDetected = !faceRects.isEmpty();
                    if (faceDetected) {
                        recentFaceRegion = new Rect(faceRects.get(0));
                        recentFaceRegionAt = SystemClock.elapsedRealtime();
                        recentFaceWidth = outputWidth;
                        recentFaceHeight = outputHeight;
                        faceSearchHint = new FaceSearchHint(faceRects.get(0), outputWidth, outputHeight, now);
                    }
                    faceAnalysisCount++;
                    if (faceAnalysisCount == 1 || now - lastFaceLogAt >= 5_000L) {
                        lastFaceLogAt = now;
                        // Metadata only: no image, landmark, or personal data is logged/saved.
                        Log.d(TAG, "face frames=" + faceAnalysisCount + " detected=" + faceDetected
                                + " input=" + outputWidth + "x" + outputHeight + " rotation=" + rotation
                                + " format=" + imageProxy.getFormat() + " lowLight=" + lowLightThisFrame
                                + " analysisMs=" + (SystemClock.elapsedRealtime() - now));
                    }
                } catch (Exception faceError) {
                    Log.e(TAG, "Face analysis failed", faceError);
                    faceDetected = false;
                    faceRects.clear();
                }
                lastImageWidth = outputWidth;
                lastImageHeight = outputHeight;
                if (listener != null && !faceResultDeferred) {
                    listener.onDetection(new DetectionResult(faceDetected, false, false,
                            new ArrayList<>(faceRects), outputWidth, outputHeight, frontCamera, lowLightApplied, true, now));
                }
            }

            boolean manualStart = simulatedStartGesture;
            boolean manualPause = simulatedPauseGesture;
            simulatedStartGesture = simulatedPauseGesture = false;
            if (gestureEnabledThisFrame && (manualStart || manualPause)) {
                publishGesture(manualStart, manualPause, outputWidth, outputHeight, lowLightApplied, now,
                        gestureGenerationThisFrame);
            }

        } catch (Throwable t) {
            Log.e(TAG, "Analyze error", t);
            // A low-light/conversion/model failure must not indefinitely replay
            // a previous positive face and keep the focus timer running.

            DetectionResult result = new DetectionResult(
                    false,
                    false,
                    false,
                    new ArrayList<>(),
                    outputWidth,
                    outputHeight,
                    frontCamera,
                    false,
                    doFace,
                    now
            );

            if (listener != null) {
                listener.onDetection(result);
            }
        } finally {
            if (analysisBitmap != null && analysisBitmap != frameBitmap) analysisBitmap.recycle();
            if (frameBitmap != null) frameBitmap.recycle();
            isProcessing.set(false);
            imageProxy.close();
            lastFrameAnalysisMs = SystemClock.elapsedRealtime() - now;
            if (faceAnalysisCount > 0) startFaceRescueWarmup();
        }
    }

    private boolean submitOriginalFaceRescue(Bitmap source, Bitmap enhanced,
                                              FaceDetector ready, long capturedAt) {
        if (closed || !faceRescueBusy.compareAndSet(false, true)) return false;
        Bitmap originalCopy = null;
        Bitmap enhancedCopy = null;
        try {
            originalCopy = source.copy(Bitmap.Config.ARGB_8888, false);
            if (originalCopy == null) throw new IllegalStateException("No owned rescue bitmap");
            if (enhanced != null && enhanced != source) {
                enhancedCopy = enhanced.copy(Bitmap.Config.ARGB_8888, false);
            }
        } catch (Throwable error) {
            if (originalCopy != null) originalCopy.recycle();
            if (enhancedCopy != null) enhancedCopy.recycle();
            faceRescueBusy.set(false);
            return false;
        }
        final Bitmap owned = originalCopy;
        final Bitmap ownedEnhanced = enhancedCopy;
        final boolean capturedFrontCamera = frontCamera;
        try {
            faceRescueExecutor.execute(() -> {
                long startedAt = SystemClock.elapsedRealtime();
                try {
                    if (closed) return;
                    List<Rect> boxes = new ArrayList<>();
                    boolean enhancedDetected = false;
                    Rect searchHint = currentSearchHint(owned.getWidth(), owned.getHeight(), capturedAt);
                    try {
                        ensurePartialFaceDetector();
                        if (partialFaceDetector != null) {
                            boxes.addAll(partialFaceDetector.detect(owned, searchHint));
                            lastRescuePath = "current-partial-face";
                            if (boxes.isEmpty() && ownedEnhanced != null
                                    && SystemClock.elapsedRealtime() - capturedAt < 3_000L) {
                                boxes.addAll(partialFaceDetector.detect(ownedEnhanced, searchHint));
                                lastRescuePath = "enhanced-partial-face";
                                enhancedDetected = !boxes.isEmpty();
                            }
                        }
                    } catch (Exception error) {
                        Log.w(TAG, "Optional partial-face rescue failed", error);
                    }
                    long accurateStarted = SystemClock.elapsedRealtime();
                    boolean useEnhanced = ownedEnhanced != null;
                    if (boxes.isEmpty() && ready != null && !closed
                            && studyFaceRescueBudget.canAttempt(accurateStarted, capturedAt)
                            && studyFaceRescueBudget.canUseAccurate(accurateStarted, faceAnalyzeIntervalMs)
                            && (!useEnhanced || lowLightRescueBudget.canUseAccurate(accurateStarted, faceAnalyzeIntervalMs))) {
                        studyFaceRescueBudget.accurateStarted();
                        if (useEnhanced) lowLightRescueBudget.accurateStarted();
                        lastRescuePath = useEnhanced ? "enhanced-accurate" : "original-accurate";
                        try {
                            Bitmap accurateInput = useEnhanced ? ownedEnhanced : owned;
                            for (Face face : Tasks.await(ready.process(InputImage.fromBitmap(accurateInput, 0)))) {
                                boxes.add(face.getBoundingBox());
                            }
                            enhancedDetected = useEnhanced && !boxes.isEmpty();
                        } catch (Exception error) {
                            Log.w(TAG, "Optional original-face rescue unavailable", error);
                        } finally {
                            long completedAt = SystemClock.elapsedRealtime();
                            studyFaceRescueBudget.accurateFinished(completedAt);
                            if (useEnhanced) lowLightRescueBudget.accurateFinished(completedAt, completedAt - accurateStarted);
                        }
                    }
                    // Preserve capture time: stale model results must still be
                    // rejected by the existing detection/timer controller.
                    if (!closed && !boxes.isEmpty()) {
                        Rect nextHint = new Rect(boxes.get(0));
                        if (searchHint != null && searchHint.height() > nextHint.height()) {
                            int centerY = nextHint.centerY();
                            nextHint.top = centerY - searchHint.height() / 2;
                            nextHint.bottom = nextHint.top + searchHint.height();
                        }
                        faceSearchHint = new FaceSearchHint(nextHint, owned.getWidth(), owned.getHeight(), capturedAt);
                    }
                    if (!closed && listener != null) listener.onDetection(new DetectionResult(
                            !boxes.isEmpty(), false, false, boxes, owned.getWidth(), owned.getHeight(),
                            capturedFrontCamera, enhancedDetected, true, capturedAt));
                } finally {
                    lastRescueMs = SystemClock.elapsedRealtime() - startedAt;
                    owned.recycle();
                    if (ownedEnhanced != null) ownedEnhanced.recycle();
                    faceRescueBusy.set(false);
                }
            });
            return true;
        } catch (RejectedExecutionException error) {
            owned.recycle();
            if (ownedEnhanced != null) ownedEnhanced.recycle();
            faceRescueBusy.set(false);
            return false;
        }
    }

    // A bounded hand-model worker never blocks subsequent face frames.
    private void submitGesture(Bitmap bitmap, boolean lowLight, long capturedAt, int generation) {
        if (closed || !gestureBusy.compareAndSet(false, true)) return;
        final Bitmap owned;
        try {
            owned = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        } catch (Throwable error) {
            gestureBusy.set(false);
            Log.w(TAG, "Gesture frame copy failed", error);
            return;
        }
        try {
            gestureExecutor.execute(() -> {
                try {
                    if (closed || !isGestureResultStillEnabled(true, generation)) return;
                    if (filteredGestureGeneration != generation) {
                        gestureFilter.reset();
                        filteredGestureGeneration = generation;
                    }
                    ensureGestureHelper();
                    if (!gestureAvailable || gestureHelper == null) return;
                    if (SystemClock.elapsedRealtime() - capturedAt > 3_000L) {
                        gestureFilter.reset();
                        return;
                    }
                    GestureRecognizerResult result = gestureHelper.recognize(owned, capturedAt);
                    gestureAnalysisCount++;
                    lastGestureErrorForDebug = gestureHelper.getLastErrorMessage();
                    // A missing hand is a valid, non-null result. Only actual
                    // model errors count toward rebuilding the gesture model.
                    boolean rebuild = gestureRecovery.onInferenceResult(result != null,
                            SystemClock.elapsedRealtime());
                    if (result == null) {
                        gestureFilter.reset();
                        lastGestureLabelForDebug = "model_error";
                        lastGestureScoreForDebug = -1f;
                        if (rebuild) {
                            gestureAvailable = false;
                            gestureHelper.close();
                            gestureHelper = null;
                        }
                        return;
                    }
                    String label = "no_hand";
                    float score = -1f;
                    if (!result.gestures().isEmpty() && !result.gestures().get(0).isEmpty()) {
                        Category top = result.gestures().get(0).get(0);
                        label = top.categoryName();
                        score = top.score();
                    }
                    lastGestureSource = "model";
                    // Reference ZIP policy: classify the actual model label;
                    // do not turn None/weak categories into landmark commands.
                    lastGestureLabelForDebug = label;
                    lastGestureScoreForDebug = score;
                    if (closed || !isGestureResultStillEnabled(true, generation)
                            || SystemClock.elapsedRealtime() - capturedAt > 3_000L) {
                        gestureFilter.reset();
                        return;
                    }
                    String event = gestureFilter.update(label, score, capturedAt);
                    // User controls: closed fist resumes; open palm pauses.
                    if (!event.isEmpty()) publishGesture(GestureEventFilter.isResumeCommand(event),
                            GestureEventFilter.isPauseCommand(event), owned.getWidth(), owned.getHeight(),
                            lowLight, capturedAt, generation);
                } catch (Throwable error) {
                    gestureFilter.reset();
                    lastGestureErrorForDebug = error.getClass().getSimpleName();
                    Log.e(TAG, "Gesture analysis failed", error);
                } finally {
                    owned.recycle();
                    gestureBusy.set(false);
                }
            });
        } catch (RejectedExecutionException error) {
            owned.recycle();
            gestureBusy.set(false);
        }
    }

    private void publishGesture(boolean start, boolean pause, int width, int height,
            boolean lowLight, long capturedAt, int generation) {
        if (!closed && listener != null && isGestureResultStillEnabled(true, generation)) {
            listener.onDetection(new DetectionResult(false, start, pause, Collections.emptyList(),
                    width, height, frontCamera, lowLight, false, capturedAt));
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy, boolean lowLightThisFrame) {
        long conversionStarted = SystemClock.elapsedRealtime();
        try {
            Bitmap bitmap;
            if (imageProxy.getFormat() == PixelFormat.RGBA_8888 || imageProxy.getPlanes().length == 1) {
                bitmap = rgbaImageProxyToBitmap(imageProxy);
            } else {
                bitmap = yuvImageProxyToBitmap(imageProxy);
            }

            if (bitmap == null) return null;
            bitmap = rotateBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
            int longest = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (longest > 640) {
                float scale = 640f / longest;
                Bitmap smaller = Bitmap.createScaledBitmap(bitmap,
                        Math.max(1, Math.round(bitmap.getWidth() * scale)),
                        Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
                if (smaller != bitmap) bitmap.recycle();
                bitmap = smaller;
            }
            return bitmap;
        } catch (Throwable t) {
            Log.e(TAG, "imageProxyToBitmap failed", t);
            lastGestureErrorForDebug = "bitmap convert: " + t.getClass().getSimpleName()
                    + " / " + String.valueOf(t.getMessage());
            return null;
        } finally {
            lastConversionMs = SystemClock.elapsedRealtime() - conversionStarted;
        }
    }

    private Bitmap prepareAnalysisBitmap(Bitmap source, boolean enabled) {
        if (!enabled) return source;
        long enhancementStarted = SystemClock.elapsedRealtime();
        int width = source.getWidth();
        int height = source.getHeight();
        if (enhancementPixels.length != width * height) enhancementPixels = new int[width * height];
        source.getPixels(enhancementPixels, 0, width, 0, 0, width, height);
        Rect region = new Rect(width / 8, height / 10, width * 7 / 8, height * 9 / 10);
        boolean useRecentFace = recentFaceRegion != null
                && recentFaceWidth > 0 && recentFaceHeight > 0
                && SystemClock.elapsedRealtime() - recentFaceRegionAt <= FACE_METERING_MEMORY_MS;
        if (useRecentFace) {
            region = new Rect(recentFaceRegion.left * width / recentFaceWidth,
                    recentFaceRegion.top * height / recentFaceHeight,
                    recentFaceRegion.right * width / recentFaceWidth,
                    recentFaceRegion.bottom * height / recentFaceHeight);
        }
        boolean applied = lowLightEnhancer.enhance(enhancementPixels, width, height,
                region.left, region.top, region.right, region.bottom);
        lastEnhancementMs = SystemClock.elapsedRealtime() - enhancementStarted;
        if (applied) enhancedFrameCount++;
        lowLightDiagnostics = "software=" + (applied ? "enhanced"
                : lowLightEnhancer.hasUsableDetail() ? "bright-enough" : "insufficient-detail")
                + " metering=" + (useRecentFace ? "recent-face" : "central-shadows")
                + " roiLuma=" + Math.round(lowLightEnhancer.getMeasuredLuma())
                + " contrast=" + lowLightEnhancer.getMeasuredContrast()
                + " shadowPercent=" + Math.round(lowLightEnhancer.getShadowFraction() * 100)
                + " gamma=" + Math.round(lowLightEnhancer.getGamma() * 100) / 100.0
                + " enhancedFrames=" + enhancedFrameCount;
        if (!applied) return source;
        Bitmap enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        enhanced.setPixels(enhancementPixels, 0, width, 0, 0, width, height);
        return enhanced;
    }

    private boolean isGestureResultStillEnabled(boolean gestureEnabledThisFrame, int gestureGenerationThisFrame) {
        return gestureEnabledThisFrame
                && gestureRecognitionEnabled
                && gestureGenerationThisFrame == gestureSettingsGeneration.get();
    }

    private void resetGestureState(String labelForDebug) {
        lastGestureLabelForDebug = labelForDebug;
        lastGestureScoreForDebug = -1f;
    }

    private Bitmap rgbaImageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes == null || planes.length == 0) return null;

        ImageProxy.PlaneProxy plane = planes[0];
        ByteBuffer buffer = plane.getBuffer().duplicate();

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();

        // Camera buffers may omit padding after the last row. Copy actual pixels
        // with explicit RGBA byte order rather than copying a padded Bitmap.
        int[] colors = RgbaPlaneConverter.toArgb(buffer, width, height, rowStride, pixelStride);
        return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
    }

    private Bitmap yuvImageProxyToBitmap(ImageProxy imageProxy) {
        byte[] nv21 = yuv420888ToNv21(imageProxy);
        if (nv21 == null) return null;

        android.graphics.YuvImage yuvImage =
                new android.graphics.YuvImage(
                        nv21,
                        ImageFormat.NV21,
                        imageProxy.getWidth(),
                        imageProxy.getHeight(),
                        null
                );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean compressed = yuvImage.compressToJpeg(
                new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()),
                90,
                out
        );
        if (!compressed) return null;

        byte[] imageBytes = out.toByteArray();
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        int longest = Math.max(imageProxy.getWidth(), imageProxy.getHeight());
        options.inSampleSize = 1;
        while (longest / (options.inSampleSize * 2) >= 640) options.inSampleSize *= 2;
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }

    private byte[] yuv420888ToNv21(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes == null || planes.length < 3) return null;

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();
        byte[] nv21 = new byte[width * height * 3 / 2];

        ByteBuffer yBuffer = planes[0].getBuffer().duplicate();
        ByteBuffer uBuffer = planes[1].getBuffer().duplicate();
        ByteBuffer vBuffer = planes[2].getBuffer().duplicate();

        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();
        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();
        int yStart = yBuffer.position();
        int uStart = uBuffer.position();
        int vStart = vBuffer.position();

        int outIndex = 0;
        for (int row = 0; row < height; row++) {
            int rowOffset = yStart + row * yRowStride;
            for (int col = 0; col < width; col++) {
                nv21[outIndex++] = yBuffer.get(rowOffset + col * yPixelStride);
            }
        }

        int chromaHeight = height / 2;
        int chromaWidth = width / 2;
        for (int row = 0; row < chromaHeight; row++) {
            int uRowOffset = uStart + row * uRowStride;
            int vRowOffset = vStart + row * vRowStride;
            for (int col = 0; col < chromaWidth; col++) {
                // NV21은 VU 순서다.
                nv21[outIndex++] = vBuffer.get(vRowOffset + col * vPixelStride);
                nv21[outIndex++] = uBuffer.get(uRowOffset + col * uPixelStride);
            }
        }

        return nv21;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (bitmap == null || rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) bitmap.recycle();
        return rotated;
    }

    public void shutdown() {
        FaceDetector lowLightToClose;
        synchronized (lowLightModelLock) {
            if (closed) return;
            closed = true;
            lowLightToClose = lowLightFaceDetector;
            lowLightFaceDetector = null;
        }
        try {
            faceDetector.close();
        } catch (Throwable t) {
            Log.e(TAG, "FaceDetector close failed", t);
        }
        // Do not close a detector or recycle its owned input during inference.
        faceRescueExecutor.execute(() -> {
            try {
                if (lowLightToClose != null) lowLightToClose.close();
            } catch (Exception error) {
                Log.w(TAG, "Face rescue detector close failed", error);
            } finally {
                if (partialFaceDetector != null) {
                    partialFaceDetector.close();
                    partialFaceDetector = null;
                }
            }
        });
        faceRescueExecutor.shutdown();
        // Never interrupt warm Tasks.await and recycle its still-in-use owned frame.
        lowLightWarmupExecutor.shutdown();

        // VIDEO inference and close stay on the same worker.
        gestureExecutor.execute(() -> {
            if (gestureHelper != null) gestureHelper.close();
        });
        gestureExecutor.shutdown();
    }
}
