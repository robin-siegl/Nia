package dev.robinsiegl.nia.platform

import dev.robinsiegl.nia.model.AppRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.coroutineContext

class UpdateCheckException(message: String) : IOException(message)

/** No scheduled work, credentials, app inventory or automatic APK downloads. */
class ReleaseRepository(private val connect: () -> HttpsURLConnection = {
    URL(AppRelease.LATEST_API).openConnection() as HttpsURLConnection
}) {
    suspend fun latest(): AppRelease? = withContext(Dispatchers.IO) {
        val connection = connect()
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "nia-Android")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            when (connection.responseCode) {
                200 -> Unit
                404 -> return@withContext null
                403, 429 -> throw UpdateCheckException("GitHub’s request limit was reached. Try again later.")
                else -> throw UpdateCheckException("GitHub is unavailable. Try again later.")
            }
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 1024 * 1024) throw UpdateCheckException("GitHub’s response could not be read. Try again later.")
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
            try { AppRelease.parse(bytes) }
            catch (_: Exception) { throw UpdateCheckException("The release information could not be read. View releases on GitHub instead.") }
        } finally { connection.disconnect() }
    }
}
