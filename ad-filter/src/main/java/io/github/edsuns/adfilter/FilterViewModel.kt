package io.github.edsuns.adfilter

import androidx.work.WorkInfo
import androidx.work.WorkRequest
import kotlinx.coroutines.flow.StateFlow

interface FilterViewModel {
    /**
     * Work info of AdFilter.
     */
    val workInfo: StateFlow<List<WorkInfo>>

    /**
     * Count of enabled filters (excluding custom filter).
     */
    val enabledFilterCount: StateFlow<Int>

    /**
     * All added filters.
     * [Filter.id] to [Filter]
     */
    val filters: StateFlow<Map<String, Filter>>
    fun updateFilterByFilterId(id: String, filter: Filter)
    fun updateFilters()

    /**
     * Used to observe download has been added or removed.
     * Only includes [ENQUEUED], [RUNNING] and [BLOCKED].
     * [WorkRequest.getId] to [Filter.id]
     */
    val workToFilterMap: StateFlow<Map<String, String>>
    fun updateWorkToFilterMap(map: Map<String, String>)

    /**
     * Add a new filter.
     * @param name the name of the filter
     * @param url the subscription url of the filter
     */
    fun addFilter(name: String, url: String): Filter

    /**
     * Remove specified filter.
     * @param id [Filter.id]
     */
    fun removeFilter(id: String)

    /**
     * Enable or disable specified filter with notification.
     * @param id [Filter.id]
     * @param enabled true to enable
     * @param post true to notify changes by [StateFlow]
     */
    fun setFilterEnabled(id: String, enabled: Boolean, post: Boolean = true)

    /**
     * Rename specified filter.
     * @param id [Filter.id]
     * @param name new name of the filter
     */
    fun renameFilter(id: String, name: String)

    /**
     * @return true if custom filter is enabled
     */
    fun isCustomFilterEnabled(): Boolean

    /**
     * Enable custom filter.
     */
    fun enableCustomFilter()

    /**
     * Disable custom filter.
     */
    fun disableCustomFilter()

    /**
     * Download or update filter.
     * @param id [Filter.id]
     */
    fun download(id: String)

    /**
     * Cancel download task.
     * @param id [Filter.id]
     */
    fun cancelDownload(id: String)
}