package com.sm.myapplication.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector as MediaPipeFaceDetector
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import kotlin.random.Random

/** Public fixture only. Logs crop/occlusion limits; not a physical-mask accuracy claim. */
class FaceVisibilityInstrumentedTest {
    /** A remembered search rectangle is never evidence; only current pixels count. */
    @Test fun hintedRescueDetectsThirtyFivePercentFacesButRejectsBlankAndHands() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = instrumentation.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }
        val portrait = Bitmap.createScaledBitmap(source, 480, 640, true)
        val fist = instrumentation.context.assets.open("fist.jpg").use { BitmapFactory.decodeStream(it) }
        val fast = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
        val partial = PartialFaceDetector(instrumentation.targetContext)
        val failures = mutableListOf<String>()
        try {
            val hint = Rect(Tasks.await(fast.process(InputImage.fromBitmap(portrait, 0))).first().boundingBox)
            fun checkCurrent(label: String, bitmap: Bitmap, expectedFace: Boolean) {
                val boxes = partial.detect(bitmap, hint)
                Log.i("HintFaceRescueProbe", "$label expected=$expectedFace hint=$hint boxes=$boxes")
                if (boxes.isNotEmpty() != expectedFace) failures.add("$label expected=$expectedFace boxes=$boxes")
            }
            checkCurrent("normal", portrait, true)
            for (leftEdge in listOf(true, false)) {
                val current = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
                try {
                    current.eraseColor(Color.GRAY)
                    val fraction = .35f
                    val offset = if (leftEdge) -(hint.right - hint.width() * fraction)
                        else 480f - hint.left - hint.width() * fraction
                    Canvas(current).drawBitmap(portrait, offset, 0f, null)
                    checkCurrent("${if (leftEdge) "left" else "right"}-visible-.35", current, true)
                } finally { current.recycle() }
            }
            val current = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
            try {
                for (color in intArrayOf(Color.BLACK, Color.GRAY, Color.WHITE)) {
                    current.eraseColor(color)
                    checkCurrent("negative-blank-$color", current, false)
                }
                // The real public hand image is placed at the same vertical
                // search area, including both edges where ROI rescue searches.
                // A prior portrait/hint must never bless those hand pixels.
                val handHeight = maxOf(1, (hint.height() * 1.5f).toInt())
                val handWidth = maxOf(1, fist.width * handHeight / fist.height)
                val hand = Bitmap.createScaledBitmap(fist, handWidth, handHeight, true)
                try {
                    for (location in listOf("hint-center", "left-edge", "right-edge")) {
                        current.eraseColor(Color.GRAY)
                        val x = when (location) {
                            "left-edge" -> -hand.width / 2f
                            "right-edge" -> current.width - hand.width / 2f
                            else -> hint.centerX() - hand.width / 2f
                        }
                        Canvas(current).drawBitmap(hand, x, hint.centerY() - hand.height / 2f, null)
                        checkCurrent("negative-fist-$location", current, false)
                    }
                } finally { if (hand !== fist) hand.recycle() }
            } finally { current.recycle() }
            // Keep every result visible even if a .35 fixture remains below
            // model capability or any hard-negative exposes an unsafe path.
            assertTrue("Hinted rescue mismatches: ${failures.joinToString("; ")}", failures.isEmpty())
        } finally {
            partial.close(); fast.close(); fist.recycle()
            if (portrait !== source) portrait.recycle()
            source.recycle()
        }
    }

    /** Edge acceptance must not turn an edge-clipped real hand into a face. */
    @Test fun characterizeEdgeHandNegatives() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fist = instrumentation.context.assets.open("fist.jpg").use { BitmapFactory.decodeStream(it) }
        val partial = PartialFaceDetector(instrumentation.targetContext)
        val falsePositives = mutableListOf<String>()
        try {
            for (fraction in floatArrayOf(.50f, .25f)) {
                for (edge in listOf("left", "right", "top", "bottom")) {
                    val current = Bitmap.createBitmap(fist.width, fist.height, Bitmap.Config.ARGB_8888)
                    try {
                        current.eraseColor(Color.GRAY)
                        val hiddenWidth = fist.width * (1f - fraction)
                        val hiddenHeight = fist.height * (1f - fraction)
                        val offsetX = when (edge) { "left" -> -hiddenWidth; "right" -> hiddenWidth; else -> 0f }
                        val offsetY = when (edge) { "top" -> -hiddenHeight; "bottom" -> hiddenHeight; else -> 0f }
                        // Only move the public hand photo beyond the boundary;
                        // do not generate or paste any face-like features.
                        Canvas(current).drawBitmap(fist, offsetX, offsetY, null)
                        val boxes = partial.detect(current)
                        val label = "fist-$edge-visible-$fraction"
                        Log.i("EdgeHandNegativeProbe", "$label input=${current.width}x${current.height} boxes=$boxes")
                        if (boxes.isNotEmpty()) falsePositives.add("$label: $boxes")
                    } finally { current.recycle() }
                }
            }
            // Report every clipping direction before failing, so calibration
            // cannot accidentally hide later hard-negative failures.
            assertTrue("Clipped hands must not count as a face: ${falsePositives.joinToString("; ")}", falsePositives.isEmpty())
        } finally { partial.close(); fist.recycle() }
    }

    /** Crop coordinates are hints only; every inference uses the CURRENT clipped frame. */
    @Test fun characterizeEdgeZoomCandidates() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = instrumentation.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }
        val portrait = Bitmap.createScaledBitmap(source, 480, 640, true)
        val fast = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
        val accepted = PartialFaceDetector(instrumentation.targetContext)
        val raw = MediaPipeFaceDetector.createFromOptions(instrumentation.targetContext,
            MediaPipeFaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder()
                    .setModelAssetPath("blaze_face_short_range.tflite").setDelegate(Delegate.CPU).build())
                .setRunningMode(RunningMode.IMAGE).setMinDetectionConfidence(.10f).build())
        fun decimal(value: Float) = String.format(Locale.US, "%.4f", value)
        fun rawPass(label: String, bitmap: Bitmap, cropLeft: Int, cropTop: Int, padX: Int, padY: Int) {
            val owned = checkNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, false))
            val image = BitmapImageBuilder(owned).build()
            try {
                val detections = raw.detect(image).detections()
                Log.i("EdgeZoomFaceProbe", "$label input=${bitmap.width}x${bitmap.height} count=${detections.size}")
                for ((index, detection) in detections.withIndex()) {
                    val box = detection.boundingBox()
                    val score = detection.categories().maxOfOrNull { it.score() } ?: -1f
                    val sourceBox = "${decimal(box.left - padX + cropLeft)},${decimal(box.top - padY + cropTop)},${decimal(box.right - padX + cropLeft)},${decimal(box.bottom - padY + cropTop)}"
                    val points = detection.keypoints().orElse(emptyList()).mapIndexed { pointIndex, point ->
                        "$pointIndex:(${decimal(point.x() * bitmap.width - padX + cropLeft)},${decimal(point.y() * bitmap.height - padY + cropTop)})"
                    }.joinToString(" ")
                    Log.i("EdgeZoomFaceProbe", "$label candidate=$index score=${decimal(score)} sourceBox=($sourceBox) keypoints=$points")
                }
            } finally {
                image.close()
                if (!owned.isRecycled) owned.recycle()
            }
        }
        try {
            val hint = Tasks.await(fast.process(InputImage.fromBitmap(portrait, 0))).first().boundingBox
            for (fraction in floatArrayOf(.50f, .35f, .25f)) {
                for (leftEdge in listOf(true, false)) {
                    val current = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
                    try {
                        current.eraseColor(Color.GRAY)
                        val offset = if (leftEdge) -(hint.right - hint.width() * fraction)
                            else 480f - hint.left - hint.width() * fraction
                        Canvas(current).drawBitmap(portrait, offset, 0f, null)
                        val sideLabel = if (leftEdge) "left" else "right"
                        Log.i("EdgeZoomFaceProbe", "$sideLabel-visible-$fraction fullAccepted=${accepted.detect(current)}")
                        // The hint supplies vertical position/size, not missing
                        // pixels or a cached-positive result. The crop below is
                        // taken exclusively from this already clipped image.
                        for (scale in floatArrayOf(1.0f, 1.5f, 2.0f)) {
                            val size = (hint.height() * scale).toInt().coerceIn(1, minOf(current.width, current.height))
                            val cropLeft = if (leftEdge) 0 else current.width - size
                            val cropTop = (hint.centerY() - size / 2).coerceIn(0, current.height - size)
                            val crop = Bitmap.createBitmap(current, cropLeft, cropTop, size, size)
                            try {
                                val label = "$sideLabel-visible-$fraction scale=$scale crop=$cropLeft,$cropTop,$size"
                                Log.i("EdgeZoomFaceProbe", "$label acceptedInCrop=${accepted.detect(crop)}")
                                rawPass("$label-original", crop, cropLeft, cropTop, 0, 0)
                                val pad = maxOf(1, size / 4)
                                val padded = Bitmap.createBitmap(size + pad * 2, size + pad * 2, Bitmap.Config.ARGB_8888)
                                try {
                                    padded.eraseColor(Color.rgb(127, 127, 127))
                                    Canvas(padded).drawBitmap(crop, pad.toFloat(), pad.toFloat(), null)
                                    rawPass("$label-padded", padded, cropLeft, cropTop, pad, pad)
                                } finally { padded.recycle() }
                            } finally { if (crop !== current) crop.recycle() }
                        }
                    } finally { current.recycle() }
                }
            }
        } finally {
            raw.close(); accepted.close(); fast.close()
            if (portrait !== source) portrait.recycle()
            source.recycle()
        }
    }

    /** Diagnostic only: calibrates public fixtures without relaxing negative tests. */
    @Test fun characterizeRawFaceScores() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = instrumentation.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }
        val portrait = Bitmap.createScaledBitmap(source, 480, 640, true)
        val fist = instrumentation.context.assets.open("fist.jpg").use { BitmapFactory.decodeStream(it) }
        val fast = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
        val raw = MediaPipeFaceDetector.createFromOptions(instrumentation.targetContext,
            MediaPipeFaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder()
                    .setModelAssetPath("blaze_face_short_range.tflite").setDelegate(Delegate.CPU).build())
                .setRunningMode(RunningMode.IMAGE).setMinDetectionConfidence(.10f).build())
        fun decimal(value: Float) = String.format(Locale.US, "%.4f", value)
        fun rawPass(label: String, frame: Bitmap, offsetX: Int, offsetY: Int, realWidth: Int, realHeight: Int) {
            val owned = checkNotNull(frame.copy(Bitmap.Config.ARGB_8888, false))
            val image = BitmapImageBuilder(owned).build()
            try {
                val detections = raw.detect(image).detections()
                val scores = detections.map { detection -> detection.categories().maxOfOrNull { it.score() } ?: -1f }
                val thresholds = floatArrayOf(.25f, .30f, .35f, .40f, .50f).joinToString(" ") { threshold ->
                    "${decimal(threshold)}:${scores.count { it >= threshold }}"
                }
                Log.i("RawFaceScoreProbe", "$label input=${frame.width}x${frame.height} real=${realWidth}x$realHeight offset=$offsetX,$offsetY count=${detections.size} thresholds=$thresholds")
                for ((index, detection) in detections.withIndex()) {
                    val box = detection.boundingBox()
                    val boxInSource = "${decimal(box.left - offsetX)},${decimal(box.top - offsetY)},${decimal(box.right - offsetX)},${decimal(box.bottom - offsetY)}"
                    val points = detection.keypoints().orElse(emptyList()).mapIndexed { pointIndex, point ->
                        // Source-space pixel positions make original/padded
                        // candidates comparable without storing any image.
                        "$pointIndex:(${decimal(point.x() * frame.width - offsetX)},${decimal(point.y() * frame.height - offsetY)})"
                    }.joinToString(" ")
                    Log.i("RawFaceScoreProbe", "$label candidate=$index score=${decimal(scores[index])} sourceBox=($boxInSource) keypoints=$points")
                }
            } finally {
                image.close()
                if (!owned.isRecycled) owned.recycle()
            }
        }
        fun rawProbe(label: String, bitmap: Bitmap) {
            rawPass("$label-original", bitmap, 0, 0, bitmap.width, bitmap.height)
            val padX = maxOf(1, bitmap.width / 4)
            val padY = maxOf(1, bitmap.height / 4)
            val padded = Bitmap.createBitmap(bitmap.width + padX * 2, bitmap.height + padY * 2, Bitmap.Config.ARGB_8888)
            try {
                padded.eraseColor(Color.rgb(127, 127, 127))
                Canvas(padded).drawBitmap(bitmap, padX.toFloat(), padY.toFloat(), null)
                rawPass("$label-padded", padded, padX, padY, bitmap.width, bitmap.height)
            } finally { padded.recycle() }
        }
        try {
            rawProbe("normal-portrait", portrait)
            rawProbe("negative-closed-fist", fist)
            val box = Tasks.await(fast.process(InputImage.fromBitmap(portrait, 0))).first().boundingBox
            for (fraction in floatArrayOf(.50f, .35f, .25f)) {
                for (leftEdge in listOf(true, false)) {
                    val frame = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
                    try {
                        frame.eraseColor(Color.GRAY)
                        val offset = if (leftEdge) -(box.right - box.width() * fraction)
                            else 480f - box.left - box.width() * fraction
                        Canvas(frame).drawBitmap(portrait, offset, 0f, null)
                        rawProbe("${if (leftEdge) "left" else "right"}-visible-$fraction", frame)
                    } finally { frame.recycle() }
                }
            }
            for (fraction in floatArrayOf(.60f, .40f)) {
                val upper = portrait.copy(Bitmap.Config.ARGB_8888, true)
                try {
                    Canvas(upper).drawRect(box.left.toFloat(), box.top + box.height() * fraction,
                        box.right.toFloat(), box.bottom.toFloat(), Paint().apply { color = Color.LTGRAY })
                    rawProbe("synthetic-upper-visible-$fraction", upper)
                } finally { upper.recycle() }
            }
        } finally {
            raw.close(); fast.close(); fist.recycle()
            if (portrait !== source) portrait.recycle()
            source.recycle()
        }
    }

    @Test fun configuredModelsDetectPortraitAndRejectBlankScenes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = instrumentation.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }
        val portrait = Bitmap.createScaledBitmap(source, 480, 640, true)
        val fast = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
        val accurate = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE))
        val partial = PartialFaceDetector(instrumentation.targetContext)
        val blank = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
        try {
            assertTrue(Tasks.await(fast.process(InputImage.fromBitmap(portrait, 0))).isNotEmpty())
            assertTrue(Tasks.await(accurate.process(InputImage.fromBitmap(portrait, 0))).isNotEmpty())
            assertTrue("The permissive model must detect the normal portrait", partial.detect(portrait).isNotEmpty())
            for (color in intArrayOf(Color.BLACK, Color.GRAY, Color.WHITE)) {
                blank.eraseColor(color)
                assertTrue(Tasks.await(fast.process(InputImage.fromBitmap(blank, 0))).isEmpty())
                assertTrue(Tasks.await(accurate.process(InputImage.fromBitmap(blank, 0))).isEmpty())
                // Run after a real positive: a prior face must not be replayed
                // when the current camera frame no longer contains a face.
                assertTrue("Blank color $color must not reuse the portrait", partial.detect(blank).isEmpty())
            }
        } finally {
            fast.close(); accurate.close(); partial.close(); blank.recycle()
            if (portrait !== source) portrait.recycle()
            source.recycle()
        }
    }

    @Test fun characterizePartialFaceAndSyntheticMaskLimits() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = instrumentation.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }
        val portrait = Bitmap.createScaledBitmap(source, 480, 640, true)
        val baseline = FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).enableTracking().build())
        val fast = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
        val accurate = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE))
        val partial = PartialFaceDetector(instrumentation.targetContext)
        fun probe(label: String, frame: Bitmap) {
            try {
                val before = Tasks.await(baseline.process(InputImage.fromBitmap(frame, 0))).isNotEmpty()
                val quick = Tasks.await(fast.process(InputImage.fromBitmap(frame, 0))).isNotEmpty()
                val rescue = Tasks.await(accurate.process(InputImage.fromBitmap(frame, 0))).isNotEmpty()
                val partialBoxes = partial.detect(frame)
                for (box in partialBoxes) {
                    assertTrue("A padded detection must overlap actual pixels: $label $box",
                        box.width() > 0 && box.height() > 0 && Rect.intersects(box, Rect(0, 0, frame.width, frame.height)))
                }
                Log.i("FaceVisibilityProbe", "$label baseline=$before tunedFast=$quick accurate=$rescue partial=${partialBoxes.isNotEmpty()}")
            } finally { frame.recycle() }
        }
        try {
            val box = Tasks.await(fast.process(InputImage.fromBitmap(portrait, 0))).first().boundingBox
            // Translate a real face beyond the image edge, without inventing
            // missing features via mirroring or replaying a past positive.
            for (fraction in floatArrayOf(.85f, .70f, .60f, .50f, .35f, .25f)) {
                val left = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
                left.eraseColor(Color.GRAY)
                Canvas(left).drawBitmap(portrait, -(box.right - box.width() * fraction), 0f, null)
                probe("left-visible-$fraction", left)
                val right = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
                right.eraseColor(Color.GRAY)
                Canvas(right).drawBitmap(portrait, 480f - box.left - box.width() * fraction, 0f, null)
                probe("right-visible-$fraction", right)
            }
            for (fraction in floatArrayOf(.70f, .60f, .50f, .40f)) {
                val occluded = portrait.copy(Bitmap.Config.ARGB_8888, true)
                Canvas(occluded).drawRect(box.left.toFloat(), box.top + box.height() * fraction,
                    box.right.toFloat(), box.bottom.toFloat(), Paint().apply { color = Color.LTGRAY })
                probe("synthetic-upper-visible-$fraction", occluded)
            }
            // This is a measured characterization, not an assertion that every
            // half-face or a real mask MUST be detected by this trained model.
        } finally {
            baseline.close(); fast.close(); accurate.close(); partial.close()
            if (portrait !== source) portrait.recycle()
            source.recycle()
        }
    }

    @Test fun permissiveFaceModelRejectsHandAndTextureHardNegatives() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val partial = PartialFaceDetector(instrumentation.targetContext)
        val fast = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
        val hands = instrumentation.context.assets.open("right_hands.jpg").use { BitmapFactory.decodeStream(it) }
        val fist = instrumentation.context.assets.open("fist.jpg").use { BitmapFactory.decodeStream(it) }
        fun assertNoFace(label: String, bitmap: Bitmap) {
            val mlKitFace = Tasks.await(fast.process(InputImage.fromBitmap(bitmap, 0))).isNotEmpty()
            val boxes = partial.detect(bitmap)
            Log.i("FaceVisibilityProbe", "negative-$label tunedFast=$mlKitFace partial=${boxes.isNotEmpty()} boxes=$boxes")
            assertTrue("A hand or non-face texture must not keep the timer running: $label $boxes", boxes.isEmpty())
        }
        try {
            assertNoFace("open-hands", hands)
            assertNoFace("closed-fist", fist)
            val noise = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
            try {
                for (seed in intArrayOf(7, 19, 41)) {
                    val random = Random(seed)
                    val pixels = IntArray(noise.width * noise.height) {
                        val gray = random.nextInt(256)
                        Color.rgb(gray, gray, gray)
                    }
                    noise.setPixels(pixels, 0, noise.width, 0, 0, noise.width, noise.height)
                    assertNoFace("deterministic-noise-$seed", noise)
                }
                // Broad high-contrast shapes are harder negatives than a blank
                // image; no synthetic face or personal camera image is used.
                val checker = IntArray(noise.width * noise.height) { index ->
                    if ((index % noise.width / 40 + index / noise.width / 40) % 2 == 0) Color.LTGRAY else Color.DKGRAY
                }
                noise.setPixels(checker, 0, noise.width, 0, 0, noise.width, noise.height)
                assertNoFace("checker-texture", noise)
            } finally { noise.recycle() }
        } finally {
            partial.close(); fast.close(); hands.recycle(); fist.recycle()
        }
    }
}
