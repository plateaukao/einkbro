package info.plateaukao.einkbro.unit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.view.EBToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

object AppUpdater {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun upgradeToLatestRelease(
        context: Context,
        progressCallback: (suspend (Float) -> Unit)? = null,
    ) {
        if (isAppInstalledFromPlayStore(context)) {
            progressCallback?.invoke(1.0f)
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

        progressCallback?.invoke(0.1f)
        val url = "https://api.github.com/repos/plateaukao/einkbro/releases"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            try {
                progressCallback?.invoke(0.2f)
                val jsonArray = JSONArray(response.body!!.string())
                val latestRelease = jsonArray.getJSONObject(0)
                val tagName = latestRelease.getString("tag_name").replace("v", "")
                val isPreRelease = latestRelease.getBoolean("prerelease")

                if (tagName > BuildConfig.VERSION_NAME && !isPreRelease) {
                    progressCallback?.invoke(0.3f)

                    val downloadUrl = latestRelease.getJSONArray("assets")
                        .getJSONObject(0)
                        .getString("browser_download_url")

                    val file = File.createTempFile("temp", ".apk", context.cacheDir)
                    downloadFileWithProgress(downloadUrl, file.absolutePath, progressCallback, 0.4f, 0.5f)

                    progressCallback?.invoke(0.9f)
                    installApkFromFile(context, file)
                    progressCallback?.invoke(1.0f)
                } else {
                    progressCallback?.invoke(1.0f)
                    withContext(Dispatchers.Main) {
                        EBToast.show(context, "Already up to date")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                progressCallback?.invoke(0.0f)
                withContext(Dispatchers.Main) {
                    EBToast.show(context, "Something went wrong")
                }
            }
        }
    }

    private suspend fun installApkFromFile(context: Context, file: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            file
        )

        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = apkUri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
            .apply {
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            }

        withContext(Dispatchers.Main) {
            context.startActivity(intent)
        }
    }

    private suspend fun downloadFileWithProgress(
        url: String,
        destinationPath: String,
        progressCallback: (suspend (Float) -> Unit)?,
        baseProgress: Float,
        progressScale: Float
    ) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download file: $response")

            val responseBody = response.body ?: throw IOException("Response body is null")
            val contentLength = responseBody.contentLength()
            val inputStream = responseBody.byteStream()

            FileOutputStream(destinationPath).use { outputStream ->
                val buffer = ByteArray(8192)
                var totalBytesRead = 0L
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val progress = baseProgress + (totalBytesRead.toFloat() / contentLength.toFloat()) * progressScale
                        progressCallback?.invoke(progress)
                    }
                }
            }
        }
    }

    suspend fun upgradeFromSnapshot(
        context: Context,
        progressCallback: (suspend (Float) -> Unit)? = null,
    ) {
        val url =
            "https://nightly.link/plateaukao/einkbro/workflows/buid-app-workflow.yaml/main/app-arm64-v8a-release.apk.zip"
        try {
            progressCallback?.invoke(0.1f)

            val zipFile = File.createTempFile("snapshot", ".zip", context.cacheDir)
            downloadFileWithProgress(url, zipFile.absolutePath, progressCallback, 0.2f, 0.5f)

            progressCallback?.invoke(0.7f)
            extractApkAndInstallWithProgress(zipFile.inputStream(), context, progressCallback)
            zipFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            progressCallback?.invoke(0.0f)
            withContext(Dispatchers.Main) {
                EBToast.show(context, "Something went wrong")
            }
        }
    }

    private suspend fun extractApkAndInstallWithProgress(
        inputStream: InputStream?,
        context: Context,
        progressCallback: (suspend (Float) -> Unit)?,
    ) {
        val zipInputStream = ZipInputStream(inputStream)

        var found = false
        var zipEntry = zipInputStream.nextEntry
        while (zipEntry != null) {
            if (zipEntry.name.endsWith(".apk")) {
                val tempFile = File("${context.cacheDir.absolutePath}/app.apk")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                tempFile.createNewFile()
                tempFile.deleteOnExit()

                val entrySize = zipEntry.size // uncompressed size, may be -1
                FileOutputStream(tempFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var totalBytesWritten = 0L
                    var bytesRead: Int
                    while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        totalBytesWritten += bytesRead
                        if (entrySize > 0) {
                            val progress = 0.75f + (totalBytesWritten.toFloat() / entrySize.toFloat()) * 0.15f
                            progressCallback?.invoke(progress)
                        }
                    }
                }

                progressCallback?.invoke(0.9f)
                installApkFromFile(context, tempFile)
                progressCallback?.invoke(1.0f)

                found = true
                break
            }
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.closeEntry()
        zipInputStream.close()

        if (!found) {
            throw IOException("No APK found in snapshot archive")
        }
    }

    @Suppress("DEPRECATION")
    fun isAppInstalledFromPlayStore(context: Context): Boolean {
        val packageName = context.packageName
        val pm = context.packageManager

        return try {
            val installerPackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                pm.getInstallerPackageName(packageName)
            }
            "com.android.vending" == installerPackageName
        } catch (e: Exception) {
            false
        }
    }
}
