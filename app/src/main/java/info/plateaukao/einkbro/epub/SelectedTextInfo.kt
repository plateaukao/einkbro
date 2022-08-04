package info.plateaukao.einkbro.epub

import org.json.JSONObject

data class SelectedTextInfo(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val startNodeData: String,
    val startNodeHtml: String,
    val startNodeTagName: String,
    val endNodeData: String,
    val endNodeHtml: String,
    val endNodeTagName: String,
    val isCollapsed: Boolean,
    val chapterNumber: Int,
    val dataString: String,
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("selectedText", text)
        put("startOffset", startOffset)
        put("endOffset", endOffset)
        put("startNodeData", startNodeData)
        put("startNodeHTML", startNodeHtml)
        put("startNodeTagName", startNodeTagName)
        put("endNodeData", endNodeData)
        put("endNodeHTML", endNodeHtml)
        put("endNodeTagName", endNodeTagName)
        put("status", isCollapsed)
        put("chapterNumber", chapterNumber)
        put("dataString", dataString)
    }

    companion object {
        fun from(jsonString: String, chapterNumber: Int, dataString: String): SelectedTextInfo? {
            try {
                val obj = JSONObject(jsonString)
                return SelectedTextInfo(
                        obj.getString("selectedText"),
                        obj.getInt("startOffset"),
                        obj.getInt("endOffset"),
                        obj.getString("startNodeData"),
                        obj.getString("startNodeHTML"),
                        obj.getString("startNodeTagName"),
                        obj.getString("endNodeData"),
                        obj.getString("endNodeHTML"),
                        obj.getString("endNodeTagName"),
                        obj.getInt("status") == 1,
                        chapterNumber,
                        dataString,
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}