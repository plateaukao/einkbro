@file:Suppress("ArrayInDataClass")

package info.plateaukao.einkbro.epub

@Suppress("ArrayInDataClass")
data class EpubFile(
    val absPath: String,
    val data: ByteArray,
)

@Suppress("ArrayInDataClass")
data class EpubBook(
    val fileName: String,
    val title: String,
    val author: String?,
    val description: String?,
    val coverImage: Image?,
    val pageProgressDirection: PageProgressDirection?,
    val chapters: List<Chapter>,
    val images: List<Image>,
    val toc: List<ToCEntry> = emptyList(),
    val rootPath: String = "",
) {
    data class Chapter(
        val absPath: String,
        val title: String,
        val parts: List<ChapterPart>,
    )

    data class ChapterPart(
        val absPath: String,
        val body: String,
    )

    data class Image(
        val absPath: String,
        val mediaType: String,
        val image: ByteArray
    )

    data class ToCEntry(
        val chapterTitle: String,
        val chapterLink: String
    )
}
data class ManifestItem(
    val id: String,
    val absPath: String,
    val mediaType: String,
    val properties: String
)

enum class PageProgressDirection { LTR, RTL }