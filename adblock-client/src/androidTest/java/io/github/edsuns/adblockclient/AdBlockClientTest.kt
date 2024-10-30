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

import org.junit.Assert.*
import org.junit.Test

/**
 * Modified by Edsuns@qq.com.
 *
 * Description: In `duckduckgo/Android`, `tag/5.38.1` is the last version that has the implement of AdBlockClient.
 *
 * Reference: [github.com/duckduckgo/Android/releases/tag/5.38.1](https://github.com/duckduckgo/Android/releases/tag/5.38.1)
 */
class AdBlockClientTest {

    companion object {
        private const val id = "test"
        private const val documentUrl = "http://example.com"
        private const val trackerUrl = "http://imasdk.googleapis.com/js/sdkloader/ima3.js"
        private const val nonTrackerUrl = "http://duckduckgo.com/index.html"
        private val resourceType = ResourceType.UNKNOWN
    }

    @Test
    fun whenBasicDataLoadedThenTrackerIsBlocked() {
        val testee = AdBlockClient(id)
        testee.loadBasicData(data(), true)
        val result = testee.matches(trackerUrl, documentUrl, resourceType)
        assertTrue(result.shouldBlock)
        assertFalse(result.matchedRule.isNullOrBlank())
    }

    @Test
    fun whenBasicDataLoadedThenNonTrackerIsNotBlocked() {
        val testee = AdBlockClient(id)
        testee.loadBasicData(data())
        val result = testee.matches(nonTrackerUrl, documentUrl, resourceType)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun whenBasicDataLoadedThenExceptionIsNotBlocked() {
        val testee = AdBlockClient(id)
        testee.loadBasicData(data())// do not enable preserveRules, test disabling it here
        val exceptionUrl = "https://exception-rule.com/a/b/info"
        val result = testee.matches(exceptionUrl, documentUrl, resourceType)
        assertTrue(result.hasException)
        assertFalse(result.matchedRule.isNullOrBlank())
        assertFalse(result.shouldBlock)
    }

    @Test
    fun whenBasicDataLoadedWithThirdPartyOptionThenFirstPartyIsNotBlocked() {
        val testee = AdBlockClient(id)
        testee.loadBasicData(data())
        val result = testee.matches(trackerUrl, trackerUrl, resourceType)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun whenProcessedDataLoadedThenTrackerIsBlocked() {
        val testee = loadClientFromProcessedData()
        val result = testee.matches(trackerUrl, documentUrl, resourceType)
        assertTrue(result.shouldBlock)
        assertFalse(result.matchedRule.isNullOrBlank())
    }

    @Test
    fun whenProcessedDataLoadedThenNonTrackerIsNotBlocked() {
        val testee = loadClientFromProcessedData()
        val result = testee.matches(nonTrackerUrl, documentUrl, resourceType)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun whenProcessedDataLoadedWithThirdPartyOptionThenFirstPartyIsNotBlocked() {
        val testee = loadClientFromProcessedData()
        val result = testee.matches(trackerUrl, trackerUrl, resourceType)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun whenProcessedDataLoadedThenUrlBlockedByRegexRule() {
        val testee = loadClientFromProcessedData()
        testee.loadBasicData(data(), true)
        val urlBlockedByRegexRule = "https://example.com:4443/ty/c-2705-25-1.html"
        val result = testee.matches(urlBlockedByRegexRule, documentUrl, ResourceType.SUBDOCUMENT)
        assertTrue(result.shouldBlock)
        assertFalse(result.matchedRule.isNullOrBlank())
    }

    @Test
    fun whenGetSelectorsForNonTrackerUrlThenOnlyObtainGenericSelectors() {
        val testee = loadClientFromProcessedData()
        testee.isGenericElementHidingEnabled = true
        val selectors =
            testee.getElementHidingSelectors(nonTrackerUrl) ?: throw NullPointerException()
        assertEquals(2, selectors.split(", ").size)
        assertTrue(selectors.contains("#videoad"))
        assertTrue(selectors.contains("#videoads"))
    }

    @Test
    fun whenGetSelectorsWithExceptionsThenItDoNotContainsExceptions() {
        val testee = loadClientFromProcessedData()
        testee.isGenericElementHidingEnabled = true
        val selectors =
            testee.getElementHidingSelectors(documentUrl) ?: throw NullPointerException()
        assertEquals(2, selectors.split(", ").size)
        assertTrue(selectors.contains("#videoad"))
        assertTrue(selectors.contains(".video_ads"))
        assertFalse(selectors.contains("#videoads"))
    }

    private fun loadClientFromProcessedData(): AdBlockClient {
        val original = AdBlockClient(id)
        original.loadBasicData(data(), true)
        val processedData = original.getProcessedData()
        val testee = AdBlockClient(id)
        testee.loadProcessedData(processedData)
        return testee
    }

    private fun data(): ByteArray =
        javaClass.classLoader!!.getResource("binary/easylist_sample").readBytes()
}
