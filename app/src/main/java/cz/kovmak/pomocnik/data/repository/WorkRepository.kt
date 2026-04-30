package cz.kovmak.pomocnik.data.repository

import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.data.database.WorkEntryDao
import cz.kovmak.pomocnik.data.network.OpenRouterApi
import cz.kovmak.pomocnik.data.network.TranslationRequest
import cz.kovmak.pomocnik.data.network.Message
import cz.kovmak.pomocnik.data.network.ContentPart
import cz.kovmak.pomocnik.data.network.ImageUrl
import cz.kovmak.pomocnik.data.model.SapFieldResult
import cz.kovmak.pomocnik.data.model.SapCatalogs
import cz.kovmak.pomocnik.data.network.ModelConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkRepository(
    private val dao: WorkEntryDao,
    private val api: OpenRouterApi
) {

    fun getAllEntries(): Flow<List<WorkEntry>> = dao.getAllEntries()
    fun searchEntries(query: String): Flow<List<WorkEntry>> = dao.searchEntries(query)
    fun getRecentEntries(limit: Int = 20): Flow<List<WorkEntry>> = dao.getRecentEntries(limit)
    suspend fun getEntryById(id: Long): WorkEntry? = dao.getEntryById(id)
    suspend fun insertEntry(entry: WorkEntry): Long = dao.insertEntry(entry)
    suspend fun updateEntry(entry: WorkEntry) = dao.updateEntry(entry)
    suspend fun deleteEntry(entry: WorkEntry) = dao.deleteEntry(entry)
    suspend fun deleteAllEntries() = dao.deleteAllEntries()

    /**
     * Переклад UA→CS у формальному номінальному стилі для SAP.
     */
    suspend fun translateToCzech(text: String, apiKey: String, model: String = ModelConfig.DEFAULT_MODEL): String = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val systemPrompt = "Jsi odborný překladatel technických textů UA→CS pro průmyslové systémy SAP. " +
            "Překládáš stručně, přesně, ve formálním nominálním stylu."

        val userPrompt = """Přelož tento technický popis z ukrajinštiny do češtiny.

Originál (UA): $text

Pravidla překladu:
- Formální nominální styl vhodný pro SAP systém
- Technická terminologie pro elektrikáře/mechaniky
- Krátce a věcně
- Vrať POUZE přeložený text, nic jiného"""

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.3
        )

        val response = dynamicApi.translate(request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: text
    }

    /**
     * Генерація технічної зправи для SAP IW41.
     */
    suspend fun generateTechnicalReport(
        descriptionCz: String,
        descriptionUa: String,
        orderId: String,
        workType: String,
        startTime: String,
        endTime: String,
        hours: Double,
        materials: String,
        apiKey: String,
        model: String = ModelConfig.DEFAULT_MODEL
    ): String = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val workTypeLabel = when (workType) {
            "E" -> "Elektrická"
            "M" -> "Mechanická"
            else -> "Elektrická + Mechanická"
        }

        val systemPrompt = "Jsi zkušený průmyslový elektrikář/mechanik. " +
            "Píšeš stručné technické zprávy pro SAP systém. Formální styl, odborná terminologie."

        val userPrompt = """Vytvoř stručnou technickou zprávu pro SAP IW41.

Popis CZ: $descriptionCz
Popis UA: $descriptionUa
Zakázka: $orderId
Typ práce: $workTypeLabel
Čas: $startTime - $endTime ($hours h)
Materiály: $materials

Formát:
**SOUHRN:** [1-2 věty]
**TECHNICKÉ DETAILY:**
• [krok/popis]
**ZÁVĚR:** [výsledek práce]"""

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.3
        )

        val response = dynamicApi.translate(request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }

    /**
     * Режим "Порадник" — запит до досвідченого електрика Knorr-Bremse.
     * Підтримує відправку фото (base64) для аналізу через Gemini Vision.
     */
    suspend fun askAdvisor(question: String, apiKey: String, model: String = ModelConfig.DEFAULT_MODEL, imageBase64: String? = null): String = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val systemPrompt = "Ти практичний електрик на виробництві Knorr-Bremse. " +
            "Даєш конкретні відповіді на основі досвіду. " +
            "Знаєш типові проблеми гальмівних систем для потягів. " +
            "Відповідаєш завжди українською."

        val userContent: Any = if (imageBase64 != null) {
            // Multimodal: text + image
            listOf(
                ContentPart(
                    type = "text",
                    text = """Відповідай як практичний електрик на виробництві з великим досвідом.

Питання: $question

Дивись на фотографію проблеми і дай конкретну пораду.

Вимоги до відповіді:
- Конкретно і по суті
- Практичні поради з досвіду
- Українською мовою
- Безпека на першому місці

Формат:
**АНАЛІЗ:** [що трапилось, що видно на фото]
**ПРИЧИНА:** [найімовірніша причина]
**ДІЯ:** [конкретні кроки для виправлення]
**БЕЗПЕКА:** [важливі застереження]"""
                ),
                ContentPart(
                    type = "image_url",
                    image_url = ImageUrl(url = "data:image/jpeg;base64,$imageBase64")
                )
            )
        } else {
            """Відповідай як практичний електрик на виробництві з великим досвідом.

Питання: $question

Вимоги до відповіді:
- Конкретно і по суті
- Практичні поради з досвіду
- Українською мовою
- Безпека на першому місці

Формат:
**АНАЛІЗ:** [що трапилось]
**ПРИЧИНА:** [найімовірніша причина]
**ДІЯ:** [конкретні кроки]
**БЕЗПЕКА:** [важливі застереження]"""
        }

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userContent)
            ),
            temperature = 0.4
        )

        val response = dynamicApi.translate(request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }

    /**
     * Витягує SAP поля з опису роботи через AI.
     * Аналізує UA та CZ опис і визначає коди з каталогів.
     */
    suspend fun extractSapFields(
        descriptionCz: String,
        descriptionUa: String,
        apiKey: String,
        model: String = ModelConfig.DEFAULT_MODEL
    ): SapFieldResult = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val catalogsText = SapCatalogs.formatForPrompt()

        val systemPrompt = "Jsi expert na SAP PM modul pro průmyslovou údržbu. " +
            "Analyzuješ popis práce a přiřazuješ správné kódy z katalogů."

        val userPrompt = """Analyzuj popis práce a vyplň SAP pole. Vrať POUZE validní JSON bez dalšího textu.

Popis práce (UA): $descriptionUa
Popis práce (CZ): $descriptionCz

Dostupné katalogy:
$catalogsText

Pravidla:
- Část obj.: vyber nejvhodnější kód z MGLC001-005 podle popisu (jen číslo, např. "2208" nebo "2302")
- Popis škody: vyber kód z MCZ001 který nejlépe odpovídá (jen číslo, např. "1023")
- Text poškození: stručný popis v češtině (1 věta, formální styl)
- Příčina: vyber nejvhodnější kód z MGLO001-007 podle popisu (jen číslo, např. "1305" nebo "1226")
- Text příčiny: stručná příčina v češtině
- Dopad: 1=Bez vlivu, 2=Omezení výroby, 3=Výpadek výroby

Vrať POUZE tento JSON, nic jiného:
{"objectPart":"2208","damageDesc":"1023","damageText":"Nelze posunout do home pozice","cause":"1305","causeText":"Zkrat nebo elektrická porucha","impact":"3"}"""

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.2
        )

        val response = dynamicApi.translate(request)
        var rawContent = response.choices.firstOrNull()?.message?.content?.trim() ?: "{}"
        
        // Strip markdown code fences (```json ... ```) and extract JSON block
        rawContent = rawContent
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```\\s*$"), "")
            .trim()
        
        // Fallback: extract JSON between first { and last }
        val jsonStart = rawContent.indexOf('{')
        val jsonEnd = rawContent.lastIndexOf('}')
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            rawContent = rawContent.substring(jsonStart, jsonEnd + 1)
        }

        // Parse JSON — use Gson (already in project for multimodal)
        val gson = com.google.gson.Gson()
        try {
            val json = gson.fromJson(rawContent, com.google.gson.JsonObject::class.java)
            val result = SapFieldResult(
                objectPart = json.get("objectPart")?.asString ?: "",
                damageDesc = json.get("damageDesc")?.asString ?: "",
                damageText = json.get("damageText")?.asString ?: "",
                cause = json.get("cause")?.asString ?: "",
                causeText = json.get("causeText")?.asString ?: "",
                impact = json.get("impact")?.asString ?: ""
            )
            android.util.Log.d("Pomocnik", "SAP fields extracted: $result")
            return@withContext result
        } catch (e: Exception) {
            android.util.Log.e("Pomocnik", "Failed to parse SAP fields JSON: '$rawContent'", e)
            return@withContext SapFieldResult()
        }
    }

    /**
     * OCR розпізнавання коду матеріалу з фото.
     */
    suspend fun ocrMaterialCode(imageBase64: String, apiKey: String, model: String = ModelConfig.DEFAULT_MODEL): String = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val systemPrompt = "You are an OCR assistant that extracts material codes from images."

        val userPrompt = """Rozpoznej kód materiálu na obrázku.

Kód má formát: písmeno + 6 číslic. Například: E000175 nebo M001234

Vrať POUZE JSON ve formátu:
{"prefix": "E", "number": "000175", "full_code": "E000175"}

NEBO pokud kód není nalezen:
{"prefix": null, "number": null, "full_code": null}

NIC DALŠÍHO nepřidávej - pouze JSON."""

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.1
        )

        val response = dynamicApi.translate(request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: """{"prefix": null, "number": null, "full_code": null}"""
    }
}