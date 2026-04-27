package cz.kovmak.pomocnik.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.data.repository.WorkRepository
import cz.kovmak.pomocnik.data.settings.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WorkFormState(
    val orderId: String = "",
    val workType: String = "E",
    val descriptionUa: String = "",
    val materials: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val hours: Double = 0.0,
    val photoUri: String? = null,
    val isTranslating: Boolean = false,
    val translationError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class WorkViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as cz.kovmak.pomocnik.PomocnikApp).database
    private val settingsRepo = SettingsRepository(application)
    private lateinit var repository: WorkRepository

    private val _formState = MutableStateFlow(WorkFormState())
    val formState: StateFlow<WorkFormState> = _formState

    val userProfile = settingsRepo.userProfile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _translationResult = MutableStateFlow<String?>(null)
    val translationResult: StateFlow<String?> = _translationResult

    init {
        loadDefaults()
    }

    private fun loadDefaults() {
        viewModelScope.launch {
            userProfile.collect { profile ->
                profile?.let {
                    _formState.update {
                        it.copy(
                            workType = it.workType.takeIf { wt -> wt.isNotEmpty() } ?: it.workType,
                            startTime = it.startTime.takeIf { st -> st.isNotEmpty() } ?: it.startTime,
                            endTime = it.endTime.takeIf { et -> et.isNotEmpty() } ?: it.endTime
                        )
                    }
                }
            }
        }
    }

    fun updateOrderId(orderId: String) {
        _formState.update { it.copy(orderId = orderId) }
    }

    fun updateWorkType(workType: String) {
        _formState.update { it.copy(workType = workType) }
    }

    fun updateDescriptionUa(description: String) {
        _formState.update { it.copy(descriptionUa = description) }
    }

    fun updateMaterials(materials: String) {
        _formState.update { it.copy(materials = materials) }
    }

    fun updateStartTime(time: String) {
        _formState.update { it.copy(startTime = time) }
        calculateHours()
    }

    fun updateEndTime(time: String) {
        _formState.update { it.copy(endTime = time) }
        calculateHours()
    }

    fun setPhotoUri(uri: String?) {
        _formState.update { it.copy(photoUri = uri) }
    }

    private fun calculateHours() {
        val state = _formState.value
        if (state.startTime.isNotEmpty() && state.endTime.isNotEmpty()) {
            try {
                val startParts = state.startTime.split(":")
                val endParts = state.endTime.split(":")
                val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
                val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
                val hours = (endMinutes - startMinutes) / 60.0
                _formState.update { it.copy(hours = hours.coerceAtLeast(0.0)) }
            } catch (_: Exception) {
            }
        }
    }

    fun translate(apiKey: String) {
        val description = _formState.value.descriptionUa
        if (description.isBlank()) return

        _formState.update { it.copy(isTranslating = true, translationError = null) }

        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))
                val translated = repository.translateToCzech(description, apiKey)
                _translationResult.value = translated
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isTranslating = false,
                        translationError = "Chyba překladu: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun saveEntry(apiKey: String) {
        val state = _formState.value
        val profile = userProfile.value

        if (state.descriptionUa.isBlank()) {
            _formState.update { it.copy(translationError = "Zadejte popis práce") }
            return
        }

        _formState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))

                val descriptionCz = _translationResult.value ?: run {
                    repository.translateToCzech(state.descriptionUa, apiKey)
                }

                val entry = WorkEntry(
                    orderId = state.orderId,
                    workType = state.workType,
                    descriptionUa = state.descriptionUa,
                    descriptionCz = descriptionCz,
                    materials = state.materials,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    hours = state.hours,
                    photoUri = state.photoUri,
                    userName = profile?.name ?: "",
                    userEmail = profile?.email ?: ""
                )

                repository.insertEntry(entry)
                _formState.update { it.copy(isSaving = false, saveSuccess = true) }

                // Reset form after delay
                kotlinx.coroutines.delay(1500)
                resetForm(profile)

            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isSaving = false,
                        translationError = "Chyba uložení: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun resetForm(profile: cz.kovmak.pomocnik.data.settings.UserProfile?) {
        _formState.value = WorkFormState(
            workType = profile?.defaultWorkType ?: "E",
            startTime = profile?.defaultStartTime ?: "07:00",
            endTime = profile?.defaultEndTime ?: "15:30"
        )
        _translationResult.value = null
    }

    fun resetError() {
        _formState.update { it.copy(translationError = null, saveSuccess = false) }
    }
}
