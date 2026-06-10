package info.plateaukao.einkbro.epub

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds an in-memory zip (epub) from the given entries.
 */
internal fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
    val bos = ByteArrayOutputStream()
    ZipOutputStream(bos).use { zos ->
        entries.forEach { (name, data) ->
            zos.putNextEntry(ZipEntry(name))
            zos.write(data)
            zos.closeEntry()
        }
    }
    return bos.toByteArray()
}

internal fun containerXml(opfPath: String): String = """
    <?xml version="1.0" encoding="UTF-8"?>
    <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
      <rootfiles>
        <rootfile full-path="$opfPath" media-type="application/oebps-package+xml"/>
      </rootfiles>
    </container>
""".trimIndent()
