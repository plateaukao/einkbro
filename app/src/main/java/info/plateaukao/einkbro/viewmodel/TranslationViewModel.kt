package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.TranslateRepository
import info.plateaukao.einkbro.util.TranslationLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
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

    fun updateTranslationLanguage(translateApi: TRANSLATE_API, language: TranslationLanguage) {
        _translationLanguage.value = language
        _responseMessage.value = "..."
        when (translateApi) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate()
            TRANSLATE_API.PAPAGO -> callPapagoTranslate()
        }
    }

    fun updateSourceLanguage(translateApi: TRANSLATE_API, language: TranslationLanguage) {
        _sourceLanguage.value = language
        _responseMessage.value = "..."
        when (translateApi) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate()
            TRANSLATE_API.PAPAGO -> callPapagoTranslate()
        }
    }

    fun translate(translateApi: TRANSLATE_API, userMessage: String? = null) {
        when (translateApi) {
            TRANSLATE_API.GOOGLE -> callGoogleTranslate(userMessage)
            TRANSLATE_API.PAPAGO -> callPapagoTranslate(userMessage)
        }
    }

    private fun callGoogleTranslate(userMessage: String? = null) {
        if (userMessage != null) {
            _inputMessage.value = userMessage
        }

        viewModelScope.launch(Dispatchers.IO) {
            _responseMessage.value =
                translateRepository.gTranslate(
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
                    sourceLanguage = config.sourceLanguage.value
                )
                    ?: "Something went wrong."
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    val scope = CoroutineScope(newFixedThreadPoolContext(5, "requests"))

    suspend fun translateImage(
        url: String,
        sourceLanguage: TranslationLanguage,
        targetLanguage: TranslationLanguage,
    ): String? {
        val result = translateRepository.translateImage(
            url,
            sourceLanguage.value,
            targetLanguage.value,
            true,
        )
        return result?.renderedImage
    }

    suspend fun translateByParagraph(
        html: String,
        translateApi: TRANSLATE_API,
        updateProgress: (Int) -> Unit
    ): String {
        val parsedHtml = Jsoup.parse(html)
        val nodesWithText = fetchNodesWithText(parsedHtml)
        var completeCount = 0
        withContext(scope.coroutineContext) {
            nodesWithText.forEach { node ->
                async {
                    val translation = if (translateApi == TRANSLATE_API.PAPAGO) {
                        translateRepository.ppTranslate(
                            node.text(),
                            config.translationLanguage.value,
                            config.sourceLanguage.value
                        )
                    } else {
                        translateRepository.gTranslate(
                            node.text(),
                            config.translationLanguage.value
                        )
                    }
                    node.append("<br><br>$translation")
                    completeCount++
                    withContext(Dispatchers.Main) {
                        updateProgress(completeCount * 100 / nodesWithText.size)
                    }
                }.await()
            }
        }

        return parsedHtml.toString()
    }

    private fun fetchNodesWithText(
        element: Element,
    ): List<Element> {
        val result = mutableListOf<Element>()
        for (child in element.children()) {
            if ((child.children().size == 0 && child.text().isNotBlank()) ||
                child.tagName() == "p"
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
    GOOGLE, PAPAGO
}