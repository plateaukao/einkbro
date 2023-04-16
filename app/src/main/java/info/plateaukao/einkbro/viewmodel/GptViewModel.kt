package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import info.plateaukao.einkbro.BuildConfig
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@OptIn(BetaOpenAI::class)
class GptViewModel : ViewModel(), KoinComponent {
    private val config: ConfigManager by inject()

    private val openai: OpenAI by lazy { OpenAI(config.gptApiKey) }

    private val _responseMessage = MutableStateFlow("")
    val responseMessage: StateFlow<String> = _responseMessage.asStateFlow()

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    fun hasApiKey(): Boolean = config.gptApiKey.isNotBlank()

    fun updateInputMessage(userMessage: String) {
        _inputMessage.value = userMessage
        _responseMessage.value = "..."
    }

    fun query(userMessage: String? = null) {
        if (userMessage != null) {
            _inputMessage.value = userMessage
        }

        val messages = mutableListOf<ChatMessage>()
        if (config.gptSystemPrompt.isNotBlank()) {
            messages.add(config.gptSystemPrompt.toSystemMessage())
        }
        messages.add("${config.gptUserPromptPrefix}${_inputMessage.value}".toUserMessage())

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages = messages
        )

        viewModelScope.launch(Dispatchers.IO) {
            if (!BuildConfig.DEBUG) {
                val response = openai.chatCompletion(chatCompletionRequest)
                _responseMessage.value = response.choices.first().message?.content ?: ""
            } else {
                _responseMessage.value = "12345"
            }
        }
    }
}

@OptIn(BetaOpenAI::class)
fun String.toUserMessage() = ChatMessage(
    role = ChatRole.User,
    content = this
)

@OptIn(BetaOpenAI::class)
fun String.toSystemMessage() = ChatMessage(
    role = ChatRole.System,
    content = this
)