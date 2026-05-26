package cz.kovmak.pomocnik.data.network

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class ContentPart(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String
)

data class Message(
    val role: String,
    val content: Any  // String or List<ContentPart>
)

data class TranslationRequest(
    val model: String = "google/gemini-2.0-flash-001",
    val messages: List<Message>,
    val temperature: Double = 0.3,
    val max_tokens: Int = 1024
)

data class TranslationResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageContent
)

data class MessageContent(
    val role: String,
    val content: String
)

/**
 * Custom TypeAdapter for Message class that handles the polymorphic content field.
 * When content is a String, serializes as a JSON string.
 * When content is a List<ContentPart>, serializes as a JSON array.
 */
class MessageTypeAdapter : TypeAdapter<Message>() {
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    override fun write(out: JsonWriter, message: Message) {
        out.beginObject()
        out.name("role").value(message.role)
        out.name("content")
        when (val c = message.content) {
            is String -> out.value(c)
            is List<*> -> {
                out.beginArray()
                for (item in c) {
                    if (item is ContentPart) {
                        out.beginObject()
                        out.name("type").value(item.type)
                        item.text?.let { out.name("text").value(it) }
                        item.image_url?.let { img ->
                            out.name("image_url")
                            out.beginObject()
                            out.name("url").value(img.url)
                            out.endObject()
                        }
                        out.endObject()
                    }
                }
                out.endArray()
            }
            else -> out.value(c?.toString() ?: "")
        }
        out.endObject()
    }

    override fun read(reader: JsonReader): Message {
        // We only need write for API calls, but implement read for completeness
        var role = ""
        var content: Any = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "role" -> role = reader.nextString()
                "content" -> {
                    if (reader.peek() == com.google.gson.stream.JsonToken.STRING) {
                        content = reader.nextString()
                    } else {
                        content = reader.nextString() // fallback
                    }
                }
            }
        }
        reader.endObject()
        return Message(role, content)
    }
}

interface OpenRouterApi {

    @POST("chat/completions")
    suspend fun translate(
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

            val gson = GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(Message::class.java, MessageTypeAdapter())
                .create()

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(OpenRouterApi::class.java)
        }
    }
}
