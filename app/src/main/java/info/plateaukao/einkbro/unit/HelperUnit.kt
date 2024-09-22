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
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.EinkBroApplication
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.EpubReaderActivity
import info.plateaukao.einkbro.util.Constants
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream


object HelperUnit {
    private const val REQUEST_CODE_ASK_PERMISSIONS = 123
    private const val REQUEST_CODE_ASK_PERMISSIONS_1 = 1234

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()



    fun getStringFromAsset(fileName: String): String =
        EinkBroApplication.instance.assets.open(fileName).bufferedReader().use { it.readText() }

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

    fun getCachedPathFromURI(context: Context, contentURI: Uri): String {
        //val tempFile = File(context.filesDir.absolutePath + "/temp.mht")
        val tempFile = File.createTempFile("tempfile", ".mht", context.cacheDir)
        tempFile.deleteOnExit()

        context.contentResolver.openInputStream(contentURI)?.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile.absolutePath
    }

    fun readContentAsStringList(contentResolver: ContentResolver, contentUri: Uri): List<String> {
        contentResolver.openInputStream(contentUri).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val lines = mutableListOf<String>()
                var line: String? = reader.readLine()
                while (line != null) {
                    lines.add(line)
                    line = reader.readLine()
                }
                return lines
            }
        }
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
    fun fileName(url: String?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        val domain = Uri.parse(url).host?.replace("www.", "")?.trim { it <= ' ' }.orEmpty()
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
        shouldGoToEnd: Boolean = false
    ) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (shouldGoToEnd) {
            intent.putExtra(EpubReaderActivity.ARG_TO_LAST_CHAPTER, true)
        }

        try {
            activity.startActivity(Intent.createChooser(intent, "Open file with"))
        } catch (exception: SecurityException) {
            NinjaToast.show(activity, "open file failed, re-select the file again.")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = Constants.MIME_TYPE_ANY
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra("android.provider.extra.INITIAL_URI", uri);
            resultLauncher?.launch(intent)
        }
    }

    fun showBrowserChooser(activity: Activity, url: String?, title: String) {
        val nonNullUrl = url ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nonNullUrl))
        activity.startActivity(Intent.createChooser(intent, title))
    }

    fun loadAssetFileToString(context: Context, filename: String): String {
        val inputStream = context.assets.open(filename)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        bufferedReader.useLines { lines -> lines.forEach { stringBuilder.append(it) } }

        return stringBuilder.toString()
    }

    fun getFileInfoFromContentUri(context: Context, contentUri: Uri): Pair<String?, String?> {
        var fileName: String? = null
        var mimeType: String? = null

        val cursor = context.contentResolver.query(contentUri, null, null, null, null)
        cursor?.use {
            try {
                if (it.moveToFirst()) {
                    val displayNameColumnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                    fileName = if (displayNameColumnIndex != -1)
                        it.getString(displayNameColumnIndex)
                    else {
                        contentUri.path?.split("/")?.last()
                    }

                    val mimeTypeColumnIndex = it.getColumnIndex(
                        MimeTypeMap.getSingleton()
                            .getExtensionFromMimeType(it.getString(it.getColumnIndexOrThrow("mime_type")))
                    )
                    mimeType = if (mimeTypeColumnIndex != -1)
                        it.getString(mimeTypeColumnIndex)
                    else {
                        context.contentResolver.getType(contentUri)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Pair(fileName, mimeType)
    }

    suspend fun upgradeToLatestRelease(context: Context) {
        if (isAppInstalledFromPlayStore(context)) {
            withContext(Dispatchers.Main) {
                // launch play store with my app page
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data =
                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    setPackage("com.android.vending")
                }
                context.startActivity(intent)
            }
            return
        }

        val url = "https://api.github.com/repos/plateaukao/einkbro/releases"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            try {
                val jsonArray = JSONArray(response.body!!.string())
                val latestRelease = jsonArray.getJSONObject(0)
                val tagName = latestRelease
                    .getString("tag_name")
                    .replace("v", "")
                if (tagName > BuildConfig.VERSION_NAME) {
                    withContext(Dispatchers.Main) {
                        NinjaToast.show(context, "Start downloading...")
                    }

                    val downloadUrl = latestRelease.getJSONArray("assets")
                        .getJSONObject(0)
                        .getString("browser_download_url")

                    val file = File.createTempFile("temp", ".apk", context.cacheDir)
                    downloadApkFile(downloadUrl, file.absolutePath)
                    installApkFromFile(context, file)
                } else {
                    withContext(Dispatchers.Main) {
                        NinjaToast.show(context, "Already up to date")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    NinjaToast.show(context, "Something went wrong")
                }
            }
        }
    }

    private fun installApkFromFile(context: Context, file: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }

    private fun downloadApkFile(apkUrl: String, destinationPath: String) {
        val request = Request.Builder().url(apkUrl).build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download file: $response")

            val fos = FileOutputStream(destinationPath)
            fos.use { outputStream ->
                outputStream.write(response.body?.bytes())
            }
        }
    }

    suspend fun upgradeFromSnapshot(context: Context) {
        val url =
            "https://nightly.link/plateaukao/einkbro/workflows/buid-app-workflow.yaml/main/app-release.apk.zip"
        val request = Request.Builder().url(url).build()
        try {
            withContext(Dispatchers.Main) {
                NinjaToast.show(context, "start downloading...")
            }

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to download file: $response")

                val inputStream = response.body?.byteStream()
                withContext(Dispatchers.Main) {
                    NinjaToast.show(context, "Extracting zip...")
                }
                extractApkAndInstall(inputStream, context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                NinjaToast.show(context, "Something went wrong")
            }
        }
    }

    // may throw error
    private fun extractApkAndInstall(inputStream: InputStream?, context: Context) {
        val zipInputStream = ZipInputStream(inputStream)

        var zipEntry = zipInputStream.nextEntry
        while (zipEntry != null) {
            if (zipEntry.name == "app-release.apk") {
                val tempFile = File("${context.cacheDir.absolutePath}/app.apk")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                tempFile.createNewFile()
                tempFile.deleteOnExit()
                FileOutputStream(tempFile).use { fos -> zipInputStream.copyTo(fos) }

                installApkFromFile(context, tempFile)

                break
            }
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.closeEntry()
        zipInputStream.close()
    }

    private fun isAppInstalledFromPlayStore(context: Context): Boolean {
        val packageName = context.packageName
        val pm = context.packageManager

        return try {
            val installerPackageName = pm.getInstallerPackageName(packageName)
            "com.android.vending" == installerPackageName
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private const val DEFAULT_FONT_SIZE = 18
    /**
     * Parses a given markdown text and converts it into an [AnnotatedString] with appropriate styles.
     * from: mdparserkitcore/src/main/java/com/daksh/mdparserkit/core/ParseMarkdown.kt
     *
     * @param markdownText The input markdown text to parse.
     * @return An [AnnotatedString] with styles applied according to the markdown syntax.
     */
    fun parseMarkdown(markdownText: String): AnnotatedString {
        val lines = markdownText.split("\n")
        val resultBuilder = AnnotatedString.Builder()
        var currentStyle: SpanStyle

        lines.forEach { line ->
            when {
                // Heading 1: Extracting content, applying bold style, and appending to resultBuilder
                line.startsWith("# ") -> {
                    val content = line.removePrefix("# ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 4).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Similar processing for Heading 2 to Heading 6
                // Heading 2
                line.startsWith("## ") -> {
                    val content = line.removePrefix("## ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 3).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Heading 3
                line.startsWith("### ") -> {
                    val content = line.removePrefix("### ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 2).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Heading 4
                line.startsWith("#### ") -> {
                    val content = line.removePrefix("#### ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 2).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Heading 5
                line.startsWith("##### ") -> {
                    val content = line.removePrefix("##### ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 1).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Heading 6
                line.startsWith("###### ") -> {
                    val content = line.removePrefix("###### ").trim()
                    textMarkDown(
                        content,
                        resultBuilder,
                        fontSize = (DEFAULT_FONT_SIZE + 1).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Unordered list item: Extracting content, applying bold style, appending bullet point symbol, and appending to resultBuilder
                line.startsWith("* ") || line.startsWith("- ") -> {
                    val content = line.removePrefix("* ").removePrefix("- ").trim()
                    currentStyle = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = DEFAULT_FONT_SIZE.sp
                    )
                    resultBuilder.append(
                        AnnotatedString("• ", currentStyle)
                    )
                    textMarkDown(content, resultBuilder, fontSize = 14.sp)
                }
                // Ordered list item: Extracting content, applying bold style, appending number and period, and appending to resultBuilder
                line.matches(Regex("^\\d+\\.\\s.*$")) -> {
                    val regex = Regex("^\\d+\\.\\s.*$")
                    val startIndex = regex.find(line)?.range?.first ?: 0
                    currentStyle = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = DEFAULT_FONT_SIZE.sp
                    )
                    val annotatedString = buildAnnotatedString {
                        if (startIndex > 0) {
                            append(line.substring(0, startIndex))
                        }
                        withStyle(currentStyle) {
                            append(line.substring(startIndex, startIndex + 2))
                        }
                    }
                    resultBuilder.append(annotatedString)
                    textMarkDown(
                        inputText = line.substring(startIndex + 2, line.length),
                        resultBuilder = resultBuilder,
                        fontSize = 16.sp
                    )
                }
                // Remaining Text
                else -> {
                    textMarkDown(line, resultBuilder, fontSize = DEFAULT_FONT_SIZE.sp)
                }
            } // Appending new line
            resultBuilder.append("\n")
        }
        return resultBuilder.toAnnotatedString().trim() as AnnotatedString
    }

    /**
     * Converts markdown-style text formatting to [AnnotatedString] with appropriate [SpanStyle]s.
     *
     * @param inputText The input text to be converted.
     * @param resultBuilder The [AnnotatedString.Builder] to append the converted text to.
     * @param fontSize The desired font size for the text.
     * @return The converted text with markdown formatting replaced by appropriate [SpanStyle]s.
     */
    private fun textMarkDown(
        inputText: String,
        resultBuilder: AnnotatedString.Builder,
        fontSize: TextUnit,
        fontWeight: FontWeight = FontWeight.Normal
    ) {
        val boldItalicPattern = Regex("\\*\\*[*_](.*?)[*_]\\*\\*")
        val boldPattern = Regex("\\*\\*(.*?)\\*\\*")
        val italicPattern = Regex("[*_](.*?)[*_]")
        val strikethroughPattern = Regex("~~(.+?)~~")

        var currentIndex = 0

        while (currentIndex < inputText.length) {
            val nextBoldItalic = boldItalicPattern.find(inputText, startIndex = currentIndex)
            val nextBold = boldPattern.find(inputText, startIndex = currentIndex)
            val nextItalic = italicPattern.find(inputText, startIndex = currentIndex)
            val nextStrikethrough = strikethroughPattern.find(inputText, startIndex = currentIndex)

            val nextMarkDown = listOfNotNull(
                nextBoldItalic,
                nextBold,
                nextItalic,
                nextStrikethrough
            ).minByOrNull { it.range.first }

            if (nextMarkDown != null) {
                if (nextMarkDown.range.first > currentIndex) {
                    // Append any normal text before the markdown
                    val normalText = inputText.substring(currentIndex, nextMarkDown.range.first)
                    val style = SpanStyle(fontWeight = fontWeight, fontSize = fontSize)
                    resultBuilder.append(AnnotatedString(normalText, style))
                }

                val matchText = nextMarkDown.groupValues.getOrNull(1).orEmpty()

                val style = when (nextMarkDown) {
                    nextBoldItalic -> SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontSize = fontSize
                    )

                    nextBold -> SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = DEFAULT_FONT_SIZE.sp
                    )

                    nextItalic -> SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontSize = fontSize
                    )

                    nextStrikethrough -> SpanStyle(
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = fontSize
                    )

                    else -> throw IllegalStateException("Unhandled markdown type")
                }

                resultBuilder.append(AnnotatedString(matchText, style))

                currentIndex = nextMarkDown.range.last + 1
            } else {
                // Append any remaining text if no more markdown found
                val normalText = inputText.substring(currentIndex)
                val style = SpanStyle(fontWeight = fontWeight, fontSize = fontSize)
                resultBuilder.append(AnnotatedString(normalText, style))
                currentIndex = inputText.length
            }
        }
    }

}

fun processedTextToChunks(text: String): MutableList<String> {
    val processedText = text.replace("\\n", " ").replace("\\\"", "").replace("\\t", "").replace("\\", "")
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
