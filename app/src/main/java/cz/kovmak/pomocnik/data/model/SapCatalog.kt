package cz.kovmak.pomocnik.data.model

data class CatalogEntry(
    val code: String,
    val description: String
)

data class ObjectPartGroup(
    val profileCode: String,  // MGLC001
    val name: String,         // Elektronic
    val items: List<CatalogEntry>
)

data class CauseGroup(
    val profileCode: String,  // MGLO001
    val name: String,         // Programové
    val items: List<CatalogEntry>
)

object SapCatalogs {
    
    // ==================== ČÁST OBJEKTU (Object Parts) ====================
    // Групи MGLC (001-005) з повними кодами
    
    val objectPartGroups = listOf(
        ObjectPartGroup("MGLC001", "Elektronic", listOf(
            CatalogEntry("2101", "PLC"),
            CatalogEntry("2102", "Servo"),
            CatalogEntry("2103", "Pohon"),
            CatalogEntry("2104", "Zesilovač"),
            CatalogEntry("2105", "Ovládací panel"),
            CatalogEntry("2106", "Odměřování"),
            CatalogEntry("2107", "Síťový prvek (switch)/síť"),
            CatalogEntry("2108", "Kamera / osvětlení"),
            CatalogEntry("2109", "Šroubovák"),
            CatalogEntry("2110", "Převodník tlaku"),
            CatalogEntry("2111", "Computer"),
            CatalogEntry("2112", "I/O modul"),
            CatalogEntry("2113", "Čtečka RFID"),
            CatalogEntry("2114", "Čtečka QR / Bar code"),
            CatalogEntry("2115", "Optické tlačítko"),
            CatalogEntry("2116", "Průtokoměr"),
        )),
        ObjectPartGroup("MGLC002", "Elektrik", listOf(
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
        )),
        ObjectPartGroup("MGLC003", "Mechanik", listOf(
            CatalogEntry("2301", "Převodovka"),
            CatalogEntry("2302", "Ložisko"),
            CatalogEntry("2303", "Řemen / řetěz"),
            CatalogEntry("2304", "Hřídel"),
            CatalogEntry("2305", "Nástroj"),
            CatalogEntry("2306", "Těsnění gufero"),
            CatalogEntry("2307", "Čelist"),
            CatalogEntry("2308", "Trapézový/kuličkový šroub"),
            CatalogEntry("2309", "Vřeteno"),
            CatalogEntry("2310", "Upínací systém"),
            CatalogEntry("2311", "Gripper"),
            CatalogEntry("2312", "Aplikátor maziva / oleje"),
            CatalogEntry("2313", "Paleta"),
            CatalogEntry("2314", "Přípravek"),
            CatalogEntry("2315", "Pružinový balancer"),
        )),
        ObjectPartGroup("MGLC004", "Hydraulika", listOf(
            CatalogEntry("2401", "Motor"),
            CatalogEntry("2402", "Čerpadlo"),
            CatalogEntry("2403", "Snímač průtoku"),
            CatalogEntry("2404", "Válec"),
            CatalogEntry("2405", "Nádrž"),
            CatalogEntry("2406", "Potrubí"),
            CatalogEntry("2407", "Tlakový snímač"),
            CatalogEntry("2408", "Hadice"),
            CatalogEntry("2409", "Regulátor tlaku"),
            CatalogEntry("2410", "Zesilovač tlaku"),
            CatalogEntry("2411", "Olejový filtr"),
            CatalogEntry("2412", "Výměník tepla"),
            CatalogEntry("2413", "Ventil"),
        )),
        ObjectPartGroup("MGLC005", "Pneumatika", listOf(
            CatalogEntry("2501", "Kompresor"),
            CatalogEntry("2502", "Snímač průtoku"),
            CatalogEntry("2503", "Ventil"),
            CatalogEntry("2504", "Válec"),
            CatalogEntry("2505", "Nádrž"),
            CatalogEntry("2506", "Hadice"),
            CatalogEntry("2507", "Tlakový snímač"),
            CatalogEntry("2508", "Úpravna vzduchu"),
            CatalogEntry("2509", "Regulátor tlaku vzduchu"),
            CatalogEntry("2510", "Potrubí"),
            CatalogEntry("2511", "Vzduchový filtr"),
        )),
    )
    
    // Backward compatibility — flat list of all object parts with group headers
    val objectParts: List<CatalogEntry>
        get() = objectPartGroups.flatMap { group ->
            listOf(CatalogEntry("HEADER", "--- ${group.profileCode} — ${group.name} ---")) +
                group.items
        }
    
    // ==================== PŘÍČINA (Cause Categories) ====================
    // Групи MGLO (001-007) з повними кодами
    
    val causeGroups = listOf(
        CauseGroup("MGLO001", "Programové", listOf(
            CatalogEntry("1101", "Chyba programu"),
            CatalogEntry("1102", "SPS/chyba programu"),
            CatalogEntry("1103", "Chyba komunikace (Bus, Ethernet)"),
            CatalogEntry("1104", "Chyba operačního systému"),
            CatalogEntry("1105", "Chyba databáze"),
            CatalogEntry("1107", "Chyba aplikace"),
            CatalogEntry("1110", "Chyba driveru / ovladače"),
            CatalogEntry("1112", "Selhání programu"),
            CatalogEntry("1113", "Porucha rozhraní"),
            CatalogEntry("1114", "IT – Server, síť, switch ostatní"),
            CatalogEntry("1199", "Ostatní"),
        )),
        CauseGroup("MGLO002", "Mechaniku", listOf(
            CatalogEntry("1202", "Koroze"),
            CatalogEntry("1203", "Vzduchové připojení"),
            CatalogEntry("1204", "Vibrace"),
            CatalogEntry("1205", "Zalepení"),
            CatalogEntry("1206", "Nadměrný prach"),
            CatalogEntry("1207", "Vniknutí cizího předmětu"),
            CatalogEntry("1208", "Vysoká vzdušná vlhkost/voda"),
            CatalogEntry("1209", "Vysoký tlak"),
            CatalogEntry("1210", "Nízký tlak"),
            CatalogEntry("1211", "Vysoká teplota"),
            CatalogEntry("1212", "Nízká teplota"),
            CatalogEntry("1214", "Přetížení"),
            CatalogEntry("1215", "Silové působení"),
            CatalogEntry("1216", "Nárazové působení - kolize"),
            CatalogEntry("1223", "Kontaminace systému, cizí materiál"),
            CatalogEntry("1225", "Uvolněný / přepnutý"),
            CatalogEntry("1226", "Opotřebený"),
            CatalogEntry("1227", "Špatné nastavení"),
            CatalogEntry("1299", "Ostatní"),
        )),
        CauseGroup("MGLO003", "Elektrická", listOf(
            CatalogEntry("1301", "Přetížení"),
            CatalogEntry("1302", "Elektrické rušení"),
            CatalogEntry("1305", "Zkrat"),
            CatalogEntry("1306", "Vysoká teplota"),
            CatalogEntry("1307", "Nízká teplota"),
            CatalogEntry("1308", "Porušený díl, žíla, kabel"),
            CatalogEntry("1314", "Výpadek fáze AC"),
            CatalogEntry("1315", "Výpadek proudu"),
            CatalogEntry("1316", "Špatné nastavení"),
            CatalogEntry("1317", "Slabý zdroj DC"),
            CatalogEntry("1399", "Ostatní"),
        )),
        CauseGroup("MGLO004", "Pneumatický / olej / voda", listOf(
            CatalogEntry("1410", "Netěsný"),
            CatalogEntry("1411", "Zkorodovaný"),
            CatalogEntry("1412", "Kolísání tlaku"),
            CatalogEntry("1413", "Chyba teploty"),
            CatalogEntry("1414", "Zanesený filtr"),
            CatalogEntry("1415", "Hladina plnění min/max"),
            CatalogEntry("1419", "Výpadek dodávky"),
            CatalogEntry("1421", "Kontaminace systému, cizí materiál"),
            CatalogEntry("1426", "Ostatní"),
            CatalogEntry("1427", "Opotřebený"),
            CatalogEntry("1428", "Špatné nastavení"),
        )),
        CauseGroup("MGLO005", "Chyba obsluhy", listOf(
            CatalogEntry("1501", "Nedostatek znalostí operátora"),
            CatalogEntry("1502", "Nesprávné kroky ovládání zařízení"),
            CatalogEntry("1503", "Nesprávné použití"),
            CatalogEntry("1504", "Běžící část mimo rozsah stroje"),
            CatalogEntry("1599", "Ostatní"),
        )),
        CauseGroup("MGLO006", "Špatná údržba, seřízení, oprava", listOf(
            CatalogEntry("1601", "TPM / Autonomní údržba nekompletní"),
            CatalogEntry("1602", "TPM / Autonomní údržba neexistuje"),
            CatalogEntry("1603", "TPM / Autonomní údržba chybí položka"),
            CatalogEntry("1604", "TPM / Autonomní údržba špatně provedena"),
            CatalogEntry("1605", "Preventivní údržba nekompletní"),
            CatalogEntry("1606", "Preventivní údržba neexistuje"),
            CatalogEntry("1607", "Preventivní údržba chybí položka"),
            CatalogEntry("1608", "Preventivní údržba špatně provedena"),
            CatalogEntry("1609", "Použity nevhodné díly"),
            CatalogEntry("1610", "Nesprávně nastavení kroutící moment"),
            CatalogEntry("1611", "Špatné nastavení"),
            CatalogEntry("1612", "Nedostatečné čištění"),
            CatalogEntry("1613", "Nedostatečné mazání"),
        )),
        CauseGroup("MGLO007", "Ostatní/Obecné", listOf(
            CatalogEntry("1701", "Použity slabé komponenty"),
            CatalogEntry("1704", "Chyba dodavatelského řetězce (materiál)"),
            CatalogEntry("1705", "Vadný materiál / chyba produktu"),
            CatalogEntry("1706", "Vyráběné díly mimo specifikaci"),
            CatalogEntry("1707", "Opotřebený"),
            CatalogEntry("1708", "Chyba konstrukce"),
            CatalogEntry("1709", "Chyba přestavby/uvedení do provozu"),
            CatalogEntry("1710", "Chyba designu"),
        )),
    )
    
    // Backward compatibility — flat list of all cause categories with group headers
    val causes: List<CatalogEntry>
        get() = causeGroups.flatMap { group ->
            listOf(CatalogEntry("HEADER", "--- ${group.profileCode} — ${group.name} ---")) +
                group.items
        }
    
    // ==================== DOPAD (Impact) ====================
    val impacts = listOf(
        CatalogEntry("1", "Bez vlivu"),
        CatalogEntry("2", "Omezení výroby"),
        CatalogEntry("3", "Výpadek výroby"),
    )
    
    // ==================== HELPERS ====================
    fun formatForPrompt(): String {
        return """
Каталоги SAP:

Část obj. (MGLC — 5 груп):
${objectPartGroups.flatMap { it.items }.map { "  ${it.code}: ${it.description}" }.joinToString("\n")}

Příčina (MGLO — 7 груп з кодами):
${causeGroups.flatMap { it.items }.map { "  ${it.code}: ${it.description}" }.joinToString("\n")}

Dopad:
${impacts.map { "${it.code}: ${it.description}" }.joinToString("\n")}
""".trimIndent()
    }
}
