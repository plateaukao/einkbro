package info.plateaukao.einkbro.unit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import info.plateaukao.einkbro.preference.ConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.OutputStream

/**
 * Routes file writes to the Supernote `Document/` folder via a persisted SAF tree URI.
 * Android 11+ scoped storage blocks both DownloadManager (path allowlist) and direct
 * File I/O (EPERM) on `/storage/emulated/0/Document/`, so we must go through SAF.
 */
object SupernoteStorage : KoinComponent {

    private val config: ConfigManager by inject()

    @Volatile
    private var pickerLauncher: ActivityResultLauncher<Uri?>? = null

    @Volatile
    private var pendingCallback: ((Uri?) -> Unit)? = null

    fun registerPicker(launcher: ActivityResultLauncher<Uri?>) {
        pickerLauncher = launcher
    }

    fun unregisterPicker(launcher: ActivityResultLauncher<Uri?>) {
        if (pickerLauncher === launcher) {
            pickerLauncher = null
            pendingCallback = null
        }
    }

    fun storedTreeUri(): Uri? = config.browser.supernoteFolderUri?.toUri()

    /** Persist a freshly-granted tree URI and run the queued callback (if any). */
    fun onPickerResult(context: Context, uri: Uri?) {
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (e: Exception) {
                Log.w("SupernoteStorage", "takePersistableUriPermission failed: $e")
            }
            config.browser.supernoteFolderUri = uri.toString()
        }
        val cb = pendingCallback
        pendingCallback = null
        cb?.invoke(uri)
    }

    /**
     * Resolve the persisted tree URI, or prompt the user to grant access.
     * Callback is invoked with the URI on success or null if the user cancels
     * / no launcher is currently registered.
     */
    fun ensureTreeUri(onResult: (Uri?) -> Unit) {
        storedTreeUri()?.let { onResult(it); return }
        val launcher = pickerLauncher
        if (launcher == null) {
            onResult(null)
            return
        }
        pendingCallback = onResult
        try {
            launcher.launch(HelperUnit.supernoteStorageRootInitialUri())
        } catch (e: Exception) {
            pendingCallback = null
            Log.w("SupernoteStorage", "launch picker failed: $e")
            onResult(null)
        }
    }

    /**
     * Open an output stream for [fileName] under the granted tree.
     * Replaces any existing file with the same name.
     */
    fun openOutputStream(
        context: Context,
        treeUri: Uri,
        fileName: String,
        mimeType: String,
    ): Pair<OutputStream, Uri>? {
        return try {
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            tree.findFile(fileName)?.delete()
            val effectiveMime = mimeType.ifBlank { "application/octet-stream" }
            val file = tree.createFile(effectiveMime, fileName) ?: return null
            val stream = context.contentResolver.openOutputStream(file.uri) ?: return null
            stream to file.uri
        } catch (e: Exception) {
            Log.w("SupernoteStorage", "openOutputStream failed: $e")
            null
        }
    }
}
