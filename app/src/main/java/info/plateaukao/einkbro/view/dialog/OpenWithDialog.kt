package info.plateaukao.einkbro.view.dialog

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.view.compose.MyTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * In-process replacement for [Intent.createChooser]. The system chooser
 * launches a separate activity and on multi-window devices (notably
 * Supernote, which keeps a launcher pinned to one half of the screen)
 * that activity ends up in the secondary split slot. Rendering the picker
 * as a dialog inside the calling activity sidesteps that.
 */
class OpenWithDialog(
    private val activity: Activity,
    private val viewIntent: Intent,
    private val title: String,
    private val extraTargets: List<Target> = emptyList(),
) : KoinComponent {

    private val config: ConfigManager by inject()

    data class Target(val label: String, val icon: Drawable?, val intent: Intent)

    fun show() {
        val targets = buildTargets()
        if (targets.isEmpty()) return
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            activity.runOnUiThread { showOnMain(targets) }
        } else {
            showOnMain(targets)
        }
    }

    private fun showOnMain(targets: List<Target>) {
        val composeView = ComposeView(activity)

        val dialog = AlertDialog.Builder(activity, R.style.TouchAreaDialog)
            .setView(composeView)
            .create()
            .apply {
                window?.setGravity(if (config.ui.isToolbarOnTop) Gravity.CENTER else Gravity.BOTTOM)
                window?.setBackgroundDrawableResource(R.drawable.background_with_border_margin)
                window?.decorView?.setViewTreeLifecycleOwner(activity as LifecycleOwner)
                window?.decorView?.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
            }

        composeView.setContent {
            MyTheme {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onBackground,
                    )
                    LazyColumn {
                        items(targets, key = { it.intent.component?.flattenToShortString() ?: it.label }) { target ->
                            TargetRow(
                                label = target.label,
                                iconPainter = target.icon?.let { rememberDrawablePainter(it) },
                            ) {
                                dialog.dismiss()
                                runCatching { activity.startActivity(target.intent) }
                            }
                        }
                    }
                    Text(
                        text = stringResource(android.R.string.cancel),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dialog.dismiss() }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        color = MaterialTheme.colors.onBackground,
                    )
                }
            }
        }

        dialog.show()
    }

    private fun buildTargets(): List<Target> {
        val pm = activity.packageManager
        val resolved = pm.queryIntentActivities(viewIntent, 0).map { info ->
            val launch = Intent(viewIntent).apply {
                component = android.content.ComponentName(
                    info.activityInfo.packageName,
                    info.activityInfo.name,
                )
            }
            Target(
                label = info.loadLabel(pm).toString(),
                icon = info.loadIcon(pm),
                intent = launch,
            )
        }
        // Extras first so they're easy to spot (e.g. Supernote entry).
        return extraTargets + resolved
    }
}

@Composable
private fun TargetRow(
    label: String,
    iconPainter: Painter?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        if (iconPainter != null) {
            androidx.compose.foundation.Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = label,
            modifier = Modifier
                .weight(1F)
                .padding(start = 16.dp),
            fontSize = 15.sp,
            color = MaterialTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
