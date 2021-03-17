package de.baumann.browser.view.dialog

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.TouchAreaType

class TouchAreaDialog( context: Context) : BottomSheetDialog(context, R.style.BottomSheetDialog) {
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View.inflate(context, R.layout.dialog_touch_area, null))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        initViews()
    }

    private fun initViews() {
        // init touch area types
        initHintTypes()
        initToggles()
    }

    private fun initHintTypes() {
        // UI setup
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup) ?: return
        when(sp.getInt("sp_touch_area_type", 0)) {
            0 -> radioGroup.check(R.id.touch_left_right)
            1 -> radioGroup.check(R.id.touch_area_bottom_left)
            2 -> radioGroup.check(R.id.touch_area_bottom_right)
            else -> radioGroup.check(R.id.touch_left_right)
        }

        // action
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val type = when(checkedId) {
                R.id.touch_area_bottom_left -> TouchAreaType.Left
                R.id.touch_area_bottom_right -> TouchAreaType.Right
                R.id.touch_left_right -> TouchAreaType.BottomLeftRight
                else -> TouchAreaType.BottomLeftRight
            }
            sp.edit { putInt("sp_touch_area_type", type.ordinal) }
            dismiss()
        }
    }

    private fun initToggles() {
        val toggleTouchAreaHint = findViewById<CheckBox>(R.id.switch_show_touch_area_hint) ?: return
        val showTouchArea = findViewById<View>(R.id.show_touch_area) ?: return

        updateViewStatus(toggleTouchAreaHint, sp.getBoolean("sp_touch_area_hint", true))

        showTouchArea.setOnClickListener {
            updateBooleanPref("sp_touch_area_hint")
            updateViewStatus(toggleTouchAreaHint, sp.getBoolean("sp_touch_area_hint", false))
            dismiss()
        }
        toggleTouchAreaHint.setOnClickListener {
            updateBooleanPref("sp_touch_area_hint")
            updateViewStatus(toggleTouchAreaHint, sp.getBoolean("sp_touch_area_hint", false))
            dismiss()
        }
    }

    private fun updateBooleanPref(prefKey: String, defaultValue: Boolean = true) =
        sp.edit { putBoolean(prefKey, !sp.getBoolean(prefKey, defaultValue)) }

    private fun updateViewStatus(checkBox: CheckBox, shouldBeChecked: Boolean) {
        checkBox.isChecked = shouldBeChecked
    }
}