package com.argus.agi.platform.android.device

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

data class SystemHardwareInfo(
    val socManufacturer: String,
    val socModel: String,
    val hardwareBoard: String,
    val cpuCores: Int,
    val maxHeapMb: Long,
    val vulkanSupported: Boolean,
    val vulkanVersionCode: Int,
    val npuApuDetected: String,
    val primaryAccelerator: String,
    val isSnapdragon: Boolean,
    val isMediaTek: Boolean
)

class HardwareProfile(private val context: Context) {

    fun getHardwareInfo(): SystemHardwareInfo {
        val pm = context.packageManager

        // Read SoC info with Android 12+ API or fallback to cpuinfo
        var socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER
        } else {
            "Unknown"
        }
        var socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            Build.HARDWARE
        }

        if (socManufacturer.isBlank() || socManufacturer == "Unknown") {
            socManufacturer = detectManufacturerFromCpuInfo()
        }

        val board = Build.HARDWARE ?: "Generic"
        val isSnapdragon = socManufacturer.contains("Qualcomm", ignoreCase = true) ||
                board.contains("qcom", ignoreCase = true) ||
                board.contains("snapdragon", ignoreCase = true)
        val isMediaTek = socManufacturer.contains("MediaTek", ignoreCase = true) ||
                board.contains("mt", ignoreCase = true)

        // Vulkan support detection
        var vulkanSupported = false
        var vulkanVersionCode = 0
        if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
            vulkanSupported = true
            val pkgInfo = pm.systemAvailableFeatures.firstOrNull {
                it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION
            }
            vulkanVersionCode = pkgInfo?.version ?: 0
        }

        // NPU / APU Detection
        val npuApuDetected = when {
            isSnapdragon -> "Qualcomm Hexagon Vector Extensions (HVX / NPU)"
            isMediaTek -> "MediaTek NeuroPilot APU (AI Processing Unit)"
            socManufacturer.contains("Google", ignoreCase = true) -> "Google Tensor TPU"
            socManufacturer.contains("Samsung", ignoreCase = true) -> "Samsung Exynos NPU"
            else -> "On-Device Neural Engine (Int8/FP32 Core)"
        }

        val primaryAccelerator = when {
            vulkanSupported && (vulkanVersionCode >= 0x401000) -> {
                if (isSnapdragon) "Adreno GPU + Vulkan 1.2+ Acceleration"
                else if (isMediaTek) "Mali/Immortalis GPU + Vulkan Compute"
                else "Vulkan High-Performance Compute"
            }
            vulkanSupported -> "Vulkan Hardware Accelerated Compute"
            else -> "Multi-Threaded SIMD CPU Fallback (${Runtime.getRuntime().availableProcessors()} Cores)"
        }

        val maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val cpuCores = Runtime.getRuntime().availableProcessors()

        return SystemHardwareInfo(
            socManufacturer = socManufacturer.ifBlank { "Mobile ARMv8/v9" },
            socModel = socModel.ifBlank { board },
            hardwareBoard = board,
            cpuCores = cpuCores,
            maxHeapMb = maxHeapMb,
            vulkanSupported = vulkanSupported,
            vulkanVersionCode = vulkanVersionCode,
            npuApuDetected = npuApuDetected,
            primaryAccelerator = primaryAccelerator,
            isSnapdragon = isSnapdragon,
            isMediaTek = isMediaTek
        )
    }

    private fun detectManufacturerFromCpuInfo(): String {
        return try {
            val file = File("/proc/cpuinfo")
            if (file.exists()) {
                val lines = file.readLines()
                val hardwareLine = lines.firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
                if (hardwareLine != null) {
                    val value = hardwareLine.substringAfter(":").trim()
                    if (value.contains("qcom", ignoreCase = true) || value.contains("Qualcomm", ignoreCase = true)) {
                        return "Qualcomm"
                    }
                    if (value.contains("MT", ignoreCase = true) || value.contains("MediaTek", ignoreCase = true)) {
                        return "MediaTek"
                    }
                    return value
                }
            }
            Build.MANUFACTURER ?: "ARM Architecture"
        } catch (_: Exception) {
            Build.MANUFACTURER ?: "ARM Architecture"
        }
    }
}
