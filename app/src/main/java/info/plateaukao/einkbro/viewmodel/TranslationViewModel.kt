package info.plateaukao.einkbro.viewmodel

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.TranslateRepository
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.util.TranslationLanguage
import info.plateaukao.einkbro.view.NinjaWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Element
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TranslationViewModel : ViewModel(), KoinComponent {
    private val translateRepository = TranslateRepository()
    private val config: ConfigManager by inject()

    private val _responseMessage = MutableStateFlow("")
    val responseMessage: StateFlow<String> = _responseMessage.asStateFlow()

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    private val _translationLanguage = MutableStateFlow(config.translationLanguage)
    val translationLanguage: StateFlow<TranslationLanguage> = _translationLanguage.asStateFlow()

    private val _sourceLanguage = MutableStateFlow(config.sourceLanguage)
    val sourceLanguage: StateFlow<TranslationLanguage> = _sourceLanguage.asStateFlow()

    fun updateInputMessage(userMessage: String) {
        _inputMessage.value = StringEscapeUtils.unescapeJava(userMessage)
        _responseMessage.value = "..."
    }

    fun updateTranslationLanguage(language: TranslationLanguage) {
        _translationLanguage.value = language
    }

    fun updateTranslationLanguageAndGo(translateApi: TRANSLATE_API, language: TranslationLanguage) {
        updateTranslationLanguage(language)
        _responseMessage.value = "..."
        when (translateApi) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate()
            TRANSLATE_API.PAPAGO -> callPapagoTranslate()
            TRANSLATE_API.NAVER -> callNaverDict()
        }
    }

    fun updateSourceLanguage(language: TranslationLanguage) {
        _sourceLanguage.value = language
    }

    fun updateSourceLanguageAndGo(translateApi: TRANSLATE_API, language: TranslationLanguage) {
        updateSourceLanguage(language)
        _responseMessage.value = "..."
        when (translateApi) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate()
            TRANSLATE_API.PAPAGO -> callPapagoTranslate()
            else -> {}
        }
    }

    fun translate(translateApi: TRANSLATE_API, userMessage: String? = null) {
        when (translateApi) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate(userMessage)
            TRANSLATE_API.PAPAGO -> callPapagoTranslate(userMessage)
            TRANSLATE_API.NAVER -> callNaverDict(userMessage)
        }
    }

    private fun callNaverDict(userMessage: String? = null) {
        if (userMessage != null) {
            _inputMessage.value = userMessage
        }

        viewModelScope.launch(Dispatchers.IO) {
            val byteArray =
                BrowserUnit.getResourceFromUrl("https://dict.naver.com/search.dict?target=dic&dicQuery=${_inputMessage.value}")
            val document = Jsoup.parse(String(byteArray))
            val container = document.body().getElementById("content")
            var content = ""
            content += container?.getElementsByClass("cn_dic_section")?.html() ?: ""
            content += container?.getElementsByClass("hanja_dic_section")?.html() ?: ""
            content += container?.getElementsByClass("jp_dic_section")?.html() ?: ""
            content += container?.getElementsByClass("en_dic_section")?.html() ?: ""
            content += container?.getElementsByClass("ko_dic_section")?.html() ?: ""
            _responseMessage.value = content
        }
    }

    private fun callGoogleTranslate(userMessage: String? = null) {
        if (userMessage != null) {
            _inputMessage.value = userMessage
        }

        viewModelScope.launch(Dispatchers.IO) {
            _responseMessage.value =
                translateRepository.gTranslateWithApi(
                    _inputMessage.value,
                    targetLanguage = config.translationLanguage.value,
                )
                    ?: "Something went wrong."
        }
    }

    private fun callPapagoTranslate(userMessage: String? = null) {
        if (userMessage != null) {
            _inputMessage.value = userMessage
        }

        viewModelScope.launch(Dispatchers.IO) {
            _responseMessage.value =
                translateRepository.ppTranslate(
                    _inputMessage.value,
                    targetLanguage = config.translationLanguage.value,
                )
                    ?: "Something went wrong."
        }
    }

    suspend fun translateWebView(
        view: View,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): String? {
        val bitmap = ViewUnit.captureDrawingCache(view)
        val result = translateRepository.translateBitmap(
            bitmap,
            sourceLanguage.value,
            targetLanguage.value,
            true,
        )
        return result?.renderedImage
    }

    suspend fun translateImage(
        referer: String,
        url: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): String? {
        val result = translateRepository.translateImageFromUrl(
            referer,
            url,
            sourceLanguage.value,
            targetLanguage.value,
            true,
        )
        return result?.renderedImage
    }

    fun translateByParagraph(html: String): String {
        val parsedHtml = Jsoup.parse(html)
        val nodesWithText = fetchNodesWithText(parsedHtml)
        nodesWithText.forEachIndexed { index, node ->
            // for monitoring visibility
            node.addClass("to-translate")
            // for locating element's position
            node.id(index.toString())
            // for later inserting translated text
            node.after(Element("p"))
        }
        // add observer
        val script: Element = parsedHtml.createElement("script")
        script.attr("type", "text/javascript")
        script.appendChild(DataNode(NinjaWebView.textNodesMonitorJs))
        parsedHtml.body().appendChild(script)

        return parsedHtml.toString()
    }

    private fun fetchNodesWithText(
        element: Element,
    ): List<Element> {
        val result = mutableListOf<Element>()
        for (child in element.children()) {
            // by pass non-necessary element
            if (child.attr("data-tiara-action-name") == "헤드글씨크기_클릭" ||
                child.text() == "original link"
            ) {
                child.text("")
                break
            }
            if ((child.children().size == 0 && child.text().isNotBlank()) ||
                child.tagName() in listOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "em")
            ) {
                if (child.text().isNotEmpty()) {
                    result += child
                }
            } else {
                result += fetchNodesWithText(child)
            }
        }
        return result
    }
}

enum class TRANSLATE_API {
    GOOGLE, PAPAGO, NAVER
}