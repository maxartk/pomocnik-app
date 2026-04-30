package cz.kovmak.pomocnik.data.sap

/**
 * SAP PM Catalog data for Pomocnik App.
 * 
 * Structure follows SAP PM notification catalogs:
 *   - Catalog Profile (e.g. MGLC) → Code Group (e.g. 002) → Codes (e.g. 2102 Servo)
 * 
 * Fields in IW41 notification:
 *   - Část objektu (Object Part): MGLC002 + code
 *   - Popis škody (Damage Desc): MCZ001 + code
 *   - Příčina (Cause): MGL0003 + code
 *   - Dopad (Impact): free text or catalog code
 */

object SapData {
    
    /** Object Part catalog profiles (Katalogový profil pro část objektu) */
    data class ObjectPart(
        val code: String,   // Kódová skupina, např. "002"
        val name: String,   // Název skupiny
        val fullCode: String = "MGLC$code"  // Plný kód profilu
    )
    
    /** Damage/Defect code groups (Kódová skupina pro popis škody) */
    data class DamageCode(
        val code: String,       // např. "2102"
        val name: String,       // např. "Servo"
        val codeGroup: String = "MCZ001"  // Skupina kódů
    )
    
    /** Cause code groups */
    data class CauseCode(
        val code: String,
        val name: String,
        val codeGroup: String = "MGL0003"
    )

    // ==================== OBJECT PARTS ====================
    val objectParts = listOf(
        ObjectPart("001", "Elektronik"),
        ObjectPart("002", "Elektrik"),
        ObjectPart("003", "Mechanik"),
        ObjectPart("004", "Hydraulika"),
        ObjectPart("005", "Pneumatika"),
    )
    
    /** Get display label for object part */
    fun objectPartLabel(part: ObjectPart): String = "${part.fullCode} - ${part.name}"
    
    /** Get display label for selected code: "2102 Servo" → "2102 - Servo" */
    fun itemLabel(code: String, name: String): String = "$code - $name"

    // ==================== DAMAGE / DEFECT CODES (MCZ001) ====================
    /** Damage codes organized by object part code */
    val damageCodes: Map<String, List<DamageCode>> = mapOf(
        "001" to listOf(  // Elektronik
            DamageCode("1101", "Procesor / CPU"),
            DamageCode("1102", "Paměťový modul"),
            DamageCode("1103", "Napájecí zdroj"),
            DamageCode("1104", "Sběrnice / Backplane"),
            DamageCode("1105", "Vstupní karta"),
            DamageCode("1106", "Výstupní karta"),
            DamageCode("1107", "Speciální karta"),
            DamageCode("1108", "Displej / panel"),
            DamageCode("1109", "Chlazení"),
            DamageCode("1110", "Baterie / záloha"),
        ),
        "002" to listOf(  // Elektrik
            DamageCode("2102", "Servo"),
            DamageCode("2103", "Pohon"),
            DamageCode("2104", "Zesilovač"),
            DamageCode("2105", "Ovládací panel"),
            DamageCode("2106", "Odměřování"),
            DamageCode("2107", "Síťový prvek (switch)"),
            DamageCode("2108", "Kamera / displej"),
            DamageCode("2109", "Převodník"),
            DamageCode("2110", "Počítač"),
            DamageCode("2111", "I/O modul"),
            DamageCode("2112", "Čtečka RFID"),
            DamageCode("2113", "Čtečka QR / Bar code"),
            DamageCode("2114", "Optické tlačítko"),
            DamageCode("2115", "Průtokoměr"),
        ),
        "003" to listOf(  // Mechanik
            DamageCode("3101", "Ložisko"),
            DamageCode("3102", "Hřídel"),
            DamageCode("3103", "Převodovka"),
            DamageCode("3104", "Spojka"),
            DamageCode("3105", "Řemen / řetěz"),
            DamageCode("3106", "Těsnění"),
            DamageCode("3107", "Pružina"),
            DamageCode("3108", "Vedení / lišta"),
            DamageCode("3109", "Rám / konstrukce"),
            DamageCode("3110", "Kryt / ochrana"),
            DamageCode("3111", "Mazání"),
            DamageCode("3112", "Brzda"),
        ),
        "004" to listOf(  // Hydraulika
            DamageCode("4101", "Čerpadlo"),
            DamageCode("4102", "Motor"),
            DamageCode("4103", "Ventil"),
            DamageCode("4104", "Válec"),
            DamageCode("4105", "Nádrž"),
            DamageCode("4106", "Filtr"),
            DamageCode("4107", "Hadice / potrubí"),
            DamageCode("4108", "Těsnění / O-kroužek"),
            DamageCode("4109", "Akumulátor"),
            DamageCode("4110", "Chladič"),
            DamageCode("4111", "Snímač tlaku"),
            DamageCode("4112", "Rozvaděč"),
        ),
        "005" to listOf(  // Pneumatika
            DamageCode("2501", "Kompresor"),
            DamageCode("2502", "Snímač průtoku"),
            DamageCode("2503", "Ventil"),
            DamageCode("2504", "Válec"),
            DamageCode("2505", "Nádrž"),
            DamageCode("2506", "Hadice"),
            DamageCode("2507", "Tlakový snímač"),
            DamageCode("2508", "Úpravna vzduchu"),
            DamageCode("2509", "Regulátor tlaku vzduchu"),
            DamageCode("2510", "Potrubí"),
            DamageCode("2511", "Vzduchový filtr"),
        )
    )
    
    /** Get damage codes for a given object part code */
    fun damageCodesFor(partCode: String): List<DamageCode> = 
        damageCodes[partCode] ?: emptyList()

    // ==================== CAUSE CODES (MGL0003) ====================
    val causeCodes = listOf(
        // Lidský faktor
        CauseCode("1801", "Nedostatek znalostí operátora"),
        CauseCode("1802", "Chyba obsluhy"),
        CauseCode("1803", "Nedodržení pracovního postupu"),
        // Software / Řízení
        CauseCode("1401", "Chyba programu / BPS"),
        CauseCode("1402", "Chyba komunikace (Bus, Ethernet)"),
        CauseCode("1403", "Chyba operačního systému"),
        CauseCode("1404", "Chyba databáze"),
        CauseCode("1405", "Chyba aplikace"),
        CauseCode("1406", "Drivers / ovladače"),
        // Provozní
        CauseCode("1415", "Hladina plnění min/max"),
        CauseCode("1416", "Výpadek dodávky"),
        CauseCode("1421", "Kontaminace systému, cizí materiál"),
        CauseCode("1422", "Výpadek fáze"),
        // Údržba / Stav
        CauseCode("1501", "Špatná údržba, seřízení, oprava"),
        CauseCode("1502", "Špatný stav"),
        CauseCode("1503", "Opotřebení"),
        CauseCode("1504", "Koroze"),
        // Externí
        CauseCode("1601", "Přepětí / podpětí"),
        CauseCode("1602", "Vlhkost / voda"),
        CauseCode("1603", "Teplota - přehřátí"),
        CauseCode("1604", "Vibrace"),
        CauseCode("1605", "Prach / nečistoty"),
    )
    
    /** Impact codes - usually free text, but can have catalog */
    val impactOptions = listOf(
        "Zastavení výroby",
        "Omezení výroby",
        "Snížení kvality",
        "Bezpečnostní riziko",
        "Environmentální riziko",
        "Žádný dopad na výrobu",
        "Zvýšené náklady",
    )
}
