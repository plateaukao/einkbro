package info.plateaukao.einkbro.preference

import android.print.PrintAttributes
import androidx.compose.ui.graphics.Color
import info.plateaukao.einkbro.R

enum class PaperSize(val sizeString: String, val mediaSize: PrintAttributes.MediaSize) {
    ISO_13("A4 (13\")", PrintAttributes.MediaSize.ISO_A4),
    SIZE_10("A5 (10\")", PrintAttributes.MediaSize.ISO_A5),
    ISO_67("Hisense A7 (6.7\")", PrintAttributes.MediaSize.PRC_5),
    SIZE_8("C6 (8\")", PrintAttributes.MediaSize.ISO_C6),
}

enum class FabPosition {
    Right, Left, Center, NotShow, Custom
}

enum class TranslationMode(val labelResId: Int) {
    GOOGLE_URL(R.string.google_full_page),
    GOOGLE_IN_PLACE(R.string.google_in_place),
    TRANSLATE_BY_PARAGRAPH(R.string.translate_by_paragraph),
    PAPAGO_TRANSLATE_BY_SCREEN(R.string.papago_translate_by_screen),
    DEEPL_BY_PARAGRAPH(R.string.deepl_translate_by_paragraph),
    OPENAI_BY_PARAGRAPH(R.string.openai_translate_by_paragraph),
    GEMINI_BY_PARAGRAPH(R.string.gemini_translate_by_paragraph),
    OPENAI_IN_PLACE(R.string.openai_in_place),
    GEMINI_IN_PLACE(R.string.gemini_in_place),
}

@kotlinx.serialization.Serializable
enum class FontType(val resId: Int) {
    SYSTEM_DEFAULT(R.string.system_default),
    SERIF(R.string.serif),
    GOOGLE_SERIF(R.string.googleserif),
    CUSTOM(R.string.custom_font),
    TC_IANSUI(R.string.iansui_tc),
    JA_MINCHO(R.string.mincho_ja),
    KO_GAMJA(R.string.gamja_flower_ko)
}

enum class DarkMode {
    SYSTEM, FORCE_ON, DISABLED
}

enum class NewTabBehavior {
    START_INPUT, SHOW_HOME, SHOW_RECENT_BOOKMARKS
}

enum class HighlightStyle(
    val color: Color?,
    val stringResId: Int,
    val iconResId: Int,
) {
    UNDERLINE(
        null,
        R.string.underline,
        R.drawable.ic_underscore,
    ),
    BACKGROUND_YELLOW(
        Color.Yellow,
        R.string.yellow,
        R.drawable.ic_highlight_color,
    ),
    BACKGROUND_GREEN(
        Color.Green,
        R.string.green,
        R.drawable.ic_highlight_color,
    ),
    BACKGROUND_BLUE(
        Color.Blue,
        R.string.blue,
        R.drawable.ic_highlight_color,
    ),
    BACKGROUND_PINK(
        Color.Red,
        R.string.pink,
        R.drawable.ic_highlight_color,
    ),
}

enum class TranslationTextStyle(
    val stringResId: Int,
) {
    NONE(R.string.none),
    DASHED_BORDER(R.string.dashed_border),
    VERTICAL_LINE(R.string.vertical_line),
    GRAY(R.string.gray),
    BOLD(R.string.bold),
}

enum class SaveHistoryMode {
    SAVE_WHEN_OPEN, SAVE_WHEN_CLOSE, DISABLED
}

enum class EinkImageAdjustment(val strength: Int, val labelResId: Int) {
    OFF(0, R.string.eink_image_off),
    LEVEL_10(10, R.string.eink_image_10),
    LEVEL_30(30, R.string.eink_image_30),
    LEVEL_50(50, R.string.eink_image_50),
    LEVEL_70(70, R.string.eink_image_70),
    LEVEL_100(100, R.string.eink_image_100),
}

enum class ToolbarPosition {
    Bottom, Top, Left, Right
}
