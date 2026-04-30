package cz.kovmak.pomocnik.data.model

data class CatalogEntry(
    val code: String,
    val description: String
)

object SapCatalogs {
    
    // MGLC001-005 — Část obj. (Object Part)
    // Повний список із SAP. UI/логіка v1.34 лишається та сама: один dropdown, код зберігається як 2101/2201/...
    val objectParts = listOf(
        // MGLC001 — Elektronic
        CatalogEntry("2101", "MGLC001 Elektronic — PLC"),
        CatalogEntry("2102", "MGLC001 Elektronic — Servo"),
        CatalogEntry("2103", "MGLC001 Elektronic — Pohon"),
        CatalogEntry("2104", "MGLC001 Elektronic — Zesilovač"),
        CatalogEntry("2105", "MGLC001 Elektronic — Ovládací panel"),
        CatalogEntry("2106", "MGLC001 Elektronic — Odměřování"),
        CatalogEntry("2107", "MGLC001 Elektronic — Síťový prvek (switch)/síť"),
        CatalogEntry("2108", "MGLC001 Elektronic — Kamera / osvětlení"),
        CatalogEntry("2109", "MGLC001 Elektronic — Šroubovák"),
        CatalogEntry("2110", "MGLC001 Elektronic — Převodník tlaku"),
        CatalogEntry("2111", "MGLC001 Elektronic — Computer"),
        CatalogEntry("2112", "MGLC001 Elektronic — I/O modul"),
        CatalogEntry("2113", "MGLC001 Elektronic — Čtečka RFID"),
        CatalogEntry("2114", "MGLC001 Elektronic — Čtečka QR / Bar code"),
        CatalogEntry("2115", "MGLC001 Elektronic — Optické tlačítko"),
        CatalogEntry("2116", "MGLC001 Elektronic — Průtokoměr"),

        // MGLC002 — Elektrik
        CatalogEntry("2201", "MGLC002 Elektrik — Motor"),
        CatalogEntry("2202", "MGLC002 Elektrik — Snímač"),
        CatalogEntry("2203", "MGLC002 Elektrik — Spínač"),
        CatalogEntry("2204", "MGLC002 Elektrik — Relé"),
        CatalogEntry("2205", "MGLC002 Elektrik — Stykač"),
        CatalogEntry("2206", "MGLC002 Elektrik — Bezpečnostní relé/snímač/závora"),
        CatalogEntry("2207", "MGLC002 Elektrik — Zdroj"),
        CatalogEntry("2208", "MGLC002 Elektrik — Robot"),
        CatalogEntry("2209", "MGLC002 Elektrik — Napájecí zásuvka AC"),
        CatalogEntry("2210", "MGLC002 Elektrik — Testovací sondy"),

        // MGLC003 — Mechanik
        CatalogEntry("2301", "MGLC003 Mechanik — Převodovka"),
        CatalogEntry("2302", "MGLC003 Mechanik — Ložisko"),
        CatalogEntry("2303", "MGLC003 Mechanik — Řemen / řetěz"),
        CatalogEntry("2304", "MGLC003 Mechanik — Hřídel"),
        CatalogEntry("2305", "MGLC003 Mechanik — Nástroj"),
        CatalogEntry("2306", "MGLC003 Mechanik — Těsnění gufero"),
        CatalogEntry("2307", "MGLC003 Mechanik — Čelist"),
        CatalogEntry("2308", "MGLC003 Mechanik — Trapézový/kuličkový šroub"),
        CatalogEntry("2309", "MGLC003 Mechanik — Vřeteno"),
        CatalogEntry("2310", "MGLC003 Mechanik — Upínací systém"),
        CatalogEntry("2311", "MGLC003 Mechanik — Gripper"),
        CatalogEntry("2312", "MGLC003 Mechanik — Aplikátor maziva / oleje"),
        CatalogEntry("2313", "MGLC003 Mechanik — Paleta"),
        CatalogEntry("2314", "MGLC003 Mechanik — Přípravek"),
        CatalogEntry("2315", "MGLC003 Mechanik — Pružinový balancer"),

        // MGLC004 — Hydraulika
        CatalogEntry("2401", "MGLC004 Hydraulika — Motor"),
        CatalogEntry("2402", "MGLC004 Hydraulika — Čerpadlo"),
        CatalogEntry("2403", "MGLC004 Hydraulika — Snímač průtoku"),
        CatalogEntry("2404", "MGLC004 Hydraulika — Válec"),
        CatalogEntry("2405", "MGLC004 Hydraulika — Nádrž"),
        CatalogEntry("2406", "MGLC004 Hydraulika — Potrubí"),
        CatalogEntry("2407", "MGLC004 Hydraulika — Tlakový snímač"),
        CatalogEntry("2408", "MGLC004 Hydraulika — Hadice"),
        CatalogEntry("2409", "MGLC004 Hydraulika — Regulátor tlaku"),
        CatalogEntry("2410", "MGLC004 Hydraulika — Zesilovač tlaku"),
        CatalogEntry("2411", "MGLC004 Hydraulika — Olejový filtr"),
        CatalogEntry("2412", "MGLC004 Hydraulika — Výměník tepla"),
        CatalogEntry("2413", "MGLC004 Hydraulika — Ventil"),

        // MGLC005 — Pneumatika
        CatalogEntry("2501", "MGLC005 Pneumatika — Kompresor"),
        CatalogEntry("2502", "MGLC005 Pneumatika — Snímač průtoku"),
        CatalogEntry("2503", "MGLC005 Pneumatika — Ventil"),
        CatalogEntry("2504", "MGLC005 Pneumatika — Válec"),
        CatalogEntry("2505", "MGLC005 Pneumatika — Nádrž"),
        CatalogEntry("2506", "MGLC005 Pneumatika — Hadice"),
        CatalogEntry("2507", "MGLC005 Pneumatika — Tlakový snímač"),
        CatalogEntry("2508", "MGLC005 Pneumatika — Úpravna vzduchu"),
        CatalogEntry("2509", "MGLC005 Pneumatika — Regulátor tlaku vzduchu"),
        CatalogEntry("2510", "MGLC005 Pneumatika — Potrubí"),
        CatalogEntry("2511", "MGLC005 Pneumatika — Vzduchový filtr"),
    )
    
    // MCZ001 — Popis škody (Damage Description) — лишено як у робочій v1.34
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
    
    // MGLO001-007 — Příčina (Cause)
    val causes = listOf(
        // MGLO001 — Programové
        CatalogEntry("1101", "MGLO001 Programové — Chyba programu"),
        CatalogEntry("1102", "MGLO001 Programové — SPS/chyba programu"),
        CatalogEntry("1103", "MGLO001 Programové — Chyba komunikace (Bus, Ethernet)"),
        CatalogEntry("1104", "MGLO001 Programové — Chyba operačního systému"),
        CatalogEntry("1105", "MGLO001 Programové — Chyba databáze"),
        CatalogEntry("1107", "MGLO001 Programové — Chyba aplikace"),
        CatalogEntry("1110", "MGLO001 Programové — Chyba driveru / ovladače"),
        CatalogEntry("1112", "MGLO001 Programové — Selhání programu"),
        CatalogEntry("1113", "MGLO001 Programové — Porucha rozhraní"),
        CatalogEntry("1114", "MGLO001 Programové — IT – Server, síť, switch ostatní"),
        CatalogEntry("1199", "MGLO001 Programové — Ostatní"),

        // MGLO002 — Mechaniku
        CatalogEntry("1202", "MGLO002 Mechaniku — Koroze"),
        CatalogEntry("1203", "MGLO002 Mechaniku — Vzduchové připojení"),
        CatalogEntry("1204", "MGLO002 Mechaniku — Vibrace"),
        CatalogEntry("1205", "MGLO002 Mechaniku — Zalepení"),
        CatalogEntry("1206", "MGLO002 Mechaniku — Nadměrný prach"),
        CatalogEntry("1207", "MGLO002 Mechaniku — Vniknutí cizího předmětu"),
        CatalogEntry("1208", "MGLO002 Mechaniku — Vysoká vzdušná vlhkost/voda"),
        CatalogEntry("1209", "MGLO002 Mechaniku — Vysoký tlak"),
        CatalogEntry("1210", "MGLO002 Mechaniku — Nízký tlak"),
        CatalogEntry("1211", "MGLO002 Mechaniku — Vysoká teplota"),
        CatalogEntry("1212", "MGLO002 Mechaniku — Nízká teplota"),
        CatalogEntry("1214", "MGLO002 Mechaniku — Přetížení"),
        CatalogEntry("1215", "MGLO002 Mechaniku — Silové působení"),
        CatalogEntry("1216", "MGLO002 Mechaniku — Nárazové působení - kolize"),
        CatalogEntry("1223", "MGLO002 Mechaniku — Kontaminace systému, cizí materiál"),
        CatalogEntry("1225", "MGLO002 Mechaniku — Uvolněný / přepnutý"),
        CatalogEntry("1226", "MGLO002 Mechaniku — Opotřebený"),
        CatalogEntry("1227", "MGLO002 Mechaniku — Špatné nastavení"),
        CatalogEntry("1299", "MGLO002 Mechaniku — Ostatní"),

        // MGLO003 — Elektrická
        CatalogEntry("1301", "MGLO003 Elektrická — Přetížení"),
        CatalogEntry("1302", "MGLO003 Elektrická — Elektrické rušení"),
        CatalogEntry("1305", "MGLO003 Elektrická — Zkrat"),
        CatalogEntry("1306", "MGLO003 Elektrická — Vysoká teplota"),
        CatalogEntry("1307", "MGLO003 Elektrická — Nízká teplota"),
        CatalogEntry("1308", "MGLO003 Elektrická — Porušený díl, žíla, kabel"),
        CatalogEntry("1314", "MGLO003 Elektrická — Výpadek fáze AC"),
        CatalogEntry("1315", "MGLO003 Elektrická — Výpadek proudu"),
        CatalogEntry("1316", "MGLO003 Elektrická — Špatné nastavení"),
        CatalogEntry("1317", "MGLO003 Elektrická — Slabý zdroj DC"),
        CatalogEntry("1399", "MGLO003 Elektrická — Ostatní"),

        // MGLO004 — Pneumatický / olej / voda
        CatalogEntry("1410", "MGLO004 Pneumatický / olej / voda — Netěsný"),
        CatalogEntry("1411", "MGLO004 Pneumatický / olej / voda — Zkorodovaný"),
        CatalogEntry("1412", "MGLO004 Pneumatický / olej / voda — Kolísání tlaku"),
        CatalogEntry("1413", "MGLO004 Pneumatický / olej / voda — Chyba teploty"),
        CatalogEntry("1414", "MGLO004 Pneumatický / olej / voda — Zanesený filtr"),
        CatalogEntry("1415", "MGLO004 Pneumatický / olej / voda — Hladina plnění min/max"),
        CatalogEntry("1419", "MGLO004 Pneumatický / olej / voda — Výpadek dodávky"),
        CatalogEntry("1421", "MGLO004 Pneumatický / olej / voda — Kontaminace systému, cizí materiál"),
        CatalogEntry("1426", "MGLO004 Pneumatický / olej / voda — Ostatní"),
        CatalogEntry("1427", "MGLO004 Pneumatický / olej / voda — Opotřebený"),
        CatalogEntry("1428", "MGLO004 Pneumatický / olej / voda — Špatné nastavení"),

        // MGLO005 — Chyba obsluhy
        CatalogEntry("1501", "MGLO005 Chyba obsluhy — Nedostatek znalostí operátora"),
        CatalogEntry("1502", "MGLO005 Chyba obsluhy — Nesprávné kroky ovládání zařízení"),
        CatalogEntry("1503", "MGLO005 Chyba obsluhy — Nesprávné použití"),
        CatalogEntry("1504", "MGLO005 Chyba obsluhy — Běžící část mimo rozsah stroje"),
        CatalogEntry("1599", "MGLO005 Chyba obsluhy — Ostatní"),

        // MGLO006 — Špatná údržba, seřízení, oprava
        CatalogEntry("1601", "MGLO006 Špatná údržba, seřízení, oprava — TPM / Autonomní údržba nekompletní"),
        CatalogEntry("1602", "MGLO006 Špatná údržba, seřízení, oprava — TPM / Autonomní údržba neexistuje"),
        CatalogEntry("1603", "MGLO006 Špatná údržba, seřízení, oprava — TPM / Autonomní údržba chybí položka"),
        CatalogEntry("1604", "MGLO006 Špatná údržba, seřízení, oprava — TPM / Autonomní údržba špatně provedena"),
        CatalogEntry("1605", "MGLO006 Špatná údržba, seřízení, oprava — Preventivní údržba nekompletní"),
        CatalogEntry("1606", "MGLO006 Špatná údržba, seřízení, oprava — Preventivní údržba neexistuje"),
        CatalogEntry("1607", "MGLO006 Špatná údržba, seřízení, oprava — Preventivní údržba chybí položka"),
        CatalogEntry("1608", "MGLO006 Špatná údržba, seřízení, oprava — Preventivní údržba špatně provedena"),
        CatalogEntry("1609", "MGLO006 Špatná údržba, seřízení, oprava — Použity nevhodné díly"),
        CatalogEntry("1610", "MGLO006 Špatná údržba, seřízení, oprava — Nesprávně nastavení kroutící moment"),
        CatalogEntry("1611", "MGLO006 Špatná údržba, seřízení, oprava — Špatné nastavení"),
        CatalogEntry("1612", "MGLO006 Špatná údržba, seřízení, oprava — Nedostatečné čištění"),
        CatalogEntry("1613", "MGLO006 Špatná údržba, seřízení, oprava — Nedostatečné mazání"),

        // MGLO007 — Ostatní/Obecné
        CatalogEntry("1701", "MGLO007 Ostatní/Obecné — Použity slabé komponenty"),
        CatalogEntry("1704", "MGLO007 Ostatní/Obecné — Chyba dodavatelského řetězce (materiál)"),
        CatalogEntry("1705", "MGLO007 Ostatní/Obecné — Vadný materiál / chyba produktu"),
        CatalogEntry("1706", "MGLO007 Ostatní/Obecné — Vyráběné díly mimo specifikaci"),
        CatalogEntry("1707", "MGLO007 Ostatní/Obecné — Opotřebený"),
        CatalogEntry("1708", "MGLO007 Ostatní/Obecné — Chyba konstrukce"),
        CatalogEntry("1709", "MGLO007 Ostatní/Obecné — Chyba přestavby/uvedení do provozu"),
        CatalogEntry("1710", "MGLO007 Ostatní/Obecné — Chyba designu"),
    )
    
    // Dopad (Impact) — лишено як у робочій v1.34
    val impacts = listOf(
        CatalogEntry("1", "Bez vlivu"),
        CatalogEntry("2", "Omezení výroby"),
        CatalogEntry("3", "Výpadek výroby"),
    )
    
    // Catalog metadata for AI prompt
    fun formatForPrompt(): String {
        return """
Каталоги SAP:

Část obj. (MGLC001-005):
${objectParts.map { "  ${it.code}: ${it.description}" }.joinToString("\n")}

Popis škody (MCZ001):
${damageDescriptions.map { "  ${it.code}: ${it.description}" }.joinToString("\n")}

Příčina (MGLO001-007):
${causes.map { "  ${it.code}: ${it.description}" }.joinToString("\n")}

Dopad:
${impacts.map { "  ${it.code}: ${it.description}" }.joinToString("\n")}
""".trimIndent()
    }
}
