package info.plateaukao.einkbro.unit.pdf

import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.zip.Inflater

internal class PdfParseException(message: String) : Exception(message)

/**
 * Read-only view of one PDF file: cross-reference resolution (classic tables,
 * xref streams, object streams, hybrid files) plus on-demand object parsing.
 * Backed by a memory-mapped buffer, so a large document never has to be fully
 * resident. Unsupported constructs throw [PdfParseException] — callers treat
 * any failure as "leave the file alone" and fall back.
 */
internal class PdfFile private constructor(private val buf: ByteBuffer) {

    sealed class XrefEntry {
        class Regular(val offset: Long, val gen: Int) : XrefEntry()
        class InObjectStream(val streamObjNum: Int, val index: Int) : XrefEntry()
    }

    private val xref = HashMap<Int, XrefEntry>()
    private val objectCache = HashMap<Int, CosObject>()
    private val objectStreamCache = HashMap<Int, Map<Int, CosObject>>()

    lateinit var trailer: CosDict
        private set
    var maxObjectNumber = 0
        private set
    var lastStartXref = 0L
        private set
    var usesXrefStream = false
        private set

    val size: Long get() = buf.limit().toLong()

    val isEncrypted: Boolean get() = trailer["Encrypt"] != null

    // region raw byte access

    private fun byteAt(pos: Long): Int {
        if (pos < 0 || pos >= size) return -1
        return buf.get(pos.toInt()).toInt() and 0xff
    }

    fun readBytes(offset: Long, length: Int): ByteArray {
        val out = ByteArray(length)
        val dup = buf.duplicate()
        dup.position(offset.toInt())
        dup.get(out, 0, length)
        return out
    }

    fun copyBytesTo(offset: Long, length: Int, out: OutputStream) {
        val chunk = ByteArray(64 * 1024)
        var pos = offset
        var remaining = length
        val dup = buf.duplicate()
        while (remaining > 0) {
            val n = minOf(remaining, chunk.size)
            dup.position(pos.toInt())
            dup.get(chunk, 0, n)
            out.write(chunk, 0, n)
            pos += n
            remaining -= n
        }
    }

    /** Streams the whole original file into [out]. */
    fun copyAllTo(out: OutputStream) = copyBytesTo(0, size.toInt(), out)

    fun endsWithNewline(): Boolean {
        val last = byteAt(size - 1)
        return last == '\n'.code || last == '\r'.code
    }

    // endregion

    // region lexer

    private class Cursor(var pos: Long)

    private fun isWhite(c: Int) = c == 0 || c == 9 || c == 10 || c == 12 || c == 13 || c == 32

    private fun isDelim(c: Int) =
        c == '('.code || c == ')'.code || c == '<'.code || c == '>'.code ||
            c == '['.code || c == ']'.code || c == '{'.code || c == '}'.code ||
            c == '/'.code || c == '%'.code

    private fun skipWs(cur: Cursor) {
        while (true) {
            val c = byteAt(cur.pos)
            if (isWhite(c)) {
                cur.pos++
            } else if (c == '%'.code) {
                while (cur.pos < size && byteAt(cur.pos) != '\n'.code && byteAt(cur.pos) != '\r'.code) cur.pos++
            } else {
                return
            }
        }
    }

    private fun readKeyword(cur: Cursor): String {
        skipWs(cur)
        val sb = StringBuilder()
        while (true) {
            val c = byteAt(cur.pos)
            if (c < 0 || isWhite(c) || isDelim(c)) break
            sb.append(c.toChar())
            cur.pos++
        }
        return sb.toString()
    }

    private fun expectKeyword(cur: Cursor, keyword: String) {
        val actual = readKeyword(cur)
        if (actual != keyword) throw PdfParseException("expected '$keyword', got '$actual' at ${cur.pos}")
    }

    // endregion

    // region object parsing

    private fun parseObject(cur: Cursor): CosObject {
        skipWs(cur)
        return when (val c = byteAt(cur.pos)) {
            '/'.code -> parseName(cur)
            '('.code -> parseLiteralString(cur)
            '<'.code ->
                if (byteAt(cur.pos + 1) == '<'.code) parseDict(cur) else parseHexString(cur)

            '['.code -> parseArray(cur)
            't'.code, 'f'.code, 'n'.code -> when (val kw = readKeyword(cur)) {
                "true" -> CosBool(true)
                "false" -> CosBool(false)
                "null" -> CosNull
                else -> throw PdfParseException("unexpected keyword '$kw' at ${cur.pos}")
            }

            else ->
                if (c == '+'.code || c == '-'.code || c == '.'.code || c in '0'.code..'9'.code) {
                    parseNumberOrRef(cur)
                } else {
                    throw PdfParseException("unexpected byte $c at ${cur.pos}")
                }
        }
    }

    private fun parseName(cur: Cursor): CosName {
        cur.pos++ // '/'
        val sb = StringBuilder()
        while (true) {
            val c = byteAt(cur.pos)
            if (c < 0 || isWhite(c) || isDelim(c)) break
            if (c == '#'.code) {
                val hex = "${byteAt(cur.pos + 1).toChar()}${byteAt(cur.pos + 2).toChar()}"
                sb.append(hex.toInt(16).toChar())
                cur.pos += 3
            } else {
                sb.append(c.toChar())
                cur.pos++
            }
        }
        return CosName(sb.toString())
    }

    private fun parseLiteralString(cur: Cursor): CosString {
        cur.pos++ // '('
        val out = ArrayList<Byte>(32)
        var depth = 1
        while (true) {
            val c = byteAt(cur.pos)
            if (c < 0) throw PdfParseException("unterminated string")
            cur.pos++
            when (c) {
                '\\'.code -> {
                    val e = byteAt(cur.pos)
                    cur.pos++
                    when (e) {
                        'n'.code -> out.add('\n'.code.toByte())
                        'r'.code -> out.add('\r'.code.toByte())
                        't'.code -> out.add('\t'.code.toByte())
                        'b'.code -> out.add('\b'.code.toByte())
                        'f'.code -> out.add(12)
                        '('.code, ')'.code, '\\'.code -> out.add(e.toByte())
                        '\r'.code -> if (byteAt(cur.pos) == '\n'.code) cur.pos++ // line continuation
                        '\n'.code -> {}
                        in '0'.code..'7'.code -> {
                            var value = e - '0'.code
                            var digits = 1
                            while (digits < 3 && byteAt(cur.pos) in '0'.code..'7'.code) {
                                value = value * 8 + (byteAt(cur.pos) - '0'.code)
                                cur.pos++
                                digits++
                            }
                            out.add(value.toByte())
                        }

                        else -> out.add(e.toByte()) // unknown escape: drop the backslash
                    }
                }

                '('.code -> {
                    depth++
                    out.add(c.toByte())
                }

                ')'.code -> {
                    depth--
                    if (depth == 0) break
                    out.add(c.toByte())
                }

                else -> out.add(c.toByte())
            }
        }
        return CosString(out.toByteArray(), hex = false)
    }

    private fun parseHexString(cur: Cursor): CosString {
        cur.pos++ // '<'
        val out = ArrayList<Byte>(32)
        var high = -1
        while (true) {
            val c = byteAt(cur.pos)
            if (c < 0) throw PdfParseException("unterminated hex string")
            cur.pos++
            if (c == '>'.code) break
            val digit = Character.digit(c, 16)
            if (digit < 0) continue
            if (high < 0) high = digit
            else {
                out.add(((high shl 4) or digit).toByte())
                high = -1
            }
        }
        if (high >= 0) out.add((high shl 4).toByte())
        return CosString(out.toByteArray(), hex = true)
    }

    private fun parseArray(cur: Cursor): CosArray {
        cur.pos++ // '['
        val array = CosArray()
        while (true) {
            skipWs(cur)
            if (byteAt(cur.pos) == ']'.code) {
                cur.pos++
                return array
            }
            array.items.add(parseObject(cur))
        }
    }

    private fun parseDict(cur: Cursor): CosDict {
        cur.pos += 2 // '<<'
        val dict = CosDict()
        while (true) {
            skipWs(cur)
            if (byteAt(cur.pos) == '>'.code && byteAt(cur.pos + 1) == '>'.code) {
                cur.pos += 2
                return dict
            }
            val key = parseObject(cur) as? CosName
                ?: throw PdfParseException("dictionary key is not a name at ${cur.pos}")
            dict[key.name] = parseObject(cur)
        }
    }

    private fun parseNumberOrRef(cur: Cursor): CosObject {
        val first = parseNumber(cur)
        if (first !is CosInt || first.value < 0) return first
        // "num gen R" lookahead
        val save = cur.pos
        skipWs(cur)
        val c = byteAt(cur.pos)
        if (c in '0'.code..'9'.code) {
            val second = parseNumber(cur)
            if (second is CosInt) {
                skipWs(cur)
                if (byteAt(cur.pos) == 'R'.code) {
                    val after = byteAt(cur.pos + 1)
                    if (after < 0 || isWhite(after) || isDelim(after)) {
                        cur.pos++
                        return CosRef(first.value.toInt(), second.value.toInt())
                    }
                }
            }
        }
        cur.pos = save
        return first
    }

    private fun parseNumber(cur: Cursor): CosObject {
        skipWs(cur)
        val sb = StringBuilder()
        var isReal = false
        while (true) {
            val c = byteAt(cur.pos)
            if (c == '+'.code || c == '-'.code || c in '0'.code..'9'.code) {
                sb.append(c.toChar())
            } else if (c == '.'.code) {
                isReal = true
                sb.append('.')
            } else break
            cur.pos++
        }
        if (sb.isEmpty()) throw PdfParseException("expected number at ${cur.pos}")
        return if (isReal) CosReal(sb.toString())
        else CosInt(sb.toString().toLong())
    }

    /** Parses "num gen obj ... endobj" at [offset], including stream data. */
    private fun parseIndirectObjectAt(offset: Long, expectedNum: Int): CosObject {
        val cur = Cursor(offset)
        val num = (parseNumber(cur) as? CosInt)?.value?.toInt()
            ?: throw PdfParseException("bad object header at $offset")
        parseNumber(cur) // generation
        expectKeyword(cur, "obj")
        if (num != expectedNum) throw PdfParseException("object $expectedNum not found at $offset (found $num)")
        val obj = parseObject(cur)
        skipWs(cur)
        if (obj is CosDict && byteAt(cur.pos) == 's'.code) {
            val save = cur.pos
            if (readKeyword(cur) == "stream") {
                // EOL after 'stream': CRLF or LF
                if (byteAt(cur.pos) == '\r'.code) cur.pos++
                if (byteAt(cur.pos) == '\n'.code) cur.pos++
                val dataStart = cur.pos
                val length = (resolve(obj["Length"]) as? CosInt)?.value?.toInt()
                    ?: throw PdfParseException("stream without valid /Length at $offset")
                var dataLength = length
                cur.pos = dataStart + length
                skipWs(cur)
                if (readKeyword(cur) != "endstream") {
                    // tolerate a wrong /Length: scan forward for "endstream"
                    val found = scanForEndstream(dataStart)
                        ?: throw PdfParseException("endstream not found for object $num")
                    dataLength = (found - dataStart).toInt()
                }
                return CosStream(obj, CosStream.RawData.InFile(this, dataStart, dataLength))
            }
            cur.pos = save
        }
        return obj
    }

    private fun scanForEndstream(from: Long): Long? {
        val marker = "endstream".toByteArray()
        var pos = from
        outer@ while (pos < size - marker.size) {
            for (i in marker.indices) {
                if (byteAt(pos + i) != marker[i].toInt()) {
                    pos++
                    continue@outer
                }
            }
            // trim the EOL preceding the marker
            var end = pos
            if (byteAt(end - 1) == '\n'.code) end--
            if (byteAt(end - 1) == '\r'.code) end--
            return end
        }
        return null
    }

    // endregion

    // region object resolution

    fun getObject(num: Int): CosObject {
        objectCache[num]?.let { return it }
        val entry = xref[num] ?: return CosNull
        val obj = when (entry) {
            is XrefEntry.Regular -> parseIndirectObjectAt(entry.offset, num)
            is XrefEntry.InObjectStream ->
                loadObjectStream(entry.streamObjNum)[entry.index] ?: CosNull
        }
        objectCache[num] = obj
        return obj
    }

    fun resolve(obj: CosObject?): CosObject? = when (obj) {
        null -> null
        is CosRef -> getObject(obj.num)
        else -> obj
    }

    fun resolveDict(obj: CosObject?): CosDict? = when (val r = resolve(obj)) {
        is CosDict -> r
        is CosStream -> r.dict
        else -> null
    }

    fun generationOf(num: Int): Int =
        (xref[num] as? XrefEntry.Regular)?.gen ?: 0

    private fun loadObjectStream(streamObjNum: Int): Map<Int, CosObject> {
        objectStreamCache[streamObjNum]?.let { return it }
        val stream = getObject(streamObjNum) as? CosStream
            ?: throw PdfParseException("object stream $streamObjNum missing")
        val data = decodeStream(stream)
        val count = (resolve(stream.dict["N"]) as? CosInt)?.value?.toInt()
            ?: throw PdfParseException("object stream without /N")
        val first = (resolve(stream.dict["First"]) as? CosInt)?.value?.toInt()
            ?: throw PdfParseException("object stream without /First")
        val inner = PdfFile(ByteBuffer.wrap(data))
        val headerCur = Cursor(0)
        val members = HashMap<Int, CosObject>(count)
        val pairs = ArrayList<Pair<Int, Int>>(count) // objNum to relative offset
        repeat(count) {
            val objNum = (inner.parseNumber(headerCur) as CosInt).value.toInt()
            val relOffset = (inner.parseNumber(headerCur) as CosInt).value.toInt()
            pairs.add(objNum to relOffset)
        }
        for ((index, pair) in pairs.withIndex()) {
            val cur = Cursor((first + pair.second).toLong())
            members[index] = inner.parseObject(cur)
            // also cache by object number for direct lookups
            objectCache.putIfAbsent(pair.first, members[index]!!)
        }
        objectStreamCache[streamObjNum] = members
        return members
    }

    // endregion

    // region stream decoding (FlateDecode + PNG predictors; enough for xref/object streams)

    fun decodeStream(stream: CosStream): ByteArray {
        var data = stream.readRaw()
        val filters = when (val f = resolve(stream.dict["Filter"])) {
            null -> emptyList()
            is CosName -> listOf(f.name)
            is CosArray -> f.items.map { (resolve(it) as? CosName)?.name ?: "?" }
            else -> throw PdfParseException("bad /Filter")
        }
        val parmsList: List<CosDict?> = when (val p = resolve(stream.dict["DecodeParms"])) {
            null -> List(filters.size) { null }
            is CosDict -> listOf(p)
            is CosArray -> p.items.map { resolve(it) as? CosDict }
            else -> List(filters.size) { null }
        }
        filters.forEachIndexed { i, filter ->
            data = when (filter) {
                "FlateDecode", "Fl" -> {
                    val inflated = inflate(data)
                    applyPredictor(inflated, parmsList.getOrNull(i))
                }

                else -> throw PdfParseException("unsupported stream filter $filter")
            }
        }
        return data
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = java.io.ByteArrayOutputStream(data.size * 4)
        val chunk = ByteArray(16 * 1024)
        try {
            while (!inflater.finished()) {
                val n = inflater.inflate(chunk)
                if (n == 0 && inflater.needsInput()) break
                out.write(chunk, 0, n)
            }
        } finally {
            inflater.end()
        }
        return out.toByteArray()
    }

    private fun applyPredictor(data: ByteArray, parms: CosDict?): ByteArray {
        val predictor = ((parms?.get("Predictor") as? CosInt)?.value ?: 1L).toInt()
        if (predictor < 10) return data // 1 = none, 2 = TIFF (unused for xref)
        val columns = ((parms?.get("Columns") as? CosInt)?.value ?: 1L).toInt()
        val colors = ((parms?.get("Colors") as? CosInt)?.value ?: 1L).toInt()
        val bpc = ((parms?.get("BitsPerComponent") as? CosInt)?.value ?: 8L).toInt()
        val bytesPerPixel = maxOf(1, colors * bpc / 8)
        val rowLength = columns * colors * bpc / 8
        val rows = data.size / (rowLength + 1)
        val out = ByteArray(rows * rowLength)
        var prevRow = ByteArray(rowLength)
        for (r in 0 until rows) {
            val filterType = data[r * (rowLength + 1)].toInt() and 0xff
            val row = data.copyOfRange(r * (rowLength + 1) + 1, (r + 1) * (rowLength + 1))
            when (filterType) {
                0 -> {}
                1 -> for (i in bytesPerPixel until rowLength) {
                    row[i] = (row[i] + row[i - bytesPerPixel]).toByte()
                }

                2 -> for (i in 0 until rowLength) {
                    row[i] = (row[i] + prevRow[i]).toByte()
                }

                3 -> for (i in 0 until rowLength) {
                    val left = if (i >= bytesPerPixel) row[i - bytesPerPixel].toInt() and 0xff else 0
                    val up = prevRow[i].toInt() and 0xff
                    row[i] = (row[i] + ((left + up) / 2)).toByte()
                }

                4 -> for (i in 0 until rowLength) {
                    val a = if (i >= bytesPerPixel) row[i - bytesPerPixel].toInt() and 0xff else 0
                    val b = prevRow[i].toInt() and 0xff
                    val c = if (i >= bytesPerPixel) prevRow[i - bytesPerPixel].toInt() and 0xff else 0
                    val p = a + b - c
                    val pa = kotlin.math.abs(p - a)
                    val pb = kotlin.math.abs(p - b)
                    val pc = kotlin.math.abs(p - c)
                    val pred = if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
                    row[i] = (row[i] + pred).toByte()
                }

                else -> throw PdfParseException("unsupported PNG predictor filter $filterType")
            }
            row.copyInto(out, r * rowLength)
            prevRow = row
        }
        return out
    }

    // endregion

    // region xref parsing

    private fun parseXrefChain() {
        val tail = readBytes(maxOf(0, size - 2048), minOf(2048, size).toInt())
        val marker = "startxref"
        val tailStr = String(tail, Charsets.ISO_8859_1)
        val idx = tailStr.lastIndexOf(marker)
        if (idx < 0) throw PdfParseException("startxref not found")
        val cur = Cursor(maxOf(0, size - 2048) + idx + marker.length)
        lastStartXref = (parseNumber(cur) as? CosInt)?.value
            ?: throw PdfParseException("bad startxref value")

        var offset: Long? = lastStartXref
        var newestTrailer: CosDict? = null
        val visited = HashSet<Long>()
        while (offset != null && visited.add(offset)) {
            val sectionTrailer: CosDict
            val c = Cursor(offset)
            skipWs(c)
            if (byteAt(c.pos) == 'x'.code) {
                if (offset == lastStartXref) usesXrefStream = false
                sectionTrailer = parseClassicXrefAt(c)
                // hybrid file: the classic table's companion xref stream
                (sectionTrailer["XRefStm"] as? CosInt)?.let { parseXrefStreamAt(it.value) }
            } else {
                if (offset == lastStartXref) usesXrefStream = true
                sectionTrailer = parseXrefStreamAt(offset)
            }
            if (newestTrailer == null) newestTrailer = sectionTrailer
            (sectionTrailer["Size"] as? CosInt)?.let {
                maxObjectNumber = maxOf(maxObjectNumber, it.value.toInt() - 1)
            }
            offset = (sectionTrailer["Prev"] as? CosInt)?.value
        }
        trailer = newestTrailer ?: throw PdfParseException("no trailer found")
    }

    private fun parseClassicXrefAt(cur: Cursor): CosDict {
        expectKeyword(cur, "xref")
        while (true) {
            skipWs(cur)
            if (byteAt(cur.pos) == 't'.code) {
                expectKeyword(cur, "trailer")
                return parseObject(cur) as? CosDict
                    ?: throw PdfParseException("trailer is not a dictionary")
            }
            val start = (parseNumber(cur) as? CosInt)?.value?.toInt()
                ?: throw PdfParseException("bad xref subsection header")
            val count = (parseNumber(cur) as? CosInt)?.value?.toInt()
                ?: throw PdfParseException("bad xref subsection header")
            skipWs(cur)
            for (i in 0 until count) {
                val entry = String(readBytes(cur.pos, 18), Charsets.ISO_8859_1)
                val offset = entry.substring(0, 10).trim().toLong()
                val gen = entry.substring(11, 16).trim().toInt()
                val type = entry[17]
                val objNum = start + i
                if (type == 'n' && objNum !in xref) {
                    xref[objNum] = XrefEntry.Regular(offset, gen)
                }
                maxObjectNumber = maxOf(maxObjectNumber, objNum)
                cur.pos += 18
                // entries are 20 bytes, but tolerate 19-byte (single-EOL) writers
                while (isWhite(byteAt(cur.pos))) cur.pos++
            }
        }
    }

    private fun parseXrefStreamAt(offset: Long): CosDict {
        val cur = Cursor(offset)
        val num = (parseNumber(cur) as? CosInt)?.value?.toInt()
            ?: throw PdfParseException("bad xref stream header")
        val stream = parseIndirectObjectAt(offset, num) as? CosStream
            ?: throw PdfParseException("xref stream is not a stream")
        val dict = stream.dict
        val data = decodeStream(stream)
        val w = (dict["W"] as? CosArray)?.items?.map { (it as CosInt).value.toInt() }
            ?: throw PdfParseException("xref stream without /W")
        val sizeEntry = (dict["Size"] as? CosInt)?.value?.toInt()
            ?: throw PdfParseException("xref stream without /Size")
        val index = (dict["Index"] as? CosArray)?.items?.map { (it as CosInt).value.toInt() }
            ?: listOf(0, sizeEntry)
        val entryLength = w.sum()
        var pos = 0
        var i = 0
        while (i + 1 < index.size) {
            val start = index[i]
            val count = index[i + 1]
            for (j in 0 until count) {
                if (pos + entryLength > data.size) break
                var f = 0
                val fields = IntArray(3)
                for (k in 0 until 3) {
                    val width = w.getOrElse(k) { 0 }
                    var value = 0
                    repeat(width) {
                        value = (value shl 8) or (data[pos + f].toInt() and 0xff)
                        f++
                    }
                    fields[k] = if (width == 0 && k == 0) 1 else value // missing type = 1
                }
                pos += entryLength
                val objNum = start + j
                maxObjectNumber = maxOf(maxObjectNumber, objNum)
                if (objNum in xref) continue
                when (fields[0]) {
                    1 -> xref[objNum] = XrefEntry.Regular(fields[1].toLong(), fields[2])
                    2 -> xref[objNum] = XrefEntry.InObjectStream(fields[1], fields[2])
                    // 0 = free, ignore
                }
            }
            i += 2
        }
        return dict
    }

    // endregion

    companion object {
        fun open(file: File): PdfFile {
            val mapped = RandomAccessFile(file, "r").use { raf ->
                raf.channel.map(FileChannel.MapMode.READ_ONLY, 0, raf.length())
            }
            return PdfFile(mapped).apply { parseXrefChain() }
        }
    }
}
