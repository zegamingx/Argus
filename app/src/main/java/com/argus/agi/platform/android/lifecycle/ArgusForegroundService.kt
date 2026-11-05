package com.argus.agi.platform.android.lifecycle

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.argus.agi.R
import com.argus.agi.application.runtime.ArgusRuntimeOwner
import com.argus.agi.platform.android.notification.ARGUS_CORE_NOTIFICATION_CHANNEL_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ArgusForegroundService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.argus.agi.START_SERVICE"
        const val ACTION_STOP = "com.argus.agi.STOP_SERVICE"

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

    private fun runtimeOrNull() =
        (application as? ArgusRuntimeOwner)?.argusRuntime

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
        serviceScope.cancel()
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
        val runtime = runtimeOrNull()
        val training = runtime?.training?.snapshot()
        val safety = runtime?.device?.snapshot()
        val primaryAccelerator = runtime?.hardware?.primaryAcceleratorLabel() ?: "Unknown"

        val isWorkflowAvailable = training?.isWorkflowAvailable ?: false
        val trainingStatus = training?.statusMessage
            ?: "Learning workflow status is unavailable."
        val batteryPercent = safety?.batteryPercent ?: 0
        val temperatureCelsius = safety?.temperatureCelsius ?: 0f

        val title = if (isWorkflowAvailable) {
            "A.R.G.U.S Core: Learning workflow available"
        } else {
            "A.R.G.U.S Core: Learning workflow unavailable"
        }
        val content = "$trainingStatus | $batteryPercent% (${String.format("%.1f", temperatureCelsius)}°C) | ${primaryAccelerator.substringBefore('+')}"

        val openAppPendingIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { launchIntent ->
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

        return NotificationCompat.Builder(this, ARGUS_CORE_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_argus_logo)
            .apply { if (openAppPendingIntent != null) setContentIntent(openAppPendingIntent) }
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
