/*
    This file is part of the browser WebApp.

    browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */
package de.baumann.browser.unit

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Html
import android.text.SpannableString
import android.text.util.Linkify
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogActionBinding
import de.baumann.browser.Ninja.databinding.DialogHelpBinding
import java.text.SimpleDateFormat
import java.util.*

object HelperUnit {
    private const val REQUEST_CODE_ASK_PERMISSIONS = 123
    private const val REQUEST_CODE_ASK_PERMISSIONS_1 = 1234
    @JvmStatic
    fun grantPermissionsStorage(activity: Activity) {
        val hasWriteExternalStorage = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasWriteExternalStorage == PackageManager.PERMISSION_GRANTED) { return }

        if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val bottomSheetDialog = BottomSheetDialog(activity)
            val binding = DialogActionBinding.inflate(activity.layoutInflater)
            binding.dialogText.setText(R.string.toast_permission_sdCard)
            binding.actionOk.setOnClickListener {
                activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_ASK_PERMISSIONS)
                bottomSheetDialog.dismiss()
            }
            binding.actionCancel.apply {
                setText(R.string.setting_label)
                setOnClickListener {
                    val intent = Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    activity.startActivity(intent)
                    bottomSheetDialog.dismiss()
                }
            }
            bottomSheetDialog.setContentView(binding.root)
            bottomSheetDialog.show()
            setBottomSheetBehavior(bottomSheetDialog, binding.root, BottomSheetBehavior.STATE_EXPANDED)
        }
    }

    @JvmStatic
    fun grantPermissionsLoc(activity: Activity) {
        val hasAccessFineLocation = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (hasAccessFineLocation == PackageManager.PERMISSION_GRANTED) { return }

        val bottomSheetDialog = BottomSheetDialog(activity)
        val binding = DialogActionBinding.inflate(activity.layoutInflater)
        binding.dialogText.setText(R.string.toast_permission_loc)
        binding.actionOk.setOnClickListener {
            activity.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_ASK_PERMISSIONS_1)
            bottomSheetDialog.dismiss()
        }
        binding.actionCancel.apply {
            setText(R.string.setting_label)
            setOnClickListener {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
                bottomSheetDialog.dismiss()
            }
        }
        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.show()
        setBottomSheetBehavior(bottomSheetDialog, binding.root, BottomSheetBehavior.STATE_EXPANDED)
    }

    @JvmStatic
    fun applyTheme(context: Context) = context.setTheme(R.style.AppTheme)

    @JvmStatic
    fun setBottomSheetBehavior(dialog: BottomSheetDialog, view: View, beh: Int) {
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.state = beh
        mBehavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dialog.cancel()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    @JvmStatic
    fun createShortcut(context: Context, title: String?, url: String?, bitmap: Bitmap?) {
        val url = url ?: return
        val uri = convertUrlToAppScheme(url)
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = uri
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // code for adding shortcut on pre oreo device
                val installer = Intent().apply {
                    action = "com.android.launcher.action.INSTALL_SHORTCUT"
                    putExtra("android.intent.extra.shortcut.INTENT", intent)
                    putExtra("android.intent.extra.shortcut.NAME", title)
                    if (bitmap != null) {
                        putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
                    } else {
                        putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context.applicationContext, R.drawable.qc_bookmarks))
                    }
                }

                context.sendBroadcast(installer)
            } else {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
                var icon: Icon = if (bitmap != null) {
                    Icon.createWithBitmap(bitmap)
                } else {
                    Icon.createWithResource(context, R.drawable.qc_bookmarks)
                }

                if (shortcutManager.isRequestPinShortcutSupported) {
                    val pinShortcutInfo = ShortcutInfo.Builder(context, uri.toString())
                            .setShortLabel(title!!)
                            .setLongLabel(title)
                            .setIcon(icon)
                            .setIntent(Intent(Intent.ACTION_VIEW, uri))
                            .build()
                    shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                } else {
                    println("failed_to_add")
                }
            }
        } catch (e: Exception) {
            println("failed_to_add")
        }
    }

    private fun convertUrlToAppScheme(url: String): Uri {
        val originalUri = Uri.parse(url)
        val scheme = when (originalUri.scheme) {
            "https" -> "einkbros"
            "https" -> "einkbro"
            else -> originalUri.scheme
        }
        return originalUri.buildUpon().scheme(scheme).build()
    }

    fun Uri.toNormalScheme(): Uri {
        val scheme = when (scheme) {
            "einkbros" -> "https"
            "einkbro" -> "https"
            else -> scheme
        }
        return buildUpon().scheme(scheme).build()
    }

    @JvmStatic
    fun showDialogHelp(activity: Activity) {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val binding = DialogHelpBinding.inflate(activity.layoutInflater)
        binding.dialogHelpTitle.text = textSpannable(activity.resources.getString(R.string.dialogHelp_tipTitle))
        binding.dialogHelpTv.text = textSpannable(activity.resources.getString(R.string.dialogHelp_tipText))
        binding.dialogHelpTip.setOnClickListener {
            binding.dialogHelpTipView.visibility = View.VISIBLE
            binding.dialogHelpOverviewView.visibility = View.GONE
            binding.dialogHelpTitle.text = textSpannable(activity.resources.getString(R.string.dialogHelp_tipTitle))
            binding.dialogHelpTv.text = textSpannable(activity.resources.getString(R.string.dialogHelp_tipText))
        }
        binding.dialogHelpOverview.setOnClickListener {
            binding.dialogHelpTipView.visibility = View.GONE
            binding.dialogHelpOverviewView.visibility = View.VISIBLE
            binding.dialogHelpTitle.text = textSpannable(activity.resources.getString(R.string.dialogHelp_overviewTitle))
            binding.dialogHelpTv.text = textSpannable(activity.resources.getString(R.string.dialogHelp_overviewText))
        }
        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.show()
        setBottomSheetBehavior(bottomSheetDialog, binding.root, BottomSheetBehavior.STATE_EXPANDED)
    }

    @JvmStatic
    fun fileName(url: String?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        val domain = Uri.parse(url).host?.replace("www.", "")?.trim { it <= ' ' } ?: ""
        return domain.replace(".", "_").trim { it <= ' ' } + "_" + currentTime.trim { it <= ' ' }
    }

    @JvmStatic
    fun secString(string: String?): String = string?.replace("'".toRegex(), "\'\'") ?: "No title"

    @JvmStatic
    fun domain(url: String?): String {
        return if (url == null) {
            ""
        } else {
            try {
                Uri.parse(url).host?.replace("www.", "")?.trim { it <= ' ' } ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    @JvmStatic
    fun textSpannable(text: String?): SpannableString {
        val s: SpannableString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SpannableString(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY))
        } else {
            SpannableString(Html.fromHtml(text))
        }
        Linkify.addLinks(s, Linkify.WEB_URLS)
        return s
    }

    private val NEGATIVE_COLOR = floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f, 0f, -1.0f, 0f, 0f, 255f, 0f, 0f, -1.0f, 0f, 255f, 0f, 0f, 0f, 1.0f, 0f)

    @JvmStatic
    fun initRendering(view: View, shouldInvert: Boolean) {
        if (shouldInvert) {
            val paint = Paint()
            val matrix = ColorMatrix()
            matrix.set(NEGATIVE_COLOR)
            val gcm = ColorMatrix()
            gcm.setSaturation(0f)
            val concat = ColorMatrix()
            concat.setConcat(matrix, gcm)
            val filter = ColorMatrixColorFilter(concat)
            paint.colorFilter = filter
            view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        } else {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }

    fun openFile(activity: Activity, uri: Uri, mimeType: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        activity.startActivity(Intent.createChooser(intent, "Open file with"))
    }

}