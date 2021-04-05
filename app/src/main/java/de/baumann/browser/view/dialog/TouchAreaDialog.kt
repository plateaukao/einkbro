package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.TouchAreaType
import de.baumann.browser.unit.ViewUnit

class TouchAreaDialog(private val context: Context) {
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private val config: ConfigManager = ConfigManager(context)

    private lateinit var dialog: AlertDialog

    fun show() {
        val view = View.inflate(context, R.layout.dialog_touch_area, null)
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(view) }

        initViews(view)
        dialog = builder.create().apply {
            window?.setGravity(Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(R.drawable.background_with_margin)
        }
        dialog.show()
    }

    private fun initViews(view: View) {
        // init touch area types
        initHintTypes(view)
        initToggles(view)
    }

    private fun initHintTypes(view: View) {
        // UI setup
        val buttonShouldBeChecked: RadioButton = when(sp.getInt("sp_touch_area_type", 0)) {
            0 -> view.findViewById(R.id.touch_left_right)
            1 -> view.findViewById(R.id.touch_area_bottom_left)
            2 -> view.findViewById(R.id.touch_area_bottom_right)
            else -> view.findViewById(R.id.touch_left_right)
        }
        buttonShouldBeChecked.isChecked = true

        // action
        view.findViewById<View>(R.id.layout_left_right).setOnClickListener {
            sp.edit { putInt("sp_touch_area_type", TouchAreaType.BottomLeftRight.ordinal) }
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.layout_bottom_left).setOnClickListener {
            sp.edit { putInt("sp_touch_area_type", TouchAreaType.Left.ordinal) }
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.layout_bottom_right).setOnClickListener {
            sp.edit { putInt("sp_touch_area_type", TouchAreaType.Right.ordinal) }
            dialog.dismiss()
        }
    }

    private fun initToggles(view: View) {
        val toggleTouchAreaHint = view.findViewById<CheckBox>(R.id.switch_show_touch_area_hint) ?: return
        val showTouchArea = view.findViewById<View>(R.id.show_touch_area) ?: return

        updateViewStatus(toggleTouchAreaHint, config.touchAreaHint)

        showTouchArea.setOnClickListener {
            config.touchAreaHint = !config.touchAreaHint
            updateViewStatus(toggleTouchAreaHint, config.touchAreaHint)
            dialog.dismiss()
        }
        toggleTouchAreaHint.setOnClickListener {
            config.touchAreaHint = !config.touchAreaHint
            updateViewStatus(toggleTouchAreaHint, config.touchAreaHint)
            dialog.dismiss()
        }
    }

    private fun updateViewStatus(checkBox: CheckBox, shouldBeChecked: Boolean) {
        checkBox.isChecked = shouldBeChecked
    }
}