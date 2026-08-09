package com.vikalpai.maya.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vikalpai.maya.ui.theme.MayaTextSecondary

private data class PermissionItem(
    val icon: String,
    val title: String,
    val description: String,
    val isGranted: () -> Boolean,
    val request: () -> Unit
)

@Composable
fun PermissionChecklistScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }

    // Re-check every permission's status whenever the user comes back to
    // the app — e.g. after being sent to a Settings screen and tapping Allow.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshKey++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshKey++ }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshKey++ }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshKey++ }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    val items = remember(refreshKey) {
        buildList {
            add(
                PermissionItem(
                    icon = "🎤",
                    title = "Microphone",
                    description = "Voice se baat karne aur 'Hey Maya' ke liye",
                    isGranted = { hasPermission(Manifest.permission.RECORD_AUDIO) },
                    request = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            )
            if (Build.VERSION.SDK_INT >= 33) {
                add(
                    PermissionItem(
                        icon = "🔔",
                        title = "Notifications",
                        description = "Background listening aur reminders dikhane ke liye",
                        isGranted = { hasPermission(Manifest.permission.POST_NOTIFICATIONS) },
                        request = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                )
            }
            add(
                PermissionItem(
                    icon = "👥",
                    title = "Contacts",
                    description = "Naam se call/SMS/WhatsApp karne ke liye",
                    isGranted = { hasPermission(Manifest.permission.READ_CONTACTS) },
                    request = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
                )
            )
            add(
                PermissionItem(
                    icon = "📩",
                    title = "SMS",
                    description = "Unread messages padhne ke liye",
                    isGranted = { hasPermission(Manifest.permission.READ_SMS) },
                    request = { smsLauncher.launch(Manifest.permission.READ_SMS) }
                )
            )
            add(
                PermissionItem(
                    icon = "🔋",
                    title = "Battery optimization",
                    description = "'Hey Maya' background mein reliably chalne ke liye",
                    isGranted = { isIgnoringBatteryOptimizations() },
                    request = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            )
            add(
                PermissionItem(
                    icon = "☀️",
                    title = "Brightness control",
                    description = "'Brightness 70% kar do' jaisa command ke liye",
                    isGranted = { Settings.System.canWrite(context) },
                    request = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Permissions", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Jo chahiye bas usko dabao — seedha sahi jagah khul jayega. Baad mein Settings (⚙) se bhi kabhi kar sakte ho.",
            color = MayaTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            items.forEach { item ->
                val granted = item.isGranted()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.icon, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title)
                        Text(item.description, color = MayaTextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (granted) {
                        Text("✅")
                    } else {
                        Button(onClick = item.request) {
                            Text("On karo")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Aage badho")
        }
    }
}
