package info.plateaukao.einkbro.activity

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAppOriginTest {
    private fun uri(scheme: String = "https", host: String = "example.com", port: Int = -1): Uri {
        val uri = mockk<Uri>()
        every { uri.scheme } returns scheme
        every { uri.host } returns host
        every { uri.port } returns port
        return uri
    }

    @Test
    fun `default ports and host casing belong to the same origin`() {
        assertTrue(uri().isSameOriginAs(uri(host = "EXAMPLE.COM", port = 443)))
        assertTrue(uri(scheme = "http").isSameOriginAs(uri(scheme = "http", port = 80)))
    }

    @Test
    fun `scheme host and non-default port changes leave the installed origin`() {
        assertFalse(uri().isSameOriginAs(uri(scheme = "http")))
        assertFalse(uri().isSameOriginAs(uri(host = "other.example.com")))
        assertFalse(uri().isSameOriginAs(uri(host = "example.com.evil.test")))
        assertFalse(uri().isSameOriginAs(uri(port = 8443)))
    }
}
