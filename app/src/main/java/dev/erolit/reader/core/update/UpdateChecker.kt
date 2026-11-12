package dev.erolit.reader.core.update

import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.AppJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * The one place to change if the project moves. The app reads the latest
 * published GitHub release and compares its tag with the installed version.
 */
object AppUpdate {
    const val GITHUB_OWNER = "arpan-khan"
    const val GITHUB_REPO = "erolit"
    const val REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val RELEASES_URL = "$REPO_URL/releases"
    const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
}

data class UpdateInfo(
    val version: String,
    val releaseUrl: String,
)

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val prerelease: Boolean = false,
)

class UpdateChecker(private val api: ApiClient) {

    /**
     * Returns null when the app is current, there is no release yet, or the
     * check could not be completed. A failed check must never interrupt use.
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = try {
        val release = api.getJson(AppUpdate.LATEST_RELEASE_API) { raw ->
            AppJson.decodeFromString<GitHubRelease>(raw)
        }
        val tag = release.tagName?.trim().orEmpty()
        when {
            tag.isBlank() || release.prerelease -> null
            isNewer(tag, currentVersion) -> UpdateInfo(
                version = tag,
                releaseUrl = release.htmlUrl?.takeIf { it.isNotBlank() } ?: AppUpdate.RELEASES_URL,
            )
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    internal fun isNewer(candidate: String, installed: String): Boolean {
        val a = versionParts(candidate)
        val b = versionParts(installed)
        if (a.isEmpty() || b.isEmpty()) return false
        for (index in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun versionParts(raw: String): List<Int> =
        raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
            .split(Regex("[^0-9]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
}
