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
internal class FilterViewModelImpl(
    context: Context,
    private val filterDataLoader: FilterDataLoader,
) : FilterViewModel {

    internal val sharedPreferences: FilterSharedPreferences =
        FilterSharedPreferences(context)

    private val workManager: WorkManager = WorkManager.getInstance(context)

    private val _workInfo: MutableStateFlow<List<WorkInfo>> = MutableStateFlow(emptyList())
    override val workInfo: StateFlow<List<WorkInfo>> = _workInfo.asStateFlow().apply {
        workManager.getWorkInfosByTagLiveData(TAG_FILTER_WORK).let {
            it.observeForever { _workInfo.value = it }
        }
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
    private val _filterMap: MutableStateFlow<Map<String, Filter>> =
        MutableStateFlow(Json.decodeFromString(sharedPreferences.filterMap))

    override val filters: StateFlow<Map<String, Filter>> = _filterMap.asStateFlow()
    override fun updateFilterByFilterId(id: String, filter: Filter) {
        _filterMap.value = filters.value.toMutableMap().apply { set(id, filter) }
        saveFilterMap()
    }

    override fun updateFilters() {
        _filterMap.value = filters.value.toMutableMap()
    }


    /**
     * Used to observe download has been added or removed.
     * [WorkRequest.getId] to [Filter.id]
     */
    private val _workToFilterMap: MutableStateFlow<Map<String, String>> = MutableStateFlow(HashMap<String, String>())
    override val workToFilterMap: StateFlow<Map<String, String>> = _workToFilterMap.asStateFlow()
    override fun updateWorkToFilterMap(map: Map<String, String>) {
        _workToFilterMap.value = map
    }

    init {
        workManager.pruneWork()
        // clear bad running download state
        filters.value.values.forEach { filter ->
            if (filter.downloadState.isRunning) {
                val list = workManager.getWorkInfosForUniqueWork(filter.id).get()
                if (list == null || list.isEmpty()) {
                    val updatedFilter = filter.copy(downloadState = DownloadState.FAILED)
                    updateFilterByFilterId(filter.id, updatedFilter)
                } else {
                    if (list[0].state == WorkInfo.State.ENQUEUED
                        && filter.downloadState != DownloadState.ENQUEUED
                    ) {
                        val updatedFilter = filter.copy(downloadState = DownloadState.ENQUEUED)
                        updateFilterByFilterId(filter.id, updatedFilter)
                    }
                }
            }
        }
    }

    override fun addFilter(name: String, url: String): Filter {
        val newFilter = Filter(url, name)
        _filterMap.value = filters.value.toMutableMap().apply { set(newFilter.id, newFilter) }
        updateFilterByFilterId(newFilter.id, newFilter)
        return newFilter
    }

    override fun removeFilter(id: String) {
        cancelDownload(id)
        filterDataLoader.remove(id)
        _filterMap.value = filters.value.toMutableMap().apply { remove(id) }
        flushFilter()
    }

    override fun setFilterEnabled(id: String, enabled: Boolean, post: Boolean) {
        filters.value[id]?.let {
            val enableMask = enabled && it.hasDownloaded()
            if (it.isEnabled != enableMask) {
                if (enableMask)
                    enableFilter(it)
                else
                    disableFilter(it)

                _filterMap.value = filters.value.toMutableMap()
                saveFilterMap()
            }
        }
    }

    internal fun enableFilter(filter: Filter) {
        if (filter.filtersCount > 0) {
            filterDataLoader.load(filter.id)
            filter.copy(isEnabled = true).let { updateFilterByFilterId(it.id, it) }
            updateEnabledFilterCount()
        }
    }

    private fun disableFilter(filter: Filter) {
        filterDataLoader.unload(filter.id)
        filter.copy(isEnabled = false).let { updateFilterByFilterId(it.id, it) }
        updateEnabledFilterCount()
    }

    override fun renameFilter(id: String, name: String) {
        filters.value[id]?.let {
            val updatedFilter = it.copy(name = name)
            updateFilterByFilterId(id, updatedFilter)
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
        if (workToFilterMap.value.any { it.value == id }) {
            return
        }
        filters.value[id]?.let {
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
            _workToFilterMap.value = _workToFilterMap.value.toMutableMap().apply {
                this[download.id.toString()] = it.id
                this[install.id.toString()] = it.id
            }
            // notify download work added
            // mark the beginning of the download
            filters.value[id]?.copy(downloadState = DownloadState.NONE)?.let { updateFilterByFilterId(id, it) }
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
        sharedPreferences.filterMap = Json.encodeToString(_filterMap.value)
    }

    companion object {
        private const val TAG_FILTER_WORK = "TAG_FILTER_WORK"
    }
}