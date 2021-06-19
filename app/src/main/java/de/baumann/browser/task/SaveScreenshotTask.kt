package de.baumann.browser.task

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebView
import android.widget.ProgressBar
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.HelperUnit.fileName
import de.baumann.browser.unit.HelperUnit.grantPermissionsStorage
import de.baumann.browser.unit.ViewUnit.capture
import de.baumann.browser.unit.ViewUnit.getDensity
import de.baumann.browser.unit.ViewUnit.getWindowWidth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

class SaveScreenshotTask(
    private val context: Context,
    private val webView: WebView,
) {

    private val configManager: ConfigManager = ConfigManager(context)

    @SuppressLint("NewApi")
    suspend fun execute() {
        val url = webView.url ?: return

        // progress dialog
        val progressDialog = AlertDialog.Builder(context).setView(ProgressBar(context)).show()

        //background
        val title = fileName(url)
        val windowWidth = getWindowWidth(context).toFloat()
        val contentHeight = webView.contentHeight * getDensity(context)

        if (Build.VERSION.SDK_INT in 23..28) {
            val hasWriteExternalStorage = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (hasWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
                grantPermissionsStorage(context as Activity)
                return
            }
        }

        val uri = captureAndSaveImage(webView, windowWidth, contentHeight, title) ?: return

        // post
        progressDialog.dismiss()
        showSavedScreenshot(uri)
    }

    suspend fun captureAndSaveImage(webView: WebView, width: Float, height: Float, name: String): Uri? {
        val bitmap = capture(webView, width, height)
        var uri: Uri? = null
        withContext(Dispatchers.IO) {
            val fos: OutputStream
            uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/" + "Screenshots/")
                }
                val resolver: ContentResolver = context.contentResolver
                val nonNullUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext null
                fos = resolver.openOutputStream(nonNullUri) ?: return@withContext null
                nonNullUri
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory("Screenshots")
                    .toString() + File.separator
                val file = File(imagesDir)
                if (!file.exists()) {
                    file.mkdir()
                }
                val image = File(imagesDir, "$name.jpg")
                fos = FileOutputStream(image)
                Uri.fromFile(image)
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            Objects.requireNonNull(fos).flush()
            fos.close()
        }

        return uri
    }

    private fun showSavedScreenshot(uri: Uri) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, "image/*")
        }

        context.startActivity(intent)
    }
}