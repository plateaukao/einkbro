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

/**
 * Modified by Edsuns@qq.com.
 *
 * Description: In `duckduckgo/Android`, `tag/5.38.1` is the last version that has the implement of AdBlockClient.
 *
 * Reference: [github.com/duckduckgo/Android/releases/tag/5.38.1](https://github.com/duckduckgo/Android/releases/tag/5.38.1)
 */
interface Client {

    val id: String

    var isGenericElementHidingEnabled: Boolean

    fun matches(url: String, documentUrl: String, resourceType: ResourceType): MatchResult

    fun getElementHidingSelectors(url: String): String?

    fun getExtendedCssSelectors(url: String): Array<String>?

    fun getCssRules(url: String): Array<String>?

    fun getScriptlets(url: String): Array<String>?

}