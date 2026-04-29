package cz.kovmak.pomocnik.data.model

data class SapFieldResult(
    val objectPart: String = "",      // "2208"
    val damageDesc: String = "",      // "1023"
    val damageText: String = "",      // "Nelze posunout do home pozice"
    val cause: String = "",           // "1003"
    val causeText: String = "",       // "DCS zóna"
    val impact: String = ""           // "3"
)
