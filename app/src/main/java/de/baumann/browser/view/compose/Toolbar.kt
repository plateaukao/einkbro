@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package de.baumann.browser.view.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.appcompattheme.AppCompatTheme
import de.baumann.browser.view.toolbaricons.ToolbarAction


@Composable
fun ComposedToolbar(
    toolbarActions: List<ToolbarAction>,
    title: String,
    onClick: (ToolbarAction)->Unit,
    onLongClick:((ToolbarAction)->Unit)? = null,
) {
    Row(
        modifier = Modifier.height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        toolbarActions.forEach { toolbarAction ->
            if (toolbarAction == ToolbarAction.Title) {
                Text(
                    modifier = Modifier.weight(1F),
                    text = "Hello"
                )
            }
            Icon(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(46.dp)
                    .padding(8.dp)
                    .combinedClickable (
                        onClick = { onClick(toolbarAction) },
                        onLongClick = { onLongClick?.invoke(toolbarAction)}
                    ),
                painter = painterResource(id = toolbarAction.iconResId), contentDescription = null,
                tint = MaterialTheme.colors.onBackground
            )
        }
    }
}

@Preview
@Composable
fun previewToolbar() {
    AppCompatTheme {
        ComposedToolbar(
            toolbarActions = ToolbarAction.values().toList(),
            "hihi",
            {_-> },
        )
    }
}