package info.plateaukao.einkbro.unit.pdf

import java.io.File
import java.io.OutputStream

/**
 * The two save-as-PDF post-processing operations, built on incremental
 * updates: add a TOC (outline) entry to a document, and append another
 * document's pages (with a TOC entry for the first appended page).
 */
internal object PdfTocEditor {

    /**
     * Copies [source] to [out], appending an outline entry titled [tocTitle]
     * that points at the first page. A blank title degrades to a plain copy.
     */
    fun writeWithToc(source: File, out: OutputStream, tocTitle: String?): Boolean {
        val doc = PdfFile.open(source)
        if (doc.isEncrypted) return false
        val update = PdfIncrementalUpdate(doc)
        if (!tocTitle.isNullOrBlank()) {
            val firstPage = firstPageRef(doc) ?: return false
            addOutlineEntry(doc, update, tocTitle, firstPage)
        }
        if (update.isEmpty()) {
            doc.copyAllTo(out)
        } else {
            update.writeTo(out)
        }
        return true
    }

    /**
     * Copies [existing] to [out] with [newPages]'s pages appended to its page
     * tree and an outline entry titled [tocTitle] pointing at the first
     * appended page.
     */
    fun appendWithToc(existing: File, newPages: File, out: OutputStream, tocTitle: String?): Boolean {
        val dest = PdfFile.open(existing)
        if (dest.isEncrypted) return false
        val src = PdfFile.open(newPages)
        if (src.isEncrypted) return false

        val update = PdfIncrementalUpdate(dest)

        val destRoot = dest.resolveDict(dest.trailer["Root"]) ?: return false
        val destPagesRef = destRoot["Pages"] as? CosRef ?: return false
        val destPages = dest.resolveDict(destPagesRef) ?: return false

        val copiedPageRefs = copyPages(src, update, destPagesRef)
        if (copiedPageRefs.isEmpty()) return false

        // updated page-tree root: appended kids, bumped count
        val newPagesDict = destPages.copy()
        val kids = CosArray()
        (dest.resolve(destPages["Kids"]) as? CosArray)?.items?.let { kids.items.addAll(it) }
        kids.items.addAll(copiedPageRefs)
        newPagesDict["Kids"] = kids
        val oldCount = (dest.resolve(destPages["Count"]) as? CosInt)?.value ?: 0L
        newPagesDict["Count"] = CosInt(oldCount + copiedPageRefs.size)
        update.put(CosRef(destPagesRef.num, dest.generationOf(destPagesRef.num)), newPagesDict)

        if (!tocTitle.isNullOrBlank()) {
            addOutlineEntry(dest, update, tocTitle, copiedPageRefs.first())
        }

        update.writeTo(out)
        return true
    }

    // region page tree

    private fun firstPageRef(doc: PdfFile): CosRef? {
        val root = doc.resolveDict(doc.trailer["Root"]) ?: return null
        var nodeRef = root["Pages"] as? CosRef ?: return null
        while (true) {
            val node = doc.resolveDict(nodeRef) ?: return null
            if ((node["Type"] as? CosName)?.name == "Page") return nodeRef
            val kids = doc.resolve(node["Kids"]) as? CosArray ?: return null
            nodeRef = kids.items.firstOrNull() as? CosRef ?: return null
        }
    }

    /** Page leaf refs of [doc] in document order, with the attributes a leaf inherits. */
    private fun collectPages(doc: PdfFile): List<Pair<CosRef, CosDict>> {
        val root = doc.resolveDict(doc.trailer["Root"]) ?: return emptyList()
        val pagesRef = root["Pages"] as? CosRef ?: return emptyList()
        val result = ArrayList<Pair<CosRef, CosDict>>()
        fun walk(ref: CosRef, inherited: CosDict) {
            val node = doc.resolveDict(ref) ?: return
            if ((node["Type"] as? CosName)?.name == "Page") {
                result.add(ref to inherited)
                return
            }
            val nextInherited = inherited.copy()
            for (key in INHERITABLE_PAGE_KEYS) {
                node[key]?.let { nextInherited[key] = it }
            }
            (doc.resolve(node["Kids"]) as? CosArray)?.items?.forEach { kid ->
                (kid as? CosRef)?.let { walk(it, nextInherited) }
            }
        }
        walk(pagesRef, CosDict())
        return result
    }

    /**
     * Deep-copies every page of [src] (content, resources, annotations…) into
     * [update] with fresh object numbers, re-parented onto [destPagesRef] and
     * with inherited attributes materialized onto each page.
     */
    private fun copyPages(
        src: PdfFile,
        update: PdfIncrementalUpdate,
        destPagesRef: CosRef,
    ): List<CosRef> {
        val memo = HashMap<Int, CosRef>()

        fun copyValue(obj: CosObject): CosObject = when (obj) {
            is CosRef -> memo[obj.num] ?: run {
                val newRef = update.allocate()
                memo[obj.num] = newRef
                update.put(newRef, copyValue(src.getObject(obj.num)))
                newRef
            }

            is CosArray -> CosArray(obj.items.mapTo(mutableListOf()) { copyValue(it) })
            is CosDict -> CosDict(
                obj.entries.entries.associateTo(LinkedHashMap()) { it.key to copyValue(it.value) }
            )

            is CosStream -> CosStream(
                CosDict(
                    obj.dict.entries.entries.associateTo(LinkedHashMap()) { it.key to copyValue(it.value) }
                ),
                obj.raw,
            )

            else -> obj
        }

        return collectPages(src).map { (pageRef, inherited) ->
            val page = src.resolveDict(pageRef) ?: throw PdfParseException("page ${pageRef.num} missing")
            val newRef = update.allocate()
            memo[pageRef.num] = newRef
            val copied = CosDict()
            copied["Type"] = CosName("Page")
            copied["Parent"] = destPagesRef
            for ((key, value) in page.entries) {
                if (key == "Type" || key == "Parent") continue
                copied[key] = copyValue(value)
            }
            for (key in INHERITABLE_PAGE_KEYS) {
                if (copied[key] == null) inherited[key]?.let { copied[key] = copyValue(it) }
            }
            update.put(newRef, copied)
            newRef
        }
    }

    private val INHERITABLE_PAGE_KEYS = listOf("Resources", "MediaBox", "CropBox", "Rotate")

    // endregion

    // region outline

    /** Appends an outline (TOC) item titled [title] targeting [pageRef]. */
    private fun addOutlineEntry(
        doc: PdfFile,
        update: PdfIncrementalUpdate,
        title: String,
        pageRef: CosRef,
    ) {
        val rootRef = doc.trailer["Root"] as? CosRef ?: throw PdfParseException("no /Root reference")
        val root = doc.resolveDict(rootRef) ?: throw PdfParseException("no catalog")
        val destination = CosArray(mutableListOf(pageRef, CosName("FitH"), CosNull))
        val itemRef = update.allocate()
        val item = CosDict()
        item["Title"] = textString(title)
        item["Dest"] = destination

        val outlineRef = root["Outlines"] as? CosRef
        val outline = outlineRef?.let { doc.resolveDict(it) }
        if (outlineRef == null || outline == null) {
            val newOutlineRef = update.allocate()
            val newOutline = CosDict()
            newOutline["Type"] = CosName("Outlines")
            newOutline["First"] = itemRef
            newOutline["Last"] = itemRef
            newOutline["Count"] = CosInt(1)
            item["Parent"] = newOutlineRef
            update.put(newOutlineRef, newOutline)
            update.put(itemRef, item)
            val newRoot = root.copy()
            newRoot["Outlines"] = newOutlineRef
            update.put(CosRef(rootRef.num, doc.generationOf(rootRef.num)), newRoot)
        } else {
            item["Parent"] = outlineRef
            val lastRef = outline["Last"] as? CosRef
            val newOutline = outline.copy()
            if (lastRef != null) {
                item["Prev"] = lastRef
                val lastItem = doc.resolveDict(lastRef)
                if (lastItem != null) {
                    val newLast = lastItem.copy()
                    newLast["Next"] = itemRef
                    update.put(CosRef(lastRef.num, doc.generationOf(lastRef.num)), newLast)
                }
            }
            if (newOutline["First"] == null) newOutline["First"] = itemRef
            newOutline["Last"] = itemRef
            val count = (outline["Count"] as? CosInt)?.value
            if (count != null && count >= 0) newOutline["Count"] = CosInt(count + 1)
            update.put(itemRef, item)
            update.put(CosRef(outlineRef.num, doc.generationOf(outlineRef.num)), newOutline)
        }
    }

    /** ASCII stays a plain literal; anything else becomes UTF-16BE with a BOM. */
    private fun textString(text: String): CosString =
        if (text.all { it.code in 0x20..0x7e }) {
            CosString(text.toByteArray(Charsets.US_ASCII), hex = false)
        } else {
            CosString(byteArrayOf(0xfe.toByte(), 0xff.toByte()) + text.toByteArray(Charsets.UTF_16BE), hex = true)
        }

    // endregion
}
