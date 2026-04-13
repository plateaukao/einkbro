package info.plateaukao.einkbro.view.statusbar

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.preference.TouchConfig
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val iconSize = 16.dp
private val barHeight = 22.dp

@Composable
fun Statusbar(
    items: List<StatusbarItem>,
    pageInfo: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        items.forEach { item ->
            when (item) {
                StatusbarItem.Time -> StatusbarTime()
                StatusbarItem.PageInfo -> StatusbarPageInfo(pageInfo)
                StatusbarItem.Battery -> StatusbarBattery()
                StatusbarItem.Wifi -> StatusbarWifi()
                StatusbarItem.TouchPagination -> StatusbarTouchPagination()
                StatusbarItem.VolumePagination -> StatusbarVolumePagination()
            }
        }
    }
}

@Composable
private fun StatusbarTime() {
    var now by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(60_000L)
        }
    }
    StatusbarText(now)
}

@Composable
private fun StatusbarPageInfo(pageInfo: String) {
    if (pageInfo.isBlank()) return
    StatusbarText(pageInfo)
}

@Composable
private fun StatusbarText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colors.onBackground,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusbarBattery() {
    val context = LocalContext.current
    var percent by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        while (true) {
            percent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            delay(60_000L)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.BatteryFull,
            contentDescription = null,
            tint = MaterialTheme.colors.onBackground,
            modifier = Modifier.size(iconSize),
        )
        percent?.let {
            Text(
                text = "$it%",
                color = MaterialTheme.colors.onBackground,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

@Composable
private fun StatusbarWifi() {
    val context = LocalContext.current
    var isWifi by remember { mutableStateOf(isWifiConnected(context)) }
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isWifi = isWifiConnected(context) }
            override fun onLost(network: Network) { isWifi = isWifiConnected(context) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                isWifi = isWifiConnected(context)
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        runCatching { cm?.registerNetworkCallback(request, callback) }
        onDispose { runCatching { cm?.unregisterNetworkCallback(callback) } }
    }
    Icon(
        imageVector = if (isWifi) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground,
        modifier = Modifier.size(iconSize),
    )
}

private fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val active = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(active) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

@Composable
private fun StatusbarTouchPagination() {
    val context = LocalContext.current
    val enabled = rememberBooleanPref(context, TouchConfig.K_ENABLE_TOUCH, default = false)
    Icon(
        imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(
            id = if (enabled) R.drawable.ic_touch_enabled else R.drawable.ic_touch_disabled
        ),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground,
        modifier = Modifier.size(iconSize),
    )
}

@Composable
private fun StatusbarVolumePagination() {
    val context = LocalContext.current
    val enabled = rememberBooleanPref(context, TouchConfig.K_VOLUME_PAGE_TURN, default = true)
    Icon(
        imageVector = if (enabled) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground,
        modifier = Modifier.size(iconSize),
    )
}

@Composable
private fun rememberBooleanPref(context: Context, key: String, default: Boolean): Boolean {
    @Suppress("DEPRECATION")
    val sp = remember { android.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    var value by remember { mutableStateOf(sp.getBoolean(key, default)) }
    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) value = prefs.getBoolean(key, default)
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}
