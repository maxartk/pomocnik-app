package cz.kovmak.pomocnik.data.model

import com.google.gson.Gson
import com.google.gson.JsonObject

object SapFieldParser {
    private val gson = Gson()
    private val objectPartCodes = SapCatalogs.objectParts.map { it.code }.toSet()
    private val damageCodes = SapCatalogs.damageDescriptions.map { it.code }.toSet()
    private val causeCodes = SapCatalogs.causes.map { it.code }.toSet()
    private val impactCodes = SapCatalogs.impacts.map { it.code }.toSet()

    fun parse(rawResponse: String): SapFieldResult {
        val normalized = sanitizeRawResponse(rawResponse)
        val jsonCandidate = extractJsonBlock(normalized)

        parseJsonObject(jsonCandidate)?.let { json ->
            return parseJsonResult(json)
        }

        return parseKeyValueFallback(normalized)
    }

    private fun sanitizeRawResponse(raw: String): String {
        return raw
            .replace("\u201c", "\"")
            .replace("\u201d", "\"")
            .replace("\u2018", "'")
            .replace("\u2019", "'")
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```\\s*$"), "")
            .trim()
    }

    private fun extractJsonBlock(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private fun parseJsonObject(text: String): JsonObject? {
        return try {
            gson.fromJson(text, JsonObject::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJsonResult(json: JsonObject): SapFieldResult {
        val root = when {
            json.has("sapFields") && json.get("sapFields").isJsonObject -> json.getAsJsonObject("sapFields")
            json.has("sap") && json.get("sap").isJsonObject -> json.getAsJsonObject("sap")
            else -> json
        }

        return SapFieldResult(
            objectPart = normalizeCatalogCode(readString(root,
                "objectPart", "object_part", "objectPartCode", "object_part_code", "part", "castObj"), objectPartCodes),
            damageDesc = normalizeCatalogCode(readString(root,
                "damageDesc", "damage_desc", "damageDescription", "damage_description", "damageCode", "damage_code"), damageCodes),
            damageText = normalizeFreeText(readString(root,
                "damageText", "damage_text", "damageDescriptionText", "textPoskozeni", "text_poškození")),
            cause = normalizeCatalogCode(readString(root,
                "cause", "causeCode", "cause_code", "causeDescriptionCode", "pricina", "příčina"), causeCodes),
            causeText = normalizeFreeText(readString(root,
                "causeText", "cause_text", "textPriciny", "textPříčiny", "pricinaText", "příčinaText")),
            impact = normalizeImpact(readString(root,
                "impact", "impactCode", "impact_code", "dopad", "vliv"))
        )
    }

    private fun readString(json: JsonObject, vararg keys: String): String {
        keys.forEach { key ->
            val element = json.get(key)
            if (element != null && !element.isJsonNull && element.isJsonPrimitive) {
                return try {
                    element.asString.trim()
                } catch (_: Exception) {
                    ""
                }
            }
        }
        return ""
    }

    private fun normalizeCatalogCode(value: String, allowedCodes: Set<String>): String {
        if (value.isBlank()) return ""
        val trimmed = value.trim()
        if (trimmed in allowedCodes) return trimmed

        allowedCodes.firstOrNull { code -> Regex("(?<!\\d)${Regex.escape(code)}(?!\\d)").containsMatchIn(trimmed) }?.let {
            return it
        }

        val digits = Regex("\\b\\d{4}\\b").find(trimmed)?.value.orEmpty()
        return if (digits in allowedCodes) digits else ""
    }

    private fun normalizeImpact(value: String): String {
        if (value.isBlank()) return ""
        val trimmed = value.trim()
        if (trimmed in impactCodes) return trimmed

        Regex("(?<!\\d)([123])(?!\\d)").find(trimmed)?.groupValues?.getOrNull(1)?.let {
            if (it in impactCodes) return it
        }

        val lower = trimmed.lowercase()
        return when {
            "výpad" in lower || "vypad" in lower -> "3"
            "omezen" in lower -> "2"
            "bez vlivu" in lower || "no impact" in lower -> "1"
            else -> ""
        }
    }

    private fun normalizeFreeText(value: String): String {
        return value.trim().trim('"').trim()
    }

    private fun parseKeyValueFallback(text: String): SapFieldResult {
        fun pick(label: String): String {
            return Regex("(?im)^\\s*${Regex.escape(label)}\\s*[:=-]\\s*(.+)$")
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                .orEmpty()
        }

        return SapFieldResult(
            objectPart = normalizeCatalogCode(firstNonBlank(
                pick("objectPart"), pick("object_part"), pick("Část obj."), pick("Cast obj."), pick("part")
            ), objectPartCodes),
            damageDesc = normalizeCatalogCode(firstNonBlank(
                pick("damageDesc"), pick("damage_desc"), pick("Popis škody")
            ), damageCodes),
            damageText = normalizeFreeText(firstNonBlank(
                pick("damageText"), pick("damage_text"), pick("Text poškození")
            )),
            cause = normalizeCatalogCode(firstNonBlank(
                pick("cause"), pick("cause_code"), pick("Příčina"), pick("Pricina")
            ), causeCodes),
            causeText = normalizeFreeText(firstNonBlank(
                pick("causeText"), pick("cause_text"), pick("Text příčiny"), pick("Text priciny")
            )),
            impact = normalizeImpact(firstNonBlank(
                pick("impact"), pick("dopad")
            ))
        )
    }

    private fun firstNonBlank(vararg values: String): String = values.firstOrNull { it.isNotBlank() }.orEmpty()
}
