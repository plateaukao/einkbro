package info.plateaukao.einkbro.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageRequestClassifierTest {

    @Test
    fun `identifies extensionless image CDN requests from Accept header`() {
        assertTrue(
            ImageRequestClassifier.isNetworkImage(
                url = "https://cdn.example.com/media/12345",
                requestHeaders = mapOf("Accept" to "image/avif,image/webp,image/*,*/*;q=0.8"),
                isMainFrame = false,
            )
        )
    }

    @Test
    fun `uses Sec Fetch destination as the authoritative signal`() {
        assertTrue(
            ImageRequestClassifier.isNetworkImage(
                url = "https://cdn.example.com/media/12345",
                requestHeaders = mapOf("Sec-Fetch-Dest" to "image"),
                isMainFrame = false,
            )
        )
        assertFalse(
            ImageRequestClassifier.isNetworkImage(
                url = "https://example.com/api/data.png",
                requestHeaders = mapOf("Sec-Fetch-Dest" to "empty"),
                isMainFrame = false,
            )
        )
    }

    @Test
    fun `identifies image extension when WebView omits Accept header`() {
        assertTrue(
            ImageRequestClassifier.isNetworkImage(
                url = "https://example.com/images/cover.PNG?width=800",
                requestHeaders = emptyMap(),
                isMainFrame = false,
            )
        )
    }

    @Test
    fun `does not replace document navigation or non-network resources`() {
        assertFalse(
            ImageRequestClassifier.isNetworkImage(
                url = "https://example.com/image.jpg",
                requestHeaders = mapOf("Accept" to "image/*"),
                isMainFrame = true,
            )
        )
        assertFalse(
            ImageRequestClassifier.isNetworkImage(
                url = "data:image/png;base64,abc",
                requestHeaders = mapOf("Accept" to "image/*"),
                isMainFrame = false,
            )
        )
    }
}
