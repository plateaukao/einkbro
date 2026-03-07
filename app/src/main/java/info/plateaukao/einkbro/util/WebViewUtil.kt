package info.plateaukao.einkbro.util

import android.content.Context
import android.content.pm.PackageManager

object WebViewUtil {
    // This snippet is borrow from mihon
    // https://github.com/mihonapp/mihon/blob/81871a34694c8e408d907731292b7266c5b993cc/core/common/src/main/kotlin/eu/kanade/tachiyomi/util/system/WebViewUtil.kt#L14
    private const val CHROME_PACKAGE = "com.android.chrome"
    private const val SYSTEM_SETTINGS_PACKAGE = "com.android.settings"

    fun spoofedPackageName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(CHROME_PACKAGE, PackageManager.GET_META_DATA)

            CHROME_PACKAGE
        } catch (_: PackageManager.NameNotFoundException) {
            SYSTEM_SETTINGS_PACKAGE
        }
    }
    // end snippet
}