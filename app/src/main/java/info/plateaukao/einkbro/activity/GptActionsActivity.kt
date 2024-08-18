package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.SelectableText
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GptActionsActivity : ComponentActivity(), KoinComponent {
    private val config: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionIndex = intent?.getIntExtra("actionIndex", -1) ?: -1

        setContent {
            val actionList = remember { mutableStateOf(config.gptActionList) }
            var showDialog by remember { mutableStateOf(false) }
            var editActionIndex by remember { mutableIntStateOf(actionIndex) }

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
                                    editActionIndex = -1
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
                        actionList,
                        editAction = { index ->
                            editActionIndex = index
                            showDialog = true
                        },
                    ) {
                        actionList.value = actionList.value.toMutableList().apply { remove(it) }
                        config.deleteGptAction(it)
                    }
                }
            }
            GptActionDialog(
                showDialog,
                editActionIndex,
                if (editActionIndex >= 0) actionList.value[editActionIndex] else null,
                okAction = { name, systemMessage, userMessage, type ->
                    actionList.value = actionList.value.toMutableList().apply {
                        if (editActionIndex >= 0)
                            set(
                                editActionIndex,
                                ChatGPTActionInfo(name, systemMessage, userMessage, type)
                            )
                        else
                            add(ChatGPTActionInfo(name, systemMessage, userMessage, type))
                    }
                    config.gptActionList = actionList.value
                    showDialog = false
                },
                dismissAction = { showDialog = false }
            )
        }

    }

    companion object {
        fun start(context: Context) = context.startActivity(
            Intent(
                context,
                GptActionsActivity::class.java
            )
        )
    }
}

@Composable
fun GptActionListContent(
    modifier: Modifier = Modifier,
    list: MutableState<List<ChatGPTActionInfo>>,
    editAction: (Int) -> Unit, // edit action with index
    deleteAction: (ChatGPTActionInfo) -> Unit = {},
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
                        SelectableText(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 1.dp, vertical = 3.dp),
                            selected = false,
                            text = gptAction.name
                        ) {
                            editAction(index)
                        }
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
    editActionIndex: Int,
    action: ChatGPTActionInfo? = null,
    okAction: (String, String, String, GptActionType) -> Unit,
    dismissAction: () -> Unit,
) {
    val name = remember { mutableStateOf("") }
    val systemPrompt = remember { mutableStateOf("") }
    val userPrompt = remember { mutableStateOf("") }
    val actionType = remember { mutableStateOf(GptActionType.Default) }

    if (editActionIndex >= 0 && action != null) {
        name.value = action.name
        systemPrompt.value = action.systemMessage
        userPrompt.value = action.userMessage
        actionType.value = action.actionType
    } else {
        name.value = ""
        systemPrompt.value = ""
        userPrompt.value = ""
        actionType.value = GptActionType.Default
    }

    var actionExpanded by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            modifier = Modifier
                .padding(2.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.onBackground,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(2.dp),
            title = { Text("Action Setting") },
            text = {
                // set dim amount to 0 to avoid dialog window's dim
                (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0f)
                Column {
                    TextField(
                        modifier = Modifier.padding(2.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.colors.onBackground,
                            backgroundColor = MaterialTheme.colors.background,
                        ),
                        value = name.value,
                        onValueChange = { name.value = it },
                        label = { Text("Name") }
                    )
                    TextField(
                        modifier = Modifier.padding(2.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.colors.onBackground,
                            backgroundColor = MaterialTheme.colors.background,
                        ),
                        value = systemPrompt.value,
                        onValueChange = { systemPrompt.value = it },
                        label = { Text("System Prompt") }
                    )
                    TextField(
                        modifier = Modifier.padding(2.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.colors.onBackground,
                            backgroundColor = MaterialTheme.colors.background,
                        ),
                        minLines = 3,
                        value = userPrompt.value,
                        onValueChange = { userPrompt.value = it },
                        label = { Text("User Prompt") }
                    )
                    TextButton(onClick = { actionExpanded = true }) {
                        Text(
                            text = "Action Type: ${actionType.value}",
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                    DropdownMenu(
                        modifier = Modifier.padding(2.dp),
                        expanded = actionExpanded,
                        onDismissRequest = { actionExpanded = false }
                    ) {
                        GptActionType.entries.forEach { type ->
                            DropdownMenuItem(onClick = {
                                actionType.value = type
                                actionExpanded = false
                            }) {
                                Text(text = type.name)
                            }
                        }
                    }
                }
            },
            onDismissRequest = { dismissAction() },
            confirmButton = {
                TextButton(
                    onClick = {
                        okAction(name.value, systemPrompt.value, userPrompt.value, actionType.value)
                    }
                ) {
                    Text(
                        stringResource(id = android.R.string.ok),
                        color = MaterialTheme.colors.onBackground
                    )
                }
            }
        )
    }
}