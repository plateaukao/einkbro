package info.plateaukao.einkbro.caption

import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.unit.BrowserUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DualCaptionProcessor:KoinComponent {
    private val config: ConfigManager by inject()
    private val serializer = TimedText.serializer()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun processUrl(url: String): String? {
        if (config.dualCaptionLocale.isEmpty()) return null
        if (!url.contains(urlWithCaption)) return null


        try {
            val newUrl = "$url&tlang=${config.dualCaptionLocale}"
            val oldCaption = runBlocking { BrowserUnit.getResourceFromUrl(url) }
            val newCaption = runBlocking { BrowserUnit.getResourceFromUrl(newUrl) }
            val oldCaptionJson = json.decodeFromString(serializer, String(oldCaption))
            val newCaptionJson = json.decodeFromString(serializer, String(newCaption))

            oldCaptionJson.wsWinStyles.forEach {
                if (it.mhModeHint != null) {
                    it.mhModeHint = 0
                }
                if (it.sdScrollDir != null) {
                    it.sdScrollDir = 0
                }
            }

            oldCaptionJson.events.forEach { event ->
                if (event.segs != null && event.segs.size > 0) {
                    val first = event.segs.first()
                    first.utf8 = event.segs.map { it.utf8 }.reduce { acc, s -> acc + s }

                    val newCaptionSeg = newCaptionJson.events.firstOrNull { it.tStartMs == event.tStartMs }?.segs
                    if (!newCaptionSeg.isNullOrEmpty()) {
                        first.utf8 += ("\n" +
                                newCaptionSeg.map { it.utf8 }.reduce { acc, str -> acc + str })
                    }
                    event.segs.clear()
                    event.segs.add(first)
                }
            }

            return json.encodeToString(serializer, oldCaptionJson)
        } catch (exception: Exception) {
            return null
        }
    }

    fun convertToHtml(jsonString: String): String {
        val timedText = json.decodeFromString(serializer, jsonString)
        val sb = StringBuilder()
        sb.append("<html><head><style>body{font-size: 1.5em;}</style></head><body>")
        timedText.events.forEach { event ->
            event.segs?.forEach { segment ->
                sb.append("${segment.utf8.replace("\n\n", "<br>").replace("\n", "<br>")}<br><br>")
            }
        }
        sb.append("</body></html>")
        return sb.toString().replace("<br><br><br>", "<br><br>").replace("<br><br><br>", "<br><br>")
    }

    companion object {
        const val urlWithCaption = "timedtext"
    }
}