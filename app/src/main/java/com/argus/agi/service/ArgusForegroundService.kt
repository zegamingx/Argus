package com.argus.agi.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.argus.agi.ArgusApp
import com.argus.agi.MainActivity
import com.argus.agi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ArgusForegroundService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.argus.agi.START_SERVICE"
        const val ACTION_STOP = "com.argus.agi.STOP_SERVICE"
        const val ACTION_TOGGLE_PAUSE = "com.argus.agi.TOGGLE_PAUSE"

        fun startService(context: Context) {
            val intent = Intent(context, ArgusForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ArgusForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var updateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_PAUSE -> {
                ArgusApp.trainingManager.togglePauseTraining()
                updateNotification()
            }
            else -> {
                startForegroundCompat()
                startNotificationLoop()
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }

    private fun startNotificationLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                delay(4000)
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val trainingManager = ArgusApp.trainingManager
        val deviceGuard = ArgusApp.deviceGuard
        val hardware = ArgusApp.hardwareProfile.getHardwareInfo()

        val isPaused = trainingManager.isTrainingPaused.value
        val safety = deviceGuard.safetyState.value
        val currentItem = trainingManager.currentTrainingItem.value
        val latestLoss = trainingManager.lossHistory.value.lastOrNull()

        val title = "A.R.G.U.S Core: " + when {
            isPaused -> "Training paused"
            !safety.isSafeToTrain -> "Thermal and battery guard active"
            currentItem != null -> "Learning from ${currentItem.source}"
            else -> "Monitoring approved context sources"
        }

        val lossText = if (latestLoss != null) "Loss: ${String.format("%.4f", latestLoss)}" else "Loss: not available"
        val content = "$lossText | ${safety.batteryPct}% (${String.format("%.1f", safety.temperatureCelsius)}°C) | ${hardware.primaryAccelerator.substringBefore('+')}"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, ArgusForegroundService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeLabel = if (isPaused) "Resume training" else "Pause training"

        return NotificationCompat.Builder(this, ArgusApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_argus_logo)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(0, pauseResumeLabel, togglePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
