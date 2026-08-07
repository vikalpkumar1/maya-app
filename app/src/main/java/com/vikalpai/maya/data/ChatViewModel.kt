package com.vikalpai.maya.data

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vikalpai.maya.service.HotwordService
import com.vikalpai.maya.voice.VoiceOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val apiKeyManager = ApiKeyManager(application)
    private val actionExecutor = ActionExecutor(application)
    private val voiceOutput = VoiceOutputManager(application)
    private val personalityManager = PersonalityManager(application)
    private val historyStore = ChatHistoryStore(application)

    val messages = mutableStateListOf<ChatMessage>()
    val isThinking = mutableStateOf(false)
    val apiKeyMissing = mutableStateOf(!apiKeyManager.hasApiKey())
    val speakReplies = mutableStateOf(true)
    val continuousMode = mutableStateOf(false)
    val hotwordEnabled = mutableStateOf(false)
    val humor = mutableStateOf(personalityManager.humor)
    val formality = mutableStateOf(personalityManager.formality)

    /** True if the message currently being answered came in via the mic —
     *  used by the UI to decide whether to auto-relisten in continuous mode. */
    var lastInputWasVoice: Boolean = false
        private set

    init {
        messages.addAll(historyStore.load())
    }

    fun saveApiKey(key: String) {
        apiKeyManager.saveApiKey(key)
        apiKeyMissing.value = false
    }

    fun updatePersonality(newHumor: Int, newFormality: Int) {
        humor.value = newHumor
        formality.value = newFormality
        personalityManager.humor = newHumor
        personalityManager.formality = newFormality
    }

    fun clearChat() {
        messages.clear()
        historyStore.clear()
    }

    fun setHotwordEnabled(enabled: Boolean) {
        hotwordEnabled.value = enabled
        val app = getApplication<Application>()
        val intent = Intent(app, HotwordService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(app, intent)
        } else {
            app.stopService(intent)
        }
    }

    fun sendMessage(text: String, viaVoice: Boolean = false) {
        if (text.isBlank()) return
        lastInputWasVoice = viaVoice
        messages.add(ChatMessage(text, isUser = true))
        isThinking.value = true

        viewModelScope.launch {
            try {
                val apiKey = apiKeyManager.getApiKey()
                if (apiKey.isNullOrBlank()) {
                    apiKeyMissing.value = true
                    return@launch
                }
                val rawReply = withContext(Dispatchers.IO) {
                    GeminiService(apiKey, humor.value, formality.value).sendMessage(text, messages.toList())
                }

                val (displayText, actionResult) = extractAndRunAction(rawReply)
                messages.add(ChatMessage(displayText, isUser = false))
                if (speakReplies.value) voiceOutput.speak(displayText)
                if (actionResult != null) {
                    messages.add(ChatMessage("✓ $actionResult", isUser = false))
                }
            } catch (e: Exception) {
                messages.add(ChatMessage("Error: ${e.message}", isUser = false))
            } finally {
                isThinking.value = false
                historyStore.save(messages)
            }
        }
    }

    /** Pulls a trailing "ACTION:{...}" line out of a Gemini reply, runs it, and
     *  returns the clean reply text plus a short human-readable result of the action. */
    private fun extractAndRunAction(reply: String): Pair<String, String?> {
        val lines = reply.trim().lines()
        val actionLine = lines.lastOrNull { it.trim().startsWith("ACTION:") }
        if (actionLine == null) return reply to null

        val json = actionLine.trim().removePrefix("ACTION:").trim()
        val result = actionExecutor.run(json)
        val cleanText = lines.filterNot { it == actionLine }.joinToString("\n").trim()
        return (cleanText.ifBlank { "Theek hai." }) to result
    }

    override fun onCleared() {
        voiceOutput.shutdown()
        super.onCleared()
    }
}
