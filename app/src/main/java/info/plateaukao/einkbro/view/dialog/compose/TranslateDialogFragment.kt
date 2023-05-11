package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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

class TranslateDialogFragment(
    private val translationViewModel: TranslationViewModel,
    private val translateApi: TRANSLATE_API,
    private val anchorPoint: Point,
) : DraggableComposeDialogFragment() {

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            TranslateResponse(
                translationViewModel,
                { changeTranslationLanguage() },
                { dismiss() }
            )
        }
    }

    private fun changeTranslationLanguage() {
        lifecycleScope.launch {
            val translationLanguage =
                TranslationLanguageDialog(requireActivity()).show() ?: return@launch
            translationViewModel.updateTranslationLanguageAndGo(translateApi, translationLanguage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setupDialogPosition(anchorPoint)

        translationViewModel.translate(translateApi)
        return view
    }
}

@Composable
private fun TranslateResponse(
    translationViewModel: TranslationViewModel,
    onTargetLanguageClick: () -> Unit,
    closeClick: () -> Unit = { },
) {
    val requestMessage by translationViewModel.inputMessage.collectAsState()
    val responseMessage by translationViewModel.responseMessage.collectAsState()
    val targetLanguage by translationViewModel.translationLanguage.collectAsState()
    val showRequest = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 200.dp)
            .wrapContentHeight()
            .width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectableText(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                selected = true,
                text = targetLanguage.language,
                textAlign = TextAlign.Center,
                onClick = onTargetLanguageClick
            )
            Icon(
                painter = painterResource(
                    id = if (showRequest.value) R.drawable.icon_arrow_up_gest else R.drawable.icon_info
                ),
                contentDescription = "Info Icon",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { showRequest.value = !showRequest.value }
            )
            Icon(
                painter = painterResource(id = R.drawable.icon_close),
                contentDescription = "Close Icon",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { closeClick() }
            )
        }
        if (showRequest.value) {
            Text(
                text = requestMessage,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.padding(10.dp)
            )
            Divider()
        }
        Text(
            text = responseMessage,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(10.dp)
        )
    }
}
