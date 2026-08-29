package info.plateaukao.einkbro.unit

import info.plateaukao.einkbro.unit.FaviconCandidates.Candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FaviconCandidatesTest {

    @Test
    fun `parses icon links and resolves relative hrefs against the page`() {
        val html = """
            <html><head>
              <LINK REL="shortcut icon" HREF="/static/fav.ico">
              <link rel=icon type="image/png" sizes="32x32" href='img/32.png'>
              <link rel="apple-touch-icon" href="https://cdn.example.org/touch.png?v=2&amp;x=1">
              <link rel="stylesheet" href="/style.css">
            </head></html>
        """.trimIndent()
        val links = FaviconCandidates.parseIconLinks(html, "https://example.com/a/b/page.html")
        assertEquals(
            listOf(
                "https://example.com/static/fav.ico",
                "https://example.com/a/b/img/32.png",
                "https://cdn.example.org/touch.png?v=2&x=1",
            ),
            links.map { it.href },
        )
        assertEquals("32x32", links[1].sizes)
        assertEquals("apple-touch-icon", links[2].rel)
    }

    @Test
    fun `orders favicons nearest 48px first, touch icons after, favicon ico last`() {
        val candidates = listOf(
            Candidate("https://e.com/touch.png", rel = "apple-touch-icon", sizes = "180x180"),
            Candidate("https://e.com/16.png", sizes = "16x16"),
            Candidate("https://e.com/48.png", sizes = "48x48"),
            Candidate("https://e.com/any.ico"),
        )
        assertEquals(
            listOf(
                "https://e.com/48.png",
                "https://e.com/any.ico",
                "https://e.com/16.png",
                "https://e.com/touch.png",
                "https://e.com/favicon.ico",
            ),
            FaviconCandidates.orderedUrls(candidates, "https://e.com/some/page"),
        )
    }

    @Test
    fun `skips svg sources and non-http schemes but keeps data uris`() {
        val candidates = listOf(
            Candidate("https://e.com/icon.svg"),
            Candidate("https://e.com/vector", type = "image/svg+xml"),
            Candidate("data:image/svg+xml;base64,AAAA"),
            Candidate("data:image/png;base64,iVBORw0KGgo="),
            Candidate("file:///etc/passwd"),
        )
        assertEquals(
            listOf("data:image/png;base64,iVBORw0KGgo=", "https://e.com/favicon.ico"),
            FaviconCandidates.orderedUrls(candidates, "https://e.com/"),
        )
    }

    @Test
    fun `fallback keeps a non-default port and dedupes an explicit favicon ico`() {
        val candidates = listOf(Candidate("http://host:8080/favicon.ico"))
        assertEquals(
            listOf("http://host:8080/favicon.ico"),
            FaviconCandidates.orderedUrls(candidates, "http://host:8080/index.html"),
        )
    }

    @Test
    fun `parseProbe reads the JSON-encoded string returned by evaluateJavascript`() {
        val js = """{"host":"example.com","icons":[{"href":"https://example.com/i.png","rel":"icon","sizes":"32x32","type":""}]}"""
        val encoded = "\"" + js.replace("\"", "\\\"") + "\""
        val probe = FaviconFetcher.parseProbe(encoded)!!
        assertEquals("example.com", probe.host)
        assertEquals(listOf(Candidate("https://example.com/i.png", "icon", "32x32", "")), probe.candidates)
        assertNull(FaviconFetcher.parseProbe("null"))
        assertNull(FaviconFetcher.parseProbe(null))
    }
}
