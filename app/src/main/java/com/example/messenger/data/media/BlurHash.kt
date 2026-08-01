package com.example.messenger.data.media

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

object BlurHash {

    private const val CHARS =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#\$%*+,-.:;=?@[]^_{|}~"

    fun encode(bitmap: Bitmap, componentX: Int = 4, componentY: Int = 3): String {
        val compX = componentX.coerceIn(1, 9)
        val compY = componentY.coerceIn(1, 9)
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return ""

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val factors = Array(compX * compY) { FloatArray(3) }
        for (j in 0 until compY) {
            for (i in 0 until compX) {
                val normalisation = if (i == 0 && j == 0) 1f else 2f
                var r = 0f
                var g = 0f
                var b = 0f
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val basis = normalisation *
                            cos(Math.PI * i * x / width).toFloat() *
                            cos(Math.PI * j * y / height).toFloat()
                        val pixel = pixels[y * width + x]
                        r += basis * sRGBToLinear((pixel shr 16) and 0xFF)
                        g += basis * sRGBToLinear((pixel shr 8) and 0xFF)
                        b += basis * sRGBToLinear(pixel and 0xFF)
                    }
                }
                val scale = 1f / (width * height)
                val index = j * compX + i
                factors[index][0] = r * scale
                factors[index][1] = g * scale
                factors[index][2] = b * scale
            }
        }

        val dc = factors[0]
        val ac = if (factors.size > 1) factors.copyOfRange(1, factors.size) else emptyArray()

        val sb = StringBuilder()
        val sizeFlag = (compX - 1) + (compY - 1) * 9
        sb.append(encode83(sizeFlag, 1))

        val maximumValue: Float
        if (ac.isNotEmpty()) {
            val actualMax = ac.maxOf { comp -> comp.maxOf { abs(it) } }
            val quantisedMax = max(0, min(82, floor(actualMax * 166f - 0.5f).toInt()))
            maximumValue = (quantisedMax + 1) / 166f
            sb.append(encode83(quantisedMax, 1))
        } else {
            maximumValue = 1f
            sb.append(encode83(0, 1))
        }

        sb.append(encode83(encodeDC(dc), 4))
        for (factor in ac) {
            sb.append(encode83(encodeAC(factor, maximumValue), 2))
        }
        return sb.toString()
    }

    fun decode(blurHash: String?, width: Int, height: Int, punch: Float = 1f): Bitmap? {
        if (blurHash == null || blurHash.length < 6 || width <= 0 || height <= 0) return null
        val sizeFlag = decode83(blurHash, 0, 1)
        val numY = sizeFlag / 9 + 1
        val numX = sizeFlag % 9 + 1
        if (blurHash.length != 4 + 2 * numX * numY) return null

        val quantisedMaximumValue = decode83(blurHash, 1, 2)
        val maximumValue = (quantisedMaximumValue + 1) / 166f

        val colors = Array(numX * numY) { index ->
            if (index == 0) {
                decodeDC(decode83(blurHash, 2, 6))
            } else {
                decodeAC(decode83(blurHash, 4 + index * 2, 6 + index * 2), maximumValue * punch)
            }
        }

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f
                for (j in 0 until numY) {
                    for (i in 0 until numX) {
                        val basis = (cos(Math.PI * x * i / width) * cos(Math.PI * y * j / height)).toFloat()
                        val color = colors[j * numX + i]
                        r += color[0] * basis
                        g += color[1] * basis
                        b += color[2] * basis
                    }
                }
                pixels[y * width + x] = (0xFF shl 24) or
                    (linearToSRGB(r) shl 16) or
                    (linearToSRGB(g) shl 8) or
                    linearToSRGB(b)
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun encode83(value: Int, length: Int): String {
        val sb = StringBuilder()
        for (i in 1..length) {
            val divisor = Math.pow(83.0, (length - i).toDouble()).toInt()
            val digit = (value / divisor) % 83
            sb.append(CHARS[digit])
        }
        return sb.toString()
    }

    private fun decode83(str: String, from: Int, to: Int): Int {
        var value = 0
        for (i in from until to) {
            value = value * 83 + CHARS.indexOf(str[i])
        }
        return value
    }

    private fun encodeDC(color: FloatArray): Int {
        val r = linearToSRGB(color[0])
        val g = linearToSRGB(color[1])
        val b = linearToSRGB(color[2])
        return (r shl 16) + (g shl 8) + b
    }

    private fun decodeDC(value: Int): FloatArray {
        val r = value shr 16
        val g = (value shr 8) and 255
        val b = value and 255
        return floatArrayOf(sRGBToLinear(r), sRGBToLinear(g), sRGBToLinear(b))
    }

    private fun encodeAC(color: FloatArray, maximumValue: Float): Int {
        val r = quantise(color[0] / maximumValue)
        val g = quantise(color[1] / maximumValue)
        val b = quantise(color[2] / maximumValue)
        return r * 19 * 19 + g * 19 + b
    }

    private fun decodeAC(value: Int, maximumValue: Float): FloatArray {
        val quantR = value / (19 * 19)
        val quantG = (value / 19) % 19
        val quantB = value % 19
        return floatArrayOf(
            signPow((quantR - 9) / 9f, 2f) * maximumValue,
            signPow((quantG - 9) / 9f, 2f) * maximumValue,
            signPow((quantB - 9) / 9f, 2f) * maximumValue,
        )
    }

    private fun quantise(value: Float): Int =
        max(0, min(18, floor(signPow(value, 0.5f) * 9f + 9.5f).toInt()))

    private fun signPow(value: Float, exp: Float): Float = sign(value) * abs(value).pow(exp)

    private fun sRGBToLinear(value: Int): Float {
        val v = value / 255f
        return if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSRGB(value: Float): Int {
        val v = value.coerceIn(0f, 1f)
        return if (v <= 0.0031308f) {
            (v * 12.92f * 255f + 0.5f).toInt()
        } else {
            ((1.055f * v.pow(1f / 2.4f) - 0.055f) * 255f + 0.5f).toInt()
        }
    }
}
