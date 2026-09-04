package com.sm.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sm.myapplication.data.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insert(session: StudySessionEntity): Long

    @Query("SELECT * FROM study_sessions WHERE dateEpochDay = :day AND durationMs <= 43200000 ORDER BY startTime ASC")
    fun observeByDate(day: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM study_sessions WHERE dateEpochDay = :day AND durationMs <= 43200000")
    fun observeTotalMsByDate(day: Long): Flow<Long>

    @Query("SELECT dateEpochDay AS day, COALESCE(SUM(durationMs), 0) AS total FROM study_sessions WHERE dateEpochDay BETWEEN :fromDay AND :toDay AND durationMs <= 43200000 GROUP BY dateEpochDay")
    fun observeMonthlyTotals(fromDay: Long, toDay: Long): Flow<List<DailyTotal>>

    @Query("DELETE FROM study_sessions WHERE durationMs > 43200000")
    suspend fun deleteAbnormal()
}

data class DailyTotal(val day: Long, val total: Long)
