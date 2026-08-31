package info.plateaukao.einkbro.view.dialog.compose

import android.app.Dialog
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import info.plateaukao.einkbro.preference.UiBorder
import info.plateaukao.einkbro.view.compose.UiThemeState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.ToolbarPosition
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import info.plateaukao.einkbro.view.ThemedBorders


abstract class ComposeDialogFragment : DialogFragment(), KoinComponent {
    protected val config: ConfigManager by inject()
    protected lateinit var composeView: ComposeView

    protected var shouldShowInCenter: Boolean = false
    private var dialogAnchorX: Int = -1
    private var dialogAnchorY: Int = -1

    companion object {
        /** WindowManager.LayoutParams.PRIVATE_FLAG_NO_MOVE_ANIMATION (hidden). */
        private const val PRIVATE_FLAG_NO_MOVE_ANIMATION = 0x00000040

        /** Space between a long-press point and the edge of a menu anchored to it. */
        private const val FINGER_GAP_DP = 16

        /** Horizontal center X (px) of the last clicked toolbar icon. Set by toolbar, consumed by dialog. */
        var anchorX: Int = -1
        /** Vertical center Y (px) of the last clicked toolbar icon. Set by toolbar, consumed by dialog. */
        var anchorY: Int = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // These dialogs take constructor lambdas/views; an instance restored
            // after process death would hold dead callbacks. They are transient
            // popups, so dropping them on restore is the correct behavior.
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NO_FRAME, R.style.EinkPanelDialogTheme)
        dialogAnchorX = anchorX
        dialogAnchorY = anchorY
        anchorX = -1
        anchorY = -1
        return super.onCreateDialog(savedInstanceState)
    }

    private fun setupDialog() {
        dialog?.apply {
            setCanceledOnTouchOutside(true)
            val w = window ?: return
            w.attributes = w.attributes.apply {
                windowAnimations = 0
                disableMoveAnimation()
            }
            if (!shouldShowInCenter) {
                if (config.ui.isVerticalToolbar) {
                    val horizontalGravity = if (config.ui.toolbarPosition == ToolbarPosition.Left) Gravity.START else Gravity.END
                    w.setGravity(Gravity.TOP or horizontalGravity)
                } else {
                    val verticalGravity = if (config.ui.isToolbarOnTop) Gravity.TOP else Gravity.BOTTOM
                    if (dialogAnchorX >= 0) {
                        w.setGravity(verticalGravity or Gravity.START)
                    } else {
                        w.setGravity(verticalGravity or Gravity.END)
                    }
                }
            }
            w.setBackgroundDrawable(ThemedBorders.dialogFrame(requireContext()))
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupDialog()

        composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        beforeComposing()
        composeView.setContent { MyTheme { Content() } }

        return composeView
    }

    /** One-time setup that must run before the composition is created. */
    protected open fun beforeComposing() {}

    override fun onStart() {
        super.onStart()
        if (config.ui.isVerticalToolbar) {
            adjustVerticalPosition()
        } else {
            adjustHorizontalPosition()
        }
    }

    protected open fun adjustHorizontalPosition() {
        if (dialogAnchorX < 0 || shouldShowInCenter) return

        val window = dialog?.window ?: return
        // Hide while repositioning to avoid visible jump from left edge
        window.attributes = window.attributes.apply { alpha = 0f }

        window.decorView.post {
            val w = dialog?.window ?: return@post
            val dialogWidth = w.decorView.width
            val screenWidth = resources.displayMetrics.widthPixels
            val targetX = if (dialogWidth > 0 && screenWidth > 0) {
                (dialogAnchorX - dialogWidth / 2)
                    .coerceIn(0, maxOf(0, screenWidth - dialogWidth))
            } else 0
            w.attributes = w.attributes.apply {
                x = targetX
                alpha = 1f
            }
        }
    }

    protected open fun adjustVerticalPosition() {
        if (dialogAnchorY < 0 || shouldShowInCenter) return

        val window = dialog?.window ?: return
        window.attributes = window.attributes.apply { alpha = 0f }

        window.decorView.post {
            val w = dialog?.window ?: return@post
            val dialogHeight = w.decorView.height
            val screenHeight = resources.displayMetrics.heightPixels
            val targetY = if (dialogHeight > 0 && screenHeight > 0) {
                (dialogAnchorY - dialogHeight / 2)
                    .coerceIn(0, maxOf(0, screenHeight - dialogHeight))
            } else 0
            w.attributes = w.attributes.apply {
                y = targetY
                alpha = 1f
            }
        }
    }

    /** A long-press anchor; (0,0) is what callers pass when no position is known. */
    protected fun Point.isValidAnchor() = x != 0 && y != 0

    /**
     * True between [prepareFingerAnchor] and the window landing at its final
     * place. Until then the (invisible) window sits at the top-left corner, so
     * hover hit-tests against its items would match the wrong spot.
     */
    protected var isFingerAnchorPending = false
        private set

    /**
     * Call from onCreateView for a dialog anchored to a long-press point:
     * absolute top-left placement, kept invisible until [positionAboveFinger]
     * has measured the content and moved the window there.
     *
     * The window starts just below the top system inset: a floating window
     * that overlaps the status bar gets that inset applied as decor padding,
     * which inflates the measured size and does not go away when the window
     * is moved later, so the content would end up shifted and clipped.
     */
    protected fun prepareFingerAnchor() {
        val window = dialog?.window ?: return
        window.setGravity(Gravity.TOP or Gravity.LEFT)
        window.attributes = window.attributes.apply {
            x = 0
            y = topSystemInset()
            alpha = 0f
        }
        isFingerAnchorPending = true
    }

    /**
     * Places the dialog above [finger] (screen coordinates): the finger that is
     * still holding the item, and may drag onto a menu entry, covers anything
     * drawn below it. Only when there is no room above (without going under
     * the status bar) does the dialog go below. Needs the laid-out size,
     * hence the post; call from onStart.
     */
    protected fun positionAboveFinger(finger: Point) {
        val window = dialog?.window ?: return
        window.decorView.post {
            val w = dialog?.window ?: return@post
            val decor = w.decorView
            // Window x/y are relative to the area the window may occupy; the
            // finger point is on-screen.
            val origin = IntArray(2).also { decor.getLocationOnScreen(it) }
            val originX = origin[0] - w.attributes.x
            val originY = origin[1] - w.attributes.y
            val gap = ViewUnit.dpToPixel(FINGER_GAP_DP).toInt()
            val topInset = topSystemInset()
            val screenWidth = resources.displayMetrics.widthPixels

            val aboveY = finger.y - originY - decor.height - gap
            val targetY = if (decor.height > 0 && aboveY >= topInset) aboveY
                else (finger.y - originY + gap).coerceAtLeast(topInset)
            val targetX = (finger.x - originX).coerceIn(0, maxOf(0, screenWidth - decor.width))
            // Hover hit-tests use on-screen item positions, which only update
            // once the moved window has gone through a traversal.
            OneShotPreDrawListener.add(decor) { isFingerAnchorPending = false }
            w.attributes = w.attributes.apply {
                x = targetX
                y = targetY
                alpha = 1f
            }
        }
    }

    /** Height of the status bar / display cutout band at the top of the screen. */
    private fun topSystemInset(): Int {
        val activityDecor = activity?.window?.decorView ?: return 0
        val insets = ViewCompat.getRootWindowInsets(activityDecor) ?: return 0
        return insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        ).top
    }

    /**
     * WindowManager animates a window whose frame moves (~250ms slide). These wrap-content,
     * bottom/side-anchored dialogs move whenever their content changes height or they are
     * repositioned over the toolbar anchor, which on e-ink shows as a smeared "shift".
     * `android:windowNoMoveAnimation` (API 34+, set in the dialog theme) maps to the same
     * private flag; this covers older devices.
     */
    private fun WindowManager.LayoutParams.disableMoveAnimation() {
        try {
            val field = WindowManager.LayoutParams::class.java.getDeclaredField("privateFlags")
            field.setInt(this, field.getInt(this) or PRIVATE_FLAG_NO_MOVE_ANIMATION)
        } catch (_: Throwable) {
            // Hidden field unavailable; the theme attribute still applies on API 34+.
        }
    }

    /** Dialog content; the base class wraps it in MyTheme and sets it on composeView. */
    @Composable
    protected abstract fun Content()

}

/**
 * Divider that follows the theme's border style, so separators inside a dialog
 * speak the same visual language as its frame: dashed borders get dashed
 * dividers, paper/certificate get double lines, stamp gets a perforation row,
 * sketch a hand-drawn line; the rest stay solid at the border's weight.
 */
@Composable
fun HorizontalSeparator(modifier: Modifier = Modifier) {
    ThemedSeparator(modifier, horizontal = true)
}

@Composable
fun VerticalSeparator(modifier: Modifier = Modifier) {
    ThemedSeparator(modifier, horizontal = false)
}

@Composable
private fun ThemedSeparator(modifier: Modifier, horizontal: Boolean) {
    val border = UiThemeState.uiBorder.value
    val color = MaterialTheme.colors.primary
    val thickness = when (border) {
        UiBorder.PAPER -> 4.dp
        UiBorder.CERTIFICATE -> 5.dp
        UiBorder.SKETCH -> 4.dp
        UiBorder.STAMP -> 3.dp
        else -> maxOf(border.widthDp, 1f).dp
    }
    Spacer(
        modifier
            .then(
                if (horizontal) Modifier
                    .fillMaxWidth()
                    .height(thickness)
                else Modifier
                    .width(thickness)
                    .height(30.dp)
            )
            .drawBehind { drawThemedSeparator(border, color, horizontal) }
    )
}

private fun DrawScope.drawThemedSeparator(border: UiBorder, color: Color, horizontal: Boolean) {
    val length = if (horizontal) size.width else size.height
    val breadth = if (horizontal) size.height else size.width
    fun line(
        cross: Float,
        stroke: Float,
        effect: PathEffect? = null,
        cap: StrokeCap = StrokeCap.Butt,
    ) {
        val start = if (horizontal) Offset(0f, cross) else Offset(cross, 0f)
        val end = if (horizontal) Offset(length, cross) else Offset(cross, length)
        drawLine(color, start, end, strokeWidth = stroke, pathEffect = effect, cap = cap)
    }
    when (border) {
        UiBorder.DASHED -> line(
            breadth / 2f, 1.5.dp.toPx(),
            PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()), 0f),
        )
        UiBorder.PAPER -> {
            line(0.5.dp.toPx(), 1.dp.toPx())
            line(breadth - 0.5.dp.toPx(), 1.dp.toPx())
        }
        UiBorder.CERTIFICATE -> {
            line(1.dp.toPx(), 2.dp.toPx())
            line(breadth - 0.5.dp.toPx(), 1.dp.toPx())
        }
        // perforation row, echoing the stamp frame's bite holes
        UiBorder.STAMP -> line(
            breadth / 2f, 2.5.dp.toPx(),
            PathEffect.dashPathEffect(floatArrayOf(0.1f, 6.dp.toPx()), 0f),
            StrokeCap.Round,
        )
        UiBorder.SKETCH -> {
            // same idea as sketchShape: short segments with a deterministic
            // wobble, so the line looks hand-drawn but doesn't shimmer
            val amplitude = 1.2.dp.toPx()
            val step = 12.dp.toPx()
            val mid = breadth / 2f
            val n = kotlin.math.max(2, (length / step).toInt())
            val path = Path()
            for (k in 0..n) {
                val t = k.toFloat() / n
                val h = kotlin.math.sin(k * 12.9898 + length) * 43758.5453
                val j =
                    if (k == 0 || k == n) 0f
                    else ((h - kotlin.math.floor(h)).toFloat() * 2f - 1f) * amplitude
                val along = t * length
                val x = if (horizontal) along else mid + j
                val y = if (horizontal) mid + j else along
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(1.5.dp.toPx()))
        }
        // NONE, CLASSIC, ROUND, SHARP, STICKER: solid at the border's weight
        else -> line(breadth / 2f, maxOf(border.widthDp, 1f).dp.toPx())
    }
}

