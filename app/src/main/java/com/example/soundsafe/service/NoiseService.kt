package com.example.soundsafe.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.soundsafe.R
import kotlin.math.log10

class NoiseService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private var isRunning = false
    private var warningActive = false

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startNoiseMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: Der Service bleibt im Hintergrund aktiv und wird ggf. vom System neu gestartet.
        return START_STICKY
    }

    /**
     * Erstellt die Low-Priority-Notification für den Foreground Service,
     * damit die App auch im Hintergrund weiterläuft.
     */
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    /**
     * Hier wird in einer Endlosschleife der aktuelle dB-Wert über MediaRecorder ermittelt.
     */
    private fun startNoiseMonitoring() {
        try {
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

                    // Sende den aktualisierten dB-Wert via Broadcast an die MainActivity
                    val intent = Intent("UPDATE_DECIBEL").apply {
                        putExtra("DECIBEL_VALUE", decibels)
                        setPackage(applicationContext.packageName)
                    }
                    sendBroadcast(intent)

                    // Schwellenwert aus SharedPreferences lesen
                    val sharedPreferences = getSharedPreferences("SoundSafePrefs", MODE_PRIVATE)
                    val noiseLimit = sharedPreferences.getInt("NOISE_LIMIT", 85)

                    // Wenn zu laut, dann Warnung auslösen (Notification + Vibration)
                    if (decibels > noiseLimit) {
                        if (!warningActive) {
                            showHeadsUpNotification()
                            startContinuousVibration()
                            warningActive = true
                        }
                    } else {
                        // Wenn wieder unter dem Limit, Warnung beenden
                        if (warningActive) {
                            cancelHeadsUpNotification()
                            cancelContinuousVibration()
                            warningActive = false
                        }
                    }

                    // Update alle 100ms
                    Thread.sleep(100)
                } catch (e: Exception) {
                    Log.e("NoiseService", "Fehler in der Überwachungsschleife", e)
                }
            }
        }.start()
    }

    /**
     * Heads-up Notification mit hoher Wichtigkeit (IMPORTANCE_HIGH).
     * Dadurch sollte ein Benachrichtigungsbanner angezeigt werden.
     */
    private fun showHeadsUpNotification() {
        val channelId = "WarningChannel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ab Android O+ muss der Channel eine hohe Wichtigkeit haben, damit Heads-up erscheinen kann.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lärm Warnungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen, wenn der Lärmpegel das Limit überschreitet."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Warnung!")
            .setContentText("Das Lautstärke-Limit wurde überschritten.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Ab Android Lollipop (API 21) kann CATEGORY_ALARM helfen, die Heads-up anzuzeigen.
            .setCategory(Notification.CATEGORY_ALARM)
            // Ton oder Standard-Benachrichtigungseinstellungen
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // Damit die Notification „Heads-up“ ist
            .setAutoCancel(true)

        // Optionale Farbe für den Titel oder Icon-Hintergrund
        notificationBuilder.color = ContextCompat.getColor(this, R.color.purple_200)

        notificationManager.notify(2, notificationBuilder.build())
    }

    /**
     * Entfernt die laufende Heads-up Notification, wenn der Pegel wieder sinkt.
     */
    private fun cancelHeadsUpNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)
    }

    /**
     * Startet eine wiederholende Vibration (500ms an, 500ms aus).
     */
    private fun startContinuousVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        vibrator?.let {
            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, 0)
            }
        }
    }

    /**
     * Beendet das Vibrationsmuster.
     */
    private fun cancelContinuousVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        vibrator?.cancel()
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
