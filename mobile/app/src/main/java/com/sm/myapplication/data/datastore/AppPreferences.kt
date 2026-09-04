package com.sm.myapplication.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {
    private val KEY_DDAY_EPOCH = longPreferencesKey("dday_epoch_day")
    private val KEY_DDAY_LABEL = stringPreferencesKey("dday_label")

    val ddayEpochDay: Flow<Long?> = context.dataStore.data.map { it[KEY_DDAY_EPOCH] }
    val ddayLabel: Flow<String> = context.dataStore.data.map { it[KEY_DDAY_LABEL] ?: "기말 고사" }

    suspend fun setDDay(epochDay: Long, label: String) {
        context.dataStore.edit {
            it[KEY_DDAY_EPOCH] = epochDay
            it[KEY_DDAY_LABEL] = label
        }
    }
}
