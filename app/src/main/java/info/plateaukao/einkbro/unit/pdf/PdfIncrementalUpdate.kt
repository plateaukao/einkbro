package info.plateaukao.einkbro.unit.pdf

import java.io.OutputStream

/**
 * Builds a PDF incremental update: the original file's bytes are copied
 * verbatim and new/updated objects plus a fresh cross-reference section are
 * appended. Anything the parser didn't understand is preserved untouched,
 * which is why this replaces a full rewrite (the pdfbox approach) safely.
 */
internal class PdfIncrementalUpdate(private val source: PdfFile) {

    // object number -> (generation, object), in write order
    private val newObjects = LinkedHashMap<Int, Pair<Int, CosObject>>()
    private var nextObjNum = source.maxObjectNumber + 1

    fun allocate(): CosRef = CosRef(nextObjNum++, 0)

    /** Adds a new object, or an updated body for an existing object number. */
    fun put(ref: CosRef, obj: CosObject) {
        newObjects[ref.num] = ref.gen to obj
    }

    fun isEmpty(): Boolean = newObjects.isEmpty()

    fun writeTo(rawOut: OutputStream) {
        val out = CountingOutputStream(rawOut)
        source.copyAllTo(out)
        if (!source.endsWithNewline()) out.write('\n'.code)

        val offsets = HashMap<Int, Long>()
        // Reserve the xref-stream object number up front (its own entry must be
        // in the table it writes).
        val xrefStreamRef = if (source.usesXrefStream) allocate() else null

        for ((num, genAndObj) in newObjects) {
            offsets[num] = out.count
            writeIndirectObject(out, num, genAndObj.first, genAndObj.second)
        }

        val xrefOffset = out.count
        if (xrefStreamRef != null) {
            offsets[xrefStreamRef.num] = xrefOffset
            writeXrefStream(out, xrefStreamRef, offsets)
        } else {
            writeClassicXref(out, offsets)
        }
        out.writeAscii("startxref\n$xrefOffset\n%%EOF\n")
        out.flush()
    }

    // region xref writing

    private fun entryRuns(offsets: Map<Int, Long>): List<Pair<Int, List<Int>>> {
        val nums = offsets.keys.sorted()
        val runs = ArrayList<Pair<Int, MutableList<Int>>>()
        for (num in nums) {
            val last = runs.lastOrNull()
            if (last != null && last.first + last.second.size == num) {
                last.second.add(num)
            } else {
                runs.add(num to mutableListOf(num))
            }
        }
        return runs
    }

    private fun writeClassicXref(out: CountingOutputStream, offsets: Map<Int, Long>) {
        out.writeAscii("xref\n")
        for ((start, nums) in entryRuns(offsets)) {
            out.writeAscii("$start ${nums.size}\n")
            for (num in nums) {
                val gen = newObjects[num]?.first ?: 0
                out.writeAscii(String.format(java.util.Locale.ROOT, "%010d %05d n\r\n", offsets[num], gen))
            }
        }
        val trailer = CosDict()
        trailer["Size"] = CosInt(nextObjNum.toLong())
        trailer["Prev"] = CosInt(source.lastStartXref)
        source.trailer["Root"]?.let { trailer["Root"] = it }
        source.trailer["Info"]?.let { trailer["Info"] = it }
        source.trailer["ID"]?.let { trailer["ID"] = it }
        out.writeAscii("trailer\n")
        serialize(out, trailer)
        out.write('\n'.code)
    }

    private fun writeXrefStream(
        out: CountingOutputStream,
        selfRef: CosRef,
        offsets: Map<Int, Long>,
    ) {
        // W = [1 4 2]: type byte, 4-byte offset, 2-byte generation
        val runs = entryRuns(offsets)
        val data = java.io.ByteArrayOutputStream()
        val index = CosArray()
        for ((start, nums) in runs) {
            index.items.add(CosInt(start.toLong()))
            index.items.add(CosInt(nums.size.toLong()))
            for (num in nums) {
                val offset = offsets[num] ?: 0L
                val gen = newObjects[num]?.first ?: 0
                data.write(1)
                data.write(((offset shr 24) and 0xff).toInt())
                data.write(((offset shr 16) and 0xff).toInt())
                data.write(((offset shr 8) and 0xff).toInt())
                data.write((offset and 0xff).toInt())
                data.write((gen shr 8) and 0xff)
                data.write(gen and 0xff)
            }
        }
        val bytes = data.toByteArray()
        val dict = CosDict()
        dict["Type"] = CosName("XRef")
        dict["Size"] = CosInt(nextObjNum.toLong())
        dict["Index"] = index
        dict["W"] = CosArray(mutableListOf(CosInt(1), CosInt(4), CosInt(2)))
        dict["Prev"] = CosInt(source.lastStartXref)
        dict["Length"] = CosInt(bytes.size.toLong())
        source.trailer["Root"]?.let { dict["Root"] = it }
        source.trailer["Info"]?.let { dict["Info"] = it }
        source.trailer["ID"]?.let { dict["ID"] = it }
        writeIndirectObject(out, selfRef.num, selfRef.gen, CosStream(dict, CosStream.RawData.InMemory(bytes)))
    }

    // endregion

    // region serialization

    private fun writeIndirectObject(out: CountingOutputStream, num: Int, gen: Int, obj: CosObject) {
        out.writeAscii("$num $gen obj\n")
        serialize(out, obj)
        out.writeAscii("\nendobj\n")
    }

    private fun serialize(out: CountingOutputStream, obj: CosObject) {
        when (obj) {
            is CosNull -> out.writeAscii("null")
            is CosBool -> out.writeAscii(if (obj.value) "true" else "false")
            is CosInt -> out.writeAscii(obj.value.toString())
            is CosReal -> out.writeAscii(obj.raw)
            is CosRef -> out.writeAscii("${obj.num} ${obj.gen} R")
            is CosName -> writeName(out, obj)
            is CosString -> writeString(out, obj)
            is CosArray -> {
                out.write('['.code)
                obj.items.forEachIndexed { i, item ->
                    if (i > 0) out.write(' '.code)
                    serialize(out, item)
                }
                out.write(']'.code)
            }

            is CosDict -> writeDict(out, obj)
            is CosStream -> {
                val dict = obj.dict.copy()
                dict["Length"] = CosInt(obj.rawLength.toLong())
                writeDict(out, dict)
                out.writeAscii("\nstream\n")
                obj.copyRawTo(out)
                out.writeAscii("\nendstream")
            }
        }
    }

    private fun writeDict(out: CountingOutputStream, dict: CosDict) {
        out.writeAscii("<<")
        for ((key, value) in dict.entries) {
            writeName(out, CosName(key))
            out.write(' '.code)
            serialize(out, value)
        }
        out.writeAscii(">>")
    }

    private fun writeName(out: CountingOutputStream, name: CosName) {
        out.write('/'.code)
        for (ch in name.name) {
            val c = ch.code
            if (c in 0x21..0x7e && c != '#'.code && c != '/'.code && c != '('.code &&
                c != ')'.code && c != '<'.code && c != '>'.code && c != '['.code &&
                c != ']'.code && c != '{'.code && c != '}'.code && c != '%'.code
            ) {
                out.write(c)
            } else {
                out.writeAscii(String.format(java.util.Locale.ROOT, "#%02X", c and 0xff))
            }
        }
    }

    private fun writeString(out: CountingOutputStream, str: CosString) {
        if (str.hex) {
            out.write('<'.code)
            for (b in str.bytes) {
                out.writeAscii(String.format(java.util.Locale.ROOT, "%02X", b.toInt() and 0xff))
            }
            out.write('>'.code)
        } else {
            out.write('('.code)
            for (b in str.bytes) {
                when (val c = b.toInt() and 0xff) {
                    '('.code, ')'.code, '\\'.code -> {
                        out.write('\\'.code)
                        out.write(c)
                    }

                    '\r'.code -> out.writeAscii("\\r")
                    '\n'.code -> out.writeAscii("\\n")
                    else -> out.write(c)
                }
            }
            out.write(')'.code)
        }
    }

    // endregion

    private class CountingOutputStream(private val delegate: OutputStream) : OutputStream() {
        var count: Long = 0
            private set

        override fun write(b: Int) {
            delegate.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            count += len
        }

        override fun flush() = delegate.flush()

        fun writeAscii(s: String) = write(s.toByteArray(Charsets.ISO_8859_1))
    }
}
