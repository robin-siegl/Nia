package dev.robinsiegl.nia.model

import kotlinx.serialization.json.*
import java.net.URI

data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int = compareValuesBy(this, other,
        ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)
    override fun toString() = "$major.$minor.$patch"

    companion object {
        private val pattern = Regex("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")
        fun parse(value: String): ReleaseVersion? {
            val parts = pattern.matchEntire(value)?.groupValues?.drop(1)?.map { it.toIntOrNull() ?: return null } ?: return null
            return ReleaseVersion(parts[0], parts[1], parts[2])
        }
    }
}

data class AppRelease(val version: ReleaseVersion, val pageUrl: String, val apkUrl: String?) {
    companion object {
        const val REPOSITORY = "https://github.com/robin-siegl/Nia"
        const val LATEST_API = "https://api.github.com/repos/robin-siegl/Nia/releases/latest"

        fun parse(text: String): AppRelease {
            val root = Json.parseToJsonElement(text).jsonObject
            require(root["draft"]?.jsonPrimitive?.booleanOrNull == false &&
                root["prerelease"]?.jsonPrimitive?.booleanOrNull == false)
            val tag = root.getValue("tag_name").jsonPrimitive.content
            val version = requireNotNull(ReleaseVersion.parse(tag))
            require(tag == "v$version")
            fun expectedLink(value: String, expected: String): String {
                val uri = URI(value)
                require(value == expected && uri.scheme == "https" && uri.host == "github.com" &&
                    uri.userInfo == null && uri.port == -1 && uri.query == null && uri.fragment == null)
                return value
            }
            val page = expectedLink(root.getValue("html_url").jsonPrimitive.content, "$REPOSITORY/releases/tag/$tag")
            val apk = root["assets"]?.jsonArray?.firstOrNull {
                it.jsonObject["name"]?.jsonPrimitive?.content == "nia-$version.apk" &&
                    it.jsonObject["state"]?.jsonPrimitive?.content == "uploaded"
            }?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content?.let {
                expectedLink(it, "$REPOSITORY/releases/download/$tag/nia-$version.apk")
            }
            return AppRelease(version, page, apk)
        }
    }
}

sealed interface UpdateCheck {
    data object Idle : UpdateCheck
    data object Checking : UpdateCheck
    data object NoRelease : UpdateCheck
    data class Result(val release: AppRelease) : UpdateCheck
    data class Failed(val message: String) : UpdateCheck
}
