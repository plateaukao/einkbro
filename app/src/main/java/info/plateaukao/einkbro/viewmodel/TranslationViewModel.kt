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
    var gptActionInfo = config.gptActionForExternalSearch ?: config.gptActionList.firstOrNull()
    ?: ChatGPTActionInfo()

    private val _responseMessage = MutableStateFlow("")
    val responseMessage: StateFlow<String> = _responseMessage.asStateFlow()

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    private val _messageWithContext = MutableStateFlow("")
    val messageWithContext: StateFlow<String> = _messageWithContext.asStateFlow()

    private val _translationLanguage = MutableStateFlow(config.translationLanguage)
    val translationLanguage: StateFlow<TranslationLanguage> = _translationLanguage.asStateFlow()

    private val _sourceLanguage = MutableStateFlow(config.sourceLanguage)
    val sourceLanguage: StateFlow<TranslationLanguage> = _sourceLanguage.asStateFlow()

    private val _rotateResultScreen = MutableStateFlow(false)
    val rotateResultScreen: StateFlow<Boolean> = _rotateResultScreen.asStateFlow()

    private val _translateMethod = MutableStateFlow(config.externalSearchMethod)
    val translateMethod: StateFlow<TRANSLATE_API> = _translateMethod.asStateFlow()

    fun updateRotateResultScreen(rotate: Boolean) {
        _rotateResultScreen.value = rotate
    }

    fun updateTranslateMethod(translateApi: TRANSLATE_API) {
        _translateMethod.value = translateApi
        config.externalSearchMethod = translateApi
    }

    fun hasOpenAiApiKey(): Boolean = config.gptApiKey.isNotBlank()

    fun updateMessageWithContext(userMessage: String) {
        _messageWithContext.value = StringEscapeUtils.unescapeJava(userMessage)
    }

    fun updateInputMessage(userMessage: String) {
        _inputMessage.value = StringEscapeUtils.unescapeJava(userMessage)
        _responseMessage.value = "..."
    }

    fun updateTranslationLanguage(language: TranslationLanguage) {
        _translationLanguage.value = language
    }

    fun updateTranslationLanguageAndGo(language: TranslationLanguage) {
        updateTranslationLanguage(language)
        _responseMessage.value = "..."
        when (_translateMethod.value) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate()
            TRANSLATE_API.PAPAGO -> callPapagoTranslate()
            TRANSLATE_API.NAVER -> callNaverDict()
            TRANSLATE_API.DEEPL -> callDeepLTranslate()
            else -> Unit
        }
    }

    fun updateSourceLanguage(language: TranslationLanguage) {
        _sourceLanguage.value = language
    }

    fun isWebViewStyle(): Boolean {
        return _translateMethod.value == TRANSLATE_API.NAVER
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

    fun translate(
        translateApi: TRANSLATE_API = _translateMethod.value,
        userMessage: String? = null
    ) {
        _translateMethod.value = translateApi
        config.externalSearchMethod = translateApi
        _responseMessage.value = "..."

        if (userMessage != null) {
            _inputMessage.value = userMessage
        }

        when (translateApi) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate()
            TRANSLATE_API.PAPAGO -> callPapagoTranslate()
            TRANSLATE_API.NAVER -> callNaverDict()
            TRANSLATE_API.GPT -> queryGpt()
            TRANSLATE_API.DEEPL -> callDeepLTranslate()
        }
    }

    fun getGptActionList(): List<ChatGPTActionInfo> {
        return config.gptActionList
    }

    private fun callNaverDict() {
        val message = _inputMessage.value
        viewModelScope.launch(Dispatchers.IO) {
            val byteArray =
                BrowserUnit.getResourceFromUrl("https://dict.naver.com/dict.search?query=$message}")
            val document = Jsoup.parse(String(byteArray))
            val container = document.getElementById("contents")
            var content = ""
            content += container?.getElementsByClass("section")?.html() ?: ""
            //_responseMessage.value = content
            //_responseMessage.value = String(byteArray)
            _responseMessage.value =
                "https://ja.dict.naver.com/#/search?query=$message}"
        }
    }

    private fun callGoogleTranslate() {
        val message = _inputMessage.value
        viewModelScope.launch(Dispatchers.IO) {
            _responseMessage.value =
                translateRepository.gTranslateWithApi(
                    message,
                    targetLanguage = config.translationLanguage.value,
                )
                    ?: "Something went wrong."
        }
    }

    private fun callDeepLTranslate() {
        val message = _inputMessage.value
        viewModelScope.launch(Dispatchers.IO) {
            val targetLanguage = if (config.translationLanguage == TranslationLanguage.ZH_TW ||
                config.translationLanguage == TranslationLanguage.ZH_CN
            ) {
                "zh"
            } else {
                config.translationLanguage.value
            }
            _responseMessage.value =
                translateRepository.deepLTranslate(
                    message,
                    targetLanguage = targetLanguage,
                )
                    ?: "Something went wrong."
        }
    }

    private fun callPapagoTranslate() {
        val message = _inputMessage.value
        viewModelScope.launch(Dispatchers.IO) {
            _responseMessage.value =
                translateRepository.ppTranslate(
                    message,
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

    private fun queryGpt() {
        _translateMethod.value = TRANSLATE_API.GPT
        config.gptActionForExternalSearch = gptActionInfo

        val messages = mutableListOf<ChatMessage>()
        if (gptActionInfo.systemMessage.isNotBlank()) {
            messages.add(gptActionInfo.systemMessage.toSystemMessage())
        }

        val promptPrefix = gptActionInfo.userMessage
        val selectedText = if (promptPrefix.contains("<<") &&
            promptPrefix.contains(">>") &&
            _messageWithContext.value.contains("<<") &&
            _messageWithContext.value.contains(">>")
        ) {
            _messageWithContext.value
        } else {
            _inputMessage.value
        }
        messages.add("$promptPrefix$selectedText".toUserMessage())

        // stream case
        if (config.enableOpenAiStream) {
            openAiRepository.chatStream(
                messages,
                appendResponseAction = {
                    if (_responseMessage.value == "...") _responseMessage.value = it
                    else _responseMessage.value += it
                },
                doneAction = { },
                failureAction = {
                    _responseMessage.value = "Something went wrong."
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
    GOOGLE, PAPAGO, NAVER, GPT, DEEPL
}