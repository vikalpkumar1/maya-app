package com.vikalpai.maya

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vikalpai.maya.data.ChatViewModel
import com.vikalpai.maya.ui.ChatScreen
import com.vikalpai.maya.ui.LockScreen
import com.vikalpai.maya.ui.PermissionChecklistScreen
import com.vikalpai.maya.ui.theme.MayaTheme

/**
 * FragmentActivity (not plain ComponentActivity) because androidx.biometric's
 * BiometricPrompt requires it — FragmentActivity extends ComponentActivity,
 * so setContent { } for Compose still works exactly the same.
 */
class MainActivity : FragmentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleVoiceCommandIntent(intent)
        setContent {
            MayaTheme {
                when {
                    !viewModel.isUnlocked.value -> LockScreen(onUnlockClick = { showBiometricPrompt() })
                    !viewModel.onboardingDone.value || viewModel.showPermissionChecklist.value ->
                        PermissionChecklistScreen(onDone = { viewModel.dismissPermissionChecklist() })
                    else -> ChatScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleVoiceCommandIntent(intent)
    }

    private fun handleVoiceCommandIntent(intent: Intent?) {
        val command = intent?.getStringExtra(EXTRA_VOICE_COMMAND)
        if (!command.isNullOrBlank()) {
            viewModel.sendMessage(command, viaVoice = true)
        }
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val allowed = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        if (biometricManager.canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometric hardware/enrollment on this device — don't lock
            // the user out of their own app over a phone limitation.
            viewModel.unlock()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.unlock()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Maya unlock karo")
            .setSubtitle("Face ya fingerprint se verify karo")
            .setAllowedAuthenticators(allowed)
            .build()

        prompt.authenticate(promptInfo)
    }

    companion object {
        const val EXTRA_VOICE_COMMAND = "voice_command"
    }
}
