package com.duesoon.app.data.repository

import com.duesoon.app.data.local.FocusSessionDao
import com.duesoon.app.data.local.toDomainModel
import com.duesoon.app.data.local.toEntity
import com.duesoon.app.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

open class FocusRepository(
    private val focusSessionDao: FocusSessionDao
) {
    open fun observeAllSessions(): Flow<List<FocusSession>> {
        return focusSessionDao.observeAllSessions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    open fun observeSessionsByDateKey(dateKey: String): Flow<List<FocusSession>> {
        return focusSessionDao.observeSessionsByDateKey(dateKey).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    open fun observeSessionsForTask(taskId: Long): Flow<List<FocusSession>> {
        return focusSessionDao.observeSessionsForTask(taskId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    open suspend fun recordSession(session: FocusSession): Long {
        return focusSessionDao.insert(session.toEntity())
    }

    open suspend fun importSessions(sessions: List<FocusSession>) {
        if (sessions.isEmpty()) return
        val entities = sessions.map { it.copy(id = 0).toEntity() }
        focusSessionDao.insertAll(entities)
    }

    open suspend fun getAllSessions(): List<FocusSession> {
        return focusSessionDao.getAllSessions().map { it.toDomainModel() }
    }

    open suspend fun clearAllSessions() {
        focusSessionDao.deleteAll()
    }
}
