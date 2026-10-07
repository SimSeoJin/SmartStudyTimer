package com.sm.myapplication.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sm.myapplication.data.dao.StudySessionDao
import com.sm.myapplication.data.dao.TodoDao
import com.sm.myapplication.data.entity.StudySessionEntity
import com.sm.myapplication.data.entity.TodoEntity

@Database(
    entities = [TodoEntity::class, StudySessionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }

        // v2: todos.serverId 추가(서버 동기화용). 기존 Todo는 serverId=null로 남았다가 첫 동기화 때 서버로 올라간다.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN serverId INTEGER")
            }
        }
    }
}
