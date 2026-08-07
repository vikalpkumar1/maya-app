package com.vikalpai.maya

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.vikalpai.maya.data.ChatViewModel
import com.vikalpai.maya.ui.ChatScreen
import com.vikalpai.maya.ui.theme.MayaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleVoiceCommandIntent(intent)
        setContent {
            MayaTheme {
                ChatScreen(viewModel = viewModel)
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

    companion object {
        const val EXTRA_VOICE_COMMAND = "voice_command"
    }
}
