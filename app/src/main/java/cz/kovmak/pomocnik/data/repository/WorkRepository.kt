package cz.kovmak.pomocnik.data.repository

import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.data.database.WorkEntryDao
import cz.kovmak.pomocnik.data.network.OpenRouterApi
import cz.kovmak.pomocnik.data.network.TranslationRequest
import cz.kovmak.pomocnik.data.network.Message
import cz.kovmak.pomocnik.data.network.ContentPart
import cz.kovmak.pomocnik.data.network.ImageUrl
import cz.kovmak.pomocnik.data.model.SapCatalogs
import cz.kovmak.pomocnik.data.model.SapFieldParser
import cz.kovmak.pomocnik.data.model.SapFieldResult
import cz.kovmak.pomocnik.data.model.SapNotificationData
import cz.kovmak.pomocnik.data.model.SapNotificationParser
import cz.kovmak.pomocnik.data.network.ModelConfig
import cz.kovmak.pomocnik.BuildConfig
import com.google.gson.JsonParser

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



data class SapPhotoReportDraft(
    val descriptionCz: String,
    val technicalReport: String
)

class WorkRepository(
    private val dao: WorkEntryDao,
    private val api: OpenRouterApi
) {

    private fun cleanWorkerReport(text: String): String {
        return text
            .replace(Regex("\\*\\*(SOUHRN|TECHNICKÉ DETAILY|ZÁVĚR):\\*\\*", RegexOption.IGNORE_CASE), "")
            .replace("**", "")
            .lines()
            .map { line -> line.trim().removePrefix("•").trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun getAllEntries(): Flow<List<WorkEntry>> = dao.getAllEntries()
    fun searchEntries(query: String): Flow<List<WorkEntry>> = dao.searchEntries(query)
    fun getRecentEntries(limit: Int = 20): Flow<List<WorkEntry>> = dao.getRecentEntries(limit)
    suspend fun getEntryById(id: Long): WorkEntry? = dao.getEntryById(id)
    suspend fun insertEntry(entry: WorkEntry): Long = dao.insertEntry(entry)
    suspend fun updateEntry(entry: WorkEntry) = dao.updateEntry(entry)
    suspend fun deleteEntry(entry: WorkEntry) = dao.deleteEntry(entry)
    suspend fun deleteAllEntries() = dao.deleteAllEntries()

    /**
     * Переклад UA→CS у природному робочому стилі.
     */
    suspend fun translateToCzech(text: String, apiKey: String, model: String = ModelConfig.DEFAULT_MODEL): String = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val systemPrompt = "Jsi zkušený překladatel a jazykový asistent pro průmyslovou údržbu. " +
            "Pracuješ s texty psanými směsí ukrajinštiny a češtiny — uživatel píše převážně ukrajinsky, " +
            "ale občas vkládá česká slova nebo technické termíny (i špatně napsané). " +
            "Tvým úkolem je porozumět celému textu bez ohledu na jazyk nebo pravopisné chyby, " +
            "a přeložit vše do přirozené, stručné češtiny jak by psal pracovník kolegovi v práci."

        val userPrompt = """Přelož tento text do přirozené češtiny.

Text může obsahovat:
- ukrajinská slova
- česká slova (i špatně napsaná nebo foneticky zapsaná)
- směs obou jazyků v jedné větě

Originál: $text

Pravidla:
- Porozumět textu jako celku, bez ohledu na jazyk nebo pravopis
- Přeložit vše do přirozené, stručné češtiny
- Zachovat technický význam přesně
- Nepoužívat úřední nebo robotický styl
- Nepřidávat fráze které nejsou v originálu ("Dobrý den", "Úkol splněn" apod.)
- Vrátit POUZE přeložený text, nic jiného"""

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.3,
            max_tokens = 512
        )

        val response = dynamicApi.translate(request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: text
    }

    /**
     * Генерація технічного звіту для SAP IW41.
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

        val systemPrompt = "Jsi zkušený elektrikář z údržby. " +
            "Píšeš krátce a obyčejně, jako pracovník na dílně, ne jako kancelář nebo inženýr."

        val userPrompt = """Vytvoř krátký zápis práce pro SAP IW41.

Popis CZ: $descriptionCz
Popis UA: $descriptionUa
Zakázka: $orderId
Typ práce: $workTypeLabel
Čas: $startTime - $endTime ($hours h)
Materiály: $materials

Styl:
- 1 až 3 krátké české věty
- jednoduchá řeč elektrikáře z údržby
- bez nadpisů, bez odrážek, bez markdownu, bez **
- nepiš slova jako "provedena", "provozuschopnost", "zařízení obnovena", "technické detaily"
- klidně piš normálně: "Vyměnili jsme...", "Zavařili jsme...", "Po výměně zkouška OK."
- pokud je potřeba objednat díl, napiš to jednoduše

Příklad stylu:
Zavařili jsme spojku. Druhá spojka už je špatná, je potřeba objednat dvě nové."""

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.3,
            max_tokens = 768
        )

        val response = dynamicApi.translate(request)
        cleanWorkerReport(response.choices.firstOrNull()?.message?.content?.trim() ?: "")
    }

    /**
     * Режим "Порадник" — запит до досвідченого електрика Knorr-Bremse.
     * Підтримує відправку фото (base64) для аналізу через Gemini Vision.
     */
    suspend fun askAdvisor(question: String, apiKey: String, model: String = ModelConfig.DEFAULT_MODEL, imageBase64: String? = null): String = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)
        val chosenModel = if (imageBase64 != null) ModelConfig.VISION_MODEL else model

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
            model = chosenModel,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userContent)
            ),
            temperature = 0.4,
            max_tokens = 1024
        )

        val response = dynamicApi.translate(request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: ""
    }


    /** Read the source notification context from a SAP screenshot without inventing unreadable values. */
    suspend fun extractSapNotification(
        orderImageBase64: String,
        apiKey: String
    ): SapNotificationData = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)
        val prompt = """Přečti fotografii SAP PM hlášení. Vrať POUZE validní JSON bez markdownu:
{
  "orderId": "číslo pole Zakázka nebo prázdný řetězec",
  "technicalLocation": "Technické místo nebo prázdný řetězec",
  "notificationText": "doslovný popis samotné závady z pole Stav objektu, bez hlavičky s telefonem a timestampem",
  "author": "Autor hlášení nebo prázdný řetězec",
  "notificationDate": "datum pole Datum hlášení ve formátu DD.MM.YYYY",
  "notificationTime": "čas pole Datum hlášení ve formátu HH:mm",
  "priority": "priorita přesně z obrazovky"
}

Pravidla:
- Čti hodnoty pouze z viditelných polí SAP.
- Datum a čas ber z pole Datum hlášení, ne z časové značky uvnitř textu Stav objektu.
- Nic nedoplňuj a nic neodhaduj. Nečitelná hodnota musí být prázdný řetězec.
- Zachovej původní český text závady, pouze odstraň telefonní číslo a technickou časovou značku před textem.""".trimIndent()
        val request = TranslationRequest(
            model = ModelConfig.VISION_MODEL,
            messages = listOf(
                Message("system", "Jsi přesný OCR parser obrazovek SAP PM. Nikdy si nevymýšlíš nečitelné hodnoty."),
                Message("user", listOf(
                    ContentPart("text", text = prompt),
                    ContentPart("image_url", image_url = ImageUrl("data:image/jpeg;base64,$orderImageBase64"))
                ))
            ),
            temperature = 0.0,
            max_tokens = 700
        )
        val raw = dynamicApi.translate(request).choices.firstOrNull()?.message?.content.orEmpty()
        SapNotificationParser.parse(raw)
    }

    /**
     * Compose a Czech SAP work report from reviewed notification data, an optional real-part photo,
     * and the worker's Ukrainian/Czech repair note. The worker note and detail photo are authoritative.
     */
    suspend fun generateReportFromSapPhotos(
        notification: SapNotificationData,
        repairNote: String,
        detailImageBase64: String?,
        apiKey: String,
        model: String = ModelConfig.VISION_MODEL
    ): SapPhotoReportDraft = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)
        val chosenModel = if (detailImageBase64 != null) ModelConfig.VISION_MODEL else model

        val systemPrompt = """Jsi zkušený technik průmyslové údržby v SAP PM.
Umíš číst fotky hlášení/zakázky ze SAP a z krátké poznámky pracovníka napsat jednoduchý český zápis práce.
Piš jako normální elektrikář z údržby, ne jako kancelář nebo inženýr. Zachovej technický význam přesně.""".trimIndent()

        val textPrompt = """Vytvoř výstup pro pracovní hlášení podle těchto vstupů:
1) PŮVODNÍ HLÁŠENÍ AUTORA:
- Zakázka: ${notification.orderId}
- Technické místo: ${notification.technicalLocation}
- Datum a čas hlášení: ${notification.notificationDate} ${notification.notificationTime}
- Autor: ${notification.author}
- Priorita: ${notification.priority}
- Popis závady od autora: ${notification.notificationText}

2) FOTO SKUTEČNÉHO DÍLU: je nepovinné. Pokud je přiložené, použij ho k ověření správné části.
3) POZNÁMKA PRACOVNÍKA PO OPRAVĚ: je rozhodující pro to, co se opravdu udělalo.

Poznámka pracovníka:
$repairNote

Důležitá pravidla:
- Pokud SAP nebo mistr určil díl špatně, oprav to podle poznámky pracovníka a/nebo fotky dílu.
- Například když SAP píše "trn", ale pracovník píše nebo fotka ukazuje "spojka", napiš spojku, ne trn.
- Nehádej neviditelné údaje. Když něco není jisté, napiš obecněji.
- Výsledkem má být krátký český text použitelný do SAP/hlášení po opravě.
- Piš jednoduše: co jsme udělali, co jsme vyměnili/opravili, případně co se má objednat.
- Nepiš žádné nadpisy, odrážky, markdown ani **.
- Nepoužívej kancelářská slova jako "provedena", "provozuschopnost", "zařízení obnovena", "doporučena objednávka".
- Nezačínej "Dobrý den" a nepřidávej fráze typu "úkol splněn".
- Spoj kontext od autora hlášení s tím, co pracovník skutečně udělal. Nezaměňuj původní závadu za provedenou opravu.

Správný styl příkladu:
Zavařili jsme spojku. Druhá spojka už je špatná, je potřeba objednat dvě nové.

Špatný styl: nadpisy, markdown a kancelářské věty.

Vrať POUZE validní JSON bez markdownu:
{
  "descriptionCz": "jedna přirozená česká věta co bylo opraveno",
  "technicalReport": "1 až 3 krátké obyčejné české věty bez nadpisů a bez markdownu"
}""".trimIndent()

        val content = mutableListOf<ContentPart>(ContentPart(type = "text", text = textPrompt))
        detailImageBase64?.let {
            content += ContentPart(type = "text", text = "Foto skutečného dílu / detail závady (má přednost pro určení dílu)")
            content += ContentPart(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$it"))
        }

        val request = TranslationRequest(
            model = chosenModel,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = content)
            ),
            temperature = 0.2,
            max_tokens = 900
        )

        val raw = dynamicApi.translate(request).choices.firstOrNull()?.message?.content?.trim().orEmpty()
        val jsonText = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return@withContext try {
            val obj = JsonParser.parseString(jsonText).asJsonObject
            SapPhotoReportDraft(
                descriptionCz = obj.get("descriptionCz")?.asString?.trim().orEmpty().ifBlank { repairNote },
                technicalReport = cleanWorkerReport(obj.get("technicalReport")?.asString?.trim().orEmpty())
            )
        } catch (_: Exception) {
            SapPhotoReportDraft(descriptionCz = raw.ifBlank { repairNote }, technicalReport = cleanWorkerReport(raw))
        }
    }

    /**
     * Витягує SAP поля з опису роботи через AI.
     * Аналізує UA та CZ опис і визначає коди з каталогів.
     */
    suspend fun extractSapFields(
        descriptionCz: String,
        descriptionUa: String,
        apiKey: String,
        model: String = ModelConfig.DEFAULT_MODEL,
        notificationContext: SapNotificationData? = null
    ): SapFieldResult = withContext(Dispatchers.IO) {
        val dynamicApi = OpenRouterApi.create(apiKey)

        val catalogsText = SapCatalogs.formatForPrompt()

        val systemPrompt = "Jsi expert na SAP PM modul pro průmyslovou údržbu. " +
            "Analyzuješ popis práce a přiřazuješ správné kódy z katalogů."

        val userPrompt = """Analyzuj popis práce a vyplň SAP pole. Vrať POUZE validní JSON bez dalšího textu.

Popis práce (UA): $descriptionUa
Popis práce (CZ): $descriptionCz

Původní hlášení autora:
- Technické místo: ${notificationContext?.technicalLocation.orEmpty()}
- Popis původní závady: ${notificationContext?.notificationText.orEmpty()}
- Autor: ${notificationContext?.author.orEmpty()}

Dostupné katalogy:
$catalogsText

Pravidla:
- Část obj.: vyber nejvhodnější kód z MGLC001-005 podle popisu (jen číslo, např. "2208" nebo "2302")
- Popis škody: vyber kód z MCZ001 který nejlépe odpovídá (jen číslo, např. "1023")
- Text poškození: stručný popis v češtině (1 věta, formální styl)
- Příčina: vyber nejvhodnější kód z MGLO001-007 podle popisu (jen číslo, např. "1305" nebo "1226")
- Text příčiny: stručný popis příčiny závady v češtině (1 věta, co způsobilo problém — např. "Špatně nastavený senzor", "Uvolněný kabel", "Opotřebené těsnění")
- Dopad: 1=Bez vlivu, 2=Omezení výroby, 3=Výpadek výroby
- VŠECH 6 KLÍČŮ je povinných. Pokud si nejsi jistý textem, vrať krátký odhad; nevynechávej `damageText`, `cause`, `causeText` ani `impact`.
- U katalogových polí vrať jen samotný kód bez prefixu a bez popisu:
  - `objectPart`: jen 4 číslice (např. `2208`)
  - `damageDesc`: jen 4 číslice (např. `1012`)
  - `cause`: jen 4 číslice (např. `1399`)
  - `impact`: jen `1`, `2` nebo `3`

Vrať POUZE tento JSON, nic jiného:
{"objectPart":"2208","damageDesc":"1023","damageText":"Nelze posunout do home pozice","cause":"1305","causeText":"Zkrat způsobený vadným kabelem","impact":"3"}"""

        val request = TranslationRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            temperature = 0.2,
            max_tokens = 512
        )

        val response = dynamicApi.translate(request)
        val rawContent = response.choices.firstOrNull()?.message?.content?.trim() ?: "{}"
        val result = SapFieldParser.parse(rawContent)
        if (BuildConfig.DEBUG) {
            android.util.Log.d("Pomocnik", "SAP fields extracted from raw='$rawContent' => $result")
        }
        return@withContext result
    }

    /**
     * OCR розпізнавання коду матеріалу з фото.
     */
    suspend fun ocrMaterialCode(imageBase64: String, apiKey: String, model: String = ModelConfig.VISION_MODEL): String = withContext(Dispatchers.IO) {
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
            temperature = 0.1,
            max_tokens = 256
        )

        val response = dynamicApi.translate(request)
        response.choices.firstOrNull()?.message?.content?.trim() ?: """{"prefix": null, "number": null, "full_code": null}"""
    }
}