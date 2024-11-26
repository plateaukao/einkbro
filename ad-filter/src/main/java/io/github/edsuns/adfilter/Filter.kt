package io.github.edsuns.adfilter

import io.github.edsuns.adfilter.util.sha1
import kotlinx.serialization.Serializable

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
@Serializable
data class Filter(
    val url: String,
    val name: String = "",
    val isEnabled: Boolean = false,
    val downloadState: DownloadState = DownloadState.NONE,
    val updateTime: Long = -1L,
    val filtersCount: Int = 0,
    val checksum: String = "",
) {
    val id by lazy { url.sha1 }
    fun hasDownloaded() = updateTime > 0
}

enum class DownloadState {
    ENQUEUED, DOWNLOADING, INSTALLING, SUCCESS, FAILED, CANCELLED, NONE;

    val isRunning
        get() = when (this) {
            ENQUEUED, DOWNLOADING, INSTALLING -> true
            else -> false
        }
}