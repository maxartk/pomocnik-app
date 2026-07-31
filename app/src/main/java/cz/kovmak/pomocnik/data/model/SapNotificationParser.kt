package cz.kovmak.pomocnik.data.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

data class SapNotificationData(
    val orderId: String = "",
    val technicalLocation: String = "",
    val notificationText: String = "",
    val author: String = "",
    val notificationDate: String = "",
    val notificationTime: String = "",
    val priority: String = ""
)

object SapNotificationParser {
    private val gson = Gson()
    private val outputDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT)
    private val acceptedDateFormatters = listOf(
        DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("d.M.uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("d. M. uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
    )

    fun parse(rawResponse: String): SapNotificationData {
        val text = rawResponse
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```\\s*$"), "")
            .trim()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        val candidate = if (start >= 0 && end > start) text.substring(start, end + 1) else text
        val json = runCatching { gson.fromJson(candidate, JsonObject::class.java) }.getOrNull()
            ?: return SapNotificationData()
        val root = when {
            json.get("notification")?.isJsonObject == true -> json.getAsJsonObject("notification")
            json.get("hlaseni")?.isJsonObject == true -> json.getAsJsonObject("hlaseni")
            else -> json
        }

        return SapNotificationData(
            orderId = normalizeOrderId(read(root, "orderId", "order_id", "zakazka", "zakázka")),
            technicalLocation = read(root, "technicalLocation", "technical_location", "technickeMisto", "technickéMísto"),
            notificationText = read(root, "notificationText", "notification_text", "stavObjektu", "stav_objektu", "problemText"),
            author = read(root, "author", "notificationAuthor", "autorHlaseni", "autor_hlášení"),
            notificationDate = normalizeDate(read(root, "notificationDate", "notification_date", "datumHlaseni", "datum_hlášení")),
            notificationTime = normalizeTime(read(root, "notificationTime", "notification_time", "casHlaseni", "čas_hlášení")),
            priority = normalizePriority(read(root, "priority", "priorita"))
        )
    }

    private fun read(json: JsonObject, vararg keys: String): String {
        for (key in keys) {
            val value = json.get(key)
            if (value != null && !value.isJsonNull && value.isJsonPrimitive) {
                return runCatching { value.asString.trim() }.getOrDefault("")
            }
        }
        return ""
    }

    private fun normalizeOrderId(value: String): String =
        Regex("(?<!\\d)\\d{6,12}(?!\\d)").find(value)?.value.orEmpty()

    private fun normalizeDate(value: String): String {
        val cleaned = value.trim().replace(Regex("\\s+"), " ")
        for (formatter in acceptedDateFormatters) {
            try {
                return LocalDate.parse(cleaned, formatter).format(outputDateFormatter)
            } catch (_: DateTimeParseException) {
                // Try the next accepted SAP date format.
            }
        }
        return ""
    }

    private fun normalizeTime(value: String): String {
        val match = Regex("(?<!\\d)(\\d{1,2}):(\\d{2})(?::\\d{2})?(?!\\d)").find(value) ?: return ""
        val hour = match.groupValues[1].toIntOrNull() ?: return ""
        val minute = match.groupValues[2].toIntOrNull() ?: return ""
        if (hour !in 0..23 || minute !in 0..59) return ""
        return LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))
    }

    private fun normalizePriority(value: String): String {
        val normalized = value.trim()
        return when (normalized.lowercase(Locale.ROOT)) {
            "vysoká", "vysoka", "high" -> "Vysoká"
            "střední", "stredni", "medium" -> "Střední"
            "nízká", "nizka", "low" -> "Nízká"
            else -> normalized
        }
    }
}
