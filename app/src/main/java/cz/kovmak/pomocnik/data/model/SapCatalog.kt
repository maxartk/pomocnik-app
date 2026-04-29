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
