package info.plateaukao.einkbro.search

import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BaseWhiteListType
import info.plateaukao.einkbro.browser.DomainInterface
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.SplitSearchItemInfo
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SplitSearchListType : BaseWhiteListType() {
    override val titleId: Int
        get() = R.string.split_search_settings
    override val domainHandler: DomainInterface by lazy { SplitSearchHandler() }

    override fun addDomain(
        lifecycleScope: LifecycleCoroutineScope,
        dialogManager: DialogManager,
        postAction: (String) -> Unit
    ) {
        lifecycleScope.launch {
            val title = dialogManager.getTextInput(
                titleId, R.string.description_split_screen_title, ""
            )?.trim() ?: return@launch
            val stringPattern = dialogManager.getTextInput(
                titleId, R.string.description_split_screen_string_pattern, ""
            )?.trim() ?: return@launch
            if (title.isNotBlank() && stringPattern.isNotBlank()) {
                (domainHandler as SplitSearchHandler).addSplitSearchItem(
                    SplitSearchItemInfo(title, stringPattern, true)
                )
                postAction(title)
            }
        }
    }
}

private class SplitSearchHandler : DomainInterface, KoinComponent {
    private val configManager: ConfigManager by inject()
    override fun getDomains(): List<String> =
        configManager.splitSearchItemInfoList.map { it.title }

    override fun addDomain(domain: String) { /* use addSplitSearchItem instead */
    }

    fun addSplitSearchItem(splitSearchItemInfo: SplitSearchItemInfo) {
        configManager.addSplitSearchItem(splitSearchItemInfo)
    }

    override fun deleteDomain(domain: String) {
        configManager.deleteSplitSearchItem(SplitSearchItemInfo(domain, domain, true))
    }

    override fun deleteAllDomains() = configManager.deleteAllSplitSearchItems()
}