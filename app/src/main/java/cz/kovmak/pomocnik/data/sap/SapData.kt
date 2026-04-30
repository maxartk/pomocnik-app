package cz.kovmak.pomocnik.data.sap

/**
 * SAP PM Catalog data for Pomocnik App.
 * Structure follows SAP PM notification catalogs (IW41):
 *
 *   Část objektu: Katalogový profil MGLC (kódové skupiny 001-005)
 *     → Kódy poškození pod každou skupinou
 *   Příčina: Katalogový profil MGLO (kódové skupiny 001-007)  
 *     → Kódy příčin pod každou skupinou
 */

object SapData {

    // ==================== OBJECT PARTS (ČÁST OBJEKTU) ====================
    
    data class CatalogGroup(
        val code: String,   // např. "001"
        val name: String,   // např. "Elektronic"
        val profileCode: String = "MGLC"
    ) {
        val fullCode get() = "$profileCode$code"
        val label get() = "$fullCode – $name"
    }

    data class CatalogItem(
        val code: String,   // např. "2101"
        val name: String    // např. "PLC"
    )

    val objectPartGroups = listOf(
        CatalogGroup("001", "Elektronic"),
        CatalogGroup("002", "Elektrik"),
        CatalogGroup("003", "Mechanik"),
        CatalogGroup("004", "Hydraulika"),
        CatalogGroup("005", "Pneumatika"),
    )

    /** Damage codes for each object part group */
    val damageCodes: Map<String, List<CatalogItem>> = mapOf(
        "001" to listOf(  // Elektronic
            CatalogItem("2101", "PLC"),
            CatalogItem("2102", "Servo"),
            CatalogItem("2103", "Pohon"),
            CatalogItem("2104", "Zesilovač"),
            CatalogItem("2105", "Ovládací panel"),
            CatalogItem("2106", "Odměřování"),
            CatalogItem("2107", "Síťový prvek (switch)/síť"),
            CatalogItem("2108", "Kamera / osvětlení"),
            CatalogItem("2109", "Šroubovák"),
            CatalogItem("2110", "Převodník tlaku"),
            CatalogItem("2111", "Computer"),
            CatalogItem("2112", "I/O modul"),
            CatalogItem("2113", "Čtečka RFID"),
            CatalogItem("2114", "Čtečka QR / Bar code"),
            CatalogItem("2115", "Optické tlačítko"),
            CatalogItem("2116", "Průtokoměr"),
        ),
        "002" to listOf(  // Elektrik
            CatalogItem("2201", "Motor"),
            CatalogItem("2202", "Snímač"),
            CatalogItem("2203", "Spínač"),
            CatalogItem("2204", "Relé"),
            CatalogItem("2205", "Stykač"),
            CatalogItem("2206", "Bezpečnostní relé/snímač/závora"),
            CatalogItem("2207", "Zdroj"),
            CatalogItem("2208", "Robot"),
            CatalogItem("2209", "Napájecí zásuvka AC"),
            CatalogItem("2210", "Testovací sondy"),
        ),
        "003" to listOf(  // Mechanik
            CatalogItem("2301", "Převodovka"),
            CatalogItem("2302", "Ložisko"),
            CatalogItem("2303", "Řemen / řetěz"),
            CatalogItem("2304", "Hřídel"),
            CatalogItem("2305", "Nástroj"),
            CatalogItem("2306", "Těsnění gufero"),
            CatalogItem("2307", "Čelist"),
            CatalogItem("2308", "Trapézový/kuličkový šroub"),
            CatalogItem("2309", "Vřeteno"),
            CatalogItem("2310", "Upínací systém"),
            CatalogItem("2311", "Gripper"),
            CatalogItem("2312", "Aplikátor maziva / oleje"),
            CatalogItem("2313", "Paleta"),
            CatalogItem("2314", "Přípravek"),
            CatalogItem("2315", "Pružinový balancer"),
        ),
        "004" to listOf(  // Hydraulika
            CatalogItem("2401", "Motor"),
            CatalogItem("2402", "Čerpadlo"),
            CatalogItem("2403", "Snímač průtoku"),
            CatalogItem("2404", "Válec"),
            CatalogItem("2405", "Nádrž"),
            CatalogItem("2406", "Potrubí"),
            CatalogItem("2407", "Tlakový snímač"),
            CatalogItem("2408", "Hadice"),
            CatalogItem("2409", "Regulátor tlaku"),
            CatalogItem("2410", "Zesilovač tlaku"),
            CatalogItem("2411", "Olejový filtr"),
            CatalogItem("2412", "Výměník tepla"),
            CatalogItem("2413", "Ventil"),
        ),
        "005" to listOf(  // Pneumatika
            CatalogItem("2501", "Kompresor"),
            CatalogItem("2502", "Snímač průtoku"),
            CatalogItem("2503", "Ventil"),
            CatalogItem("2504", "Válec"),
            CatalogItem("2505", "Nádrž"),
            CatalogItem("2506", "Hadice"),
            CatalogItem("2507", "Tlakový snímač"),
            CatalogItem("2508", "Úpravna vzduchu"),
            CatalogItem("2509", "Regulátor tlaku vzduchu"),
            CatalogItem("2510", "Potrubí"),
            CatalogItem("2511", "Vzduchový filtr"),
        )
    )

    fun damageCodesFor(objectPartCode: String): List<CatalogItem> =
        damageCodes[objectPartCode] ?: emptyList()

    // ==================== CAUSES (PŘÍČINA) ====================
    
    val causeGroups = listOf(
        CatalogGroup("001", "Programové", "MGLO"),
        CatalogGroup("002", "Mechaniku", "MGLO"),
        CatalogGroup("003", "Elektrická", "MGLO"),
        CatalogGroup("004", "Pneumatický / olej / voda", "MGLO"),
        CatalogGroup("005", "Chyba obsluhy", "MGLO"),
        CatalogGroup("006", "Špatná údržba, seřízení, oprava", "MGLO"),
        CatalogGroup("007", "Ostatní/Obecné", "MGLO"),
    )

    val causeCodes: Map<String, List<CatalogItem>> = mapOf(
        "001" to listOf(  // Programové
            CatalogItem("1101", "Chyba programu"),
            CatalogItem("1102", "SPS/chyba programu"),
            CatalogItem("1103", "Chyba komunikace (Bus, Ethernet)"),
            CatalogItem("1104", "Chyba operačního systému"),
            CatalogItem("1105", "Chyba databáze"),
            CatalogItem("1107", "Chyba aplikace"),
            CatalogItem("1110", "Chyba driveru / ovladače"),
            CatalogItem("1112", "Selhání programu"),
            CatalogItem("1113", "Porucha rozhraní"),
            CatalogItem("1114", "IT – Server, síť, switch ostatní"),
            CatalogItem("1199", "Ostatní"),
        ),
        "002" to listOf(  // Mechaniku
            CatalogItem("1202", "Koroze"),
            CatalogItem("1203", "Vzduchové připojení"),
            CatalogItem("1204", "Vibrace"),
            CatalogItem("1205", "Zalepení"),
            CatalogItem("1206", "Nadměrný prach"),
            CatalogItem("1207", "Vniknutí cizího předmětu"),
            CatalogItem("1208", "Vysoká vzdušná vlhkost/voda"),
            CatalogItem("1209", "Vysoký tlak"),
            CatalogItem("1210", "Nízký tlak"),
            CatalogItem("1211", "Vysoká teplota"),
            CatalogItem("1212", "Nízká teplota"),
            CatalogItem("1214", "Přetížení"),
            CatalogItem("1215", "Silové působení"),
            CatalogItem("1216", "Nárazové působení – kolize"),
            CatalogItem("1223", "Kontaminace systému, cizí materiál"),
            CatalogItem("1225", "Uvolněný / přepnutý"),
            CatalogItem("1226", "Opotřebený"),
            CatalogItem("1227", "Špatné nastavení"),
            CatalogItem("1299", "Ostatní"),
        ),
        "003" to listOf(  // Elektrická
            CatalogItem("1301", "Přetížení"),
            CatalogItem("1302", "Elektrické rušení"),
            CatalogItem("1305", "Zkrat"),
            CatalogItem("1306", "Vysoká teplota"),
            CatalogItem("1307", "Nízká teplota"),
            CatalogItem("1308", "Porušený díl, žíla, kabel"),
            CatalogItem("1314", "Výpadek fáze AC"),
            CatalogItem("1315", "Výpadek proudu"),
            CatalogItem("1316", "Špatné nastavení"),
            CatalogItem("1317", "Slabý zdroj DC"),
            CatalogItem("1399", "Ostatní"),
        ),
        "004" to listOf(  // Pneumatický / olej / voda
            CatalogItem("1410", "Netěsný"),
            CatalogItem("1411", "Zkorodovaný"),
            CatalogItem("1412", "Kolísání tlaku"),
            CatalogItem("1413", "Chyba teploty"),
            CatalogItem("1414", "Zanesený filtr"),
            CatalogItem("1415", "Hladina plnění min/max"),
            CatalogItem("1419", "Výpadek dodávky"),
            CatalogItem("1421", "Kontaminace systému, cizí materiál"),
            CatalogItem("1426", "Ostatní"),
            CatalogItem("1427", "Opotřebený"),
            CatalogItem("1428", "Špatné nastavení"),
        ),
        "005" to listOf(  // Chyba obsluhy
            CatalogItem("1501", "Nedostatek znalostí operátora"),
            CatalogItem("1502", "Nesprávné kroky ovládání zařízení"),
            CatalogItem("1503", "Nesprávné použití"),
            CatalogItem("1504", "Běžící část mimo rozsah stroje"),
            CatalogItem("1599", "Ostatní"),
        ),
        "006" to listOf(  // Špatná údržba
            CatalogItem("1601", "TPM / Autonomní údržba nekompletní"),
            CatalogItem("1602", "TPM / Autonomní údržba neexistuje"),
            CatalogItem("1603", "TPM / Autonomní údržba chybí položka"),
            CatalogItem("1604", "TPM / Autonomní údržba špatně provedena"),
            CatalogItem("1605", "Preventivní údržba nekompletní"),
            CatalogItem("1606", "Preventivní údržba neexistuje"),
            CatalogItem("1607", "Preventivní údržba chybí položka"),
            CatalogItem("1608", "Preventivní údržba špatně provedena"),
            CatalogItem("1609", "Použity nevhodné díly"),
            CatalogItem("1610", "Nesprávně nastavení kroutící moment"),
            CatalogItem("1611", "Špatné nastavení"),
            CatalogItem("1612", "Nedostatečné čištění"),
            CatalogItem("1613", "Nedostatečné mazání"),
        ),
        "007" to listOf(  // Ostatní/Obecné
            CatalogItem("1701", "Použity slabé komponenty"),
            CatalogItem("1704", "Chyba dodavatelského řetězce (materiál)"),
            CatalogItem("1705", "Vadný materiál / chyba produktu"),
            CatalogItem("1706", "Vyráběné díly mimo specifikaci"),
            CatalogItem("1707", "Opotřebený"),
            CatalogItem("1708", "Chyba konstrukce"),
            CatalogItem("1709", "Chyba přestavby/uvedení do provozu"),
            CatalogItem("1710", "Chyba designu"),
        )
    )

    fun causeCodesFor(causeGroupCode: String): List<CatalogItem> =
        causeCodes[causeGroupCode] ?: emptyList()

    // ==================== IMPACT (DOPAD) ====================
    
    val impactOptions = listOf(
        "Zastavení výroby",
        "Omezení výroby",
        "Snížení kvality",
        "Bezpečnostní riziko",
        "Environmentální riziko",
        "Žádný dopad na výrobu",
        "Zvýšené náklady",
    )

    // ==================== HELPERS ====================
    
    fun itemLabel(code: String, name: String): String = "$code – $name"
}
