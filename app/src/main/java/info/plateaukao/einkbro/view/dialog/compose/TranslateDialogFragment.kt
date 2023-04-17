package info.plateaukao.einkbro.view.dialog.compose

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import info.plateaukao.einkbro.viewmodel.TranslationViewModel
import kotlinx.coroutines.launch

class TranslateDialogFragment(
    private val translationViewModel: TranslationViewModel,
    private val anchorPoint: Point,
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            TranslateResponse(translationViewModel) {
                changeTranslationLanguage()
            }
        }
    }

    private fun changeTranslationLanguage() {
        lifecycleScope.launch {
            val translationLanguage =
                TranslationLanguageDialog(requireActivity()).show() ?: return@launch
            translationViewModel.updateTranslationLanguage(translationLanguage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setupDialogPosition(anchorPoint)

        translationViewModel.query()
        return view
    }

    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var initialX: Int = 0
    private var initialY: Int = 0

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDialogPosition(position: Point) {
        val window = dialog?.window ?: return
        window.setGravity(Gravity.TOP or Gravity.LEFT)

        if (position.isValid()) {
            val params = window.attributes.apply {
                x = position.x
                y = position.y
            }
            window.attributes = params
        }

        supportDragToMove(window)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun supportDragToMove(window: Window) {
        val windowManager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        window.decorView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Get the initial touch position and dialog window position
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialX = window.attributes.x
                    initialY = window.attributes.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    // Calculate the new position of the dialog window
                    val newX = initialX + (event.rawX - initialTouchX).toInt()
                    val newY = initialY + (event.rawY - initialTouchY).toInt()

                    // Update the position of the dialog window
                    window.attributes.x = newX
                    window.attributes.y = newY
                    windowManager.updateViewLayout(window.decorView, window.attributes)
                    true
                }

                else -> false
            }
        }
    }

    private fun Point.isValid() = x != 0 && y != 0
}

@Composable
private fun TranslateResponse(
    translationViewModel: TranslationViewModel,
    onLanguageClick: () -> Unit
) {
    val requestMessage by translationViewModel.inputMessage.collectAsState()
    val responseMessage by translationViewModel.responseMessage.collectAsState()
    val targetLanguage by translationViewModel.translationLanguage.collectAsState()
    val showRequest = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectableText(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                selected = true,
                text = targetLanguage.language,
                textAlign = TextAlign.Center,
                onClick = onLanguageClick
            )
            Icon(
                painter = painterResource(
                    id = if (showRequest.value) R.drawable.icon_arrow_up_gest else R.drawable.icon_arrow_down_gest
                ),
                contentDescription = "Info Icon",
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { showRequest.value = !showRequest.value }
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
