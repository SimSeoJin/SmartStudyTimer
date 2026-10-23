package com.sm.myapplication.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.ui.screens.puremode.PureModeDetectionController
import com.sm.myapplication.ui.screens.timer.TimerController
import com.sm.myapplication.ui.screens.timer.TimerPauseCause
import org.junit.Assert.*
import org.junit.Test

/** Bundled MediaPipe CPU model + real public hand fixtures + filter + timer. */
class GestureModelInstrumentedTest {
    @Test fun croppedCrossedArmsMustNotInventPalmPause() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val helper = GestureRecognizerHelper()
        val source = instrumentation.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }
        val portrait = Bitmap.createScaledBitmap(source, 480, 640, true)
        val partial = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
        val faceDetector = FaceDetection.getClient(FrameAnalyzer.faceOptions(FaceDetectorOptions.PERFORMANCE_MODE_FAST))
        try {
            val face = Tasks.await(faceDetector.process(InputImage.fromBitmap(portrait, 0))).first().boundingBox
            partial.eraseColor(Color.GRAY)
            Canvas(partial).drawBitmap(portrait, -(face.right - face.width() * .50f), 0f, null)
            assertTrue(helper.lastErrorMessage, helper.init(instrumentation.targetContext))
            repeat(5) { index ->
                val result = checkNotNull(helper.recognize(partial, (index + 1) * 200L)) { helper.lastErrorMessage }
                val category = result.gestures().firstOrNull()?.firstOrNull()
                val fallback = GestureRecognizerHelper.handPoseFallbackLabel(result,
                    category?.categoryName() ?: "no_hand", category?.score() ?: -1f)
                assertNotEquals("A clipped resting hand must not become manual pause: " +
                    "model=$category image=${result.landmarks()} world=${result.worldLandmarks()}",
                    "Open_Palm", fallback)
            }
        } finally {
            faceDetector.close(); helper.close(); partial.recycle()
            if (portrait !== source) portrait.recycle()
            source.recycle()
        }
    }

    @Test fun actualWorldLandmarksCanRescueFistButNotPalm() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val helper = GestureRecognizerHelper()
        val hands = instrumentation.context.assets.open("right_hands.jpg").use { BitmapFactory.decodeStream(it) }
        val palm = Bitmap.createBitmap(hands, hands.width / 2, 0, hands.width / 2, hands.height)
        val fist = instrumentation.context.assets.open("fist.jpg").use { BitmapFactory.decodeStream(it) }
        var timestamp = 0L
        try {
            assertTrue(helper.lastErrorMessage, helper.init(instrumentation.targetContext))
            for ((name, bitmap, expected) in listOf(Triple("fist", fist, true), Triple("palm", palm, false))) {
                // Retain this helper-only geometry regression. Production uses
                // the restored model-only policy and does not call the fallback.
                timestamp += 200
                val result = checkNotNull(helper.recognize(bitmap, timestamp)) { helper.lastErrorMessage }
                assertFalse("Fixture must produce real world landmarks", result.worldLandmarks().isEmpty())
                assertEquals("World pose fallback for $name: ${result.worldLandmarks()}", expected,
                    GestureRecognizerHelper.hasClosedFistPose(result, "None", .9f))
                assertEquals("World command fallback for $name: world=${result.worldLandmarks()}",
                    if (expected) "Closed_Fist" else "Open_Palm",
                    // A weak palm classification can be rescued; a confident None
                    // must not be turned into an unintended manual stop.
                    GestureRecognizerHelper.handPoseFallbackLabel(result,
                        if (expected) "None" else "Open_Palm", if (expected) .9f else .19f))
                if (!expected) assertEquals("A clear no-command result is not a palm command", "",
                    GestureRecognizerHelper.handPoseFallbackLabel(result, "None", .9f))
                assertEquals("Clear model commands must remain authoritative", "",
                    GestureRecognizerHelper.handPoseFallbackLabel(result,
                        if (expected) "Open_Palm" else "Closed_Fist", .9f))
            }
            assertFalse("Explicit palm command must never become resume",
                GestureRecognizerHelper.hasClosedFistPose(helper.recognize(fist, timestamp + 200), "Open_Palm", .9f))
        } finally {
            helper.close(); fist.recycle()
            if (palm !== hands) palm.recycle()
            hands.recycle()
        }
    }

    @Test fun actualPalmPausesAndActualFistResumes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val helper = GestureRecognizerHelper()
        val filter = GestureEventFilter()
        val controller = PureModeDetectionController { 0L }
        val hands = instrumentation.context.assets.open("right_hands.jpg").use { BitmapFactory.decodeStream(it) }
        val palm = Bitmap.createBitmap(hands, hands.width / 2, 0, hands.width / 2, hands.height)
        val fist = instrumentation.context.assets.open("fist.jpg").use { BitmapFactory.decodeStream(it) }
        var timestamp = 0L
        fun confirm(bitmap: Bitmap, expected: String) {
            var emitted = false
            // The reference policy shares a 1.5-second cooldown between palm
            // and fist, rather than allowing an immediate opposite command.
            timestamp += 1_500L
            repeat(4) {
                timestamp += 200L
                val result = checkNotNull(helper.recognize(bitmap, timestamp)) { helper.lastErrorMessage }
                val category = result.gestures().firstOrNull()?.firstOrNull()
                val modelLabel = category?.categoryName() ?: "no_hand"
                val modelScore = category?.score() ?: -1f
                // Exercise exactly the production model-only classification:
                // score >= .55 on two independent matching observations.
                val event = filter.update(modelLabel, modelScore, timestamp)
                if (event == expected) {
                    emitted = true
                    instrumentation.runOnMainSync {
                        controller.handle(DetectionResult(false, GestureEventFilter.isResumeCommand(event), GestureEventFilter.isPauseCommand(event),
                            emptyList(), bitmap.width, bitmap.height, true, false, false, -1L))
                    }
                }
            }
            assertTrue("Actual model must confirm $expected", emitted)
        }
        try {
            assertTrue(helper.lastErrorMessage, helper.init(instrumentation.targetContext))
            instrumentation.runOnMainSync {
                TimerController.start(StudyMode.PURE, false)
                controller.handle(DetectionResult(true, false, false, emptyList(), 480, 640, true, false))
            }
            confirm(palm, "Open_Palm")
            assertEquals(TimerPauseCause.MANUAL_GESTURE, TimerController.state.value.pauseCause)
            instrumentation.runOnMainSync {
                controller.handle(DetectionResult(true, false, false, emptyList(), 480, 640, true, false))
            }
            assertFalse("Face alone must not undo a palm pause", TimerController.state.value.isRunning)
            confirm(fist, "Closed_Fist")
            assertTrue(TimerController.state.value.isRunning)
        } finally {
            helper.close()
            fist.recycle()
            if (palm !== hands) palm.recycle()
            hands.recycle()
            instrumentation.runOnMainSync { TimerController.start(StudyMode.BASIC, false) }
        }
    }
}
