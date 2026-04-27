package cz.kovmak.pomocnik.data.repository

import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.data.database.WorkEntryDao
import cz.kovmak.pomocnik.data.network.OpenRouterApi
import cz.kovmak.pomocnik.data.network.TranslationRequest
import cz.kovmak.pomocnik.data.network.Message
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
     * System prompt + user prompt з n8n workflow "AI - Překlad UA→CS1".
     */
    suspend fun translateToCzech(text: String, apiKey: String): String = withContext(Dispatchers.IO) {
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
            model = "google/gemini-2.0-flash-001",
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.3
        )

        val response = dynamicApi.translate("Bearer $apiKey", request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: text
    }

    /**
     * Генерація технічної зправи для SAP IW41.
     * System prompt + user prompt з n8n workflow "AI - Technická zpráva1".
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
        apiKey: String
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
            model = "google/gemini-2.0-flash-001",
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.3
        )

        val response = dynamicApi.translate("Bearer $apiKey", request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }

    /**
     * Режим "Порадник" — запит до досвідченого електрика Knorr-Bremse.
     * System prompt + user prompt з n8n workflow "AI Production Advisor1".
     */
    suspend fun askAdvisor(question: String, apiKey: String): String = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val systemPrompt = "Ти практичний електрик на виробництві Knorr-Bremse. " +
            "Даєш конкретні відповіді на основі досвіду. " +
            "Знаєш типові проблеми гальмівних систем для потягів. " +
            "Відповідаєш завжди українською."

        val userPrompt = """Відповідай як практичний електрик на виробництві з великим досвідом.

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

        val request = TranslationRequest(
            model = "google/gemini-2.0-flash-001",
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.4
        )

        val response = dynamicApi.translate("Bearer $apiKey", request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }

    /**
     * OCR розпізнавання коду матеріалу з фото.
     * System prompt + user prompt з n8n workflow "OCR Agent1".
     */
    suspend fun ocrMaterialCode(imageBase64: String, apiKey: String): String = withContext(Dispatchers.IO) {
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
            model = "google/gemini-2.0-flash-001",
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.1
        )

        val response = dynamicApi.translate("Bearer $apiKey", request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: """{"prefix": null, "number": null, "full_code": null}"""
    }
}
