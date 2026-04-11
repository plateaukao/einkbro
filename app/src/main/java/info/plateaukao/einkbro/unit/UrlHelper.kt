package info.plateaukao.einkbro.unit

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.util.Constants
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Base64
import java.util.Locale
import java.util.regex.Pattern

object UrlHelper : KoinComponent {

    private const val SEARCH_ENGINE_GOOGLE = "https://www.google.com/search?q="
    private const val SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q="
    private const val SEARCH_ENGINE_STARTPAGE = "https://startpage.com/do/search?query="
    private const val SEARCH_ENGINE_BING = "http://www.bing.com/search?q="
    private const val SEARCH_ENGINE_BAIDU = "https://www.baidu.com/s?wd="
    private const val SEARCH_ENGINE_QWANT = "https://www.qwant.com/?q="
    private const val SEARCH_ENGINE_ECOSIA = "https://www.ecosia.org/search?q="
    private const val SEARCH_ENGINE_YANDEX = "https://yandex.com/search/?text="
    private const val SEARCH_ENGINE_STARTPAGE_DE =
        "https://startpage.com/do/search?lui=deu&language=deutsch&query="
    private const val SEARCH_ENGINE_SEARX = "https://searx.me/?q="

    private const val URL_ABOUT_BLANK = "about:blank"
    private const val URL_SCHEME_ABOUT = "about:"
    private const val URL_SCHEME_MAIL_TO = "mailto:"
    private const val URL_SCHEME_FILE = "file://"
    private const val URL_SCHEME_HTTPS = "https://"
    private const val URL_SCHEME_HTTP = "http://"
    private const val URL_PREFIX_GOOGLE_PLAY = "www.google.com/url?q="
    private const val URL_SUFFIX_GOOGLE_PLAY = "&sa"
    private const val URL_ENCODING = "utf-8"

    private val config: ConfigManager by inject()

    internal val neatUrlConfigs: List<String> = parseNeatUrlConfigs()

    @JvmStatic
    fun isURL(url: String?): Boolean {
        var url = url ?: return false
        url = url.lowercase(Locale.getDefault())
        if (url.startsWith(URL_ABOUT_BLANK)
            || url.startsWith(URL_SCHEME_MAIL_TO)
            || url.startsWith(URL_SCHEME_FILE)
        ) {
            return true
        }
        val regex = ("^((ftp|http|https|intent)?://)" // support scheme
                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" // ftp的user@
                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP形式的URL -> 199.194.52.184
                + "|" // 允许IP和DOMAIN（域名）
                + "(.)*" // 域名 -> www.
                // + "([0-9a-z_!~*'()-]+\\.)*"                               // 域名 -> www.
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // 二级域名
                + "[a-z]{2,6})" // first level domain -> .com or .museum
                + "(:[0-9]{1,4})?" // 端口 -> :80
                + "((/?)|" // a slash isn't required if there is no file name
                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$")
        val pattern = Pattern.compile(regex)
        val isMatch = pattern.matcher(url).matches()
        return if (isMatch) true else try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme
            scheme == "ftp" || scheme == "http" || scheme == "https" || scheme == "intent"
        } catch (exception: Exception) {
            false
        }
    }

    fun queryWrapper(context: Context, query: String): String {
        // Use prefix and suffix to process some special links
        var query = query
        val temp = query.lowercase(Locale.getDefault())
        if (temp.contains(URL_PREFIX_GOOGLE_PLAY) && temp.contains(URL_SUFFIX_GOOGLE_PLAY)) {
            val start = temp.indexOf(URL_PREFIX_GOOGLE_PLAY) + URL_PREFIX_GOOGLE_PLAY.length
            val end = temp.indexOf(URL_SUFFIX_GOOGLE_PLAY)
            query = query.substring(start, end)
        }

        // remove prefix non-url part
        if (config.browser.shouldTrimInputUrl) {
            var foundIndex = query.indexOf(URL_SCHEME_HTTPS)
            if (foundIndex > 0) {
                query = query.substring(foundIndex)
            }
            foundIndex = query.indexOf(URL_SCHEME_HTTP)
            if (foundIndex > 0) {
                query = query.substring(foundIndex)
            }
        }

        if (isURL(query)) {
            if (query.startsWith(URL_SCHEME_ABOUT) || query.startsWith(URL_SCHEME_MAIL_TO)) {
                return query
            }
            if (!query.contains("://")) {
                query = URL_SCHEME_HTTPS + query
            }
            return query.replace(" ", "+")
        }

        try {
            query = URLEncoder.encode(query, URL_ENCODING)
        } catch (u: UnsupportedEncodingException) {
            Log.w("browser", "Unsupported Encoding Exception")
        }
        return when (config.browser.searchEngine.toInt()) {
            0 -> SEARCH_ENGINE_STARTPAGE + query
            1 -> SEARCH_ENGINE_STARTPAGE_DE + query
            2 -> SEARCH_ENGINE_BAIDU + query
            3 -> SEARCH_ENGINE_BING + query
            6 -> SEARCH_ENGINE_SEARX + query
            7 -> SEARCH_ENGINE_QWANT + query
            8 -> SEARCH_ENGINE_ECOSIA + query
            9 -> config.browser.searchEngineUrl + query
            10 -> SEARCH_ENGINE_YANDEX + query
            5 -> SEARCH_ENGINE_GOOGLE + query
            4 -> SEARCH_ENGINE_DUCKDUCKGO + query
            else -> SEARCH_ENGINE_GOOGLE + query
        }
    }

    fun stripUrlQuery(url: String): String {
        if (!config.browser.shouldPruneQueryParameters) return url

        try {
            var strippedCount = 0
            val uri = Uri.parse(url)
            if (uri.authority == null) return url

            val params = uri.queryParameterNames
            if (params.isEmpty()) return url

            val uriBuilder = uri.buildUpon().clearQuery()
            for (param in params) {
                if (!matchNeatUrlConfig(uri.host.orEmpty(), param)) {
                    uriBuilder.appendQueryParameter(param, uri.getQueryParameter(param))
                } else {
                    strippedCount++
                }
            }

            // only return the stripped url if we stripped something
            // to fix the only key but no value scenario
            return if (strippedCount > 0) {
                Log.d("strippedCount", "$strippedCount")
                uriBuilder.build().toString()
            } else {
                url
            }
        } catch (e: Exception) {
            return url
        }
    }

    internal fun matchNeatUrlConfig(host: String, param: String): Boolean {
        neatUrlConfigs.forEach { paramConfig ->
            // handle host part
            if (paramConfig.contains("@")) {
                val paramConfigs = paramConfig.split("@")
                if (paramConfigs.size == 2) {
                    val paramValue = paramConfigs[0]
                    val hostValue = paramConfigs[1]
                    val modifiedHost = host.replace("www.", "")
                    if (matchStarString(hostValue, modifiedHost) &&
                        matchStarString(paramValue, param)
                    ) {
                        return true
                    }
                }
            }

            // handle normal param part
            if (matchStarString(paramConfig, param)) return true
        }
        return false
    }

    internal fun matchStarString(config: String, param: String): Boolean {
        if (config.endsWith("*")) {
            if (param.startsWith(config.substring(0, config.length - 1))) return true
        } else if (config.startsWith("*")) {
            if (param.endsWith(config.substring(1, config.length))) return true
        } else if (config == param) {
            return true
        }
        return false
    }

    internal data class NeatUrlConfig(val name: String, val params: List<String>)

    @Suppress("UNCHECKED_CAST")
    internal fun parseNeatUrlConfigs(): List<String> {
        val configArray = JSONObject(Constants.NEAT_URL_DATA)
            .getJSONArray("categories")

        return (0 until configArray.length()).map { index ->
            val config = configArray.getJSONObject(index)
            val paramsArray = config.getJSONArray("params")
            (0 until paramsArray.length()).map { s -> paramsArray.getString(s) }
        }.flatten()
    }

    fun dataUrlToMimeType(dataUrl: String): String =
        dataUrl.substring(dataUrl.indexOf("/") + 1, dataUrl.indexOf(";"))

    @RequiresApi(Build.VERSION_CODES.O)
    fun dataUrlToStream(dataUrl: String): InputStream {
        val data = dataUrl.split(",")
        val base64 = data[1]
        val imageBytes = Base64.getDecoder().decode(base64)
        return ByteArrayInputStream(imageBytes)
    }
}
