package de.baumann.browser.helper;


import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;

/**
 * サムネイルオブジェクト
 *
 * @author YUKI Kaoru <k_yuki@menue.co.jp>
 */
@SuppressWarnings("JavaDoc")
public class class_Thumbnail {

    private Bitmap mBitmap;

    public class_Thumbnail(Bitmap bitmap)
    {
        mBitmap = bitmap.copy(bitmap.getConfig(), true);
    }

    public Bitmap getBitmap()
    {
        return mBitmap;
    }

    /**
     * recycle bitmap object
     */
    private void recycle() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    /**
     * 中央で切り取ってサムネイルを作成する
     *
     * @return Cut out center Bitmap object.
     */
    public void centerCrop() {
        Bitmap thumb;
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        paint.setDither(false);
        paint.setFilterBitmap(true);
        Rect srcRect = new Rect();
        Rect dstRect = new Rect();

        int canvasWidth, canvasHeight;
        int bitmapWidth = mBitmap.getWidth();
        int bitmapHeight = mBitmap.getHeight();

        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));
        canvasWidth = 76;
        canvasHeight = 76;

        if (canvasWidth < bitmapWidth && canvasHeight < bitmapWidth) {
            // キャンバスより大きかったら長い辺をキャンバスサイズに合わせてリサイズする
            float ratio = bitmapWidth < bitmapHeight ? (float) 76 / (float)bitmapWidth : (float) 76 / (float)bitmapHeight;
            Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap, (int)(bitmapWidth * ratio), (int)(bitmapHeight * ratio), true);
            recycle();
            bitmapWidth = bitmap.getWidth();
            bitmapHeight = bitmap.getHeight();
            mBitmap = bitmap;
        }

        if (bitmapWidth > canvasWidth || bitmapHeight > canvasHeight) {
            // キャンバスからはみ出していたら中央をキャンバスサイズで切り取る
            Bitmap.Config config = mBitmap.getConfig() != null ? mBitmap.getConfig() : Config.ARGB_8888;
            thumb = Bitmap.createBitmap(canvasWidth, canvasHeight, config);
            canvas.setBitmap(thumb);
            canvas.drawColor(Color.TRANSPARENT);
            if (bitmapWidth > canvasWidth) {
                int left = (bitmapWidth - canvasWidth) / 2;
                srcRect.set(left , 0, left + canvasWidth, bitmapHeight);
                int top = (canvasHeight - bitmapHeight) / 2;
                dstRect.set(0, top, canvasWidth, canvasHeight - top);
            } else {
                int top = (bitmapHeight - canvasHeight) / 2;
                srcRect.set(0, top, bitmapWidth, top + canvasHeight);
                int left = (canvasWidth - bitmapWidth) / 2;
                dstRect.set(left, 0, canvasWidth - left, canvasHeight);
            }
            canvas.drawBitmap(mBitmap, srcRect, dstRect, paint);
        } else {
            // 画像がキャンバスサイズよりも小さかったら中央寄せ
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            thumb = Bitmap.createBitmap(canvasWidth, canvasHeight, config);
            canvas.setBitmap(thumb);
            canvas.drawColor(Color.TRANSPARENT);
            int left = (canvasWidth - bitmapWidth) / 2;
            int top = (canvasHeight - bitmapHeight) / 2;
            canvas.drawBitmap(mBitmap, left, top, paint);
        }
        if (thumb != null) {
            recycle();
            mBitmap = thumb;
        }
    }
}