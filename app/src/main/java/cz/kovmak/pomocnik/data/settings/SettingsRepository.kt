package cz.kovmak.pomocnik.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val openRouterApiKey: String = "",
    val defaultWorkType: String = "E",
    val defaultStartTime: String = "07:00",
    val defaultEndTime: String = "15:30"
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val DEFAULT_WORK_TYPE = stringPreferencesKey("default_work_type")
        val DEFAULT_START_TIME = stringPreferencesKey("default_start_time")
        val DEFAULT_END_TIME = stringPreferencesKey("default_end_time")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[PreferencesKeys.NAME] ?: "",
            email = prefs[PreferencesKeys.EMAIL] ?: "",
            openRouterApiKey = prefs[PreferencesKeys.OPENROUTER_API_KEY] ?: "",
            defaultWorkType = prefs[PreferencesKeys.DEFAULT_WORK_TYPE] ?: "E",
            defaultStartTime = prefs[PreferencesKeys.DEFAULT_START_TIME] ?: "07:00",
            defaultEndTime = prefs[PreferencesKeys.DEFAULT_END_TIME] ?: "15:30"
        )
    }

    suspend fun updateName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NAME] = name
        }
    }

    suspend fun updateEmail(email: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.EMAIL] = email
        }
    }

    suspend fun updateApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.OPENROUTER_API_KEY] = apiKey
        }
    }

    suspend fun updateDefaultWorkType(workType: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_WORK_TYPE] = workType
        }
    }

    suspend fun updateDefaultTimes(startTime: String, endTime: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_START_TIME] = startTime
            prefs[PreferencesKeys.DEFAULT_END_TIME] = endTime
        }
    }
}
