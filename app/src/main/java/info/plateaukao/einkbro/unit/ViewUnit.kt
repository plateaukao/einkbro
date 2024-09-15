package info.plateaukao.einkbro.unit

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Menu
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView.LAYER_TYPE_HARDWARE
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowInsetsCompat
import info.plateaukao.einkbro.EinkBroApplication
import info.plateaukao.einkbro.databinding.ActivityMainBinding
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.util.TranslationLanguage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


object ViewUnit: KoinComponent {
    private val config: ConfigManager by inject()

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

    fun captureDrawingCache(view: View): Bitmap {
        view.isDrawingCacheEnabled = true
        view.destroyDrawingCache()
        view.buildDrawingCache()
        var bitmap: Bitmap? = null
        while (bitmap == null) {
            bitmap = view.drawingCache
        }
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
    fun dpToPixel(dp: Int): Float {
        val metrics = EinkBroApplication.instance.resources.displayMetrics
        return dp * (metrics.densityDpi / 160f)
    }


    fun isWideLayout(context: Context): Boolean = isLandscape(context) || isTablet(context)

    fun Int.dp(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return (this * (metrics.densityDpi / 160f)).toInt()
    }

    fun isEdgeToEdgeEnabled(resources: Resources): Boolean {
        val resourceId: Int =
            resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        if (resourceId > 0) {
            return resources.getInteger(resourceId) == 2
        }
        return false
    }

    private var isNavigationBarDisplayed: Boolean? = null
    fun setCustomFullscreen(
        window: Window,
        fullscreen: Boolean,
        keepHideStatusBar: Boolean,
        hideNavigationBar: Boolean
    ) {
        if (fullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (isNavigationBarDisplayed == null) isNavigationBarDisplayed =
                    WindowInsetsCompat.toWindowInsetsCompat(window.decorView.rootWindowInsets)
                        .isVisible(WindowInsetsCompat.Type.navigationBars())
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars())
                    it.hide(WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let {
                    if (isNavigationBarDisplayed == true && !hideNavigationBar) it.show(WindowInsets.Type.navigationBars())
                    if (!keepHideStatusBar) it.show(WindowInsets.Type.statusBars())
                }
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
    }

    fun hideKeyboard(activity: Activity) {
        val imm =
            activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        activity.runOnUiThread {
            imm.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
    }

    fun showKeyboard(activity: Activity) {
        val imm =
            activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        activity.runOnUiThread {
            imm.toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY
            )
            //val view = activity.currentFocus ?: return@runOnUiThread
            //imm.showSoftInput(view, 0)
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

    fun isMultiWindowEnabled(activity: Activity): Boolean = activity.isInMultiWindowMode

    fun updateLanguageLabel(textView: TextView, translationLanguage: TranslationLanguage) {
        val languageString = translationLanguage.value
        val language = languageString.split("-").last()
        textView.text = language
    }

    fun invertColor(view: View, shouldInvertColor: Boolean) {
        val invertPaint: Paint = Paint().apply {
            val colorMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        if (shouldInvertColor) {
            view.setLayerType(LAYER_TYPE_HARDWARE, invertPaint)
        } else {
            view.setLayerType(LAYER_TYPE_HARDWARE, null)
        }
    }

    fun updateViewPosition(view: View, point: Point) {
        val properPoint = getProperPosition(view, point)
        view.x = properPoint.x + dpToPixel(10)
        view.y = properPoint.y + dpToPixel(10)
    }

    private fun getProperPosition(view: View, point: Point): Point {
        val parentWidth = (view.parent as View).width
        val parentHeight = (view.parent as View).height

        val width = view.width
        val height = view.height
        // Calculate the new position to ensure the view is within bounds
        val padding = dpToPixel(10)
        val x =
            if (point.x + width + padding > parentWidth) parentWidth - width - padding else point.x
        val y =
            if (point.y + height + padding > parentHeight) parentHeight - height - padding else point.y

        return Point(x.toInt(), y.toInt())
    }

    fun isTextEditMode(context: Context, menu: Menu): Boolean {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.title == context.getString(android.R.string.paste)) {
                return true
            }
        }
        return false
    }

    fun createCountString(superScript: Int, subScript: Int): String {
        if (subScript == 0 || superScript == 0) return "1"
        if (subScript >= 10) return subScript.toString()

        if (subScript == superScript) return subScript.toString()

        val superScripts = listOf("¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹")
        val subScripts = listOf("₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉")
        val separator = "⁄"
        return "${superScripts[superScript - 1]}$separator${subScripts[subScript - 1]}"
    }


    fun updateAppbarPosition(binding: ActivityMainBinding) {
        if (config.isToolbarOnTop) {
            moveAppbarToTop(binding)
        } else {
            moveAppbarToBottom(binding)
        }
        binding.inputUrl.shouldReverse = config.isToolbarOnTop
    }


    private fun moveAppbarToBottom(binding: ActivityMainBinding) {
        val constraintSet = ConstraintSet().apply {
            clone(binding.root)
            connect(
                binding.appBar.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )
            connect(
                binding.inputUrl.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
                0
            )
            connect(
                binding.twoPanelLayout.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP
            )
            connect(
                binding.twoPanelLayout.id,
                ConstraintSet.BOTTOM,
                binding.appBar.id,
                ConstraintSet.TOP
            )

            clear(binding.contentSeparator.id, ConstraintSet.TOP)
            connect(
                binding.contentSeparator.id,
                ConstraintSet.BOTTOM,
                binding.appBar.id,
                ConstraintSet.TOP
            )
        }
        constraintSet.applyTo(binding.root)
    }

    private fun moveAppbarToTop(binding: ActivityMainBinding) {
        val constraintSet = ConstraintSet().apply {
            clone(binding.root)
            clear(binding.appBar.id, ConstraintSet.BOTTOM)
            clear(binding.inputUrl.id, ConstraintSet.BOTTOM)

            connect(
                binding.twoPanelLayout.id,
                ConstraintSet.TOP,
                binding.appBar.id,
                ConstraintSet.BOTTOM
            )
            connect(
                binding.twoPanelLayout.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM
            )

            clear(binding.contentSeparator.id, ConstraintSet.BOTTOM)
            connect(
                binding.contentSeparator.id,
                ConstraintSet.TOP,
                binding.appBar.id,
                ConstraintSet.BOTTOM
            )
        }
        constraintSet.applyTo(binding.root)
    }
}

fun MotionEvent.toRawPoint(): Point = Point(rawX.toInt(), rawY.toInt())
