package com.sm.myapplication.data.db

import androidx.room.TypeConverter
import com.sm.myapplication.data.entity.StudyMode
import com.sm.myapplication.data.entity.TodoCategory

class Converters {
    @TypeConverter fun fromCategory(c: TodoCategory): String = c.name
    @TypeConverter fun toCategory(s: String): TodoCategory = TodoCategory.valueOf(s)

    @TypeConverter fun fromMode(m: StudyMode): String = m.name
    @TypeConverter fun toMode(s: String): StudyMode = StudyMode.valueOf(s)
}
