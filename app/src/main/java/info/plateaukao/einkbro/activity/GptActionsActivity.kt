package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.DialogManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GptActionsActivity : ComponentActivity(), KoinComponent {
    private val config: ConfigManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val actionList = remember { mutableStateOf(config.gptActionList) }
            var showDialog by remember { mutableStateOf(false) }

            MyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.gpt_actions_title),
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
                                    actionList.value = emptyList()
                                    config.deleteAllGptActions()
                                }) {
                                    Icon(
                                        tint = MaterialTheme.colors.onPrimary,
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.menu_delete)
                                    )
                                }
                                IconButton(onClick = {
                                    showDialog = true
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
                    GptActionListContent(
                        modifier = Modifier.padding(innerPadding),
                        actionList
                    ) {
                        actionList.value = actionList.value.toMutableList().apply { remove(it) }
                        config.deleteGptAction(it)
                    }
                }
            }
            GptActionDialog(
                showDialog,
                okAction = { name, systemMessage, userMessage ->
                    actionList.value = actionList.value.toMutableList().apply {
                        add(ChatGPTActionInfo(name, systemMessage, userMessage))
                    }
                    config.gptActionList = actionList.value
                    showDialog = false
                },
                dismissAction = { showDialog = false }
            )
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(
            context,
            GptActionsActivity::class.java
        )
    }
}

@Composable
fun GptActionListContent(
    modifier: Modifier = Modifier,
    list: MutableState<List<ChatGPTActionInfo>>,
    deleteAction: (ChatGPTActionInfo) -> Unit = {}
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
                    val gptAction = list.value[index]
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(modifier = Modifier.weight(1f), text = gptAction.name)
                        IconButton(onClick = {
                            deleteAction(list.value[index])
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

@Composable
fun GptActionDialog(
    showDialog: Boolean,
    okAction: (String, String, String) -> Unit,
    dismissAction: () -> Unit
) {
    val name = remember { mutableStateOf("") }
    val systemPrompt = remember { mutableStateOf("") }
    val userPrompt = remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            title = { Text("Action Setting") },
            text = {
                Column {
                    TextField(
                        value = name.value,
                        onValueChange = { name.value = it },
                        label = { Text("name") }
                    )
                    TextField(
                        value = systemPrompt.value,
                        onValueChange = { systemPrompt.value = it },
                        label = { Text("system prompt") }
                    )
                    TextField(
                        value = userPrompt.value,
                        onValueChange = { userPrompt.value = it },
                        label = { Text("user prompt") }
                    )
                }
            },
            onDismissRequest = { dismissAction() },
            confirmButton = {
                TextButton(
                    onClick = {
                        okAction(name.value, systemPrompt.value, userPrompt.value)
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
