package io.github.edsuns.adfilter.impl

import io.github.edsuns.adfilter.impl.Constants.FILTER_DATA_FORMAT_VERSION
import timber.log.Timber
import java.io.File

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
internal class BinaryDataStore(private val dir: File) {

    /**
     * True if the on-disk filter cache was purged during this init because its
     * recorded format version did not match [FILTER_DATA_FORMAT_VERSION]. Callers
     * can inspect this flag to trigger a re-download of previously-installed filters.
     */
    val wasPurged: Boolean

    init {
        if (!dir.exists() && !dir.mkdirs()) {
            Timber.v("BinaryDataStore: failed to create store dirs")
        }
        wasPurged = checkAndPurgeIfOutdated()
    }

    private fun checkAndPurgeIfOutdated(): Boolean {
        val versionFile = File(dir, VERSION_MARKER)
        val storedVersion = runCatching { versionFile.readText().trim().toIntOrNull() ?: 0 }
            .getOrDefault(0)

        if (storedVersion == FILTER_DATA_FORMAT_VERSION) {
            return false
        }

        Timber.i("BinaryDataStore: filter cache format changed ($storedVersion -> $FILTER_DATA_FORMAT_VERSION), purging")
        dir.listFiles()?.forEach {
            if (it.isFile) {
                it.delete()
            }
        }
        runCatching { versionFile.writeText(FILTER_DATA_FORMAT_VERSION.toString()) }
            .onFailure { Timber.w(it, "BinaryDataStore: failed to write version marker") }
        return true
    }

    fun hasData(name: String): Boolean =
        name != VERSION_MARKER && File(dir, name).exists()

    fun loadData(name: String): ByteArray =
        File(dir, name).readBytes()

    fun saveData(name: String, byteArray: ByteArray) {
        File(dir, name).writeBytes(byteArray)
    }

    fun clearData(name: String) {
        File(dir, name).delete()
    }

    private companion object {
        const val VERSION_MARKER = ".format_version"
    }
}
