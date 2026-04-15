package io.github.edsuns.adblockrust

import io.github.edsuns.adblockclient.Client
import io.github.edsuns.adblockclient.MatchResult
import io.github.edsuns.adblockclient.ResourceType

class AdBlockRustClient(override val id: String) : Client {

    private var enginePtr: Long = nativeCreateEngine()

    fun loadBasicData(data: ByteArray, preserveRules: Boolean = false) {
        TODO("implemented in step 2")
    }

    fun loadProcessedData(data: ByteArray) {
        TODO("implemented in step 2")
    }

    fun getProcessedData(): ByteArray {
        TODO("implemented in step 2")
    }

    override var isGenericElementHidingEnabled: Boolean
        get() = TODO("implemented in step 2")
        set(_) { TODO("implemented in step 2") }

    override fun matches(
        url: String,
        documentUrl: String,
        resourceType: ResourceType
    ): MatchResult {
        TODO("implemented in step 3")
    }

    override fun getElementHidingSelectors(url: String): String? =
        TODO("implemented in step 4")

    override fun getExtendedCssSelectors(url: String): Array<String>? =
        TODO("implemented in step 4")

    override fun getCssRules(url: String): Array<String>? =
        TODO("implemented in step 4")

    override fun getScriptlets(url: String): Array<String>? =
        TODO("implemented in step 4")

    @Suppress("unused", "protectedInFinal")
    protected fun finalize() {
        if (enginePtr != 0L) {
            nativeReleaseEngine(enginePtr)
            enginePtr = 0
        }
    }

    private external fun nativeCreateEngine(): Long
    private external fun nativeReleaseEngine(ptr: Long)

    companion object {
        init {
            System.loadLibrary("adblock_rust_client")
        }
    }
}
