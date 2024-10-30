package io.github.edsuns.adfilter

import android.webkit.WebResourceResponse

/**
 * Created by Edsuns@qq.com on 2021/1/24.
 */
data class FilterResult(
    val rule: String?,
    val resourceUrl: String,
    val resourceResponse: WebResourceResponse?,
    val shouldBlock: Boolean = rule != null
)
