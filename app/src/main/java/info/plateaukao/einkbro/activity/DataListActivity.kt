package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AdBlock
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.browser.DomainInterface
import info.plateaukao.einkbro.browser.Javascript
import info.plateaukao.einkbro.search.SplitSearchListType
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DataListActivity : ComponentActivity(), KoinComponent {

    private lateinit var whitelistType: BaseWhiteListType
    private val dialogManager: DialogManager by lazy { DialogManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        whitelistType = getWhitelistType()
        setContent {
            val whitelist = remember { mutableStateOf(whitelistType.getDomains()) }
            MyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(whitelistType.titleId),
                                    color = MaterialTheme.colors.onPrimary,
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        tint = MaterialTheme.colors.onPrimary,
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    whitelistType.deleteAllDomains()
                                    whitelist.value = emptyList()
                                }) {
                                    Icon(
                                        tint = MaterialTheme.colors.onPrimary,
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.menu_delete)
                                    )
                                }
                                IconButton(onClick = {
                                    whitelistType.addDomain(
                                        lifecycleScope,
                                        dialogManager
                                    ) { whitelist.value += it }
                                }) {
                                    Icon(
                                        tint = MaterialTheme.colors.onPrimary,
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = stringResource(R.string.whitelist_add)
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    WhiteListContent(
                        modifier = Modifier.padding(innerPadding),
                        whitelist,
                        whitelistType::deleteDomain
                    )
                }
            }
        }
    }

    private fun getWhitelistType(): BaseWhiteListType =
        when (intent.getSerializableExtra(TYPE) as WhiteListType) {
            WhiteListType.Adblock -> WhiteListTypeAdblock()
            WhiteListType.Javascript -> BaseWhiteListTypeJavascript()
            WhiteListType.Cookie -> BaseWhiteListTypeCookie()
            WhiteListType.SplitSearch -> SplitSearchListType()
        }

    companion object {
        private const val TYPE = "type"
        fun createIntent(context: Context, type: WhiteListType) = Intent(
            context,
            DataListActivity::class.java
        ).apply {
            putExtra(TYPE, type)
        }
    }
}

enum class WhiteListType {
    Adblock,
    Cookie,
    Javascript,
    SplitSearch
}


@Composable
fun WhiteListContent(
    modifier: Modifier = Modifier,
    list: MutableState<List<String>>,
    deleteAction: (String) -> Unit = {},
) {
    if (list.value.isEmpty()) {
        // show empty text in center of screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.list_empty) + "\n" + stringResource(R.string.empty_whitelist_hint),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            content = {
                items(list.value.size) { index ->
                    val itemText = list.value[index]
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(modifier = Modifier.weight(1f), text = itemText)
                        IconButton(onClick = {
                            deleteAction(itemText)
                            list.value = list.value.filter { it != itemText }
                        }) {
                            Icon(
                                tint = MaterialTheme.colors.onBackground,
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.menu_delete)
                            )
                        }
                    }
                }
            }
        )
    }
}

abstract class BaseWhiteListType {
    abstract val titleId: Int
    abstract val domainHandler: DomainInterface
    fun getDomains(): List<String> = domainHandler.getDomains()
    open fun addDomain(
        lifecycleScope: LifecycleCoroutineScope,
        dialogManager: DialogManager,
        postAction: (String) -> Unit,
    ) {
        lifecycleScope.launch {
            val value = dialogManager.getTextInput(
                R.string.whitelist_add, titleId, ""
            ) ?: return@launch
            if (value.isNotBlank()) {
                domainHandler.addDomain(value.trim());
                postAction(value.trim())
            }
        }
    }

    fun deleteDomain(domain: String) = domainHandler.deleteDomain(domain)
    fun deleteAllDomains() = domainHandler.deleteAllDomains()
}

class WhiteListTypeAdblock : BaseWhiteListType(), KoinComponent {
    override val titleId: Int = R.string.setting_title_whitelist
    private val adBlock: AdBlock by inject()
    override val domainHandler: DomainInterface by lazy { adBlock }
}

class BaseWhiteListTypeJavascript : BaseWhiteListType(), KoinComponent {
    override val titleId: Int = R.string.setting_title_whitelistJS
    private val javascript: Javascript by inject()
    override val domainHandler: DomainInterface by lazy { javascript }
}

class BaseWhiteListTypeCookie : BaseWhiteListType(), KoinComponent {
    override val titleId: Int = R.string.setting_title_whitelistCookie
    private val cookie: Cookie by inject()
    override val domainHandler: DomainInterface by lazy { cookie }
}
