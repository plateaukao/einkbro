package info.plateaukao.einkbro.unit

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadHelperTest {

    @Test
    fun `guessFilename uses current page url for blob downloads`() {
        val fileName = DownloadHelper.guessFilename(
            url = "blob:https://github.com/d1ecbcaa-07ea-4e7c-9031-d44d75b0cb80",
            contentDisposition = "",
            mimeType = "text/plain",
            fallbackUrl = "https://github.com/plateaukao/einkbro/blob/main/CODE_OF_CONDUCT.md",
        )

        assertEquals("CODE_OF_CONDUCT.md", fileName)
    }

    @Test
    fun `guessFilename prefers content disposition over fallback page url`() {
        val fileName = DownloadHelper.guessFilename(
            url = "blob:https://github.com/d1ecbcaa-07ea-4e7c-9031-d44d75b0cb80",
            contentDisposition = "attachment; filename*=UTF-8''README.md",
            mimeType = "text/plain",
            fallbackUrl = "https://github.com/plateaukao/einkbro/blob/main/CODE_OF_CONDUCT.md",
        )

        assertEquals("README.md", fileName)
    }
}
