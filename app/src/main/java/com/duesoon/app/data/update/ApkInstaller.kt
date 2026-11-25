package com.duesoon.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed class ApkInstallLaunchResult {
    object Launched : ApkInstallLaunchResult()
    object NeedsUnknownSourcesPermission : ApkInstallLaunchResult()
    data class Error(val message: String) : ApkInstallLaunchResult()
}

object ApkInstaller {

    fun canInstallPackages(context: Context): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun createUnknownSourcesIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun launchInstall(context: Context, apkFilePath: String): ApkInstallLaunchResult {
        if (!canInstallPackages(context)) {
            return ApkInstallLaunchResult.NeedsUnknownSourcesPermission
        }

        val apkFile = File(apkFilePath)
        if (!apkFile.exists() || !apkFile.isFile) {
            return ApkInstallLaunchResult.Error("Berkas APK tidak ditemukan. Silakan unduh ulang.")
        }

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            ApkInstallLaunchResult.Launched
        } catch (e: Exception) {
            ApkInstallLaunchResult.Error("Gagal membuka pemasang aplikasi: ${e.message ?: "error tidak diketahui"}")
        }
    }
}
