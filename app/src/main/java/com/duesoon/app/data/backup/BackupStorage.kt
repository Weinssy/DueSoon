package com.duesoon.app.data.backup

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

interface BackupStorage {
    fun openOutputStream(uri: Uri): OutputStream?
    fun openInputStream(uri: Uri): InputStream?
}
