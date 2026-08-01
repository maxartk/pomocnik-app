package cz.kovmak.pomocnik.data.network

import com.google.gson.Gson
import cz.kovmak.pomocnik.data.model.SapNotificationData
import cz.kovmak.pomocnik.data.model.SapNotificationParser
import okhttp3.OkHttpClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

data class MaintenanceOcrResponse(
    val success: Boolean = false,
    val orderId: String? = null,
    val technicalLocation: String? = null,
    val notificationText: String? = null,
    val author: String? = null,
    val notificationDate: String? = null,
    val notificationTime: String? = null,
    val priority: String? = null,
    val error: String? = null
) {
    fun requireNotification(): SapNotificationData {
        if (!success) {
            throw MaintenanceApiException(error?.trim().orEmpty().ifBlank { "n8n AI Vision failed" })
        }
        val notification = SapNotificationParser.parse(Gson().toJson(this))
        if (notification.orderId.isBlank() && notification.notificationText.isBlank()) {
            throw MaintenanceApiException("AI Vision did not return readable SAP fields")
        }
        return notification
    }
}

class MaintenanceApiException(message: String) : IllegalStateException(message)

interface MaintenanceApi {
    @Multipart
    @POST("webhook/maintenance-v3")
    suspend fun readSapNotification(
        @Header("X-Pomocnik-Key") accessToken: String,
        @Part("action") action: RequestBody,
        @Part image: MultipartBody.Part
    ): MaintenanceOcrResponse

    companion object {
        private const val BASE_URL = "https://maxartk.duckdns.org/"

        fun create(baseUrl: String = BASE_URL): MaintenanceApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(150, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MaintenanceApi::class.java)
        }
    }
}
