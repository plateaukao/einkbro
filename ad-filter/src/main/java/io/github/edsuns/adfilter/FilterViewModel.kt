package io.github.edsuns.adfilter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.*
import androidx.work.WorkRequest
import io.github.edsuns.adfilter.util.None

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
interface FilterViewModel {

    /**
     * Status of the filter master switch.
     */
    val isEnabled: MutableLiveData<Boolean>

    /**
     * Work info of AdFilter.
     */
    val workInfo: LiveData<List<WorkInfo>>

    /**
     * Count of enabled filters (excluding custom filter).
     */
    val enabledFilterCount: LiveData<Int>

    /**
     * All added filters.
     * [Filter.id] to [Filter]
     */
    val filters: LiveData<LinkedHashMap<String, Filter>>

    /**
     * Used to observe download has been added or removed.
     * Only includes [ENQUEUED], [RUNNING] and [BLOCKED].
     * [WorkRequest.getId] to [Filter.id]
     */
    val workToFilterMap: LiveData<Map<String, String>>

    /**
     * Used to notify that the filters have changed (enable, disable, add, remove).
     */
    val onDirty: LiveData<None>

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
     * @param post true to notify changes by [LiveData]
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