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
import android.webkit.WebResourceRequest
import java.util.*


/**
 * Modified by Edsuns@qq.com.
 *
 * Description: In `duckduckgo/Android`, `tag/5.38.1` is the last version that has the implement of AdBlockClient.
 *
 * Reference: [github.com/duckduckgo/Android/releases/tag/5.38.1](https://github.com/duckduckgo/Android/releases/tag/5.38.1)
 */
enum class ResourceType(val filterOption: Int) {

    UNKNOWN(0),
    SCRIPT(1),
    IMAGE(2),
    CSS(4),
    XMLHTTPREQUEST(0x10),
    SUBDOCUMENT(0x40),
    FONT(0x80000),
    MEDIA(0x100000);

    companion object {
        /**
         * A coarse approach to guessing the resource type from a request
         * to assist the tracker matcher
         */
        fun from(webResourceRequest: WebResourceRequest): ResourceType {
            var result = HeadersResourceTypeDetector.detect(webResourceRequest.requestHeaders)
            if (result == null) {
                result = from(webResourceRequest.url)
            }
            if (result == null) {
                result = UNKNOWN
            }
            return result
        }

        fun from(url: Uri): ResourceType? = UrlResourceTypeDetector.detect(url)
    }
}

private object HeadersResourceTypeDetector {

    private const val HEADER_REQUESTED_WITH = "X-Requested-With"
    private const val HEADER_REQUESTED_WITH_XMLHTTPREQUEST = "XMLHttpRequest"

    fun detect(headers: Map<String, String>?): ResourceType? {
        if (headers == null) {
            return null
        }

        // Be aware that this header can be removed by JavaScript, so there will still be misses.
        val isXmlHttpRequest =
            HEADER_REQUESTED_WITH_XMLHTTPREQUEST == headers[HEADER_REQUESTED_WITH]
        if (isXmlHttpRequest) {
            return ResourceType.XMLHTTPREQUEST
        }

        val acceptHeader = headers["Accept"]
        if (acceptHeader != null) {
            return detect(acceptHeader)
        }
        return null
    }

    private fun detect(acceptHeader: String): ResourceType? {
        // accept header may contain different MIME types, pick up the first one
        val comma = acceptHeader.indexOf(',')
        val firstMIME = if (comma > -1) acceptHeader.substring(0, comma) else acceptHeader

        if (firstMIME.contains("image/")) {
            return ResourceType.IMAGE
        }
        if (firstMIME.contains("/css")) {
            return ResourceType.CSS
        }
        if (firstMIME.contains("javascript")) {
            return ResourceType.SCRIPT
        }
        if (firstMIME.contains("text/html")) {
            return ResourceType.SUBDOCUMENT
        }
        if (firstMIME.contains("font/")) {
            return ResourceType.FONT
        }
        if (firstMIME.contains("audio/") || firstMIME.contains("video/")
            || firstMIME.contains("application/ogg")
        ) {
            return ResourceType.MEDIA
        }
        return null
    }
}

private object UrlResourceTypeDetector {

    private val EXTENSIONS_JS = arrayOf("js")
    private val EXTENSIONS_CSS = arrayOf("css")
    private val EXTENSIONS_FONT = arrayOf("ttf", "woff", "woff2")
    private val EXTENSIONS_HTML = arrayOf("htm", "html")

    // listed https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types
    private val EXTENSIONS_IMAGE = arrayOf(
        "png", "jpg", "jpe", "jpeg", "bmp", "gif", "apng", "cur", "jfif",
        "ico", "pjpeg", "pjp", "svg", "tif", "tiff", "webp"
    )

    // video files listed here https://en.wikipedia.org/wiki/Video_file_format
    // audio files listed here https://en.wikipedia.org/wiki/Audio_file_format
    private val EXTENSIONS_MEDIA = arrayOf(
        "webm", "mkv", "flv", "vob", "ogv", "drc", "mng", "avi", "mov", "gifv", "qt", "wmv", "yuv",
        "rm", "rmvb", "asf", "amv", "mp4", "m4p", "mp2", "mpe", "mpv", "mpg", "mpeg", "m2v", "m4v",
        "svi", "3gp", "3g2", "mxf", "roq", "nsv", "8svx", "aa", "aac", "aax", "act", "aiff", "alac",
        "amr", "ape", "au", "awb", "cda", "dct", "dss", "dvf", "flac", "gsm", "iklax", "ivs", "m4a",
        "m4b", "mmf", "mogg", "mp3", "mpc", "msv", "nmf", "oga", "ogg", "opus", "ra", "raw", "rf64",
        "sln", "tta", "voc", "vox", "wav", "wma", "wv"
    )

    private val extensionTypeMap: MutableMap<String, ResourceType> = HashMap<String, ResourceType>()

    init {
        mapExtensions(EXTENSIONS_JS, ResourceType.SCRIPT)
        mapExtensions(EXTENSIONS_CSS, ResourceType.CSS)
        mapExtensions(EXTENSIONS_FONT, ResourceType.FONT)
        mapExtensions(EXTENSIONS_HTML, ResourceType.SUBDOCUMENT)
        mapExtensions(EXTENSIONS_IMAGE, ResourceType.IMAGE)
        mapExtensions(EXTENSIONS_MEDIA, ResourceType.MEDIA)
    }

    private fun mapExtensions(extensions: Array<String>, contentType: ResourceType) {
        for (extension in extensions) {
            // all comparisons are in lower case, force that the extensions are in lower case
            extensionTypeMap[extension.toLowerCase(Locale.ROOT)] = contentType
        }
    }

    fun detect(url: Uri): ResourceType? {
        val path = url.path ?: return null
        val lastIndexOfDot = path.lastIndexOf('.')
        if (lastIndexOfDot > -1) {
            val fileExtension = path.substring(lastIndexOfDot + 1)
            return extensionTypeMap[fileExtension.toLowerCase(Locale.ROOT)]
        }
        return null
    }
}