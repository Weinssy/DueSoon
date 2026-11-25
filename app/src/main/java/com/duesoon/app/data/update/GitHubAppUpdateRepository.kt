package com.duesoon.app.data.update

import android.content.Context
import com.duesoon.app.domain.update.ApkDownloadState
import com.duesoon.app.domain.update.AppReleaseInfo
import com.duesoon.app.domain.update.AppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name")
    val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("published_at")
    val publishedAt: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
data class GitHubAssetDto(
    val name: String = "",
    val size: Long = 0L,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = ""
)

class GitHubAppUpdateRepository(
    private val context: Context,
    private val owner: String = DEFAULT_REPO_OWNER,
    private val repo: String = DEFAULT_REPO_NAME,
    private val appVersion: String = "1.0.0",
    private val httpClient: OkHttpClient = defaultClient()
) : AppUpdateRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun fetchLatestRelease(): Result<AppReleaseInfo?> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DueSoon-Android/$appVersion")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    404 -> return@withContext Result.success(null)
                    403 -> return@withContext Result.failure(
                        IOException("Batas permintaan GitHub tercapai. Coba beberapa saat lagi.")
                    )
                }
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Gagal memeriksa pembaruan (HTTP ${response.code}).")
                    )
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Respons server kosong."))
                val dto = json.decodeFromString<GitHubReleaseDto>(body)
                if (dto.draft || dto.tagName.isBlank()) {
                    return@withContext Result.success(null)
                }
                Result.success(dto.toAppReleaseInfo())
            }
        } catch (e: IOException) {
            Result.failure(IOException("Tidak dapat terhubung ke server pembaruan. Periksa koneksi internet Anda.", e))
        } catch (e: Exception) {
            Result.failure(IOException("Gagal membaca informasi rilis terbaru.", e))
        }
    }

    override fun downloadApk(release: AppReleaseInfo): Flow<ApkDownloadState> = flow {
        val downloadUrl = release.apkDownloadUrl
        if (downloadUrl.isNullOrBlank()) {
            emit(ApkDownloadState.Failed("Rilis ini belum menyertakan berkas APK."))
            return@flow
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Clean previous cached APKs before downloading a new one
        updatesDir.listFiles()?.forEach { old ->
            if (old.isFile) old.delete()
        }

        val safeName = sanitizeFileName(release.apkFileName ?: "DueSoon-${release.versionName}.apk")
        val targetFile = File(updatesDir, safeName)
        val tempFile = File(updatesDir, "$safeName.part")

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "DueSoon-Android/$appVersion")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(ApkDownloadState.Failed("Gagal mengunduh APK (HTTP ${response.code})."))
                    return@flow
                }
                val responseBody = response.body
                    ?: run {
                        emit(ApkDownloadState.Failed("Berkas APK kosong dari server."))
                        return@flow
                    }

                val totalBytes = responseBody.contentLength().takeIf { it > 0L }
                    ?: release.apkSizeBytes?.takeIf { it > 0L }
                    ?: -1L

                emit(ApkDownloadState.Downloading(0L, totalBytes, if (totalBytes > 0L) 0 else null))

                var bytesReadTotal = 0L
                var lastEmittedPercent = -1

                responseBody.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesReadTotal += read

                            val percent = if (totalBytes > 0L) {
                                ((bytesReadTotal * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                null
                            }
                            if (percent == null || percent != lastEmittedPercent) {
                                lastEmittedPercent = percent ?: -1
                                emit(
                                    ApkDownloadState.Downloading(
                                        bytesDownloaded = bytesReadTotal,
                                        totalBytes = totalBytes,
                                        progressPercent = percent
                                    )
                                )
                            }
                        }
                        output.flush()
                    }
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
                emit(ApkDownloadState.Completed(targetFile.absolutePath))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            tempFile.delete()
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            emit(ApkDownloadState.Failed("Unduhan gagal. Periksa koneksi internet lalu coba lagi."))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val DEFAULT_REPO_OWNER = "Weinssy"
        const val DEFAULT_REPO_NAME = "DueSoon"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        internal fun sanitizeFileName(raw: String): String {
            val cleaned = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
            return if (cleaned.endsWith(".apk", ignoreCase = true)) cleaned else "$cleaned.apk"
        }

        internal fun GitHubReleaseDto.toAppReleaseInfo(): AppReleaseInfo {
            val apkAsset = assets.firstOrNull { asset ->
                asset.name.endsWith(".apk", ignoreCase = true) && asset.browserDownloadUrl.isNotBlank()
            }
            val cleanVersion = tagName.trim().removePrefix("v").removePrefix("V")
            return AppReleaseInfo(
                tagName = tagName.trim(),
                versionName = cleanVersion.ifEmpty { tagName.trim() },
                releaseName = name?.trim()?.takeIf { it.isNotEmpty() } ?: tagName.trim(),
                releaseNotes = body?.trim().orEmpty(),
                htmlUrl = htmlUrl.ifBlank {
                    "https://github.com/$DEFAULT_REPO_OWNER/$DEFAULT_REPO_NAME/releases"
                },
                apkDownloadUrl = apkAsset?.browserDownloadUrl,
                apkFileName = apkAsset?.name,
                apkSizeBytes = apkAsset?.size?.takeIf { it > 0L },
                publishedAt = publishedAt
            )
        }
    }
}
