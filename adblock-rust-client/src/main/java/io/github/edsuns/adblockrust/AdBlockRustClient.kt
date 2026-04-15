package io.github.edsuns.adblockrust

import io.github.edsuns.adblockclient.Client
import io.github.edsuns.adblockclient.MatchResult
import io.github.edsuns.adblockclient.ResourceType

class AdBlockRustClient(override val id: String) : Client {

    @Volatile
    private var enginePtr: Long = nativeCreateEngine()

    fun loadBasicData(data: ByteArray, preserveRules: Boolean = false) {
        enginePtr = nativeLoadRules(enginePtr, data)
    }

    fun loadProcessedData(data: ByteArray) {
        enginePtr = nativeLoadSerialized(enginePtr, data)
    }

    fun getProcessedData(): ByteArray = nativeSerialize(enginePtr)

    override var isGenericElementHidingEnabled: Boolean
        get() = nativeIsGenericHideEnabled(enginePtr)
        set(value) = nativeSetGenericHideEnabled(enginePtr, value)

    override fun matches(
        url: String,
        documentUrl: String,
        resourceType: ResourceType
    ): MatchResult {
        val result = nativeMatches(enginePtr, url, documentUrl, resourceType.filterOption)
        return result ?: MatchResult(false, null, null)
    }

    override fun getElementHidingSelectors(url: String): String? =
        nativeGetElementHidingSelectors(enginePtr, url)

    override fun getExtendedCssSelectors(url: String): Array<String>? =
        nativeGetExtendedCssSelectors(enginePtr, url)

    override fun getCssRules(url: String): Array<String>? =
        nativeGetCssRules(enginePtr, url)

    override fun getScriptlets(url: String): Array<String>? =
        nativeGetScriptlets(enginePtr, url)

    @Suppress("unused", "protectedInFinal")
    protected fun finalize() {
        val ptr = enginePtr
        enginePtr = 0
        if (ptr != 0L) {
            nativeReleaseEngine(ptr)
        }
    }

    private external fun nativeCreateEngine(): Long
    private external fun nativeReleaseEngine(ptr: Long)
    private external fun nativeLoadRules(ptr: Long, data: ByteArray): Long
    private external fun nativeLoadSerialized(ptr: Long, data: ByteArray): Long
    private external fun nativeSerialize(ptr: Long): ByteArray
    private external fun nativeMatches(
        ptr: Long,
        url: String,
        documentUrl: String,
        filterOption: Int
    ): MatchResult?

    private external fun nativeGetElementHidingSelectors(ptr: Long, url: String): String?
    private external fun nativeGetExtendedCssSelectors(ptr: Long, url: String): Array<String>?
    private external fun nativeGetCssRules(ptr: Long, url: String): Array<String>?
    private external fun nativeGetScriptlets(ptr: Long, url: String): Array<String>?
    private external fun nativeSetGenericHideEnabled(ptr: Long, enabled: Boolean)
    private external fun nativeIsGenericHideEnabled(ptr: Long): Boolean

    companion object {
        init {
            System.loadLibrary("adblock_rust_client")
        }
    }
}
