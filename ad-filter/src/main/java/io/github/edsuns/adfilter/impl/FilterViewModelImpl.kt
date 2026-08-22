package io.github.edsuns.adfilter.impl

import android.content.Context
import io.github.edsuns.adfilter.Filter
import io.github.edsuns.adfilter.FilterViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Edsuns@qq.com on 2021/7/29.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class FilterViewModelImpl(
    context: Context,
    private val filterDataLoader: FilterDataLoader,
    binaryDataStore: BinaryDataStore,
) : FilterViewModel {

    internal val sharedPreferences: FilterSharedPreferences =
        FilterSharedPreferences(context)

    private val updater = FilterUpdater(context, binaryDataStore, ::updateFilter, ::onInstalled)

    override val activeDownloads: StateFlow<Set<String>> get() = updater.activeDownloads

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
        MutableStateFlow(
            try {
                Json.decodeFromString(sharedPreferences.filterMap)
            } catch (_: Exception) {
                emptyMap()
            }
        )

    override val filters: StateFlow<Map<String, Filter>> = _filterMap.asStateFlow()
    override fun updateFilterByFilterId(id: String, filter: Filter) {
        _filterMap.update { it + (id to filter) }
        saveFilterMap()
    }

    /**
     * Atomically replace the filter with [id] by [transform] of its current value.
     * No-op if the filter has been removed in the meantime (e.g. a download
     * finishing after [removeFilter]). Called from the updater's IO threads.
     */
    private fun updateFilter(id: String, transform: (Filter) -> Filter) {
        var changed = false
        _filterMap.update { map ->
            val current = map[id] ?: return@update map
            changed = true
            map + (id to transform(current))
        }
        if (changed) saveFilterMap()
    }

    /**
     * Load freshly installed filter data into the detector right away, so an
     * update takes effect without restarting the app. [Detector.addClient]
     * replaces any client with the same id.
     */
    private fun onInstalled(id: String) {
        val filter = filters.value[id] ?: return
        if (filter.isEnabled && filter.filtersCount > 0) {
            filterDataLoader.load(id)
            updateEnabledFilterCount()
        }
    }

    override fun updateFilters() {
        _filterMap.value = filters.value.toMutableMap()
    }


    init {
        // Downloads do not survive the process. A filter still flagged as running was
        // interrupted by process death rather than by a real failure, so pick it up
        // again (WorkManager used to persist and resume such work). Filters that
        // genuinely FAILED are left alone: nothing retries them automatically.
        try {
            filters.value.values.forEach { filter ->
                if (filter.downloadState.isRunning) {
                    updater.download(filter)
                }
            }
        } catch (_: Exception) {
            // guard against SharedPreferences failures during init
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
        filters.value[id]?.let { updater.download(it) }
    }

    override fun cancelDownload(id: String) {
        updater.cancel(id)
    }

    internal fun flushFilter() {
        saveFilterMap()
    }

    private fun saveFilterMap() {
        sharedPreferences.filterMap = Json.encodeToString(_filterMap.value)
    }
}