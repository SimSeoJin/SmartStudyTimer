package com.sm.myapplication.data.repository

import android.content.Context
import com.sm.myapplication.data.dao.StudySessionDao
import com.sm.myapplication.data.dao.TodoDao
import com.sm.myapplication.data.datastore.AppPreferences
import com.sm.myapplication.data.db.AppDatabase
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.entity.StudySessionEntity
import com.sm.myapplication.data.entity.TodoEntity

class AppRepository(
    val todoDao: TodoDao,
    val studySessionDao: StudySessionDao,
    val prefs: AppPreferences,
) {
    fun observeTodosByDate(day: Long) = todoDao.observeByDate(day)
    suspend fun addTodo(todo: TodoEntity) = todoDao.insert(todo)
    suspend fun updateTodo(todo: TodoEntity) = todoDao.update(todo)
    suspend fun deleteTodo(todo: TodoEntity) = todoDao.delete(todo)
    suspend fun toggleTodo(id: Long, done: Boolean) = todoDao.setDone(id, done)

    suspend fun cleanupAbnormalSessions() = studySessionDao.deleteAbnormal()

    suspend fun saveStudySession(start: Long, end: Long, mode: StudyMode, dateEpochDay: Long) {
        studySessionDao.insert(
            StudySessionEntity(
                startTime = start,
                endTime = end,
                durationMs = end - start,
                mode = mode,
                dateEpochDay = dateEpochDay,
            )
        )
    }

    fun observeTodaySessionMs(day: Long) = studySessionDao.observeTotalMsByDate(day)
    fun observeMonthlyTotals(fromDay: Long, toDay: Long) = studySessionDao.observeMonthlyTotals(fromDay, toDay)
    fun observeSessionsByDate(day: Long) = studySessionDao.observeByDate(day)

    val ddayEpochDay = prefs.ddayEpochDay
    val ddayLabel = prefs.ddayLabel
    suspend fun setDDay(epochDay: Long, label: String) = prefs.setDDay(epochDay, label)

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun get(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val db = AppDatabase.get(context)
                    AppRepository(
                        todoDao = db.todoDao(),
                        studySessionDao = db.studySessionDao(),
                        prefs = AppPreferences(context.applicationContext),
                    ).also { INSTANCE = it }
                }
            }
        }
    }
}
