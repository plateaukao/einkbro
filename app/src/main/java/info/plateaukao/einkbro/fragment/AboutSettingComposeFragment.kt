package info.plateaukao.einkbro.fragment

import android.content.Intent
import android.content.Intent.EXTRA_TEXT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserActivity
import info.plateaukao.einkbro.view.compose.MyTheme

class AboutSettingComposeFragment : Fragment(), FragmentTitleInterface {

    override fun getTitleId(): Int = R.string.title_about

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
            .apply {
                setContent {
                    MyTheme {
                        SettingsMainContent(
                            AboutSettingItem.values().toList(),
                            onItemClick = { handleLink(it.url) },
                        )
                    }
                }
            }
    }

    private fun handleLink(url: String) {
        requireContext().startActivity(
            Intent(requireContext(), BrowserActivity::class.java).apply {
                action = Intent.ACTION_SEND
                putExtra(EXTRA_TEXT, url)
            }
        )
        requireActivity().finish()
    }
}

private enum class AboutSettingItem(
    override val titleResId: Int,
    override val iconId: Int,
    val url: String,
    override val summaryResId: Int = 0,
) : SettingItemInterface {
    ProjectSite(R.string.project_site, R.drawable.ic_home, "https://github.com/plateaukao/browser"),
    LatestRelease(
        R.string.latest_release,
        R.drawable.icon_earth,
        "https://github.com/plateaukao/browser/releases"
    ),
    Facebook(R.string.twitter, R.drawable.icon_earth, "https://twitter.com/einkbro"),
    ChangeLogs(
        R.string.changelogs,
        R.drawable.icon_earth,
        "https://github.com/plateaukao/browser/blob/main/CHANGELOG.md"
    ),
    Contributors(
        R.string.contributors,
        R.drawable.icon_copyright,
        "https://github.com/plateaukao/browser/blob/main/CONTRIBUTORS.md"
    ),
    Medium(R.string.medium_articles, R.drawable.ic_reader, "https://medium.com/einkbro")
}
