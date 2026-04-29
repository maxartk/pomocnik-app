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
import cz.kovmak.pomocnik.data.network.ModelConfig
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
    val saveSuccess: Boolean = false,

    // SAP fields
    val sapObjectPart: String = "",
    val sapDamageDesc: String = "",
    val sapDamageText: String = "",
    val sapCause: String = "",
    val sapCauseText: String = "",
    val sapImpact: String = "",
    val isAutoFilling: Boolean = false
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

    fun updateSapObjectPart(code: String) = _formState.update { it.copy(sapObjectPart = code) }
    fun updateSapDamageDesc(code: String) = _formState.update { it.copy(sapDamageDesc = code) }
    fun updateSapDamageText(text: String) = _formState.update { it.copy(sapDamageText = text) }
    fun updateSapCause(code: String) = _formState.update { it.copy(sapCause = code) }
    fun updateSapCauseText(text: String) = _formState.update { it.copy(sapCauseText = text) }
    fun updateSapImpact(code: String) = _formState.update { it.copy(sapImpact = code) }

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
     * Supports both content:// and file:// URIs.
     */
    private fun uriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>()
            val inputStream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                // Fallback: try file:// URI
                null
            } ?: return null
            
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()
            
            // Scale down if too large (max 1024px for API efficiency + size limit)
            val maxSize = 1024
            var width = bitmap.width
            var height = bitmap.height
            val outputStream = ByteArrayOutputStream()
            if (width > maxSize || height > maxSize) {
                val scale = maxSize.toFloat() / maxOf(width, height)
                width = (width * scale).toInt()
                height = (height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
                bitmap.recycle()
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                scaled.recycle()
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                bitmap.recycle()
            }
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("Pomocnik", "uriToBase64 failed: ${e.message}", e)
            null
        }
    }

    /** Get the selected model from user profile, fallback to default */
    private fun getModel(): String = userProfile.value?.selectedModel ?: ModelConfig.DEFAULT_MODEL

    /** SUBMIT mode: переклад UA→CS */
    fun translate(apiKey: String) {
        val desc = _formState.value.descriptionUa
        if (desc.isBlank()) return
        val model = getModel()
        _formState.update { it.copy(isTranslating = true, translationError = null) }
        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))
                val translated = repository.translateToCzech(desc, apiKey, model)
                _translationResult.value = translated
                _formState.update { it.copy(isTranslating = false) }
                
                // Auto-fill SAP fields
                autoFillSapFields(apiKey)
            } catch (e: Exception) {
                _formState.update { it.copy(isTranslating = false, translationError = "Chyba: ${e.localizedMessage}") }
            }
        }
    }

    /** ADVISOR mode: питання до електрика (з фото або без) */
    fun askAdvisor(apiKey: String) {
        val question = _formState.value.descriptionUa
        if (question.isBlank()) return
        val model = getModel()
        _formState.update { it.copy(isTranslating = true, translationError = null) }
        
        val photoUri = _formState.value.photoUri
        val imageBase64 = photoUri?.let { uri ->
            val result = uriToBase64(uri)
            if (result == null) {
                android.util.Log.e("Pomocnik", "Failed to convert photo to base64: $uri")
            } else {
                android.util.Log.d("Pomocnik", "Photo converted to base64, size=${result.length}")
            }
            result
        }
        
        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))
                val answer = repository.askAdvisor(question, apiKey, model, imageBase64)
                _advisorResult.value = answer
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                android.util.Log.e("Pomocnik", "Advisor error: ${e.message}", e)
                _formState.update { it.copy(isTranslating = false, translationError = "Помилка: ${e.localizedMessage ?: e.toString().take(100)}") }
            }
        }
    }

    /** Генерація технічної зправи після перекладу */
    fun generateReport(apiKey: String) {
        val state = _formState.value
        val translation = _translationResult.value ?: return
        val profile = userProfile.value ?: return
        val model = getModel()
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
                    apiKey = apiKey,
                    model = model
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
        val model = getModel()
        _formState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))
                val descCz = _translationResult.value ?: repository.translateToCzech(state.descriptionUa, apiKey, model)
                val techReport = _technicalReport.value ?: ""
                val entry = WorkEntry(
                    orderId = state.orderId, workType = state.workType,
                    descriptionUa = state.descriptionUa, descriptionCz = descCz,
                    technicalReport = techReport,
                    materials = state.materials, startTime = state.startTime,
                    endTime = state.endTime, hours = state.hours,
                    photoUri = state.photoUri, userName = profile?.name ?: "",
                    userEmail = profile?.email ?: "",
                    sapObjectPart = state.sapObjectPart,
                    sapDamageDesc = state.sapDamageDesc,
                    sapDamageText = state.sapDamageText,
                    sapCause = state.sapCause,
                    sapCauseText = state.sapCauseText,
                    sapImpact = state.sapImpact,
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

    /** Clear text and all results (translation, advisor, report) */
    fun clearResults() {
        _formState.update { it.copy(descriptionUa = "", translationError = null) }
        _translationResult.value = null
        _advisorResult.value = null
        _technicalReport.value = null
    }

    fun autoFillSapFields(apiKey: String) {
        val state = _formState.value
        val translation = _translationResult.value ?: state.descriptionUa
        if (translation.isBlank()) return
        val model = getModel()
        _formState.update { it.copy(isAutoFilling = true, translationError = null) }
        viewModelScope.launch {
            try {
                repository = WorkRepository(database.workEntryDao(), cz.kovmak.pomocnik.data.network.OpenRouterApi.create(apiKey))
                val sapFields = repository.extractSapFields(translation, state.descriptionUa, apiKey, model)
                _formState.update { 
                    it.copy(
                        sapObjectPart = sapFields.objectPart,
                        sapDamageDesc = sapFields.damageDesc,
                        sapDamageText = sapFields.damageText,
                        sapCause = sapFields.cause,
                        sapCauseText = sapFields.causeText,
                        sapImpact = sapFields.impact,
                        isAutoFilling = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("Pomocnik", "SAP auto-fill error: ${e.message}", e)
                _formState.update { it.copy(isAutoFilling = false, translationError = "SAP auto-fill chyba: ${e.localizedMessage}") }
            }
        }
    }
}