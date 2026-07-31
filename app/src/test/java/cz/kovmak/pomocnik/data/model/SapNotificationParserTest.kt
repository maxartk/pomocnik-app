package cz.kovmak.pomocnik.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SapNotificationParserTest {
    @Test
    fun parsesNotificationContextFromStrictJson() {
        val raw = """{
          "orderId":"22314597",
          "technicalLocation":"Z901-FP -FP09-DOP1",
          "notificationText":"Opotřebene spodky paletek, požadavek na opravu paletek",
          "author":"MANYCH",
          "notificationDate":"21.07.2026",
          "notificationTime":"15:29:08",
          "priority":"Vysoká"
        }"""

        val result = SapNotificationParser.parse(raw)

        assertEquals("22314597", result.orderId)
        assertEquals("Z901-FP -FP09-DOP1", result.technicalLocation)
        assertEquals("Opotřebene spodky paletek, požadavek na opravu paletek", result.notificationText)
        assertEquals("MANYCH", result.author)
        assertEquals("21.07.2026", result.notificationDate)
        assertEquals("15:29", result.notificationTime)
        assertEquals("Vysoká", result.priority)
    }

    @Test
    fun parsesNestedMarkdownJsonAndAlternateKeys() {
        val raw = """
            ```json
            {
              "notification": {
                "zakazka": "22314597",
                "technickeMisto": "Z901-FP -FP09-DOP1",
                "stavObjektu": "Požadavek na opravu paletek",
                "autorHlaseni": "MANYCH",
                "datumHlaseni": "21. 7. 2026",
                "casHlaseni": "15:29:08 CET",
                "priorita": "vysoká"
              }
            }
            ```
        """.trimIndent()

        val result = SapNotificationParser.parse(raw)

        assertEquals("22314597", result.orderId)
        assertEquals("Z901-FP -FP09-DOP1", result.technicalLocation)
        assertEquals("Požadavek na opravu paletek", result.notificationText)
        assertEquals("MANYCH", result.author)
        assertEquals("21.07.2026", result.notificationDate)
        assertEquals("15:29", result.notificationTime)
        assertEquals("Vysoká", result.priority)
    }

    @Test
    fun rejectsInventedOrMalformedDateAndTime() {
        val raw = """{"orderId":"není čitelné","notificationDate":"31.02.2026","notificationTime":"29:91","author":""}"""

        val result = SapNotificationParser.parse(raw)

        assertEquals("", result.orderId)
        assertEquals("", result.notificationDate)
        assertEquals("", result.notificationTime)
        assertEquals("", result.author)
    }
}
