package com.sm.myapplication.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Independent current-frame rescue for study poses/edge faces missed by ML Kit.
 * Single-worker owned. Never treats an old box, skin colour, or a visible hand
 * as evidence of a face. Padding is neutral: no invented/mirrored facial parts.
 */
public final class PartialFaceDetector implements AutoCloseable {
    private final FaceDetector detector;
    private com.google.mlkit.vision.face.FaceDetector confirmationDetector;

    public PartialFaceDetector(Context context) {
        detector = FaceDetector.createFromOptions(context,
                FaceDetector.FaceDetectorOptions.builder()
                        .setBaseOptions(BaseOptions.builder()
                                .setModelAssetPath("blaze_face_short_range.tflite")
                                .setDelegate(Delegate.CPU).build())
                        .setRunningMode(RunningMode.IMAGE)
                        .setMinDetectionConfidence(PartialFaceAcceptance.MIN_EDGE_CONFIDENCE)
                        .build());
    }

    public List<Rect> detect(Bitmap currentFrame) {
        return detect(currentFrame, null);
    }

    public List<Rect> detect(Bitmap currentFrame, Rect searchHint) {
        List<Rect> boxes = detectOwnedCopy(currentFrame, 0, 0,
                currentFrame.getWidth(), currentFrame.getHeight());
        if (!boxes.isEmpty()) return boxes;

        boxes = detectPadded(currentFrame, 0, 0,
                currentFrame.getWidth(), currentFrame.getHeight());
        if (!boxes.isEmpty() || searchHint == null || searchHint.height() <= 0) return boxes;

        // Zoom current edge pixels using only the old face's SCALE/Y as a search
        // hint. Never feed old pixels or turn the hint itself into a detection.
        for (float scale : new float[]{1.5f, 2.0f}) {
            int size = Math.min(Math.min(currentFrame.getWidth(), currentFrame.getHeight()),
                    Math.max(96, Math.round(searchHint.height() * scale)));
            int top = Math.max(0, Math.min(currentFrame.getHeight() - size,
                    searchHint.centerY() - size / 2));
            for (int left : new int[]{0, currentFrame.getWidth() - size}) {
                Bitmap crop = Bitmap.createBitmap(currentFrame, left, top, size, size);
                try {
                    boxes = detectPadded(crop, left, top,
                            currentFrame.getWidth(), currentFrame.getHeight());
                    if (!boxes.isEmpty()) return boxes;
                } finally {
                    if (crop != currentFrame) crop.recycle();
                }
            }
        }
        return boxes;
    }

    private List<Rect> detectPadded(Bitmap currentFrame, int cropLeft, int cropTop,
                                    int realWidth, int realHeight) {
        // Edge detections need anchors beyond the image boundary. Adding neutral
        // context permits those anchors without assuming a face is still there.
        int padX = Math.max(1, currentFrame.getWidth() / 4);
        int padY = Math.max(1, currentFrame.getHeight() / 4);
        Bitmap padded = Bitmap.createBitmap(currentFrame.getWidth() + padX * 2,
                currentFrame.getHeight() + padY * 2, Bitmap.Config.ARGB_8888);
        try {
            padded.eraseColor(Color.rgb(127, 127, 127));
            new Canvas(padded).drawBitmap(currentFrame, padX, padY, null);
            // Apply confidence/edge checks in ORIGINAL camera coordinates,
            // not at artificial crop or neutral-padding boundaries.
            return detectOwnedCopy(padded, padX - cropLeft, padY - cropTop, realWidth, realHeight);
        } finally {
            padded.recycle();
        }
    }

    private List<Rect> detectOwnedCopy(Bitmap frame, int offsetX, int offsetY,
                                       int realWidth, int realHeight) {
        // MPImage.close may recycle its Bitmap; never give it a caller-owned one.
        Bitmap owned = frame.copy(Bitmap.Config.ARGB_8888, false);
        if (owned == null) throw new IllegalStateException("No face rescue bitmap");
        MPImage image = null;
        try {
            image = new BitmapImageBuilder(owned).build();
            List<Rect> boxes = new ArrayList<>();
            for (Detection detection : detector.detect(image).detections()) {
                RectF box = detection.boundingBox();
                if (detection.categories().isEmpty()
                        || !PartialFaceAcceptance.accepts(detection.categories().get(0).score(),
                        box.left - offsetX, box.top - offsetY, box.right - offsetX,
                        box.bottom - offsetY, realWidth, realHeight)) continue;
                Rect clipped = new Rect((int) Math.floor(box.left) - offsetX,
                        (int) Math.floor(box.top) - offsetY,
                        (int) Math.ceil(box.right) - offsetX,
                        (int) Math.ceil(box.bottom) - offsetY);
                if (clipped.intersect(0, 0, realWidth, realHeight)
                        && clipped.width() > 0 && clipped.height() > 0) boxes.add(clipped);
            }
            if (!boxes.isEmpty()) {
                // Low-threshold BlazeFace can mistake even a clipped fist for
                // a face. Require independent facial evidence in these SAME
                // pixels. Padding/zoom lets ML Kit inspect edge fragments that
                // it could not see in the original full camera frame.
                if (confirmationDetector == null) {
                    confirmationDetector = FaceDetection.getClient(
                            FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE));
                }
                List<Rect> confirmed = new ArrayList<>();
                try {
                    for (Face face : Tasks.await(confirmationDetector.process(InputImage.fromBitmap(owned, 0)))) {
                        Rect box = new Rect(face.getBoundingBox());
                        box.offset(-offsetX, -offsetY);
                        if (box.intersect(0, 0, realWidth, realHeight)) confirmed.add(box);
                    }
                } catch (Exception error) {
                    // Optional confirmation failure is not face evidence.
                    return new ArrayList<>();
                }
                return confirmed;
            }
            return boxes;
        } finally {
            if (image != null) image.close();
            if (!owned.isRecycled()) owned.recycle();
        }
    }

    @Override public void close() {
        try { detector.close(); }
        finally { if (confirmationDetector != null) confirmationDetector.close(); }
    }
}
