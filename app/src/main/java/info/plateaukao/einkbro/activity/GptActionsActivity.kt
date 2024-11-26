package info.plateaukao.einkbro.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ChatGPTActionInfo
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.GptActionType
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.SelectableText
import org.koin.android.ext.android.inject

class GptActionsActivity : ComponentActivity() {
    private val config: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionIndex = intent?.getIntExtra("actionIndex", -1) ?: -1
        val defaultActionType = config.getDefaultActionType()

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
                        defaultActionType,
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
            if (showDialog) {
                GptActionDialog(
                    editActionIndex,
                    if (editActionIndex >= 0)
                        actionList.value[editActionIndex] else createDefaultGptAction(),

                    config.getGptTypeModelMap(),
                    okAction = { modifiedAction ->
                        actionList.value = actionList.value.toMutableList().apply {
                            if (editActionIndex >= 0) set(editActionIndex, modifiedAction)
                            else add(modifiedAction)
                        }
                        config.gptActionList = actionList.value
                        showDialog = false
                    },
                    dismissAction = { showDialog = false }
                )
            }
        }

    }

    private fun createDefaultGptAction(): ChatGPTActionInfo {
        return ChatGPTActionInfo(
            "New Action",
            "",
            "",
            GptActionType.Default,
            config.getDefaultActionModel()
        )
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
    defaultActionType: GptActionType,
    editAction: (Int) -> Unit, // edit action with index
    deleteAction: (ChatGPTActionInfo) -> Unit = {},
) {
    val context = LocalContext.current

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
            modifier = modifier.padding(10.dp),
            content = {
                items(list.value.size) { index ->
                    val gptAction = list.value[index]
                    val actionType = gptAction.actionType.takeIf { it != GptActionType.Default }
                        ?: defaultActionType

                    val iconRes = when (actionType) {
                        GptActionType.OpenAi -> R.drawable.ic_chat_gpt
                        GptActionType.SelfHosted -> R.drawable.ic_ollama
                        GptActionType.Gemini -> R.drawable.ic_gemini
                        else -> R.drawable.ic_chat_gpt
                    }
                    Row(
                        Modifier
                            .fillMaxSize()
                            .clickable {
                                editAction(index)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // icon: action type
                        Icon(
                            modifier = Modifier.wrapContentWidth(),
                            imageVector = ImageVector.vectorResource(id = iconRes),
                            contentDescription = "Action Type",
                        )
                        Spacer(modifier = Modifier.width(15.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
                                text = gptAction.name,
                                style = MaterialTheme.typography.h6,
                                color = MaterialTheme.colors.onBackground
                            )
                            if (gptAction.model.isNotEmpty()) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
                                    text = gptAction.model,
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onBackground
                                )
                            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GptActionDialog(
    editActionIndex: Int,
    action: ChatGPTActionInfo,
    gptTypeModelMap: Map<GptActionType, String>,
    okAction: (ChatGPTActionInfo) -> Unit,
    dismissAction: () -> Unit,
) {
    val name = remember { mutableStateOf("") }
    val systemPrompt = remember { mutableStateOf("") }
    val userPrompt = remember { mutableStateOf("") }
    val currentActionType = remember { mutableStateOf(GptActionType.Default) }
    val model = remember { mutableStateOf(action.model) }

    if (editActionIndex >= 0) {
        name.value = action.name
        systemPrompt.value = action.systemMessage
        userPrompt.value = action.userMessage
        currentActionType.value = action.actionType
    } else {
        name.value = ""
        systemPrompt.value = ""
        userPrompt.value = ""
        currentActionType.value = GptActionType.Default
    }

    AlertDialog(
        modifier = Modifier
            .padding(2.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(2.dp),
        // use caption style
        title = { Text("Action Setting", style = MaterialTheme.typography.h6) },
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
                Text(
                    modifier = Modifier.padding(5.dp),
                    text = "Service",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onBackground
                )
                FlowRow {
                    GptActionType.entries.map { gptActionType ->
                        val isSelect = currentActionType.value == gptActionType
                        SelectableText(
                            modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
                            selected = isSelect,
                            text = "$gptActionType",
                        ) {
                            currentActionType.value = gptActionType
                            model.value = gptTypeModelMap[gptActionType] ?: ""
                        }
                    }
                }
                TextField(
                    modifier = Modifier.padding(2.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = MaterialTheme.colors.onBackground,
                        backgroundColor = MaterialTheme.colors.background,
                    ),
                    value = model.value,
                    onValueChange = { model.value = it },
                    label = { Text("model") }
                )
            }
        },
        onDismissRequest = { dismissAction() },
        confirmButton = {
            TextButton(
                onClick = {
                    okAction(
                        ChatGPTActionInfo(
                            name.value,
                            systemPrompt.value,
                            userPrompt.value,
                            currentActionType.value,
                            model.value,
                        )
                    )
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

@Preview
@Composable
private fun GptActionListContentPreview() {
    val actionList = remember {
        mutableStateOf(
            listOf(
                ChatGPTActionInfo(
                    "ChatGPT",
                    "system message",
                    "user message",
                    GptActionType.SelfHosted,
                    "gpt-3"
                )
            )
        )
    }
    MyTheme {
        GptActionListContent(
            list = actionList,
            defaultActionType = GptActionType.OpenAi,
            editAction = {},
            deleteAction = {}
        )
    }
}