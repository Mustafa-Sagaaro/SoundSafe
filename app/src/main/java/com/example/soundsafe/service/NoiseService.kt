package com.example.soundsafe.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.soundsafe.R
import kotlin.math.log10

class NoiseService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private var isRunning = false
    private var warningNotificationActive = false

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startNoiseMonitoring()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "NoiseServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Noise Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SoundSafe läuft")
            .setContentText("Lärmpegel wird überwacht...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun startNoiseMonitoring() {
        try {
            // Verwende ein temporäres File im Cache-Verzeichnis anstelle von /dev/null
            val outputFile = "${externalCacheDir?.absolutePath}/temp_audio.3gp"
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            isRunning = true
        } catch (e: Exception) {
            Log.e("NoiseService", "MediaRecorder konnte nicht gestartet werden", e)
            stopSelf()
            return
        }

        Thread {
            while (isRunning) {
                try {
                    val amplitude = mediaRecorder?.maxAmplitude ?: 0
                    val decibels = if (amplitude > 0) 20 * log10(amplitude.toDouble()) else 0.0

                    Log.d("NoiseService", "Amplitude: $amplitude, Decibels: $decibels")

                    // Sende den aktualisierten dB-Wert via Broadcast
                    val intent = Intent("UPDATE_DECIBEL").apply {
                        putExtra("DECIBEL_VALUE", decibels)
                        // Expliziter Broadcast an die eigene App
                        setPackage(applicationContext.packageName)
                    }
                    sendBroadcast(intent)

                    // Prüfe, ob der Schwellenwert überschritten wurde
                    val sharedPreferences = getSharedPreferences("SoundSafePrefs", MODE_PRIVATE)
                    val noiseLimit = sharedPreferences.getInt("NOISE_LIMIT", 85)
                    if (decibels > noiseLimit) {
                        if (!warningNotificationActive) {
                            sendWarningNotification()
                            warningNotificationActive = true
                        }
                    } else {
                        if (warningNotificationActive) {
                            cancelWarningNotification()
                            warningNotificationActive = false
                        }
                    }

                    // Update-Frequenz: 100 ms
                    Thread.sleep(100)
                } catch (e: Exception) {
                    Log.e("NoiseService", "Fehler in der Überwachungsschleife", e)
                }
            }
        }.start()
    }

    private fun sendWarningNotification() {
        val channelId = "WarningChannel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lärm Warnungen",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Lärmwarnung")
            .setContentText("Es ist zu laut!")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Passe hier dein Warn-Icon an
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(2, notification)
    }

    private fun cancelWarningNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e("NoiseService", "Fehler beim Stoppen des MediaRecorders", e)
        }
        mediaRecorder?.release()
        mediaRecorder = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
