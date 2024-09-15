package info.plateaukao.einkbro.viewmodel

import android.view.View
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.ChatGptQuery
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.service.ChatMessage
import info.plateaukao.einkbro.service.ChatRole
import info.plateaukao.einkbro.service.OpenAiRepository
import info.plateaukao.einkbro.service.TranslateRepository
import info.plateaukao.einkbro.unit.BrowserUnit
import info.plateaukao.einkbro.unit.HelperUnit
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
    private val bookmarkManager: BookmarkManager by inject()
    private val translateRepository = TranslateRepository()

    private lateinit var openAiRepository: OpenAiRepository
    var gptActionInfo = config.gptActionForExternalSearch ?: config.gptActionList.firstOrNull()
    ?: ChatGPTActionInfo()

    private val _responseMessage = MutableStateFlow(AnnotatedString(""))
    val responseMessage: StateFlow<AnnotatedString> = _responseMessage.asStateFlow()

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

    private val _showEditDialogWithIndex = MutableStateFlow(-1)
    val showEditDialogWithIndex: StateFlow<Int> = _showEditDialogWithIndex.asStateFlow()

    var url: String = ""

    private var toBeSavedResponseString = ""

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
        _responseMessage.value = AnnotatedString("...")
    }

    fun updateTranslationLanguage(language: TranslationLanguage) {
        _translationLanguage.value = language
    }

    fun updateTranslationLanguageAndGo(language: TranslationLanguage) {
        updateTranslationLanguage(language)
        _responseMessage.value = AnnotatedString("...")
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
        _responseMessage.value = AnnotatedString("...")
        when (translateApi) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate()
            TRANSLATE_API.PAPAGO -> callPapagoTranslate()
            else -> {}
        }
    }

    fun translate(
        translateApi: TRANSLATE_API = _translateMethod.value,
        userMessage: String? = null,
    ) {
        _translateMethod.value = translateApi
        config.externalSearchMethod = translateApi
        _responseMessage.value = AnnotatedString("...")

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

    fun cancel() {
        if (this::openAiRepository.isInitialized) {
            openAiRepository.cancel()
        }
    }

    fun setupGptAction(gptAction: ChatGPTActionInfo) {
        updateTranslateMethod(TRANSLATE_API.GPT)
        gptActionInfo = gptAction
    }

    fun setupTextSummary(text: String): Boolean {
        if (!hasOpenAiApiKey()) return false

        updateInputMessage(text)
        setupGptAction(ChatGPTActionInfo(systemMessage = config.gptUserPromptForWebPage))

        return true
    }

    private fun callNaverDict() {
        val message = _inputMessage.value
        viewModelScope.launch(Dispatchers.IO) {
            val byteArray =
                BrowserUnit.getResourceFromUrl("https://dict.naver.com/dict.search?query=$message}")
            val document = Jsoup.parse(String(byteArray))
            val container = document.getElementById("contents")
            var content = ""
            content += container?.getElementsByClass("section")?.html().orEmpty()
            //_responseMessage.value = content
            //_responseMessage.value = String(byteArray)
            _responseMessage.value =
                AnnotatedString("https://ja.dict.naver.com/#/search?query=$message}")
        }
    }

    private fun callGoogleTranslate() {
        val message = _inputMessage.value
        viewModelScope.launch(Dispatchers.IO) {
            _responseMessage.value =
                AnnotatedString(
                    translateRepository.gTranslateWithApi(
                        message,
                        targetLanguage = config.translationLanguage.value,
                    )
                        ?: "Something went wrong."
                )
        }
    }

    private fun callDeepLTranslate() {
        val message = _inputMessage.value
        viewModelScope.launch(Dispatchers.IO) {
            val targetLanguage = when (config.translationLanguage) {
                TranslationLanguage.ZH_TW,
                TranslationLanguage.ZH_CN,
                -> "zh"

                else -> config.translationLanguage.value
            }
            _responseMessage.value =
                AnnotatedString(
                    translateRepository.deepLTranslate(
                        message,
                        targetLanguage = targetLanguage,
                    )
                        ?: "Something went wrong."
                )
        }
    }

    private fun callPapagoTranslate() {
        val message = _inputMessage.value
        viewModelScope.launch(Dispatchers.IO) {
            _responseMessage.value =
                AnnotatedString(
                    translateRepository.ppTranslate(
                        message,
                        targetLanguage = config.translationLanguage.value,
                    )
                        ?: "Something went wrong."
                )
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

    fun showEditGptActionDialog(gptActionInfoIndex: Int) {
        _showEditDialogWithIndex.value = gptActionInfoIndex
    }

    fun resetEditDialogIndex() {
        _showEditDialogWithIndex.value = -1
    }

    suspend fun saveTranslationResult() {
        if (_translateMethod.value != TRANSLATE_API.GPT) {
            bookmarkManager.addChatGptQuery(
                ChatGptQuery(
                    date = System.currentTimeMillis(),
                    url = url,
                    model = _translateMethod.value.name,
                    selectedText = _inputMessage.value,
                    result = _responseMessage.value.text,
                )
            )
        } else {
            val (_, selectedText) = getSelectedTextAndPromptPrefix()
            val model = gptActionInfo.model.ifEmpty {
                when (gptActionInfo.actionType) {
                    GptActionType.OpenAi -> config.gptModel
                    GptActionType.Gemini -> config.geminiModel
                    GptActionType.SelfHosted -> config.alternativeModel
                    GptActionType.Default -> config.getDefaultActionModel()
                }
            }
            bookmarkManager.addChatGptQuery(
                ChatGptQuery(
                    date = System.currentTimeMillis(),
                    url = url,
                    model = "${gptActionInfo.name} $model",
                    selectedText = selectedText,
                    result = toBeSavedResponseString,
                )
            )
        }
        toBeSavedResponseString = ""
    }


    private fun queryGpt() {
        if (!this::openAiRepository.isInitialized) {
            openAiRepository = OpenAiRepository()
        }

        _translateMethod.value = TRANSLATE_API.GPT
        config.gptActionForExternalSearch = gptActionInfo

        val messages = mutableListOf<ChatMessage>()
        if (gptActionInfo.systemMessage.isNotBlank()) {
            messages.add(gptActionInfo.systemMessage.toSystemMessage())
        }

        val (promptPrefix, selectedText) = getSelectedTextAndPromptPrefix()
        messages.add("$promptPrefix$selectedText".toUserMessage())

        viewModelScope.launch(Dispatchers.IO) {
            when (gptActionInfo.actionType) {
                GptActionType.OpenAi -> queryOpenAi(messages)
                GptActionType.Gemini -> queryGemini(messages)
                GptActionType.SelfHosted,
                GptActionType.Default,
                -> { // Default
                    if (config.useGeminiApi && config.geminiApiKey.isNotBlank()) {
                        queryGemini(messages)
                    } else {
                        queryOpenAi(messages)
                    }
                }
            }
        }
    }

    private suspend fun queryOpenAi(messages: MutableList<ChatMessage>) {
        if (config.enableOpenAiStream) {
            queryWithStream(messages, GptActionType.OpenAi)
            return
        }

        val chatCompletion = openAiRepository.chatCompletion(messages)
        if (chatCompletion == null || chatCompletion.choices.isEmpty()) {
            _responseMessage.value = AnnotatedString("Something went wrong.")
            return
        } else {
            val responseContent = chatCompletion.choices
                .firstOrNull { it.message.role == ChatRole.Assistant }?.message?.content
                ?: "Something went wrong."
            toBeSavedResponseString = responseContent
            _responseMessage.value = AnnotatedString(responseContent)
        }
    }

    private suspend fun queryGemini(messages: MutableList<ChatMessage>) {
        if (config.enableOpenAiStream) {
            queryWithStream(messages, GptActionType.Gemini)
            return
        }

        val result = openAiRepository.queryGemini(
            messages,
            apiKey = config.geminiApiKey
        )
        toBeSavedResponseString = result
        _responseMessage.value = AnnotatedString(result)
    }

    private fun queryWithStream(messages: MutableList<ChatMessage>, gptActionType: GptActionType) {
        var responseString = ""
        openAiRepository.chatStream(
            messages,
            gptActionType,
            appendResponseAction = {
                if (_responseMessage.value.text == "...") {
                    responseString = it
                } else {
                    responseString += it
                }
                toBeSavedResponseString = responseString.unescape()
                _responseMessage.value = HelperUnit.parseMarkdown(toBeSavedResponseString)
            },
            doneAction = { },
            failureAction = {
                _responseMessage.value = AnnotatedString("## Something went wrong.")
            }
        )
    }

    private fun getSelectedTextAndPromptPrefix(): Pair<String, String> {
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
        return Pair(promptPrefix, selectedText)
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

private fun String.unescape(): String {
    return this.replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\'", "'")
        .replace("\\\\", "\\")
        .replace("\\u003c", "<")
        .replace("\\u003e", ">")
}
