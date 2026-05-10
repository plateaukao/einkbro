package info.plateaukao.einkbro.unit

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.HelperUnit.needGrantStoragePermission
import info.plateaukao.einkbro.view.EBWebView
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.EBToast.showShort
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.TextInputDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object DownloadHelper {

    var downloadFileId = -1L

    private const val SUPERNOTE_DOCUMENT_DIR = "Document"
    private const val MAX_BLOB_BASE64_LENGTH = 64 * 1024 * 1024
    private data class PendingBlobDownload(
        val activity: Activity,
        val fileName: String,
        val fallbackMimeType: String,
        val base64Data: StringBuffer = StringBuffer(),
    )

    private val pendingBlobDownloads = ConcurrentHashMap<String, PendingBlobDownload>()

    private fun publicDownloadDirName(context: Context): String =
        if (HelperUnit.isSupernoteDocumentInstalled(context)) {
            SUPERNOTE_DOCUMENT_DIR
        } else {
            Environment.DIRECTORY_DOWNLOADS
        }

    private fun publicDownloadDir(context: Context): File =
        Environment.getExternalStoragePublicDirectory(publicDownloadDirName(context))

    private fun useSupernoteStorage(context: Context): Boolean =
        HelperUnit.isSupernoteDocumentInstalled(context)

    private fun guessMime(fileName: String, fallback: String): String {
        if (fallback.isNotBlank()) return fallback
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    /**
     * Write [bytes] into the granted Supernote `Document/` tree. Prompts for SAF
     * permission on first use. Reports completion / failure via toast.
     */
    private fun writeBytesViaSupernote(
        activity: Activity,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ) {
        SupernoteStorage.ensureTreeUri { treeUri ->
            if (treeUri == null) {
                showShort(activity, R.string.error_download_link_invalid)
                return@ensureTreeUri
            }
            try {
                val opened = SupernoteStorage.openOutputStream(
                    activity, treeUri, fileName, guessMime(fileName, mimeType),
                ) ?: run {
                    showShort(activity, R.string.error_download_link_invalid)
                    return@ensureTreeUri
                }
                opened.first.use { it.write(bytes) }
                showShort(activity, R.string.toast_downloadComplete)
            } catch (e: Exception) {
                Log.w("browser", "Supernote write failed: $e")
                showShort(activity, R.string.error_download_link_invalid)
            }
        }
    }

    @JvmStatic
    fun download(
        context: Context,
        url: String,
        contentDisposition: String,
        mimeType: String,
        webView: EBWebView? = null,
    ) {
        val activity = context as Activity
        if (needGrantStoragePermission(activity)) {
            return
        }

        if (url.startsWith("data:")) {
            saveDataUrl(activity, url, mimeType)
            return
        }

        val filename = Uri.decode(guessFilename(url, contentDisposition, mimeType, webView?.url))

        if (url.startsWith("blob:")) {
            if (webView == null) {
                showShort(activity, R.string.error_download_link_invalid)
                return
            }
            val githubRawUrl = githubRawUrl(webView.url)
            if (githubRawUrl != null) {
                internalDownload(activity, githubRawUrl, mimeType, filename)
                return
            }
            if (hasExactFilename(contentDisposition) || filename != "download") {
                downloadBlobUrl(activity, webView, url, filename, mimeType)
            } else {
                val title = context.getString(R.string.dialog_title_download)
                (activity as LifecycleOwner).lifecycleScope.launch {
                    val modifiedFilename = TextInputDialog(
                        context,
                        "",
                        title,
                        filename
                    ).show()

                    modifiedFilename?.let { downloadBlobUrl(activity, webView, url, it, mimeType) }
                }
            }
            return
        }

        if (hasExactFilename(contentDisposition)) {
            internalDownload(activity, url, mimeType, filename)
        } else {
            val title = context.getString(R.string.dialog_title_download)
            (activity as LifecycleOwner).lifecycleScope.launch {
                val modifiedFilename = TextInputDialog(
                    context,
                    "",
                    title,
                    filename
                ).show()

                modifiedFilename?.let { internalDownload(activity, url, mimeType, it) }
            }
        }
    }

    private fun downloadBlobUrl(
        activity: Activity,
        webView: EBWebView,
        blobUrl: String,
        filename: String,
        mimeType: String,
    ) {
        val downloadId = UUID.randomUUID().toString()
        pendingBlobDownloads[downloadId] = PendingBlobDownload(
            activity = activity,
            fileName = sanitizeFilename(filename),
            fallbackMimeType = mimeType,
        )
        showShort(activity, R.string.toast_start_download)
        webView.jsBridge.downloadBlobUrl(blobUrl, mimeType, downloadId)
    }

    fun beginBlobDownload(activity: Activity, fileName: String, mimeType: String): String {
        val downloadId = UUID.randomUUID().toString()
        pendingBlobDownloads[downloadId] = PendingBlobDownload(
            activity = activity,
            fileName = sanitizeFilename(fileName),
            fallbackMimeType = mimeType,
        )
        showShort(activity, R.string.toast_start_download)
        return downloadId
    }

    private fun saveDataUrl(activity: Activity, dataUrl: String, fallbackMimeType: String) {
        try {
            val header = dataUrl.substring(dataUrl.indexOf(":") + 1, dataUrl.indexOf(","))
            val dataMimeType = if (header.contains(";")) {
                header.substring(0, header.indexOf(";"))
            } else {
                header
            }
            val isBase64 = header.contains("base64", ignoreCase = true)
            val rawData = dataUrl.substring(dataUrl.indexOf(",") + 1)

            val effectiveMimeType = dataMimeType.ifEmpty { fallbackMimeType }
            val ext = android.webkit.MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(effectiveMimeType) ?: "bin"
            val filename = "download_${System.currentTimeMillis()}.$ext"

            val bytes = if (isBase64) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Base64.getDecoder().decode(rawData)
                } else {
                    android.util.Base64.decode(rawData, android.util.Base64.DEFAULT)
                }
            } else {
                java.net.URLDecoder.decode(rawData, "UTF-8").toByteArray()
            }

            if (useSupernoteStorage(activity)) {
                writeBytesViaSupernote(activity, filename, effectiveMimeType, bytes)
            } else {
                val destFile = File(publicDownloadDir(activity), filename)
                destFile.writeBytes(bytes)
                showShort(activity, R.string.toast_downloadComplete)
            }
        } catch (e: Exception) {
            Log.w("browser", "Failed to save data URL: $e")
            showShort(activity, R.string.error_download_link_invalid)
        }
    }

    fun appendBlobDownloadChunk(downloadId: String, base64Chunk: String) {
        val pending = pendingBlobDownloads[downloadId]
        if (pending == null) {
            Log.w("browser", "Missing blob download session for chunk: $downloadId")
            return
        }
        if (pending.base64Data.length + base64Chunk.length > MAX_BLOB_BASE64_LENGTH) {
            pendingBlobDownloads.remove(downloadId)
            Log.w("browser", "Blob download exceeded supported size: $downloadId")
            showShort(pending.activity, R.string.error_download_link_invalid)
            return
        }
        pending.base64Data.append(base64Chunk)
    }

    fun completeBlobDownload(downloadId: String, mimeType: String) {
        val pending = pendingBlobDownloads.remove(downloadId)
        if (pending == null) {
            Log.w("browser", "Missing blob download session on completion: $downloadId")
            return
        }

        val effectiveMimeType = mimeType.ifBlank { pending.fallbackMimeType }
        val encodedData = pending.base64Data.toString()
        (pending.activity as LifecycleOwner).lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Base64.getDecoder().decode(encodedData)
                } else {
                    android.util.Base64.decode(encodedData, android.util.Base64.DEFAULT)
                }
                if (useSupernoteStorage(pending.activity)) {
                    withContext(Dispatchers.Main) {
                        writeBytesViaSupernote(
                            pending.activity,
                            pending.fileName,
                            effectiveMimeType,
                            bytes,
                        )
                    }
                } else {
                    val destFile = File(publicDownloadDir(pending.activity), pending.fileName)
                    destFile.writeBytes(bytes)
                    withContext(Dispatchers.Main) {
                        showShort(pending.activity, R.string.toast_downloadComplete)
                    }
                }
            } catch (e: Exception) {
                Log.w("browser", "Failed to complete blob download: $e")
                withContext(Dispatchers.Main) {
                    showShort(pending.activity, R.string.error_download_link_invalid)
                }
            }
        }
    }

    fun failBlobDownload(downloadId: String, message: String?) {
        val pending = pendingBlobDownloads.remove(downloadId) ?: return
        Log.w("browser", "Blob download failed: $message")
        showShort(pending.activity, R.string.error_download_link_invalid)
    }

    private fun hasExactFilename(contentDisposition: String): Boolean =
        parseContentDispositionFilename(contentDisposition) != null

    private fun internalDownload(
        activity: Activity,
        url: String,
        mimeType: String,
        filename: String
    ) {
        val cookie = CookieManager.getInstance().getCookie(url)
        val userAgent = WebSettings.getDefaultUserAgent(activity)
        if (Uri.parse(url).host == null) {
            showShort(activity, R.string.error_download_link_invalid)
            return
        }
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                if (cookie != null) addRequestHeader("Cookie", cookie)
                addRequestHeader("User-Agent", userAgent)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setTitle(filename)
                setMimeType(mimeType)
                try {
                    setDestinationInExternalPublicDir(publicDownloadDirName(activity), filename)
                } catch (e: Exception) {
                    setDestinationUri(Uri.fromFile(File(publicDownloadDir(activity), filename)))
                }
            }
            val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadFileId = manager.enqueue(request)
            showShort(activity, R.string.toast_start_download)
        } catch (e: Exception) {
            Log.w("browser", "DownloadManager failed, falling back to direct download: $e")
            directDownload(activity, url, cookie, userAgent, filename, mimeType)
        }
    }

    private fun directDownload(
        activity: Activity,
        url: String,
        cookie: String?,
        userAgent: String,
        filename: String,
        mimeType: String
    ) {
        showShort(activity, R.string.toast_start_download)
        (activity as LifecycleOwner).lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.addRequestProperty("User-Agent", userAgent)
                if (cookie != null) connection.addRequestProperty("Cookie", cookie)
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    withContext(Dispatchers.Main) {
                        showShort(activity, R.string.error_download_link_invalid)
                    }
                    return@launch
                }

                if (useSupernoteStorage(activity)) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    withContext(Dispatchers.Main) {
                        writeBytesViaSupernote(activity, filename, mimeType, bytes)
                    }
                } else {
                    val destFile = File(publicDownloadDir(activity), filename)
                    connection.inputStream.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        showShort(activity, R.string.toast_downloadComplete)
                    }
                }
            } catch (e: Exception) {
                Log.w("browser", "Direct download also failed: $e")
                withContext(Dispatchers.Main) {
                    showShort(activity, R.string.error_download_link_invalid)
                }
            }
        }
    }

    fun guessFilename(
        url: String,
        contentDisposition: String,
        mimeType: String,
        fallbackUrl: String? = null,
    ): String {
        val cdFilename = parseContentDispositionFilename(contentDisposition)
        if (cdFilename != null) return sanitizeFilename(cdFilename)

        if (url.startsWith("blob:")) {
            val fallbackFilename = fallbackUrl?.let { extractFilenameFromUrl(it, mimeType) }
            if (fallbackFilename != null) return sanitizeFilename(fallbackFilename)
        }

        val urlFilename = extractFilenameFromUrl(url, mimeType)
        if (urlFilename != null) return sanitizeFilename(urlFilename)

        val fallbackFilename = fallbackUrl?.let { extractFilenameFromUrl(it, mimeType) }
        if (fallbackFilename != null) return sanitizeFilename(fallbackFilename)

        return sanitizeFilename(URLUtil.guessFileName(url, contentDisposition, mimeType))
    }

    private fun githubRawUrl(currentUrl: String?): String? {
        val uri = currentUrl?.let(Uri::parse) ?: return null
        if (uri.host != "github.com") return null
        val segments = uri.pathSegments
        if (segments.size < 5 || segments[2] != "blob") return null

        val owner = segments[0]
        val repo = segments[1]
        val branch = segments[3]
        val path = segments.drop(4).joinToString("/")
        if (path.isBlank()) return null

        return uri.buildUpon()
            .path("/$owner/$repo/raw/refs/heads/$branch/$path")
            .clearQuery()
            .fragment(null)
            .build()
            .toString()
    }

    private fun parseContentDispositionFilename(contentDisposition: String): String? {
        if (contentDisposition.isBlank()) return null

        // RFC 5987: filename*=UTF-8''percent-encoded-name (takes priority per RFC 6266)
        val extValuePattern = Regex("""filename\*\s*=\s*[^']*'[^']*'(.+?)(?:\s*;|$)""", RegexOption.IGNORE_CASE)
        extValuePattern.find(contentDisposition)?.let { match ->
            val encoded = match.groupValues[1].trim()
            return try {
                URLDecoder.decode(encoded, "UTF-8")
            } catch (e: Exception) {
                encoded
            }
        }

        // Quoted: filename="name"
        val quotedPattern = Regex("""filename\s*=\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        quotedPattern.find(contentDisposition)?.let { match ->
            return match.groupValues[1]
        }

        // Unquoted: filename=name
        val unquotedPattern = Regex("""filename\s*=\s*([^\s;]+)""", RegexOption.IGNORE_CASE)
        unquotedPattern.find(contentDisposition)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    private fun extractFilenameFromUrl(url: String, mimeType: String): String? {
        val decodedUrl = try { URLDecoder.decode(url, "UTF-8") } catch (e: Exception) { return null }

        // Strip query string and fragment
        val cleanUrl = decodedUrl.substringBefore('?').substringBefore('#')
        if (cleanUrl.endsWith("/")) return null

        val lastSegment = cleanUrl.substringAfterLast('/')
        if (lastSegment.isBlank()) return null

        // If it has a recognizable file extension, use it
        if (lastSegment.contains('.')) return lastSegment

        // No extension -- try to derive one from mimeType
        val ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        return if (ext != null) "$lastSegment.$ext" else null
    }

    private fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[/\\\\]"), "_")
            .replace(Regex("[\\x00-\\x1f]"), "")
            .trim()
            .ifBlank { "download" }
    }

    fun createDownloadReceiver(activity: Activity): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (activity.isFinishing || downloadFileId == -1L) return

                val downloadManager = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                val mostRecentDownload: Uri =
                    downloadManager.getUriForDownloadedFile(downloadFileId) ?: return
                val mimeType: String = downloadManager.getMimeTypeForDownloadedFile(downloadFileId)
                val fileIntent = Intent(ACTION_VIEW).apply {
                    setDataAndType(mostRecentDownload, mimeType)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                downloadFileId = -1L
                DialogManager(activity).showOkCancelDialog(
                    messageResId = R.string.toast_downloadComplete,
                    okAction = {
                        try {
                            activity.startActivity(fileIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            EBToast.show(activity, R.string.toast_error)
                        }
                    }
                )
            }
        }
    }

    /**
     * Download [url] under [fileName] without prompting. For data:image URLs (Android O+),
     * defer to the SAF picker via [imagePickerLauncher]. For regular URLs, enqueue a
     * DownloadManager request straight to the public Downloads directory.
     */
    fun saveFileWithName(
        activity: Activity,
        url: String,
        fileName: String,
        imagePickerLauncher: ActivityResultLauncher<Intent>,
    ) {
        if (url.startsWith("data:image")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                BrowserUnit.saveImageFromUrl(url, imagePickerLauncher)
            } else {
                EBToast.show(activity, "Not supported dataUrl")
            }
            return
        }
        if (needGrantStoragePermission(activity)) return
        val cookie = CookieManager.getInstance().getCookie(url)
        val userAgent = WebSettings.getDefaultUserAgent(activity)
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                if (cookie != null) addRequestHeader("Cookie", cookie)
                addRequestHeader("User-Agent", userAgent)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                try {
                    setDestinationInExternalPublicDir(publicDownloadDirName(activity), fileName)
                } catch (e: Exception) {
                    setDestinationUri(Uri.fromFile(File(publicDownloadDir(activity), fileName)))
                }
            }
            (activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        } catch (e: Exception) {
            // DownloadManager rejects custom public dirs (e.g. Supernote's "Document"),
            // fall back to a direct HTTP download into the resolved folder.
            Log.w("browser", "DownloadManager rejected destination, falling back: $e")
            directDownload(activity, url, cookie, userAgent, fileName, "")
        }
        ViewUnit.hideKeyboard(activity)
    }

    fun openDownloadFolder(activity: Activity) {
        val uri = Uri.parse(publicDownloadDir(activity).toString())
        val intent =
            Intent(Intent.ACTION_GET_CONTENT).apply { setDataAndType(uri, "resource/folder") }
        activity.startActivity(
            Intent.createChooser(intent, activity.getString(R.string.dialog_title_download))
        )
    }
}
