package info.plateaukao.einkbro.view.dialog.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import info.plateaukao.einkbro.preference.ConfigManager
import info.plateaukao.einkbro.preference.FontType
import info.plateaukao.einkbro.view.compose.MyTheme

class ReaderFontDialogFragment(
    private val onFontCustomizeClick: () -> Unit
) : ComposeDialogFragment() {
    override fun setupComposeView() {
        composeView.setContent {
            MyTheme {
                val customFontName =
                    remember { mutableStateOf(config.readerCustomFontInfo?.name ?: "") }
                config.registerOnSharedPreferenceChangeListener { _, key ->
                    if (key == ConfigManager.K_READER_CUSTOM_FONT) {
                        customFontName.value = config.readerCustomFontInfo?.name ?: ""
                    }
                }
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
                    onFontCustomizeClick = {
                        onFontCustomizeClick()
                        config.registerOnSharedPreferenceChangeListener { _, key ->
                            if (key == ConfigManager.K_READER_CUSTOM_FONT) {
                                customFontName.value = config.readerCustomFontInfo?.name ?: ""
                            }
                        }
                    },
                    okAction = { dismiss() },
                )
            }
        }
    }
}