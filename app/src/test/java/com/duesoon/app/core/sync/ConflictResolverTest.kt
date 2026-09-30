package com.duesoon.app.core.sync

import com.duesoon.app.data.local.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictResolverTest {

    @Test
    fun `resolve returns APPLY_REMOTE when local is null`() {
        val verdict = ConflictResolver.resolve(
            local = null,
            remoteUuid = "uuid1",
            remoteUpdatedAtUtc = 1000L,
            remoteRevision = 1L,
            remoteIsDeleted = false,
            remoteCiphertext = "remoteData"
        )
        
        assertEquals(ResolutionVerdict.APPLY_REMOTE, verdict)
    }

    @Test
    fun `resolve applies remote when remote timestamp is newer`() {
        val local = TaskEntity(
            id = 1,
            title = "Local Task",
            description = null,
            deadline = null,
            category = null,
            priority = "NORMAL",
            reminderType = "NONE",
            isRecurring = false,
            recurrenceInterval = null,
            completed = false,
            snoozedUntil = null,
            createdAt = 0L,
            updatedAt = 0L,
            uuid = "uuid1",
            isDeleted = false,
            updatedAtUtc = 1000L,
            revision = 1L,
            syncState = "SYNCED"
        )

        val verdict = ConflictResolver.resolve(
            local = local,
            remoteUuid = "uuid1",
            remoteUpdatedAtUtc = 2000L, // Newer
            remoteRevision = 1L,
            remoteIsDeleted = false,
            remoteCiphertext = "remoteData"
        )
        
        assertEquals(ResolutionVerdict.APPLY_REMOTE, verdict)
    }

    @Test
    fun `resolve keeps local when local timestamp is newer`() {
        val local = TaskEntity(
            id = 1,
            title = "Local Task",
            description = null,
            deadline = null,
            category = null,
            priority = "NORMAL",
            reminderType = "NONE",
            isRecurring = false,
            recurrenceInterval = null,
            completed = false,
            snoozedUntil = null,
            createdAt = 0L,
            updatedAt = 0L,
            uuid = "uuid1",
            isDeleted = false,
            updatedAtUtc = 2000L, // Newer
            revision = 1L,
            syncState = "DIRTY"
        )

        val verdict = ConflictResolver.resolve(
            local = local,
            remoteUuid = "uuid1",
            remoteUpdatedAtUtc = 1000L, 
            remoteRevision = 2L, // Revision is higher, but timestamp is primary
            remoteIsDeleted = false,
            remoteCiphertext = "remoteData"
        )
        
        assertEquals(ResolutionVerdict.KEEP_LOCAL, verdict)
    }

    @Test
    fun `resolve applies remote when timestamps tie but remote revision is higher`() {
        val local = TaskEntity(
            id = 1,
            title = "Local Task",
            description = null,
            deadline = null,
            category = null,
            priority = "NORMAL",
            reminderType = "NONE",
            isRecurring = false,
            recurrenceInterval = null,
            completed = false,
            snoozedUntil = null,
            createdAt = 0L,
            updatedAt = 0L,
            uuid = "uuid1",
            isDeleted = false,
            updatedAtUtc = 2000L, // Tie
            revision = 1L,
            syncState = "DIRTY"
        )

        val verdict = ConflictResolver.resolve(
            local = local,
            remoteUuid = "uuid1",
            remoteUpdatedAtUtc = 2000L, // Tie
            remoteRevision = 2L, // Higher revision
            remoteIsDeleted = false,
            remoteCiphertext = "remoteData"
        )
        
        assertEquals(ResolutionVerdict.APPLY_REMOTE, verdict)
    }

    @Test
    fun `resolve applies remote on tie if remote ciphertext is lexicographically greater`() {
        val local = TaskEntity(
            id = 1,
            title = "Local Task",
            description = null,
            deadline = null,
            category = null,
            priority = "NORMAL",
            reminderType = "NONE",
            isRecurring = false,
            recurrenceInterval = null,
            completed = false,
            snoozedUntil = null,
            createdAt = 0L,
            updatedAt = 0L,
            uuid = "uuid1",
            isDeleted = false,
            updatedAtUtc = 2000L, // Tie
            revision = 1L, // Tie
            syncState = "DIRTY"
        )

        val verdict = ConflictResolver.resolve(
            local = local,
            remoteUuid = "uuid1",
            remoteUpdatedAtUtc = 2000L,
            remoteRevision = 1L,
            remoteIsDeleted = false,
            remoteCiphertext = "B_remoteData", 
            localCiphertext = "A_localData"
        )
        
        assertEquals(ResolutionVerdict.APPLY_REMOTE, verdict)
    }
}
