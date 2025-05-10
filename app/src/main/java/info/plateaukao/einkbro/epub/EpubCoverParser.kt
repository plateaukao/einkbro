package info.plateaukao.einkbro.epub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.io.path.invariantSeparatorsPathString

@Throws(Exception::class)
suspend fun epubCoverParser(
    inputStream: InputStream
): EpubBook.Image? = withContext(Dispatchers.Default) {
    val files = getZipFiles(inputStream)

    val container = files["META-INF/container.xml"]
        ?: throw Exception("META-INF/container.xml file missing")

    val opfFilePath = parseXMLFile(container.data)
        ?.selectFirstTag("rootfile")
        ?.getAttributeValue("full-path")
        ?.decodedURL ?: throw Exception("Invalid container.xml file")

    val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

    val document = parseXMLFile(opfFile.data)
        ?: throw Exception(".opf file failed to parse data")
    val metadata = document.selectFirstTag("metadata")
        ?: throw Exception(".opf file metadata section missing")
    val manifest = document.selectFirstTag("manifest")
        ?: throw Exception(".opf file manifest section missing")

    val metadataCoverId = metadata
        .selectChildTag("meta")
        .find { it.getAttributeValue("name") == "cover" }
        ?.getAttributeValue("content")

    val hrefRootPath = File(opfFilePath).parentFile ?: File("")
    fun String.hrefAbsolutePath() = File(hrefRootPath, this).canonicalFile
        .toPath()
        .invariantSeparatorsPathString
        .removePrefix("/")

    data class EpubManifestItem(
        val id: String,
        val absoluteFilePath: String,
        val mediaType: String,
        val properties: String
    )

    val manifestItems = manifest
        .selectChildTag("item").map {
            EpubManifestItem(
                id = it.getAttribute("id"),
                absoluteFilePath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
                mediaType = it.getAttribute("media-type"),
                properties = it.getAttribute("properties")
            )
        }.associateBy { it.id }

    manifestItems[metadataCoverId]
        ?.let { files[it.absoluteFilePath] }
        ?.let { EpubBook.Image(absPath = it.absPath, mediaType = "image/jpeg", image = it.data) }
}