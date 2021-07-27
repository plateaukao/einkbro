package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.TouchAreaType

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
            window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
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
            3 -> view.findViewById(R.id.touch_middle_left_right)
            else -> view.findViewById(R.id.touch_left_right)
        }
        buttonShouldBeChecked.isChecked = true

        // action
        view.findViewById<View>(R.id.layout_left_right).setOnClickListener {
            config.touchAreaType = TouchAreaType.BottomLeftRight
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.layout_bottom_left).setOnClickListener {
            config.touchAreaType = TouchAreaType.Left
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.layout_bottom_right).setOnClickListener {
            config.touchAreaType = TouchAreaType.Right
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.layout_middle_left_right).setOnClickListener {
            config.touchAreaType = TouchAreaType.MiddleLeftRight
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