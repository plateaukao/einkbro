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
import info.plateaukao.einkbro.browser.Javascript
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.unit.RecordUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WhiteListActivity : ComponentActivity(), KoinComponent {

    private lateinit var whitelistType: WhiteListTypeInterface
    private val dialogManager: DialogManager by lazy { DialogManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        whitelistType = getWhitelistType()
        setContent {
            val whitelist = remember { mutableStateOf(whitelistType.getList()) }
            MyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(whitelistType.titleId),
                                    color = MaterialTheme.colors.onPrimary
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
                                    whitelistType.addDomain { whitelist.value += it }
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

    private fun getWhitelistType(): WhiteListTypeInterface = when (intent.getSerializableExtra(TYPE) as WhiteListType) {
        WhiteListType.Adblock -> WhiteListTypeInterfaceAdblock(lifecycleScope, dialogManager)
        WhiteListType.Javascript -> WhiteListTypeInterfaceJavascript(lifecycleScope, dialogManager)
        WhiteListType.Cookie -> WhiteListTypeInterfaceCookie(lifecycleScope, dialogManager)
    }

    companion object {
        private const val TYPE = "type"
        fun createIntent(context: Context, type: WhiteListType) = Intent(
            context,
            WhiteListActivity::class.java
        ).apply {
            putExtra(TYPE, type)
        }
    }
}

enum class WhiteListType {
    Adblock,
    Cookie,
    Javascript
}


@Composable
fun WhiteListContent(
    modifier: Modifier = Modifier,
    list: MutableState<List<String>>,
    deleteAction: (String) -> Unit = {}
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
                text = stringResource(R.string.list_empty) + stringResource(R.string.empty_whitelist_hint),
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

interface WhiteListTypeInterface {
    val titleId: Int
    fun getList(): List<String>
    fun addDomain(postAction: (String) -> Unit)
    fun deleteDomain(domain: String)
    fun deleteAllDomains()
}

class WhiteListTypeInterfaceAdblock(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val dialogManager: DialogManager,
) : WhiteListTypeInterface, KoinComponent {
    override val titleId: Int = R.string.setting_title_whitelist
    private val adBlock: AdBlock by inject()
    private val recordDb: RecordDb by inject()
    override fun getList(): List<String> = recordDb.listDomains(RecordUnit.TABLE_WHITELIST)

    override fun addDomain(postAction: (String) -> Unit) {
        lifecycleScope.launch {
            val value = dialogManager.getTextInput(
                R.string.whitelist_add, R.string.setting_title_whitelist, ""
            ) ?: return@launch
            if (value.isNotBlank()) {
                adBlock.addDomain(value.trim());
                postAction(value.trim())
            }
        }
    }

    override fun deleteDomain(domain: String) = adBlock.removeDomain(domain)

    override fun deleteAllDomains() {
        dialogManager.showOkCancelDialog(
            messageResId = R.string.hint_database,
            okAction = {
                adBlock.clearDomains()
            },
            cancelAction = {}
        )
    }
}

class WhiteListTypeInterfaceJavascript(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val dialogManager: DialogManager,
) : WhiteListTypeInterface, KoinComponent {
    override val titleId: Int = R.string.setting_title_whitelistJS
    private val javascript: Javascript by inject()
    private val recordDb: RecordDb by inject()
    override fun getList(): List<String> = recordDb.listDomains(RecordUnit.TABLE_JAVASCRIPT)

    override fun addDomain(postAction: (String) -> Unit) {
        lifecycleScope.launch {
            val value = dialogManager.getTextInput(
                R.string.whitelist_add, R.string.setting_title_whitelistJS, ""
            ) ?: return@launch
            if (value.isNotBlank()) {
                javascript.addDomain(value.trim());
                postAction(value.trim())
            }
        }
    }

    override fun deleteDomain(domain: String) = javascript.removeDomain(domain)

    override fun deleteAllDomains() = javascript.clearDomains()

}

class WhiteListTypeInterfaceCookie(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val dialogManager: DialogManager,
) : WhiteListTypeInterface, KoinComponent {
    override val titleId: Int = R.string.setting_title_whitelistCookie
    private val cookie: Cookie by inject()
    private val recordDb: RecordDb by inject()
    override fun getList(): List<String> = recordDb.listDomains(RecordUnit.TABLE_COOKIE)

    override fun addDomain(postAction: (String) -> Unit) {
        lifecycleScope.launch {
            val value = dialogManager.getTextInput(
                R.string.whitelist_add, R.string.setting_title_whitelistCookie, ""
            ) ?: return@launch
            if (value.isNotBlank()) {
                cookie.addDomain(value.trim());
                postAction(value.trim())
            }
        }
    }

    override fun deleteDomain(domain: String) = cookie.removeDomain(domain)

    override fun deleteAllDomains() = cookie.clearDomains()
}
