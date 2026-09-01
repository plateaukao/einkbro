package info.plateaukao.einkbro.view

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import info.plateaukao.einkbro.R

class MainActivityLayout(
    val root: ConstraintLayout,
    val appBar: FrameLayout,
    val composeIconBar: ComposeView,
    val mainSearchPanel: ComposeView,
    val twoPanelLayout: TwoPaneLayout,
    val activityMainContent: MainContentLayout,
    val inputUrl: ComposeView,
    val contentSeparator: ThemedEdgeBorderView,
    val layoutOverview: ComposeView,
    val statusBar: ComposeView,
    val sideTabBar: ComposeView,
) {
    companion object {
        fun create(context: Context): MainActivityLayout {
            val root = ConstraintLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                fitsSystemWindows = false
            }

            // appBar FrameLayout
            val appBar = FrameLayout(context).apply {
                id = R.id.appBar
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // ComposeView for icon bar
            val composeIconBar = ComposeView(context).apply {
                id = R.id.compose_icon_bar
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            appBar.addView(composeIconBar)

            // ComposeView for search panel
            val mainSearchPanel = ComposeView(context).apply {
                id = R.id.main_search_panel
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            appBar.addView(mainSearchPanel)

            root.addView(appBar)

            // TwoPaneLayout
            val twoPanelLayout = TwoPaneLayout(context).apply {
                id = R.id.two_panel_layout
                layoutParams = ConstraintLayout.LayoutParams(0, 0)
            }

            // Create main content and add it to TwoPaneLayout
            val mainContentLayout = MainContentLayout.create(context)
            mainContentLayout.root.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            twoPanelLayout.addView(mainContentLayout.root)

            root.addView(twoPanelLayout)

            // inputUrl ComposeView
            val inputUrl = ComposeView(context).apply {
                id = R.id.input_url
                layoutParams = ConstraintLayout.LayoutParams(0, 0)
                visibility = View.INVISIBLE
            }
            root.addView(inputUrl)

            // contentSeparator: the toolbar's page-facing edge as a themed
            // border. It overlaps the content flush against the app bar; the
            // toolbar side of the line is filled with the theme background
            // and the page side stays transparent (stamp bites / sketch
            // wobble are die-cut, showing the page through them) — the same
            // semantics as the dialog frames.
            val contentSeparator = ThemedEdgeBorderView(context).apply {
                id = R.id.content_separator
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    (5 * context.resources.displayMetrics.density).toInt(),
                )
            }
            root.addView(contentSeparator)

            // layoutOverview ComposeView
            val layoutOverview = ComposeView(context).apply {
                id = R.id.layout_overview
                layoutParams = ConstraintLayout.LayoutParams(0, 0)
                visibility = View.INVISIBLE
            }
            root.addView(layoutOverview)

            // statusBar ComposeView (shown when toolbar is hidden)
            val statusBar = ComposeView(context).apply {
                id = R.id.status_bar
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                )
                visibility = View.GONE
            }
            root.addView(statusBar)

            // sideTabBar ComposeView: the tab strip for vertical toolbar mode, where
            // the 50dp toolbar column has no room for it. Positioned at runtime by
            // ViewUnit.updateAppbarPosition; GONE for top/bottom toolbars, which keep
            // the strip inside the app bar itself.
            val sideTabBar = ComposeView(context).apply {
                id = R.id.side_tab_bar
                layoutParams = ConstraintLayout.LayoutParams(
                    0,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                )
                visibility = View.GONE
            }
            root.addView(sideTabBar)

            // Apply constraints
            val constraintSet = ConstraintSet()
            constraintSet.clone(root)

            // appBar: bottom to parent bottom
            constraintSet.connect(appBar.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // twoPanelLayout: start/end to parent, top to parent, bottom to appBar top
            constraintSet.connect(twoPanelLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(twoPanelLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(twoPanelLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(twoPanelLayout.id, ConstraintSet.BOTTOM, appBar.id, ConstraintSet.TOP)

            // inputUrl: all edges to parent
            constraintSet.connect(inputUrl.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(inputUrl.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(inputUrl.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(inputUrl.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            // contentSeparator: bottom to appBar top
            constraintSet.connect(contentSeparator.id, ConstraintSet.BOTTOM, appBar.id, ConstraintSet.TOP)

            // layoutOverview: all edges to parent
            constraintSet.connect(layoutOverview.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(layoutOverview.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(layoutOverview.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(layoutOverview.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            // statusBar: full-width; anchored to top by default (position is applied at runtime)
            constraintSet.connect(statusBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(statusBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(statusBar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

            // sideTabBar: anchored to the top edge; the start/end sides are attached to
            // the app bar at runtime, since which side it clears depends on the toolbar.
            constraintSet.connect(sideTabBar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(sideTabBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(sideTabBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            constraintSet.applyTo(root)

            return MainActivityLayout(
                root = root,
                appBar = appBar,
                composeIconBar = composeIconBar,
                mainSearchPanel = mainSearchPanel,
                twoPanelLayout = twoPanelLayout,
                activityMainContent = mainContentLayout,
                inputUrl = inputUrl,
                contentSeparator = contentSeparator,
                layoutOverview = layoutOverview,
                statusBar = statusBar,
                sideTabBar = sideTabBar,
            )
        }
    }
}
