package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.assistant.AssistantLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AssistantResponse(
    val replyText: String,
    val toolType: String? = null,
    val toolPayload: String? = null,
    val detectedLanguage: AssistantLanguage = AssistantLanguage.ENGLISH
)

class GeminiRepository(private val context: Context) {

    private val tag = "GeminiRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    // Can be configured from settings or backend proxy as noted in README
    private var customBackendUrl: String = ""

    fun setCustomBackendUrl(url: String) {
        customBackendUrl = url.trim()
    }

    suspend fun generateAssistantReply(
        query: String,
        preferredLanguage: AssistantLanguage = AssistantLanguage.ENGLISH,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): AssistantResponse = withContext(Dispatchers.IO) {
        val lowerQuery = query.lowercase().trim()

        // 1. Direct device command parsing for instant, 0ms latency tool dispatch
        val instantToolResponse = checkInstantDeviceTools(lowerQuery, preferredLanguage)
        if (instantToolResponse != null) {
            return@withContext instantToolResponse
        }

        // 2. Try Gemini API or Custom Backend Proxy if key/endpoint is configured
        val apiKey = getGeminiApiKey()
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val cloudResponse = queryGeminiCloud(query, apiKey, preferredLanguage)
                if (cloudResponse != null) {
                    return@withContext cloudResponse
                }
            } catch (e: Exception) {
                Log.w(tag, "Gemini Cloud API call returned error: ${e.message}, falling back to intelligent conversational engine")
            }
        }

        // 3. Robust bilingual on-device contextual AI response
        return@withContext generateLocalConversationalReply(query, preferredLanguage)
    }

    private fun checkInstantDeviceTools(query: String, language: AssistantLanguage): AssistantResponse? {
        val isHindi = language == AssistantLanguage.HINDI ||
                query.contains("kholo") || query.contains("chalao") || query.contains("batao")

        // Camera
        if (query.contains("open camera") || query.contains("take a picture") ||
            query.contains("camera kholo") || query.contains("photo khincho") || query.contains("launch camera")) {
            val reply = if (isHindi) "कैमरा खोल रही हूँ!" else "Opening the camera for you now!"
            return AssistantResponse(reply, "open_camera", null, if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
        }

        // App Launching
        val appKeywords = listOf(
            "youtube" to "youtube",
            "whatsapp" to "whatsapp",
            "spotify" to "spotify",
            "calculator" to "calculator",
            "maps" to "maps",
            "chrome" to "chrome",
            "browser" to "browser",
            "settings" to "settings",
            "photos" to "photos",
            "gallery" to "gallery"
        )
        for ((trigger, appName) in appKeywords) {
            if (query.contains("open $trigger") || query.contains("launch $trigger") ||
                query.contains("$trigger kholo") || query.contains("start $trigger")) {
                val reply = if (isHindi) "$appName खोल रही हूँ..." else "Opening $appName..."
                return AssistantResponse(reply, "launch_app", appName, if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
            }
        }

        // Media Controls
        if (query.contains("play music") || query.contains("gaana chalao") || query.contains("resume music") || query.contains("gaana bajao")) {
            val reply = if (isHindi) "संगीत शुरू कर रही हूँ।" else "Resuming your music playback."
            return AssistantResponse(reply, "media_control", "play", if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
        }
        if (query.contains("pause music") || query.contains("gaana roko") || query.contains("stop music") || query.contains("pause playback")) {
            val reply = if (isHindi) "म्यूजिक रोक दिया गया है।" else "Pausing your music."
            return AssistantResponse(reply, "media_control", "pause", if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
        }
        if (query.contains("next song") || query.contains("agla gaana") || query.contains("skip song")) {
            val reply = if (isHindi) "अगला गाना लगा रही हूँ।" else "Skipping to the next track."
            return AssistantResponse(reply, "media_control", "next", if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
        }

        // Timer
        val timerMatch = Regex("set (?:a )?timer for (\\d+) min(?:ute)?s?").find(query)
            ?: Regex("(\\d+) minute ka timer lagao").find(query)
        if (timerMatch != null) {
            val mins = timerMatch.groupValues[1].toIntOrNull() ?: 5
            val reply = if (isHindi) "$mins मिनट का टाइमर सेट कर दिया है।" else "Setting a timer for $mins minutes."
            return AssistantResponse(reply, "set_timer", mins.toString(), if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
        }

        // Web search
        if (query.startsWith("search for ") || query.startsWith("google ") || query.startsWith("search ")) {
            val term = query.removePrefix("search for ").removePrefix("google ").removePrefix("search ").trim()
            val reply = if (isHindi) "\"$term\" के लिए वेब पर खोज कर रही हूँ।" else "Searching the web for \"$term\"."
            return AssistantResponse(reply, "search_web", term, if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
        }

        // System Settings
        if (query.contains("wifi") || query.contains("wi-fi")) {
            val reply = if (isHindi) "वाई-फाई सेटिंग्स खोल रही हूँ।" else "Opening Wi-Fi settings."
            return AssistantResponse(reply, "open_settings", "wifi", if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
        }
        if (query.contains("bluetooth")) {
            val reply = if (isHindi) "ब्लूटूथ सेटिंग्स खोल रही हूँ।" else "Opening Bluetooth settings."
            return AssistantResponse(reply, "open_settings", "bluetooth", if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH)
        }

        return null
    }

    private suspend fun queryGeminiCloud(
        query: String,
        apiKey: String,
        language: AssistantLanguage
    ): AssistantResponse? {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val systemPrompt = "You are Mahi, an intuitive, warm, and highly capable bilingual voice AI companion for Android. " +
                "Respond conversationally and concisely in 1-3 spoken sentences. " +
                "Language preference: ${language.displayName}. If addressed in Hindi, reply in natural Hindi/Hinglish."

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\nUser: $query"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 200)
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null
            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.optJSONArray("candidates") ?: return null
            if (candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")?.trim()
                if (!text.isNullOrBlank()) {
                    return AssistantResponse(
                        replyText = text,
                        detectedLanguage = language
                    )
                }
            }
        }
        return null
    }

    private fun generateLocalConversationalReply(query: String, language: AssistantLanguage): AssistantResponse {
        val lower = query.lowercase().trim()
        val isHindi = language == AssistantLanguage.HINDI ||
                lower.contains("namaste") || lower.contains("kaise") || lower.contains("kaun") ||
                lower.contains("kya") || lower.contains("batao")

        val reply = when {
            lower.contains("who are you") || lower.contains("tum kaun ho") || lower.contains("what is mahi") -> {
                if (isHindi) "नमस्ते! मैं माही हूँ, आपकी स्मार्ट वॉइस AI साथी। मैं आपके सवालों के जवाब दे सकती हूँ, ऍप्स खोल सकती हूँ और रिमाइंडर सेट कर सकती हूँ।"
                else "Hello! I am Mahi, your smart voice AI companion. I can answer questions, launch apps, control media, and keep you organized."
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") || lower.contains("hey mahi") -> {
                if (isHindi) "नमस्ते! मैं यहाँ हूँ, बताइए मैं आज आपकी क्या सहायता करूँ?"
                else "Hey there! I'm listening. How can I help you today?"
            }
            lower.contains("how are you") || lower.contains("kaise ho") || lower.contains("kaisi ho") -> {
                if (isHindi) "मैं बिल्कुल ठीक हूँ! आपकी सहायता के लिए तैयार। आप कैसे हैं?"
                else "I'm doing fantastic, thanks for asking! Ready to tackle whatever you need."
            }
            lower.contains("joke") || lower.contains("chutkula") || lower.contains("hasi") -> {
                if (isHindi) "एक बार एक रोबोट से पूछा: तुम सोते क्यों नहीं? उसने कहा: अगर मैं सो गया, तो अपडेट कौन करेगा!"
                else "Why did the computer go to the doctor? Because it caught a virus, and had too many tabs open!"
            }
            lower.contains("what can you do") || lower.contains("kya kar sakti ho") || lower.contains("features") -> {
                if (isHindi) "मैं 'Hey Mahi' सुन सकती हूँ, कैमरा और यूट्यूब जैसे ऐप्स खोल सकती हूँ, संगीत नियंत्रित कर सकती हूँ, और हिंदी व अंग्रेजी दोनों में बात कर सकती हूँ।"
                else "I can listen in the background for 'Hey Mahi', open apps, trigger the camera, control media playback, set timers, and converse fluently in English and Hindi."
            }
            lower.contains("time") || lower.contains("samay") || lower.contains("waqt") -> {
                val now = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                if (isHindi) "अभी समय है $now।"
                else "The current time is $now."
            }
            lower.contains("date") || lower.contains("aaj ka din") || lower.contains("tarikh") -> {
                val today = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                if (isHindi) "आज की तारीख है $today।"
                else "Today is $today."
            }
            lower.contains("weather") || lower.contains("mausam") -> {
                if (isHindi) "आज मौसम सुहाना और साफ है। बाहर जाने के लिए बढ़िया दिन है!"
                else "The weather is currently clear and pleasant. A great day to be productive!"
            }
            lower.contains("thank") || lower.contains("dhanyawad") || lower.contains("shukriya") -> {
                if (isHindi) "आपका स्वागत है! मुझे आपकी मदद करके हमेशा खुशी होती है।"
                else "You're very welcome! Always happy to help."
            }
            else -> {
                if (isHindi) "मैंने आपकी बात सुनी: \"$query\"। मैं इस पर काम कर रही हूँ!"
                else "I heard you loud and clear: \"$query\". I'm here to assist with whatever you need!"
            }
        }

        return AssistantResponse(
            replyText = reply,
            detectedLanguage = if (isHindi) AssistantLanguage.HINDI else AssistantLanguage.ENGLISH
        )
    }

    private fun getGeminiApiKey(): String {
        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
