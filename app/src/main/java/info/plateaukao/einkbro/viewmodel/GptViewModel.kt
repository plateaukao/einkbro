package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.service.ChatMessage
import info.plateaukao.einkbro.service.ChatRole
import info.plateaukao.einkbro.service.OpenAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class GptViewModel : ViewModel(), KoinComponent {
    var gptActionInfo = ChatGPTActionInfo("ChatGPT", "", "")
    private val config: ConfigManager by inject()

    private val openaiRepository: OpenAiRepository
            by lazy { OpenAiRepository(config.gptApiKey) }

    private val _showControls = MutableStateFlow(false)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    private val _responseMessage = MutableStateFlow("")
    val responseMessage: StateFlow<String> = _responseMessage.asStateFlow()

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    fun hasApiKey(): Boolean = config.gptApiKey.isNotBlank()

    fun updateInputMessage(userMessage: String) {
        _inputMessage.value = userMessage
        _responseMessage.value = "..."
    }

    fun query(
        userMessage: String? = null,
    ) {
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
            openaiRepository.chatStream(
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
            val chatCompletion = openaiRepository.chatCompletion(messages)
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
}

fun String.toUserMessage() = ChatMessage(
    role = ChatRole.User,
    content = this
)

fun String.toSystemMessage() = ChatMessage(
    role = ChatRole.System,
    content = this
)