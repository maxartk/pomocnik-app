package cz.kovmak.pomocnik.data.repository

import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.data.database.WorkEntryDao
import cz.kovmak.pomocnik.data.network.OpenRouterApi
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

    suspend fun translateToCzech(text: String, apiKey: String): String = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val prompt = "Přelož následující text z ukrajinštiny do přirozené, profesionální češtiny. " +
            "Použij technické a elektrotechnické termíny, pokud jsou relevantní. " +
            "Vrať POUZE přeložený text bez vysvětlování:\n\n$text"

        val request = cz.kovmak.pomocnik.data.network.TranslationRequest(
            messages = listOf(
                cz.kovmak.pomocnik.data.network.Message(
                    role = "system",
                    content = "Jsi profesionální překladatel z ukrajinštiny do češtiny, " +
                        "specializující se na technické a elektrotechnické termíny."
                ),
                cz.kovmak.pomocnik.data.network.Message(
                    role = "user",
                    content = prompt
                )
            )
        )

        val response = dynamicApi.translate("Bearer $apiKey", request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: text
    }

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

        val prompt = """
Vytvoř technickou zprávu:

Popis (CZ): $descriptionCz
Popis (UA): $descriptionUa
Zakázka: $orderId
Typ: $workTypeLabel
Čas: $startTime - $endTime ($hours h)
Materiály: $materials

Formát:
**SOUHRN:** [1-2 věty]
**TECHNICKÉ DETAILY:** • [krok]
**BEZPEČNOST:** • [poznámka]
**DOPORUČENÍ:** • [tip]
""".trimIndent()

        val request = cz.kovmak.pomocnik.data.network.TranslationRequest(
            messages = listOf(
                cz.kovmak.pomocnik.data.network.Message(
                    role = "system",
                    content = "Jsi expertní elektrikář/mechanik. Vytváříš stručné, " +
                        "profesionální technické zprávy."
                ),
                cz.kovmak.pomocnik.data.network.Message(
                    role = "user",
                    content = prompt
                )
            )
        )

        val response = dynamicApi.translate("Bearer $apiKey", request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }
}
