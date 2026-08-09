package com.vikalpai.maya.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vikalpai.maya.data.ChatViewModel
import com.vikalpai.maya.ui.components.AiOrb
import com.vikalpai.maya.ui.theme.MayaSurface
import com.vikalpai.maya.ui.theme.MayaTextSecondary
import com.vikalpai.maya.voice.VoiceInputManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val voiceInput = remember { VoiceInputManager(context) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceInput.listen(
                onResult = { text -> viewModel.sendMessage(text, viaVoice = true) },
                onError = { }
            )
        }
    }

    // Requests mic (+ notification permission on Android 13+) before turning
    // the background "Hey Maya" listener on.
    val hotwordPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.setHotwordEnabled(true)
        }
    }

    fun startListening() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            voiceInput.listen(
                onResult = { text -> viewModel.sendMessage(text, viaVoice = true) },
                onError = { }
            )
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun toggleHotword() {
        if (viewModel.hotwordEnabled.value) {
            viewModel.setHotwordEnabled(false)
        } else {
            val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            hotwordPermissionsLauncher.launch(perms.toTypedArray())
        }
    }

    // Continuous mode: once a voice-driven reply finishes, auto re-listen
    // so the conversation keeps flowing without pressing the mic again.
    LaunchedEffect(viewModel.isThinking.value) {
        if (!viewModel.isThinking.value && viewModel.continuousMode.value && viewModel.lastInputWasVoice) {
            delay(700)
            startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose { voiceInput.destroy() }
    }

    if (showSettings) {
        SettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maya") },
                actions = {
                    TextButton(onClick = { toggleHotword() }) {
                        Text(if (viewModel.hotwordEnabled.value) "🎧" else "🎧⛔")
                    }
                    TextButton(onClick = { viewModel.continuousMode.value = !viewModel.continuousMode.value }) {
                        Text(if (viewModel.continuousMode.value) "🔁" else "⏸")
                    }
                    TextButton(onClick = { viewModel.speakReplies.value = !viewModel.speakReplies.value }) {
                        Text(if (viewModel.speakReplies.value) "🔊" else "🔇")
                    }
                    TextButton(onClick = { showSettings = true }) {
                        Text("⚙")
                    }
                }
            )
        },
        bottomBar = {
            if (!viewModel.apiKeyMissing.value) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { startListening() }) {
                        Text("🎤")
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Maya se kuch pucho...") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        viewModel.sendMessage(input, viaVoice = false)
                        input = ""
                    }) {
                        Text("Bhejo")
                    }
                }
            }
        }
    ) { padding ->
        if (viewModel.apiKeyMissing.value) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AiOrb(isThinking = false)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Apni free Gemini API key daalo", color = MayaTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "aistudio.google.com/apikey se free milegi",
                    color = MayaTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("API key") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.saveApiKey(apiKeyInput) }) {
                    Text("Save")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AiOrb(isThinking = viewModel.isThinking.value)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    items(viewModel.messages) { msg ->
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(color = MayaSurface, shape = RoundedCornerShape(16.dp)) {
                                Text(text = msg.text, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    var humorSlider by remember { mutableStateOf(viewModel.humor.value.toFloat()) }
    var formalitySlider by remember { mutableStateOf(viewModel.formality.value.toFloat()) }
    var languageChoice by remember { mutableStateOf(viewModel.language.value) }
    val languages = listOf("Hinglish", "Hindi", "English", "Spanish")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maya Settings") },
        text = {
            Column {
                Text("Humor: ${humorSlider.toInt()}%")
                Slider(value = humorSlider, onValueChange = { humorSlider = it }, valueRange = 0f..100f)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Formality: ${formalitySlider.toInt()}%")
                Slider(value = formalitySlider, onValueChange = { formalitySlider = it }, valueRange = 0f..100f)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Language")
                Row(modifier = Modifier.fillMaxWidth()) {
                    languages.forEach { lang ->
                        val selected = languageChoice == lang
                        TextButton(onClick = { languageChoice = lang }) {
                            Text(if (selected) "● $lang" else lang)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    viewModel.openPermissionChecklist()
                    onDismiss()
                }) {
                    Text("🔑 Sab permissions ek jagah dekho")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔒 App lock (face/fingerprint)")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = viewModel.appLockEnabled.value,
                        onCheckedChange = { viewModel.setAppLockEnabled(it) }
                    )
                }
                TextButton(onClick = { viewModel.clearChat() }) {
                    Text("Chat history clear karo")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.updatePersonality(humorSlider.toInt(), formalitySlider.toInt(), languageChoice)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
