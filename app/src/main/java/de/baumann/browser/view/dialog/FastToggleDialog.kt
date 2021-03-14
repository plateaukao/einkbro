package de.baumann.browser.view.dialog

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
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
        context: Context,
        private val title: String,
        private val url: String,
        private val okAction: () -> Unit,
) : BottomSheetDialog(context) {
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View.inflate(context, R.layout.dialog_toggle, null))
        initViews()
    }

    private fun initViews() {
        initTitle()
        initButtons()
        initSwitches()
        initToggles()
        initOkCancelBar()
    }

    private fun initTitle() {
        val dialogTitle = findViewById<TextView>(R.id.dialog_title) ?: return
        dialogTitle.text = title
    }

    private fun initToggles() {
        val toggleHistory = findViewById<ImageButton>(R.id.toggle_history) ?: return
        val toggleHistoryView = findViewById<View>(R.id.toggle_historyView) ?: return
        val toggleLocation = findViewById<ImageButton>(R.id.toggle_location) ?: return
        val toggleLocationView = findViewById<View>(R.id.toggle_locationView) ?: return
        val toggleImages = findViewById<ImageButton>(R.id.toggle_images) ?: return
        val toggleImagesView = findViewById<View>(R.id.toggle_imagesView) ?: return
        val toggleRemote = findViewById<ImageButton>(R.id.toggle_remote) ?: return
        val toggleRemoteView = findViewById<View>(R.id.toggle_remoteView) ?: return
        val toggleDesktop = findViewById<ImageButton>(R.id.toggle_desktop) ?: return
        val toggleDesktopView = findViewById<View>(R.id.toggle_desktopView) ?: return

        updateViewVisibility(toggleHistoryView, sp.getBoolean("saveHistory", false))
        updateViewVisibility(toggleLocationView, R.string.sp_location)
        updateViewVisibility(toggleImagesView, sp.getBoolean(getString(R.string.sp_images), true))
        updateViewVisibility(toggleRemoteView, sp.getBoolean("sp_remote", true))
        updateViewVisibility(toggleDesktopView, sp.getBoolean("sp_desktop", false))

        toggleHistory.setOnClickListener {
            updateBooleanPref("saveHistory")
            updateViewVisibility(toggleHistoryView, sp.getBoolean("saveHistory", false))
        }
        toggleLocation.setOnClickListener {
            updateBooleanPref(getString(R.string.sp_location))
            updateViewVisibility(toggleLocationView, R.string.sp_location)
        }
        toggleImages.setOnClickListener {
            updateBooleanPref(getString(R.string.sp_images))
            updateViewVisibility(toggleImagesView, R.string.sp_images)
        }
        toggleRemote.setOnClickListener {
            updateBooleanPref("sp_remote")
            updateViewVisibility(toggleRemoteView, sp.getBoolean("sp_remote", true))
        }
        toggleDesktop.setOnClickListener {
            updateBooleanPref("sp_desktop")
            updateViewVisibility(toggleDesktopView, sp.getBoolean("sp_desktop", false))
        }
    }

    private fun initButtons() {
        val javaHosts = Javascript(context)
        val cookieHosts = Cookie(context)
        val adBlock = AdBlock(context)

        val btnJavaScriptWhiteList = findViewById<ImageButton>(R.id.imageButton_js) ?: return
        val btnAbWhiteList = findViewById<ImageButton>(R.id.imageButton_ab) ?: return
        val btnCookieWhiteList = findViewById<ImageButton>(R.id.imageButton_cookie) ?: return

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
        val switchJavascript = findViewById<CheckBox>(R.id.switch_js) ?: return
        val switchAdBlock = findViewById<CheckBox>(R.id.switch_adBlock) ?: return
        val switchCookie = findViewById<CheckBox>(R.id.switch_cookie) ?: return
        switchJavascript.isChecked = sp.getBoolean(getString(R.string.sp_javascript), true)
        switchAdBlock.isChecked = sp.getBoolean(getString(R.string.sp_ad_block), true)
        switchCookie.isChecked = sp.getBoolean(getString(R.string.sp_cookies), true)

        switchJavascript.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(getString(R.string.sp_javascript), isChecked).apply() }
        switchAdBlock.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(getString(R.string.sp_ad_block), isChecked).apply() }
        switchCookie.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(getString(R.string.sp_cookies), isChecked).apply() }
    }

    private fun initOkCancelBar() {
        findViewById<Button>(R.id.action_ok)?.setOnClickListener {
            dismiss()
            okAction.invoke()
        }
        findViewById<Button>(R.id.action_cancel)?.setOnClickListener { dismiss() }
    }

    private fun getString(resId: Int): String = context.getString(resId)

    private fun setImgButtonResource(imgButton: ImageButton, isEnabled: Boolean) {
        val resId = if (isEnabled) R.drawable.check_green else R.drawable.ic_action_close_red
        imgButton.setImageResource(resId)
    }

    private fun updateBooleanPref(prefKey: String) =
        sp.edit { putBoolean(prefKey, !sp.getBoolean(prefKey, true)) }

    private fun updateViewVisibility(view: View, shouldBeVisible: Boolean) {
        view.visibility = if (shouldBeVisible) VISIBLE else INVISIBLE
    }

    private fun updateViewVisibility(view: View, stringResId: Int) {
        val shouldBeVisible = sp.getBoolean(getString(stringResId), false)
        updateViewVisibility(view, shouldBeVisible)
    }
}