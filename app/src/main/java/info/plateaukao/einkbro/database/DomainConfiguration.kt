package info.plateaukao.einkbro.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import info.plateaukao.einkbro.preference.FontType
import kotlinx.serialization.Serializable

@Entity(tableName = "domain_configuration")
data class DomainConfiguration(
    @PrimaryKey
    var domain: String,
    var configuration: String,
)

@Serializable
data class DomainConfigurationData(
    val domain: String,
    var shouldFixScroll: Boolean = false,
    var shouldSendPageNavKey: Boolean = false,
    var shouldTranslateSite: Boolean = false,
    var shouldUseWhiteBackground: Boolean = false,
    var shouldInvertColor: Boolean = false,
    // Per-site display overrides (null = use global setting)
    var fontSize: Int? = null,
    var fontType: FontType? = null,
    var boldFontStyle: Boolean? = null,
    var blackFontStyle: Boolean? = null,
    var fontBoldness: Int? = null,
    var desktopMode: Boolean? = null,
    var enableJavascript: Boolean? = null,
)