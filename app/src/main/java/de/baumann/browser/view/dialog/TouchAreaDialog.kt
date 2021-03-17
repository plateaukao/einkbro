package de.baumann.browser.view.dialog

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.browser.AdBlock
import de.baumann.browser.browser.Cookie
import de.baumann.browser.browser.Javascript
import de.baumann.browser.unit.HelperUnit

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
        initToggles()
        initOk()
    }

    private fun initToggles() {
        val toggleTouchAreaHint = findViewById<CheckBox>(R.id.switch_show_touch_area_hint) ?: return
        val showTouchArea = findViewById<View>(R.id.text_show_touch_area) ?: return

        updateViewStatus(toggleTouchAreaHint, sp.getBoolean("sp_touch_area_hint", true))

        showTouchArea.setOnClickListener {
            updateBooleanPref("sp_touch_area_hint")
            updateViewStatus(toggleTouchAreaHint, sp.getBoolean("sp_touch_area_hint", false))
        }
        toggleTouchAreaHint.setOnClickListener {
            updateBooleanPref("sp_touch_area_hint")
            updateViewStatus(toggleTouchAreaHint, sp.getBoolean("sp_touch_area_hint", false))
        }
    }

    private fun initOk() {
        findViewById<Button>(R.id.action_close)?.setOnClickListener { dismiss() }
    }

    //private fun getString(resId: Int): String = context.getString(resId)

    private fun updateBooleanPref(prefKey: String, defaultValue: Boolean = true) =
        sp.edit { putBoolean(prefKey, !sp.getBoolean(prefKey, defaultValue)) }

    private fun updateViewStatus(checkBox: CheckBox, shouldBeChecked: Boolean) {
        checkBox.isChecked = shouldBeChecked
    }
}