package info.plateaukao.einkbro.view.dialog.compose

import android.content.DialogInterface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.view.compose.MyTheme

class FontBoldnessDialogFragment(
    private val initBoldness: Int,
    private val okAction: (Int) -> Unit,
    private val onDismissAction: () -> Unit = {},
) : ComposeDialogFragment() {
    override fun setupComposeView() = composeView.setContent {
        MyTheme {
            FontBoldnessContent(
                initBoldness,
            ) { fontBoldness -> okAction(fontBoldness) }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDismissAction()
        super.onDismiss(dialog)
    }
}

@Composable
fun FontBoldnessContent(
    fontBoldness: Int,
    onFontBoldnessChanged: (Int) -> Unit,
) {
    val values = listOf(500f, 600f, 700f, 800f, 900f)
    var sliderPosition by remember { mutableFloatStateOf(findClosestIndex(fontBoldness, values)) }
    val progressValue = values[sliderPosition.toInt()]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Value: $progressValue")

        Slider(
            value = sliderPosition,
            valueRange = 0f..(values.size - 1).toFloat(),
            steps = values.size - 2,
            modifier = Modifier.padding(16.dp),
            onValueChange = {
                sliderPosition = it
                onFontBoldnessChanged(progressValue.toInt())
            },
        )
    }
}

private fun findClosestIndex(target: Int, values: List<Float>): Float {
    return values.indexOf(values.minByOrNull { kotlin.math.abs(it - target) }!!).toFloat()
}