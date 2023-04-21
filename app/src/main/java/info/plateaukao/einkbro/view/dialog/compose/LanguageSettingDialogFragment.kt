package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.SelectableText
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import kotlinx.coroutines.launch

class LanguageSettingDialogFragment(
    private val translateApi: TRANSLATE_API,
    private val translationViewModel: TranslationViewModel,
    private val translate: () -> Unit,
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            PapagoSetting(
                translateApi == TRANSLATE_API.PAPAGO,
                translationViewModel,
                { changeTranslationLanguage() },
                { changeSourceLanguage() },
                { translate() },
                { dismiss() }

            )
        }
    }

    private fun changeTranslationLanguage() {
        lifecycleScope.launch {
            config.translationLanguage =
                (TranslationLanguageDialog(requireActivity()).show() ?: return@launch)
            translationViewModel.updateTranslationLanguage(config.translationLanguage)
        }
    }

    private fun changeSourceLanguage() {
        lifecycleScope.launch {
            config.sourceLanguage =
                TranslationLanguageDialog(requireActivity()).showPapagoSourceLanguage()
                    ?: return@launch
            translationViewModel.updateSourceLanguage(config.sourceLanguage)
        }
    }
}

@Composable
fun PapagoSetting(
    shouldShowSourceLanguage: Boolean,
    translationViewModel: TranslationViewModel,
    changeTranslationLanguage: () -> Unit,
    changeSourceLanguage: () -> Unit,
    translate: () -> Unit,
    dismiss: () -> Unit,
) {
    val targetLanguage by translationViewModel.translationLanguage.collectAsState()
    val sourceLanguage by translationViewModel.sourceLanguage.collectAsState()
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = stringResource(R.string.papago_language_setting),
            style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (shouldShowSourceLanguage) {
                SelectableText(
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp),
                    selected = true,
                    text = sourceLanguage.language,
                    textAlign = TextAlign.Center,
                    onClick = changeSourceLanguage
                )
                Text(
                    text = "→",
                    color = MaterialTheme.colors.onBackground,
                )
            }
            SelectableText(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                selected = true,
                text = targetLanguage.language,
                textAlign = TextAlign.Center,
                onClick = changeTranslationLanguage
            )
        }
        HorizontalSeparator()
        DialogButtonBar(
            okResId = R.string.translate,
            dismissAction = { dismiss() },
            okAction = {
                translate()
                dismiss()
            }
        )
    }
}