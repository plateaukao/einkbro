package io.github.edsuns.adfilter.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.impl.AdFilterImpl
import io.github.edsuns.adfilter.impl.Constants.KEY_DOWNLOADED_DATA
import io.github.edsuns.adfilter.impl.Constants.KEY_DOWNLOAD_URL
import io.github.edsuns.adfilter.impl.Constants.KEY_FILTER_ID
import io.github.edsuns.net.HttpRequest
import timber.log.Timber
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
internal class DownloadWorker(context: Context, params: WorkerParameters) : Worker(
    context,
    params
) {
    private val binaryDataStore = (AdFilter.get(applicationContext) as AdFilterImpl).binaryDataStore

    override fun doWork(): Result {
        val id = inputData.getString(KEY_FILTER_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        Timber.v("Start download: $url $id")
        try {
            val request = HttpRequest(url).timeout(10000).get()
            if (request.isBadStatus) {
                Timber.v("Failed to download (${request.status}): $url $id")
                return Result.failure(inputData)
            }
            // convert to UTF-8 if needed
            val bodyBytes =
                if (request.encoding == StandardCharsets.UTF_8) request.bodyBytes else request.body.toByteArray()
            val dataName = "_$id"
            binaryDataStore.saveData(dataName, bodyBytes)
            return Result.success(
                workDataOf(
                    KEY_FILTER_ID to id,
                    KEY_DOWNLOADED_DATA to dataName
                )
            )
        } catch (e: IOException) {
            Timber.v(e, "Failed to download: $url $id")
        }
        return Result.failure(inputData)
    }
}