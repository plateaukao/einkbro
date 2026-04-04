package info.plateaukao.einkbro.view.data

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.vector.ImageVector

data class MenuInfo(
    val title: String,
    val drawable: Drawable? = null, // for data from resolveInfo
    val imageVector: ImageVector? = null, // for other locally created data
    val intent: Intent? = null,
    val closeMenu: Boolean = true,
    val action: (() -> Unit)? = null,
    val longClickAction: (() -> Unit)? = null,
    val cornerDrawable: Drawable? = null, // optional small icon at bottom-right corner (e.g. GPT type)
)

fun ResolveInfo.toMenuInfo(pm: PackageManager): MenuInfo {
    val title = loadLabel(pm).toString()
    val icon = loadIcon(pm)
    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
        setClassName(activityInfo.packageName, activityInfo.name)
    }
    return MenuInfo(title, icon, null, intent)
}