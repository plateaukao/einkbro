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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.HelperUnit.needGrantStoragePermission
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

object DownloadHelper {

    var downloadFileId = -1L

    @JvmStatic
    fun download(context: Context, url: String, contentDisposition: String, mimeType: String) {
        val activity = context as Activity
        if (needGrantStoragePermission(activity)) {
            return
        }

        if (url.startsWith("data:")) {
            saveDataUrl(activity, url, mimeType)
            return
        }

        val filename = Uri.decode(guessFilename(url, contentDisposition, mimeType))

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

            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, filename)

            val bytes = if (isBase64) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Base64.getDecoder().decode(rawData)
                } else {
                    android.util.Base64.decode(rawData, android.util.Base64.DEFAULT)
                }
            } else {
                java.net.URLDecoder.decode(rawData, "UTF-8").toByteArray()
            }

            destFile.writeBytes(bytes)
            showShort(activity, R.string.toast_downloadComplete)
        } catch (e: Exception) {
            Log.w("browser", "Failed to save data URL: $e")
            showShort(activity, R.string.error_download_link_invalid)
        }
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
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                } catch (e: Exception) {
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    setDestinationUri(Uri.fromFile(java.io.File(downloadsDir, filename)))
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

                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, filename)
                connection.inputStream.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    showShort(activity, R.string.toast_downloadComplete)
                }
            } catch (e: Exception) {
                Log.w("browser", "Direct download also failed: $e")
                withContext(Dispatchers.Main) {
                    showShort(activity, R.string.error_download_link_invalid)
                }
            }
        }
    }

    fun guessFilename(url: String, contentDisposition: String, mimeType: String): String {
        val cdFilename = parseContentDispositionFilename(contentDisposition)
        if (cdFilename != null) return sanitizeFilename(cdFilename)

        val urlFilename = extractFilenameFromUrl(url, mimeType)
        if (urlFilename != null) return sanitizeFilename(urlFilename)

        return sanitizeFilename(URLUtil.guessFileName(url, contentDisposition, mimeType))
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
        val decodedUrl = try { Uri.decode(url) } catch (e: Exception) { return null } ?: return null

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

    fun openDownloadFolder(activity: Activity) {
        val uri = Uri.parse(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString()
        )
        val intent =
            Intent(Intent.ACTION_GET_CONTENT).apply { setDataAndType(uri, "resource/folder") }
        activity.startActivity(
            Intent.createChooser(intent, activity.getString(R.string.dialog_title_download))
        )
    }
}
