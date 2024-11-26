package io.github.edsuns.adfilter.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adblockclient.AdBlockClient
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.impl.AdFilterImpl
import io.github.edsuns.adfilter.impl.Constants.KEY_CHECK_LICENSE
import io.github.edsuns.adfilter.impl.Constants.KEY_DOWNLOADED_DATA
import io.github.edsuns.adfilter.impl.Constants.KEY_FILTERS_COUNT
import io.github.edsuns.adfilter.impl.Constants.KEY_FILTER_ID
import io.github.edsuns.adfilter.impl.Constants.KEY_FILTER_NAME
import io.github.edsuns.adfilter.impl.Constants.KEY_RAW_CHECKSUM
import io.github.edsuns.adfilter.util.Checksum

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
        val rawData = binaryDataStore.loadData(downloadedDataName)
        val dataStr = String(rawData)
        val name = extractTitle(dataStr)
        val checksum = Checksum(dataStr)

        // TODO: need to comment out this section to avoid the list without checksum and license
        // reject filter that doesn't include both checksum and license if checkLicense is true
//        if (checksum.checksumIn == null && checkLicense && !validateLicense(dataStr)) {
//            Timber.v("Filter is invalid: $id")
//            return Result.success()
//        }
//        Timber.v("Checksum: $rawChecksum, ${checksum.checksumIn}, ${checksum.checksumCalc}, ${checksum.validate()}")
//        if (!checksum.validate()) {
//            return Result.failure()
//        }
//        if (checksum.validate(rawChecksum)) {
//            Timber.v("Filter is up to date: $id")
//            return Result.success(
//                workDataOf(
//                    KEY_FILTER_NAME to name,
//                    KEY_ALREADY_UP_TO_DATE to true
//                )
//            )
//        }
        val filtersCount = persistFilterData(id, rawData)
        binaryDataStore.clearData(downloadedDataName)
        return Result.success(
            workDataOf(
                KEY_FILTERS_COUNT to filtersCount,
                KEY_FILTER_NAME to name,
                KEY_RAW_CHECKSUM to checksum.checksumCalc
            )
        )
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
        val client = AdBlockClient(id)
        if (rawBytes.isEmpty()) {
            return 0
        }
        client.loadBasicData(rawBytes, true)
        binaryDataStore.saveData(id, client.getProcessedData())
        return client.getFiltersCount()
    }
}