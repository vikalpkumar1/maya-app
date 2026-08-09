package com.vikalpai.maya.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vikalpai.maya.ui.components.AiOrb
import com.vikalpai.maya.ui.theme.MayaTextSecondary

@Composable
fun LockScreen(onUnlockClick: () -> Unit) {
    // Prompt automatically as soon as this screen appears, plus a manual
    // retry button in case the system prompt gets dismissed/cancelled.
    LaunchedEffect(Unit) { onUnlockClick() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AiOrb(isThinking = false)
        Spacer(modifier = Modifier.height(24.dp))
        Text("🔒 Maya lock hai", color = MayaTextSecondary, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Face ya fingerprint se verify karo", color = MayaTextSecondary)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onUnlockClick) {
            Text("Unlock karo")
        }
    }
}
