package com.duesoon.app.domain.update

import com.duesoon.app.data.update.GitHubAssetDto
import com.duesoon.app.data.update.GitHubAppUpdateRepository.Companion.toAppReleaseInfo
import com.duesoon.app.data.update.GitHubReleaseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticVersionTest {

    @Test
    fun `parses standard and v-prefixed versions`() {
        assertEquals(SemanticVersion(2, 0, 0), SemanticVersion.parseOrNull("v2.0.0"))
        assertEquals(SemanticVersion(1, 9, 5), SemanticVersion.parseOrNull("1.9.5"))
        assertEquals(SemanticVersion(2, 1, 0, "beta.1"), SemanticVersion.parseOrNull("v2.1.0-beta.1"))
        assertNull(SemanticVersion.parseOrNull("not-a-version"))
    }

    @Test
    fun `compares versions correctly including prereleases`() {
        assertTrue(SemanticVersion.isNewer("v2.0.0", "1.9.5"))
        assertTrue(SemanticVersion.isNewer("1.9.6", "1.9.5"))
        assertTrue(SemanticVersion.isNewer("2.0.0", "2.0.0-beta.1"))
        assertFalse(SemanticVersion.isNewer("1.9.5", "1.9.5"))
        assertFalse(SemanticVersion.isNewer("1.9.4", "1.9.5"))
        assertFalse(SemanticVersion.isNewer("2.0.0-alpha.1", "2.0.0"))
    }

    @Test
    fun `maps GitHubReleaseDto and picks apk asset`() {
        val dto = GitHubReleaseDto(
            tagName = "v2.0.0",
            name = "DueSoon 2.0.0",
            body = "- Tab Fokus penuh\n- In-App Update",
            htmlUrl = "https://github.com/Weinssy/DueSoon/releases/tag/v2.0.0",
            assets = listOf(
                GitHubAssetDto(name = "checksums.txt", size = 128, browserDownloadUrl = "https://example.com/checksums.txt"),
                GitHubAssetDto(name = "DueSoon-v2.0.0.apk", size = 5_242_880, browserDownloadUrl = "https://example.com/DueSoon-v2.0.0.apk")
            )
        )

        val info = dto.toAppReleaseInfo()
        assertEquals("2.0.0", info.versionName)
        assertEquals("DueSoon-v2.0.0.apk", info.apkFileName)
        assertEquals("https://example.com/DueSoon-v2.0.0.apk", info.apkDownloadUrl)
        assertEquals(5_242_880L, info.apkSizeBytes)
    }
}
