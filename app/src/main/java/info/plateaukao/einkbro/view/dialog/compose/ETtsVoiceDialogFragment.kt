package info.plateaukao.einkbro.view.dialog.compose

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.tts.entity.VoiceItem
import info.plateaukao.einkbro.unit.HelperUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import kotlinx.serialization.json.Json
import java.util.Locale

class ETtsVoiceDialogFragment(
    private val selectedAction: (VoiceItem) -> Unit,
) : ComposeDialogFragment() {
    private val voices: List<VoiceItem> = Json.decodeFromString(
        HelperUnit.getStringFromAsset("eVoiceList.json")
    )

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            LanguageListScreen(
                selectedVoiceItem = config.ettsVoice,
                voices,
            ) {
                config.ettsVoice = it
                selectedAction(it)
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}

@Composable
fun LanguageListScreen(
    selectedVoiceItem: VoiceItem,
    voices: List<VoiceItem>,
    selectedAction: (VoiceItem) -> Unit = {},
) {
    // create language list based on voices' first segment by - separator
    val languageList = voices.map { it.getLanguageCode() }.distinct().toMutableList()
        .apply {
            remove("en")
            remove("zh")
            remove("ja")
            remove("ko")
            remove("fr")
            add(0, "fr")
            add(0, "ko")
            add(0, "ja")
            add(0, "zh")
            add(0, "en")
        }

    LazyColumn(
        modifier = Modifier.width(400.dp)
    ) {
        languageList.forEach { language ->
            item {
                val isExpanded = remember { mutableStateOf(false) }
                Text(
                    text = Locale(language).displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            isExpanded.value = !isExpanded.value
                        },
                    color = MaterialTheme.colors.onBackground
                )
                Divider()
                if (isExpanded.value) {
                    voices.filter { it.getLanguageCode() == language }
                        .forEach { voice ->
                            VoiceItemRow(
                                voice = voice,
                                selected = voice == selectedVoiceItem,
                                onClick = {
                                    selectedAction(voice)
                                }
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun VoiceItemRow(
    voice: VoiceItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val country = Locale(
        voice.getLanguageCode(),
        voice.getCountryCode()
    ).displayCountry
    val role = voice.getVoiceRole()
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp, 16.dp, 16.dp, 16.dp)
            .clickable(onClick = onClick),
        text = "$country - $role",
        color = MaterialTheme.colors.onBackground
    )
    Divider()
}

