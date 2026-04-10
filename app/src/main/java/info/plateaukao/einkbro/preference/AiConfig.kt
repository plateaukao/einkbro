package info.plateaukao.einkbro.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import info.plateaukao.einkbro.data.remote.GptVoiceOption
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AiConfig(private val sp: SharedPreferences) {

    var gptApiKey by StringPreference(sp, K_GPT_API_KEY, "")

    var geminiApiKey by StringPreference(sp, K_GEMINI_API_KEY, "")

    var gptSystemPrompt by StringPreference(
        sp,
        K_GPT_SYSTEM_PROMPT,
        "You are a good interpreter."
    )
    var gptUserPromptPrefix by StringPreference(
        sp,
        K_GPT_USER_PROMPT_PREFIX,
        "Translate following content to English:"
    )
    var gptUserPromptForWebPage by StringPreference(
        sp,
        K_GPT_USER_PROMPT_WEB_PAGE,
        "Summarize in 50 words:"
    )
    var imageApiKey by StringPreference(sp, K_IMAGE_API_KEY, "")
    var imageTranslateIntervalSeconds by IntPreference(sp, "K_IMAGE_TRANSLATE_INTERVAL", 4)
    var gptModel by StringPreference(sp, K_GPT_MODEL, "gpt-4.1")
    var alternativeModel by StringPreference(sp, K_ALTERNATIVE_MODEL, gptModel)
    var geminiModel by StringPreference(sp, K_GEMINI_MODEL, "gemini-2.5-flash")
    var gptVoiceOption: GptVoiceOption
        get() = GptVoiceOption.entries[sp.getInt("K_GPT_VOICE_OPTION", 0)]
        set(value) = sp.edit { putInt("K_GPT_VOICE_OPTION", value.ordinal) }
    var gptVoiceModel by StringPreference(sp, K_GPT_VOICE_MODEL, "tts-1")
    var gptVoicePrompt by StringPreference(sp, K_GPT_VOICE_PROMPT, "")

    var gptUrl by StringPreference(sp, K_GPT_SERVER_URL, "https://api.openai.com")
    var useCustomGptUrl by BooleanPreference(sp, K_USE_CUSTOM_GPT_URL, false)
    var useGeminiApi by BooleanPreference(sp, K_USE_GEMINI_API, false)

    var enableOpenAiStream by BooleanPreference(sp, K_ENABLE_OPEN_AI_STREAM, true)

    var externalSearchWithGpt by BooleanPreference(sp, K_EXTERNAL_SEARCH_WITH_GPT, false)

    var externalSearchWithPopUp by BooleanPreference(sp, K_EXTERNAL_SEARCH_WITH_POPUP, false)

    var isExternalSearchInSameTab by BooleanPreference(sp, K_EXTERNAL_SEARCH_IN_SAME_TAB, false)

    var externalSearchMethod: TRANSLATE_API
        get() = TRANSLATE_API.entries[sp.getInt(K_EXTERNAL_SEARCH_METHOD, 0)]
        set(value) {
            sp.edit { putInt(K_EXTERNAL_SEARCH_METHOD, value.ordinal) }
        }

    var gptActionList: List<ChatGPTActionInfo>
        get() {
            val str = sp.getString(K_GPT_ACTION_ITEMS, "").orEmpty()
            return if (str.isBlank()) {
                if (gptSystemPrompt.isNotBlank() || gptUserPromptPrefix.isNotBlank()) {
                    listOf(
                        ChatGPTActionInfo(
                            systemMessage = gptSystemPrompt,
                            userMessage = gptUserPromptPrefix
                        )
                    )
                } else {
                    emptyList()
                }
            } else str.convertToDataClass<List<ChatGPTActionInfo>>()
        }
        set(value) {
            sp.edit {
                putString(
                    K_GPT_ACTION_ITEMS,
                    Json.encodeToString(value)
                )
            }
        }

    var gptForChatWeb: GptActionType
        get() = GptActionType.entries[sp.getInt(K_GPT_FOR_CHAT_WEB, 0)]
        set(value) {
            sp.edit { putInt(K_GPT_FOR_CHAT_WEB, value.ordinal) }
        }
    var gptForSummary: GptActionType
        get() = GptActionType.entries[sp.getInt(K_GPT_FOR_SUMMARY, 0)]
        set(value) {
            sp.edit { putInt(K_GPT_FOR_SUMMARY, value.ordinal) }
        }

    var gptActionForExternalSearch: ChatGPTActionInfo?
        get() {
            val str = sp.getString(K_GPT_ACTION_EXTERNAL, "").orEmpty()
            return if (str.isBlank()) null
            else str.convertToDataClass<ChatGPTActionInfo>()
        }
        set(value) {
            sp.edit {
                putString(
                    K_GPT_ACTION_EXTERNAL,
                    Json.encodeToString(value)
                )
            }
        }

    var remoteQueryActionName by StringPreference(sp, K_REMOTE_QUERY_ACTION_NAME, "Search")

    fun addGptAction(action: ChatGPTActionInfo) {
        gptActionList = gptActionList.toMutableList().apply { add(action) }
    }

    fun deleteGptAction(action: ChatGPTActionInfo) {
        gptActionList = gptActionList.toMutableList().apply { remove(action) }
    }

    fun deleteAllGptActions() {
        gptActionList = emptyList()
    }

    fun getDefaultActionModel(): String = if (useGeminiApi) {
        geminiModel
    } else if (useCustomGptUrl) {
        alternativeModel
    } else {
        gptModel
    }

    fun getDefaultActionType(): GptActionType = if (useGeminiApi) {
        GptActionType.Gemini
    } else if (useCustomGptUrl) {
        GptActionType.SelfHosted
    } else {
        GptActionType.OpenAi
    }

    fun getGptTypeModelMap(): Map<GptActionType, String> = mapOf(
        GptActionType.Default to getDefaultActionModel(),
        GptActionType.OpenAi to gptModel,
        GptActionType.SelfHosted to alternativeModel,
        GptActionType.Gemini to geminiModel
    )

    private inline fun <reified R : Any> String.convertToDataClass() =
        Json {
            ignoreUnknownKeys = true
        }.decodeFromString<R>(this)

    companion object {
        const val K_GPT_API_KEY = "sp_gpt_api_key"
        const val K_GEMINI_API_KEY = "sp_gemini_api_key"
        const val K_GPT_SYSTEM_PROMPT = "sp_gpt_system_prompt"
        const val K_GPT_USER_PROMPT_PREFIX = "sp_gpt_user_prompt"
        const val K_GPT_USER_PROMPT_WEB_PAGE = "sp_gpt_user_prompt_web_page"
        const val K_IMAGE_API_KEY = "sp_image_api_key"
        const val K_GPT_MODEL = "sp_gp_model"
        const val K_GPT_VOICE_MODEL = "sp_gpt_voice_model"
        const val K_GPT_VOICE_PROMPT = "sp_gpt_voice_prompt"
        const val K_ALTERNATIVE_MODEL = "sp_alternative_model"
        const val K_GEMINI_MODEL = "sp_gemini_model"
        const val K_EXTERNAL_SEARCH_WITH_GPT = "sp_external_search_with_gpt"
        const val K_EXTERNAL_SEARCH_WITH_POPUP = "sp_external_search_with_pop"
        const val K_ENABLE_OPEN_AI_STREAM = "sp_enable_open_ai_stream"
        const val K_EXTERNAL_SEARCH_IN_SAME_TAB = "sp_external_search_in_same_tab"
        const val K_GPT_ACTION_ITEMS = "sp_gpt_action_items"
        private const val K_GPT_ACTION_EXTERNAL = "sp_gpt_action_external"
        private const val K_GPT_FOR_CHAT_WEB = "sp_gpt_for_chat_web"
        private const val K_GPT_FOR_SUMMARY = "sp_gpt_for_summary"
        private const val K_GPT_SERVER_URL = "sp_gpt_server_url"
        private const val K_USE_CUSTOM_GPT_URL = "sp_use_custom_gpt_url"
        private const val K_USE_GEMINI_API = "sp_use_gemini_api"
        private const val K_EXTERNAL_SEARCH_METHOD = "sp_external_search_method"
        private const val K_REMOTE_QUERY_ACTION_NAME = "sp_remote_query_action_name"
    }
}
