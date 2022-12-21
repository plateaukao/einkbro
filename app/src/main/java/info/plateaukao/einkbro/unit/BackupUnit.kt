package info.plateaukao.einkbro.unit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.unit.HelperUnit.copyDirectory
import info.plateaukao.einkbro.view.NinjaToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class BackupUnit(
    private val context: Context,
    private val activity: Activity,
): KoinComponent {
    private val manager: BookmarkManager by inject()

    fun backup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            NinjaToast.show(activity, activity.getString(R.string.not_supported))
            return
        }

        val sd = activity.getExternalFilesDir(null)
        val data = Environment.getDataDirectory()
        val previewsPathApp = "//data//" + activity.packageName + "//"
        val previewsPathBackup = "browser_backup//data//"
        val previewsFolderApp = File(data, previewsPathApp)
        val previewsFolderBackup = File(sd, previewsPathBackup)

        makeBackupDir()
        BrowserUnit.deleteDir(previewsFolderBackup)
        copyDirectory(previewsFolderApp, previewsFolderBackup)
        backupUserPrefs(activity)
        NinjaToast.show(activity, activity.getString(R.string.toast_export_successful) + "browser_backup")
    }

    fun restore() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            NinjaToast.show(activity, activity.getString(R.string.not_supported))
            return
        }

        val sd = activity.getExternalFilesDir(null)
        val data = Environment.getDataDirectory()
        val previewsPathApp = "//data//" + activity.packageName + "//"
        val previewsPathBackup = "browser_backup//data//"
        val previewsFolderApp = File(data, previewsPathApp)
        val previewsFolderBackup = File(sd, previewsPathBackup)

        if (Build.VERSION.SDK_INT in 23..28) {
            val hasWriteExternalStoragePermission =
                activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                HelperUnit.needGrantStoragePermission(activity)
            } else {
                copyDirectory(previewsFolderBackup, previewsFolderApp)
                restoreUserPrefs()
            }
        } else {
            copyDirectory(previewsFolderBackup, previewsFolderApp)
            restoreUserPrefs()
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun makeBackupDir() {
        val backupDir = File(activity.getExternalFilesDir(null), "browser_backup//")
        val noMedia = File(backupDir, "//.nomedia")
        if (!HelperUnit.needGrantStoragePermission(activity)) {
            if (!backupDir.exists()) {
                try {
                    backupDir.mkdirs()
                    noMedia.createNewFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun backupUserPrefs(context: Context) {
        val prefsFile = File(context.filesDir, "../shared_prefs/" + context.packageName + "_preferences.xml")
        val backupFile = File(
            context.getExternalFilesDir(null),
            "browser_backup/preferenceBackup.xml"
        )
        try {
            val src = FileInputStream(prefsFile).channel
            val dst = FileOutputStream(backupFile).channel
            dst.transferFrom(src, 0, src.size())
            src.close()
            dst.close()
            NinjaToast.show(context, "Backed up user prefs to " + backupFile.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ApplySharedPref")
    private fun restoreUserPrefs() {
        val backupFile = File(
            context.getExternalFilesDir(null),
            "browser_backup/preferenceBackup.xml"
        )
        val error: String
        try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sharedPreferences.edit()
            val inputStream: InputStream = FileInputStream(backupFile)
            val docFactory = DocumentBuilderFactory.newInstance()
            val docBuilder = docFactory.newDocumentBuilder()
            val doc = docBuilder.parse(inputStream)
            val root = doc.documentElement
            var child = root.firstChild
            while (child != null) {
                if (child.nodeType == Node.ELEMENT_NODE) {
                    val element = child as Element
                    val type = element.nodeName
                    val name = element.getAttribute("name")

                    // In my app, all prefs seem to get serialized as either "string" or
                    // "boolean" - this will need expanding if yours uses any other types!
                    if (type == "string") {
                        val value = element.textContent
                        editor.putString(name, value)
                    } else if (type == "boolean") {
                        val value = element.getAttribute("value")
                        editor.putBoolean(name, value == "true")
                    }
                }
                child = child.nextSibling
            }
            editor.commit()
            NinjaToast.show(context, "Restored user prefs from " + backupFile.absolutePath)
            return
        } catch (e: IOException) {
            error = e.message ?: ""
            e.printStackTrace()
        } catch (e: SAXException) {
            error = e.message ?: ""
            e.printStackTrace()
        } catch (e: ParserConfigurationException) {
            error = e.message ?: ""
            e.printStackTrace()
        }
        Toast.makeText(
            context,
            """Failed to restore user prefs from ${backupFile.absolutePath} - $error""",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun importBookmarks(lifecycleScope: CoroutineScope, uri: Uri) {
        val context = context ?: return

        lifecycleScope.launch {
            try {
                context.contentResolver.openInputStream(uri).use {
                    val jsonString = it?.bufferedReader()?.readText() ?: ""
                    val bookmarkArray = JSONArray(jsonString)
                    if (bookmarkArray.length() != 0) {
                        manager.deleteAll()
                        for (i in 0 until bookmarkArray.length()) {
                            val bookmark = (bookmarkArray[i] as JSONObject).toBookmark()
                            manager.insert(bookmark)
                        }
                    }
                }
                Toast.makeText(context, "Bookmarks are imported", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Bookmarks import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportBookmarks(lifecycleScope: CoroutineScope, uri: Uri) {
        lifecycleScope.launch {
            val bookmarks = manager.getAllBookmarks()
            try {
                context.contentResolver.openOutputStream(uri).use {
                    it?.write(bookmarks.toJsonString().toByteArray())
                }
                Toast.makeText(context, "Bookmarks are exported", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Bookmarks export failed", Toast.LENGTH_SHORT).show()
            }
        }
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
