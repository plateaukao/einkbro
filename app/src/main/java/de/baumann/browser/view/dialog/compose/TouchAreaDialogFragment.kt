package de.baumann.browser.view.dialog.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.baumann.browser.view.compose.MyTheme
import de.baumann.browser.Ninja.R
import de.baumann.browser.preference.TouchAreaType
import de.baumann.browser.preference.TouchAreaType.*
import org.koin.core.component.KoinComponent

class TouchAreaDialogFragment : ComposeDialogFragment(), KoinComponent {
    override fun setupComposeView() = composeView.setContent {
        val touchAreaType  = remember { mutableStateOf(config.touchAreaType) }
        MyTheme {
            TouchAreaContent(
                touchAreaType = touchAreaType.value,
                onTouchTypeClick = { type ->
                    config.touchAreaType = type
                    touchAreaType.value = type
                },
                onAction = { config.enableTouchTurn = true; dismiss() },
                offAction = { config.enableTouchTurn = false ; dismiss() },
                showHint = config.touchAreaHint,
                hideTouchWhenType = config.hideTouchAreaWhenInput,
                switchTouchArea = config.switchTouchAreaAction,
                onShowHintClick = { config.touchAreaHint = !config.touchAreaHint },
                onHideWhenTypeClick = { config.hideTouchAreaWhenInput = !config.hideTouchAreaWhenInput },
                onSwitchAreaClick = { config.switchTouchAreaAction = !config.switchTouchAreaAction },
            )
        }
    }
}

@Composable
fun TouchAreaContent(
    touchAreaType: TouchAreaType,
    onTouchTypeClick: (TouchAreaType) -> Unit,
    onAction: () -> Unit,
    offAction: () -> Unit,
    showHint: Boolean = true,
    hideTouchWhenType: Boolean = true,
    switchTouchArea: Boolean = false,
    onShowHintClick: () -> Unit = {},
    onHideWhenTypeClick: () -> Unit = {},
    onSwitchAreaClick: () -> Unit = {},
) {
    Column(Modifier.width(300.dp)) {
        Text(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.title_touch_area),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            TouchAreaItem(state = touchAreaType == Left, titleResId = R.string.touch_left_side, iconResId = R.drawable.ic_touch_left) { onTouchTypeClick(Left) }
            TouchAreaItem(state = touchAreaType == Right, titleResId = R.string.touch_right_side, iconResId = R.drawable.ic_touch_right) { onTouchTypeClick(Right) }
        }
        Row(Modifier.fillMaxWidth( ), horizontalArrangement = Arrangement.SpaceAround) {
            TouchAreaItem(state = touchAreaType == MiddleLeftRight, titleResId = R.string.middle, iconResId = R.drawable.ic_touch_middle_left_right) { onTouchTypeClick(MiddleLeftRight) }
            TouchAreaItem(state = touchAreaType == BottomLeftRight, titleResId = R.string.bottom, iconResId = R.drawable.ic_touch_left_right) { onTouchTypeClick(BottomLeftRight) }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ToggleItem(state = showHint, titleResId = R.string.show_touch_area_hint, iconResId = -1, onClicked = { onShowHintClick() } )
        ToggleItem(state = hideTouchWhenType, titleResId = R.string.hie_touch_area_when_input, iconResId = -1, onClicked = { onHideWhenTypeClick() } )
        ToggleItem(state = switchTouchArea, titleResId = R.string.switch_touch_area_action, iconResId = -1, onClicked ={ onSwitchAreaClick() } )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalSeparator()

        Row(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick = offAction) {
                Text(stringResource(id = R.string.turn_off), color = MaterialTheme.colors.onBackground)
            }
            VerticalSeparator()
            TextButton(
                modifier = Modifier.wrapContentWidth(),
                onClick = onAction) {
                Text(stringResource(id = R.string.turn_on), color = MaterialTheme.colors.onBackground)
            }
        }
    }
}

@Composable
fun TouchAreaItem(
    state: Boolean,
    titleResId: Int,
    iconResId: Int,
    onClicked: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderWidth = if (state) 4.dp else -1.dp
    Column (
        modifier = Modifier
            .padding(15.dp)
            .clickable(
                indication = null,
                interactionSource = interactionSource) {
                onClicked()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ){
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier .border(borderWidth, MaterialTheme.colors.onBackground),
            tint = MaterialTheme.colors.onBackground
        )
        Text(
            modifier = Modifier.padding(top = 10.dp),
            text = stringResource(id = titleResId),
            fontSize = 13.sp,
            color = MaterialTheme.colors.onBackground,
            fontWeight = if (state) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Preview(
    name  = "default",
    showSystemUi = true,
    showBackground = true, device = "spec:shape=Normal,width=1080,height=2340,unit=px,dpi=440",
)
@Composable
fun PreviewTouchAreaContent() {
    MaterialTheme {
        TouchAreaContent(
            touchAreaType = Left,
            onTouchTypeClick = { },
            onAction = {},
            offAction = {}
        )
    }
}