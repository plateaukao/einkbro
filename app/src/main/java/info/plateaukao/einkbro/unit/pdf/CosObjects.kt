package info.plateaukao.einkbro.unit.pdf

import java.io.OutputStream

/**
 * Minimal COS object model for the save-as-PDF post-processing (TOC entries,
 * page appending). Replaces pdfbox-android, which cost about 370 KB of dex for
 * these two operations.
 */
internal sealed class CosObject

internal object CosNull : CosObject()

internal data class CosBool(val value: Boolean) : CosObject()

internal data class CosInt(val value: Long) : CosObject()

/** Real number; the raw source text is kept so re-serialization is lossless. */
internal data class CosReal(val raw: String) : CosObject()

internal data class CosName(val name: String) : CosObject()

/** String kept as raw decoded bytes; [hex] only picks the output form. */
internal class CosString(val bytes: ByteArray, val hex: Boolean) : CosObject()

internal data class CosRef(val num: Int, val gen: Int) : CosObject()

internal class CosArray(val items: MutableList<CosObject> = mutableListOf()) : CosObject()

internal class CosDict(
    val entries: LinkedHashMap<String, CosObject> = LinkedHashMap(),
) : CosObject() {
    operator fun get(key: String): CosObject? = entries[key]
    operator fun set(key: String, value: CosObject) {
        entries[key] = value
    }

    fun copy(): CosDict = CosDict(LinkedHashMap(entries))
}

/**
 * Stream object. The raw (still-encoded) data stays where it is — in the
 * source file for copied streams, in memory for streams this writer builds —
 * and is only streamed through when serializing, so appending an image-heavy
 * document never holds more than one stream's bytes at a time.
 */
internal class CosStream(val dict: CosDict, val raw: RawData) : CosObject() {
    sealed class RawData {
        class InFile(val file: PdfFile, val offset: Long, val length: Int) : RawData()
        class InMemory(val bytes: ByteArray) : RawData()
    }

    val rawLength: Int
        get() = when (raw) {
            is RawData.InFile -> raw.length
            is RawData.InMemory -> raw.bytes.size
        }

    fun readRaw(): ByteArray = when (raw) {
        is RawData.InFile -> raw.file.readBytes(raw.offset, raw.length)
        is RawData.InMemory -> raw.bytes
    }

    fun copyRawTo(out: OutputStream) {
        when (raw) {
            is RawData.InFile -> raw.file.copyBytesTo(raw.offset, raw.length, out)
            is RawData.InMemory -> out.write(raw.bytes)
        }
    }
}
