package io.github.edsuns.adfilter

import io.github.edsuns.adfilter.impl.BinaryDataStore
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BinaryDataStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun store() = BinaryDataStore(File(tmp.root, "bin_data"))

    @Test
    fun roundTrip() {
        val store = store()
        val payload = ByteArray(70000) { (it % 251).toByte() }
        store.saveData("blob", payload)
        assertTrue(store.hasData("blob"))
        assertArrayEquals(payload, store.loadData("blob"))
    }

    @Test
    fun legacyFileWithoutHeaderPassesThrough() {
        val store = store()
        val legacy = "1,2,3 legacy native blob".toByteArray()
        File(File(tmp.root, "bin_data"), "old").writeBytes(legacy)
        assertArrayEquals(legacy, store.loadData("old"))
    }

    @Test
    fun truncatedFileIsDiscarded() {
        val store = store()
        val payload = ByteArray(50000) { it.toByte() }
        store.saveData("blob", payload)
        val file = File(File(tmp.root, "bin_data"), "blob")
        file.writeBytes(file.readBytes().copyOfRange(0, 12345))
        assertEquals(0, store.loadData("blob").size)
        assertFalse("corrupt file should be deleted", store.hasData("blob"))
    }

    @Test
    fun bitFlippedPayloadIsDiscarded() {
        val store = store()
        val payload = ByteArray(50000) { it.toByte() }
        store.saveData("blob", payload)
        val file = File(File(tmp.root, "bin_data"), "blob")
        val bytes = file.readBytes()
        bytes[bytes.size / 2] = (bytes[bytes.size / 2] + 1).toByte()
        file.writeBytes(bytes)
        assertEquals(0, store.loadData("blob").size)
        assertFalse(store.hasData("blob"))
    }

    @Test
    fun saveLeavesNoTempFileBehind() {
        val store = store()
        store.saveData("blob", byteArrayOf(1, 2, 3))
        assertFalse(File(File(tmp.root, "bin_data"), "blob.tmp").exists())
    }
}
