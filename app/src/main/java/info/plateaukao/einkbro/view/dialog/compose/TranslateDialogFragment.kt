package info.plateaukao.einkbro.view.dialog.compose

import android.content.DialogInterface
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.unit.ShareUtil
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.dialog.TranslationLanguageDialog
import info.plateaukao.einkbro.viewmodel.TRANSLATE_API
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TranslateDialogFragment(
    private val translationViewModel: TranslationViewModel,
    private val webView: WebView,
    private val anchorPoint: Point? = null,
    private val closeAction: (() -> Unit)? = null,
) : DraggableComposeDialogFragment() {

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            TranslateResponse(
                translationViewModel,
                showExtraIcons = config.papagoApiSecret.isNotBlank(),
                this::changeTranslationLanguage,
                this::getTranslationWebView,
                closeAction ?: { dismiss() }
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        translationViewModel.cancel()
        closeAction?.invoke()
    }

    private fun getTranslationWebView() = webView

    private fun changeTranslationLanguage() {
        lifecycleScope.launch {
            val translationLanguage =
                TranslationLanguageDialog(requireActivity()).show() ?: return@launch
            translationViewModel.updateTranslationLanguageAndGo(translationLanguage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        anchorPoint?.let { setupDialogPosition(it) }

        //dialog?.window?.setBackgroundDrawableResource(R.drawable.white_bgd_with_border_margin)

        translationViewModel.translate()

        return view
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TranslateResponse(
    translationViewModel: TranslationViewModel,
    showExtraIcons: Boolean,
    onTargetLanguageClick: () -> Unit,
    getTranslationWebView: () -> WebView,
    closeClick: () -> Unit,
) {
    val iconSize = 40.dp
    val iconPadding = 5.dp
    val requestMessage by translationViewModel.inputMessage.collectAsState()
    val responseMessage by translationViewModel.responseMessage.collectAsState()
    val rotateScreen by translationViewModel.rotateResultScreen.collectAsState()
    val showRequest = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(top = 6.dp, start = 6.dp, end = 6.dp)
            .run {
                if (rotateScreen) {
                    width(400.dp)
                        .height(400.dp)
                        .rotate(-90f)
                } else {
                    wrapContentWidth()
                        .height(IntrinsicSize.Max)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .wrapContentHeight()
                .wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "Copy text",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(iconSize)
                    .padding(iconPadding)
                    .clickable { ShareUtil.copyToClipboard(context, responseMessage.text) }
            )

            if (ViewUnit.isTablet(LocalContext.current)) {
                GptRow(translationViewModel)
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_translate),
                contentDescription = "Deepl Translate",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(iconSize)
                    .padding(iconPadding)
                    .combinedClickable(
                        onClick = {
                            translationViewModel.translate(TRANSLATE_API.DEEPL)
                        },
                        onLongClick = { onTargetLanguageClick() }
                    )
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_translate_google),
                contentDescription = "Google Translate",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(iconSize)
                    .padding(iconPadding)
                    .combinedClickable(
                        onClick = {
                            translationViewModel.translate(TRANSLATE_API.GOOGLE)
                        },
                        onLongClick = { onTargetLanguageClick() }
                    )
            )
            if (showExtraIcons) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_papago),
                    contentDescription = "Papago Translate Icon",
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .size(iconSize)
                        .padding(iconPadding)
                        .combinedClickable(
                            onClick = {
                                translationViewModel.translate(TRANSLATE_API.PAPAGO)
                            },
                            onLongClick = { onTargetLanguageClick() }
                        )
                )
                Icon(
                    painter = painterResource(id = R.drawable.icon_search),
                    contentDescription = "Naver dict icon",
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .size(iconSize)
                        .padding(iconPadding)
                        .clickable {
                            translationViewModel.translate(TRANSLATE_API.NAVER)
                        }
                )
            }
            Icon(
                painter = painterResource(
                    id = if (showRequest.value) R.drawable.icon_arrow_up_gest else R.drawable.icon_info
                ),
                contentDescription = "Info Icon",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(iconSize)
                    .padding(10.dp)
                    .combinedClickable(
                        onClick = { showRequest.value = !showRequest.value },
                        onLongClick = { translationViewModel.updateRotateResultScreen(!rotateScreen) }
                    )
            )
            Icon(
                painter = painterResource(id = R.drawable.icon_close),
                contentDescription = "Close Icon",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(iconSize)
                    .padding(iconPadding)
                    .clickable { closeClick() }
            )
        }
        if (!ViewUnit.isTablet(LocalContext.current)) {
            GptRow(
                translationViewModel,
                modifier = Modifier.align(Alignment.End),
            )
        }
        Column(
            modifier = Modifier
                .defaultMinSize(minWidth = 300.dp)
                .wrapContentHeight()
                .width(IntrinsicSize.Max)
                .weight(1f)
                .align(Alignment.Start)
                .conditionalScroll(
                    !translationViewModel.isWebViewStyle(),
                    scrollState
                ),
            horizontalAlignment = Alignment.End
        ) {
            if (showRequest.value) {
                Text(
                    text = requestMessage,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Divider()
            }
            if (translationViewModel.isWebViewStyle() && responseMessage.text != "...") {
                WebResultView(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    getTranslationWebView(),
                    responseMessage.text
                )
            } else {
                Text(
                    text = responseMessage,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }
        RoundedDragBar()
    }
}

@Composable
private fun GptRow(
    translationViewModel: TranslationViewModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        val context = LocalContext.current
        ActionMenuItem(
            "",
            context.getDrawable(R.drawable.icon_menu_save),
            onClicked = {
                translationViewModel.saveTranslationResult()
            }
        )
        translationViewModel.getGptActionList().mapIndexed { index, gptActionInfo ->
            ActionMenuItem(
                gptActionInfo.name,
                null,
                onClicked = {
                    translationViewModel.gptActionInfo = gptActionInfo
                    translationViewModel.translate(TRANSLATE_API.GPT)
                },
                onLongClicked = { translationViewModel.showEditGptActionDialog(index) }
            )
        }
    }
}

@Composable
fun RoundedDragBar(width: Dp = 100.dp) {
    Box(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(4.dp)
                .align(Alignment.Center)
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(50) // Use a high value to ensure fully rounded corners
                )
        )
    }
}

@Composable
private fun WebResultView(modifier: Modifier, webView: WebView, webContent: String) {
    AndroidView(
        factory = { webView },
        modifier = modifier
            .height(400.dp)
            .width(500.dp)
    )

    LaunchedEffect(webContent) {
        delay(1)
        val headers = HashMap<String, String>().apply { put("accept-language", "zh-TW,zh") }
        webView.loadUrl(webContent, headers)
    }
}

@Preview
@Composable
fun PreviewRoundedDragBar() {
    RoundedDragBar()
}

@Preview
@Composable
fun PreviewTranslateResponse() {
    val context = LocalContext.current
    MyTheme {
        TranslateResponse(
            translationViewModel = TranslationViewModel(),
            showExtraIcons = true,
            onTargetLanguageClick = {},
            getTranslationWebView = { WebView(context) },
            closeClick = {},
        )
    }
}

private fun Modifier.conditionalScroll(applyScroll: Boolean, scrollState: ScrollState): Modifier =
    this.then(
        if (applyScroll) Modifier.verticalScroll(scrollState) else Modifier
    )