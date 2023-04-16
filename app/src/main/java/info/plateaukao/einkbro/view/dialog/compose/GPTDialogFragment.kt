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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.viewmodel.GptViewModel


class GPTDialogFragment(
    private val gptViewModel: GptViewModel,
    private val anchorPoint: Point,
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = true
    }

    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            GptResponse(gptViewModel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setupDialogPosition(anchorPoint)

        gptViewModel.query()
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
private fun GptResponse(gptViewModel: GptViewModel) {
    val requestMessage by gptViewModel.inputMessage.collectAsState()
    val responseMessage by gptViewModel.responseMessage.collectAsState()
    val showRequest = remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .defaultMinSize(minWidth = 200.dp)
                .wrapContentHeight()
                .wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
        if (!showRequest.value) {
            Icon(
                painter = painterResource(id = R.drawable.icon_arrow_down_gest),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = "Info Icon",
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.TopEnd)
                    .clickable { showRequest.value = true }
            )
        }
    }
}

