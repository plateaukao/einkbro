package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.GptActionsActivity
import info.plateaukao.einkbro.activity.GptQueryListActivity
import info.plateaukao.einkbro.activity.SettingRoute
import info.plateaukao.einkbro.setting.ActionSettingItem
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.ListSettingWithEnumItem
import info.plateaukao.einkbro.setting.NavigateSettingItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.ValueSettingItem

fun buildChatGptSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        ListSettingWithEnumItem(
            R.string.setting_title_default_ai_engine,
            0,
            0,
            config.ai::defaultGptEngine,
            listOf(
                R.string.openai,
                R.string.self_hosted,
                R.string.google_gemini
            )
        ),
        NavigateSettingItem(
            R.string.openai,
            destination = SettingRoute.GptOpenAi,
        ),
        NavigateSettingItem(
            R.string.openai_compatible_server,
            destination = SettingRoute.GptSelfHosted,
        ),
        NavigateSettingItem(
            R.string.google_gemini,
            destination = SettingRoute.GptGemini,
        ),
        DividerSettingItem(R.string.web_content_processing),
        ListSettingWithEnumItem(
            R.string.summary_gpt_type,
            0,
            R.string.setting_summary_summary_gpt_type,
            config.ai::gptForSummary,
            listOf(
                R.string.system_default,
                R.string.openai,
                R.string.self_hosted,
                R.string.google_gemini
            )
        ),
        ValueSettingItem(
            R.string.setting_title_gpt_prompt_for_web_page,
            0,
            R.string.setting_summary_gpt_prompt_for_web_page,
            config.ai::gptUserPromptForWebPage
        ),
        ListSettingWithEnumItem(
            R.string.web_processing_gpt_type,
            0,
            R.string.setting_summary_web_processing_gpt_type,
            config.ai::gptForChatWeb,
            listOf(
                R.string.system_default,
                R.string.openai,
                R.string.self_hosted,
                R.string.google_gemini
            )
        ),
        BooleanSettingItem(
            R.string.use_it_on_dict_search,
            0,
            R.string.setting_summary_search_in_dict,
            config.ai::externalSearchWithGpt
        ),
        BooleanSettingItem(
            R.string.setting_title_chat_stream,
            0,
            R.string.setting_summary_chat_stream,
            config.ai::enableOpenAiStream
        ),
        DividerSettingItem(),
        ActionSettingItem(
            R.string.setting_title_gpt_action_list,
            0,
            R.string.setting_summary_gpt_action_list,
        ) { GptActionsActivity.start(deps.activity) },
        ActionSettingItem(
            R.string.setting_title_gpt_query_list,
            0,
            R.string.setting_summary_gpt_query_list,
        ) {
            deps.activity.startActivity(GptQueryListActivity.createIntent(deps.activity))
        },
    )
}

fun buildGptOpenAiSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        ValueSettingItem(
            R.string.setting_title_edit_gpt_api_key,
            0,
            R.string.setting_summary_edit_gpt_api_key,
            config.ai::gptApiKey
        ),
        ValueSettingItem(
            R.string.setting_title_gpt_model_name,
            0,
            R.string.setting_summary_gpt_model_name,
            config.ai::gptModel
        ),
        DividerSettingItem(),
        BooleanSettingItem(
            R.string.use_it_on_tts,
            0,
            R.string.setting_summary_use_gpt_for_tts,
            config.tts::useOpenAiTts
        ),
        ValueSettingItem(
            R.string.setting_title_gpt_audio_model_name,
            0,
            R.string.setting_summary_gpt_audio_model_name,
            config.ai::gptVoiceModel
        ),
        ValueSettingItem(
            R.string.setting_title_gpt_prompt_for_tts,
            0,
            R.string.setting_summary_gpt_prompt_for_tts,
            config.ai::gptVoicePrompt
        ),
    )
}

fun buildGptSelfHostedSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        ValueSettingItem(
            R.string.setting_title_custom_gpt_url,
            0,
            R.string.setting_summary_custom_gpt_url,
            config.ai::gptUrl
        ),
        ValueSettingItem(
            R.string.setting_title_other_model_name,
            0,
            R.string.setting_summary_other_model_name,
            config.ai::alternativeModel
        ),
    )
}

fun buildGptGeminiSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        ValueSettingItem(
            R.string.setting_title_gemini_key,
            0,
            R.string.setting_summary_gemini_key,
            config.ai::geminiApiKey
        ),
        ValueSettingItem(
            R.string.setting_title_gemini_model_name,
            0,
            R.string.setting_summary_gemini_model_name,
            config.ai::geminiModel
        ),
    )
}
