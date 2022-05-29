package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import android.view.LayoutInflater
import androidx.core.content.edit
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogToggleBinding
import de.baumann.browser.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FastToggleDialog(
        private val context: Context,
        private val okAction: () -> Unit,
) : KoinComponent {
    private val sp: SharedPreferences by inject()
    private val config: ConfigManager by inject()
    private lateinit var dialog: AlertDialog
    private lateinit var binding: DialogToggleBinding

    fun show() {
        binding = DialogToggleBinding.inflate(LayoutInflater.from(context))
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(binding.root) }

        initViews()
        dialog = builder.create().apply {
            window?.setGravity(if (config.isToolbarOnTop) Gravity.CENTER else Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
        }
        dialog.show()
    }

    private fun initViews() {
        initToggles()
        initOkCancelBar()
    }

    private fun initToggles() {
        binding.switchHistory.isChecked = config.saveHistory
        binding.switchLocation.isChecked = sp.getBoolean("SP_LOCATION_9", false)
        binding.switchMediaContinue.isChecked = config.continueMedia
        binding.switchDesktop.isChecked = config.desktop
        binding.switchVolume.isChecked = config.volumePageTurn

        binding.toggleHistoryContainer.setOnClickListener {
            config.saveHistory = !config.saveHistory
            binding.switchHistory.isChecked = config.saveHistory
            dialog.dismiss()
        }
        binding.toggleLocationContainer.setOnClickListener {
            updateBooleanPref("SP_LOCATION_9", false)
            binding.switchLocation.isChecked = sp.getBoolean("SP_LOCATION_9", false)
            dialog.dismiss()
        }
        binding.toggleVolumeContainer.setOnClickListener {
            config.volumePageTurn = !config.volumePageTurn
            binding.switchVolume.isChecked = config.volumePageTurn
            dialog.dismiss()
        }
        binding.toggleBackgroundPlayContainer.setOnClickListener {
            config.continueMedia = !config.continueMedia
            binding.switchMediaContinue.isChecked = config.continueMedia
            dialog.dismiss()
        }
        binding.toggleDesktopContainer.setOnClickListener {
            config.desktop = !config.desktop
            binding.switchDesktop.isChecked = config.desktop
            dialog.dismiss()
        }

        binding.switchIncognito.isChecked = config.isIncognitoMode
        binding.switchAdBlock.isChecked = sp.getBoolean(getString(R.string.sp_ad_block), true)
        binding.switchCookie.isChecked = sp.getBoolean(getString(R.string.sp_cookies), true)
        binding.switchJavascript.isChecked = config.enableJavascript

        binding.toggleJavascriptContainer.setOnClickListener {
            config.enableJavascript = !config.enableJavascript
            binding.switchJavascript.isChecked = config.enableJavascript
            okAction.invoke()
            dialog.dismiss()
        }

        binding.toggleIncognitoContainer.setOnClickListener {
            config.isIncognitoMode = !config.isIncognitoMode
            config.cookies = !config.isIncognitoMode
            config.saveHistory = !config.isIncognitoMode
            binding.switchIncognito.isChecked = config.isIncognitoMode

            okAction.invoke()
            dialog.dismiss()
        }

        binding.toggleAdblockContainer.setOnClickListener {
            updateBooleanPref("SP_AD_BLOCK_9", true)
            binding.switchAdBlock.isChecked = sp.getBoolean(getString(R.string.sp_ad_block), true)
            okAction.invoke()
            dialog.dismiss()
        }
        binding.toggleCookiesContainer.setOnClickListener {
            updateBooleanPref("SP_AD_COOKIES_9", true)
            binding.switchCookie.isChecked = sp.getBoolean(getString(R.string.sp_cookies), true)
            okAction.invoke()
            dialog.dismiss()
        }
    }

    private fun initOkCancelBar() {
        binding.actionCancel.setOnClickListener { dialog.dismiss() }
    }

    private fun getString(resId: Int): String = context.getString(resId)

    private fun updateBooleanPref(prefKey: String, defaultValue: Boolean = true) =
            sp.edit { putBoolean(prefKey, !sp.getBoolean(prefKey, defaultValue)) }
}