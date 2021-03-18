package de.baumann.browser.view.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.browser.AdBlock
import de.baumann.browser.browser.Cookie
import de.baumann.browser.browser.Javascript
import de.baumann.browser.unit.HelperUnit

class FastToggleDialog(
        private val context: Context,
        private val title: String,
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
        }
        dialog.show()
    }

    private fun initViews() {
        initTitle()
        initButtons()
        initSwitches()
        initToggles()
        initOkCancelBar()
    }

    private fun initTitle() {
        val dialogTitle = view.findViewById<TextView>(R.id.dialog_title) ?: return
        dialogTitle.text = title
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

        updateViewVisibility(toggleHistoryView, sp.getBoolean("saveHistory", false))
        updateViewVisibility(toggleLocationView, R.string.sp_location)
        updateViewVisibility(toggleImagesView, sp.getBoolean(getString(R.string.sp_images), true))
        updateViewVisibility(toggleMediaContinueView, sp.getBoolean("sp_media_continue", false))
        updateViewVisibility(toggleDesktopView, sp.getBoolean("sp_desktop", false))

        toggleHistory.setOnClickListener {
            updateBooleanPref("saveHistory")
            updateViewVisibility(toggleHistoryView, sp.getBoolean("saveHistory", false))
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
        val javaHosts = Javascript(context)
        val cookieHosts = Cookie(context)
        val adBlock = AdBlock(context)

        val btnJavaScriptWhiteList = view.findViewById<ImageButton>(R.id.imageButton_js) ?: return
        val btnAbWhiteList = view.findViewById<ImageButton>(R.id.imageButton_ab) ?: return
        val btnCookieWhiteList = view.findViewById<ImageButton>(R.id.imageButton_cookie) ?: return

        setImgButtonResource(btnJavaScriptWhiteList, javaHosts.isWhite(url))
        setImgButtonResource(btnCookieWhiteList, cookieHosts.isWhite(url))
        setImgButtonResource(btnAbWhiteList, adBlock.isWhite(url))

        btnJavaScriptWhiteList.setOnClickListener {
            if (javaHosts.isWhite(url)) {
                javaHosts.removeDomain(HelperUnit.domain(url))
            } else {
                javaHosts.addDomain(HelperUnit.domain(url))
            }
            setImgButtonResource(btnJavaScriptWhiteList, javaHosts.isWhite(url))
        }
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
    }

    private fun initSwitches() {
        val switchJavascript = view.findViewById<CheckBox>(R.id.switch_js) ?: return
        val switchAdBlock = view.findViewById<CheckBox>(R.id.switch_adBlock) ?: return
        val switchCookie = view.findViewById<CheckBox>(R.id.switch_cookie) ?: return
        switchJavascript.isChecked = sp.getBoolean(getString(R.string.sp_javascript), true)
        switchAdBlock.isChecked = sp.getBoolean(getString(R.string.sp_ad_block), true)
        switchCookie.isChecked = sp.getBoolean(getString(R.string.sp_cookies), true)

        switchJavascript.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(getString(R.string.sp_javascript), isChecked).apply() }
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