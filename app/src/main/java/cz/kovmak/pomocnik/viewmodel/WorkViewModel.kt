package cz.kovmak.pomocnik.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.data.repository.WorkRepository
import cz.kovmak.pomocnik.data.settings.SettingsRepository
import cz.kovmak.pomocnik.data.settings.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class WorkFormState(
    val orderId: String = "",
    val workType: String = "E",
    val descriptionUa: String = "",
    val materials: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val hours: Double = 0.0,
    val photoUri: String? = null,
    val mode: String = "submit", // submit | advisor
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

    val userProfile: StateFlow<UserProfile?> = settingsRepo.userProfile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _translationResult = MutableStateFlow<String?>(null)
    val translationResult: StateFlow<String?> = _translationResult

    private val _advisorResult = MutableStateFlow<String?>(null)
    val advisorResult: StateFlow<String?> = _advisorResult

    private val _technicalReport = MutableStateFlow<String?>(null)
    val technicalReport: StateFlow<String?> = _technicalReport

    init {
        loadDefaults()
    }

    private fun loadDefaults() {
        viewModelScope.launch {
            userProfile.collect { profile ->
                profile?.let {
                    _formState.update {
                        it.copy(
                            workType = it.workType.ifBlank { profile.defaultWorkType }.ifBlank { "E" },
                            startTime = it.startTime.ifBlank { profile.defaultStartTime }.ifBlank { "07:00" },
                            endTime = it.endTime.ifBlank { profile.defaultEndTime }.ifBlank { "15:30" }
                        )
                    }
                }
            }
        }
    }

    fun updateOrderId(orderId: String) = _formState.update { it.copy(orderId = orderId) }
    fun updateWorkType(workType: String) = _formState.update { it.copy(workType = workType) }
    fun updateDescriptionUa(desc: String) = _formState.update { it.copy(descriptionUa = desc) }
    fun updateMaterials(materials: String) = _formState.update { it.copy(materials = materials) }
    fun updateStartTime(time: String) { _formState.update { it.copy(startTime = time) }; calculateHours() }
    fun updateEndTime(time: String) { _formState.update { it.copy(endTime = time) }; calculateHours() }
    fun setPhotoUri(uri: String?) = _formState.update { it.copy(photoUri = uri) }
    fun setMode(mode: String) = _formState.update { it.copy(mode = mode) }

    private fun calculateHours() {
        val s = _formState.value
        if (s.startTime.isNotEmpty() && s.endTime.isNotEmpty()) {
            try {
                val sm = s.startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                val em = s.endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                _formState.update { it.copy(hours = ((em - sm) / 60.0).coerceAtLeast(0.0)) }
            } catch (_: Exception) {}
        }
    }

    /**
     * Convert URI to Base64 string for sending to vision API.
     * Compresses image to reasonable size for API calls.
     */
    private fun uriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            // Scale down if too large (max 1024px for API efficiency)
            val maxSize = 1024
            var width = bitmap.width
            var height = bitmap.height
            if (width > maxSize || height > maxSize) {
                val scale = maxSize.toFloat() / maxOf(width, height)
                width = (width * scale).toInt()
                height = (height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
                bitmap.recycle()
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                scaled.recycle()
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            } else {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                bitmap.recycle()
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** SUBMIT mode: переклад UA→CS */
    fun translate(apiKey: String) {
        val desc = _formState.value.descriptionUa
        if (desc.isBlank()) return
        _formState.update { it.copy(isTranslating = true, translationError = null) }
        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))
                val translated = repository.translateToCzech(desc, apiKey)
                _translationResult.value = translated
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                _formState.update { it.copy(isTranslating = false, translationError = "Chyba: ${e.localizedMessage}") }
            }
        }
    }

    /** ADVISOR mode: питання до електрика (з фото або без) */
    fun askAdvisor(apiKey: String) {
        val question = _formState.value.descriptionUa
        if (question.isBlank()) return
        _formState.update { it.copy(isTranslating = true, translationError = null) }
        
        val photoUri = _formState.value.photoUri
        val imageBase64 = photoUri?.let { uriToBase64(it) }
        
        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))
                val answer = repository.askAdvisor(question, apiKey, imageBase64)
                _advisorResult.value = answer
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                _formState.update { it.copy(isTranslating = false, translationError = "Chyba: ${e.localizedMessage}") }
            }
        }
    }

    /** Генерація технічної зправи після перекладу */
    fun generateReport(apiKey: String) {
        val state = _formState.value
        val translation = _translationResult.value ?: return
        val profile = userProfile.value ?: return
        _formState.update { it.copy(isTranslating = true) }
        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))
                val report = repository.generateTechnicalReport(
                    descriptionCz = translation,
                    descriptionUa = state.descriptionUa,
                    orderId = state.orderId,
                    workType = state.workType,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    hours = state.hours,
                    materials = state.materials,
                    apiKey = apiKey
                )
                _technicalReport.value = report
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                _formState.update { it.copy(isTranslating = false, translationError = "Chyba reportu: ${e.localizedMessage}") }
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
                val descCz = _translationResult.value ?: repository.translateToCzech(state.descriptionUa, apiKey)
                val techReport = _technicalReport.value ?: ""
                val entry = WorkEntry(
                    orderId = state.orderId, workType = state.workType,
                    descriptionUa = state.descriptionUa, descriptionCz = descCz,
                    technicalReport = techReport,
                    materials = state.materials, startTime = state.startTime,
                    endTime = state.endTime, hours = state.hours,
                    photoUri = state.photoUri, userName = profile?.name ?: "",
                    userEmail = profile?.email ?: ""
                )
                repository.insertEntry(entry)
                _formState.update { it.copy(isSaving = false, saveSuccess = true) }
                kotlinx.coroutines.delay(1500)
                resetForm(profile)
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, translationError = "Chyba: ${e.localizedMessage}") }
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
        _advisorResult.value = null
        _technicalReport.value = null
    }

    fun resetError() = _formState.update { it.copy(translationError = null, saveSuccess = false) }
}
