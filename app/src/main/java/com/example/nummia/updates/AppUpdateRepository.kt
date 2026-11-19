package com.example.nummia.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal data class AppUpdate(
    val versionName: String,
    val downloadUrl: String,
    val sha256: String,
    val releaseNotes: String,
    val releaseTitle: String,
    val publishedAt: String
)

internal data class AppRelease(
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val publishedAt: String
)

internal sealed interface AppUpdateCheckResult {
    data object UpToDate : AppUpdateCheckResult
    data class Available(val update: AppUpdate) : AppUpdateCheckResult
}

internal object AppUpdateRepository {
    private const val RELEASE_API_URL =
        "https://api.github.com/repos/Marmoan05/Nummia/releases/latest"
    private const val RELEASES_API_URL =
        "https://api.github.com/repos/Marmoan05/Nummia/releases"
    private const val CHANGELOG_RAW_URL =
        "https://raw.githubusercontent.com/Marmoan05/Nummia/refs/heads/main/CHANGELOG.md"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val MAX_APK_SIZE_BYTES = 100L * 1024L * 1024L

    suspend fun checkForUpdate(currentVersion: String): AppUpdateCheckResult =
        withContext(Dispatchers.IO) {
            val connection = (URL(RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Nummia-Android")
            }

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("GitHub respondió con HTTP ${connection.responseCode}.")
                }
                val release = connection.inputStream.bufferedReader().use {
                    JSONObject(it.readText())
                }
                val version = release.getString("tag_name").removePrefix("v")
                if (!isNewerVersion(version, currentVersion)) {
                    return@withContext AppUpdateCheckResult.UpToDate
                }

                val assetName = "Nummia-$version.apk"
                val assets = release.getJSONArray("assets")
                val asset = (0 until assets.length())
                    .asSequence()
                    .map(assets::getJSONObject)
                    .firstOrNull { it.optString("name") == assetName }
                    ?: throw IOException("La Release $version no contiene $assetName.")
                val digest = asset.optString("digest")
                if (!digest.startsWith("sha256:") || digest.length != 71) {
                    throw IOException("GitHub no proporcionó un SHA-256 válido para el APK.")
                }

                val update = AppUpdate(
                    versionName = version,
                    downloadUrl = asset.getString("browser_download_url"),
                    sha256 = digest.removePrefix("sha256:").lowercase(Locale.ROOT),
                    releaseNotes = release.optString("body").trim(),
                    releaseTitle = release.optString("name").ifBlank { "Nummia $version" },
                    publishedAt = release.optString("published_at")
                )
                validateDownloadUrl(update.downloadUrl)
                AppUpdateCheckResult.Available(update)
            } catch (error: JSONException) {
                throw IOException("La respuesta de GitHub no tiene el formato esperado.", error)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun releaseHistory(limit: Int = 100): List<AppRelease> =
        withContext(Dispatchers.IO) {
            require(limit > 0) { "El límite de Releases debe ser mayor que cero." }
            val connection = (URL("$RELEASES_API_URL?per_page=$limit").openConnection()
                as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Nummia-Android")
            }

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("GitHub respondió con HTTP ${connection.responseCode}.")
                }
                connection.inputStream.bufferedReader().use { reader ->
                    parseReleaseHistory(JSONArray(reader.readText()))
                }
            } catch (error: JSONException) {
                throw IOException("La respuesta de Releases de GitHub no tiene el formato esperado.", error)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun changelogHistory(): List<AppRelease> =
        withContext(Dispatchers.IO) {
            val connection = (URL(CHANGELOG_RAW_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "Nummia-Android")
            }

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("GitHub respondió con HTTP ${connection.responseCode}.")
                }
                connection.inputStream.bufferedReader().use { reader ->
                    parseChangelogHistory(reader.readText())
                }
            } finally {
                connection.disconnect()
            }
        }

    suspend fun changelogForVersion(version: String): AppRelease? =
        changelogHistory().firstOrNull {
            it.versionName == version.removePrefix("v")
        }

    internal suspend fun releaseNotesForVersion(
        version: String,
        changelogLookup: suspend (String) -> AppRelease? = ::changelogForVersion,
        releaseLookup: suspend (String) -> AppRelease? = ::releaseForVersion,
        onChangelogFailure: (IOException) -> Unit = {
            Log.w("AppUpdateRepository", "No se pudieron cargar las notas del changelog.", it)
        }
    ): AppRelease? {
        var changelogFailure: IOException? = null
        val changelogRelease = try {
            changelogLookup(version)
        } catch (error: IOException) {
            changelogFailure = error
            onChangelogFailure(error)
            null
        }
        if (changelogRelease != null) return changelogRelease

        val release = try {
            releaseLookup(version)
        } catch (error: IOException) {
            changelogFailure?.let(error::addSuppressed)
            throw error
        }
        if (release == null && changelogFailure != null) throw changelogFailure
        return release
    }

    internal suspend fun withChangelogNotes(
        update: AppUpdate,
        changelogLookup: suspend (String) -> AppRelease? = ::changelogForVersion,
        onChangelogFailure: (IOException) -> Unit = {
            Log.w("AppUpdateRepository", "No se pudieron cargar las notas del changelog.", it)
        }
    ): AppUpdate {
        val release = try {
            changelogLookup(update.versionName)
        } catch (error: IOException) {
            onChangelogFailure(error)
            null
        } ?: return update
        return update.copy(releaseNotes = release.releaseNotes, releaseTitle = release.title)
    }

    suspend fun releaseForVersion(version: String): AppRelease? =
        releaseHistory(limit = 100).firstOrNull {
            it.versionName == version.removePrefix("v")
        }

    suspend fun downloadUpdate(context: Context, update: AppUpdate): File =
        withContext(Dispatchers.IO) {
            validateDownloadUrl(update.downloadUrl)
            val updateDirectory = File(context.cacheDir, "updates")
            if (!updateDirectory.exists() && !updateDirectory.mkdirs()) {
                throw IOException("No se pudo crear el directorio temporal de descarga.")
            }

            val apkFile = File(updateDirectory, "Nummia-${update.versionName}.apk")
            val temporaryFile = File(updateDirectory, "${apkFile.name}.part")
            val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "Nummia-Android")
            }

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("La descarga respondió con HTTP ${connection.responseCode}.")
                }
                val contentLength = connection.contentLengthLong
                if (contentLength > MAX_APK_SIZE_BYTES) {
                    throw IOException("El APK supera el tamaño máximo permitido.")
                }

                val digest = MessageDigest.getInstance("SHA-256")
                var totalBytes = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(temporaryFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead < 0) break
                            totalBytes += bytesRead
                            if (totalBytes > MAX_APK_SIZE_BYTES) {
                                throw IOException("El APK supera el tamaño máximo permitido.")
                            }
                            digest.update(buffer, 0, bytesRead)
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                if (totalBytes == 0L) {
                    throw IOException("GitHub devolvió un APK vacío.")
                }
                val actualDigest = digest.digest().joinToString("") {
                    "%02x".format(Locale.ROOT, it)
                }
                if (!actualDigest.equals(update.sha256, ignoreCase = true)) {
                    throw IOException("El SHA-256 del APK descargado no coincide con GitHub.")
                }
                if (apkFile.exists() && !apkFile.delete()) {
                    throw IOException("No se pudo reemplazar el APK descargado anteriormente.")
                }
                if (!temporaryFile.renameTo(apkFile)) {
                    throw IOException("No se pudo guardar el APK descargado.")
                }
                validateApk(context, apkFile, update.versionName)
                apkFile
            } finally {
                connection.disconnect()
                temporaryFile.delete()
            }
        }

    fun createInstallIntent(context: Context, apkFile: File) =
        Intent(Intent.ACTION_VIEW).apply {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    internal fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        val latest = parseVersion(latestVersion)
        val current = parseVersion(currentVersion)
        val segmentCount = maxOf(latest.size, current.size)
        for (index in 0 until segmentCount) {
            val latestSegment = latest.getOrElse(index) { 0 }
            val currentSegment = current.getOrElse(index) { 0 }
            if (latestSegment != currentSegment) return latestSegment > currentSegment
        }
        return false
    }

    internal fun parseReleaseHistory(releases: JSONArray): List<AppRelease> =
        (0 until releases.length()).mapNotNull { index ->
            val release = releases.getJSONObject(index)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
                return@mapNotNull null
            }
            val version = release.optString("tag_name").removePrefix("v")
            if (version.isBlank()) return@mapNotNull null
            AppRelease(
                versionName = version,
                title = release.optString("name").ifBlank { "Nummia $version" },
                releaseNotes = release.optString("body").trim(),
                publishedAt = release.optString("published_at")
            )
        }

    internal fun parseChangelogHistory(changelog: String): List<AppRelease> {
        val versionHeading = Regex("""^##\s+(\d+(?:\.\d+)+)\s*(?:-\s*(.*))?$""")
        val releases = mutableListOf<AppRelease>()
        var currentVersion: String? = null
        var currentLabel = ""
        val currentNotes = mutableListOf<String>()

        fun addCurrentRelease() {
            val version = currentVersion ?: return
            val label = currentLabel.trim()
            releases += AppRelease(
                versionName = version,
                title = buildString {
                    append("Nummia $version")
                    if (label.isNotEmpty()) append(" · $label")
                },
                releaseNotes = currentNotes.joinToString("\n").trim(),
                publishedAt = ""
            )
        }

        changelog.lineSequence().forEach { line ->
            val match = versionHeading.matchEntire(line.trim())
            if (match != null) {
                addCurrentRelease()
                currentVersion = match.groupValues[1]
                currentLabel = match.groupValues.getOrElse(2) { "" }
                currentNotes.clear()
            } else if (currentVersion != null) {
                currentNotes += line
            }
        }
        addCurrentRelease()
        return releases
    }

    private fun parseVersion(version: String): List<Int> {
        val normalized = version.removePrefix("v")
        if (!normalized.matches(Regex("\\d+(\\.\\d+)*"))) {
            throw IllegalArgumentException("Versión no válida: $version")
        }
        return normalized.split('.').map(String::toInt)
    }

    private fun validateDownloadUrl(downloadUrl: String) {
        val uri = Uri.parse(downloadUrl)
        if (uri.scheme != "https" ||
            uri.host != "github.com" ||
            !uri.path.orEmpty().startsWith("/MarmoAn05/Nummia/releases/download/", ignoreCase = true)
        ) {
            throw IOException("La URL del APK no pertenece a la Release pública de Nummia.")
        }

    }

    @Suppress("DEPRECATION")
    private fun validateApk(context: Context, apkFile: File, expectedVersion: String) {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
        } ?: throw IOException("El archivo descargado no es un APK válido.")

        if (packageInfo.packageName != context.packageName ||
            packageInfo.versionName != expectedVersion
        ) {
            throw IOException("El APK descargado no corresponde a Nummia $expectedVersion.")
        }
    }
}
