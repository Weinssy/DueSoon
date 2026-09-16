package com.duesoon.app.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SafBackupStorageTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var storage: SafBackupStorage

    @Before
    fun setup() {
        context = mock(Context::class.java)
        contentResolver = mock(ContentResolver::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        storage = SafBackupStorage(context)
    }

    @Test
    fun testOpenInputStream() {
        val uri = mock(Uri::class.java)
        val expectedStream = ByteArrayInputStream("test".toByteArray())
        `when`(contentResolver.openInputStream(uri)).thenReturn(expectedStream)
        
        val stream = storage.openInputStream(uri)
        assertEquals(expectedStream, stream)
    }

    @Test
    fun testOpenOutputStream() {
        val uri = mock(Uri::class.java)
        val expectedStream = ByteArrayOutputStream()
        `when`(contentResolver.openOutputStream(uri)).thenReturn(expectedStream)
        
        val stream = storage.openOutputStream(uri)
        assertEquals(expectedStream, stream)
    }
}
