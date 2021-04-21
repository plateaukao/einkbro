package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import de.baumann.browser.Ninja.R
import de.baumann.browser.browser.AdBlock
import de.baumann.browser.browser.Cookie
import de.baumann.browser.unit.HelperUnit

class FastToggleDialog(
        private val context: Context,
        private val url: String,
        private val okAction: () -> Unit,
) {
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private lateinit var dialog: AlertDialog
    private lateinit var view: View


    fun show() {
        view = View.inflate(context, R.layout.dialog_toggle, null)
        val builder = AlertDialog.Builder(context, R.style.TouchAreaDialog).apply { setView(view) }

        initViews()
        dialog = builder.create().apply {
            window?.setGravity(Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(R.drawable.background_with_margin)
        }
        dialog.show()
    }

    private fun initViews() {
        initButtons()
        initSwitches()
        initToggles()
        initOkCancelBar()
    }

    private fun initToggles() {
        val toggleHistory = view.findViewById<ImageButton>(R.id.toggle_history) ?: return
        val toggleHistoryView = view.findViewById<View>(R.id.toggle_historyView) ?: return
        val toggleLocation = view.findViewById<ImageButton>(R.id.toggle_location) ?: return
        val toggleLocationView = view.findViewById<View>(R.id.toggle_locationView) ?: return
        val toggleImages = view.findViewById<ImageButton>(R.id.toggle_images) ?: return
        val toggleImagesView = view.findViewById<View>(R.id.toggle_imagesView) ?: return
        val toggleMediaContinue = view.findViewById<ImageButton>(R.id.toggle_media_continue) ?: return
        val toggleMediaContinueView = view.findViewById<View>(R.id.toggle_media_continue_view) ?: return
        val toggleDesktop = view.findViewById<ImageButton>(R.id.toggle_desktop) ?: return
        val toggleDesktopView = view.findViewById<View>(R.id.toggle_desktopView) ?: return

        updateViewVisibility(toggleHistoryView, sp.getBoolean("saveHistory", true))
        updateViewVisibility(toggleLocationView, R.string.sp_location)
        updateViewVisibility(toggleImagesView, sp.getBoolean(getString(R.string.sp_images), true))
        updateViewVisibility(toggleMediaContinueView, sp.getBoolean("sp_media_continue", false))
        updateViewVisibility(toggleDesktopView, sp.getBoolean("sp_desktop", false))

        toggleHistory.setOnClickListener {
            updateBooleanPref("saveHistory")
            updateViewVisibility(toggleHistoryView, sp.getBoolean("saveHistory", true))
        }
        toggleLocation.setOnClickListener {
            updateBooleanPref(getString(R.string.sp_location), false)
            updateViewVisibility(toggleLocationView, R.string.sp_location)
        }
        toggleImages.setOnClickListener {
            updateBooleanPref(getString(R.string.sp_images))
            updateViewVisibility(toggleImagesView, R.string.sp_images)
        }
        toggleMediaContinue.setOnClickListener {
            updateBooleanPref("sp_media_continue", false)
            updateViewVisibility(toggleMediaContinueView, sp.getBoolean("sp_media_continue", false))
        }
        toggleDesktop.setOnClickListener {
            updateBooleanPref("sp_desktop", false)
            updateViewVisibility(toggleDesktopView, sp.getBoolean("sp_desktop", false))
        }
    }

    private fun initButtons() {
        /*
        val cookieHosts = Cookie(context)
        val adBlock = AdBlock(context)

        val btnAbWhiteList = view.findViewById<ImageButton>(R.id.imageButton_ab) ?: return
        val btnCookieWhiteList = view.findViewById<ImageButton>(R.id.imageButton_cookie) ?: return

        setImgButtonResource(btnCookieWhiteList, cookieHosts.isWhite(url))
        setImgButtonResource(btnAbWhiteList, adBlock.isWhite(url))

        btnCookieWhiteList.setOnClickListener {
            if (cookieHosts.isWhite(url)) {
                cookieHosts.removeDomain(HelperUnit.domain(url))
            } else {
                cookieHosts.addDomain(HelperUnit.domain(url))
            }
            setImgButtonResource(btnCookieWhiteList, cookieHosts.isWhite(url))
        }
        btnAbWhiteList.setOnClickListener {
            if (adBlock.isWhite(url)) {
                adBlock.removeDomain(HelperUnit.domain(url))
            } else {
                adBlock.addDomain(HelperUnit.domain(url))
            }
            setImgButtonResource(btnAbWhiteList, adBlock.isWhite(url))
        }
         */
    }

    private fun initSwitches() {
        val switchAdBlock = view.findViewById<CheckBox>(R.id.switch_adBlock) ?: return
        val switchCookie = view.findViewById<CheckBox>(R.id.switch_cookie) ?: return
        switchAdBlock.isChecked = sp.getBoolean(getString(R.string.sp_ad_block), true)
        switchCookie.isChecked = sp.getBoolean(getString(R.string.sp_cookies), true)

        switchAdBlock.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(getString(R.string.sp_ad_block), isChecked).apply() }
        switchCookie.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(getString(R.string.sp_cookies), isChecked).apply() }
    }

    private fun initOkCancelBar() {
        view.findViewById<Button>(R.id.action_ok)?.setOnClickListener {
            dialog.dismiss()
            okAction.invoke()
        }
        view.findViewById<Button>(R.id.action_cancel)?.setOnClickListener { dialog.dismiss() }
    }

    private fun getString(resId: Int): String = context.getString(resId)

    private fun setImgButtonResource(imgButton: ImageButton, isEnabled: Boolean) {
        val resId = if (isEnabled) R.drawable.check_green else R.drawable.ic_action_close_red
        imgButton.setImageResource(resId)
    }

    private fun updateBooleanPref(prefKey: String, defaultValue: Boolean = true) =
        sp.edit { putBoolean(prefKey, !sp.getBoolean(prefKey, defaultValue)) }

    private fun updateViewVisibility(view: View, shouldBeVisible: Boolean) {
        view.visibility = if (shouldBeVisible) VISIBLE else INVISIBLE
    }

    private fun updateViewVisibility(view: View, stringResId: Int) {
        val shouldBeVisible = sp.getBoolean(getString(stringResId), false)
        updateViewVisibility(view, shouldBeVisible)
    }
}