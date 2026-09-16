package com.duesoon.app.data.backup

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

class SafBackupStorage(private val context: Context) : BackupStorage {
    override fun openOutputStream(uri: Uri): OutputStream? {
        return context.contentResolver.openOutputStream(uri)
    }

    override fun openInputStream(uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }
}
