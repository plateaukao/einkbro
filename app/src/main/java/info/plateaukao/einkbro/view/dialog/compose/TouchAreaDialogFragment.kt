package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.SettingRoute
import info.plateaukao.einkbro.preference.TouchAreaType
import info.plateaukao.einkbro.preference.TouchAreaType.BottomLeftRight
import info.plateaukao.einkbro.preference.TouchAreaType.Left
import info.plateaukao.einkbro.preference.TouchAreaType.MiddleLeftRight
import info.plateaukao.einkbro.preference.TouchAreaType.Ebook
import info.plateaukao.einkbro.preference.TouchAreaType.Right
import info.plateaukao.einkbro.preference.toggle
import info.plateaukao.einkbro.unit.IntentUnit
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent

class TouchAreaDialogFragment : ComposeDialogFragment(), KoinComponent {
    override fun setupComposeView() = composeView.setContent {
        val touchAreaType = remember { mutableStateOf(config.touch.touchAreaType) }
        val enableTurn = remember { mutableStateOf(config.touch.enableTouchTurn) }

        MyTheme {
            TouchAreaContent(
                touchAreaType = touchAreaType.value,
                onTouchTypeClick = { type ->
                    config.touch.touchAreaType = type
                    touchAreaType.value = type
                },
                enableTurn = enableTurn.value,
                onToggle = {
                    config.touch::enableTouchTurn.toggle()
                    enableTurn.value = config.touch.enableTouchTurn
                },
                onCloseClick = { dismiss() },
                onConfigActionsClick = {
                    IntentUnit.gotoSettings(requireActivity(), SettingRoute.Gesture)
                }
            )
        }
    }
}

@Composable
fun TouchAreaContent(
    touchAreaType: TouchAreaType,
    onTouchTypeClick: (TouchAreaType) -> Unit,
    enableTurn: Boolean = true,
    onToggle: () -> Unit = {},
    onCloseClick: () -> Unit = {},
    onConfigActionsClick: () -> Unit = {},
) {
    Column(Modifier.width(300.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TouchAreaItem(
                state = touchAreaType == Left,
                iconResId = R.drawable.ic_touch_left
            ) { onTouchTypeClick(Left) }
            TouchAreaItem(
                state = touchAreaType == MiddleLeftRight,
                iconResId = R.drawable.ic_touch_middle_left_right
            ) { onTouchTypeClick(MiddleLeftRight) }
            TouchAreaItem(
                state = touchAreaType == Right,
                iconResId = R.drawable.ic_touch_right
            ) { onTouchTypeClick(Right) }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TouchAreaItem(
                state = touchAreaType == BottomLeftRight,
                iconResId = R.drawable.ic_touch_left_right
            ) { onTouchTypeClick(BottomLeftRight) }
            TouchAreaItem(
                state = touchAreaType == TouchAreaType.Long,
                iconResId = R.drawable.ic_touch_long
            ) { onTouchTypeClick(TouchAreaType.Long) }
            TouchAreaItem(
                state = touchAreaType == Ebook,
                iconResId = R.drawable.ic_touch_ebook
            ) { onTouchTypeClick(Ebook) }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ActionItem(
            R.string.touch_aciton_settings,
            onConfigActionsClick,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                modifier = Modifier
                    .padding(10.dp)
                    .weight(1f),
                text = stringResource(R.string.dialog_touch_area_enable),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Start,
            )
            Switch(
                modifier = Modifier
                    .wrapContentHeight(),
                checked = enableTurn,
                onCheckedChange = {
                    onToggle()
                },
            )
        }

        HorizontalSeparator()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = onCloseClick
            ) {
                Text(
                    stringResource(id = android.R.string.ok),
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}

@Composable
fun TouchAreaItem(
    state: Boolean,
    iconResId: Int,
    onClicked: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderWidth = if (state) 4.dp else -1.dp
    Column(
        modifier = Modifier
            .padding(5.dp)
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) {
                onClicked()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier
                .height(80.dp)
                .border(borderWidth, MaterialTheme.colors.onBackground),
            tint = MaterialTheme.colors.onBackground
        )
    }
}

// action item with an arrow on right side
@Composable
fun ActionItem(
    titleResId: Int,
    onClicked: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) {
                onClicked()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = titleResId),
            fontSize = 18.sp,
            color = MaterialTheme.colors.onBackground,
        )
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.icon_arrow_right_gest),
            contentDescription = null,
            tint = MaterialTheme.colors.onBackground
        )
    }
}

@Preview(
    name = "default",
    showBackground = true,
)
@Composable
fun PreviewTouchAreaContent() {
    MaterialTheme {
        TouchAreaContent(
            touchAreaType = Left,
            onTouchTypeClick = {},
        )
    }
}