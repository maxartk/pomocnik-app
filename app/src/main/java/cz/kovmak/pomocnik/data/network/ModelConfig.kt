package cz.kovmak.pomocnik.data.network

/**
 * Available OpenRouter vision models for Pomocnik.
 * All models support image input for the Advisor mode.
 */
data class ModelOption(
    val id: String,           // OpenRouter model ID (used in API calls)
    val displayName: String,  // UI name in the app
    val description: String,  // Short price/quality description
    val category: String      // "budget", "balanced", "powerful"
)

object ModelConfig {
    val DEFAULT_MODEL = "google/gemini-2.5-flash"
    val SCHEDULE_OCR_MODEL = "google/gemini-2.5-flash"

    val models = listOf(
        // Budget
        ModelOption(
            id = "google/gemini-2.0-flash-001",
            displayName = "Gemini 2.0 Flash",
            description = "⚡ $0.10/$0.40 — швидка, дешева",
            category = "budget"
        ),
        ModelOption(
            id = "meta-llama/llama-4-scout",
            displayName = "Llama 4 Scout",
            description = "⚡ $0.08/$0.30 — найдешевша",
            category = "budget"
        ),
        // Balanced
        ModelOption(
            id = "google/gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            description = "🎯 $0.30/$2.50 — оптимальна якість",
            category = "balanced"
        ),
        ModelOption(
            id = "meta-llama/llama-4-maverick",
            displayName = "Llama 4 Maverick",
            description = "🎯 $0.15/$0.60 — потужна 400B",
            category = "balanced"
        ),
        // Powerful
        ModelOption(
            id = "google/gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            description = "💎 $1.25/$10.00 — найкраща якість",
            category = "powerful"
        ),
    )

    fun getModelById(id: String): ModelOption =
        models.find { it.id == id } ?: models.find { it.id == DEFAULT_MODEL }!!
}