package info.plateaukao.einkbro.unit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GithubUtilTest {

    @Test
    fun `branch blob page resolves to raw url`() {
        assertEquals(
            "https://github.com/plateaukao/einkbro/raw/main/CODE_OF_CONDUCT.md",
            GithubUtil.rawUrlForBlobPage(
                "https://github.com/plateaukao/einkbro/blob/main/CODE_OF_CONDUCT.md"
            )
        )
    }

    @Test
    fun `tag blob page resolves to raw url`() {
        assertEquals(
            "https://github.com/owner/repo/raw/v1.2.3/path/to/file.txt",
            GithubUtil.rawUrlForBlobPage(
                "https://github.com/owner/repo/blob/v1.2.3/path/to/file.txt"
            )
        )
    }

    @Test
    fun `commit sha blob page resolves to raw url`() {
        assertEquals(
            "https://github.com/owner/repo/raw/abc1234/dir/file.go",
            GithubUtil.rawUrlForBlobPage(
                "https://github.com/owner/repo/blob/abc1234/dir/file.go"
            )
        )
    }

    @Test
    fun `non github url is rejected`() {
        assertNull(GithubUtil.rawUrlForBlobPage("https://example.com/blob/main/foo"))
    }

    @Test
    fun `non blob github url is rejected`() {
        assertNull(GithubUtil.rawUrlForBlobPage("https://github.com/owner/repo/tree/main/dir"))
    }

    @Test
    fun `null url returns null`() {
        assertNull(GithubUtil.rawUrlForBlobPage(null))
    }
}
