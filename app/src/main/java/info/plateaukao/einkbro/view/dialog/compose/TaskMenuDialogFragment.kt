package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.task.TaskDescriptor
import info.plateaukao.einkbro.view.compose.MyTheme

class TaskMenuDialogFragment(
    private val descriptors: List<TaskDescriptor>,
    private val onTemplateClicked: (TaskDescriptor) -> Unit,
    private val onCustomClicked: () -> Unit,
) : ComposeDialogFragment() {

    init {
        shouldShowInCenter = false
    }

    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.task_menu_title),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    descriptors.forEach { descriptor ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTemplateClicked(descriptor)
                                    composeView.post { dismiss() }
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(descriptor.displayNameResId),
                                style = MaterialTheme.typography.subtitle1,
                                color = MaterialTheme.colors.onBackground,
                            )
                            Text(
                                text = stringResource(descriptor.descriptionResId),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onBackground,
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colors.onBackground)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCustomClicked()
                                composeView.post { dismiss() }
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.task_custom),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onBackground,
                        )
                        Text(
                            text = stringResource(R.string.task_custom_desc),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onBackground,
                        )
                    }
                }
            }
        }
    }
}
