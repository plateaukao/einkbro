package de.baumann.browser.unit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.view.TouchDelegate
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity

object ViewUnit {
    @JvmStatic
    fun bound(context: Context, view: View) {
        val windowWidth = getWindowWidth(context)
        val windowHeight = getWindowHeight(context)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(windowWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(windowHeight, View.MeasureSpec.EXACTLY)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    @JvmStatic
    fun createImage(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = color
        paint.alpha = 50
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    @JvmStatic
    fun capture(view: View, width: Float, height: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        val canvas = Canvas(bitmap)
        val left = view.left
        val top = view.top
        val status = canvas.save()
        canvas.translate(-left.toFloat(), -top.toFloat())
        val scale = width / view.width
        canvas.scale(scale, scale, left.toFloat(), top.toFloat())
        view.draw(canvas)
        canvas.restoreToCount(status)
        val alphaPaint = Paint()
        alphaPaint.color = Color.TRANSPARENT
        canvas.drawRect(0f, 0f, 1f, height, alphaPaint)
        canvas.drawRect(width - 1f, 0f, width, height, alphaPaint)
        canvas.drawRect(0f, 0f, width, 1f, alphaPaint)
        canvas.drawRect(0f, height - 1f, width, height, alphaPaint)
        canvas.setBitmap(null)
        return bitmap
    }

    @JvmStatic
    fun isLandscape(context: Context): Boolean =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    @JvmStatic
    fun isTablet(context: Context): Boolean =
            (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >=
                    Configuration.SCREENLAYOUT_SIZE_LARGE

    @JvmStatic
    fun getDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    @JvmStatic
    private fun getWindowHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    @JvmStatic
    fun getWindowWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    @JvmStatic
    fun dpToPixel(context: Context, dp: Int): Float {
        val metrics = context.resources.displayMetrics
        return dp * (metrics.densityDpi / 160f)
    }

    fun Int.dp(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return (this * (metrics.densityDpi / 160f)).toInt()
    }

    fun setCustomFullscreen(window: Window, fullscreen: Boolean) {
        val decorView = window.decorView
        if (fullscreen) {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: return
        view.post {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun showKeyboard(activity: Activity) {
        val imm = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        activity.runOnUiThread {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }
    }

    fun expandViewTouchArea(view: View, size: Int) {
        val parent = view.parent as View // button: the view you want to enlarge hit area

        parent.post {
            val rect = Rect()
            view.getHitRect(rect)
            rect.top -= size
            rect.left -= size
            rect.bottom += size
            rect.right += size
            parent.touchDelegate = TouchDelegate(rect, view)
        }
    }

    fun isMultiWindowEnabled(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            return false
        }
        return activity.isInMultiWindowMode
    }

    fun toggleMultiWindow(activity: Activity, isEnabled: Boolean) {
        if (isEnabled && isMultiWindowEnabled(activity)) return
        if (!isEnabled && !isMultiWindowEnabled(activity)) return

        val intent = Intent().apply {
            action = if (isEnabled) "com.onyx.action.START_MULTI_WINDOW" else "com.onyx.action.QUIT_MULTI_WINDOW"
        }
        activity.sendBroadcast(intent)
    }

}