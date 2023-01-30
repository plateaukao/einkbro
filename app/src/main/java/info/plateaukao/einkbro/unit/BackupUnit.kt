package info.plateaukao.einkbro.unit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentActivity
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class BackupUnit(
    private val context: Context,
) : KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()
    private val recordDb: RecordDb by inject()
    private val config: ConfigManager by inject()

    fun backupData(context: Context, uri: Uri): Boolean {
        try {
            val fos = context.contentResolver.openOutputStream(uri) ?: return false
            val zos = ZipOutputStream(fos)

            // Add databases to the zip file
            val dbDirectory = File(DATABASE_PATH)
            val dbFiles = dbDirectory.listFiles()
            if (dbFiles != null) {
                for (dbFile in dbFiles) {
                    val fis = FileInputStream(dbFile)
                    zos.putNextEntry(ZipEntry(dbFile.name))
                    val buffer = ByteArray(1024)
                    var length = fis.read(buffer)
                    while (length > 0) {
                        zos.write(buffer, 0, length)
                        length = fis.read(buffer)
                    }
                    zos.closeEntry()
                    fis.close()
                }
            }

            // Add shared preferences to the zip file
            val sharedPrefsDirectory = File(SHARED_PREFS_PATH)
            val sharedPrefsFiles = sharedPrefsDirectory.listFiles()
            if (sharedPrefsFiles != null) {
                for (sharedPrefsFile in sharedPrefsFiles) {
                    val fis = FileInputStream(sharedPrefsFile)
                    zos.putNextEntry(ZipEntry(sharedPrefsFile.name))
                    val buffer = ByteArray(1024)
                    var length = fis.read(buffer)
                    while (length > 0) {
                        zos.write(buffer, 0, length)
                        length = fis.read(buffer)
                    }
                    zos.closeEntry()
                    fis.close()
                }
            }

            zos.close()
            fos.close()
            NinjaToast.show(context, R.string.toast_backup_successful)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun restoreBackupData(context: Context, uri: Uri): Boolean {
        try {
            bookmarkManager.database.close()
            recordDb.close()

            val fis = context.contentResolver.openInputStream(uri) ?: return false
            val zis = ZipInputStream(fis)

            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val file = File(
                    if (zipEntry.name.endsWith(".db") ||
                        zipEntry.name.contains("einkbro_db")
                    ) "$DATABASE_PATH${zipEntry.name}"
                    else "$SHARED_PREFS_PATH${zipEntry.name}"
                )
                val fos = FileOutputStream(file)
                val buffer = ByteArray(1024)
                var length = zis.read(buffer)
                while (length > 0) {
                    fos.write(buffer, 0, length)
                    length = zis.read(buffer)
                }
                fos.close()
                zipEntry = zis.nextEntry
            }
            zis.close()
            fis.close()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    fun importBookmarks(uri: Uri) {
        GlobalScope.launch {
            try {
                context.contentResolver.openInputStream(uri).use {
                    val jsonString = it?.bufferedReader()?.readText() ?: ""
                    val bookmarks = JSONArray(jsonString).toJSONObjectList()
                        .map { json -> json.toBookmark() }
                    if (bookmarks.isNotEmpty()) {
                        bookmarkManager.overwriteBookmarks(bookmarks)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bookmarks are imported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bookmarks import failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bookmarks import failed", Toast.LENGTH_SHORT).show()
                }
                config.bookmarkSyncUrl = ""
            }
        }
    }

    private fun JSONArray.toJSONObjectList() =
        (0 until length()).map { get(it) as JSONObject }

    fun exportBookmarks(uri: Uri, showToast: Boolean = true) {
        GlobalScope.launch {
            val bookmarks = bookmarkManager.getAllBookmarks()
            try {
                context.contentResolver.openOutputStream(uri).use {
                    it?.write(bookmarks.toJsonString().toByteArray())
                }
                if (showToast) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Bookmarks are exported", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                if (showToast) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Bookmarks export failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bookmarks export failed", Toast.LENGTH_SHORT).show()
                }
                config.bookmarkSyncUrl = ""
            }
        }
    }

    fun createOpenBookmarkFileLauncher(activity: ComponentActivity) =
        IntentUnit.createResultLauncher(activity) { linkToBookmarkSyncFile(it) }

    fun createCreateBookmarkFileLauncher(activity: ComponentActivity) =
        IntentUnit.createResultLauncher(activity) { createBookmarkSyncFile(it) }

    fun handleBookmarkSync(
        forceUpload: Boolean = false,
        dialogManager: DialogManager,
        openBookmarkFileLauncher: ActivityResultLauncher<Intent>,
        createBookmarkFileLauncher: ActivityResultLauncher<Intent>
    ) {
        if (config.bookmarkSyncUrl.isNotBlank()) {
            if (forceUpload) {
                exportBookmarks(Uri.parse(config.bookmarkSyncUrl), false)
            } else {
                importBookmarks(Uri.parse(config.bookmarkSyncUrl))
            }
        } else {
            dialogManager.showCreateOrOpenBookmarkFileDialog(
                { BrowserUnit.createBookmarkFilePicker(createBookmarkFileLauncher) },
                { BrowserUnit.openBookmarkFilePicker(openBookmarkFileLauncher) }
            )
        }
    }

    private fun linkToBookmarkSyncFile(result: ActivityResult) {
        val uri = preprocessActivityResult(result) ?: return
        importBookmarks(uri)
        config.bookmarkSyncUrl = uri.toString()
    }

    private fun createBookmarkSyncFile(result: ActivityResult) {
        val uri = preprocessActivityResult(result) ?: return
        exportBookmarks(uri)
        config.bookmarkSyncUrl = uri.toString()
    }

    fun preprocessActivityResult(result: ActivityResult): Uri? {
        if (result.resultCode != FragmentActivity.RESULT_OK) return null
        val uri = result.data?.data ?: return null
        context.contentResolver
            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return uri
    }


    companion object {
        private const val DATABASE_PATH = "/data/data/info.plateaukao.einkbro/databases/"
        private const val SHARED_PREFS_PATH = "/data/data/info.plateaukao.einkbro/shared_prefs/"
    }
}

private fun List<Bookmark>.toJsonString(): String {
    val jsonArrays = JSONArray()
    this.map { it.toJsonObject() }.forEach { jsonArrays.put(it) }

    return jsonArrays.toString()
}

private fun Bookmark.toJsonObject(): JSONObject =
    JSONObject().apply {
        put("id", id)
        put("title", title)
        put("url", url)
        put("isDirectory", isDirectory)
        put("parent", parent)
    }

private fun JSONObject.toBookmark(): Bookmark =
    Bookmark(
        optString("title"),
        optString("url"),
        optBoolean("isDirectory"),
        optInt("parent")
    ).apply { id = optInt("id") }
