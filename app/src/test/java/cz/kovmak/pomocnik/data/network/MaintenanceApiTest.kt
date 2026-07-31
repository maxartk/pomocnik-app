package cz.kovmak.pomocnik.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MaintenanceApiTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun postsSapPhotoToN8nAndMapsStructuredNotification() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{
                      "success": true,
                      "orderId": "22314597",
                      "technicalLocation": "Z901-FP-FP09-DOP1",
                      "notificationText": "Opotřebené spodky paletek",
                      "author": "MANYCH",
                      "notificationDate": "21.07.2026",
                      "notificationTime": "15:29",
                      "priority": "Vysoká"
                    }""".trimIndent()
                )
        )
        val api = MaintenanceApi.create(server.url("/").toString())
        val image = MultipartBody.Part.createFormData(
            "file",
            "sap.jpg",
            "jpeg-data".toRequestBody("image/jpeg".toMediaType())
        )

        val response = api.readSapNotification(
            accessToken = "device-token",
            action = "ocr_notification".toRequestBody("text/plain".toMediaType()),
            image = image
        )
        val notification = response.requireNotification()

        val request = server.takeRequest()
        assertEquals("/webhook/maintenance-v3", request.path)
        assertEquals("POST", request.method)
        assertEquals("device-token", request.getHeader("X-Pomocnik-Key"))
        val multipart = request.body.readUtf8()
        assertTrue(multipart.contains("name=\"action\""))
        assertTrue(multipart.contains("ocr_notification"))
        assertTrue(multipart.contains("name=\"file\"; filename=\"sap.jpg\""))
        assertEquals("22314597", notification.orderId)
        assertEquals("Z901-FP-FP09-DOP1", notification.technicalLocation)
        assertEquals("Opotřebené spodky paletek", notification.notificationText)
        assertEquals("MANYCH", notification.author)
        assertEquals("21.07.2026", notification.notificationDate)
        assertEquals("15:29", notification.notificationTime)
        assertEquals("Vysoká", notification.priority)
    }

    @Test(expected = MaintenanceApiException::class)
    fun rejectsUnsuccessfulWebhookResponse() {
        MaintenanceOcrResponse(success = false, error = "OCR failed").requireNotification()
    }
}
