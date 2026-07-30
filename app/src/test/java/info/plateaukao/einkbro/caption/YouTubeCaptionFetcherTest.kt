package info.plateaukao.einkbro.caption

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeCaptionFetcherTest {

    @Test
    fun `extractVideoId handles watch urls`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeCaptionFetcher.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeCaptionFetcher.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ&t=42s")
        )
        assertEquals(
            "abc-_123XYZ",
            YouTubeCaptionFetcher.extractVideoId("https://youtube.com/watch?list=PL1&v=abc-_123XYZ")
        )
    }

    @Test
    fun `extractVideoId handles short and live urls`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeCaptionFetcher.extractVideoId("https://youtu.be/dQw4w9WgXcQ?t=10")
        )
        assertEquals(
            "shortId12345",
            YouTubeCaptionFetcher.extractVideoId("https://www.youtube.com/shorts/shortId12345")
        )
        assertEquals(
            "liveId678901",
            YouTubeCaptionFetcher.extractVideoId("https://www.youtube.com/live/liveId678901?feature=share")
        )
    }

    @Test
    fun `extractVideoId rejects non-video urls and malformed ids`() {
        assertNull(YouTubeCaptionFetcher.extractVideoId("https://www.youtube.com/"))
        assertNull(YouTubeCaptionFetcher.extractVideoId("https://www.youtube.com/watch"))
        assertNull(YouTubeCaptionFetcher.extractVideoId("https://www.youtube.com/feed/subscriptions"))
        assertNull(YouTubeCaptionFetcher.extractVideoId("https://www.youtube.com/@somechannel"))
        assertNull(YouTubeCaptionFetcher.extractVideoId("https://example.com/watch?v=dQw4w9WgXcQ"))
        // Ids that could break the InnerTube request body are refused outright.
        assertNull(YouTubeCaptionFetcher.extractVideoId("https://www.youtube.com/watch?v=a%22b%7D"))
        assertFalse(YouTubeCaptionFetcher.isVideoUrl(null))
        assertTrue(YouTubeCaptionFetcher.isVideoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `pickTrack prefers manual track in preferred language`() {
        val asrEn = YouTubeCaptionTrack("url-asr-en", "en", "asr")
        val manualJa = YouTubeCaptionTrack("url-ja", "ja", "")
        val manualEnUs = YouTubeCaptionTrack("url-en", "en-US", "")

        assertEquals(
            manualEnUs,
            YouTubeCaptionFetcher.pickTrack(listOf(asrEn, manualJa, manualEnUs), "en")
        )
        // No manual track in the preferred language: any manual track beats ASR.
        assertEquals(
            manualJa,
            YouTubeCaptionFetcher.pickTrack(listOf(asrEn, manualJa), "zh")
        )
        // Only ASR available: preferred language wins, then first.
        assertEquals(
            asrEn,
            YouTubeCaptionFetcher.pickTrack(listOf(YouTubeCaptionTrack("url", "ja", "asr"), asrEn), "en")
        )
        assertNull(YouTubeCaptionFetcher.pickTrack(emptyList(), "en"))
    }

    @Test
    fun `captionUrl forces json3 format and resolves relative urls`() {
        assertEquals(
            "https://www.youtube.com/api/timedtext?v=abc&lang=en&fmt=json3",
            YouTubeCaptionFetcher.captionUrl("https://www.youtube.com/api/timedtext?v=abc&lang=en")
        )
        assertEquals(
            "https://www.youtube.com/api/timedtext?v=abc&fmt=json3&lang=en",
            YouTubeCaptionFetcher.captionUrl("https://www.youtube.com/api/timedtext?v=abc&fmt=srv3&lang=en")
        )
        assertEquals(
            "https://www.youtube.com/api/timedtext?v=abc&fmt=json3",
            YouTubeCaptionFetcher.captionUrl("/api/timedtext?v=abc")
        )
    }
}
