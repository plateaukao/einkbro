package info.plateaukao.einkbro.view

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ViewUnit.dp

class MainContentLayout(
    val root: ConstraintLayout,
    val swipeRefreshLayout: SwipeRefreshLayout,
    val mainContent: FrameLayout,
    val touchAreaMiddleLeft: View,
    val touchAreaLongLeft: View,
    val touchAreaBottomLeft: View,
    val touchAreaMiddleRight: View,
    val touchAreaLongRight: View,
    val touchAreaMiddleDrag: View,
    val touchAreaBottomRight: View,
    val touchAreaBottomDrag: View,
    val touchAreaRight1: View,
    val touchAreaRight2: View,
    val touchAreaRightDrag: View,
    val touchAreaLeft1: View,
    val touchAreaLeft2: View,
    val touchAreaLeftDrag: View,
    val mainProgressBar: ProgressBar,
    val fabImageButtonNav: TextView,
    val translationLanguage: TextView,
    val remoteTextSearch: ImageButton,
    val externalSearchClose: ImageButton,
    val externalSearchActionContainer: LinearLayout,
) {
    companion object {
        private fun dpToPx(context: Context, dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        private fun resolveBackgroundColor(context: Context): Int {
            val tv = TypedValue()
            // Resolve the custom theme attribute "backgroundColor" defined in styles.xml
            val attrId = context.resources.getIdentifier("backgroundColor", "attr", context.packageName)
            if (attrId != 0) {
                context.theme.resolveAttribute(attrId, tv, true)
                return tv.data
            }
            // Fallback to standard Android colorBackground
            context.theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
            return tv.data
        }

        private fun resolveColorControlNormal(context: Context): Int {
            val tv = TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorControlNormal, tv, true)
            return tv.data
        }

        fun create(context: Context): MainContentLayout {
            val root = ConstraintLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            // SwipeRefreshLayout
            val swipeRefreshLayout = SwipeRefreshLayout(context).apply {
                id = R.id.swipe_refresh_layout
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
            }
            root.addView(swipeRefreshLayout)

            // main_content FrameLayout inside SwipeRefreshLayout
            val mainContent = FrameLayout(context).apply {
                id = R.id.main_content
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(resolveBackgroundColor(context))
            }
            swipeRefreshLayout.addView(mainContent)

            val touchAreaBorderDrawable = ContextCompat.getDrawable(context, R.drawable.touch_area_border)

            // Touch area views
            val touchAreaMiddleLeft = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaMiddleLeft, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), dpToPx(context, 250)
            ).apply {
                marginStart = dpToPx(context, -1)
            })

            val touchAreaLongLeft = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaLongLeft, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), 0
            ).apply {
                marginStart = dpToPx(context, -5)
                topMargin = dpToPx(context, -5)
                bottomMargin = dpToPx(context, -5)
            })

            val touchAreaBottomLeft = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaBottomLeft, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), dpToPx(context, 250)
            ).apply {
                marginStart = dpToPx(context, -2)
                bottomMargin = dpToPx(context, -2)
            })

            val touchAreaMiddleRight = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaMiddleRight, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), dpToPx(context, 250)
            ).apply {
                marginEnd = dpToPx(context, -2)
            })

            val touchAreaLongRight = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaLongRight, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), 0
            ).apply {
                topMargin = dpToPx(context, -5)
                marginEnd = dpToPx(context, -5)
                bottomMargin = dpToPx(context, -5)
            })

            val touchAreaMiddleDrag = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            root.addView(touchAreaMiddleDrag, ConstraintLayout.LayoutParams(
                dpToPx(context, 40), dpToPx(context, 40)
            ).apply {
                marginStart = dpToPx(context, -20)
                topMargin = dpToPx(context, -20)
            })

            val touchAreaBottomRight = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaBottomRight, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), dpToPx(context, 250)
            ).apply {
                marginEnd = dpToPx(context, -2)
                bottomMargin = dpToPx(context, -2)
            })

            val touchAreaBottomDrag = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            root.addView(touchAreaBottomDrag, ConstraintLayout.LayoutParams(
                dpToPx(context, 40), dpToPx(context, 40)
            ).apply {
                marginStart = dpToPx(context, -20)
                topMargin = dpToPx(context, -20)
            })

            val touchAreaRight1 = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaRight1, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), dpToPx(context, 150)
            ).apply {
                marginEnd = dpToPx(context, -1)
                bottomMargin = dpToPx(context, 150)
            })

            val touchAreaRight2 = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaRight2, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), dpToPx(context, 150)
            ).apply {
                marginEnd = dpToPx(context, -1)
                bottomMargin = dpToPx(context, -2)
            })

            val touchAreaRightDrag = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            root.addView(touchAreaRightDrag, ConstraintLayout.LayoutParams(
                dpToPx(context, 40), dpToPx(context, 40)
            ).apply {
                marginStart = dpToPx(context, -20)
                topMargin = dpToPx(context, -20)
            })

            val touchAreaLeft1 = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaLeft1, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), dpToPx(context, 150)
            ).apply {
                marginStart = dpToPx(context, -1)
                bottomMargin = dpToPx(context, 150)
            })

            val touchAreaLeft2 = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                background = ContextCompat.getDrawable(context, R.drawable.touch_area_border)
            }
            root.addView(touchAreaLeft2, ConstraintLayout.LayoutParams(
                dpToPx(context, 150), dpToPx(context, 150)
            ).apply {
                marginStart = dpToPx(context, -1)
                bottomMargin = dpToPx(context, -2)
            })

            val touchAreaLeftDrag = View(context).apply {
                id = View.generateViewId()
                visibility = View.INVISIBLE
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            root.addView(touchAreaLeftDrag, ConstraintLayout.LayoutParams(
                dpToPx(context, 40), dpToPx(context, 40)
            ).apply {
                topMargin = dpToPx(context, -20)
                marginEnd = dpToPx(context, -20)
            })

            // ProgressBar
            val mainProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                id = R.id.main_progress_bar
                max = 100
                val tintColor = resolveColorControlNormal(context)
                progressTintList = android.content.res.ColorStateList.valueOf(tintColor)
            }
            root.addView(mainProgressBar, ConstraintLayout.LayoutParams(
                0, dpToPx(context, 4)
            ).apply {
                // maxHeight is handled by the height constraint
            })

            // fab_imageButtonNav
            val fabImageButtonNav = TextView(context).apply {
                id = R.id.fab_imageButtonNav
                layoutParams = ConstraintLayout.LayoutParams(
                    dpToPx(context, 80), dpToPx(context, 80)
                )
                background = ContextCompat.getDrawable(context, R.drawable.roundcorner)
                gravity = android.view.Gravity.CENTER
                setShadowLayer(1.6f, 1.5f, 1.3f, android.graphics.Color.WHITE)
                setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                textSize = 22f
                visibility = View.GONE
            }
            root.addView(fabImageButtonNav)

            // translation_language
            val translationLanguage = TextView(context).apply {
                id = R.id.translation_language
                val size = dpToPx(context, 40)
                layoutParams = ConstraintLayout.LayoutParams(size, size)
                val margin = dpToPx(context, 0) // 0.5dp rounds to 0 or 1
                background = ContextCompat.getDrawable(context, R.drawable.background_with_border)
                gravity = android.view.Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 16f
                val tintColor = resolveColorControlNormal(context)
                visibility = View.GONE
            }
            root.addView(translationLanguage)

            // remote_text_search
            val remoteTextSearch = ImageButton(context).apply {
                id = R.id.remote_text_search
                val size = dpToPx(context, 40)
                layoutParams = ConstraintLayout.LayoutParams(size, size)
                background = ContextCompat.getDrawable(context, R.drawable.background_with_border)
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_search))
                val tintColor = resolveColorControlNormal(context)
                imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
                contentDescription = "Search"
            }
            root.addView(remoteTextSearch)

            // external_search_action_container
            val externalSearchActionContainer = LinearLayout(context).apply {
                id = R.id.external_search_action_container
                val width = dpToPx(context, 40)
                layoutParams = ConstraintLayout.LayoutParams(width, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                visibility = View.INVISIBLE
                orientation = LinearLayout.VERTICAL
            }

            // external_search_close button inside container
            val externalSearchClose = ImageButton(context).apply {
                id = R.id.external_search_close
                val size = dpToPx(context, 40)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    val m = dpToPx(context, 0) // 0.5dp
                    setMargins(m, m, m, m)
                }
                background = ContextCompat.getDrawable(context, R.drawable.background_with_border)
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_minimize))
                val tintColor = resolveColorControlNormal(context)
                imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
                contentDescription = "Close external search"
            }
            externalSearchActionContainer.addView(externalSearchClose)
            root.addView(externalSearchActionContainer)

            // Apply all constraints
            val constraintSet = ConstraintSet()
            constraintSet.clone(root)

            // SwipeRefreshLayout constraints
            constraintSet.connect(swipeRefreshLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(swipeRefreshLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(swipeRefreshLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(swipeRefreshLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // touch_area_middle_left: top/bottom to parent (centered vertically), start to parent
            constraintSet.connect(touchAreaMiddleLeft.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(touchAreaMiddleLeft.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(touchAreaMiddleLeft.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

            // touch_area_long_left: top/bottom to parent, start to parent
            constraintSet.connect(touchAreaLongLeft.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(touchAreaLongLeft.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(touchAreaLongLeft.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

            // touch_area_bottom_left: bottom to parent, start to parent
            constraintSet.connect(touchAreaBottomLeft.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(touchAreaBottomLeft.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

            // touch_area_middle_right: top/bottom to parent, end to parent
            constraintSet.connect(touchAreaMiddleRight.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(touchAreaMiddleRight.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(touchAreaMiddleRight.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            // touch_area_long_right: top/bottom to parent, end to parent
            constraintSet.connect(touchAreaLongRight.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(touchAreaLongRight.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(touchAreaLongRight.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            // touch_area_middle_drag: start to touch_area_middle_right.start, top to touch_area_middle_right.top
            constraintSet.connect(touchAreaMiddleDrag.id, ConstraintSet.START, touchAreaMiddleRight.id, ConstraintSet.START)
            constraintSet.connect(touchAreaMiddleDrag.id, ConstraintSet.TOP, touchAreaMiddleRight.id, ConstraintSet.TOP)

            // touch_area_bottom_right: end to parent, bottom to parent
            constraintSet.connect(touchAreaBottomRight.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(touchAreaBottomRight.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // touch_area_bottom_drag: start to touch_area_bottom_right.start, top to touch_area_bottom_right.top
            constraintSet.connect(touchAreaBottomDrag.id, ConstraintSet.START, touchAreaBottomRight.id, ConstraintSet.START)
            constraintSet.connect(touchAreaBottomDrag.id, ConstraintSet.TOP, touchAreaBottomRight.id, ConstraintSet.TOP)

            // touch_area_right_1: end to parent, bottom to parent (with 150dp margin)
            constraintSet.connect(touchAreaRight1.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(touchAreaRight1.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // touch_area_right_2: end to parent, bottom to parent
            constraintSet.connect(touchAreaRight2.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(touchAreaRight2.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // touch_area_right_drag: start to touch_area_right_1.start, top to touch_area_right_1.top
            constraintSet.connect(touchAreaRightDrag.id, ConstraintSet.START, touchAreaRight1.id, ConstraintSet.START)
            constraintSet.connect(touchAreaRightDrag.id, ConstraintSet.TOP, touchAreaRight1.id, ConstraintSet.TOP)

            // touch_area_left_1: start to parent, bottom to parent (with 150dp margin)
            constraintSet.connect(touchAreaLeft1.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(touchAreaLeft1.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // touch_area_left_2: start to parent, bottom to parent
            constraintSet.connect(touchAreaLeft2.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(touchAreaLeft2.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // touch_area_left_drag: end to touch_area_left_1.end, top to touch_area_left_1.top
            constraintSet.connect(touchAreaLeftDrag.id, ConstraintSet.END, touchAreaLeft1.id, ConstraintSet.END)
            constraintSet.connect(touchAreaLeftDrag.id, ConstraintSet.TOP, touchAreaLeft1.id, ConstraintSet.TOP)

            // main_progress_bar: start/end to parent, bottom to parent
            constraintSet.connect(mainProgressBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(mainProgressBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(mainProgressBar.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // fab_imageButtonNav: end to parent, bottom to swipe_refresh_layout.bottom
            constraintSet.connect(fabImageButtonNav.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(fabImageButtonNav.id, ConstraintSet.BOTTOM, swipeRefreshLayout.id, ConstraintSet.BOTTOM)

            // translation_language: end to parent, bottom to parent
            constraintSet.connect(translationLanguage.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(translationLanguage.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // remote_text_search: end to parent, bottom to parent
            constraintSet.connect(remoteTextSearch.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(remoteTextSearch.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // external_search_action_container: end to parent, bottom to parent
            constraintSet.connect(externalSearchActionContainer.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(externalSearchActionContainer.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            constraintSet.applyTo(root)

            return MainContentLayout(
                root = root,
                swipeRefreshLayout = swipeRefreshLayout,
                mainContent = mainContent,
                touchAreaMiddleLeft = touchAreaMiddleLeft,
                touchAreaLongLeft = touchAreaLongLeft,
                touchAreaBottomLeft = touchAreaBottomLeft,
                touchAreaMiddleRight = touchAreaMiddleRight,
                touchAreaLongRight = touchAreaLongRight,
                touchAreaMiddleDrag = touchAreaMiddleDrag,
                touchAreaBottomRight = touchAreaBottomRight,
                touchAreaBottomDrag = touchAreaBottomDrag,
                touchAreaRight1 = touchAreaRight1,
                touchAreaRight2 = touchAreaRight2,
                touchAreaRightDrag = touchAreaRightDrag,
                touchAreaLeft1 = touchAreaLeft1,
                touchAreaLeft2 = touchAreaLeft2,
                touchAreaLeftDrag = touchAreaLeftDrag,
                mainProgressBar = mainProgressBar,
                fabImageButtonNav = fabImageButtonNav,
                translationLanguage = translationLanguage,
                remoteTextSearch = remoteTextSearch,
                externalSearchClose = externalSearchClose,
                externalSearchActionContainer = externalSearchActionContainer,
            )
        }
    }
}
