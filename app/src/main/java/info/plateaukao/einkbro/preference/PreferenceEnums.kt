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

@kotlinx.serialization.Serializable
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

// Persisted by ordinal; only append new entries.
enum class NewTabBehavior {
    START_INPUT, SHOW_HOME, SHOW_RECENT_BOOKMARKS, SHOW_START_PAGE
}

enum class ShareLongPressAction(val labelResId: Int) {
    COPY_LINK(R.string.share_long_press_copy_link),
    LAST_SHARE_TARGET(R.string.share_long_press_last_target),
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

// DEEP re-encodes images at the network layer (full pipeline incl. dithering);
// FAST injects a CSS filter instead: no CPU/re-encode cost, and it also covers
// data:/blob: URIs and JS-generated images, but can't dither.
enum class EinkImageMode(val labelResId: Int) {
    DEEP(R.string.eink_image_mode_deep),
    FAST(R.string.eink_image_mode_fast),
}

enum class ToolbarPosition {
    Bottom, Top, Left, Right
}

// UI accent color themes. Persisted by ordinal; only append new entries.
// accent/accentDark: buttons, borders, dividers, toolbar icons, top bar.
// background/onBackground: light-mode surface tint and text color.
// onBackgroundDark: dark-mode text color (dark background stays black for e-ink).
enum class UiTheme(
    val accent: Color,
    val accentDark: Color,
    val background: Color = Color.White,
    val onBackground: Color = Color.Black,
    val onBackgroundDark: Color = Color.Gray,
) {
    CLASSIC(Color.Black, Color(0xFFAAAAAA)),
    LIGHT_BLUE(
        Color(0xFF4A90D9), Color(0xFF8FBCE8),
        Color(0xFFF3F7FC), Color(0xFF1B3A5C), Color(0xFF9FB6CC),
    ),
    DARK_BLUE(
        Color(0xFF16437E), Color(0xFF7A9CC6),
        Color(0xFFF2F5FA), Color(0xFF122F58), Color(0xFF97A8C0),
    ),
    GREEN(
        Color(0xFF2E7D32), Color(0xFF81C784),
        Color(0xFFF2F8F2), Color(0xFF1B421D), Color(0xFF9DB89E),
    ),
    SEPIA(
        Color(0xFF795548), Color(0xFFBCAAA4),
        Color(0xFFF7F1E3), Color(0xFF3E2C23), Color(0xFFB3A79B),
    ),
    PURPLE(
        Color(0xFF673AB7), Color(0xFFB39DDB),
        Color(0xFFF6F3FB), Color(0xFF32205C), Color(0xFFA99BC4),
    ),
    RED(
        Color(0xFFC62828), Color(0xFFE57373),
        Color(0xFFFBF3F2), Color(0xFF571A17), Color(0xFFC09A98),
    ),

    // Colors ignored: the palette is derived from DisplayConfig.customThemeColor.
    CUSTOM(Color(0xFF4A90D9), Color(0xFF8FBCE8)),
}

// Resolved colors for the current theme; equals the enum's fixed colors for
// the preset themes and a derived palette for CUSTOM.
data class ThemePalette(
    val accent: Color,
    val accentDark: Color,
    val background: Color,
    val onBackground: Color,
    val onBackgroundDark: Color,
)

fun UiTheme.palette(customColor: Color): ThemePalette =
    if (this == UiTheme.CUSTOM) deriveThemePalette(customColor)
    else ThemePalette(accent, accentDark, background, onBackground, onBackgroundDark)

/**
 * Derives a readable palette from an arbitrary base color: the accent is
 * clamped so it stays visible on white, the dark-mode accent is brightened
 * for black backgrounds, and the text/background tints keep the base hue
 * with contrast-safe saturation/brightness.
 */
fun deriveThemePalette(base: Color): ThemePalette {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            255,
            (base.red * 255).toInt(),
            (base.green * 255).toInt(),
            (base.blue * 255).toInt(),
        ),
        hsv,
    )
    val h = hsv[0]
    val s = hsv[1]
    val isGrayish = s < 0.08f
    fun make(hue: Float, sat: Float, value: Float) = Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(hue, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
        )
    )
    return ThemePalette(
        // visible on white: keep it saturated and not too bright
        accent = make(h, if (isGrayish) s else maxOf(s, 0.35f), hsv[2].coerceIn(0.25f, 0.8f)),
        // visible on black: bright, softened saturation
        accentDark = make(h, minOf(s, 0.45f), maxOf(hsv[2], 0.75f)),
        // near-white with a whisper of the hue
        background = make(h, s * 0.07f, 0.985f),
        // dark shade of the hue for body text on the tinted background
        onBackground = make(h, if (isGrayish) s else minOf(maxOf(s * 0.7f, 0.35f), 0.65f), 0.30f),
        // soft tinted gray for text on black
        onBackgroundDark = make(h, minOf(s * 0.4f, 0.2f), 0.72f),
    )
}

/**
 * Border edge treatment, independent of the color theme and of the fill.
 * widthDp is the stroke weight; frameRadius applies to dialog window frames
 * and floating panels, itemRadius to in-content bordered items (the radii
 * also shape the fill when the border is NONE).
 * Persisted by ordinal; only append new entries.
 */
enum class UiBorder(
    val widthDp: Float,
    val frameRadiusDp: Float,
    val itemRadiusDp: Float,
) {
    NONE(0f, 16f, 12f),
    // the original 1dp look
    CLASSIC(1f, 5f, 7f),
    // pill / very round
    ROUND(1f, 16f, 14f),
    // sharp corners with a bold stroke
    SHARP(2f, 0f, 0f),
    // print-like double frame
    PAPER(1f, 10f, 8f),
    DASHED(1.5f, 6f, 6f),
    // postage-stamp scalloped edge
    STAMP(1f, 0f, 0f),
    // wobbly hand-drawn line
    SKETCH(1.5f, 4f, 4f),
    // thick outer frame with a hairline inner frame
    CERTIFICATE(3f, 0f, 0f),
    // rounded frame with a solid offset shadow
    STICKER(1.5f, 14f, 12f),
}

/**
 * Fill treatment for themed surfaces (dialogs, panels, selected items),
 * independent of the border. NONE is the plain theme background; GRADIENT
 * uses the adjustable angle/level. Persisted by ordinal; only append.
 */
enum class UiFill { NONE, TONAL, GRADIENT }

// baseline accent-blend fractions of the gradient fill (the user's gradient
// level scales them)
const val GRADIENT_START_FRACTION = 0.02f
const val GRADIENT_END_FRACTION = 0.28f
