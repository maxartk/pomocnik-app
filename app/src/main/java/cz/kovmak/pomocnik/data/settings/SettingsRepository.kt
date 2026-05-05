package cz.kovmak.pomocnik.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import cz.kovmak.pomocnik.data.network.ModelConfig

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val openRouterApiKey: String = "",
    val selectedModel: String = ModelConfig.DEFAULT_MODEL,
    val defaultWorkType: String = "E",
    val defaultStartTime: String = "07:00",
    val defaultEndTime: String = "15:30",
    val reportEmail: String = "Maksym.kovalevskyi@knorr-bremse.com",
    val reportEmailBcc: String = "arrogantdoor697@agentmail.to"
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val DEFAULT_WORK_TYPE = stringPreferencesKey("default_work_type")
        val DEFAULT_START_TIME = stringPreferencesKey("default_start_time")
        val DEFAULT_END_TIME = stringPreferencesKey("default_end_time")
        val REPORT_EMAIL = stringPreferencesKey("report_email")
        val REPORT_EMAIL_BCC = stringPreferencesKey("report_email_bcc")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[PreferencesKeys.NAME] ?: "",
            email = prefs[PreferencesKeys.EMAIL] ?: "",
            openRouterApiKey = prefs[PreferencesKeys.OPENROUTER_API_KEY] ?: "",
            selectedModel = prefs[PreferencesKeys.SELECTED_MODEL] ?: ModelConfig.DEFAULT_MODEL,
            defaultWorkType = prefs[PreferencesKeys.DEFAULT_WORK_TYPE] ?: "E",
            defaultStartTime = prefs[PreferencesKeys.DEFAULT_START_TIME] ?: "07:00",
            defaultEndTime = prefs[PreferencesKeys.DEFAULT_END_TIME] ?: "15:30",
            reportEmail = prefs[PreferencesKeys.REPORT_EMAIL] ?: "Maksym.kovalevskyi@knorr-bremse.com",
            reportEmailBcc = prefs[PreferencesKeys.REPORT_EMAIL_BCC] ?: "arrogantdoor697@agentmail.to"
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

    suspend fun updateSelectedModel(modelId: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_MODEL] = modelId
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
