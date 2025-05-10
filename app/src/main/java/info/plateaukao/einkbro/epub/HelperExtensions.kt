package info.plateaukao.einkbro.epub

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory


internal val NodeList.elements get() = (0..length).asSequence().mapNotNull { item(it) as? Element }
internal val Node.childElements get() = childNodes.elements
internal fun Document.selectFirstTag(tag: String): Node? = getElementsByTagName(tag).item(0)
internal fun Node.selectFirstChildTag(tag: String) = childElements.find { it.tagName == tag }
internal fun Node.selectChildTag(tag: String) = childElements.filter { it.tagName == tag }
internal fun Node.selectChildTags(tag1: String, tag2: String) = childElements.filter { it.tagName == tag1 || it.tagName == tag2 }
internal fun Node.getAttributeValue(attribute: String): String? =
    attributes?.getNamedItem(attribute)?.textContent

internal fun parseXMLFile(inputSteam: InputStream): Document? =
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSteam)

internal fun parseXMLFile(byteArray: ByteArray): Document? = parseXMLFile(byteArray.inputStream())
@Suppress("unused")
internal fun parseXMLText(text: String): Document? = text.reader().runCatching {
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(this))
}.getOrNull()

internal val String.decodedURL: String get() = URLDecoder.decode(this, "UTF-8")
internal fun String.asFileName(): String = this.replace("/", "_")

internal fun ZipInputStream.entries() = generateSequence { nextEntry }
