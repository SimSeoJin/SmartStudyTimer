package com.sm.myapplication.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.junit.Assert.*
import org.junit.Test

/** Public fixtures only: old search geometry is never accepted as current presence. */
class FaceSearchHintInstrumentedTest {
    @Test fun longAbsencePreservesSearchButOnlyCurrentFacePixelsMayMatch() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = instrumentation.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }
        val portrait = Bitmap.createScaledBitmap(source, 480, 640, true)
        val fist = instrumentation.context.assets.open("fist.jpg").use { BitmapFactory.decodeStream(it) }
        val fast = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
        val analyzer = FrameAnalyzer(instrumentation.targetContext) { }
        val partial = PartialFaceDetector(instrumentation.targetContext)
        try {
            val detectedBox = Rect(Tasks.await(fast.process(InputImage.fromBitmap(portrait, 0))).first().boundingBox)
            // Seed real detected geometry directly, so elapsed-time policy is
            // deterministic without sleeping or changing user preferences.
            val capturedAt = 1_000L
            assertNull("No prior face means no cached search geometry", currentHint(analyzer, 480, 640, capturedAt))
            seedHint(analyzer, detectedBox, 480, 640, capturedAt)
            val staleBox = Rect(detectedBox).apply { offset(90, 40) }
            seedHint(analyzer, staleBox, 480, 640, capturedAt - 1)
            assertEquals("An older rescue must not overwrite a newer FAST face hint", detectedBox,
                currentHint(analyzer, 480, 640, capturedAt))
            assertNull("Pre-observation frames must not use future search geometry", currentHint(analyzer, 480, 640, capturedAt - 1))
            val afterSixSeconds = currentHint(analyzer, 480, 640, capturedAt + 6_000L)
            assertNotNull("A five-second absence must not disable edge-face search", afterSixSeconds)
            assertEquals(detectedBox, afterSixSeconds)
            val afterMinute = currentHint(analyzer, 480, 640, capturedAt + 60_000L)
            assertNotNull("A longer break must not recreate the same search expiry", afterMinute)
            assertEquals(detectedBox, afterMinute)
            assertEquals("Search geometry must follow current frame dimensions",
                Rect(detectedBox.left / 2, detectedBox.top / 2, detectedBox.right / 2, detectedBox.bottom / 2),
                currentHint(analyzer, 240, 320, capturedAt + 60_000L))
            seedHint(analyzer, detectedBox, 0, 640, capturedAt)
            assertNull("Invalid stored width cannot produce search geometry", currentHint(analyzer, 480, 640, capturedAt + 60_000L))
            seedHint(analyzer, detectedBox, 480, 0, capturedAt)
            assertNull("Invalid stored height cannot produce search geometry", currentHint(analyzer, 480, 640, capturedAt + 60_000L))
            seedHint(analyzer, detectedBox, 480, 640, capturedAt)
            val hint = checkNotNull(currentHint(analyzer, 480, 640, capturedAt + 60_000L))

            val failures = mutableListOf<String>()
            fun checkCurrent(label: String, bitmap: Bitmap, expected: Boolean) {
                val boxes = partial.detect(bitmap, hint)
                Log.i("LongAbsenceHintProbe", "$label expected=$expected boxes=$boxes")
                if (boxes.isNotEmpty() != expected) failures.add("$label expected=$expected boxes=$boxes")
            }
            val current = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
            try {
                for (leftEdge in listOf(true, false)) {
                    current.eraseColor(Color.GRAY)
                    val fraction = .35f
                    val offset = if (leftEdge) -(hint.right - hint.width() * fraction)
                        else current.width - hint.left - hint.width() * fraction
                    Canvas(current).drawBitmap(portrait, offset, 0f, null)
                    // These are the identical pixels twice: movement must not
                    // be required to find a face after losing the old track.
                    repeat(2) { attempt ->
                        checkCurrent("${if (leftEdge) "left" else "right"}-stationary-35-percent-$attempt", current, true)
                    }
                }
                for (color in intArrayOf(Color.BLACK, Color.GRAY)) {
                    current.eraseColor(color)
                    checkCurrent("blank-$color-after-face", current, false)
                }
                val handHeight = maxOf(1, (hint.height() * 1.5f).toInt())
                val hand = Bitmap.createScaledBitmap(fist, maxOf(1, fist.width * handHeight / fist.height), handHeight, true)
                try {
                    for (location in listOf("center", "left-edge", "right-edge")) {
                        current.eraseColor(Color.GRAY)
                        val x = when (location) {
                            "left-edge" -> -hand.width / 2f
                            "right-edge" -> current.width - hand.width / 2f
                            else -> hint.centerX() - hand.width / 2f
                        }
                        Canvas(current).drawBitmap(hand, x, hint.centerY() - hand.height / 2f, null)
                        checkCurrent("fist-$location-after-face", current, false)
                    }
                } finally { if (hand !== fist) hand.recycle() }
            } finally { current.recycle() }
            assertTrue("Long-absence current-pixel mismatches: ${failures.joinToString("; ")}", failures.isEmpty())
        } finally {
            partial.close()
            analyzer.shutdown()
            fast.close()
            fist.recycle()
            if (portrait !== source) portrait.recycle()
            source.recycle()
        }
    }

    private fun seedHint(analyzer: FrameAnalyzer, box: Rect, width: Int, height: Int, capturedAt: Long) {
        val method = FrameAnalyzer::class.java.getDeclaredMethod("rememberFaceSearchHint",
            Rect::class.java, Integer.TYPE, Integer.TYPE, java.lang.Long.TYPE)
        method.isAccessible = true
        method.invoke(analyzer, box, width, height, capturedAt)
    }

    private fun currentHint(analyzer: FrameAnalyzer, width: Int, height: Int, capturedAt: Long): Rect? {
        val method = FrameAnalyzer::class.java.getDeclaredMethod("currentSearchHint", Integer.TYPE, Integer.TYPE, java.lang.Long.TYPE)
        method.isAccessible = true
        return method.invoke(analyzer, width, height, capturedAt) as Rect?
    }
}
