package com.argus.agi.platform.android.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceSafetyState(
    val batteryPct: Int = 100,
    val isCharging: Boolean = true,
    val temperatureCelsius: Float = 30.0f,
    val isBatteryOptimizedIgnored: Boolean = false,
    val isSafeToTrain: Boolean = true,
    val statusMessage: String = "System safe"
)

class DeviceGuard(private val context: Context) {

    private val _safetyState = MutableStateFlow(DeviceSafetyState())
    val safetyState: StateFlow<DeviceSafetyState> = _safetyState.asStateFlow()

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            intent?.let { updateState(it) }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        if (initialIntent != null) {
            updateState(initialIntent)
        }
    }

    fun isSafeToTrain(): Boolean {
        refreshState()
        return _safetyState.value.isSafeToTrain
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun refreshState() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = ContextCompat.registerReceiver(
            context,
            null,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        if (batteryStatus != null) {
            updateState(batteryStatus)
        }
    }

    private fun updateState(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = if (tempTenths > 0) tempTenths / 10.0f else 30.0f

        val isIgnored = isIgnoringBatteryOptimizations()

        // Safety rules:
        // 1. Temperature must be < 42°C to prevent thermal throttling / battery wear
        // 2. Battery percentage must be >= 20% or device must be plugged in / charging
        val safeTemp = tempCelsius < 42.0f
        val safeBattery = isCharging || batteryPct >= 20
        val isSafe = safeTemp && safeBattery

        val statusMsg = when {
            !safeTemp -> "High temperature (${String.format("%.1f", tempCelsius)}°C > 42°C). Training paused for device safety."
            !safeBattery -> "Low battery ($batteryPct% < 20%). Waiting for charging before resuming training."
            else -> "Device safe (${batteryPct}%, ${String.format("%.1f", tempCelsius)}°C)."
        }

        _safetyState.value = DeviceSafetyState(
            batteryPct = batteryPct,
            isCharging = isCharging,
            temperatureCelsius = tempCelsius,
            isBatteryOptimizedIgnored = isIgnored,
            isSafeToTrain = isSafe,
            statusMessage = statusMsg
        )
    }
}
