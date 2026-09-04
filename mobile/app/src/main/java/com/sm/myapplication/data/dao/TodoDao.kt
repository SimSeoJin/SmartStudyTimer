package com.sm.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sm.myapplication.data.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE dateEpochDay = :day ORDER BY isDone ASC, createdAt ASC")
    fun observeByDate(day: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY dateEpochDay ASC, createdAt ASC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("UPDATE todos SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)
}
