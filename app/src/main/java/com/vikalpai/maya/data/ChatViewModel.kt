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
    private val voiceOutput = VoiceOutputManager(application)
    private val actionExecutor = ActionExecutor(application, voiceOutput)
    private val personalityManager = PersonalityManager(application)
    private val historyStore = ChatHistoryStore(application)
    private val hotwordPrefs = HotwordPrefs(application)
    private val appLockManager = AppLockManager(application)
    private val permissionsManager = PermissionsManager(application)

    val messages = mutableStateListOf<ChatMessage>()
    val isThinking = mutableStateOf(false)
    val apiKeyMissing = mutableStateOf(!apiKeyManager.hasApiKey())
    val speakReplies = mutableStateOf(true)
    val continuousMode = mutableStateOf(false)
    val hotwordEnabled = mutableStateOf(hotwordPrefs.enabled)
    val humor = mutableStateOf(personalityManager.humor)
    val formality = mutableStateOf(personalityManager.formality)
    val language = mutableStateOf(personalityManager.language)
    val appLockEnabled = mutableStateOf(appLockManager.lockEnabled)

    /** True after the one-time permission checklist has been shown and dismissed. */
    val onboardingDone = mutableStateOf(permissionsManager.onboardingDone)

    /** Settings (⚙) can flip this true anytime to reopen the same checklist later. */
    val showPermissionChecklist = mutableStateOf(false)

    /** True once the user has passed the biometric check for this app session
     *  (or app lock isn't enabled). MainActivity/ChatScreen read this to decide
     *  whether to show the lock screen. */
    val isUnlocked = mutableStateOf(!appLockManager.lockEnabled)

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

    fun updatePersonality(newHumor: Int, newFormality: Int, newLanguage: String) {
        humor.value = newHumor
        formality.value = newFormality
        language.value = newLanguage
        personalityManager.humor = newHumor
        personalityManager.formality = newFormality
        personalityManager.language = newLanguage
    }

    fun clearChat() {
        messages.clear()
        historyStore.clear()
    }

    fun setHotwordEnabled(enabled: Boolean) {
        hotwordEnabled.value = enabled
        hotwordPrefs.enabled = enabled
        val app = getApplication<Application>()
        val intent = Intent(app, HotwordService::class.java)
        if (enabled) {
            ContextCompat.startForegroundService(app, intent)
        } else {
            app.stopService(intent)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        appLockEnabled.value = enabled
        appLockManager.lockEnabled = enabled
        if (!enabled) isUnlocked.value = true
    }

    fun unlock() {
        isUnlocked.value = true
    }

    fun openPermissionChecklist() {
        showPermissionChecklist.value = true
    }

    fun dismissPermissionChecklist() {
        if (!onboardingDone.value) {
            onboardingDone.value = true
            permissionsManager.onboardingDone = true
        }
        showPermissionChecklist.value = false
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
                // Both the AI call and any actions it triggers (weather/SMS do
                // real network/content-provider I/O) run off the main thread.
                val (displayText, actionResults) = withContext(Dispatchers.IO) {
                    val rawReply = GeminiService(apiKey, humor.value, formality.value, language.value)
                        .sendMessage(text, messages.toList())
                    extractAndRunActions(rawReply)
                }

                val finalText = if (actionResults.isNotEmpty()) {
                    displayText + "\n" + actionResults.joinToString("\n") { "✓ $it" }
                } else {
                    displayText
                }
                messages.add(ChatMessage(finalText, isUser = false))
                // Every reply is spoken — not just ones without an action —
                // so Maya never silently does something without saying so.
                if (speakReplies.value) voiceOutput.speak(displayText)
            } catch (e: Exception) {
                messages.add(ChatMessage("Error: ${e.message}", isUser = false))
            } finally {
                isThinking.value = false
                historyStore.save(messages)
            }
        }
    }

    fun sendImageMessage(uri: android.net.Uri) {
        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            apiKeyMissing.value = true
            return
        }
        messages.add(ChatMessage("📷 Photo bheji", isUser = true))
        isThinking.value = true

        viewModelScope.launch {
            try {
                val (displayText, actionResults) = withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val imageBase64 = ImageHelper.encodeForUpload(app, uri)
                    if (imageBase64 == null) {
                        "Photo read nahi ho payi" to emptyList<String>()
                    } else {
                        val prompt = "Is photo ko dhyan se dekho — jo bhi likha hai wo padho (OCR), " +
                            "jo dikh raha hai use samjhao, aur agar koi important detail (jaise date, " +
                            "number, ya text) hai toh highlight karo."
                        val rawReply = GeminiService(apiKey, humor.value, formality.value, language.value)
                            .sendImageMessage(prompt, imageBase64, messages.toList())
                        extractAndRunActions(rawReply)
                    }
                }
                val finalText = if (actionResults.isNotEmpty()) {
                    displayText + "\n" + actionResults.joinToString("\n") { "✓ $it" }
                } else {
                    displayText
                }
                messages.add(ChatMessage(finalText, isUser = false))
                if (speakReplies.value) voiceOutput.speak(displayText)
            } catch (e: Exception) {
                messages.add(ChatMessage("Error: ${e.message}", isUser = false))
            } finally {
                isThinking.value = false
                historyStore.save(messages)
            }
        }
    }

    /** Pulls every "ACTION:{...}" line out of a Gemini reply (there can be more
     *  than one when the user asked for several independent things at once),
     *  runs each in order, and returns the clean reply text plus each action's
     *  short human-readable result. */
    private fun extractAndRunActions(reply: String): Pair<String, List<String>> {
        val lines = reply.trim().lines()
        val actionLines = lines.filter { it.trim().startsWith("ACTION:") }
        if (actionLines.isEmpty()) return reply.trim() to emptyList()

        val results = mutableListOf<String>()
        for (actionLine in actionLines) {
            val json = actionLine.trim().removePrefix("ACTION:").trim()
            actionExecutor.run(json)?.let { results.add(it) }
        }
        val cleanText = lines.filterNot { it.trim().startsWith("ACTION:") }.joinToString("\n").trim()
        return (cleanText.ifBlank { "Theek hai." }) to results
    }

    override fun onCleared() {
        voiceOutput.shutdown()
        super.onCleared()
    }
}
