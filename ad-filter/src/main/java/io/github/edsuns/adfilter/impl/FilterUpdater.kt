package io.github.edsuns.adfilter.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.github.edsuns.adblockclient.AdBlockClient
import io.github.edsuns.adfilter.DownloadState
import io.github.edsuns.adfilter.Filter
import io.github.edsuns.adfilter.util.Checksum
import io.github.edsuns.net.HttpRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Downloads and installs filter lists on a background coroutine scope.
 *
 * Replaces the former WorkManager chain (DownloadWorker -> InstallationWorker).
 * Jobs live only as long as the process; [FilterViewModelImpl] restarts any
 * filter still flagged as running at startup. A failed attempt ends in
 * [DownloadState.FAILED] and is never retried on its own.
 */
internal class FilterUpdater(
    private val context: Context,
    private val binaryDataStore: BinaryDataStore,
    private val updateFilter: (id: String, transform: (Filter) -> Filter) -> Unit,
    /** Called after a filter's data has been installed and its record updated. */
    private val onInstalled: (id: String) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = HashMap<String, Job>()

    /**
     * Ids of filters with a download or installation in progress.
     */
    private val _activeDownloads = MutableStateFlow<Set<String>>(emptySet())
    val activeDownloads: StateFlow<Set<String>> = _activeDownloads.asStateFlow()

    /**
     * Start downloading [filter]. A second call for the same id while the first is
     * still running is ignored (the old ExistingWorkPolicy.KEEP).
     */
    @Synchronized
    fun download(filter: Filter) {
        val id = filter.id
        if (jobs[id]?.isActive == true) return
        _activeDownloads.update { it + id }
        updateFilter(id) { it.copy(downloadState = DownloadState.NONE) }
        jobs[id] = scope.launch {
            try {
                run(id, filter.url)
            } catch (e: CancellationException) {
                updateFilter(id) { it.copy(downloadState = DownloadState.CANCELLED) }
                throw e
            } finally {
                finish(id)
            }
        }
    }

    @Synchronized
    fun cancel(id: String) {
        jobs[id]?.cancel()
    }

    @Synchronized
    private fun finish(id: String) {
        jobs.remove(id)
        _activeDownloads.update { it - id }
    }

    private suspend fun run(id: String, url: String) {
        updateFilter(id) { it.copy(downloadState = DownloadState.ENQUEUED) }
        awaitNetwork()

        updateFilter(id) { it.copy(downloadState = DownloadState.DOWNLOADING) }
        val rawData = try {
            fetch(id, url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to download: $url $id")
            null
        }
        if (rawData == null) {
            updateFilter(id) { it.copy(downloadState = DownloadState.FAILED) }
            return
        }

        updateFilter(id) { it.copy(downloadState = DownloadState.INSTALLING) }
        val rawDataName = "_$id"
        try {
            binaryDataStore.saveData(rawDataName, rawData)
            val dataStr = String(rawData)
            val name = extractTitle(dataStr) ?: ""
            val checksum = Checksum(dataStr).checksumCalc
            val filtersCount = persistFilterData(id, rawData)
            updateFilter(id) {
                it.copy(
                    name = name,
                    // A first download switches the filter on; an update keeps
                    // whatever the user chose.
                    isEnabled = it.isEnabled || !it.hasDownloaded(),
                    downloadState = DownloadState.SUCCESS,
                    updateTime = System.currentTimeMillis(),
                    filtersCount = filtersCount,
                    checksum = checksum,
                )
            }
            onInstalled(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to install filter: $id")
            updateFilter(id) { it.copy(downloadState = DownloadState.FAILED) }
        } finally {
            try {
                binaryDataStore.clearData(rawDataName)
            } catch (_: Exception) {
            }
        }
    }

    private fun fetch(id: String, url: String): ByteArray? {
        Timber.v("Start download: $url $id")
        val request = HttpRequest(url).timeout(10000).get()
        if (request.isBadStatus) {
            Timber.v("Failed to download (${request.status}): $url $id")
            return null
        }
        // convert to UTF-8 if needed
        return if (request.encoding == StandardCharsets.UTF_8) request.bodyBytes
        else request.body.toByteArray()
    }

    private fun persistFilterData(id: String, rawBytes: ByteArray): Int {
        if (rawBytes.isEmpty()) {
            return 0
        }
        val client = AdBlockClient(id)
        client.loadBasicData(rawBytes, true)
        binaryDataStore.saveData(id, client.getProcessedData())
        return client.getFiltersCount()
    }

    private val titleRegexp = Regex(
        "^\\s*!\\s*title[\\s\\-:]+([\\S ]+)$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    private fun extractTitle(data: String): String? = titleRegexp.find(data)?.groupValues?.get(1)

    /**
     * Suspend until a network with internet capability is available, standing in
     * for the old NetworkType.CONNECTED constraint. Returns immediately when one
     * is already up, or when connectivity state cannot be queried at all (the
     * download then simply fails on its own).
     */
    private suspend fun awaitNetwork() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        if (cm.isConnected()) return
        suspendCancellableCoroutine { cont ->
            val resumed = AtomicBoolean(false)
            fun resumeOnce() {
                if (resumed.compareAndSet(false, true)) cont.resume(Unit)
            }
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runCatching { cm.unregisterNetworkCallback(this) }
                    resumeOnce()
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            if (runCatching { cm.registerNetworkCallback(request, callback) }.isFailure) {
                resumeOnce()
                return@suspendCancellableCoroutine
            }
            cont.invokeOnCancellation { runCatching { cm.unregisterNetworkCallback(callback) } }
            // Connectivity may have come up between the check and the registration.
            if (cm.isConnected()) {
                runCatching { cm.unregisterNetworkCallback(callback) }
                resumeOnce()
            }
        }
    }

    private fun ConnectivityManager.isConnected(): Boolean = runCatching {
        val caps = getNetworkCapabilities(activeNetwork ?: return false) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)
}
