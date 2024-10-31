package io.github.edsuns.adfilter.impl

import android.content.Context
import androidx.work.*
import io.github.edsuns.adfilter.DownloadState
import io.github.edsuns.adfilter.Filter
import io.github.edsuns.adfilter.FilterViewModel
import io.github.edsuns.adfilter.impl.Constants.KEY_CHECK_LICENSE
import io.github.edsuns.adfilter.impl.Constants.KEY_DOWNLOAD_URL
import io.github.edsuns.adfilter.impl.Constants.KEY_FILTER_ID
import io.github.edsuns.adfilter.impl.Constants.KEY_RAW_CHECKSUM
import io.github.edsuns.adfilter.impl.Constants.TAG_INSTALLATION
import io.github.edsuns.adfilter.workers.DownloadWorker
import io.github.edsuns.adfilter.workers.InstallationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Edsuns@qq.com on 2021/7/29.
 */
internal class FilterViewModelImpl (
    context: Context,
    private val filterDataLoader: FilterDataLoader
) : FilterViewModel {

    internal val sharedPreferences: FilterSharedPreferences =
        FilterSharedPreferences(context)

    private val workManager: WorkManager = WorkManager.getInstance(context)

    override val workInfo: StateFlow<List<WorkInfo>> =
        workManager.getWorkInfosByTagLiveData(TAG_FILTER_WORK).let {
            val stateFlow = MutableStateFlow(emptyList<WorkInfo>())
            it.observeForever { stateFlow.value = it }
            stateFlow
        }

    /**
     * Count of enabled filters (excluding custom filter).
     */
    private val _enabledFilterCount = MutableStateFlow(0)
    override val enabledFilterCount: StateFlow<Int> = _enabledFilterCount.asStateFlow()

    internal fun updateEnabledFilterCount() {
        _enabledFilterCount.value = filterDataLoader.detector.clients.size
    }

    /**
     * [Filter.id] to [Filter]
     */
    private val _filterMap: MutableStateFlow<LinkedHashMap<String, Filter>> =
        MutableStateFlow(Json.decodeFromString(sharedPreferences.filterMap))
    private val filterMap: StateFlow<LinkedHashMap<String, Filter>>  = _filterMap.asStateFlow()

    /**
     * All added filters.
     * [Filter.id] to [Filter]
     * @see filterMap
     */
    override val filters: StateFlow<LinkedHashMap<String, Filter>> = filterMap
    override fun setFilters(filters: LinkedHashMap<String, Filter>) {
        _filterMap.value = filters
        saveFilterMap()
    }


    /**
     * [WorkRequest.getId] to [Filter.id]
     */
    internal val downloadFilterIdMap: HashMap<String, String> by lazy { sharedPreferences.downloadFilterIdMap }

    /**
     * Used to observe download has been added or removed.
     * [WorkRequest.getId] to [Filter.id]
     * @see downloadFilterIdMap
     */
    override val workToFilterMap: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())

    init {
        workManager.pruneWork()
        // clear bad running download state
        filters.value.values.forEach {
            if (it.downloadState.isRunning) {
                val list = workManager.getWorkInfosForUniqueWork(it.id).get()
                if (list == null || list.isEmpty()) {
                    it.downloadState = DownloadState.FAILED
                    flushFilter()
                } else {
                    if (list[0].state == WorkInfo.State.ENQUEUED
                        && it.downloadState != DownloadState.ENQUEUED
                    ) {
                        it.downloadState = DownloadState.ENQUEUED
                        flushFilter()
                    }
                }
            }
        }
    }

    override fun addFilter(name: String, url: String): Filter {
        val filter = Filter(url)
        filter.name = name
        filterMap.value.get(filter.id)?.let {
            return it
        }
        filterMap.value.set(filter.id, filter)
        flushFilter()
        return filter
    }

    override fun removeFilter(id: String) {
        cancelDownload(id)
        filterDataLoader.remove(id)
        filterMap.value.remove(id)
        flushFilter()
    }

    override fun setFilterEnabled(id: String, enabled: Boolean, post: Boolean) {
        filterMap.value[id]?.let {
            val enableMask = enabled && it.hasDownloaded()
            if (it.isEnabled != enableMask) {
                if (enableMask)
                    enableFilter(it)
                else
                    disableFilter(it)
                // refresh
                if (post) _filterMap.value = filterMap.value
                saveFilterMap()
            }
        }
    }

    internal fun enableFilter(filter: Filter) {
        if (filter.filtersCount > 0) {
            filterDataLoader.load(filter.id)
            filter.isEnabled = true
            updateEnabledFilterCount()
            flushFilter()
        }
    }

    private fun disableFilter(filter: Filter) {
        filterDataLoader.unload(filter.id)
        filter.isEnabled = false
        updateEnabledFilterCount()
        flushFilter()
    }

    override fun renameFilter(id: String, name: String) {
        filterMap.value.get(id)?.let {
            it.name = name
            flushFilter()
        }
    }

    override fun isCustomFilterEnabled(): Boolean = filterDataLoader.isCustomFilterEnabled()

    override fun enableCustomFilter() {
        if (!isCustomFilterEnabled()) {
            filterDataLoader.load(FilterDataLoader.ID_CUSTOM)
        }
    }

    override fun disableCustomFilter() {
        filterDataLoader.unloadCustomFilter()
    }

    override fun download(id: String) {
        if (downloadFilterIdMap.any { it.value == id }) {
            return
        }
        filterMap.value.get(id)?.let {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()
            val inputData = workDataOf(
                KEY_FILTER_ID to it.id,
                KEY_DOWNLOAD_URL to it.url
            )
            val download =
                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setConstraints(constraints)
                    .addTag(TAG_FILTER_WORK)
                    .setInputData(inputData)
                    .build()
            val install =
                OneTimeWorkRequestBuilder<InstallationWorker>()
                    .addTag(TAG_FILTER_WORK)
                    .addTag(TAG_INSTALLATION)
                    .setInputData(
                        workDataOf(
                            KEY_RAW_CHECKSUM to it.checksum,
                            KEY_CHECK_LICENSE to true
                        )
                    )
                    .build()
            val continuation = workManager.beginUniqueWork(
                it.id, ExistingWorkPolicy.KEEP, download
            ).then(install)
            // record worker ids
            downloadFilterIdMap[download.id.toString()] = it.id
            downloadFilterIdMap[install.id.toString()] = it.id
            sharedPreferences.downloadFilterIdMap = downloadFilterIdMap
            // notify download work added
            workToFilterMap.value = downloadFilterIdMap
            // mark the beginning of the download
            it.downloadState = DownloadState.NONE
            // start the work
            continuation.enqueue()
        }
    }

    override fun cancelDownload(id: String) {
        workManager.cancelUniqueWork(id)
    }

    internal fun flushFilter() {
        saveFilterMap()
    }

    private fun saveFilterMap() {
        sharedPreferences.filterMap = Json.encodeToString(filterMap.value)
        _filterMap.value = Json.decodeFromString(sharedPreferences.filterMap)
    }

    companion object {
        private const val TAG_FILTER_WORK = "TAG_FILTER_WORK"
    }
}