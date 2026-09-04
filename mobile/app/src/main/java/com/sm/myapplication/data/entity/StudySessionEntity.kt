package com.sm.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StudyMode { BASIC, PURE }

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val mode: StudyMode = StudyMode.BASIC,
    val dateEpochDay: Long,
)
