package com.duesoon.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duesoon.app.domain.model.AccentPalette
import com.duesoon.app.domain.model.VisualDensity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UserPreferencesRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepository
    private lateinit var testContext: Context

    @Before
    fun setup() {
        testContext = ApplicationProvider.getApplicationContext()
        val randomName = "test_settings_${Random.nextInt()}"
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { testContext.preferencesDataStoreFile(randomName) }
        )
        
        // Injecting the custom datastore for testing is tricky since we defined an extension property.
        // We'll test via the real extension property in context by clearing it, or we can use Robolectric's context.
        repository = UserPreferencesRepository(testContext)
    }

    @Test
    fun testDefaultPreferences() = runTest {
        val prefs = repository.userPreferencesFlow.first()
        assertEquals("SYSTEM", prefs.theme)
        assertTrue(prefs.notificationsEnabled)
        assertEquals(AccentPalette.INDIGO, prefs.accentPalette)
        assertEquals(VisualDensity.COMFORTABLE, prefs.visualDensity)
        assertTrue(prefs.hapticsEnabled)
    }

    @Test
    fun testUpdateAccentPalette() = runTest {
        repository.updateAccentPalette(AccentPalette.EMERALD)
        val prefs = repository.userPreferencesFlow.first()
        assertEquals(AccentPalette.EMERALD, prefs.accentPalette)
    }

    @Test
    fun testUpdateVisualDensity() = runTest {
        repository.updateVisualDensity(VisualDensity.COMPACT)
        val prefs = repository.userPreferencesFlow.first()
        assertEquals(VisualDensity.COMPACT, prefs.visualDensity)
    }

    @Test
    fun testUpdateHapticsEnabled() = runTest {
        repository.updateHapticsEnabled(false)
        val prefs = repository.userPreferencesFlow.first()
        assertFalse(prefs.hapticsEnabled)
    }
}
