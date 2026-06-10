package info.plateaukao.einkbro.setting.screens

import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.DataListActivity
import info.plateaukao.einkbro.activity.WhiteListType
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.setting.ActionSettingItem
import info.plateaukao.einkbro.setting.BooleanSettingItem
import info.plateaukao.einkbro.setting.DividerSettingItem
import info.plateaukao.einkbro.setting.ListSettingWithClassItem
import info.plateaukao.einkbro.setting.ListSettingWithStrResIdItem
import info.plateaukao.einkbro.setting.SettingItemInterface
import info.plateaukao.einkbro.setting.ValueSettingItem

fun buildSearchSettingItems(deps: SettingScreenDeps): List<SettingItemInterface> {
    val config = deps.config
    return listOf(
        ListSettingWithStrResIdItem(
            R.string.setting_title_search_engine,
            0,
            config = config.browser::searchEngine,
            options = listOf(
                R.string.setting_summary_search_engine_startpage,
                R.string.setting_summary_search_engine_startpage_de,
                R.string.setting_summary_search_engine_baidu,
                R.string.setting_summary_search_engine_bing,
                R.string.setting_summary_search_engine_duckduckgo,
                R.string.setting_summary_search_engine_google,
                R.string.setting_summary_search_engine_searx,
                R.string.setting_summary_search_engine_qwant,
                R.string.setting_summary_search_engine_ecosia,
                R.string.setting_title_searchEngine,
                R.string.setting_summary_search_engine_yandex,
            )
        ),
        ValueSettingItem(
            R.string.setting_title_searchEngine,
            0,
            R.string.setting_summary_search_engine,
            config = config.browser::searchEngineUrl,
        ),
        BooleanSettingItem(
            R.string.setting_title_search_suggestion,
            0,
            R.string.setting_summary_search_suggestion,
            config.browser::enableSearchSuggestion,
        ),
        DividerSettingItem(),
        ValueSettingItem(
            R.string.setting_title_process_text,
            0,
            R.string.setting_summary_custom_process_text_url,
            config = config.browser::processTextUrl,
        ),
        BooleanSettingItem(
            R.string.setting_title_external_search_pop,
            0,
            R.string.setting_summary_external_search_pop,
            config.ai::externalSearchWithPopUp,
        ),
        DividerSettingItem(),
        ActionSettingItem(
            R.string.setting_title_split_search_setting,
            0,
            R.string.setting_summary_split_search_setting
        ) {
            deps.activity.startActivity(
                DataListActivity.createIntent(deps.activity, WhiteListType.SplitSearch)
            )
        },
        BooleanSettingItem(
            R.string.setting_title_search_in_same_tab,
            0,
            R.string.setting_summary_search_in_same_tab,
            config.ai::isExternalSearchInSameTab,
        ),
        DividerSettingItem(),
        ListSettingWithClassItem<ChatGPTActionInfo>(
            R.string.setting_title_remote_query,
            0,
            config = config.ai::remoteQueryActionName,
            options = listOf("Search") + config.ai.gptActionList.map { it.name }
        ),
    )
}
