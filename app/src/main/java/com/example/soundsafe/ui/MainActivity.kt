package com.example.soundsafe.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.soundsafe.service.NoiseService
import com.example.soundsafe.ui.theme.SoundSafeTheme
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundSafeTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val decibelLevel = remember { mutableStateOf(50.0) }

    // Lese den eingestellten Schwellenwert aus SharedPreferences (Standard: 85 dB)
    val sharedPreferences = context.getSharedPreferences("SoundSafePrefs", Context.MODE_PRIVATE)
    val noiseLimit = sharedPreferences.getInt("NOISE_LIMIT", 85)

    // Launcher für RECORD_AUDIO-Berechtigung
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                startNoiseService(context)
            } else {
                Log.d("MainActivity", "RECORD_AUDIO permission denied.")
            }
        }
    )

    // Launcher für POST_NOTIFICATIONS-Berechtigung (Android 13+)
    val postNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission denied.")
            }
        }
    )

    LaunchedEffect(Unit) {
        // RECORD_AUDIO-Berechtigung prüfen und anfragen
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startNoiseService(context)
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        // POST_NOTIFICATIONS-Berechtigung (ab Android 13) prüfen und anfragen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // BroadcastReceiver für Dezibel-Updates
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val decibels = intent?.getDoubleExtra("DECIBEL_VALUE", 0.0) ?: 0.0
                decibelLevel.value = decibels
                Log.d("MainActivity", "Received decibels: $decibels")
            }
        }
        val filter = IntentFilter("UPDATE_DECIBEL")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver wurde bereits abgemeldet
            }
        }
    }

    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${decibelLevel.value.toInt()} dB",
                fontSize = 50.sp,
                color = Color.White
            )
            // Zeige Warnmeldung in der UI an, wenn der gemessene Wert den Schwellenwert überschreitet
            if (decibelLevel.value.toInt() > noiseLimit) {
                Text(
                    text = "Warnung: Es ist zu laut!",
                    fontSize = 20.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Einstellungen",
                tint = Color.White,
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
            )
        }
    }
}

private fun startNoiseService(context: Context) {
    val intent = Intent(context, NoiseService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
