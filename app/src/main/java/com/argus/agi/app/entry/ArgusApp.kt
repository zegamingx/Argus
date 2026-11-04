package com.argus.agi.app.entry

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.argus.agi.model.ArgusNeuralEngine
import com.argus.agi.model.TrainingManager
import com.argus.agi.model.WorldModel
import com.argus.agi.platform.android.device.DeviceGuard
import com.argus.agi.platform.android.device.HardwareProfile

class ArgusApp : Application() {

    companion object {
        const val CHANNEL_ID = "argus_core_service_channel"
        lateinit var instance: ArgusApp
            private set

        val neuralEngine by lazy { ArgusNeuralEngine(instance) }
        val worldModel by lazy { WorldModel(instance) }
        val deviceGuard by lazy { DeviceGuard(instance) }
        val hardwareProfile by lazy { HardwareProfile(instance) }
        val trainingManager by lazy { TrainingManager(instance, neuralEngine, deviceGuard, worldModel) }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "A.R.G.U.S Neural Core & Perception",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status of on-device learning and environmental perception."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
