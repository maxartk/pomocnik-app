package cz.kovmak.pomocnik.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SapFieldParserTest {
    @Test
    fun parsesStrictJsonResponse() {
        val raw = """{"objectPart":"2208","damageDesc":"1012","damageText":"Robot je bez funkce","cause":"1399","causeText":"Ostatní elektrická závada","impact":"3"}"""

        val result = SapFieldParser.parse(raw)

        assertEquals("2208", result.objectPart)
        assertEquals("1012", result.damageDesc)
        assertEquals("Robot je bez funkce", result.damageText)
        assertEquals("1399", result.cause)
        assertEquals("Ostatní elektrická závada", result.causeText)
        assertEquals("3", result.impact)
    }

    @Test
    fun normalizesPrefixedCodesAndImpactText() {
        val raw = """
            ```json
            {
              "objectPart": "MGLC002 Elektrik — 2208 Robot",
              "damageDesc": "MCZ001 / 1012 bez funkce",
              "damageText": "Robotická buňka nekomunikuje.",
              "cause": "MGLO003 Elektrická — 1399 Ostatní",
              "causeText": "Pravděpodobná elektrická závada.",
              "impact": "3 - Výpadek výroby"
            }
            ```
        """.trimIndent()

        val result = SapFieldParser.parse(raw)

        assertEquals("2208", result.objectPart)
        assertEquals("1012", result.damageDesc)
        assertEquals("Robotická buňka nekomunikuje.", result.damageText)
        assertEquals("1399", result.cause)
        assertEquals("Pravděpodobná elektrická závada.", result.causeText)
        assertEquals("3", result.impact)
    }

    @Test
    fun parsesNestedJsonWithAlternateKeys() {
        val raw = """
            {
              "sapFields": {
                "object_part": "2302",
                "damage_description": "1023",
                "textPoskozeni": "Ložisko hlučí.",
                "pricina": "1226",
                "textPriciny": "Opotřebení.",
                "dopad": "Omezení výroby"
              }
            }
        """.trimIndent()

        val result = SapFieldParser.parse(raw)

        assertEquals("2302", result.objectPart)
        assertEquals("1023", result.damageDesc)
        assertEquals("Ložisko hlučí.", result.damageText)
        assertEquals("1226", result.cause)
        assertEquals("Opotřebení.", result.causeText)
        assertEquals("2", result.impact)
    }
}
