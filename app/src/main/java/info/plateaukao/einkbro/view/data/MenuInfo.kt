package info.plateaukao.einkbro.view.data

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

data class MenuInfo(
    val title: String,
    val icon: Drawable? = null,
    val intent: Intent? = null,
    val closeMenu: Boolean = true,
    val action: (() -> Unit)? = null
)

fun ResolveInfo.toMenuInfo(pm: PackageManager): MenuInfo {
    val title = loadLabel(pm).toString()
    val icon = loadIcon(pm)
    val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
        setClassName(activityInfo.packageName, activityInfo.name)
    }
    return MenuInfo(title, icon, intent)
}