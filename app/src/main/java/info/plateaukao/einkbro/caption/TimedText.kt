package info.plateaukao.einkbro.caption

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimedText(
    @SerialName("wireMagic") val wireMagic: String,
    @SerialName("pens") val pens: List<Pen>,
    @SerialName("wsWinStyles") val wsWinStyles: List<WsWinStyle>,
    @SerialName("wpWinPositions") val wpWinPositions: List<WpWinPosition>,
    @SerialName("events") val events: MutableList<Event>
)

@Serializable
class Pen

@Serializable
data class WsWinStyle(
    @SerialName("mhModeHint") var mhModeHint: Int? = null,
    @SerialName("juJustifCode") val juJustifCode: Int? = null,
    @SerialName("sdScrollDir") var sdScrollDir: Int? = null
)

@Serializable
data class WpWinPosition(
    @SerialName("apPoint") val apPoint: Int? = null,
    @SerialName("ahHorPos") val ahHorPos: Int? = null,
    @SerialName("avVerPos") val avVerPos: Int? = null,
    @SerialName("rcRows") val rcRows: Int? = null,
    @SerialName("ccCols") val ccCols: Int? = null
)

@Serializable
data class Event(
    @SerialName("tStartMs") val tStartMs: Long = 0,
    @SerialName("dDurationMs") val dDurationMs: Long = 0,
    @SerialName("id") val id: Int = 0,
    @SerialName("wpWinPosId") val wpWinPosId: Int? = null,
    @SerialName("wsWinStyleId") val wsWinStyleId: Int? = null,
    @SerialName("wWinId") val wWinId: Int? = 1,
    @SerialName("segs") val segs: MutableList<Segment>? = mutableListOf()
)

@Serializable
data class Segment(
    @SerialName("utf8") var utf8: String,
    @SerialName("acAsrConf") val acAsrConf: Int = 0
)