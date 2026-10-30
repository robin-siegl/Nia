package dev.robinsiegl.nia

import dev.robinsiegl.nia.model.*
import dev.robinsiegl.nia.platform.ReleaseRepository
import dev.robinsiegl.nia.platform.UpdateCheckException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

class AppReleaseTest {
    private val release = """{"tag_name":"v1.3.0","draft":false,"prerelease":false,
        "html_url":"https://github.com/robin-siegl/Nia/releases/tag/v1.3.0",
        "assets":[{"name":"nia-1.3.0.apk","state":"uploaded",
        "browser_download_url":"https://github.com/robin-siegl/Nia/releases/download/v1.3.0/nia-1.3.0.apk"}]}"""

    @Test fun `versions compare numerically rather than alphabetically`() {
        assertTrue(ReleaseVersion.parse("1.10.0")!! > ReleaseVersion.parse("1.9.99")!!)
        assertTrue(ReleaseVersion.parse("2.0.0")!! > ReleaseVersion.parse("1.999.999")!!)
        assertEquals(ReleaseVersion(1, 3, 0), ReleaseVersion.parse("v1.3.0"))
        listOf("1.3", "01.3.0", "1.3.0-rc1", "1.3.0+extra", "3.0.0.1", "999999999999999.0.0").forEach {
            assertNull(it, ReleaseVersion.parse(it))
        }
    }
    @Test fun `published stable release yields its exact APK and page`() {
        val parsed = AppRelease.parse(release)
        assertEquals(ReleaseVersion(1, 3, 0), parsed.version)
        assertEquals("${AppRelease.REPOSITORY}/releases/tag/v1.3.0", parsed.pageUrl)
        assertEquals("${AppRelease.REPOSITORY}/releases/download/v1.3.0/nia-1.3.0.apk", parsed.apkUrl)
    }
    @Test fun `release without an uploaded APK still offers its release page`() {
        assertNull(AppRelease.parse(release.replace("\"uploaded\"", "\"new\"")).apkUrl)
        assertNull(AppRelease.parse(release.replace("nia-1.3.0.apk", "source.zip")).apkUrl)
    }
    @Test fun `drafts prereleases and malformed tags are rejected`() {
        listOf(release.replace("\"draft\":false", "\"draft\":true"),
            release.replace("\"prerelease\":false", "\"prerelease\":true"),
            release.replace("v1.3.0", "v1.3.0-rc1"), "{}").forEach {
            assertThrows(RuntimeException::class.java) { AppRelease.parse(it) }
        }
    }
    @Test fun `release data cannot send the user to another repository or host`() {
        listOf(release.replace("https://github.com", "http://github.com"),
            release.replace("https://github.com", "https://github.com.evil.example"),
            release.replace("robin-siegl/Nia", "other/Nia"),
            release.replace("nia-1.3.0.apk\"}", "nia-1.3.0.apk?redirect=elsewhere\"}"),
            release.replace("github.com/", "github.com:443/")).forEach {
            assertThrows(Exception::class.java) { AppRelease.parse(it) }
        }
    }
    @Test fun `manual request has bounded timeouts and no credentials or app data`() = runTest {
        val connection = FakeConnection(200, release)
        var connections = 0
        val repository = ReleaseRepository { connections++; connection }
        assertEquals(0, connections)
        assertEquals(ReleaseVersion(1, 3, 0), repository.latest()!!.version)
        assertEquals(1, connections)
        assertTrue(connection.disconnected)
        assertEquals(8000, connection.connectTimeout)
        assertEquals(8000, connection.readTimeout)
        assertFalse(connection.instanceFollowRedirects)
        assertFalse(connection.doOutput)
        assertNull(connection.getRequestProperty("Authorization"))
        assertEquals("nia-Android", connection.getRequestProperty("User-Agent"))
    }
    @Test fun `no release and rate limiting have distinct outcomes and close connections`() = runTest {
        val missing = FakeConnection(404, "")
        assertNull(ReleaseRepository { missing }.latest())
        assertTrue(missing.disconnected)
        for (status in listOf(403, 429, 500, 302)) {
            val connection = FakeConnection(status, "")
            try { ReleaseRepository { connection }.latest(); fail("Expected failure for $status") }
            catch (e: UpdateCheckException) {
                assertEquals(status == 403 || status == 429, e.message!!.contains("limit"))
            }
            assertTrue(connection.disconnected)
        }
    }
    @Test fun `oversized and invalid responses fail without a download`() = runTest {
        for (body in listOf("x".repeat(1024 * 1024 + 1), "not JSON", "{}")) {
            val connection = FakeConnection(200, body)
            try { ReleaseRepository { connection }.latest(); fail("Expected invalid response") }
            catch (_: UpdateCheckException) { }
            assertTrue(connection.disconnected)
        }
    }

    private class FakeConnection(private val status: Int, private val body: String) : HttpsURLConnection(URL(AppRelease.LATEST_API)) {
        var disconnected = false
        override fun getResponseCode() = status
        override fun getInputStream() = body.byteInputStream()
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun connect() = Unit
        override fun getCipherSuite() = "test"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
