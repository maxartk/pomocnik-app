package cz.kovmak.pomocnik.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import cz.kovmak.pomocnik.data.network.ContentPart
import cz.kovmak.pomocnik.data.network.ImageUrl
import cz.kovmak.pomocnik.data.network.Message
import cz.kovmak.pomocnik.data.network.ModelConfig
import cz.kovmak.pomocnik.data.network.OpenRouterApi
import cz.kovmak.pomocnik.data.network.TranslationRequest
import cz.kovmak.pomocnik.data.settings.SettingsRepository
import cz.kovmak.pomocnik.data.settings.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth

/** Four shift types used in Maxim's monthly rota. */
enum class ShiftType(
    val code: String,
    val labelUa: String,
    val time: String,
    val hours: Int,
    val colorHex: Long
) {
    MORNING_8("8-R", "Ранкова 8г", "06:00–14:00", 8, 0xFF34D399),
    AFTERNOON_8("8-O", "Після обіду 8г", "14:00–22:00", 8, 0xFFFBBF24),
    DAY_12("12-R", "Денна 12г", "06:00–18:00", 12, 0xFF60A5FA),
    NIGHT_12("12-N", "Нічна 12г", "18:00–06:00", 12, 0xFFA78BFA);

    companion object {
        fun fromCode(raw: String?): ShiftType? {
            val value = raw?.uppercase()?.replace(" ", "") ?: return null
            return entries.firstOrNull { it.code == value }
        }
    }
}

data class ShiftEntry(
    val date: LocalDate,
    val type: ShiftType
)

data class ShiftScheduleState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val shifts: Map<String, ShiftType> = emptyMap(),
    val selectedPhotoUri: String? = null,
    val isImporting: Boolean = false,
    val statusMessage: String? = null,
    val error: String? = null
)

class ShiftScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("shift_schedule", Context.MODE_PRIVATE)
    private val settingsRepo = SettingsRepository(application)
    private val gson = Gson()

    private val _state = MutableStateFlow(ShiftScheduleState())
    val state: StateFlow<ShiftScheduleState> = _state

    val userProfile: StateFlow<UserProfile?> = settingsRepo.userProfile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    init {
        loadMonth(_state.value.selectedMonth)
    }

    fun previousMonth() = setMonth(_state.value.selectedMonth.minusMonths(1))
    fun nextMonth() = setMonth(_state.value.selectedMonth.plusMonths(1))
    fun currentMonth() = setMonth(YearMonth.now())

    private fun setMonth(month: YearMonth) {
        _state.update { it.copy(selectedMonth = month, statusMessage = null, error = null) }
        loadMonth(month)
    }

    fun setPhotoUri(uri: String?) = _state.update { it.copy(selectedPhotoUri = uri, statusMessage = null, error = null) }
    fun resetMessages() = _state.update { it.copy(statusMessage = null, error = null) }

    fun setShift(date: LocalDate, type: ShiftType?) {
        val key = date.toString()
        val updated = _state.value.shifts.toMutableMap()
        if (type == null) updated.remove(key) else updated[key] = type
        saveShifts(updated)
        _state.update { it.copy(shifts = updated, statusMessage = "Зміну збережено") }
    }

    fun cycleShift(date: LocalDate) {
        val current = _state.value.shifts[date.toString()]
        val next = when (current) {
            null -> ShiftType.MORNING_8
            ShiftType.MORNING_8 -> ShiftType.AFTERNOON_8
            ShiftType.AFTERNOON_8 -> ShiftType.DAY_12
            ShiftType.DAY_12 -> ShiftType.NIGHT_12
            ShiftType.NIGHT_12 -> null
        }
        setShift(date, next)
    }

    fun clearMonth() {
        val monthPrefix = _state.value.selectedMonth.toString()
        val updated = _state.value.shifts.filterKeys { !it.startsWith(monthPrefix) }
        saveShifts(updated)
        _state.update { it.copy(shifts = updated, statusMessage = "Місяць очищено") }
    }

    fun importFromPhoto(apiKey: String) {
        val photoUri = _state.value.selectedPhotoUri
        if (apiKey.isBlank()) {
            _state.update { it.copy(error = "Введи OpenRouter API ключ у налаштуваннях") }
            return
        }
        if (photoUri.isNullOrBlank()) {
            _state.update { it.copy(error = "Спочатку вибери або сфотографуй графік") }
            return
        }

        _state.update { it.copy(isImporting = true, error = null, statusMessage = null) }
        viewModelScope.launch {
            try {
                val imageBase64 = withContext(Dispatchers.IO) { uriToBase64(photoUri) }
                    ?: throw IllegalStateException("Не вдалося прочитати фото")
                val imported = recognizeSchedule(imageBase64, apiKey)
                if (imported.isEmpty()) {
                    _state.update {
                        it.copy(
                            isImporting = false,
                            error = "AI не знайшов змін. Спробуй обрізати фото ближче до твого рядка Kovalevskyi."
                        )
                    }
                    return@launch
                }
                val updated = _state.value.shifts.toMutableMap()
                imported.forEach { entry -> updated[entry.date.toString()] = entry.type }
                saveShifts(updated)
                _state.update {
                    it.copy(
                        shifts = updated,
                        isImporting = false,
                        statusMessage = "Імпортовано ${imported.size} змін з фото"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("Pomocnik", "Schedule import failed: ${e.message}", e)
                _state.update { it.copy(isImporting = false, error = "Помилка імпорту: ${e.localizedMessage ?: e.message}") }
            }
        }
    }

    private suspend fun recognizeSchedule(imageBase64: String, apiKey: String): List<ShiftEntry> = withContext(Dispatchers.IO) {
        val month = _state.value.selectedMonth
        val model = userProfile.value?.selectedModel ?: ModelConfig.DEFAULT_MODEL
        val api = OpenRouterApi.create(apiKey)
        val systemPrompt = "You are an OCR assistant for Czech factory monthly shift rota screenshots. Return strict JSON only."
        val prompt = """
Analyze the photo of an Excel/SharePoint shift schedule.
Find the row for worker name "Kovalevskyi" or "Kovalevsky".
Extract shifts for month ${month.monthValue}/${month.year} only.

Valid shift codes and meanings:
- 8-R = 06:00-14:00 morning 8h
- 8-O = 14:00-22:00 afternoon 8h
- 12-R = 06:00-18:00 day 12h
- 12-N = 18:00-06:00 night 12h

Return ONLY valid JSON object:
{"month":"${month}","shifts":[{"date":"YYYY-MM-DD","code":"8-R"}]}

Rules:
- If a day is empty, vacation, N, NVN, B, P, D, or unclear, omit it.
- Use only the four valid codes above.
- Do not invent dates. If uncertain, omit that day.
- The screenshot may show Czech month sheet names. Respect the selected month ${month}.
""".trimIndent()

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message("system", systemPrompt),
                Message(
                    "user",
                    listOf(
                        ContentPart("text", text = prompt),
                        ContentPart("image_url", image_url = ImageUrl("data:image/jpeg;base64,$imageBase64"))
                    )
                )
            ),
            temperature = 0.1
        )
        val raw = api.translate(request).choices.firstOrNull()?.message?.content?.trim().orEmpty()
        parseShiftJson(raw)
    }

    private fun parseShiftJson(raw: String): List<ShiftEntry> {
        val cleaned = raw
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```\\s*$"), "")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) return emptyList()
        val json = gson.fromJson(cleaned.substring(start, end + 1), JsonObject::class.java)
        val arr: JsonArray = json.getAsJsonArray("shifts") ?: return emptyList()
        return arr.mapNotNull { element ->
            val obj = element.asJsonObject
            val date = runCatching { LocalDate.parse(obj.get("date")?.asString ?: return@mapNotNull null) }.getOrNull()
            val type = ShiftType.fromCode(obj.get("code")?.asString)
            if (date != null && type != null) ShiftEntry(date, type) else null
        }
    }

    private fun loadMonth(month: YearMonth) {
        val all = loadAllShifts()
        _state.update { it.copy(shifts = all, selectedMonth = month) }
    }

    private fun loadAllShifts(): Map<String, ShiftType> {
        val raw = prefs.getString("shifts_json", "{}") ?: "{}"
        return runCatching {
            val obj = gson.fromJson(raw, JsonObject::class.java)
            obj.entrySet().mapNotNull { (date, value) ->
                ShiftType.fromCode(value.asString)?.let { date to it }
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun saveShifts(shifts: Map<String, ShiftType>) {
        val obj = JsonObject()
        shifts.toSortedMap().forEach { (date, type) -> obj.addProperty(date, type.code) }
        prefs.edit().putString("shifts_json", gson.toJson(obj)).apply()
    }

    private fun uriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()
            val maxSize = 1400
            val outputStream = ByteArrayOutputStream()
            if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
                val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                bitmap.recycle()
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
                scaled.recycle()
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
                bitmap.recycle()
            }
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("Pomocnik", "schedule uriToBase64 failed: ${e.message}", e)
            null
        }
    }
}
