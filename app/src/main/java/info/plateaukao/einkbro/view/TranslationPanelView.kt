package info.plateaukao.einkbro.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.widget.ImageViewCompat
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ViewUnit.dp

class TranslationPanelView(context: Context) : RelativeLayout(context) {

    val expandedButton: ImageButton
    val controlsContainer: LinearLayout
    val translationOrientation: ImageButton
    val linkHere: ImageButton
    val syncScroll: ImageButton
    val translationLanguage: TextView
    val translationFontPlus: ImageButton
    val translationFontMinus: ImageButton
    val translationClose: ImageButton

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        setBackgroundColor(Color.TRANSPARENT)
        setPadding(0, 0, 2.dp(context), 2.dp(context))

        val tv = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorControlNormal, tv, true)
        val tint = ColorStateList.valueOf(tv.data)
        val btnSize = 40.dp(context)
        val margin = 1 // 0.5dp ~= 1px

        fun createButton(iconRes: Int): ImageButton {
            return AppCompatImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setBackgroundResource(R.drawable.background_with_border)
                setImageResource(iconRes)
                ImageViewCompat.setImageTintList(this, tint)
            }
        }

        // Expanded button (initially invisible)
        expandedButton = AppCompatImageButton(context).apply {
            val lp = LayoutParams(btnSize, btnSize).apply {
                addRule(ALIGN_PARENT_END)
                addRule(ALIGN_PARENT_BOTTOM)
            }
            layoutParams = lp
            setBackgroundResource(R.drawable.background_with_border)
            setImageResource(R.drawable.icon_info)
            ImageViewCompat.setImageTintList(this, tint)
            visibility = View.INVISIBLE
        }
        addView(expandedButton)

        // Controls container
        controlsContainer = LinearLayout(context).apply {
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                addRule(ALIGN_PARENT_END)
                addRule(ALIGN_PARENT_BOTTOM)
            }
            layoutParams = lp
            orientation = LinearLayout.VERTICAL
        }

        translationOrientation = createButton(R.drawable.ic_rotate)
        linkHere = createButton(R.drawable.ic_link_here)
        syncScroll = createButton(R.drawable.ic_sync_scroll)

        translationLanguage = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins(margin, margin, margin, margin)
            }
            setBackgroundResource(R.drawable.background_with_border)
            gravity = Gravity.CENTER
            textAlignment = TEXT_ALIGNMENT_CENTER
            textSize = 16f
        }

        translationFontPlus = createButton(R.drawable.ic_font_increase)
        translationFontMinus = createButton(R.drawable.ic_font_decrease)
        translationClose = createButton(R.drawable.icon_close)

        controlsContainer.addView(translationOrientation)
        controlsContainer.addView(linkHere)
        controlsContainer.addView(syncScroll)
        controlsContainer.addView(translationLanguage)
        controlsContainer.addView(translationFontPlus)
        controlsContainer.addView(translationFontMinus)
        controlsContainer.addView(translationClose)

        addView(controlsContainer)
    }
}
