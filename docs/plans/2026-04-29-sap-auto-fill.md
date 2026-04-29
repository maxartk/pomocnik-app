# SAP Auto-Fill — Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Коли користувач диктує/пише опис роботи українською, AI автоматично визначає та заповнює SAP поля: Část obj., Popis škody, Příčina, Text, Text příčiny, Dopad.

**Architecture:** 
1. Створити SAP каталоги як Kotlin data-класи з жорстко закодованими записами з скріншотів SAP
2. Додати нові поля в WorkEntry (Room DB) та WorkFormState (UI state)
3. Створити метод `autoFillSapFields` в WorkRepository — AI prompt який з UA опису витягує SAP коди + тексти, повертає JSON
4. Додати секцію "SAP поля" в HomeScreen.kt після перекладу — показує автозаповнені поля з можливістю редагування через dropdown каталогів
5. Збереження разом з рештою даних

**Tech Stack:** Kotlin, Jetpack Compose, Room, OpenRouter API (Gemini), Gson (вже використовується для multimodal)

---

### Task 1: Створити SapCatalog.kt — SAP каталоги

**Objective:** Створити data-класи з усіма каталогами з SAP скріншотів

**Files:**
- Create: `app/src/main/java/cz/kovmak/pomocnik/data/model/SapCatalog.kt`

**Step 1: Write SapCatalog.kt**

```kotlin
package cz.kovmak.pomocnik.data.model

data class CatalogEntry(
    val code: String,
    val description: String
)

object SapCatalogs {
    
    // MGLC002 — Část obj. (Object Part) — Електричні компоненти
    val objectParts = listOf(
        CatalogEntry("2201", "Motor"),
        CatalogEntry("2202", "Snímač"),
        CatalogEntry("2203", "Spínač"),
        CatalogEntry("2204", "Relé"),
        CatalogEntry("2205", "Stykač"),
        CatalogEntry("2206", "Bezpečnostní relé/snímač/závora"),
        CatalogEntry("2207", "Zdroj"),
        CatalogEntry("2208", "Robot"),
        CatalogEntry("2209", "Napájecí zásuvka AC"),
        CatalogEntry("2210", "Testovací sondy"),
    )
    
    // MCZ001 — Popis škody (Damage Description)
    val damageDescriptions = listOf(
        CatalogEntry("1002", "ucpaný"),
        CatalogEntry("1003", "natržený/vytržený"),
        CatalogEntry("1004", "volný/uvolněný"),
        CatalogEntry("1005", "zlomený"),
        CatalogEntry("1009", "chybějící/ztracený"),
        CatalogEntry("1010", "znečištěný"),
        CatalogEntry("1011", "přetížený"),
        CatalogEntry("1012", "bez funkce"),
        CatalogEntry("1013", "neuzavřen/neotevřen"),
        CatalogEntry("1014", "spálený"),
        CatalogEntry("1015", "odpojen/přerušen"),
        CatalogEntry("1017", "hlučný"),
        CatalogEntry("1018", "horký"),
        CatalogEntry("1022", "opotřebený"),
        CatalogEntry("1023", "ostatní"),
    )
    
    // MGL0003 — Příčina (Cause)
    val causes = listOf(
        CatalogEntry("1001", "Programové"),
        CatalogEntry("1002", "Mechanické"),
        CatalogEntry("1003", "Elektrická"),
        CatalogEntry("1004", "Pneumatický / olej / voda"),
        CatalogEntry("1005", "Chyba obsluhy"),
        CatalogEntry("1006", "Špatná údržba, seřízení, oprava"),
        CatalogEntry("1007", "Ostatní/Obecné"),
    )
    
    // Dopad (Impact)
    val impacts = listOf(
        CatalogEntry("1", "Bez vlivu"),
        CatalogEntry("2", "Omezení výroby"),
        CatalogEntry("3", "Výpadek výroby"),
    )
    
    // Catalog metadata for AI prompt
    fun formatForPrompt(): String {
        return """
Каталоги SAP:

Část obj. (MGLC002):
${objectParts.map { "  ${it.code}: ${it.description}" }.joinToString("\n")}

Popis škody (MCZ001):
${damageDescriptions.map { "${it.code}: ${it.description}" }.joinToString("\n")}

Příčina (MGL0003):
${causes.map { "${it.code}: ${it.description}" }.joinToString("\n")}

Dopad:
${impacts.map { "${it.code}: ${it.description}" }.joinToString("\n")}
""".trimIndent()
    }
}
```

**Step 2: Verify compilation**
Run: `cd /tmp/pomocnik-app && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: PASS

**Step 3: Commit**
```bash
git add app/src/main/java/cz/kovmak/pomocnik/data/model/SapCatalog.kt
git commit -m "feat: add SAP catalogs (MGLC002, MCZ001, MGL0003, Dopad)"
```

---

### Task 2: Додати SAP поля в WorkEntry (Room DB)

**Objective:** Розширити WorkEntry новими полями для SAP даних

**Files:**
- Modify: `app/src/main/java/cz/kovmak/pomocnik/data/database/WorkEntry.kt`

**Step 1: Add new columns**

```kotlin
@Entity(tableName = "work_entries")
data class WorkEntry(
    // ... existing fields unchanged ...
    
    @ColumnInfo(name = "user_email")
    val userEmail: String = "",

    // SAP fields
    @ColumnInfo(name = "sap_object_part")
    val sapObjectPart: String = "",       // e.g. "2208"
    
    @ColumnInfo(name = "sap_object_part_catalog")
    val sapObjectPartCatalog: String = "MGLC002",
    
    @ColumnInfo(name = "sap_damage_desc")
    val sapDamageDesc: String = "",       // e.g. "1023"
    
    @ColumnInfo(name = "sap_damage_desc_catalog")
    val sapDamageDescCatalog: String = "MCZ001",
    
    @ColumnInfo(name = "sap_damage_text")
    val sapDamageText: String = "",       // "Nelze posunout do home pozice"
    
    @ColumnInfo(name = "sap_cause")
    val sapCause: String = "",            // e.g. "1003"
    
    @ColumnInfo(name = "sap_cause_catalog")
    val sapCauseCatalog: String = "MGL0003",
    
    @ColumnInfo(name = "sap_cause_text")
    val sapCauseText: String = "",        // "DCS zóna"
    
    @ColumnInfo(name = "sap_impact")
    val sapImpact: String = "",           // "1", "2", or "3"
)
```

**Step 2: Add Room migration in AppDatabase.kt**
Modify: `app/src/main/java/cz/kovmak/pomocnik/data/database/AppDatabase.kt`

Add migration from version 1 to 2:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_object_part TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_object_part_catalog TEXT NOT NULL DEFAULT 'MGLC002'")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_desc TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_desc_catalog TEXT NOT NULL DEFAULT 'MCZ001'")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_damage_text TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause_catalog TEXT NOT NULL DEFAULT 'MGL0003'")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_cause_text TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE work_entries ADD COLUMN sap_impact TEXT NOT NULL DEFAULT ''")
    }
}
```

Update database version and add migration:
```kotlin
Room.databaseBuilder(appContext, AppDatabase::class.java, "work_database")
    .addMigrations(MIGRATION_1_2)
    .build()
```

**Step 3: Verify compilation**
Run: `cd /tmp/pomocnik-app && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: PASS

**Step 4: Commit**
```bash
git add app/src/main/java/cz/kovmak/pomocnik/data/database/WorkEntry.kt app/src/main/java/cz/kovmak/pomocnik/data/database/AppDatabase.kt
git commit -m "feat: add SAP fields to WorkEntry with Room migration"
```

---

### Task 3: Додати SAP поля в WorkFormState та ViewModel

**Objective:** Оновити UI state та додати setters/getters для SAP полів

**Files:**
- Modify: `app/src/main/java/cz/kovmak/pomocnik/viewmodel/WorkViewModel.kt`

**Step 1: Update WorkFormState data class**

```kotlin
data class WorkFormState(
    // ... existing fields unchanged ...
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
```

**Step 2: Add update functions after setMode():**

```kotlin
fun updateSapObjectPart(code: String) = _formState.update { it.copy(sapObjectPart = code) }
fun updateSapDamageDesc(code: String) = _formState.update { it.copy(sapDamageDesc = code) }
fun updateSapDamageText(text: String) = _formState.update { it.copy(sapDamageText = text) }
fun updateSapCause(code: String) = _formState.update { it.copy(sapCause = code) }
fun updateSapCauseText(text: String) = _formState.update { it.copy(sapCauseText = text) }
fun updateSapImpact(code: String) = _formState.update { it.copy(sapImpact = code) }
```

**Step 3: Add autoFillSapFields function after clearResults():**

```kotlin
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
```

**Step 4: Verify compilation**
Run: `cd /tmp/pomocnik-app && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: PASS

**Step 5: Commit**
```bash
git add app/src/main/java/cz/kovmak/pomocnik/viewmodel/WorkViewModel.kt
git commit -m "feat: add SAP state and auto-fill to ViewModel"
```

---

### Task 4: Створити SapFieldResult data class та extractSapFields в Repository

**Objective:** Створити AI метод який з опису витягує SAP поля

**Files:**
- Create: `app/src/main/java/cz/kovmak/pomocnik/data/model/SapFieldResult.kt`
- Modify: `app/src/main/java/cz/kovmak/pomocnik/data/repository/WorkRepository.kt`

**Step 1: Create SapFieldResult.kt**

```kotlin
package cz.kovmak.pomocnik.data.model

data class SapFieldResult(
    val objectPart: String = "",      // "2208"
    val damageDesc: String = "",      // "1023"
    val damageText: String = "",      // "Nelze posunout do home pozice"
    val cause: String = "",           // "1003"
    val causeText: String = "",       // "DCS zóna"
    val impact: String = ""           // "3"
)
```

**Step 2: Add extractSapFields method to WorkRepository.kt**

Add before the `ocrMaterialCode` function:

```kotlin
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

    val catalogsText = cz.kovmak.pomocnik.data.model.SapCatalogs.formatForPrompt()

    val systemPrompt = "Jsi expert na SAP PM modul pro průmyslovou údržbu. " +
        "Analyzuješ popis práce a přiřazuješ správné kódy z katalogů."

    val userPrompt = """Z analyzuj popis práce a vyplň SAP pole. Vrať POUZE validní JSON.

Popis práce (UA): $descriptionUa
Popis práce (CZ): $descriptionCz

Dostupné katalogy:
$catalogsText

Pravidla:
- Část obj.: vyber nejvhodnější kód z MGLC002 podle popisu
- Popis škody: vyber kód z MCZ001 který nejlépe odpovídá
- Text poškození: stručný popis v češtině (1 věta, formální styl)
- Příčina: vyber kód z MGL0003
- Text příčiny: stručná příčina v češtině
- Dopad: 1=Bez vlivu, 2=Omezení výroby, 3=Výpadek výroby (odhadni podle kontextu)

Formát odpovědi (POUZE JSON, nic jiného):
{
  "objectPart": "2208",
  "damageDesc": "1023",
  "damageText": "Nelze posunout do home pozice",
  "cause": "1003",
  "causeText": "DCS zóna",
  "impact": "3"
}"""

    val request = TranslationRequest(
        model = model,
        messages = listOf(
            Message(role = "system", content = systemPrompt),
            Message(role = "user", content = userPrompt)
        ),
        temperature = 0.2
    )

    val response = dynamicApi.translate(request)
    val rawContent = response.choices.firstOrNull()?.message?.content?.trim() ?: "{}"
    
    // Parse JSON — use Gson (already in project for multimodal)
    val gson = com.google.gson.Gson()
    try {
        val json = gson.fromJson(rawContent, com.google.gson.JsonObject::class.java)
        SapFieldResult(
            objectPart = json.get("objectPart")?.asString ?: "",
            damageDesc = json.get("damageDesc")?.asString ?: "",
            damageText = json.get("damageText")?.asString ?: "",
            cause = json.get("cause")?.asString ?: "",
            causeText = json.get("causeText")?.asString ?: "",
            impact = json.get("impact")?.asString ?: ""
        )
    } catch (e: Exception) {
        android.util.Log.e("Pomocnik", "Failed to parse SAP fields JSON: $rawContent", e)
        SapFieldResult()
    }
}
```

**Step 3: Add import in WorkRepository.kt**
Add at top: `import cz.kovmak.pomocnik.data.model.SapFieldResult`

**Step 4: Verify compilation**
Run: `cd /tmp/pomocnik-app && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: PASS

**Step 5: Commit**
```bash
git add app/src/main/java/cz/kovmak/pomocnik/data/model/SapFieldResult.kt app/src/main/java/cz/kovmak/pomocnik/data/repository/WorkRepository.kt
git commit -m "feat: add AI-based SAP field extraction in Repository"
```

---

### Task 5: Додати SAP секцію в HomeScreen UI

**Objective:** Показати автозаповнені SAP поля з dropdown каталогами після перекладу

**Files:**
- Modify: `app/src/main/java/cz/kovmak/pomocnik/ui/screens/HomeScreen.kt`

**Step 1: Add import at top:**
```kotlin
import cz.kovmak.pomocnik.data.model.SapCatalogs
```

**Step 2: Add SAP section after translation result, before technical report**

Insert after line ~473 (after the translation result AnimatedVisibility block), before the advisor result section:

```kotlin
// ==================== SAP FIELDS ====================
AnimatedVisibility(
    visible = translationResult != null,
    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
    exit = fadeOut()
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📋 SAP поля", color = NeonOrange, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    // Auto-fill button
                    OutlinedButton(
                        onClick = { viewModel.autoFillSapFields(apiKey) },
                        enabled = !formState.isAutoFilling && apiKey.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
                    ) {
                        if (formState.isAutoFilling) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonBlue, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Заповнюю...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Авто-заповнити", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Část obj. (Object Part)
                Text("Část obj. (MGLC002)", color = TextGray, fontSize = 11.sp)
                DropdownSelector(
                    selectedCode = formState.sapObjectPart,
                    entries = SapCatalogs.objectParts,
                    onSelected = { viewModel.updateSapObjectPart(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Popis škody (Damage Description)
                Text("Popis škody (MCZ001)", color = TextGray, fontSize = 11.sp)
                DropdownSelector(
                    selectedCode = formState.sapDamageDesc,
                    entries = SapCatalogs.damageDescriptions,
                    onSelected = { viewModel.updateSapDamageDesc(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Text poškození
                OutlinedTextField(
                    value = formState.sapDamageText,
                    onValueChange = { viewModel.updateSapDamageText(it) },
                    label = { Text("Text poškození", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonOrange,
                        unfocusedBorderColor = TextGray.copy(alpha = 0.2f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Příčina (Cause)
                Text("Příčina (MGL0003)", color = TextGray, fontSize = 11.sp)
                DropdownSelector(
                    selectedCode = formState.sapCause,
                    entries = SapCatalogs.causes,
                    onSelected = { viewModel.updateSapCause(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Text příčiny
                OutlinedTextField(
                    value = formState.sapCauseText,
                    onValueChange = { viewModel.updateSapCauseText(it) },
                    label = { Text("Text příčiny", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonOrange,
                        unfocusedBorderColor = TextGray.copy(alpha = 0.2f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dopad (Impact)
                Text("Dopad", color = TextGray, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (impact in SapCatalogs.impacts) {
                        FilterChip(
                            selected = formState.sapImpact == impact.code,
                            onClick = { viewModel.updateSapImpact(impact.code) },
                            label = { Text("${impact.code}. ${impact.description}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurface
                            )
                        )
                    }
                }
            }
        }
    }
}
```

**Step 3: Add DropdownSelector composable at bottom of file (before closing brace)**

```kotlin
@Composable
private fun DropdownSelector(
    selectedCode: String,
    entries: List<cz.kovmak.pomocnik.data.model.CatalogEntry>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedEntry = entries.find { it.code == selectedCode }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedEntry?.let { "${it.code}: ${it.description}" } ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, tint = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonOrange,
                unfocusedBorderColor = TextGray.copy(alpha = 0.2f),
                focusedTextColor = TextWhite,
                unfocusedTextColor = if (selectedCode.isNotEmpty()) TextWhite else TextGray.copy(alpha = 0.3f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text("${entry.code}: ${entry.description}") },
                    onClick = {
                        onSelected(entry.code)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

**Step 4: Add import**
At the top of HomeScreen.kt, add:
```kotlin
import androidx.compose.material3.*
```
(ExposedDropdownMenuBox, ExposedDropdownMenu, DropdownMenuItem are in material3)

**Step 5: Verify compilation**
Run: `cd /tmp/pomocnik-app && ./gradlew compileDebugKotlin 2>&1 | tail -10`
Expected: PASS

**Step 6: Commit**
```bash
git add app/src/main/java/cz/kovmak/pomocnik/ui/screens/HomeScreen.kt
git commit -m "feat: add SAP fields UI section with dropdown selectors"
```

---

### Task 6: Інтегрувати SAP поля в saveEntry та export

**Objective:** Зберегти SAP поля разом з WorkEntry, додати їх в email та share export

**Files:**
- Modify: `app/src/main/java/cz/kovmak/pomocnik/viewmodel/WorkViewModel.kt` (saveEntry method)
- Modify: `app/src/main/java/cz/kovmak/pomocnik/ui/screens/HomeScreen.kt` (email button)

**Step 1: Update saveEntry in WorkViewModel.kt**

Find the `WorkEntry(` construction in saveEntry and add SAP fields:

```kotlin
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
```

**Step 2: Update email body in HomeScreen.kt**

In the email button onClick handler (around line 575), add SAP fields to email body:

```kotlin
val emailBody = buildString {
    append("⚡ ${if (s.workType == "E") "Elektrická" else "Mechanická"} | #${s.orderId}\n")
    append("🕐 ${s.startTime}–${s.endTime} (${s.hours}h)\n")
    append("👷 ${profile?.name ?: ""} (${profile?.email ?: ""})\n\n")
    append("🇺🇦 UA:\n${s.descriptionUa}\n\n")
    append("🇨🇿 CZ:\n$translation\n")
    if (report.isNotEmpty()) {
        append("\n📋 Technická zpráva:\n$report\n")
    }
    // SAP fields
    if (s.sapObjectPart.isNotEmpty() || s.sapDamageDesc.isNotEmpty()) {
        append("\n🔧 SAP PM:\n")
        if (s.sapObjectPart.isNotEmpty()) append("  Část obj.: MGLC002 ${s.sapObjectPart}\n")
        if (s.sapDamageDesc.isNotEmpty()) append("  Popis škody: MCZ001 ${s.sapDamageDesc}\n")
        if (s.sapDamageText.isNotEmpty()) append("  Text: ${s.sapDamageText}\n")
        if (s.sapCause.isNotEmpty()) append("  Příčina: MGL0003 ${s.sapCause}\n")
        if (s.sapCauseText.isNotEmpty()) append("  Text příčiny: ${s.sapCauseText}\n")
        if (s.sapImpact.isNotEmpty()) append("  Dopad: ${s.sapImpact}\n")
    }
}
```

**Step 3: Update HistoryScreen to show SAP fields**
Modify: `app/src/main/java/cz/kovmak/pomocnik/ui/screens/HistoryScreen.kt`

Add SAP info in the entry detail dialog if SAP fields are present. Find where entry details are shown and add:

```kotlin
if (entry.sapObjectPart.isNotEmpty() || entry.sapDamageDesc.isNotEmpty()) {
    Spacer(modifier = Modifier.height(8.dp))
    Divider(color = TextGray.copy(alpha = 0.2f))
    Spacer(modifier = Modifier.height(8.dp))
    Text("🔧 SAP PM:", color = NeonOrange, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    if (entry.sapObjectPart.isNotEmpty()) Text("  Část obj.: ${entry.sapObjectPart}", color = TextWhite, fontSize = 12.sp)
    if (entry.sapDamageDesc.isNotEmpty()) Text("  Popis škody: ${entry.sapDamageDesc}", color = TextWhite, fontSize = 12.sp)
    if (entry.sapDamageText.isNotEmpty()) Text("  Text: ${entry.sapDamageText}", color = TextWhite, fontSize = 12.sp)
    if (entry.sapCause.isNotEmpty()) Text("  Příčina: ${entry.sapCause}", color = TextWhite, fontSize = 12.sp)
    if (entry.sapCauseText.isNotEmpty()) Text("  Text příčiny: ${entry.sapCauseText}", color = TextWhite, fontSize = 12.sp)
    if (entry.sapImpact.isNotEmpty()) Text("  Dopad: ${entry.sapImpact}", color = TextWhite, fontSize = 12.sp)
}
```

**Step 4: Verify compilation**
Run: `cd /tmp/pomocnik-app && ./gradlew compileDebugKotlin 2>&1 | tail -10`
Expected: PASS

**Step 5: Commit**
```bash
git add app/src/main/java/cz/kovmak/pomocnik/viewmodel/WorkViewModel.kt app/src/main/java/cz/kovmak/pomocnik/ui/screens/HomeScreen.kt app/src/main/java/cz/kovmak/pomocnik/ui/screens/HistoryScreen.kt
git commit -m "feat: integrate SAP fields into save, email, and history"
```

---

### Task 7: Auto-fill після перекладу (опціонально)

**Objective:** Автоматично викликати autoFillSapFields після успішного перекладу

**Files:**
- Modify: `app/src/main/java/cz/kovmak/pomocnik/viewmodel/WorkViewModel.kt`

**Step 1: Auto-trigger after translate**

In the `translate` function, after setting `_translationResult.value = translated`, add:

```kotlin
_translationResult.value = translated
_formState.update { it.copy(isTranslating = false) }

// Auto-fill SAP fields
autoFillSapFields(apiKey)
```

**Step 2: Verify compilation**
Run: `cd /tmp/pomocnik-app && ./gradlew compileDebugKotlin 2>&1 | tail -5`
Expected: PASS

**Step 3: Commit**
```bash
git add app/src/main/java/cz/kovmak/pomocnik/viewmodel/WorkViewModel.kt
git commit -m "feat: auto-fill SAP fields after translation"
```

---

## Підсумок змін

| Файл | Зміни |
|------|-------|
| `data/model/SapCatalog.kt` | Новий — SAP каталоги |
| `data/model/SapFieldResult.kt` | Новий — результат AI extraction |
| `data/database/WorkEntry.kt` | +8 SAP columns |
| `data/database/AppDatabase.kt` | +Migration 1→2 |
| `data/repository/WorkRepository.kt` | +extractSapFields() метод |
| `viewmodel/WorkViewModel.kt` | +SAP state, autoFillSapFields, saveEntry оновлено |
| `ui/screens/HomeScreen.kt` | +SAP секція UI, DropdownSelector, email оновлено |
| `ui/screens/HistoryScreen.kt` | +SAP поля в detail dialog |
