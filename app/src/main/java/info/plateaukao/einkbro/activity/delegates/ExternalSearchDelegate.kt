package info.plateaukao.einkbro.activity.delegates

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.activity.BrowserState
import info.plateaukao.einkbro.viewmodel.ExternalSearchViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class ExternalSearchDelegate(
    private val activity: FragmentActivity,
    private val state: BrowserState,
    private val externalSearchViewModel: ExternalSearchViewModel,
) {
    @SuppressLint("UseCompatLoadingForDrawables")
    fun init() {
        val mainContent = state.binding.activityMainContent
        mainContent.externalSearchClose.setOnClickListener {
            activity.moveTaskToBack(true)
            externalSearchViewModel.setButtonVisibility(false)
        }
        val externalSearchContainer = mainContent.externalSearchActionContainer
        externalSearchViewModel.searchActions.forEach { action ->
            val button = TextView(activity).apply {
                height = 40.dp.value.toInt()
                textSize = 10.sp.value
                gravity = Gravity.CENTER
                background = activity.getDrawable(R.drawable.background_with_border)
                text = action.title.take(2).uppercase(Locale.getDefault())
                setOnClickListener {
                    externalSearchViewModel.currentSearchAction = action
                    state.ebWebView.loadUrl(
                        externalSearchViewModel.generateSearchUrl(splitSearchItemInfo = action),
                    )
                }
            }
            externalSearchContainer.addView(button, 0)
        }
        activity.lifecycleScope.launch {
            externalSearchViewModel.showButton.collect { show ->
                externalSearchContainer.visibility = if (show) VISIBLE else INVISIBLE
            }
        }
    }
}
