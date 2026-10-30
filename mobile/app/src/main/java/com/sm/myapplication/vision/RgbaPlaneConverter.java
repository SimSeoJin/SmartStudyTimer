package com.sm.myapplication.vision;

import java.nio.ByteBuffer;

/** CameraX RGBA plane -> packed ARGB colors; no Android or native-endian assumptions. */
public final class RgbaPlaneConverter {
    private RgbaPlaneConverter() { }

    public static int[] toArgb(ByteBuffer source, int width, int height, int rowStride, int pixelStride) {
        if (width <= 0 || height <= 0 || pixelStride < 4 || rowStride < (long) width * pixelStride) {
            throw new IllegalArgumentException("Invalid RGBA plane geometry");
        }
        int origin = source.position();
        long end = origin + (long) (height - 1) * rowStride + (long) (width - 1) * pixelStride + 4;
        if (end > source.limit() || (long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Incomplete RGBA plane");
        }
        int[] colors = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = origin + y * rowStride + x * pixelStride;
                int r = source.get(offset) & 255;
                int g = source.get(offset + 1) & 255;
                int b = source.get(offset + 2) & 255;
                int a = source.get(offset + 3) & 255;
                colors[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return colors;
    }
}
