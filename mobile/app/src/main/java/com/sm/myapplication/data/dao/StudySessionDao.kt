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

    @Query("SELECT mode AS mode, COALESCE(SUM(durationMs), 0) AS total FROM study_sessions WHERE dateEpochDay BETWEEN :fromDay AND :toDay AND durationMs <= 43200000 GROUP BY mode")
    fun observeModeTotals(fromDay: Long, toDay: Long): Flow<List<ModeTotal>>

    @Query("DELETE FROM study_sessions WHERE durationMs > 43200000")
    suspend fun deleteAbnormal()

    /** 서버 기록과 겹치는지 비교할 때 쓴다(서버는 초 단위로 저장한다). */
    @Query("SELECT startTime / 1000 AS startSec, endTime / 1000 AS endSec FROM study_sessions")
    suspend fun getAllSpans(): List<SessionSpan>

    @Insert
    suspend fun insertAll(sessions: List<StudySessionEntity>)

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()
}

data class SessionSpan(val startSec: Long, val endSec: Long)

data class DailyTotal(val day: Long, val total: Long)

/** mode는 Room에 문자열(StudyMode.name)로 저장돼 있다 */
data class ModeTotal(val mode: String, val total: Long)
