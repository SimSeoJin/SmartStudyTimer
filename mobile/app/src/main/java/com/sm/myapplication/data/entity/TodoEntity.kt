package com.sm.myapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TodoCategory { GENERAL, EXAM, CERT }

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: TodoCategory = TodoCategory.GENERAL,
    val dateEpochDay: Long,
    val timeMinutes: Int? = null,
    val memo: String = "",
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
