package com.duesoon.app.domain.update

import kotlinx.coroutines.flow.Flow

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long?,
    val publishedAt: String?
)

sealed class ApkDownloadState {
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Int?
    ) : ApkDownloadState()

    data class Completed(val filePath: String) : ApkDownloadState()
    data class Failed(val message: String) : ApkDownloadState()
}

interface AppUpdateRepository {
    suspend fun fetchLatestRelease(): Result<AppReleaseInfo?>
    fun downloadApk(release: AppReleaseInfo): Flow<ApkDownloadState>
}
