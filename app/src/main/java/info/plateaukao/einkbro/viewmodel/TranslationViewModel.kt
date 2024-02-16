package info.plateaukao.einkbro.viewmodel

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.ChatMessage
import info.plateaukao.einkbro.service.ChatRole
import info.plateaukao.einkbro.service.OpenAiRepository
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
import org.jsoup.nodes.TextNode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class TranslationViewModel : ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()
    private val translateRepository = TranslateRepository()

    private val openAiRepository by lazy { OpenAiRepository(config.gptApiKey) }
    var gptActionInfo = ChatGPTActionInfo("ChatGPT", "", "")

    private val _responseMessage = MutableStateFlow("")
    val responseMessage: StateFlow<String> = _responseMessage.asStateFlow()

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    private val _translationLanguage = MutableStateFlow(config.translationLanguage)
    val translationLanguage: StateFlow<TranslationLanguage> = _translationLanguage.asStateFlow()

    private val _sourceLanguage = MutableStateFlow(config.sourceLanguage)
    val sourceLanguage: StateFlow<TranslationLanguage> = _sourceLanguage.asStateFlow()

    private val _showControls = MutableStateFlow(false)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    fun hasOpenAiApiKey(): Boolean = config.gptApiKey.isNotBlank()

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
            else -> Unit
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
            TRANSLATE_API.GPT -> queryGpt(userMessage)
        }
    }

    private fun callNaverDict(userMessage: String? = null) {
        if (userMessage != null) {
            _inputMessage.value = userMessage
        }

        viewModelScope.launch(Dispatchers.IO) {
            val byteArray =
                BrowserUnit.getResourceFromUrl("https://dict.naver.com/dict.search?query=${_inputMessage.value}")
            val document = Jsoup.parse(String(byteArray))
            val container = document.getElementById("contents")
            var content = ""
            content += container?.getElementsByClass("section")?.html() ?: ""
            //_responseMessage.value = content
            //_responseMessage.value = String(byteArray)
            _responseMessage.value =
                "https://ja.dict.naver.com/#/search?query=${_inputMessage.value}"
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

    fun queryGpt(
        userMessage: String? = null,
    ) {
        _responseMessage.value = "..."
        if (userMessage != null) {
            _inputMessage.value = userMessage
        }
        _showControls.value = false

        val messages = mutableListOf<ChatMessage>()
        if (gptActionInfo.systemMessage.isNotBlank()) {
            messages.add(gptActionInfo.systemMessage.toSystemMessage())
        }
        messages.add("${gptActionInfo.userMessage}${_inputMessage.value}".toUserMessage())


        // stream case
        if (config.enableOpenAiStream) {
            openAiRepository.chatStream(
                messages,
                appendResponseAction = {
                    if (_responseMessage.value == "...") _responseMessage.value = it
                    else _responseMessage.value += it
                },
                doneAction = { _showControls.value = true },
                failureAction = {
                    _responseMessage.value = "Something went wrong."
                    _showControls.value = true
                }
            )
            return
        }

        // normal case: too slow!!!
        viewModelScope.launch(Dispatchers.IO) {
            val chatCompletion = openAiRepository.chatCompletion(messages)
            if (chatCompletion == null || chatCompletion.choices.isEmpty()) {
                _responseMessage.value = "Something went wrong."
                return@launch
            } else {
                val responseContent = chatCompletion.choices
                    .firstOrNull { it.message.role == ChatRole.Assistant }?.message?.content
                    ?: "Something went wrong."
                _responseMessage.value = responseContent
            }
        }
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
        for (node in element.textNodes()) {
            if (node.text().isNotBlank() && !node.hasUnwantedParent()) {
                val textElement = Element("p").apply { text(node.text()) }
                node.replaceWith(textElement)
                result += textElement
            }
        }
        for (node in element.children()) {
            // by pass non-necessary element
            if (node.attr("data-tiara-action-name") == "헤드글씨크기_클릭" ||
                node.text() == "original link"
            ) {
                node.text("")
                break
            }
            if ((node.children().size == 0 && node.text().isNotBlank()) ||
                node.tagName().lowercase(Locale.ROOT) in listOf(
                    "strong",
                    "span",
                    "p",
                    "h1",
                    "h2",
                    "h3",
                    "h4",
                    "h5",
                    "h6",
                    "em"
                )
            ) {
                if (node.text().isNotEmpty() && !node.hasUnwantedParent()) {
                    result += node
                }
            } else {
                result += fetchNodesWithText(node)
            }
        }
        return result
    }

    private fun Element.hasUnwantedParent(): Boolean {
        if (this.tagName().lowercase() in listOf("img", "button", "head", "code")) {
            return true
        }

        var parent = this.parent()
        while (parent != null) {
            if (parent.tagName().lowercase() in listOf("img", "button", "head", "code")) {
                return true
            }
            parent = parent.parent()
        }
        return false
    }

    private fun TextNode.hasUnwantedParent(): Boolean {
        val parentElement: Element? = this.parent() as? Element
        return parentElement?.hasUnwantedParent() ?: false
    }

    fun String.toUserMessage() = ChatMessage(
        role = ChatRole.User,
        content = this
    )

    fun String.toSystemMessage() = ChatMessage(
        role = ChatRole.System,
        content = this
    )
}

enum class TRANSLATE_API {
    GOOGLE, PAPAGO, NAVER, GPT
}