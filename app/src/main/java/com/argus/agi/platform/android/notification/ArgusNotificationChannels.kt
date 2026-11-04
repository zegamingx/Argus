package com.argus.agi.platform.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val ARGUS_CORE_NOTIFICATION_CHANNEL_ID = "argus_core_service_channel"

fun createArgusNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel = NotificationChannel(
        ARGUS_CORE_NOTIFICATION_CHANNEL_ID,
        "A.R.G.U.S Neural Core & Perception",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Status of on-device learning and environmental perception."
        setShowBadge(false)
    }

    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}
