package info.plateaukao.einkbro.epub

import android.content.Context
import android.os.Build
import info.plateaukao.einkbro.epub.EpubBook.Chapter
import info.plateaukao.einkbro.epub.EpubBook.ChapterPart
import info.plateaukao.einkbro.epub.EpubBook.Image
import info.plateaukao.einkbro.epub.EpubBook.ToCEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.io.path.invariantSeparatorsPathString

internal suspend fun getZipFiles(
    inputStream: InputStream,
): Map<String, EpubFile> = withContext(Dispatchers.IO) {
    ZipInputStream(inputStream).use { zipInputStream ->
        zipInputStream
            .entries()
            .filterNot { it.isDirectory }
            .map { EpubFile(absPath = it.name, data = zipInputStream.readBytes()) }
            .associateBy { it.absPath }
    }
}

// Add this function to your EpubParser.kt file
private fun createDummyMetadata(document: Document): Element {
    val metadata = document.createElement("metadata")

    // Create and add basic required elements
    val title = document.createElement("dc:title")
    title.textContent = "Unknown Title"
    metadata.appendChild(title)

    val creator = document.createElement("dc:creator")
    creator.textContent = "Unknown Author"
    metadata.appendChild(creator)

    val description = document.createElement("dc:description")
    description.textContent = "No description available"
    metadata.appendChild(description)

    return metadata
}

private fun createDummyManifest(document: Document): Element {
    val manifest = document.createElement("manifest")

    // Create a minimal item entry
    val item = document.createElement("item")
    item.setAttribute("id", "dummy-item")
    item.setAttribute("href", "dummy.xhtml")
    item.setAttribute("media-type", "application/xhtml+xml")
    manifest.appendChild(item)

    // Create an NCX item that's often required
    val ncxItem = document.createElement("item")
    ncxItem.setAttribute("id", "ncx")
    ncxItem.setAttribute("href", "toc.ncx")
    ncxItem.setAttribute("media-type", "application/x-dtbncx+xml")
    manifest.appendChild(ncxItem)

    return manifest
}


@Throws(Exception::class)
suspend fun epubParser(
    context: Context,
    inputStream: InputStream,
): EpubBook = withContext(Dispatchers.Default) {
    val files = getZipFiles(inputStream)

    val container = files["META-INF/container.xml"]
        ?: throw Exception("META-INF/container.xml file missing")

    val opfFilePath = parseXMLFile(container.data)
        ?.selectFirstTag("rootfile")
        ?.getAttributeValue("full-path")
        ?.decodedURL ?: throw Exception("Invalid container.xml file")
    // Extract rootPath
    val rootPath = opfFilePath.substringBefore('/', "") // Get the part before the first slash
    val opfFile = files[opfFilePath] ?: throw Exception(".opf file missing")

    val document = parseXMLFile(opfFile.data)
        ?: throw Exception(".opf file failed to parse data")
    val metadata = document.selectFirstTag("metadata")
        ?: document.selectFirstTag("opf:metadata")
        ?: createDummyMetadata(document)
    val manifest = document.selectFirstTag("manifest")
        ?: document.selectFirstTag("opf:manifest")
        ?: createDummyManifest(document)
    val spine = document.selectFirstTag("spine")
        ?: document.selectFirstTag("opf:spine")
        ?: throw Exception(".opf file spine section missing")
    val guide = document.selectFirstTag("guide")
    val metadataTitle = metadata.selectFirstChildTag("dc:title")?.textContent
        ?: "Unknown Title"
    val metadataCreator = metadata.selectFirstChildTag("dc:creator")?.textContent

    val metadataDesc = metadata.selectFirstChildTag("dc:description")?.textContent

    val metadataCoverId = metadata
        .selectChildTag("meta")
        .find { it.getAttributeValue("name") == "cover" }
        ?.getAttributeValue("content")


    val hrefRootPath = File(opfFilePath).parentFile ?: File("")
    fun String.hrefAbsolutePath() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        File(hrefRootPath, this).canonicalFile
            .toPath()
            .invariantSeparatorsPathString
            .removePrefix("/")
    } else {
        TODO("VERSION.SDK_INT < O")
    }

    val manifestItems = manifest.selectChildTags("item", "opf:item").map {
        ManifestItem(
            id = it.getAttribute("id"),
            absPath = it.getAttribute("href").decodedURL.hrefAbsolutePath(),
            mediaType = it.getAttribute("media-type"),
            properties = it.getAttribute("properties")
        )
    }.associateBy { it.id }


    fun parseCoverImageFromXhtml(coverFile: EpubFile): Image? {
        val doc = Jsoup.parse(coverFile.data.inputStream(), "UTF-8", "")
        // Find the <img> tag within the XHTML file
        val imgTag = doc.selectFirst("img")

        if (imgTag != null) {
            var imgSrc = imgTag.attribute("src").value.hrefAbsolutePath()
            if (!imgSrc.startsWith("$rootPath/")) {
                imgSrc = "$rootPath/$imgSrc"
            }

            val imgFile = files[imgSrc]


            if (imgFile != null) {
                return Image(absPath = imgFile.absPath, mediaType = "image/${imgSrc.split(".").last()}", image = imgFile.data)
            }
        }
        return null
    }

    // 1. Primary Method: Try to get the cover image from the manifest
    var coverImage = manifestItems[metadataCoverId]
        ?.let { files[it.absPath] }
        ?.let { Image(absPath = it.absPath, mediaType = "image/jpeg", image = it.data) }

    // 2. Fallback: Check the `<guide>` tag if the primary method didn't yield a cover
    if (coverImage == null) {
        var coverHref = guide?.selectChildTag("reference")
            ?.find { it.getAttribute("type") == "cover" }
            ?.getAttributeValue("href")?.decodedURL?.hrefAbsolutePath()

        if (guide == null) {
            val manifestCoverItem = manifestItems["cover"]
            coverHref = manifestCoverItem?.absPath
        }
        if (coverHref != null) {
            val coverFile = files[coverHref]
            if (coverFile != null) {
                coverImage = parseCoverImageFromXhtml(coverFile)
            }
        }
    }

    fun getTocEntries(): List<ToCEntry> {
        val ncxFilePath = manifestItems["ncx"]?.absPath
        if (ncxFilePath != null) {
            val ncxFilePath = manifestItems["ncx"]?.absPath
            val ncxFile = files[ncxFilePath] ?: throw Exception("ncx file missing")

            val doc = Jsoup.parse(ncxFile.data.inputStream(), "UTF-8", "")
            val navMap = doc.selectFirst("navMap") ?: throw Exception("Invalid NCX file: navMap not found")

            return navMap.select("navPoint").map { navPoint ->
                val title = navPoint.selectFirst("navLabel")?.selectFirst("text")?.text() ?: ""
                var link = navPoint.selectFirst("content")?.attribute("src")?.value ?: "" // Add the prefix
                if (!link.startsWith(rootPath))
                    link = "$rootPath/$link"
                ToCEntry(title, link)
            }
        } else {
            val tocFilePath = manifestItems["toc"]?.absPath
            val tocFile = files[tocFilePath] ?: throw Exception("toc file missing")

            val doc = Jsoup.parse(tocFile.data.inputStream(), "UTF-8", "")
            val nav = doc.select("nav[epub:type=toc], nav#toc, nav.toc").first()
                ?: doc.select("nav").first()
                ?: return emptyList()

            val entries = mutableListOf<ToCEntry>()
            // Look for ordered lists directly under the nav, then list items, then links
            nav.select("ol > li > a[href], ul > li > a[href], a[href]")
                .forEach { linkElement -> // More specific selector first
                    val title = linkElement.text().trim()
                    var hrefAttr = linkElement.attr("href")
                    if (!hrefAttr.startsWith(rootPath)) {
                        hrefAttr = "$rootPath/$hrefAttr"
                    }
                    if (title.isNotEmpty() && hrefAttr.isNotEmpty()) {
                        entries.add(ToCEntry(title, hrefAttr)) // to be fixed
                    }
                }
            return entries
        }
    }

    val tocEntries = getTocEntries()

    // Function to check if a spine item is a chapter
    fun isChapterPart(item: ManifestItem): Boolean {
        val extension = item.absPath.substringAfterLast('.')
        return listOf("xhtml", "xml", "html").contains(extension)
    }

    fun findTocEntryForChapter(tocEntries: List<ToCEntry>, chapterUrl: String): ToCEntry? {
        // Remove any potential fragment identifier from chapterUrl
        val chapterUrlWithoutFragment = chapterUrl.substringBefore('#')
        return tocEntries.firstOrNull {
            it.chapterLink.substringBefore('#').equals(chapterUrlWithoutFragment, ignoreCase = true)
        }
    }

    // Iterate through spine items to build chapters list
    val chapters = mutableListOf<Chapter>()
    var currentTOC: ToCEntry? = null
    var chapterParts = mutableListOf<ChapterPart>()
    var pageProgressDirection = PageProgressDirection.LTR

    spine.attributes.getNamedItem("page-progression-direction")?.let {
        val direction = it.nodeValue
        pageProgressDirection = if (direction == "rtl") {
            PageProgressDirection.RTL
        } else {
            PageProgressDirection.LTR
        }
    }

    spine.selectChildTags("itemref", "opf:itemref").forEach { itemRef ->
        val itemId = itemRef.getAttribute("idref")
        val spineItem = manifestItems[itemId]

        // Check if the spine item exists and is a chapter
        if (spineItem != null && isChapterPart(spineItem)) {
            var spineUrl = spineItem.absPath
            //if (!spineUrl.startsWith(rootPath)) spineUrl = "$rootPath/$spineUrl"

            val tocEntry = findTocEntryForChapter(tocEntries, spineUrl)
            val parser = EpubXMLFileParser(spineUrl, files[spineUrl]?.data ?: ByteArray(0), files)
            val res = parser.parseAsDocument()

            // handle cover image case
            if (spineItem.mediaType.startsWith("image/")) {
                chapters.add(Chapter("image_${spineItem.absPath}", "",
                    listOf(ChapterPart("", parser.parseAsImage(spineItem.absPath)))))
                return@forEach
            }
            // If currentTOC exists and we have a new tocEntry, add the accumulated chapter content
            if (currentTOC != null && tocEntry != null && chapterParts.isNotEmpty()) {
                chapters.add(Chapter(currentTOC!!.chapterLink, currentTOC!!.chapterTitle, chapterParts))
                chapterParts = mutableListOf()
            }

            if (tocEntry == null) {
                chapterParts.add(ChapterPart(spineItem.absPath, res.body))
            } else {
                currentTOC = tocEntry
                if (spineItem.mediaType.startsWith("image/")) {
                    chapters.add(Chapter("image_${spineItem.absPath}", "", listOf(ChapterPart(spineItem.absPath, ""))))
                    chapterParts = mutableListOf()
                } else {
                    chapterParts.add(ChapterPart(spineItem.absPath, res.body))
                }
            }
        }
    }

    // Add the last chapter if any content remains
    if (currentTOC != null && chapterParts.isNotEmpty()) {
        chapters.add(Chapter(currentTOC!!.chapterLink, currentTOC!!.chapterTitle, chapterParts))
        chapterParts = mutableListOf()
    }


    val imageExtensions =
        listOf("png", "gif", "raw", "png", "jpg", "jpeg", "webp", "svg").map { ".$it" }
    val unlistedImages = files
        .asSequence()
        .filter { (_, file) ->
            imageExtensions.any { file.absPath.endsWith(it, ignoreCase = true) }
        }
        .map { (_, file) ->
            Image(absPath = file.absPath, mediaType = "image/" + file.absPath.split(".").last(), image = file.data)
        }

    val listedImages = manifestItems.asSequence()
        .map { it.value }
        .filter { it.mediaType.startsWith("image") }
        //.mapNotNull { files[it.absPath] }
        .map { Image(absPath = it.absPath, mediaType = it.mediaType, image = files[it.absPath]!!.data) }

    val images = (listedImages + unlistedImages).distinctBy { it.absPath }

    val epubTempExtractionLocation = context.cacheDir.toString() + "/tempfiles"
    // clean it first
    if (File(epubTempExtractionLocation).exists()) {
        File(epubTempExtractionLocation).deleteRecursively()
    }

    // create it
    File(epubTempExtractionLocation).mkdirs()
    files.forEach { (_, file) ->
        // write file to temp location with relative path
        val relativePath = file.absPath.substringAfter("$rootPath/")
        val epubFile = File(epubTempExtractionLocation + File.separator + relativePath)
        if (!epubFile.parentFile.exists()) {
            epubFile.parentFile.mkdirs()
        }
        epubFile.writeBytes(file.data)
    }

    return@withContext EpubBook(
        fileName = metadataTitle.asFileName(),
        title = metadataTitle,
        author = metadataCreator,
        description = metadataDesc,
        coverImage = coverImage,
        pageProgressDirection = pageProgressDirection,
        chapters = chapters.toList(),
        images = images.toList(),
        rootPath = rootPath,
    )
}
