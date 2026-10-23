package com.sm.myapplication.vision

import org.junit.Assert.*
import org.junit.Test

/** Pixel-only regression tests; these do not claim device face-detection accuracy. */
class LowLightEnhancerTest {
    private fun gray(value: Int) = 0xff000000.toInt() or (value * 0x010101)
    private fun frame(value: Int) = IntArray(64 * 48) { gray(value) }
    private fun detailedFrame(value: Int) = IntArray(64 * 48) { gray(value + if ((it / 8) % 2 == 0) -5 else 5) }
    private fun apply(enhancer: LowLightEnhancer, pixels: IntArray) =
        enhancer.enhance(pixels, 64, 48, 12, 8, 52, 40)

    @Test
    fun initiallyUndetectedDarkSubjectIsNotMaskedByBrightBackground() {
        val pixels = frame(180)
        for (y in 16 until 32) for (x in 22 until 42) pixels[y * 64 + x] = gray(40)
        val enhancer = LowLightEnhancer()
        assertTrue(apply(enhancer, pixels))
        assertTrue((pixels[24 * 64 + 32] and 255) >= 100)
        assertEquals(gray(180), pixels[0])
        assertTrue(enhancer.measuredLuma < 50)
    }

    @Test
    fun readableDimStudyLightGetsMeaningfulSoftwareGain() {
        val pixels = detailedFrame(100)
        assertTrue(apply(LowLightEnhancer(), pixels))
        assertTrue((pixels[100] and 255) >= 125)
    }

    @Test
    fun brightSceneAndIsolatedShadowsAreNotWashedOut() {
        val pixels = frame(180)
        pixels[12 * 64 + 20] = gray(40)
        val original = pixels.clone()
        assertFalse(apply(LowLightEnhancer(), pixels))
        assertArrayEquals(original, pixels)
    }

    @Test
    fun nearBlackDoesNotInventDetailOrReportFakeEnhancement() {
        for (value in listOf(0, 4)) {
            val pixels = frame(value)
            val original = pixels.clone()
            val enhancer = LowLightEnhancer()
            assertFalse(apply(enhancer, pixels))
            assertFalse(enhancer.hasUsableDetail())
            assertArrayEquals(original, pixels)
        }
    }

    @Test
    fun toneCurvePreservesContrastOrderAndHighlights() {
        val pixels = IntArray(256) { gray(it) }
        val enhancer = LowLightEnhancer()
        assertTrue(enhancer.enhance(pixels, 256, 1, 0, 0, 80, 1))
        for (i in 1 until 256) {
            assertTrue((pixels[i] and 255) >= (pixels[i - 1] and 255))
            assertTrue((pixels[i] and 255) >= i)
        }
        for (i in 176 until 256) assertEquals(gray(i), pixels[i])
        assertEquals(gray(0), pixels[0])
    }

    @Test
    fun brightTransitionAndResetDoNotKeepOldShadowGain() {
        val enhancer = LowLightEnhancer()
        assertTrue(apply(enhancer, detailedFrame(40)))
        val bright = frame(180)
        val original = bright.clone()
        assertFalse(apply(enhancer, bright))
        assertArrayEquals(original, bright)
        enhancer.reset()
        assertEquals(1.0, enhancer.gamma, 0.0)
        assertFalse(enhancer.hasUsableDetail())
    }

    @Test
    fun yuvBlackEncodedAsUniformGrayDoesNotReportFakeGain() {
        for (value in listOf(16, 25, 100)) {
            val pixels = frame(value)
            val original = pixels.clone()
            val enhancer = LowLightEnhancer()
            assertFalse(apply(enhancer, pixels))
            assertFalse(enhancer.hasUsableDetail())
            assertEquals(0, enhancer.measuredContrast)
            assertArrayEquals(original, pixels)
        }
    }
}
