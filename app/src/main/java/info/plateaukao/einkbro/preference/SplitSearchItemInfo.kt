package info.plateaukao.einkbro.preference

import kotlinx.serialization.Serializable

@Serializable
data class SplitSearchItemInfo(
    val title: String,
    val stringPattern: String,
    val enabled: Boolean
)