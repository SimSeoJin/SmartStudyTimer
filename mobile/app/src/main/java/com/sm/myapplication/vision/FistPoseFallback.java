package com.sm.myapplication.vision;

/**
 * Supplemental command poses for uncertain MediaPipe classifications, not a replacement model.
 * Input is the first hand's 21 WORLD landmarks (metres), not normalized image x/y/z: image x and
 * y have different scales and image depth is not a Euclidean coordinate. All decisions below
 * use 3-D lengths or angles, so mirroring, camera rotation and hand size do not change them.
 *
 * A relaxed fist needs three curled fingers and no clearly extended finger; the thumb may lie
 * beside the fingers. An open palm needs three extended fingers and no tightly folded finger,
 * without requiring the thumb to be spread. Call only after shouldTry(), then pass a positive
 * result through the independent-frame filter. These are heuristics, not model probabilities.
 */
public final class FistPoseFallback {
    private static final int[] FINGER_BASES = {5, 9, 13, 17};
    private static final double MAX_PIP_BEND_COSINE = 0.88;
    private static final double MAX_TIP_TO_CHAIN_RATIO = 0.86;
    private static final double MAX_WRIST_RETRACTION_RATIO = 1.30;
    private static final float CLEAR_MODEL_SCORE = 0.65f;

    private FistPoseFallback() { }

    /** Keep clear model decisions, but let uncertain classifications use the hand's geometry. */
    public static boolean shouldTry(String label, float score) {
        boolean noCategory = label == null || label.isEmpty() || "no_hand".equals(label);
        if (Float.isNaN(score) || Float.isInfinite(score) || score > 1f
                || (score < 0f && !(noCategory && score == -1f))) {
            return false;
        }
        return noCategory || "None".equals(label) || score < CLEAR_MODEL_SCORE;
    }

    /** Geometry may strengthen a weak palm label, never invent a manual pause from another label. */
    public static boolean shouldTryPalm(String label, float score) {
        return "Open_Palm".equals(label) && shouldTry(label, score);
    }

    public static boolean isClosedFist(float[][] points) {
        if (!validHand(points)) return false;
        double palmLength = distance(points[0], points[9]);
        int curledFingers = 0;
        for (int base : FINGER_BASES) {
            FingerShape shape = shape(points, base);
            if (isExtended(shape)) return false;
            if (shape.cosine <= MAX_PIP_BEND_COSINE
                    && shape.tipToChain <= MAX_TIP_TO_CHAIN_RATIO
                    && shape.wristRetraction <= MAX_WRIST_RETRACTION_RATIO) curledFingers++;
        }
        if (curledFingers < 3) return false;

        // A closed group of fingers also occurs in thumbs-up: exclude its projected thumb.
        // Palm centre uses only wrist/MCP points, so curled fingertips cannot move the target.
        double[] centre = new double[3];
        int[] palm = {0, 5, 9, 13, 17};
        for (int landmark : palm) {
            for (int axis = 0; axis < 3; axis++) centre[axis] += points[landmark][axis] / 5.0;
        }
        double thumbToCentre = 0.0;
        for (int axis = 0; axis < 3; axis++) {
            double delta = points[4][axis] - centre[axis];
            thumbToCentre += delta * delta;
        }
        // A thumb beside a loose fist is fine. Reject only a thumb projecting
        // well beyond the palm, as in thumbs-up, rather than forcing it to tuck.
        boolean projectingThumb = Math.sqrt(thumbToCentre) > palmLength * 1.25
                && distance(points[4], points[0]) > palmLength * 1.65;
        return !projectingThumb;
    }

    public static boolean isOpenPalm(float[][] points) {
        if (!validHand(points)) return false;
        int extendedFingers = 0;
        for (int base : FINGER_BASES) {
            FingerShape shape = shape(points, base);
            if (isExtended(shape)) extendedFingers++;
            // A gently bent/uncertain fourth finger is fine. The caller must
            // already have a weak Open_Palm model label before using this pose.
            if (shape.tipToChain < 0.75 && shape.wristRetraction < 1.15) return false;
        }
        return extendedFingers >= 3;
    }

    private static boolean isExtended(FingerShape shape) {
        return shape.cosine >= 0.65 && shape.tipToChain >= 0.88
                && shape.wristRetraction >= 1.0;
    }

    private static boolean validHand(float[][] points) {
        if (points == null || points.length < 21) return false;
        for (int index = 0; index < 21; index++) {
            if (points[index] == null || points[index].length < 3) return false;
            for (int axis = 0; axis < 3; axis++) {
                if (Float.isNaN(points[index][axis]) || Float.isInfinite(points[index][axis])) return false;
            }
        }
        double palmLength = distance(points[0], points[9]);
        double palmWidth = distance(points[5], points[17]);
        if (!(palmLength > 0.0) || palmWidth < palmLength * 0.15
                || palmWidth > palmLength * 2.5) return false;
        if (crossLength(points[0], points[5], points[17])
                < palmLength * palmWidth * 0.04) return false;
        for (int base : FINGER_BASES) {
            if (distance(points[0], points[base + 1]) < palmLength * 0.1) return false;
            for (int joint = base; joint < base + 3; joint++) {
                double bone = distance(points[joint], points[joint + 1]);
                if (bone < palmLength * 0.015 || bone > palmLength * 1.2) return false;
            }
        }
        for (int joint = 1; joint < 4; joint++) {
            if (distance(points[joint], points[joint + 1]) < palmLength * 0.015) return false;
        }
        return true;
    }

    private static FingerShape shape(float[][] points, int base) {
        double proximal = distance(points[base], points[base + 1]);
        double middle = distance(points[base + 1], points[base + 2]);
        double distal = distance(points[base + 2], points[base + 3]);
        return new FingerShape(forwardCosine(points[base], points[base + 1],
                points[base + 2], proximal, middle),
                distance(points[base], points[base + 3]) / (proximal + middle + distal),
                distance(points[0], points[base + 3]) / distance(points[0], points[base + 1]));
    }

    private static final class FingerShape {
        final double cosine;
        final double tipToChain;
        final double wristRetraction;

        FingerShape(double cosine, double tipToChain, double wristRetraction) {
            this.cosine = cosine;
            this.tipToChain = tipToChain;
            this.wristRetraction = wristRetraction;
        }
    }

    private static double distance(float[] a, float[] b) {
        double dx = (double) a[0] - b[0];
        double dy = (double) a[1] - b[1];
        double dz = (double) a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Cosine between consecutive forward bones: 1 means straight, 0 means a 90-degree bend. */
    private static double forwardCosine(float[] mcp, float[] pip, float[] dip,
            double proximal, double middle) {
        double dot = 0.0;
        for (int axis = 0; axis < 3; axis++) {
            dot += ((double) pip[axis] - mcp[axis]) * ((double) dip[axis] - pip[axis]);
        }
        return dot / (proximal * middle);
    }

    private static double crossLength(float[] origin, float[] a, float[] b) {
        double ax = (double) a[0] - origin[0];
        double ay = (double) a[1] - origin[1];
        double az = (double) a[2] - origin[2];
        double bx = (double) b[0] - origin[0];
        double by = (double) b[1] - origin[1];
        double bz = (double) b[2] - origin[2];
        double x = ay * bz - az * by;
        double y = az * bx - ax * bz;
        double z = ax * by - ay * bx;
        return Math.sqrt(x * x + y * y + z * z);
    }
}
