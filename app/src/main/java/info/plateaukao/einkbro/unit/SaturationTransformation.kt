package info.plateaukao.einkbro.unit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import jp.wasabeef.glide.transformations.BitmapTransformation
import java.security.MessageDigest


class SaturationTransformation(
    private val saturationValue: Float = 0f
) : BitmapTransformation() {
    private val version = 1
    private val id = "info.plateaukao.einkbro.unit.SaturationTransformation.$version"

    override fun transform(
        context: Context, pool: BitmapPool,
        toTransform: Bitmap, outWidth: Int, outHeight: Int
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height
        val config = if (toTransform.config != null) toTransform.config else Bitmap.Config.ARGB_8888
        val bitmap = pool[width, height, config]
        //setCanvasBitmapDensity(toTransform, bitmap)
        val canvas = Canvas(bitmap)
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(saturationValue)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(saturationMatrix)
        canvas.drawBitmap(toTransform, 0f, 0f, paint)
        return bitmap
    }

    override fun toString(): String = "SaturationTransformation()"

    override fun equals(other: Any?): Boolean = other is SaturationTransformation &&
            other.id.hashCode() == id.hashCode()

    override fun hashCode(): Int = id.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) =
        messageDigest.update(id.toByteArray(CHARSET))
}