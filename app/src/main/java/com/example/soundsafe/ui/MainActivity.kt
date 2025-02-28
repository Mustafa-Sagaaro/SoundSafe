package com.example.soundsafe.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.soundsafe.service.NoiseService
import com.example.soundsafe.ui.theme.SoundSafeTheme
import kotlin.math.min
import kotlin.math.roundToInt

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

    // Aktueller dB-Wert
    val decibelLevel = remember { mutableStateOf(50.0) }

    // Eingestellter Schwellenwert (Standard: 85 dB)
    val sharedPreferences = context.getSharedPreferences("SoundSafePrefs", Context.MODE_PRIVATE)
    val noiseLimit = sharedPreferences.getInt("NOISE_LIMIT", 85)

    // Berechtigungen anfordern
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                startNoiseService(context)
            } else {
                Log.d("MainActivity", "RECORD_AUDIO permission denied.")
            }
        }
    )
    val postNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission denied.")
            }
        }
    )

    // Beim ersten Start Berechtigungen prüfen
    LaunchedEffect(Unit) {
        // RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startNoiseService(context)
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        // POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // BroadcastReceiver: empfängt dB-Updates vom NoiseService
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
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
            }
        }
    }

    // UI-Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Einstellungen",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,  // oder SpaceEvenly/SpaceAround
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            NoiseGauge(
                decibelValue = decibelLevel.value.toFloat(),
                limitValue = noiseLimit.toFloat(),
                gaugeSize = 300.dp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = decibelLevel.value.roundToInt().toString(),
                    fontSize = 48.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "dB",
                    fontSize = 20.sp,
                    color = Color.Gray
                )
            }

            if (decibelLevel.value > noiseLimit) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Warnung! Das Lautstärke Limit wurde überschritten.",
                    color = Color.Red,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun NoiseGauge(
    decibelValue: Float,
    limitValue: Float,
    gaugeSize: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24f
            val radius = min(size.width, size.height) / 2f - strokeWidth

            val centerArc = center.copy(y = center.y + radius)

            drawArc(
                color = Color.DarkGray,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = centerArc.copy(x = centerArc.x - radius, y = centerArc.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val maxValue = limitValue.coerceAtLeast(100f)
            val progress = (decibelValue / maxValue).coerceIn(0f, 1f)
            val sweep = 180f * progress

            val gaugeColor = if (decibelValue <= limitValue) Color(0xFFB388FF) else Color.Red

            drawArc(
                color = gaugeColor,
                startAngle = 180f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = centerArc.copy(x = centerArc.x - radius, y = centerArc.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Startet den Foreground Service.
 */
private fun startNoiseService(context: Context) {
    val intent = Intent(context, NoiseService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}