package com.sm.myapplication.vision

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.media.ImageWriter
import android.os.SystemClock
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.Closeable
import java.lang.reflect.Proxy

/** Isolated diagnostic APK only: public fixtures, no webcam and no user app data. */
class LowLightPerformanceInstrumentedTest {
    @Test fun nativeConversionMatchesJpegShadowsAndColorAndMeasuresLatency() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val analyzer = FrameAnalyzer(context, null)
        val convert = FrameAnalyzer::class.java.getDeclaredMethod("imageProxyToBitmap",
            ImageProxy::class.java, Boolean::class.javaPrimitiveType).apply { isAccessible = true }
        try {
            for ((width, height, rotation) in listOf(Triple(640, 480, 0), Triple(1280, 960, 90))) {
                YuvFixture(width, height, rotation).use { fixture ->
                    fixture.fillTiles()
                    val positions = fixture.image.planes.map { it.buffer.position() }
                    val legacy = convert.invoke(analyzer, fixture.proxy, false) as Bitmap
                    val direct = convert.invoke(analyzer, fixture.proxy, true) as Bitmap
                    try {
                        assertEquals(legacy.width, direct.width)
                        assertEquals(legacy.height, direct.height)
                        var maxDelta = 0
                        // Sample tile interiors, not JPEG ringing at color boundaries.
                        for (y in 16 until legacy.height step 32) for (x in 16 until legacy.width step 32) {
                            val a = legacy.getPixel(x, y); val b = direct.getPixel(x, y)
                            for (shift in intArrayOf(0, 8, 16)) {
                                maxDelta = maxOf(maxDelta, kotlin.math.abs((a ushr shift and 255) - (b ushr shift and 255)))
                            }
                        }
                        assertTrue("JPEG-compatible color and shadow range, delta=$maxDelta", maxDelta <= 4)
                        println("LL_CONVERT_PARITY ${width}x${height} rotation=$rotation maxChannelDelta=$maxDelta")
                    } finally { legacy.recycle(); direct.recycle() }
                    val before = LongArray(16); val after = LongArray(16)
                    for (i in -5 until 16) {
                        val order = if (i % 2 == 0) listOf(false, true) else listOf(true, false)
                        for (isDirect in order) {
                            val start = SystemClock.elapsedRealtimeNanos()
                            val bitmap = convert.invoke(analyzer, fixture.proxy, isDirect) as Bitmap
                            val elapsed = SystemClock.elapsedRealtimeNanos() - start
                            bitmap.recycle()
                            if (i >= 0) { if (isDirect) after[i] = elapsed else before[i] = elapsed }
                        }
                    }
                    before.sort(); after.sort()
                    assertEquals("Conversion must not consume camera plane positions", positions,
                        fixture.image.planes.map { it.buffer.position() })
                    println("LL_CONVERT_BENCH ${width}x${height} rotation=$rotation jpegMedianMs=${before[8]/1e6}" +
                        " onMedianMs=${after[8]/1e6} jpegP95Ms=${before[15]/1e6} onP95Ms=${after[15]/1e6} onRoute=jpeg")
                }
            }
        } finally { analyzer.shutdown() }
    }

    @Test fun nativeEnhancementKeepsTheWorkingCurveAndMeasuresLatency() {
        val width = 640; val height = 480
        val original = IntArray(width * height) {
            val v = 35 + it * 37 % 80
            0xff000000.toInt() or (v * 0x010101)
        }
        val pixels = original.clone()
        val expected = original.clone()
        val reference = LegacyLowLightEnhancerReference()
        val optimized = LowLightEnhancer()
        assertEquals(reference.enhance(expected, width, height, 80, 48, 560, 432),
            optimized.enhance(pixels, width, height, 80, 48, 560, 432))
        for (i in pixels.indices) for (shift in intArrayOf(0, 8, 16)) {
            assertTrue(kotlin.math.abs((expected[i] ushr shift and 255) - (pixels[i] ushr shift and 255)) <= 1)
        }
        val before = LongArray(16); val after = LongArray(16)
        for (i in -5 until 16) for (isOptimized in if (i % 2 == 0) listOf(false, true) else listOf(true, false)) {
            original.copyInto(pixels)
            val start = SystemClock.elapsedRealtimeNanos()
            if (isOptimized) optimized.enhance(pixels, width, height, 80, 48, 560, 432)
            else reference.enhance(pixels, width, height, 80, 48, 560, 432)
            val elapsed = SystemClock.elapsedRealtimeNanos() - start
            if (i >= 0) { if (isOptimized) after[i] = elapsed else before[i] = elapsed }
        }
        before.sort(); after.sort()
        println("LL_ENHANCE_BENCH 640x480 legacyMedianMs=${before[8]/1e6} optimizedMedianMs=${after[8]/1e6}" +
            " legacyP95Ms=${before[15]/1e6} optimizedP95Ms=${after[15]/1e6}")
    }

    @Test fun warmedRescueRetainsOriginalAndRunsFallbackWithoutStaleFace() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val original = instrumentation.context.assets.open("portrait.jpg").use { BitmapFactory.decodeStream(it) }
        val portrait = Bitmap.createScaledBitmap(original, 480, 640, true)
        if (portrait !== original) original.recycle()
        val source = IntArray(480 * 640)
        portrait.getPixels(source, 0, 480, 0, 0, 480, 640)
        val dim = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
        // A test-APK context has no Application on some runners. Prefix the
        // diagnostic APK's prefs instead; never touch the normal app package.
        val settingsContext = object : ContextWrapper(instrumentation.targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                instrumentation.targetContext.getSharedPreferences("low_light_performance_test_$name", mode)
        }
        val settings = PureModeUserSettings.saveGestureRecognitionEnabled(settingsContext, false)
        var result: DetectionResult? = null
        val analyzer = FrameAnalyzer(instrumentation.targetContext) { result = it }
        analyzer.setPureModeSettings(settings)
        var nextFrameAt = 0L
        fun analyze(fixture: YuvFixture): DetectionResult {
            val remaining = nextFrameAt - SystemClock.elapsedRealtime()
            if (remaining > 0) Thread.sleep(remaining)
            result = null
            fixture.proxyClosed = false
            val captured = SystemClock.elapsedRealtime()
            analyzer.analyze(fixture.proxy)
            nextFrameAt = captured + settings.faceAnalyzeIntervalMs + 30L
            assertTrue("Camera input must close after model completion", fixture.proxyClosed)
            val detection = checkNotNull(result)
            assertTrue(detection.faceAnalyzed)
            assertTrue("Use real production timestamps", detection.observationElapsedMs >= captured)
            return detection
        }
        try {
            YuvFixture(480, 640, 0).use { fixture ->
                fixture.fillBitmap(portrait)
                assertTrue("Bright baseline", analyze(fixture).faceDetected)
                analyzer.setLowLightModeEnabled(true)
                val deadline = SystemClock.elapsedRealtime() + 15_000L
                while (!analyzer.lowLightDiagnostics.contains("lowLightReady=true") && SystemClock.elapsedRealtime() < deadline) Thread.sleep(20L)
                assertTrue("ACCURATE preparation must complete outside camera analysis", analyzer.lowLightDiagnostics.contains("lowLightReady=true"))
                val bright = analyze(fixture)
                assertTrue(bright.faceDetected); assertFalse(bright.lowLightApplied)
                assertTrue(analyzer.lowLightDiagnostics.contains("software=original-face"))
                var rescuedFactor: Double? = null
                var rescueAttempted = false
                val candidates = listOf(0.04 to 0, 0.06 to 0, 0.08 to 4,
                    0.12 to 4, 0.16 to 4, 0.08 to 8, 0.12 to 8, 0.16 to 8)
                for ((factor, noise) in candidates) {
                    val random = java.util.Random(414L)
                    val pixels = IntArray(source.size) { i ->
                        val p = source[i]
                        val n = if (noise == 0) 0 else random.nextInt(noise * 2 + 1) - noise
                        (0xff shl 24) or (((p ushr 16 and 255) * factor + n).toInt().coerceIn(0, 255) shl 16) or
                            (((p ushr 8 and 255) * factor + n).toInt().coerceIn(0, 255) shl 8) or
                            ((p and 255) * factor + n).toInt().coerceIn(0, 255)
                    }
                    dim.setPixels(pixels, 0, 480, 0, 0, 480, 640)
                    fixture.fillBitmap(dim)
                    analyzer.setLowLightModeEnabled(false)
                    val off = analyze(fixture)
                    analyzer.setLowLightModeEnabled(true)
                    Thread.sleep(2_100L) // Accurate completion cooldown; never alter user settings.
                    val on = analyze(fixture)
                    val ageMs = SystemClock.elapsedRealtime() - on.observationElapsedMs
                    rescueAttempted = rescueAttempted || analyzer.lowLightDiagnostics.contains("rescuePath=enhanced-")
                    println("LL_RESCUE factor=$factor noise=$noise off=${off.faceDetected} on=${on.faceDetected}" +
                        " applied=${on.lowLightApplied} ageMs=$ageMs diagnostics=${analyzer.lowLightDiagnostics}")
                    if (!off.faceDetected && on.faceDetected && on.lowLightApplied) {
                        assertTrue("A rescued face must still be usable by production freshness rules",
                            ageMs < settings.faceAwayTimeoutMs)
                        assertTrue(analyzer.lowLightDiagnostics.contains("rescuePath=enhanced-"))
                        rescuedFactor = factor
                        analyzer.setLowLightModeEnabled(false)
                        assertFalse("OFF again must not reuse a rescued/cached positive", analyze(fixture).faceDetected)
                        assertTrue(analyzer.lowLightDiagnostics.contains("rescuePath=off"))
                        break
                    }
                }
                dim.eraseColor(0xff000000.toInt()); fixture.fillBitmap(dim)
                analyzer.setLowLightModeEnabled(true)
                assertFalse("Black scene must never inherit a positive result", analyze(fixture).faceDetected)
                assertTrue(analyzer.lowLightDiagnostics.contains("insufficient-detail"))
                println("LL_RESCUE_SELECTED factor=$rescuedFactor")
                // Noise can satisfy the tone-meter contrast guard by itself.
                // It must still never create a detected face or cached positive.
                val random = java.util.Random(414L)
                val noisePixels = IntArray(source.size) {
                    val value = 12 + random.nextInt(17)
                    0xff000000.toInt() or (value * 0x010101)
                }
                dim.setPixels(noisePixels, 0, 480, 0, 0, 480, 640)
                fixture.fillBitmap(dim)
                assertFalse("Uniform dark grain is not a face", analyze(fixture).faceDetected)
                rescueAttempted = rescueAttempted || analyzer.lowLightDiagnostics.contains("rescuePath=enhanced-")
                // Discovery is not a guaranteed accuracy regression: many dim
                // portraits already work in the original model. A textured
                // negative also reliably exercises the fallback without faces.
                assertTrue("An original miss must exercise the enhanced fallback", rescueAttempted)
            }
        } finally { analyzer.shutdown(); dim.recycle(); portrait.recycle() }
    }

    private class YuvFixture(width: Int, height: Int, rotation: Int) : Closeable {
        private val reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
        private val writer = ImageWriter.newInstance(reader.surface, 2)
        val image: Image = writer.dequeueInputImage()
        var proxyClosed = false
        private val info = interfaceProxy<ImageInfo> { name ->
            when (name) { "getRotationDegrees" -> rotation; "getTimestamp" -> 1L; else -> null }
        }
        val proxy = interfaceProxy<ImageProxy> { name ->
            when (name) {
                "getWidth" -> image.width; "getHeight" -> image.height; "getFormat" -> image.format
                "getImage" -> image; "getImageInfo" -> info
                "getPlanes" -> image.planes.map { plane -> interfaceProxy<ImageProxy.PlaneProxy> { method ->
                    when (method) { "getBuffer" -> plane.buffer.duplicate(); "getPixelStride" -> plane.pixelStride
                        "getRowStride" -> plane.rowStride; else -> null }
                } }.toTypedArray()
                "close" -> { proxyClosed = true; null }; else -> null
            }
        }
        private fun put(planeIndex: Int, x: Int, y: Int, value: Double) {
            val plane = image.planes[planeIndex]
            plane.buffer.put(y * plane.rowStride + x * plane.pixelStride, value.toInt().coerceIn(0, 255).toByte())
        }
        fun fillTiles() {
            val ys = intArrayOf(16, 40, 96, 180)
            for (y in 0 until image.height) for (x in 0 until image.width) {
                val tile = (x / 64 + y / 64) % 4
                put(0, x, y, ys[tile].toDouble())
                if (x % 2 == 0 && y % 2 == 0) {
                    put(1, x/2, y/2, if (tile == 2) 160.0 else 128.0)
                    put(2, x/2, y/2, if (tile == 2) 104.0 else 128.0)
                }
            }
        }
        fun fillBitmap(bitmap: Bitmap) {
            val pixels = IntArray(image.width * image.height)
            bitmap.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
            for (y in 0 until image.height) for (x in 0 until image.width) {
                val p = pixels[y * image.width + x]
                val r = p ushr 16 and 255; val g = p ushr 8 and 255; val b = p and 255
                put(0, x, y, 16 + 0.257*r + 0.504*g + 0.098*b)
                if (x % 2 == 0 && y % 2 == 0) {
                    put(1, x/2, y/2, 128 - 0.148*r - 0.291*g + 0.439*b)
                    put(2, x/2, y/2, 128 + 0.439*r - 0.368*g - 0.071*b)
                }
            }
        }
        override fun close() { image.close(); writer.close(); reader.close() }
    }

    companion object {
        private inline fun <reified T> interfaceProxy(crossinline get: (String) -> Any?): T =
            Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ -> get(method.name) } as T
    }
}
