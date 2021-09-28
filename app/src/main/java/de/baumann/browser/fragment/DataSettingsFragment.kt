package de.baumann.browser.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.Ninja.databinding.DialogActionBinding
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.unit.BrowserUnit
import de.baumann.browser.unit.HelperUnit.needGrantStoragePermission
import de.baumann.browser.unit.HelperUnit.setBottomSheetBehavior
import de.baumann.browser.util.Constants
import de.baumann.browser.view.NinjaToast
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent.inject
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.*
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class DataSettingsFragment : PreferenceFragmentCompat(), KoinComponent {
    private val manager: BookmarkManager by inject()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_data, rootKey)
        val sd = requireActivity().getExternalFilesDir(null)
        val data = Environment.getDataDirectory()
        val previewspathApp = "//data//" + requireActivity().packageName + "//"
        val previewspathBackup = "browser_backup//data//"
        val previewsfolderApp = File(data, previewspathApp)
        val previewsfolderBackup = File(sd, previewspathBackup)
        findPreference<Preference>("data_exDB")!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    val activity = activity ?: return@OnPreferenceClickListener false
                    val dialog = BottomSheetDialog(requireActivity())
                    val dialogView = DialogActionBinding.inflate(activity.layoutInflater)
                    dialogView.dialogText.setText(R.string.toast_backup)
                    dialogView.actionOk.setOnClickListener {
                        dialog.dismiss()
                        try {
                            if (!needGrantStoragePermission(requireActivity())) {

                                makeBackupDir()
                                BrowserUnit.deleteDir(previewsfolderBackup)
                                copyDirectory(previewsfolderApp, previewsfolderBackup)
                                backupUserPrefs(activity)
                                NinjaToast.show(activity, getString(R.string.toast_export_successful) + "browser_backup")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    dialogView.actionCancel.setOnClickListener { dialog.dismiss() }
                    dialog.setContentView(dialogView.root)
                    dialog.show()
                    setBottomSheetBehavior(dialog, dialogView.root, BottomSheetBehavior.STATE_EXPANDED)
                    false
                }
        findPreference<Preference>("data_imDB")!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    val textView: TextView
                    val action_ok: Button
                    val action_cancel: Button
                    val dialog = BottomSheetDialog(requireActivity())
                    val dialogView: View = View.inflate(activity, R.layout.dialog_action, null)
                    textView = dialogView.findViewById(R.id.dialog_text)
                    textView.setText(R.string.hint_database)
                    action_ok = dialogView.findViewById(R.id.action_ok)
                    action_ok.setOnClickListener { view: View? ->
                        dialog.cancel()
                        try {
                            if (Build.VERSION.SDK_INT in 23..28) {
                                val hasWRITE_EXTERNAL_STORAGE =
                                        requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                                    needGrantStoragePermission(requireActivity())
                                    dialog.cancel()
                                } else {
                                    //BrowserUnit.deleteDir(previewsFolder_app);
                                    copyDirectory(previewsfolderBackup, previewsfolderApp)
                                    restoreUserPrefs(activity)
                                    dialogRestart()
                                }
                            } else {
                                //BrowserUnit.deleteDir(previewsFolder_app);
                                copyDirectory(previewsfolderBackup, previewsfolderApp)
                                restoreUserPrefs(activity)
                                dialogRestart()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    action_cancel = dialogView.findViewById(R.id.action_cancel)
                    action_cancel.setOnClickListener { view: View? -> dialog!!.cancel() }
                    dialog.setContentView(dialogView)
                    dialog.show()
                    setBottomSheetBehavior(dialog!!, dialogView, BottomSheetBehavior.STATE_EXPANDED)
                    false
                }
        findPreference<Preference>("data_export_bookmarks")?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    showBookmarkFilePicker()
                    true
                }
        findPreference<Preference>("data_import_bookmarks")?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    showImportBookmarkFilePicker()
                    true
                }
    }

    private fun showBookmarkFilePicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_TEXT
        intent.putExtra(Intent.EXTRA_TITLE, "bookmark.txt")
        startActivityForResult(intent, EXPORT_BOOKMARKS_REQUEST_CODE)
    }

    private fun showImportBookmarkFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = Constants.MIME_TYPE_ANY
        startActivityForResult(intent, IMPORT_BOOKMARKS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == EXPORT_BOOKMARKS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = intent?.data ?: return
            exportBookmarks(uri)
        } else if (requestCode == IMPORT_BOOKMARKS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = intent?.data ?: return
            importBookmarks(uri)
        }
    }

    private fun importBookmarks(uri: Uri) {
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
            } finally {
                manager.release()
            }
        }
    }

    private fun exportBookmarks(uri: Uri) {
        val context = context ?: return

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
            } finally {
                manager.release()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun makeBackupDir() {
        val backupDir = File(requireActivity().getExternalFilesDir(null), "browser_backup//")
        val noMedia = File(backupDir, "//.nomedia")
        if (!needGrantStoragePermission(requireActivity())) {
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

    private fun dialogRestart() {
        val sp = preferenceScreen.sharedPreferences
        sp.edit().putInt("restart_changed", 1).apply()
    }

    // If targetLocation does not exist, it will be created.
    @Throws(IOException::class)
    private fun copyDirectory(sourceLocation: File, targetLocation: File) {
        if (sourceLocation.isDirectory) {
            if (sourceLocation.name == "app_webview" || sourceLocation.name == "cache") {
                return
            }
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw IOException("Cannot create dir " + targetLocation.absolutePath)
            }
            val children = sourceLocation.list()
            for (aChildren in Objects.requireNonNull(children)) {
                copyDirectory(
                        File(sourceLocation, aChildren),
                        File(targetLocation, aChildren)
                )
            }
        } else {
            // make sure the directory we plan to store the recording in exists
            val directory = targetLocation.parentFile
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw IOException("Cannot create dir " + directory.absolutePath)
            }
            val `in`: InputStream = FileInputStream(sourceLocation)
            val out: OutputStream = FileOutputStream(targetLocation)
            // Copy the bits from InputStream to OutputStream
            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            `in`.close()
            out.close()
        }
    }

    companion object {
        private const val EXPORT_BOOKMARKS_REQUEST_CODE = 2345
        private const val IMPORT_BOOKMARKS_REQUEST_CODE = 2346
        private fun backupUserPrefs(context: Context?) {
            val prefsFile = File(context!!.filesDir, "../shared_prefs/" + context.packageName + "_preferences.xml")
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
        private fun restoreUserPrefs(context: Context?) {
            val backupFile = File(
                    context!!.getExternalFilesDir(null),
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