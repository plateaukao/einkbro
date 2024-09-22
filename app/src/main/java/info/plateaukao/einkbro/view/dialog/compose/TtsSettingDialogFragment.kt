package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.SelectableText
import info.plateaukao.einkbro.view.dialog.TtsLanguageDialog
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
                val readProgress = ttsViewModel.readProgress.collectAsState()
                val readingState = ttsViewModel.isReading.collectAsState()

                Column(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    MainTtsSettingDialog(
                        selectedType = ttsType.value,
                        selectedLocale = config.ttsLocale,
                        selectedGptVoice = gptVoice.value,
                        selectedEttsVoice = ettsVoice.value,
                        selectedSpeedValue = config.ttsSpeedValue,
                        onSpeedValueClick = { config.ttsSpeedValue = it; dismiss() },
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
                        onVoiceSelected = { config.ettsVoice = it; dismiss() },
                    )
                    TtsDialogButtonBar(
                        readingState = readingState.value,
                        isVoicePlaying = ttsViewModel.isVoicePlaying(),
                        showSystemSetting = ttsType.value == TtsType.SYSTEM,
                        readProgress = readProgress.value,
                        gotoSettingAction = { IntentUnit.gotoSystemTtsSettings(requireActivity()) },
                        stopAction = { ttsViewModel.stop(); dismiss() },
                        pauseOrResumeAction = { ttsViewModel.pauseOrResume(); dismiss() },
                        addToReadListAction = this@TtsSettingDialogFragment::readCurrentArticle,
                        dismissAction = { dismiss() },
                    )
                }
            }
        }
    }

    private fun readCurrentArticle() {
        IntentUnit.readCurrentArticle(requireActivity())
        dismiss()
        NinjaToast.show(requireContext(), R.string.added_to_read_list)
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
        Text(
            stringResource(id = R.string.setting_tts_type),
            modifier = Modifier.padding(vertical = 6.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
        )
        Row {
            TtsType.entries.forEach { type ->
                val isSelect = selectedType == type
                SelectableText(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 3.dp),
                    selected = isSelect,
                    text = stringResource(type.toStringResId()),
                ) {
                    onTtsTypeSelected(type)
                }
            }
        }
        if (selectedType in listOf(TtsType.ETTS, TtsType.GPT)) {
            Text(
                stringResource(id = R.string.setting_tts_voice),
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )
        }
        if (selectedType == TtsType.GPT) {
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
        if (selectedType == TtsType.ETTS) {
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
        if (selectedType == TtsType.SYSTEM) {
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
        if (!ViewUnit.isTablet(LocalContext.current)) {
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
    }
}

@Composable
fun TtsDialogButtonBar(
    readingState: Boolean,
    isVoicePlaying: Boolean,
    showSystemSetting: Boolean,
    readProgress: String,
    stopAction: () -> Unit,
    pauseOrResumeAction: () -> Unit,
    addToReadListAction: () -> Unit,
    gotoSettingAction: () -> Unit,
    dismissAction: () -> Unit,
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
            if (showSystemSetting) {
                TextButton(
                    modifier = Modifier.wrapContentWidth(),
                    onClick = gotoSettingAction
                ) {
                    Text(
                        stringResource(id = R.string.system_settings),
                        color = MaterialTheme.colors.onBackground
                    )
                }
            } else {
                if (readingState) {
                    Text(
                        readProgress,
                        modifier = Modifier.padding(horizontal = 8.dp),
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
                    IconButton(
                        onClick = pauseOrResumeAction,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(
                            if (isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "pause or resume",
                            tint = MaterialTheme.colors.onBackground
                        )
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
            }
            VerticalSeparator()
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick = dismissAction
            ) {
                Text(
                    stringResource(id = R.string.close),
                    color = MaterialTheme.colors.onBackground
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