package info.plateaukao.einkbro.unit

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileHelper : KoinComponent {
    private val appContext: Context by inject()
    private val fileCache = mutableMapOf<String, String>()

    fun getCachedPathFromURI(context: Context, contentURI: Uri): String {
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

    fun loadAssetFileToString(context: Context, filename: String): String {
        val inputStream = context.assets.open(filename)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        bufferedReader.useLines { lines -> lines.forEach { stringBuilder.append(it) } }

        return stringBuilder.toString()
    }

    fun loadAssetFile(fileName: String): String {
        if (fileCache.containsKey(fileName)) {
            return fileCache[fileName]!!
        }

        try {
            val jsContent = appContext.assets.open(fileName).bufferedReader().use { it.readText() }
            fileCache[fileName] = jsContent
            return jsContent
        } catch (e: IOException) {
            Log.e("FileHelper", "Failed to load asset file: $fileName")
            e.printStackTrace()
            return ""
        }
    }

    fun getStringFromAsset(fileName: String): String =
        appContext.assets.open(fileName).bufferedReader().use { it.readText() }

    @JvmStatic
    fun fileName(url: String?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        val domain = Uri.parse(url).host?.replace("www.", "")?.trim { it <= ' ' }.orEmpty()
        return domain.replace(".", "_").trim { it <= ' ' } + "_" + currentTime.trim { it <= ' ' }
    }
}
