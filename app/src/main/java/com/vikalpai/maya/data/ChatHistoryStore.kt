package com.vikalpai.maya.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saves the last 100 messages to local SharedPreferences as JSON, so
 * chat history survives an app restart. No cloud, no server — stays
 * on-device like everything else in Maya.
 */
class ChatHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)

    fun load(): List<ChatMessage> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        text = o.getString("text"),
                        isUser = o.getBoolean("isUser"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(messages: List<ChatMessage>) {
        val arr = JSONArray()
        messages.takeLast(100).forEach { msg ->
            arr.put(
                JSONObject()
                    .put("text", msg.text)
                    .put("isUser", msg.isUser)
                    .put("timestamp", msg.timestamp)
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val KEY_HISTORY = "chat_history"
    }
}
