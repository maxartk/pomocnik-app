package cz.kovmak.pomocnik.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.kovmak.pomocnik.data.settings.SettingsRepository
import cz.kovmak.pomocnik.data.settings.UserProfile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    fun updateName(name: String) {
        viewModelScope.launch {
            repository.updateName(name)
        }
    }

    fun updateEmail(email: String) {
        viewModelScope.launch {
            repository.updateEmail(email)
        }
    }

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            repository.updateApiKey(apiKey)
        }
    }

    fun updateOcrAccessKey(accessKey: String) {
        viewModelScope.launch {
            repository.updateOcrAccessKey(accessKey)
        }
    }

    fun updateDefaultWorkType(workType: String) {
        viewModelScope.launch {
            repository.updateDefaultWorkType(workType)
        }
    }

    fun updateDefaultTimes(startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updateDefaultTimes(startTime, endTime)
        }
    }

    fun updateSelectedModel(modelId: String) {
        viewModelScope.launch {
            repository.updateSelectedModel(modelId)
        }
    }
}
