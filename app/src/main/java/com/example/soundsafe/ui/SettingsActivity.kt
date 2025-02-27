package com.example.soundsafe.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen()
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("SoundSafePrefs", Context.MODE_PRIVATE)

    // Lese gespeicherten Wert oder nutze Standardwert (85 dB)
    var noiseLimit by remember {
        mutableStateOf(sharedPreferences.getInt("NOISE_LIMIT", 85))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Zurück-Button
        val activity = context as? Activity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { activity?.finish() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück",
                    tint = Color.White
                )
            }
        }

        // Dezibel-Wert
        Text(text = "$noiseLimit dB", fontSize = 50.sp, color = Color.White)

        // Slider für Lärmlimit
        Slider(
            value = noiseLimit.toFloat(),
            onValueChange = {
                noiseLimit = it.toInt()
                // Speichere neuen Wert in SharedPreferences
                sharedPreferences.edit().putInt("NOISE_LIMIT", noiseLimit).apply()
            },
            valueRange = 50f..100f
        )

        Text(text = "Lärmlimit einstellen", fontSize = 16.sp, color = Color.White)
    }
}
