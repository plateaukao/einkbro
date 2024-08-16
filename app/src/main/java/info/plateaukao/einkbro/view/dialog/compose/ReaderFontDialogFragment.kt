package info.plateaukao.einkbro.view.dialog.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.view.compose.MyTheme

class ReaderFontDialogFragment(
    private val onFontCustomizeClick: () -> Unit
) : ComposeDialogFragment() {
    private val customFontNameState: MutableState<String> =
        mutableStateOf(config.readerCustomFontInfo?.name.orEmpty())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        config.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == ConfigManager.K_READER_CUSTOM_FONT) {
                customFontNameState.value = config.readerCustomFontInfo?.name.orEmpty()
            }
        }
        return view
    }

    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                val customFontName =
                    remember { customFontNameState }
                MainFontDialog(
                    selectedFontSizeValue = config.readerFontSize,
                    selectedFontType = config.readerFontType,
                    customFontName = customFontName.value,
                    onFontSizeClick = {
                        config.readerFontSize = it
                        dismiss()
                    },
                    onFontTypeClick = {
                        if (it == FontType.CUSTOM && config.readerCustomFontInfo == null) {
                            onFontCustomizeClick()
                        } else {
                            config.readerFontType = it
                            dismiss()
                        }
                    },
                    onFontCustomizeClick = { onFontCustomizeClick() },
                    okAction = { dismiss() },
                )
            }
        }
    }
}