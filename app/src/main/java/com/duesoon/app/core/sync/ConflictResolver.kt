package com.duesoon.app.core.sync

import com.duesoon.app.data.local.TaskEntity

enum class ResolutionVerdict {
    APPLY_REMOTE,
    KEEP_LOCAL
}

object ConflictResolver {

    /**
     * Resolves conflict using Last-Write-Wins (LWW) with the following hierarchy:
     * 1. Primary: updatedAtUtc (Higher timestamp wins)
     * 2. Secondary: revision (Higher revision wins if timestamps match)
     * 3. Tertiary: Lexicographical comparison of ciphertext (To ensure deterministic convergence on ties)
     */
    fun resolve(
        local: TaskEntity?,
        remoteUuid: String,
        remoteUpdatedAtUtc: Long,
        remoteRevision: Long,
        remoteIsDeleted: Boolean,
        remoteCiphertext: String,
        localCiphertext: String? = null
    ): ResolutionVerdict {
        
        // If there is no local record, always apply remote
        if (local == null) {
            return ResolutionVerdict.APPLY_REMOTE
        }

        // 1. Primary Resolution: Timestamp
        if (remoteUpdatedAtUtc > local.updatedAtUtc) {
            return ResolutionVerdict.APPLY_REMOTE
        } else if (remoteUpdatedAtUtc < local.updatedAtUtc) {
            return ResolutionVerdict.KEEP_LOCAL
        }

        // 2. Secondary Resolution: Revision
        if (remoteRevision > local.revision) {
            return ResolutionVerdict.APPLY_REMOTE
        } else if (remoteRevision < local.revision) {
            return ResolutionVerdict.KEEP_LOCAL
        }

        // 3. Tertiary Resolution: Lexicographical string comparison of ciphertext
        // If localCiphertext is not provided (e.g., we haven't encrypted it yet to compare),
        // we can default to KEEP_LOCAL or force evaluation if strictly needed.
        // For standard LWW, we evaluate non-null strings.
        if (localCiphertext != null) {
            if (remoteCiphertext > localCiphertext) {
                return ResolutionVerdict.APPLY_REMOTE
            }
        }
        
        // On absolute tie, retain local.
        return ResolutionVerdict.KEEP_LOCAL
    }
}
