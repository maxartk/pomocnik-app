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
import cz.kovmak.pomocnik.data.network.OpenRouterApi
import cz.kovmak.pomocnik.data.model.SapFieldResult
import cz.kovmak.pomocnik.BuildConfig
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
    val detailPhotoUri: String? = null,
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

    // Repository initialized once — avoids lateinit crash if apiKey not yet loaded
    private val repository: WorkRepository = WorkRepository(
        database.workEntryDao(),
        OpenRouterApi.create("")
    )

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
                    _formState.update { state ->
                        state.copy(
                            workType = state.workType.ifBlank { profile.defaultWorkType }.ifBlank { "E" },
                            startTime = state.startTime.ifBlank { profile.defaultStartTime }.ifBlank { "07:00" },
                            endTime = state.endTime.ifBlank { profile.defaultEndTime }.ifBlank { "15:30" }
                        )
                    }
                }
            }
        }
    }

    // ── Form field updates ────────────────────────────────────────────────────

    fun updateOrderId(orderId: String) = _formState.update { it.copy(orderId = orderId) }
    fun updateWorkType(workType: String) = _formState.update { it.copy(workType = workType) }
    fun updateDescriptionUa(desc: String) = _formState.update { it.copy(descriptionUa = desc) }
    fun updateMaterials(materials: String) = _formState.update { it.copy(materials = materials) }
    fun updateStartTime(time: String) { _formState.update { it.copy(startTime = time) }; calculateHours() }
    fun updateEndTime(time: String) { _formState.update { it.copy(endTime = time) }; calculateHours() }
    fun setPhotoUri(uri: String?) = _formState.update { it.copy(photoUri = uri) }
    fun setDetailPhotoUri(uri: String?) = _formState.update { it.copy(detailPhotoUri = uri) }
    fun setMode(mode: String) = _formState.update { it.copy(mode = mode) }

    fun updateSapObjectPart(code: String) = _formState.update { it.copy(sapObjectPart = code) }
    fun updateSapDamageDesc(code: String) = _formState.update { it.copy(sapDamageDesc = code) }
    fun updateSapDamageText(text: String) = _formState.update { it.copy(sapDamageText = text) }
    fun updateSapCause(code: String) = _formState.update { it.copy(sapCause = code) }
    fun updateSapCauseText(text: String) = _formState.update { it.copy(sapCauseText = text) }
    fun updateSapImpact(code: String) = _formState.update { it.copy(sapImpact = code) }
    fun resetError() = _formState.update { it.copy(translationError = null, saveSuccess = false) }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun getModel(): String = userProfile.value?.selectedModel ?: ModelConfig.DEFAULT_MODEL

    private fun apiKey(): String = userProfile.value?.openRouterApiKey ?: ""

    private fun parseMinutes(value: String): Int? {
        val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(value.trim()) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun calculateHours() {
        val s = _formState.value
        val start = parseMinutes(s.startTime) ?: return
        val end = parseMinutes(s.endTime) ?: return
        val diff = if (end >= start) end - start else (24 * 60 - start) + end
        _formState.update { it.copy(hours = diff / 60.0) }
    }

    /**
     * Converts a content:// or file:// URI to a base64 JPEG string.
     * Scales down images to reduce API payload.
     */
    private fun uriToBase64(uriString: String, maxSize: Int = 1024, quality: Int = 75): String? {
        return try {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream).also { inputStream.close() } ?: return null

            val scaled = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                    .also { bitmap.recycle() }
            } else bitmap

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("Pomocnik", "uriToBase64 failed: ${e.message}", e)
            null
        }
    }

    // ── API actions ──────────────────────────────────────────────────────────

    /** SUBMIT mode: Translate UA→CS. SAP auto-fill is NOT called automatically — user triggers it manually. */
    fun translate(apiKey: String = apiKey()) {
        val desc = _formState.value.descriptionUa
        if (desc.isBlank()) return
        if (apiKey.isBlank()) {
            _formState.update { it.copy(translationError = "Zadejte API klíč v nastavení") }
            return
        }
        _formState.update { it.copy(isTranslating = true, translationError = null) }
        viewModelScope.launch {
            try {
                val translated = repository.translateToCzech(desc, apiKey, getModel())
                _translationResult.value = translated
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                _formState.update { it.copy(isTranslating = false, translationError = "Chyba překladu: ${e.localizedMessage}") }
            }
        }
    }

    /** ADVISOR mode: Ask question with optional photo. */
    fun askAdvisor(apiKey: String = apiKey()) {
        val question = _formState.value.descriptionUa
        if (question.isBlank()) return
        if (apiKey.isBlank()) {
            _formState.update { it.copy(translationError = "Zadejte API klíč v nastavení") }
            return
        }
        _formState.update { it.copy(isTranslating = true, translationError = null) }

        val imageBase64 = _formState.value.photoUri?.let { uri ->
            uriToBase64(uri).also { result ->
                if (BuildConfig.DEBUG) {
                    if (result == null) android.util.Log.e("Pomocnik", "Failed to convert photo")
                    else android.util.Log.d("Pomocnik", "Photo base64 size=${result.length}")
                }
            }
        }

        viewModelScope.launch {
            try {
                val answer = repository.askAdvisor(question, apiKey, getModel(), imageBase64)
                _advisorResult.value = answer
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                android.util.Log.e("Pomocnik", "Advisor error: ${e.message}", e)
                _formState.update { it.copy(isTranslating = false, translationError = "Помилка: ${e.localizedMessage ?: e.toString().take(100)}") }
            }
        }
    }


    /** Build a SAP-ready Czech report from SAP order photo, optional detail photo and worker note. */
    fun generateReportFromPhotos(apiKey: String = apiKey()) {
        val state = _formState.value
        if (state.descriptionUa.isBlank()) {
            _formState.update { it.copy(translationError = "Напиши коротко, як ти це виправив") }
            return
        }
        if (state.photoUri.isNullOrBlank()) {
            _formState.update { it.copy(translationError = "Додай фото заказки з SAP") }
            return
        }
        if (apiKey.isBlank()) {
            _formState.update { it.copy(translationError = "Zadejte API klíč v nastavení") }
            return
        }

        _formState.update { it.copy(isTranslating = true, translationError = null) }
        viewModelScope.launch {
            try {
                val orderImageBase64 = uriToBase64(state.photoUri, maxSize = 1600, quality = 85)
                val detailImageBase64 = state.detailPhotoUri?.let { uriToBase64(it, maxSize = 1600, quality = 85) }
                val draft = repository.generateReportFromSapPhotos(
                    repairNote = state.descriptionUa,
                    orderImageBase64 = orderImageBase64,
                    detailImageBase64 = detailImageBase64,
                    apiKey = apiKey,
                    model = getModel()
                )
                _translationResult.value = draft.descriptionCz
                _technicalReport.value = draft.technicalReport
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                _formState.update { it.copy(isTranslating = false, translationError = "Chyba zprávy z fotek: ${e.localizedMessage}") }
            }
        }
    }

    /** Generate technical report for SAP IW41. */
    fun generateReport(apiKey: String = apiKey()) {
        val state = _formState.value
        val translation = _translationResult.value ?: return
        if (apiKey.isBlank()) return
        _formState.update { it.copy(isTranslating = true) }
        viewModelScope.launch {
            try {
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
                    model = getModel()
                )
                _technicalReport.value = report
                _formState.update { it.copy(isTranslating = false) }
            } catch (e: Exception) {
                _formState.update { it.copy(isTranslating = false, translationError = "Chyba zprávy: ${e.localizedMessage}") }
            }
        }
    }

    /** Auto-fill SAP fields from the current translation or description. */
    fun autoFillSapFields(apiKey: String = apiKey()) {
        val state = _formState.value
        val translation = _translationResult.value ?: state.descriptionUa
        if (translation.isBlank()) return
        if (apiKey.isBlank()) return
        _formState.update { it.copy(isAutoFilling = true, translationError = null) }
        viewModelScope.launch {
            try {
                val sapFields = repository.extractSapFields(translation, state.descriptionUa, apiKey, getModel())
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

    /** Save the current work entry to the local database. */
    fun saveEntry(apiKey: String = apiKey()) {
        val state = _formState.value
        val profile = userProfile.value

        if (state.descriptionUa.isBlank()) {
            _formState.update { it.copy(translationError = "Zadejte popis práce") }
            return
        }
        if (apiKey.isBlank()) {
            _formState.update { it.copy(translationError = "Zadejte API klíč v nastavení") }
            return
        }

        val start = parseMinutes(state.startTime)
        val end = parseMinutes(state.endTime)
        if ((state.startTime.isNotBlank() || state.endTime.isNotBlank()) && (start == null || end == null)) {
            _formState.update { it.copy(translationError = "Čas musí být ve formátu HH:mm") }
            return
        }

        _formState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val descCz = _translationResult.value
                    ?: repository.translateToCzech(state.descriptionUa, apiKey, getModel())

                val techReport = _technicalReport.value ?: ""

                val sapFields = if (state.sapObjectPart.isBlank() && state.sapDamageDesc.isBlank()) {
                    repository.extractSapFields(descCz, state.descriptionUa, apiKey, getModel())
                } else {
                    SapFieldResult(
                        objectPart = state.sapObjectPart,
                        damageDesc = state.sapDamageDesc,
                        damageText = state.sapDamageText,
                        cause = state.sapCause,
                        causeText = state.sapCauseText,
                        impact = state.sapImpact
                    )
                }

                val entry = WorkEntry(
                    orderId = state.orderId,
                    workType = state.workType,
                    descriptionUa = state.descriptionUa,
                    descriptionCz = descCz,
                    technicalReport = techReport,
                    materials = state.materials,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    hours = state.hours,
                    photoUri = state.photoUri ?: state.detailPhotoUri,
                    userName = profile?.name ?: "",
                    userEmail = profile?.email ?: "",
                    sapObjectPart = sapFields.objectPart,
                    sapDamageDesc = sapFields.damageDesc,
                    sapDamageText = sapFields.damageText,
                    sapCause = sapFields.cause,
                    sapCauseText = sapFields.causeText,
                    sapImpact = sapFields.impact,
                )
                repository.insertEntry(entry)
                _formState.update { it.copy(isSaving = false, saveSuccess = true) }
                kotlinx.coroutines.delay(1500)
                resetForm(profile)
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, translationError = "Chyba uložení: ${e.localizedMessage}") }
            }
        }
    }

    /** Clear all results and the text field. */
    fun clearResults() {
        _formState.update { it.copy(descriptionUa = "", translationError = null) }
        _translationResult.value = null
        _advisorResult.value = null
        _technicalReport.value = null
    }

    private fun resetForm(profile: UserProfile?) {
        _formState.value = WorkFormState(
            workType = profile?.defaultWorkType ?: "E",
            startTime = profile?.defaultStartTime ?: "07:00",
            endTime = profile?.defaultEndTime ?: "15:30"
        )
        _translationResult.value = null
        _advisorResult.value = null
        _technicalReport.value = null
    }
}
