package de.baumann.browser.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.BuildConfig
import de.baumann.browser.Ninja.R
import de.baumann.browser.activity.BrowserActivity
import de.baumann.browser.preference.ConfigManager
import de.baumann.browser.unit.HelperUnit
import de.baumann.browser.view.compose.MyTheme
import de.baumann.browser.view.dialog.PrinterDocumentPaperSizeDialog
import de.baumann.browser.view.dialog.TextInputDialog
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsComposeFragment: Fragment(), KoinComponent {
    private val config: ConfigManager by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val composeView = ComposeView(requireContext())
        val version = "Daniel Kao, v" + BuildConfig.VERSION_NAME
        composeView.setContent {
            MyTheme {
                SettingsMainContent(
                    SettingItemType.values().toList(),
                    //ViewUnit.isWideLayout(requireContext()),
                    true,
                    onItemClick = { handleSettingItem(it) },
                    version = version,
                    onVersionClick = {}
                )
            }
        }
        return composeView
    }

    private fun handleSettingItem(setting: SettingItemType) {
        when(setting) {
            SettingItemType.Ui -> showFragment(UiSettingsFragment())
            SettingItemType.Font -> showFragment(FontSettingsFragment())
            SettingItemType.Gesture -> showFragment(FragmentSettingsGesture())
            SettingItemType.Backup -> showFragment(DataSettingsFragment())
            SettingItemType.PdfSize -> PrinterDocumentPaperSizeDialog(requireContext()).show()
            SettingItemType.StartControl -> showFragment(StartSettingsFragment())
            SettingItemType.ClearControl -> showFragment(ClearDataFragment())
            SettingItemType.Search -> showFragment(SearchSettingsFragment())
            SettingItemType.UserAgent -> lifecycleScope.launch { updateUserAgent() }
            SettingItemType.License -> showLicenseList()
            SettingItemType.About -> showAboutDialog()
        }
    }

    private fun showFragment(fragment: Fragment) {
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    @SuppressLint("CutPasteId")
    private fun showAboutDialog() {
        val dialogView = View.inflate(requireActivity(), R.layout.dialog_text, null).apply {
            findViewById<TextView>(R.id.dialog_title).text = getString(R.string.menu_other_info)
            findViewById<TextView>(R.id.dialog_text).text = HelperUnit.textSpannable(getString(R.string.changelog_dialog))
            findViewById<TextView>(R.id.dialog_text).movementMethod = MyLinkMovementMethod.getInstance()
        }

        val dialog = BottomSheetDialog(requireActivity()).apply {
            setContentView(dialogView)
            show()
        }
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED)
    }

    private fun showLicenseList() {
        val intent = Intent(requireContext(), BrowserActivity::class.java).apply {
            data = Uri.parse("https://github.com/plateaukao/browser/blob/main/CONTRIBUTORS.md")
            action = Intent.ACTION_VIEW
        }
        requireActivity().startActivity(intent)
    }

    private suspend fun updateUserAgent() {
        val newValue = TextInputDialog(
                requireContext(),
                getString(R.string.setting_title_userAgent),
                "",
                config.customUserAgent
            ).show()

        newValue?.let { config.customUserAgent = it }
    }

}

@Composable
private fun SettingsMainContent(
    settings: List<SettingItemType>,
    isWideLayout: Boolean,
    onItemClick: (SettingItemType)->Unit,
    version: String,
    onVersionClick:()->Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier
            .wrapContentHeight()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        columns = GridCells.Fixed(if (isWideLayout) 2 else 1),
    ){
        itemsIndexed(settings) { _, setting ->
            SettingItem(setting, onItemClick)
        }
        item { VersionItem(version = version, onItemClick = onVersionClick) }
    }
}

@Composable
private fun SettingItem(
    setting: SettingItemType,
    onItemClick: (SettingItemType)->Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 3.dp else 1.dp
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(60.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) { onItemClick(setting) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = setting.iconId), contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight(),
            tint = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier
            .width(6.dp)
            .fillMaxHeight())
        Text(
            modifier = Modifier.wrapContentWidth(),
            text = stringResource(id = setting.titleResId),
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Composable
private fun VersionItem(
    version: String,
    onItemClick: ()->Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderWidth = if (pressed) 3.dp else 1.dp
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(60.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) { onItemClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.wrapContentWidth().padding(horizontal = 15.dp),
            text = version,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        )
    }
}

private enum class SettingItemType(val titleResId: Int, val iconId: Int) {
    Ui(R.string.setting_title_ui, R.drawable.icon_ui),
    Font(R.string.setting_title_font, R.drawable.icon_size),
    Gesture(R.string.setting_gestures, R.drawable.gesture_tap),
    Backup(R.string.setting_title_data, R.drawable.icon_backup),
    PdfSize(R.string.setting_title_pdf_paper_size, R.drawable.ic_pdf),
    StartControl(R.string.setting_title_start_control, R.drawable.icon_earth),
    ClearControl(R.string.setting_title_clear_control, R.drawable.icon_delete),
    Search(R.string.setting_title_search, R.drawable.icon_search),
    UserAgent(R.string.setting_title_userAgent, R.drawable.icon_useragent),
    License(R.string.setting_title_license, R.drawable.icon_copyright),
    About(R.string.menu_other_info, R.drawable.icon_info),
}

class MyLinkMovementMethod() : LinkMovementMethod() {
    override fun onTouchEvent(textView: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        // Get the event action
        var action = event.action

        // If action has finished
        if (action == MotionEvent.ACTION_UP) {
            // Locate the area that was pressed
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= textView.totalPaddingLeft
            y -= textView.totalPaddingTop
            x += textView.scrollX
            y += textView.scrollY

            // Locate the URL text
            val layout: Layout = textView.layout
            val line: Int = layout.getLineForVertical(y)
            val off: Int = layout.getOffsetForHorizontal(line, x.toFloat())

            // Find the URL that was pressed
            val link = buffer.getSpans(off, off, URLSpan::class.java)
            // If we've found a URL
            if (link.isNotEmpty()) {
                // Find the URL
                val url = link[0].url
                // If it's a valid URL
                if (url.contains("https") or
                    url.contains("tel") or
                    url.contains("mailto") or
                    url.contains("http") or
                    url.contains("https") or
                    url.contains("www")
                ) {
                    val intent = Intent(textView.context, BrowserActivity::class.java).apply {
                        data = Uri.parse(url)
                    }
                    intent.action = Intent.ACTION_VIEW
                    textView.context.startActivity(intent)
                }
                return true
            }
        }
        return super.onTouchEvent(textView, buffer, event)
    }

    companion object {
        // A new LinkMovementMethod
        private val myLinkMovementMethod = MyLinkMovementMethod()
        fun getInstance(): MovementMethod {
            return myLinkMovementMethod
        }
    }
}

@Preview
@Composable
fun PreviewSettingsMainContent() {
    MyTheme {
        SettingsMainContent(SettingItemType.values().toList(), isWideLayout = true, onItemClick = {}, version = "v1.2.3", {})
    }
}