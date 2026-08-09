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

                var currentPrompt = text
                var stepsLeft = MAX_STEPS

                while (stepsLeft > 0) {
                    stepsLeft--
                    // Both the AI call and any action it triggers (weather/SMS do
                    // real network/content-provider I/O) run off the main thread.
                    val (displayText, actionResult) = withContext(Dispatchers.IO) {
                        val rawReply = GeminiService(apiKey, humor.value, formality.value, language.value)
                            .sendMessage(currentPrompt, messages.toList())
                        extractAndRunAction(rawReply)
                    }

                    val stepText = if (actionResult != null) "$displayText\n✓ $actionResult" else displayText
                    messages.add(ChatMessage(stepText, isUser = false))

                    if (actionResult == null) {
                        // No further action requested — this is the final answer.
                        if (speakReplies.value) voiceOutput.speak(displayText)
                        break
                    }
                    // An action just ran as part of a multi-step request — let the
                    // model see the result and decide if another step is needed.
                    currentPrompt = "Pichla action complete hua: $actionResult. Agla step chahiye toh " +
                        "usi ACTION format mein batao, warna sirf ek chhota confirmation do (bina ACTION line ke)."
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

    companion object {
        // Safety cap on chained actions per user request — stops a
        // "do X, Y, Z" workflow from ever looping indefinitely or
        // burning through free-tier API quota unexpectedly.
        private const val MAX_STEPS = 4
    }
}
