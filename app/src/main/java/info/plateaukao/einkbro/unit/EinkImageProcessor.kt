package info.plateaukao.einkbro.unit

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Processes web images for better appearance on e-ink displays.
 *
 * Pipeline (ported from KOReader's coverimage eink-optimize):
 *   1. Gamma lift – brightens shadows and midtones
 *   2. Saturation boost – compensates for washed-out e-ink colors
 *   3. S-curve contrast – adds punch without blowing out highlights
 *   4. Floyd-Steinberg dithering – smoother gradients on limited palettes
 */
object EinkImageProcessor {

    private fun clamp(v: Int): Int = v.coerceIn(0, 255)

    private fun buildToneLUT(gamma: Double, k: Double, shadowLift: Double): IntArray {
        val lut = IntArray(256)
        val sig0: Double
        val sigRange: Double
        if (k > 0.01) {
            sig0 = 1.0 / (1.0 + exp(k * 0.5))
            sigRange = 1.0 / (1.0 + exp(-k * 0.5)) - sig0
        } else {
            sig0 = 0.0
            sigRange = 1.0
        }
        for (i in 0..255) {
            var v = Math.pow(i / 255.0, 1.0 / gamma) // gamma lift
            if (k > 0.01) { // S-curve contrast
                v = (1.0 / (1.0 + exp(-k * (v - 0.5))) - sig0) / sigRange
            }
            // Shadow lift: raise black point so dark tones don't collapse on e-ink
            if (shadowLift > 0.0) {
                v = shadowLift + v * (1.0 - shadowLift)
            }
            lut[i] = (v * 255.0 + 0.5).toInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Process a bitmap in-place with e-ink optimizations.
     * @param strength 0-100, where 0 = no change
     */
    fun process(bitmap: Bitmap, strength: Int): Bitmap {
        if (strength <= 0) return bitmap

        val t = strength / 100.0
        val gamma = 1.0 + t * 1.0       // 1.0 → 2.0
        val sat = 1.0 + t * 0.8         // 1.0 → 1.8
        val sCurveK = t * 5.0           // 0   → 5  (gentler to preserve dark detail)
        val shadowLift = t * 0.08       // 0   → 8% black-point raise

        val lut = buildToneLUT(gamma, sCurveK, shadowLift)

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // Saturation in Q12 fixed point; 1225 + 2404 + 467 = 4096 (Rec.601 luma)
        val satQ12 = (sat * 4096.0).roundToInt()
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = Color.alpha(pixel)
            if (a == 0) continue // skip fully transparent

            var r = lut[Color.red(pixel)]
            var g = lut[Color.green(pixel)]
            var b = lut[Color.blue(pixel)]

            if (satQ12 != 4096) {
                val luma = (r * 1225 + g * 2404 + b * 467) shr 12
                r = clamp(luma + ((satQ12 * (r - luma)) shr 12))
                g = clamp(luma + ((satQ12 * (g - luma)) shr 12))
                b = clamp(luma + ((satQ12 * (b - luma)) shr 12))
            }

            pixels[i] = Color.argb(a, r, g, b)
        }

        // 4. Floyd-Steinberg error-diffusion dithering
        applyDither(pixels, w, h, DITHER_LEVELS)

        val result = if (bitmap.isMutable) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private const val DITHER_LEVELS = 16 // ≈ Kaleido color depth

    /**
     * Floyd-Steinberg error-diffusion dithering in-place.
     * Quantises each channel to [levels] values.
     */
    private fun applyDither(pixels: IntArray, w: Int, h: Int, levels: Int) {
        val step = 255.0f / (levels - 1)
        // Error buffers for current and next row, per channel (offset by 1 so x-1 is safe)
        val sz = w + 2
        var ecR = FloatArray(sz); var ecG = FloatArray(sz); var ecB = FloatArray(sz)
        var enR = FloatArray(sz); var enG = FloatArray(sz); var enB = FloatArray(sz)

        for (y in 0 until h) {
            enR.fill(0f); enG.fill(0f); enB.fill(0f)
            for (x in 0 until w) {
                val i = y * w + x
                val pixel = pixels[i]
                val a = Color.alpha(pixel)
                if (a == 0) continue

                val xi = x + 1 // offset index into error buffers

                val r = Color.red(pixel) + ecR[xi]
                val g = Color.green(pixel) + ecG[xi]
                val b = Color.blue(pixel) + ecB[xi]

                // Quantise to nearest level
                val qr = clamp((Math.round(r / step) * step).roundToInt())
                val qg = clamp((Math.round(g / step) * step).roundToInt())
                val qb = clamp((Math.round(b / step) * step).roundToInt())

                pixels[i] = Color.argb(a, qr, qg, qb)

                val er = r - qr; val eg = g - qg; val eb = b - qb

                // Right: 7/16
                ecR[xi + 1] += er * 0.4375f; ecG[xi + 1] += eg * 0.4375f; ecB[xi + 1] += eb * 0.4375f
                // Below-left: 3/16
                enR[xi - 1] += er * 0.1875f; enG[xi - 1] += eg * 0.1875f; enB[xi - 1] += eb * 0.1875f
                // Below: 5/16
                enR[xi] += er * 0.3125f; enG[xi] += eg * 0.3125f; enB[xi] += eb * 0.3125f
                // Below-right: 1/16
                enR[xi + 1] += er * 0.0625f; enG[xi + 1] += eg * 0.0625f; enB[xi + 1] += eb * 0.0625f
            }
            // Swap current / next error rows
            val tmpR = ecR; val tmpG = ecG; val tmpB = ecB
            ecR = enR; ecG = enG; ecB = enB
            enR = tmpR; enG = tmpG; enB = tmpB
        }
    }

    // Icons and sprites below this edge aren't worth processing (and would
    // pollute the cache); they pass through untouched.
    private const val MIN_PROCESS_EDGE = 128

    // Decoded images are capped near the screen's long edge: the panel can't
    // show more pixels, and decode/process/encode all scale with the area.
    // inSampleSize halves in powers of two, so results land in [cap, 2*cap).
    private val maxProcessEdge: Int =
        maxOf(
            Resources.getSystem().displayMetrics.widthPixels,
            Resources.getSystem().displayMetrics.heightPixels,
        ).coerceAtLeast(1280)

    private fun computeSampleSize(srcMaxEdge: Int): Int {
        var sample = 1
        while (srcMaxEdge / (sample * 2) >= maxProcessEdge) sample *= 2
        return sample
    }

    /**
     * Rotate/flip per the EXIF orientation in [originalBytes]; the re-encoded
     * image carries no EXIF, so orientation must be baked into the pixels or
     * the browser shows camera photos rotated.
     */
    private fun applyExifOrientation(originalBytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = try {
            ExifInterface(ByteArrayInputStream(originalBytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: OutOfMemoryError) {
            return bitmap
        }
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    /**
     * Decode image bytes, apply e-ink processing, and re-encode.
     * Returns null when the image should pass through untouched
     * (strength off, too small, or undecodable).
     */
    fun processBytes(
        originalBytes: ByteArray,
        mimeType: String,
        strength: Int,
    ): ByteArray? {
        if (strength <= 0) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, bounds)
        val srcMaxEdge = maxOf(bounds.outWidth, bounds.outHeight)
        if (srcMaxEdge < MIN_PROCESS_EDGE) return null

        val opts = BitmapFactory.Options().apply {
            inMutable = true // process() adjusts in place, no defensive copy
            inSampleSize = computeSampleSize(srcMaxEdge)
        }
        val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, opts)
            ?: return null

        // Rotate after processing: per-pixel ops are orientation-independent,
        // and createBitmap's result is immutable (would force a copy in process()).
        val processed = applyExifOrientation(originalBytes, process(bitmap, strength))

        val format = when {
            mimeType.contains("png") -> Bitmap.CompressFormat.PNG
            mimeType.contains("webp") -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        val quality = if (format == Bitmap.CompressFormat.PNG) 100 else 85

        val outputStream = ByteArrayOutputStream()
        processed.compress(format, quality, outputStream)
        processed.recycle()

        return outputStream.toByteArray()
    }
}
