package info.plateaukao.einkbro.view.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SelectableText(
    modifier: Modifier,
    selected: Boolean,
    text: String,
    isEnabled: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderWidth = if (selected) 1.dp else -1.dp
    Text(
        text = text,
        color = MaterialTheme.colors.onBackground.copy(alpha = if (isEnabled) 1f else 0.5f),
        style = MaterialTheme.typography.button,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) {
                if (isEnabled) onClick()
            }
    )
}