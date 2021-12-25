package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogTouchAreaBinding
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.preference.TouchAreaType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.zip.Inflater

class TouchAreaDialog(private val context: Context) : KoinComponent {
    private val sp: SharedPreferences by inject()
    private val config: ConfigManager by inject()

    private lateinit var dialog: AlertDialog

    fun show() {
        val binding = DialogTouchAreaBinding.inflate(LayoutInflater.from(context))
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(binding.root) }

        initViews(binding)
        dialog = builder.create().apply {
            window?.setGravity(Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
        }
        dialog.show()
    }

    private fun initViews(binding: DialogTouchAreaBinding) {
        // init touch area types
        initHintTypes(binding)
        initToggles(binding)
    }

    private fun initHintTypes(binding: DialogTouchAreaBinding) {
        // UI setup
        val buttonShouldBeChecked: RadioButton = when (sp.getInt("sp_touch_area_type", 0)) {
            0 -> binding.touchLeftRight
            1 -> binding.touchAreaBottomLeft
            2 -> binding.touchAreaBottomRight
            3 -> binding.touchMiddleLeftRight
            4 -> binding.touchMiddleLeftRight
            else -> binding.touchLeftRight
        }
        buttonShouldBeChecked.isChecked = true

        // action
        binding.touchLeftRight.setOnClickListener {
            config.touchAreaType = TouchAreaType.BottomLeftRight
            dialog.dismiss()
        }
        binding.touchAreaBottomLeft.setOnClickListener {
            config.touchAreaType = TouchAreaType.Left
            dialog.dismiss()
        }
        binding.touchAreaBottomRight.setOnClickListener {
            config.touchAreaType = TouchAreaType.Right
            dialog.dismiss()
        }
        binding.touchMiddleLeftRight.setOnClickListener {
            config.touchAreaType = TouchAreaType.MiddleLeftRight
            dialog.dismiss()
        }
    }

    private fun initToggles(binding: DialogTouchAreaBinding) {
        val toggleTouchAreaHint = binding.switchShowTouchAreaHint
        val showTouchArea = binding.showTouchArea

        updateViewStatus(toggleTouchAreaHint, config.touchAreaHint)
        updateViewStatus(binding.cbHideTouchAreaWhenInput, config.hideTouchAreaWhenInput)
        updateViewStatus(binding.cbSwitchTouchAreaAction, config.switchTouchAreaAction)

        showTouchArea.setOnClickListener {
            config.touchAreaHint = !config.touchAreaHint
            updateViewStatus(toggleTouchAreaHint, config.touchAreaHint)
            dialog.dismiss()
        }
        binding.hideTouchAreaWhenInput.setOnClickListener {
            config.hideTouchAreaWhenInput= !config.hideTouchAreaWhenInput
            updateViewStatus(binding.cbHideTouchAreaWhenInput, config.hideTouchAreaWhenInput)
            dialog.dismiss()
        }
        binding.switchTouchAreaAction.setOnClickListener {
            config.switchTouchAreaAction= !config.switchTouchAreaAction
            updateViewStatus(binding.cbSwitchTouchAreaAction, config.switchTouchAreaAction)
            dialog.dismiss()
        }
    }

    private fun updateViewStatus(checkBox: CheckBox, shouldBeChecked: Boolean) {
        checkBox.isChecked = shouldBeChecked
    }
}