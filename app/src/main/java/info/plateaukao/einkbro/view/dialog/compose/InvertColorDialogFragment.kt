package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R

class InvertColorDialogFragment(
    private val onApply: (Boolean) -> Unit,
) : ComposeDialogFragment() {
    @Composable
    override fun Content() {
        var checked by remember { mutableStateOf(config.display.invertColorKeepImages) }
        val toggle: () -> Unit = {
            val next = !checked
            checked = next
            config.display.invertColorKeepImages = next
            onApply(next)
            dialog?.dismiss()
        }
        Row(
            modifier = Modifier
                .clickable { toggle() }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemedCheckbox(
                checked = checked,
                onCheckedChange = { toggle() },
            )
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
                tint = MaterialTheme.colors.onBackground,
            )
            Text(
                text = stringResource(id = R.string.invert_color_keep_images),
                fontSize = 16.sp,
                color = MaterialTheme.colors.onBackground,
                softWrap = false,
            )
        }
    }
}
