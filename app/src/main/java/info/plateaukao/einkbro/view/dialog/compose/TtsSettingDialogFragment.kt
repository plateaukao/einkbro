package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.service.GptVoiceOption
import info.plateaukao.einkbro.service.TtsManager
import info.plateaukao.einkbro.tts.entity.VoiceItem
import info.plateaukao.einkbro.tts.entity.defaultVoiceItem
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.EBToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.SelectableText
import info.plateaukao.einkbro.view.dialog.TtsLanguageDialog
import info.plateaukao.einkbro.viewmodel.TtsReadingState
import info.plateaukao.einkbro.viewmodel.TtsReadingState.IDLE
import info.plateaukao.einkbro.viewmodel.TtsReadingState.PAUSED
import info.plateaukao.einkbro.viewmodel.TtsType
import info.plateaukao.einkbro.viewmodel.TtsViewModel
import info.plateaukao.einkbro.viewmodel.toStringResId
import org.koin.core.component.inject
import java.util.Locale

class TtsSettingDialogFragment : ComposeDialogFragment() {
    private val ttsManager: TtsManager by inject()
    private val ttsViewModel: TtsViewModel by activityViewModels()

    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                val ttsType = remember { mutableStateOf(config.ttsType) }
                val ettsVoice = remember { mutableStateOf(config.ettsVoice) }
                val gptVoice = remember { mutableStateOf(config.gptVoiceOption) }
                val ttsSpeedValue = remember { mutableIntStateOf(config.ttsSpeedValue) }
                val readProgress = ttsViewModel.readProgress.collectAsState()
                val readingState = ttsViewModel.readingState.collectAsState()
                val currentReadingContent = ttsViewModel.currentReadingContent.collectAsState()
                val showReadingContext = ttsViewModel.showCurrentText.collectAsState()

                Column(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    if (readingState.value != IDLE && showReadingContext.value) {
                        Text(
                            currentReadingContent.value,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 300.dp)
                                .padding(vertical = 6.dp)
                                .clickable {
                                    ttsViewModel.toggleShowTranslation()
                                },
                            color = MaterialTheme.colors.onBackground
                        )
                    } else {
                        MainTtsSettingDialog(
                            readingState = readingState.value,
                            selectedType = ttsType.value,
                            selectedLocale = config.ttsLocale,
                            selectedGptVoice = gptVoice.value,
                            selectedEttsVoice = ettsVoice.value,
                            selectedSpeedValue = ttsSpeedValue.value,
                            onSpeedValueClick = { config.ttsSpeedValue = it; ttsSpeedValue.value = it },
                            recentVoices = config.recentUsedTtsVoices,
                            showLocaleDialog = { TtsLanguageDialog(requireContext()).show(ttsManager.getAvailableLanguages()) },
                            onTtsTypeSelected = {
                                config.ttsType = it
                                ttsType.value = it
                            },
                            showEttsVoiceDialog = {
                                ETtsVoiceDialogFragment {
                                    ettsVoice.value = it
                                }.show(parentFragmentManager, "ETtsVoiceDialog")
                            },
                            onGptVoiceSelected = { config.gptVoiceOption = it; gptVoice.value = it },
                            onVoiceSelected = { config.ettsVoice = it; ettsVoice.value = it },
                        )
                    }
                    TtsDialogButtonBar(
                        readingState = readingState.value,
                        showNextButton = ttsViewModel.hasNextArticle(),
                        ttsType = ttsType.value,
                        readProgress = readProgress.value.toString(),
                        nextArticleAction = ttsViewModel::nextArticle,
                        gotoSettingAction = { IntentUnit.gotoSystemTtsSettings(requireActivity()) },
                        stopAction = ttsViewModel::reset,
                        pauseOrResumeAction = ttsViewModel::pauseOrResume,
                        addToReadListAction = this@TtsSettingDialogFragment::readCurrentArticle,
                        dismissAction = ::dismiss,
                        clickProgressAction = { ttsViewModel.toggleShowCurrentText() }
                    )
                }
            }
        }
    }

    private fun readCurrentArticle() {
        IntentUnit.readCurrentArticle(requireActivity())
        EBToast.show(requireContext(), R.string.added_to_read_list)
    }
}

private val speedRateValueList = listOf(
    75,
    100,
    125,
)

private val speedRateValueList2 = listOf(
    150,
    175,
    200
)

@Composable
private fun MainTtsSettingDialog(
    readingState: TtsReadingState,
    selectedType: TtsType,
    selectedLocale: Locale,
    selectedGptVoice: GptVoiceOption,
    selectedEttsVoice: VoiceItem,
    recentVoices: List<VoiceItem>,
    selectedSpeedValue: Int,
    onSpeedValueClick: (Int) -> Unit,
    onGptVoiceSelected: (GptVoiceOption) -> Unit,
    onVoiceSelected: (VoiceItem) -> Unit,
    showLocaleDialog: () -> Unit,
    onTtsTypeSelected: (TtsType) -> Unit,
    showEttsVoiceDialog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            .width(IntrinsicSize.Max)
    ) {
        TtsEngineTitle()
        TtsEngineSelection(selectedType, readingState, onTtsTypeSelected)
        if (selectedType in listOf(TtsType.ETTS, TtsType.GPT)) {
            VoiceTitle()
        }
        if (selectedType == TtsType.GPT) {
            GptVoiceSelection(selectedGptVoice, onGptVoiceSelected)
        }
        if (selectedType == TtsType.ETTS) {
            ETtsVoiceSelection(selectedEttsVoice, showEttsVoiceDialog, recentVoices, onVoiceSelected)
        }
        if (selectedType == TtsType.SYSTEM) {
            SystemLanguageSelection(selectedLocale, showLocaleDialog)
        }
        ReadingSpeedSelection(selectedSpeedValue, onSpeedValueClick)
        if (!ViewUnit.isTablet(LocalContext.current)) {
            ReadingSpeedSelection2ndRow(selectedSpeedValue, onSpeedValueClick)
        }
    }
}

@Composable
private fun ReadingSpeedSelection2ndRow(selectedSpeedValue: Int, onSpeedValueClick: (Int) -> Unit) {
    Row {
        speedRateValueList2.map { speedRate ->
            val isSelect = selectedSpeedValue == speedRate
            SelectableText(
                modifier = Modifier
                    .padding(horizontal = 1.dp, vertical = 3.dp),
                selected = isSelect,
                text = "$speedRate%",
            ) {
                onSpeedValueClick(speedRate)
            }
        }
    }
}

@Composable
private fun ReadingSpeedSelection(selectedSpeedValue: Int, onSpeedValueClick: (Int) -> Unit) {
    Text(
        stringResource(id = R.string.read_speed),
        modifier = Modifier.padding(vertical = 6.dp),
        color = MaterialTheme.colors.onBackground,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
    )
    Row {
        speedRateValueList.map { speedRate ->
            val isSelect = selectedSpeedValue == speedRate
            SelectableText(
                modifier = Modifier
                    .padding(horizontal = 1.dp, vertical = 3.dp),
                selected = isSelect,
                text = "$speedRate%",
            ) {
                onSpeedValueClick(speedRate)
            }
        }
        if (ViewUnit.isTablet(LocalContext.current)) {
            speedRateValueList2.map { speedRate ->
                val isSelect = selectedSpeedValue == speedRate
                SelectableText(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 3.dp),
                    selected = isSelect,
                    text = "$speedRate%",
                ) {
                    onSpeedValueClick(speedRate)
                }
            }
        }
    }
}

@Composable
private fun SystemLanguageSelection(selectedLocale: Locale, showLocaleDialog: () -> Unit) {
    Text(
        stringResource(id = R.string.setting_tts_locale),
        modifier = Modifier.padding(vertical = 6.dp),
        color = MaterialTheme.colors.onBackground,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
    )
    SelectableText(
        modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
        selected = true, text = selectedLocale.displayName
    ) {
        showLocaleDialog()
    }
}

@Composable
private fun ETtsVoiceSelection(
    selectedEttsVoice: VoiceItem,
    showEttsVoiceDialog: () -> Unit,
    recentVoices: List<VoiceItem>,
    onVoiceSelected: (VoiceItem) -> Unit,
) {
    SelectableText(
        modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
        selected = true,
        text = Locale(selectedEttsVoice.getLanguageCode()).displayName
                + " " + selectedEttsVoice.getShortNameWithoutNeural()
    ) {
        showEttsVoiceDialog()
    }
    recentVoices.filterNot { it.name == selectedEttsVoice.name }.forEach { voice ->
        SelectableText(
            modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
            selected = false,
            text = Locale(voice.getLanguageCode()).displayName
                    + " " + voice.getShortNameWithoutNeural()
        ) {
            onVoiceSelected(voice)
        }
    }
    SelectableText(
        modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
        selected = false,
        text = LocalContext.current.getString(R.string.other_voices)
    ) {
        showEttsVoiceDialog()
    }
}

@Composable
private fun GptVoiceSelection(
    selectedGptVoice: GptVoiceOption,
    onGptVoiceSelected: (GptVoiceOption) -> Unit,
) {
    GptVoiceOption.entries.forEach {
        val isSelect = selectedGptVoice == it
        SelectableText(
            modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp),
            selected = isSelect,
            text = it.name
        ) {
            onGptVoiceSelected(it)
        }
    }
}

@Composable
private fun VoiceTitle() {
    Text(
        stringResource(id = R.string.setting_tts_voice),
        modifier = Modifier.padding(vertical = 6.dp),
        color = MaterialTheme.colors.onBackground,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun TtsEngineSelection(
    selectedType: TtsType,
    readingState: TtsReadingState,
    onTtsTypeSelected: (TtsType) -> Unit,
) {
    Row {
        TtsType.entries.forEach { type ->
            val isSelect = selectedType == type
            SelectableText(
                modifier = Modifier
                    .padding(horizontal = 1.dp, vertical = 3.dp),
                selected = isSelect,
                isEnabled = readingState == IDLE || isSelect,
                text = stringResource(type.toStringResId()),
            ) {
                onTtsTypeSelected(type)
            }
        }
    }
}

@Composable
private fun TtsEngineTitle() {
    Text(
        stringResource(id = R.string.setting_tts_type),
        modifier = Modifier.padding(vertical = 6.dp),
        color = MaterialTheme.colors.onBackground,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun TtsDialogButtonBar(
    readingState: TtsReadingState,
    showNextButton: Boolean = false,
    ttsType: TtsType,
    readProgress: String,
    nextArticleAction: () -> Unit,
    stopAction: () -> Unit,
    pauseOrResumeAction: () -> Unit,
    addToReadListAction: () -> Unit,
    gotoSettingAction: () -> Unit,
    dismissAction: () -> Unit,
    clickProgressAction: () -> Unit,
) {
    Column {
        HorizontalSeparator()
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (readingState != IDLE) {
                Text(
                    readProgress,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { clickProgressAction() },
                    color = MaterialTheme.colors.onBackground
                )
                VerticalSeparator()
                IconButton(
                    onClick = addToReadListAction,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        "Add to read list",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
                if (ttsType != TtsType.SYSTEM) {
                    IconButton(
                        onClick = pauseOrResumeAction,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(
                            if (readingState != PAUSED) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "pause or resume",
                            tint = MaterialTheme.colors.onBackground
                        )
                    }
                }
                if (showNextButton) {
                    IconButton(
                        onClick = nextArticleAction,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            "Next Article",
                            tint = MaterialTheme.colors.onBackground
                        )
                    }
                }
                IconButton(
                    onClick = stopAction,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Icon(
                        Icons.Default.Stop,
                        "Stop",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            if (ttsType == TtsType.SYSTEM) {
                TextButton(
                    modifier = Modifier.wrapContentWidth(),
                    onClick = gotoSettingAction
                ) {
                    Text(
                        stringResource(id = R.string.system_settings),
                        color = MaterialTheme.colors.onBackground
                    )
                }
            }
            if (readingState == IDLE) {
                IconButton(
                    onClick = addToReadListAction,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "pause or resume",
                        tint = MaterialTheme.colors.onBackground
                    )
                }
            }
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick = dismissAction
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    "Stop",
                    tint = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}

@Preview(widthDp = 600, showBackground = true)
@Composable
fun PreviewMainTtsDialog() {
    MyTheme {
        MainTtsSettingDialog(
            readingState = TtsReadingState.IDLE,
            selectedType = TtsType.SYSTEM,
            selectedGptVoice = GptVoiceOption.Alloy,
            selectedLocale = Locale.US,
            selectedSpeedValue = 100,
            recentVoices = listOf(defaultVoiceItem),
            onVoiceSelected = {},
            onSpeedValueClick = {},
            showLocaleDialog = {},
            onTtsTypeSelected = {},
            showEttsVoiceDialog = {},
            selectedEttsVoice = defaultVoiceItem,
            onGptVoiceSelected = {}
        )
    }
}