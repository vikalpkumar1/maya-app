package com.vikalpai.maya.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around the free Google AI Studio (Gemini) API.
 * Each user supplies their own free key (see ApiKeyManager).
 *
 * humor/formality (0-100) reshape the system instruction on every call,
 * so Maya's tone follows whatever the user set in Settings — a TARS-style
 * adjustable personality instead of one fixed voice.
 */
class GeminiService(
    private val apiKey: String,
    private val humor: Int = 50,
    private val formality: Int = 40,
    private val language: String = "Hinglish"
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val model = "gemini-3.6-flash"

    private fun personalityLine(): String {
        val humorLine = when {
            humor >= 70 -> "Bahut witty aur funny raho, halka banter bhi karo."
            humor >= 40 -> "Halka-fulka humor rakho, zyada mazak mat karo."
            else -> "Seedha, no-nonsense tone rakho, jokes avoid karo."
        }
        val formalityLine = when {
            formality >= 70 -> "Formal, professional bhasha use karo."
            formality >= 40 -> "Casual-friendly tone rakho."
            else -> "Bilkul dost jaisi, relaxed bhasha use karo."
        }
        return "$humorLine $formalityLine"
    }

    private fun systemInstruction(): String = """
        Tum Maya ho — ek AI assistant, Android phone ke andar chalti ho.
        Abhi ka time: ${currentTimeText()}
        Hamesha $language mein jawab do.
        ${personalityLine()}
        Jab user pareshan/stressed lage, tone caring aur supportive rakho (Baymax jaisa).
        Hamesha short, natural jawab do.

        Agar user ne ek se zyada kaam ek saath maange hain ("X karo aur Y bhi karo"),
        toh EK hi reply mein har kaam ke liye alag-alag ACTION line do (ek se
        zyada ACTION lines allowed hain, sab jawab ke end mein, ek ke baad ek) —
        alag-alag AI-calls mein todne ki zaroorat nahi.

        Agar user koi cheez apne phone par karwana chahta hai, apne jawab ke
        end mein EXACTLY is JSON format mein ACTION line(s) jodo (har ACTION
        line mein extra text mat likho):
        ACTION:{"type":"open_app","app":"<poora official app naam, jaise "YouTube", "WhatsApp" — short form jaise "yt" mat likho>"}
        ACTION:{"type":"play_youtube","query":"<jo video/gaana chahiye uska naam>"}
        ACTION:{"type":"call","number":"<contact ka naam JAISE user ne bola, ya phone number>"}
        ACTION:{"type":"sms","number":"<contact ka naam JAISE user ne bola, ya phone number>","message":"<text>"}
        ACTION:{"type":"alarm","hour":<0-23>,"minute":<0-59>,"label":"<text>"}
        ACTION:{"type":"search","query":"<text>"}
        ACTION:{"type":"calendar","title":"<event title>","details":"<text>"}
        ACTION:{"type":"copy","text":"<text to copy>"}
        ACTION:{"type":"settings"}
        ACTION:{"type":"whatsapp","number":"<contact naam ya number>","message":"<text>"}
        ACTION:{"type":"volume","percent":<0-100>}  ya  ACTION:{"type":"volume","direction":"increase"}  ya "decrease"
        ACTION:{"type":"brightness","percent":<0-100>}
        ACTION:{"type":"voice_speed","speed":"slow"|"normal"|"fast"}
        ACTION:{"type":"voice_style","style":"female"|"male"|"normal"}
        ACTION:{"type":"weather","city":"<jis shehar ka poocha, jaise Delhi, Mumbai>"}
        ACTION:{"type":"read_sms"}
        ACTION:{"type":"memory_stats"}
        ACTION:{"type":"open_url","url":"<full web address>"}
        ACTION:{"type":"email","to":"<email address>","subject":"<text>","body":"<text>"}
        ACTION:{"type":"reminder","minutes_from_now":<number — "abhi ka time" se calculate karo>,"message":"<text>"}

        Agar koi device action nahi chahiye, ACTION line bilkul mat do.
    """.trimIndent()

    private fun currentTimeText(): String {
        val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        return format.format(java.util.Date())
    }

    fun sendMessage(prompt: String, history: List<ChatMessage>): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val contents = historyAsContents(history)
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )

        return callGemini(url, contents)
    }

    /** Sends a picked photo + prompt for image understanding — OCR, document
     *  reading, diagram explanation, general "what's in this photo". */
    fun sendImageMessage(prompt: String, imageBase64: String, history: List<ChatMessage>): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val contents = historyAsContents(history)
        val imagePart = JSONObject().put(
            "inline_data",
            JSONObject().put("mime_type", "image/jpeg").put("data", imageBase64)
        )
        val textPart = JSONObject().put("text", prompt)
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(imagePart).put(textPart))
        )

        return callGemini(url, contents)
    }

    private fun historyAsContents(history: List<ChatMessage>): JSONArray {
        val contents = JSONArray()
        for (msg in history) {
            val part = JSONObject().put("text", msg.text)
            val entry = JSONObject()
                .put("role", if (msg.isUser) "user" else "model")
                .put("parts", JSONArray().put(part))
            contents.put(entry)
        }
        return contents
    }

    private fun callGemini(url: String, contents: JSONArray): String {
        val body = JSONObject()
            .put("contents", contents)
            .put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction())))
            )

        val requestBody = body.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Gemini API error ${response.code}: ${response.body?.string()}")
            }
            val responseBody = response.body?.string() ?: throw IOException("Empty response from Gemini")
            val json = JSONObject(responseBody)
            return json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }
}
