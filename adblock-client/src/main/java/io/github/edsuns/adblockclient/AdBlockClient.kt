/*
 * Copyright (c) 2017 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.edsuns.adblockclient

import android.net.Uri


/**
 * Modified by Edsuns@qq.com.
 *
 * Description: In `duckduckgo/Android`, `tag/5.38.1` is the last version that has the implement of AdBlockClient.
 *
 * Reference: [github.com/duckduckgo/Android/releases/tag/5.38.1](https://github.com/duckduckgo/Android/releases/tag/5.38.1)
 */
class AdBlockClient(override val id: String) : Client {

    private val nativeClientPointer: Long
    private var rawDataPointer: Long
    private var processedDataPointer: Long

    init {
        nativeClientPointer = createClient()
        rawDataPointer = 0
        processedDataPointer = 0
    }

    private external fun createClient(): Long

    /**
     * @param data requires UTF-8 bytes
     */
    fun loadBasicData(data: ByteArray, preserveRules: Boolean = false) {
        val timestamp = System.currentTimeMillis()
        rawDataPointer = loadBasicData(nativeClientPointer, data, preserveRules)
    }

    override var isGenericElementHidingEnabled: Boolean
        get() = isGenericElementHidingEnabled(nativeClientPointer)
        set(value) = setGenericElementHidingEnabled(nativeClientPointer, value)

    private external fun isGenericElementHidingEnabled(clientPointer: Long): Boolean

    private external fun setGenericElementHidingEnabled(clientPointer: Long, enabled: Boolean)

    private external fun loadBasicData(
        clientPointer: Long,
        data: ByteArray,
        preserveRules: Boolean
    ): Long

    fun loadProcessedData(data: ByteArray) {
        val timestamp = System.currentTimeMillis()
        processedDataPointer = loadProcessedData(nativeClientPointer, data)
    }

    private external fun loadProcessedData(clientPointer: Long, data: ByteArray): Long

    fun getProcessedData(): ByteArray = getProcessedData(nativeClientPointer)

    private external fun getProcessedData(clientPointer: Long): ByteArray

    fun getFiltersCount(): Int = getFiltersCount(nativeClientPointer)

    private external fun getFiltersCount(clientPointer: Long): Int

    override fun matches(
        url: String,
        documentUrl: String,
        resourceType: ResourceType
    ): MatchResult {
        val firstPartyDomain = documentUrl.baseHost() ?: return MatchResult(false, null, null)
        return matches(nativeClientPointer, url, firstPartyDomain, resourceType.filterOption)
    }

    private external fun matches(
        clientPointer: Long,
        url: String,
        firstPartyDomain: String,
        filterOption: Int
    ): MatchResult

    override fun getElementHidingSelectors(url: String): String? =
        getElementHidingSelectors(nativeClientPointer, url)

    override fun getExtendedCssSelectors(url: String): Array<String>? =
        getExtendedCssSelectors(nativeClientPointer, url)

    override fun getCssRules(url: String): Array<String>? =
        getCssRules(nativeClientPointer, url)

    override fun getScriptlets(url: String): Array<String>? =
        getScriptlets(nativeClientPointer, url)

    private external fun getElementHidingSelectors(clientPointer: Long, url: String): String?

    private external fun getExtendedCssSelectors(clientPointer: Long, url: String): Array<String>?

    private external fun getCssRules(clientPointer: Long, url: String): Array<String>?

    private external fun getScriptlets(clientPointer: Long, url: String): Array<String>?

    @Suppress("unused", "protectedInFinal")
    protected fun finalize() {
        releaseClient(nativeClientPointer, rawDataPointer, processedDataPointer)
    }

    private external fun releaseClient(
        clientPointer: Long,
        rawDataPointer: Long,
        processedDataPointer: Long
    )

    private fun String.baseHost(): String? {
        return Uri.parse(this).host?.removePrefix("www.")
    }

    companion object {
        init {
            System.loadLibrary("adblock-client")
        }
    }
}
