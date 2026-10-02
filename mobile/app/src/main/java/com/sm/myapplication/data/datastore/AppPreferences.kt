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
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_MEMBER_ID = longPreferencesKey("member_id")
    private val KEY_MEMBER_NAME = stringPreferencesKey("member_name")
    private val KEY_BIRTH_DATE = stringPreferencesKey("birth_date")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_EDUCATION = stringPreferencesKey("education")
    // 폰에 남아 있는 Todo/D-Day가 어느 회원 것인지. 다른 계정으로 로그인하면 이전 데이터를 비우는 데 쓴다.
    private val KEY_DATA_OWNER_ID = longPreferencesKey("data_owner_id")
    private val KEY_NOTIFICATIONS_ENABLED =androidx.datastore.preferences.core.booleanPreferencesKey("notifications_enabled")

    val ddayEpochDay: Flow<Long?> = context.dataStore.data.map { it[KEY_DDAY_EPOCH] }
    val ddayLabel: Flow<String> = context.dataStore.data.map { it[KEY_DDAY_LABEL] ?: "기말 고사" }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val memberId: Flow<Long?> = context.dataStore.data.map { it[KEY_MEMBER_ID] }
    val memberName: Flow<String> = context.dataStore.data.map { it[KEY_MEMBER_NAME] ?: "" }
    val birthDate: Flow<String> = context.dataStore.data.map { it[KEY_BIRTH_DATE] ?: "" }
    val email: Flow<String> = context.dataStore.data.map { it[KEY_EMAIL] ?: "" }
    val education: Flow<String> = context.dataStore.data.map { it[KEY_EDUCATION] ?: "중학생" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: false }

    suspend fun setDDay(epochDay: Long, label: String) {
        context.dataStore.edit {
            it[KEY_DDAY_EPOCH] = epochDay
            it[KEY_DDAY_LABEL] = label
        }
    }

    val dataOwnerId: Flow<Long?> = context.dataStore.data.map { it[KEY_DATA_OWNER_ID] }

    suspend fun setDataOwnerId(memberId: Long) {
        context.dataStore.edit { it[KEY_DATA_OWNER_ID] = memberId }
    }

    suspend fun clearDDay() {
        context.dataStore.edit {
            it.remove(KEY_DDAY_EPOCH)
            it.remove(KEY_DDAY_LABEL)
        }
    }

    suspend fun saveSession(accessToken: String, memberId: Long, name: String) {
        context.dataStore.edit {
            it[KEY_ACCESS_TOKEN] = accessToken
            it[KEY_MEMBER_ID] = memberId
            it[KEY_MEMBER_NAME] = name
        }
    }

    suspend fun saveProfile(name: String, birthDate: String, email: String, education: String) {
        context.dataStore.edit {
            it[KEY_MEMBER_NAME] = name
            it[KEY_BIRTH_DATE] = birthDate
            it[KEY_EMAIL] = email
            it[KEY_EDUCATION] = education
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(KEY_ACCESS_TOKEN)
            it.remove(KEY_MEMBER_ID)
            it.remove(KEY_MEMBER_NAME)
        }
    }
}
