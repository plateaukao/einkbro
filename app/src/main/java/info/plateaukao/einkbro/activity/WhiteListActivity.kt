package info.plateaukao.einkbro.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.browser.AdBlock
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.unit.RecordUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class WhiteListActivity : ComponentActivity(), KoinComponent {
    private val dialogManager by lazy { DialogManager(this) }

    private val recordDb: RecordDb by lazy {
        RecordDb(this).apply { open(false) }
    }
    private val list: MutableList<String> by lazy { recordDb.listDomains(RecordUnit.TABLE_WHITELIST).toMutableList() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.setting_title_whitelist),
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
                                IconButton(onClick = { deleteAllDomains() }) {
                                    Icon(
                                        tint = MaterialTheme.colors.onPrimary,
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.menu_delete)
                                    )
                                }
                                IconButton(onClick = { addDomain() }) {
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
                        list,
                        this::deleteDomain
                    )
                }
            }
        }
    }

    private fun addDomain() {
        lifecycleScope.launch {
            val value = dialogManager.getTextInput(
                R.string.whitelist_add, R.string.whitelist_add, ""
            ) ?: return@launch
            if (value.isNotBlank()) {
                AdBlock(this@WhiteListActivity).addDomain(value.trim());
            }
        }
    }

    private fun deleteDomain(domain: String) {
        recordDb.deleteDomain(RecordUnit.TABLE_WHITELIST, domain)
    }

    private fun deleteAllDomains() {
        AdBlock(this).clearDomains()
        list.clear()
    }
}

@Composable
fun WhiteListContent(
    modifier: Modifier = Modifier,
    list: List<String>,
    deleteAction: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        content = {
            items(list.size) { index ->
                val itemText = list[index]
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(modifier = Modifier.weight(1f), text = itemText)
                    IconButton(onClick = { deleteAction(itemText) }) {
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