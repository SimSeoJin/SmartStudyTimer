package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FaceOverlayView extends View {

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();

    private List<Rect> faces = new ArrayList<>();
    private int imageWidth = 0;
    private int imageHeight = 0;
    private boolean isFrontCamera = true;
    private String debugText = "";

    public FaceOverlayView(Context context) {
        super(context);
        init();
    }

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);
        boxPaint.setAntiAlias(true);

        textPaint.setColor(Color.RED);
        textPaint.setTextSize(42f);
        textPaint.setAntiAlias(true);
    }

    public void setFaces(List<Rect> faces, int imageWidth, int imageHeight, boolean isFrontCamera, String debugText) {
        this.faces = faces != null ? faces : new ArrayList<>();
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.isFrontCamera = isFrontCamera;
        this.debugText = debugText != null ? debugText : "";
        invalidate();
    }

    public void clearFaces() {
        this.faces = new ArrayList<>();
        this.debugText = "";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText(debugText, 20, 50, textPaint);

        if (faces == null || faces.isEmpty() || imageWidth == 0 || imageHeight == 0) {
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float scaleX = viewWidth / (float) imageWidth;
        float scaleY = viewHeight / (float) imageHeight;

        for (Rect face : faces) {
            float left = face.left * scaleX;
            float top = face.top * scaleY;
            float right = face.right * scaleX;
            float bottom = face.bottom * scaleY;

            if (isFrontCamera) {
                float mirroredLeft = viewWidth - right;
                float mirroredRight = viewWidth - left;
                left = mirroredLeft;
                right = mirroredRight;
            }

            canvas.drawRect(left, top, right, bottom, boxPaint);
        }
    }
}