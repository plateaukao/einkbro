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
package info.plateaukao.einkbro.unit

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.text.AnnotatedString
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.EpubReaderActivity
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.dialog.DialogManager


object HelperUnit {
    private const val REQUEST_CODE_ASK_PERMISSIONS = 123
    private const val REQUEST_CODE_ASK_PERMISSIONS_1 = 1234

    // --- Forwarding functions for MarkdownParser ---
    fun parseMarkdown(markdownText: String): AnnotatedString =
        MarkdownParser.parseMarkdown(markdownText)

    // --- Forwarding functions for AppUpdater ---
    suspend fun upgradeToLatestRelease(
        context: Context,
        progressCallback: (suspend (Float) -> Unit)? = null,
    ) = AppUpdater.upgradeToLatestRelease(context, progressCallback)

    suspend fun upgradeFromSnapshot(
        context: Context,
        progressCallback: (suspend (Float) -> Unit)? = null,
    ) = AppUpdater.upgradeFromSnapshot(context, progressCallback)

    // --- Forwarding functions for FileHelper ---
    fun getStringFromAsset(fileName: String): String =
        FileHelper.getStringFromAsset(fileName)

    fun getCachedPathFromURI(context: Context, contentURI: Uri): String =
        FileHelper.getCachedPathFromURI(context, contentURI)

    fun readContentAsStringList(contentResolver: ContentResolver, contentUri: Uri): List<String> =
        FileHelper.readContentAsStringList(contentResolver, contentUri)

    fun getFileInfoFromContentUri(context: Context, contentUri: Uri): Pair<String?, String?> =
        FileHelper.getFileInfoFromContentUri(context, contentUri)

    fun loadAssetFileToString(context: Context, filename: String): String =
        FileHelper.loadAssetFileToString(context, filename)

    fun loadAssetFile(fileName: String): String =
        FileHelper.loadAssetFile(fileName)

    @JvmStatic
    fun fileName(url: String?): String =
        FileHelper.fileName(url)

    // --- Remaining functions ---

    @JvmStatic
    // return true if need permissions
    fun needGrantStoragePermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT in 23..28) {
            val hasWriteExternalStoragePermission =
                activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_ASK_PERMISSIONS
                )
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun grantPermissionsMicrophone(activity: Activity) {
        val hasRecordAudioPermission =
            activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        if (hasRecordAudioPermission != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_ASK_PERMISSIONS_1
            )
        }
    }

    @JvmStatic
    fun grantPermissionsLoc(activity: Activity) {
        val hasAccessFineLocation =
            activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (hasAccessFineLocation != PackageManager.PERMISSION_GRANTED) {
            DialogManager(activity).showOkCancelDialog(
                messageResId = R.string.setting_summary_location,
                okAction = {
                    activity.requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_CODE_ASK_PERMISSIONS_1
                    )
                }
            )
        }
    }

    @JvmStatic
    fun applyTheme(context: Context) = context.setTheme(R.style.AppTheme)

    @JvmStatic
    fun setBottomSheetBehavior(dialog: BottomSheetDialog, view: View, beh: Int) {
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.state = beh
        mBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dialog.cancel()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun createShortcut(context: Context, title: String?, url: String?, bitmap: Bitmap?) {
        val url = url ?: return
        val uri = convertUrlToAppScheme(url)
        try {
            val intent =
                context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    action = Intent.ACTION_VIEW
                    data = uri
                } ?: return

            val shortcutManager =
                context.getSystemService(ShortcutManager::class.java) ?: return
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
                    .setIntent(intent)
                    .build()
                shortcutManager.requestPinShortcut(pinShortcutInfo, null)
            } else {
                println("failed_to_add")
            }
        } catch (e: Exception) {
            println("failed_to_add")
        }
    }

    private fun convertUrlToAppScheme(url: String): Uri {
        val originalUri = Uri.parse(url)
        val scheme = when (originalUri.scheme) {
            "https" -> "einkbros"
            "http" -> "einkbro"
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

    fun srtToHtml(lines: List<String>): String {
        val subtitles = mutableListOf<String>()

        for (line in lines) {
            if (line.isBlank() ||
                line.matches(Regex("\\d+")) ||
                line.contains(" --> ")
            ) {
            } else {
                subtitles.add(line)
            }
        }

        return subtitles.joinToString(separator = "") { "<p>${it}</p>" }
    }

    @JvmStatic
    fun secString(string: String?): String = string?.replace("'".toRegex(), "\'\'") ?: "No title"

    @JvmStatic
    fun domain(url: String?): String {
        return if (url == null) {
            ""
        } else {
            try {
                Uri.parse(url).host?.replace("www.", "")?.trim { it <= ' ' }.orEmpty()
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun openEpubToLastChapter(activity: Activity, uri: Uri) {
        openFile(activity, uri, shouldGoToEnd = true)
    }

    fun openFile(
        activity: Activity,
        uri: Uri,
        resultLauncher: ActivityResultLauncher<Intent>? = null,
        shouldGoToEnd: Boolean = false,
    ) {
        // SAF returns a content:// URI whose path is a numeric document ID,
        // so readers that sniff the file extension from the path don't appear
        // in the chooser. Re-expose via FileProvider with the original filename
        // so the path ends in .epub/.pdf and matches their intent filters.
        val effectiveUri = reExposeWithFilename(activity, uri) ?: uri
        val mimeType = resolveMimeType(activity, effectiveUri)
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            if (mimeType != null) setDataAndType(effectiveUri, mimeType) else data = effectiveUri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (shouldGoToEnd) {
            intent.putExtra(EpubReaderActivity.ARG_TO_LAST_CHAPTER, true)
        }

        try {
            val chooser = Intent.createChooser(intent, "Open file with")
            buildSupernoteOpenIntent(activity, uri)?.let { extra ->
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(extra))
            }
            activity.startActivity(chooser)
        } catch (exception: SecurityException) {
            EBToast.show(activity, "open file failed, re-select the file again.")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = Constants.MIME_TYPE_ANY
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra("android.provider.extra.INITIAL_URI", uri);
            resultLauncher?.launch(intent)
        }
    }

    private fun resolveMimeType(activity: Activity, uri: Uri): String? {
        val path = (uri.lastPathSegment ?: uri.toString()).lowercase()
        return when {
            path.endsWith(".epub") -> "application/epub+zip"
            path.endsWith(".pdf") -> "application/pdf"
            else -> activity.contentResolver.getType(uri)
        }
    }

    private fun reExposeWithFilename(activity: Activity, uri: Uri): Uri? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        val displayName = queryDisplayName(activity, uri) ?: return null
        val lower = displayName.lowercase()
        if (!lower.endsWith(".epub") && !lower.endsWith(".pdf")) return null
        return try {
            val sharedDir = File(activity.cacheDir, "shared").apply { mkdirs() }
            val target = File(sharedDir, displayName)
            activity.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                target,
            )
        } catch (e: Exception) {
            null
        }
    }

    private const val SUPERNOTE_DOCUMENT_PACKAGE = "com.supernote.document"
    private const val SUPERNOTE_INBOX_PACKAGE = "com.ratta.supernote.inbox"
    private const val SUPERNOTE_INBOX_ACTIVITY =
        "com.ratta.supernote.inbox.InBoxMainActivity"

    fun isSupernoteDocumentInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(SUPERNOTE_DOCUMENT_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun isSupernoteInboxInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(SUPERNOTE_INBOX_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun supernoteDocumentInitialUri(): Uri =
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocument")

    /**
     * Supernote's reader registers no VIEW intent filter, so we can't reach
     * it through the normal chooser. Inject a labelled entry that targets
     * MainActivity (exported, singleTask) with `file_path` set — its
     * onCreate forwards to the non-exported DocumentActivity for that file.
     * Falls back to the Inbox library list when the SAF URI can't be mapped
     * to an absolute path.
     */
    private fun buildSupernoteOpenIntent(activity: Activity, uri: Uri): Intent? {
        if (!isSupernoteDocumentInstalled(activity)) return null
        val absolutePath = resolveExternalStoragePath(uri)
        return if (absolutePath != null) {
            val open = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(
                    SUPERNOTE_DOCUMENT_PACKAGE,
                    "$SUPERNOTE_DOCUMENT_PACKAGE.MainActivity",
                )
                putExtra("file_path", absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            LabeledIntent(open, SUPERNOTE_DOCUMENT_PACKAGE, "Supernote", 0)
        } else if (isSupernoteInboxInstalled(activity)) {
            val launch = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(SUPERNOTE_INBOX_PACKAGE, SUPERNOTE_INBOX_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            LabeledIntent(launch, SUPERNOTE_INBOX_PACKAGE, "Supernote", 0)
        } else {
            null
        }
    }

    private fun resolveExternalStoragePath(uri: Uri): String? {
        if (uri.authority != "com.android.externalstorage.documents") return null
        val docId = try {
            DocumentsContract.getDocumentId(uri)
        } catch (_: IllegalArgumentException) {
            return null
        } ?: return null
        val (volume, relative) = docId.split(":", limit = 2)
            .takeIf { it.size == 2 } ?: return null
        return if (volume.equals("primary", ignoreCase = true)) {
            "/storage/emulated/0/$relative"
        } else {
            "/storage/$volume/$relative"
        }
    }

    private fun queryDisplayName(activity: Activity, uri: Uri): String? {
        return try {
            activity.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).takeIf { !it.isNullOrBlank() } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun showBrowserChooser(activity: Activity, url: String?, title: String) {
        val nonNullUrl = url ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nonNullUrl))
        activity.startActivity(Intent.createChooser(intent, title))
    }

    /**
     * re-implementation of org.apache.commons.text.StringEscapeUtils.unescapeJava
     */
    fun unescapeJava(input: String): String {
        val stringBuilder = StringBuilder()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch == '\\' && i + 1 < input.length) {
                val nextChar = input[i + 1]
                when (nextChar) {
                    'n' -> stringBuilder.append('\n')
                    't' -> stringBuilder.append('\t')
                    'b' -> stringBuilder.append('\b')
                    'r' -> stringBuilder.append('\r')
                    'f' -> stringBuilder.append('\u000C')
                    '\'' -> stringBuilder.append('\'')
                    '"' -> stringBuilder.append('\"')
                    '\\' -> stringBuilder.append('\\')
                    'u' -> {
                        if (i + 5 < input.length) {
                            val hexCode = input.substring(i + 2, i + 6)
                            stringBuilder.append(hexCode.toInt(16).toChar())
                            i += 4
                        }
                    }

                    else -> stringBuilder.append(nextChar)  // if it's not an escape sequence, keep the original
                }
                i += 1 // skip next character
            } else {
                stringBuilder.append(ch)
            }
            i++
        }
        return stringBuilder.toString()
    }
}

fun processedTextToChunks(text: String): MutableList<String> {
    val processedText = text.replace("\\n", " ")
        .replace("\\\"", "")
        .replace("\\t", "")
        .replace("\\", "")
    val sentences = processedText.split("(?<=\\.)(?!\\d)|(?<=。)|(?<=？)|(?<=\\?)".toRegex())
    val chunks = sentences.fold(mutableListOf<String>()) { acc, sentence ->
        if (acc.isEmpty() || (acc.last() + sentence).getWordCount() > 60) {
            acc.add(sentence.trim())
        } else {
            val last = acc.last()
            acc[acc.size - 1] = "$last$sentence"
        }
        acc
    }
    return chunks
}

fun String.getWordCount(): Int {
    val trimmedInput = trim()
    if (trimmedInput.isEmpty()) return 0

    // CJ
    if (endsWith("。") || endsWith("？") || endsWith("！")) {
        return trimmedInput.length
    }
    // korean
    val hangulRegex = "[가-힣]+".toRegex() // Matches any Hangul syllable
    // Find all matches and return the count
    val hangulCount = hangulRegex.findAll(trimmedInput).sumOf { it.value.length }
    if (hangulCount > 3) return hangulCount

    // Use regex to match words based on Unicode word boundaries
    val wordRegex = "\\p{L}+".toRegex()
    // Find all matches and return the count
    return wordRegex.findAll(trimmedInput).count()
}

fun String.pruneWebTitle(): String =
    if (contains("|")) {
        substringBefore("|").trim()
    } else if (contains("-")) {
        substringBefore("-").trim()
    } else {
        this
    }
