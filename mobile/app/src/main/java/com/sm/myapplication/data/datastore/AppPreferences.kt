package com.sm.myapplication.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

class AppPreferences(private val context: Context) {
    private val KEY_DDAY_EPOCH = longPreferencesKey("dday_epoch_day")
    private val KEY_DDAY_LABEL = stringPreferencesKey("dday_label")
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_MEMBER_ID = longPreferencesKey("member_id")
    private val KEY_MEMBER_NAME = stringPreferencesKey("member_name")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_EDUCATION = stringPreferencesKey("education")
    private val KEY_BIRTH_DATE = stringPreferencesKey("birth_date")
    private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val KEY_TODO_TOTAL = intPreferencesKey("todo_total")
    private val KEY_TODO_DONE = intPreferencesKey("todo_done")

    val ddayEpochDay: Flow<Long?> = context.dataStore.data.map { it[KEY_DDAY_EPOCH] }
    val ddayLabel: Flow<String> = context.dataStore.data.map { it[KEY_DDAY_LABEL] ?: "기말 고사" }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val memberId: Flow<Long?> = context.dataStore.data.map { it[KEY_MEMBER_ID] }
    val memberName: Flow<String> = context.dataStore.data.map { it[KEY_MEMBER_NAME] ?: "" }
    val email: Flow<String> = context.dataStore.data.map { it[KEY_EMAIL] ?: "" }
    val education: Flow<String> = context.dataStore.data.map { it[KEY_EDUCATION] ?: "" }
    val birthDate: Flow<String> = context.dataStore.data.map { it[KEY_BIRTH_DATE] ?: "" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }
    val todoTotal: Flow<Int> = context.dataStore.data.map { it[KEY_TODO_TOTAL] ?: 0 }
    val todoDone: Flow<Int> = context.dataStore.data.map { it[KEY_TODO_DONE] ?: 0 }

    suspend fun setDDay(epochDay: Long, label: String) {
        context.dataStore.edit {
            it[KEY_DDAY_EPOCH] = epochDay
            it[KEY_DDAY_LABEL] = label
        }
    }

    suspend fun saveSession(accessToken: String, memberId: Long, name: String) {
        context.dataStore.edit {
            it[KEY_ACCESS_TOKEN] = accessToken
            it[KEY_MEMBER_ID] = memberId
            it[KEY_MEMBER_NAME] = name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(KEY_ACCESS_TOKEN)
            it.remove(KEY_MEMBER_ID)
            it.remove(KEY_MEMBER_NAME)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setUserProfile(memberName: String, email: String, education: String, birthDate: String) {
        context.dataStore.edit {
            it[KEY_MEMBER_NAME] = memberName
            it[KEY_EMAIL] = email
            it[KEY_EDUCATION] = education
            it[KEY_BIRTH_DATE] = birthDate
        }
    }

    suspend fun setTodoStats(total: Int, done: Int) {
        context.dataStore.edit {
            it[KEY_TODO_TOTAL] = total
            it[KEY_TODO_DONE] = done
        }
    }
}
