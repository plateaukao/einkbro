package de.baumann.browser.view.dialog

import android.app.Activity
import android.app.Dialog
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogEditExtensionBinding
import de.baumann.browser.Ninja.databinding.DialogMenuContextLinkBinding
import de.baumann.browser.Ninja.databinding.DialogSavedEpubListBinding
import de.baumann.browser.Ninja.databinding.ListItemEpubFileBinding
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.*
import de.baumann.browser.view.NinjaToast
import de.baumann.browser.view.NinjaWebView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class DialogManager(
    private val activity: Activity
) : KoinComponent {
    private val config: ConfigManager by inject()
    private val inflater = LayoutInflater.from(activity)

    fun showSaveEpubDialog(shouldAddNewEpub: Boolean = true, onNextAction: (Uri?) -> Unit) {
        val binding = DialogSavedEpubListBinding.inflate(inflater)
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .apply { setView(binding.root) }
            .show()

        setupSavedEpubFileList(binding, dialog, shouldAddNewEpub, onNextAction)
    }

    private fun setupSavedEpubFileList(
        binding: DialogSavedEpubListBinding,
        dialog: Dialog,
        shouldAddNewEpub: Boolean = true,
        onNextAction: (Uri?) -> Unit
    ) {
        if (shouldAddNewEpub) {
            // add first item to handle picker case
            val firstItemBinding = ListItemEpubFileBinding.inflate(inflater).apply {
                buttonHide.visibility = View.GONE
                epubTitle.setText(R.string.new_epub_or_from_picker)
                root.setOnClickListener {
                    onNextAction(null)
                    dialog.dismiss()
                }
            }
            binding.epubInfoContainer.addView(firstItemBinding.root)
        }

        config.savedEpubFileInfos.reversed().forEach { epubFileInfo ->
            val itemBinding = ListItemEpubFileBinding.inflate(inflater)
            with(itemBinding.epubTitle) {
                text = "${epubFileInfo.title} (${getFileSizeString(epubFileInfo.uri)})"
                setOnClickListener {
                    onNextAction(epubFileInfo.uri.toUri())
                    dialog.dismiss()
                }
            }
            itemBinding.buttonHide.setOnClickListener {
                config.removeSavedEpubFile(epubFileInfo)
                binding.epubInfoContainer.removeView(itemBinding.root)
            }

            binding.epubInfoContainer.addView(itemBinding.root)
        }
    }

    private fun getFileSizeString(uri: String): String {
        val sizeInBytes = DocumentFile.fromSingleUri(activity, Uri.parse(uri))?.length() ?: 0
        val sizeInKB = sizeInBytes / 1024
        val sizeInMB = sizeInKB / 1024F
        return if (sizeInMB > 1) "%.1fMB".format(sizeInMB) else "${sizeInKB}KB"
    }

    private fun showSaveFileDialog(
        url: String,
        saveFile: (String, String) -> Unit,
    ) {
        val menuView = DialogEditExtensionBinding.inflate(inflater)
        val editTitle = menuView.dialogEdit.apply {
            setHint(R.string.dialog_title_hint)
            setText(HelperUnit.fileName(url))
        }
        val editExtension = menuView.dialogEditExtension
        val filename = URLUtil.guessFileName(url, null, null)
        val extension = filename.substring(filename.lastIndexOf("."))
        if (extension.length <= 8) {
            editExtension.setText(extension)
        }
        showOkCancelDialog(
            title = activity.getString(R.string.menu_edit),
            view = menuView.root,
            okAction = {
                val title = editTitle.text.toString().trim { it <= ' ' }
                val ext = editExtension.text.toString().trim { it <= ' ' }
                val fileName = title + ext
                if (title.isEmpty() || ext.isEmpty() || !extension.startsWith(".")) {
                    NinjaToast.show(activity, activity.getString(R.string.toast_input_empty))
                } else {
                    saveFile(url, fileName)
                }
            },
            cancelAction = { ViewUnit.hideKeyboard(activity) }
        )
    }

    fun showContextMenuLinkDialog(
        webView: NinjaWebView,
        message: Message,
        newTabInBkndAction: (String, String) -> Unit, // title
        splitScreenAction: (String) -> Unit,
        shareAction: (String) -> Unit,
        saveBookmarkAction: (String, String) -> Unit, // title
        newTabAction: (String, String) -> Unit, // title
        saveFileAction: (String, String) -> Unit,
        confirmAdSiteAddition: (String) -> Unit
    ) {
        val hitTestResult = webView.hitTestResult

        if (!listOf(WebView.HitTestResult.IMAGE_TYPE,
                WebView.HitTestResult.IMAGE_ANCHOR_TYPE,
                WebView.HitTestResult.SRC_ANCHOR_TYPE,
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                WebView.HitTestResult.ANCHOR_TYPE)
                .contains(hitTestResult.type)) return

        val linkUrl = message.data.getString("url")
        val imgUrl = message.data.getString("src")
        val nonNullUrl = linkUrl ?: imgUrl ?: return


        var titleText = ""
        val newMessage = Message().apply {
            target = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    titleText = (msg.data.getString("title") ?: nonNullUrl).replace("\n", "").trim()
                }
            }
        }
        webView.requestFocusNodeHref(newMessage)

        val dialogView = DialogMenuContextLinkBinding.inflate(activity.layoutInflater)
        val dialog = showOptionDialog(dialogView.root)
        dialogView.contextLinkUrl.text = nonNullUrl
        dialogView.contextLinkNewTab.setOnClickListener { dialog.dismissWithAction { newTabInBkndAction(titleText, nonNullUrl) } }
        dialogView.contextLinkSplitScreen.setOnClickListener { dialog.dismissWithAction { splitScreenAction(nonNullUrl) } }
        dialogView.contextLinkShareLink.setOnClickListener { dialog.dismissWithAction { shareAction(nonNullUrl) } }
        dialogView.contextLinkCopyLink.setOnClickListener { dialog.dismissWithAction { ShareUtil.copyToClipboard(activity, nonNullUrl) } }
        dialogView.contextLinkOpenWith.setOnClickListener { dialog.dismissWithAction { HelperUnit.showBrowserChooser(activity, nonNullUrl, activity.getString(R.string.menu_open_with)) } }
        dialogView.contextLinkSaveBookmark.setOnClickListener { dialog.dismissWithAction { saveBookmarkAction(titleText, nonNullUrl) } }
        dialogView.contextLinkNewTabOpen.setOnClickListener { dialog.dismissWithAction { newTabAction(titleText, nonNullUrl) } }
        dialogView.menuSaveFile.setOnClickListener {
            dialog.dismissWithAction {
                if (nonNullUrl.startsWith("data:")) {
                    NinjaToast.showShort(activity, "Not supported for data:image urld")
                } else {
                    if (listOf(WebView.HitTestResult.IMAGE_TYPE,
                            WebView.HitTestResult.IMAGE_ANCHOR_TYPE,
                            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)
                            .contains(hitTestResult.type)) {
                        showSaveFileDialog(url = imgUrl ?: "", saveFile = saveFileAction)
                    } else {
                        showSaveFileDialog(url = linkUrl ?: "", saveFile = saveFileAction)
                    }
                }
            }
        }

        if (hitTestResult.extra != null) {
            dialogView.menuRemoveAd.visibility = View.VISIBLE
            dialogView.menuRemoveAd.setOnClickListener {
                dialog.dismissWithAction { confirmAdSiteAddition(hitTestResult.extra ?: "") }
            }
        }
    }

    fun showOkCancelDialog(
        title: String? = null,
        messageResId: Int? = null,
        message: String? = null,
        view: View? = null,
        okAction: () -> Unit,
        cancelAction: (() -> Unit)? = null,
        showInCenter: Boolean = false,
        showNegativeButton: Boolean = true
    ): Dialog {
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .setPositiveButton(android.R.string.ok) { _, _ -> okAction() }
            .apply {
                title?.let { title -> setTitle(title) }
                view?.let { setView(it) }
                messageResId?.let { setMessage(messageResId) }
                message?.let { setMessage(message) }
                if (showNegativeButton) {
                    setNegativeButton(android.R.string.cancel) { _, _ -> cancelAction?.invoke() }
                }
            }
            .create().apply {
                window?.setGravity(if (config.isToolbarOnTop || showInCenter) Gravity.CENTER else Gravity.BOTTOM)
                window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            }
        dialog.show()
        return dialog
    }

    fun showOptionDialog(
        view: View
    ): Dialog {
        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .setView(view)
            .create().apply {
                window?.setGravity(if (config.isToolbarOnTop) Gravity.CENTER else Gravity.BOTTOM)
                window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
            }
        dialog.show()
        return dialog
    }

    suspend fun getBookmarkFolderName(): String? =
        TextInputDialog(
            activity,
            activity.getString(R.string.folder_name),
            activity.getString(R.string.folder_name_description),
            ""
        ).show()
}

fun Dialog.dismissWithAction(action: () -> Unit) {
    dismiss()
    action()
}
