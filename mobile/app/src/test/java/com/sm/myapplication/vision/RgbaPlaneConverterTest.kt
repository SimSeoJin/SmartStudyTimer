package com.sm.myapplication.vision

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class RgbaPlaneConverterTest {
    @Test fun preservesRgbaChannelOrderAndBufferPosition() {
        val buffer = ByteBuffer.wrap(byteArrayOf(9, 9, 255.toByte(), 0, 0, 255.toByte(), 0, 255.toByte(), 0, 255.toByte()))
        buffer.position(2)
        assertArrayEquals(intArrayOf(0xffff0000.toInt(), 0xff00ff00.toInt()),
            RgbaPlaneConverter.toArgb(buffer, 2, 1, 8, 4))
        assertEquals(2, buffer.position())
    }

    @Test fun finalRowDoesNotNeedTrailingPadding() {
        val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 255.toByte(), 9, 9, 9, 9, 4, 5, 6, 255.toByte()))
        assertArrayEquals(intArrayOf(0xff010203.toInt(), 0xff040506.toInt()),
            RgbaPlaneConverter.toArgb(buffer, 1, 2, 8, 4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTruncatedPixels() {
        RgbaPlaneConverter.toArgb(ByteBuffer.allocate(3), 1, 1, 4, 4)
    }
}
