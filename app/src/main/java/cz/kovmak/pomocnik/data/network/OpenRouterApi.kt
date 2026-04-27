package cz.kovmak.pomocnik.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class TranslationRequest(
    val model: String = "google/gemini-2.0-flash-001",
    val messages: List<Message>,
    val temperature: Double = 0.3
)

data class Message(
    val role: String,
    val content: String
)

data class TranslationResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

interface OpenRouterApi {

    @POST("chat/completions")
    suspend fun translate(
        @Header("Authorization") auth: String,
        @Body request: TranslationRequest
    ): TranslationResponse

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/"

        fun create(apiKey: String): OpenRouterApi {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }

            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Bearer $apiKey")
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://github.com/kovmak/pomocnik")
                    .header("X-Title", "Pomocnik")
                    .build()
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenRouterApi::class.java)
        }
    }
}
