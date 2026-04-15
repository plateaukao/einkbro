package io.github.edsuns.adfilter.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adblockrust.AdBlockRustClient
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.impl.AdFilterImpl
import io.github.edsuns.adfilter.impl.Constants.KEY_CHECK_LICENSE
import io.github.edsuns.adfilter.impl.Constants.KEY_DOWNLOADED_DATA
import io.github.edsuns.adfilter.impl.Constants.KEY_FILTERS_COUNT
import io.github.edsuns.adfilter.impl.Constants.KEY_FILTER_ID
import io.github.edsuns.adfilter.impl.Constants.KEY_FILTER_NAME
import io.github.edsuns.adfilter.impl.Constants.KEY_RAW_CHECKSUM
import io.github.edsuns.adfilter.util.Checksum
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2021/1/5.
 */
internal class InstallationWorker(context: Context, params: WorkerParameters) : Worker(
    context,
    params
) {
    private val binaryDataStore = (AdFilter.get(applicationContext) as AdFilterImpl).binaryDataStore

    override fun doWork(): Result {
        val id = inputData.getString(KEY_FILTER_ID) ?: return Result.failure()
        val downloadedDataName = inputData.getString(KEY_DOWNLOADED_DATA) ?: return Result.failure()
        val rawChecksum = inputData.getString(KEY_RAW_CHECKSUM) ?: return Result.failure()
        val checkLicense = inputData.getBoolean(KEY_CHECK_LICENSE, false)
        try {
            val rawData = binaryDataStore.loadData(downloadedDataName)
            val dataStr = String(rawData)
            val name = extractTitle(dataStr)
            val checksum = Checksum(dataStr)

            val filtersCount = persistFilterData(id, rawData)
            binaryDataStore.clearData(downloadedDataName)
            return Result.success(
                workDataOf(
                    KEY_FILTERS_COUNT to filtersCount,
                    KEY_FILTER_NAME to name,
                    KEY_RAW_CHECKSUM to checksum.checksumCalc
                )
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to install filter: $id")
            try { binaryDataStore.clearData(downloadedDataName) } catch (_: Exception) {}
            return Result.failure()
        }
    }

    private val licenseRegexp = Regex(
        "^\\s*!\\s*licen[sc]e[\\s\\-:]+([\\S ]+)$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    /**
     * Check if the filter includes a license.
     * Returning false often means that the filter is invalid.
     */
    private fun validateLicense(data: String): Boolean = licenseRegexp.containsMatchIn(data)

    private val titleRegexp = Regex(
        "^\\s*!\\s*title[\\s\\-:]+([\\S ]+)$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    private fun extractTitle(data: String): String? = titleRegexp.find(data)?.groupValues?.get(1)

    private fun persistFilterData(id: String, rawBytes: ByteArray): Int {
        if (rawBytes.isEmpty()) {
            return 0
        }
        val client = AdBlockRustClient(id)
        client.loadBasicData(rawBytes, true)
        binaryDataStore.saveData(id, client.getProcessedData())
        return 0
    }
}