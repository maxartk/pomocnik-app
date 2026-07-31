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
import cz.kovmak.pomocnik.data.model.SapNotificationData
import cz.kovmak.pomocnik.data.model.SapDurationCalculator
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
    val endDate: String = "",
    val endDateManuallyEdited: Boolean = false,
    val hours: Double = 0.0,
    val photoUri: String? = null,
    val detailPhotoUri: String? = null,
    val notification: SapNotificationData = SapNotificationData(),
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
    val isAutoFilling: Boolean = false,
    val notificationConfirmed: Boolean = false,
    val isReadingPhoto: Boolean = false
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
                        state.copy(workType = state.workType.ifBlank { profile.defaultWorkType }.ifBlank { "E" })
                    }
                }
            }
        }
    }

    // ── Form field updates ────────────────────────────────────────────────────

    fun updateOrderId(orderId: String) {
        _formState.update {
            it.copy(orderId = orderId, notification = it.notification.copy(orderId = orderId), notificationConfirmed = false)
        }
        invalidateGeneratedResults()
    }
    fun updateWorkType(workType: String) = _formState.update { it.copy(workType = workType) }
    fun updateDescriptionUa(desc: String) {
        _formState.update { it.copy(descriptionUa = desc) }
        invalidateGeneratedResults()
    }
    fun updateMaterials(materials: String) = _formState.update { it.copy(materials = materials) }
    fun updateStartTime(time: String) {
        _formState.update { state ->
            val resolvedEndDate = SapDurationCalculator.resolveEndDate(
                state.notification.notificationDate,
                time,
                state.endTime,
                state.endDate,
                state.endDateManuallyEdited
            )
            state.copy(
                startTime = time,
                notification = state.notification.copy(notificationTime = time),
                notificationConfirmed = false,
                endDate = resolvedEndDate
            )
        }
        invalidateGeneratedResults()
        calculateHours()
    }
    fun updateEndTime(time: String) {
        _formState.update { state ->
            val resolvedEndDate = SapDurationCalculator.resolveEndDate(
                state.notification.notificationDate,
                state.startTime,
                time,
                state.endDate,
                state.endDateManuallyEdited
            )
            state.copy(endTime = time, endDate = resolvedEndDate)
        }
        calculateHours()
    }
    fun updateEndDate(date: String) {
        _formState.update { it.copy(endDate = date, endDateManuallyEdited = true) }
        calculateHours()
    }
    fun setPhotoUri(uri: String?) {
        _formState.update {
            it.copy(
                photoUri = uri,
                notification = SapNotificationData(),
                notificationConfirmed = false,
                orderId = "",
                startTime = "",
                endDate = "",
                endDateManuallyEdited = false,
                hours = 0.0
            )
        }
        invalidateGeneratedResults()
    }
    fun setDetailPhotoUri(uri: String?) {
        _formState.update { it.copy(detailPhotoUri = uri) }
        invalidateGeneratedResults()
    }
    fun setMode(mode: String) = _formState.update { it.copy(mode = mode) }

    fun updateSapObjectPart(code: String) = _formState.update { it.copy(sapObjectPart = code) }
    fun updateSapDamageDesc(code: String) = _formState.update { it.copy(sapDamageDesc = code) }
    fun updateSapDamageText(text: String) = _formState.update { it.copy(sapDamageText = text) }
    fun updateSapCause(code: String) = _formState.update { it.copy(sapCause = code) }
    fun updateSapCauseText(text: String) = _formState.update { it.copy(sapCauseText = text) }
    fun updateSapImpact(code: String) = _formState.update { it.copy(sapImpact = code) }
    fun updateNotificationDate(value: String) {
        _formState.update { state ->
            val resolvedEndDate = SapDurationCalculator.resolveEndDate(
                value,
                state.startTime,
                state.endTime,
                state.endDate,
                state.endDateManuallyEdited
            )
            state.copy(
                notification = state.notification.copy(notificationDate = value),
                notificationConfirmed = false,
                endDate = resolvedEndDate
            )
        }
        invalidateGeneratedResults()
        calculateHours()
    }
    fun updateNotificationAuthor(value: String) {
        _formState.update { it.copy(notification = it.notification.copy(author = value), notificationConfirmed = false) }
        invalidateGeneratedResults()
    }
    fun updateTechnicalLocation(value: String) {
        _formState.update { it.copy(notification = it.notification.copy(technicalLocation = value), notificationConfirmed = false) }
        invalidateGeneratedResults()
    }
    fun updateNotificationText(value: String) {
        _formState.update { it.copy(notification = it.notification.copy(notificationText = value), notificationConfirmed = false) }
        invalidateGeneratedResults()
    }
    fun updateNotificationPriority(value: String) {
        _formState.update { it.copy(notification = it.notification.copy(priority = value), notificationConfirmed = false) }
        invalidateGeneratedResults()
    }

    fun confirmNotification() {
        val state = _formState.value
        when {
            state.orderId.isBlank() || state.notification.notificationText.isBlank() ->
                _formState.update { it.copy(translationError = "Перевір номер заказки та текст hlášení") }
            !SapDurationCalculator.isValidDate(state.notification.notificationDate) ->
                _formState.update { it.copy(translationError = "Некоректна дата hlášení") }
            !SapDurationCalculator.isValidTime(state.startTime) ->
                _formState.update { it.copy(translationError = "Некоректний час hlášení") }
            else -> _formState.update { it.copy(notificationConfirmed = true, translationError = null) }
        }
    }
    fun resetError() = _formState.update { it.copy(translationError = null, saveSuccess = false) }

    private fun invalidateGeneratedResults() {
        _translationResult.value = null
        _technicalReport.value = null
        _formState.update {
            it.copy(
                sapObjectPart = "",
                sapDamageDesc = "",
                sapDamageText = "",
                sapCause = "",
                sapCauseText = "",
                sapImpact = ""
            )
        }
    }

    fun loadEntry(entry: WorkEntry) {
        _formState.value = WorkFormState(
            orderId = entry.orderId,
            workType = entry.workType,
            descriptionUa = entry.descriptionUa,
            materials = entry.materials,
            startTime = entry.startTime,
            endTime = entry.endTime,
            endDate = entry.sapFailureEndDate.ifBlank { entry.sapNotificationDate },
            endDateManuallyEdited = true,
            hours = entry.hours,
            notification = SapNotificationData(
                orderId = entry.orderId,
                technicalLocation = entry.sapTechnicalLocation,
                notificationText = entry.sapNotificationText,
                author = entry.sapNotificationAuthor,
                notificationDate = entry.sapNotificationDate,
                notificationTime = entry.startTime,
                priority = entry.sapPriority
            ),
            sapObjectPart = entry.sapObjectPart,
            sapDamageDesc = entry.sapDamageDesc,
            sapDamageText = entry.sapDamageText,
            sapCause = entry.sapCause,
            sapCauseText = entry.sapCauseText,
            sapImpact = entry.sapImpact,
            notificationConfirmed = entry.sapNotificationText.isNotBlank()
        )
        _translationResult.value = entry.descriptionCz.ifBlank { null }
        _technicalReport.value = entry.technicalReport.ifBlank { null }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun getModel(): String = userProfile.value?.selectedModel ?: ModelConfig.DEFAULT_MODEL

    private fun apiKey(): String = userProfile.value?.openRouterApiKey ?: ""

    private fun ocrAccessKey(): String = userProfile.value?.ocrAccessKey ?: ""

    private fun parseMinutes(value: String): Int? {
        val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(value.trim()) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    private fun normalizeTime(value: String): String {
        val match = Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?$").matchEntire(value.trim()) ?: return ""
        val h = match.groupValues[1].toIntOrNull() ?: return ""
        val m = match.groupValues[2].toIntOrNull() ?: return ""
        if (h !in 0..23 || m !in 0..59) return ""
        return "%02d:%02d".format(h, m)
    }

    private fun calculateHours() {
        val s = _formState.value
        val calculated = SapDurationCalculator.hours(
            s.notification.notificationDate,
            s.startTime,
            s.endDate,
            s.endTime
        )
        _formState.update { it.copy(hours = calculated) }
    }

    /**
     * Converts a content:// or file:// URI to a JPEG byte array suitable for OCR.Space free API limits.
     */
    private fun uriToOcrJpeg(
        uriString: String,
        maxDimension: Int = 1800,
        maxBytes: Int = 900_000
    ): ByteArray? {
        return try {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val source = BitmapFactory.decodeStream(inputStream).also { inputStream.close() } ?: return null

            var bitmap = if (source.width > maxDimension || source.height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(source.width, source.height)
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true
                ).also { source.recycle() }
            } else {
                source
            }

            fun compress(current: Bitmap, quality: Int): ByteArray {
                val output = ByteArrayOutputStream()
                current.compress(Bitmap.CompressFormat.JPEG, quality, output)
                return output.toByteArray()
            }

            var quality = 90
            var bytes = compress(bitmap, quality)
            while (bytes.size > maxBytes && quality > 55) {
                quality -= 5
                bytes = compress(bitmap, quality)
            }

            var resizeAttempts = 0
            while (bytes.size > maxBytes && resizeAttempts < 4) {
                val smaller = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * 0.82f).toInt().coerceAtLeast(1),
                    (bitmap.height * 0.82f).toInt().coerceAtLeast(1),
                    true
                )
                bitmap.recycle()
                bitmap = smaller
                bytes = compress(bitmap, 72)
                resizeAttempts++
            }
            bitmap.recycle()
            bytes.takeIf { it.isNotEmpty() && it.size <= maxBytes }
        } catch (e: Exception) {
            android.util.Log.e("Pomocnik", "uriToOcrJpeg failed: ${e.message}", e)
            null
        }
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

    /** First step: send the SAP screenshot to n8n OCR.Space and prefill reviewed notification context. */
    fun readSapNotification() {
        val photoUri = _formState.value.photoUri
        if (photoUri.isNullOrBlank()) {
            _formState.update { it.copy(translationError = "Додай фото hlášení з SAP") }
            return
        }
        val accessKey = ocrAccessKey()
        if (accessKey.isBlank()) {
            _formState.update { it.copy(translationError = "Додай Pomocnik OCR access key у Налаштуваннях") }
            return
        }
        invalidateGeneratedResults()
        _formState.update { it.copy(isReadingPhoto = true, translationError = null) }
        viewModelScope.launch {
            try {
                val image = uriToOcrJpeg(photoUri)
                    ?: throw IllegalStateException("Не вдалося підготувати фото до 900 KB")
                val notification = repository.extractSapNotification(image, accessKey)
                if (_formState.value.photoUri != photoUri) {
                    _formState.update {
                        it.copy(isReadingPhoto = false, translationError = "Фото змінилося під час OCR — прочитай його ще раз")
                    }
                    return@launch
                }
                if (notification.notificationText.isBlank() && notification.orderId.isBlank()) {
                    throw IllegalStateException("OCR не зміг прочитати hlášení. Зроби чіткіше фото")
                }
                _formState.update {
                    it.copy(
                        notification = notification,
                        orderId = notification.orderId,
                        startTime = notification.notificationTime,
                        endDate = notification.notificationDate,
                        endDateManuallyEdited = false,
                        notificationConfirmed = false,
                        isReadingPhoto = false
                    )
                }
                calculateHours()
            } catch (e: Exception) {
                _formState.update {
                    it.copy(isReadingPhoto = false, translationError = "Помилка OCR SAP: ${e.localizedMessage}")
                }
            }
        }
    }

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
        if (state.notification.notificationText.isBlank() || state.notification.notificationTime.isBlank()) {
            _formState.update { it.copy(translationError = "Спочатку натисни «РОЗПІЗНАТИ HLÁŠENÍ» і перевір дані") }
            return
        }
        if (!state.notificationConfirmed) {
            _formState.update { it.copy(translationError = "Підтвердь, що дані з фото прочитані правильно") }
            return
        }
        if (!SapDurationCalculator.isValidDate(state.endDate) || !SapDurationCalculator.isValidTime(state.endTime)) {
            _formState.update { it.copy(translationError = "Перевір дату й час завершення порухи") }
            return
        }
        if (state.hours <= 0.0) {
            _formState.update { it.copy(translationError = "Кінець порухи має бути після початку") }
            return
        }
        if (apiKey.isBlank()) {
            _formState.update { it.copy(translationError = "Zadejte API klíč v nastavení") }
            return
        }

        _formState.update { it.copy(isTranslating = true, translationError = null) }
        viewModelScope.launch {
            try {
                val detailImageBase64 = state.detailPhotoUri?.let { uriToBase64(it, maxSize = 1600, quality = 85) }
                val draft = repository.generateReportFromSapPhotos(
                    notification = state.notification,
                    repairNote = state.descriptionUa,
                    detailImageBase64 = detailImageBase64,
                    apiKey = apiKey,
                    model = getModel()
                )
                val sapFields = repository.extractSapFields(
                    draft.descriptionCz,
                    state.descriptionUa,
                    apiKey,
                    getModel(),
                    state.notification
                )
                val current = _formState.value
                if (
                    current.notification != state.notification ||
                    current.descriptionUa != state.descriptionUa ||
                    current.detailPhotoUri != state.detailPhotoUri ||
                    !current.notificationConfirmed
                ) {
                    _formState.update {
                        it.copy(
                            isTranslating = false,
                            translationError = "Дані змінилися під час генерації — запусти її ще раз"
                        )
                    }
                    return@launch
                }
                _translationResult.value = draft.descriptionCz
                _technicalReport.value = draft.technicalReport
                _formState.update {
                    it.copy(
                        isTranslating = false,
                        sapObjectPart = sapFields.objectPart,
                        sapDamageDesc = sapFields.damageDesc,
                        sapDamageText = sapFields.damageText,
                        sapCause = sapFields.cause,
                        sapCauseText = sapFields.causeText,
                        sapImpact = sapFields.impact
                    )
                }
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
                val sapFields = repository.extractSapFields(
                    translation, state.descriptionUa, apiKey, getModel(), state.notification
                )
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

        if (!state.notificationConfirmed) {
            _formState.update { it.copy(translationError = "Перевір і підтвердь дані hlášení") }
            return
        }
        if (!SapDurationCalculator.isValidDate(state.notification.notificationDate) ||
            !SapDurationCalculator.isValidTime(state.startTime) ||
            !SapDurationCalculator.isValidDate(state.endDate) ||
            !SapDurationCalculator.isValidTime(state.endTime) ||
            state.hours <= 0.0
        ) {
            _formState.update { it.copy(translationError = "Перевір дату й час початку та завершення порухи") }
            return
        }
        val generatedCz = _translationResult.value
        val generatedReport = _technicalReport.value
        if (generatedCz.isNullOrBlank() || generatedReport.isNullOrBlank()) {
            _formState.update { it.copy(translationError = "Спочатку згенеруй спільне hlášení з даних автора і твого опису") }
            return
        }

        _formState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val descCz = generatedCz
                val techReport = generatedReport

                val sapFields = if (state.sapObjectPart.isBlank() && state.sapDamageDesc.isBlank()) {
                    repository.extractSapFields(descCz, state.descriptionUa, apiKey, getModel(), state.notification)
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
                    sapFailureEndDate = state.endDate,
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
                    sapNotificationDate = state.notification.notificationDate,
                    sapNotificationAuthor = state.notification.author,
                    sapTechnicalLocation = state.notification.technicalLocation,
                    sapNotificationText = state.notification.notificationText,
                    sapPriority = state.notification.priority,
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
        _formState.value = WorkFormState(workType = profile?.defaultWorkType ?: "E")
        _translationResult.value = null
        _advisorResult.value = null
        _technicalReport.value = null
    }
}
