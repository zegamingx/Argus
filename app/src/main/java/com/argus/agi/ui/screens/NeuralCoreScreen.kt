package com.argus.agi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.argus.agi.model.TrainingData
import com.argus.agi.ui.ArgusViewModel
import com.argus.agi.ui.theme.ArgusBlueAccent
import com.argus.agi.ui.theme.ArgusCardBorder
import com.argus.agi.ui.theme.ArgusCyanGlow
import com.argus.agi.ui.theme.ArgusCyanPrimary
import com.argus.agi.ui.theme.ArgusDarkBg
import com.argus.agi.ui.theme.ArgusError
import com.argus.agi.ui.theme.ArgusSuccess
import com.argus.agi.ui.theme.ArgusSurface
import com.argus.agi.ui.theme.ArgusSurfaceVariant
import com.argus.agi.ui.theme.ArgusTextMuted
import com.argus.agi.ui.theme.ArgusTextPrimary
import com.argus.agi.ui.theme.ArgusTextSecondary
import com.argus.agi.ui.theme.ArgusWarning
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NeuralCoreScreen(viewModel: ArgusViewModel) {
    val totalParams = viewModel.getTotalParameters()
    val adapterBytes = viewModel.getAdapterSizeBytes()
    val isPaused by viewModel.isTrainingPaused.collectAsState()
    val safetyState by viewModel.safetyState.collectAsState()
    val currentItem by viewModel.currentTrainingItem.collectAsState()
    val nextQueue by viewModel.nextQueue.collectAsState()
    val completedQueue by viewModel.completedQueue.collectAsState()
    val lossHistory by viewModel.lossHistory.collectAsState()
    val benchmark by viewModel.benchmarkState.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()
    val hardwareInfo by viewModel.hardwareInfo.collectAsState()
    val totalSteps by viewModel.totalStepsExecuted.collectAsState()

    var isSpecExpanded by remember { mutableStateOf(true) }
    var isTrainingExpanded by remember { mutableStateOf(true) }
    var isLossGraphExpanded by remember { mutableStateOf(true) }
    var isBenchmarkExpanded by remember { mutableStateOf(true) }
    var isQueueExpanded by remember { mutableStateOf(true) }

    val formattedParams = NumberFormat.getNumberInstance(Locale.US).format(totalParams)
    val adapterSizeKb = adapterBytes / 1024.0
    val adapterSizeStr = if (adapterBytes > 0) String.format(Locale.US, "%.2f KB", adapterSizeKb) else "belum ada"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ArgusDarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        item {
            Column {
                Text(
                    text = "NEURAL CORE & TRAINING ENGINE",
                    style = MaterialTheme.typography.titleLarge,
                    color = ArgusCyanPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "100% On-Device Neural Backpropagation | Zero Hardcode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArgusTextSecondary
                )
            }
        }

        // 1. Model Architecture & Parameter Card (Collapsible)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ArgusSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ArgusCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isSpecExpanded = !isSpecExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Core",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Spesifikasi Model Aktif",
                                style = MaterialTheme.typography.titleMedium,
                                color = ArgusTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ArgusCyanPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Micro-Transformer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArgusCyanPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isSpecExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isSpecExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatMetricBox(
                                    label = "JUMLAH PARAMETER",
                                    value = formattedParams,
                                    unit = "Weight Elements",
                                    valueColor = ArgusCyanPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                StatMetricBox(
                                    label = "UKURAN LORA ADAPTER",
                                    value = adapterSizeStr,
                                    unit = "Rank 4 (FP32)",
                                    valueColor = ArgusCyanGlow,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Arsitektur: D_Model=64 | Heads=4 | D_FF=128 | Pos=128 | LoRA=Rank 4",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusTextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Backend Komputasi: ${hardwareInfo.primaryAccelerator}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusCyanGlow,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 2. Training Control & Battery/Thermal Guard Status (Collapsible)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ArgusSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ArgusCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isTrainingExpanded = !isTrainingExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Autonomous Training Loop",
                                style = MaterialTheme.typography.titleMedium,
                                color = ArgusTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total Step Selesai: $totalSteps",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.toggleTrainingPause() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPaused) ArgusSuccess else ArgusWarning,
                                    contentColor = ArgusDarkBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("toggle_training_button")
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPaused) "Lanjutkan" else "Jeda",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isTrainingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isTrainingExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            // Safety Guard Pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ArgusSurfaceVariant)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (safetyState.isSafeToTrain) ArgusSuccess else ArgusError)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Battery & Thermal Guard",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ArgusTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = safetyState.statusMessage,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = if (safetyState.isSafeToTrain) ArgusTextSecondary else ArgusError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Live Real Loss History Graph (Collapsible)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ArgusSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ArgusCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isLossGraphExpanded = !isLossGraphExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grafik Real Cross-Entropy Loss",
                            style = MaterialTheme.typography.titleMedium,
                            color = ArgusTextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val currentLossStr = lossHistory.lastOrNull()?.let { String.format(Locale.US, "%.4f", it) } ?: "belum ada"
                            Text(
                                text = "Loss: $currentLossStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusCyanPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isLossGraphExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isLossGraphExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            if (lossHistory.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(ArgusSurfaceVariant, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Loss belum ada. Data training sedang diakumulasi dari layar.",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = ArgusTextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                RealLossCanvas(lossHistory = lossHistory)
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Benchmark Section (Collapsible)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ArgusSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ArgusCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isBenchmarkExpanded = !isBenchmarkExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Benchmark",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "On-Device Benchmark",
                                style = MaterialTheme.typography.titleMedium,
                                color = ArgusTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.runBenchmark() },
                                enabled = !isBenchmarking,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ArgusCyanPrimary,
                                    contentColor = ArgusDarkBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("run_benchmark_button")
                            ) {
                                if (isBenchmarking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = ArgusDarkBg,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Uji",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isBenchmarkExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isBenchmarkExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val tpsStr = benchmark?.let { String.format(Locale.US, "%.1f TPS", it.tokensPerSecond) } ?: "belum ada"
                                val latStr = benchmark?.let { String.format(Locale.US, "%.2f ms/tok", it.latencyMsPerToken) } ?: "belum ada"
                                val memStr = benchmark?.let { String.format(Locale.US, "%.1f MB", it.memoryUsageMb) } ?: "belum ada"

                                StatMetricBox(
                                    label = "KECEPATAN",
                                    value = tpsStr,
                                    unit = "Tokens / Detik",
                                    valueColor = ArgusCyanPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatMetricBox(
                                    label = "LATENSI",
                                    value = latStr,
                                    unit = "Per Token",
                                    valueColor = ArgusCyanGlow,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatMetricBox(
                                    label = "MEMORI",
                                    value = memStr,
                                    unit = "Heap Terpakai",
                                    valueColor = ArgusBlueAccent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Live Training Queue Inspector (Collapsible Header)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isQueueExpanded = !isQueueExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANTREAN & HISTORI TRAINING",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArgusCyanPrimary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isQueueExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Antrean",
                    tint = ArgusCyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isQueueExpanded) {
            // 1. DATA SEDANG DITRAINING
            item {
                QueueSectionHeader(title = "1. Data Yang Sedang Ditraining", count = if (currentItem != null) 1 else 0)
                Spacer(modifier = Modifier.height(6.dp))

                if (currentItem != null) {
                    ActiveTrainingCard(currentItem!!)
                } else {
                    EmptyQueueCard("Tidak ada data yang sedang aktif ditraining (Idle / Menunggu Stimulus)")
                }
            }

            // 2. DATA SELANJUTNYA DITRAINING
            item {
                QueueSectionHeader(
                    title = "2. Data Selanjutnya Ditraining",
                    count = nextQueue.size,
                    onClear = { viewModel.clearTrainingQueue() }
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (nextQueue.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        nextQueue.take(4).forEach { item ->
                            QueuedItemCard(item)
                        }
                    }
                } else {
                    EmptyQueueCard("Antrean kosong. Buka aplikasi lain untuk membaca layar atau kirim pesan/dokumen di chat.")
                }
            }

            // 3. DATA SUDAH DITRAINING
            item {
                QueueSectionHeader(
                    title = "3. Data Sudah Ditraining (Histori)",
                    count = completedQueue.size,
                    onClear = { viewModel.clearCompletedTraining() }
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (completedQueue.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        completedQueue.take(5).forEach { item ->
                            CompletedItemCard(item)
                        }
                    }
                } else {
                    EmptyQueueCard("Histori training belum ada.")
                }
            }
        }
    }
}

@Composable
fun RealLossCanvas(lossHistory: List<Float>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(ArgusSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (lossHistory.size < 2) return@Canvas

            val maxLoss = (lossHistory.maxOrNull() ?: 1f).coerceAtLeast(0.1f)
            val minLoss = (lossHistory.minOrNull() ?: 0f).coerceAtLeast(0f)
            val range = (maxLoss - minLoss).coerceAtLeast(0.01f)

            val width = size.width
            val height = size.height
            val stepX = width / (lossHistory.size - 1)

            val path = Path()
            lossHistory.forEachIndexed { i, loss ->
                val x = i * stepX
                val normalizedY = ((loss - minLoss) / range).coerceIn(0f, 1f)
                val y = height - (normalizedY * (height - 20f) + 10f)

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = ArgusCyanPrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw current point
            val lastX = (lossHistory.size - 1) * stepX
            val lastNormalizedY = ((lossHistory.last() - minLoss) / range).coerceIn(0f, 1f)
            val lastY = height - (lastNormalizedY * (height - 20f) + 10f)

            drawCircle(
                color = ArgusCyanGlow,
                radius = 5.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}

@Composable
fun StatMetricBox(
    label: String,
    value: String,
    unit: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ArgusSurfaceVariant)
            .border(1.dp, ArgusCardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ArgusTextMuted,
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = ArgusTextSecondary,
            fontSize = 9.sp
        )
    }
}

@Composable
fun QueueSectionHeader(
    title: String,
    count: Int,
    onClear: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            color = ArgusCyanGlow,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ArgusSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count item",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArgusCyanPrimary
                )
            }
            if (onClear != null && count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bersihkan",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ArgusTextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onClear() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveTrainingCard(item: TrainingData) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ArgusCyanPrimary.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, ArgusCyanPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = ArgusCyanPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AKTIF DILATIH: ${item.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArgusCyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                color = ArgusTextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.content.take(160) + if (item.content.length > 160) "..." else "",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = ArgusTextSecondary
            )
        }
    }
}

@Composable
fun QueuedItemCard(item: TrainingData) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ArgusSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ArgusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = "Queued",
                tint = ArgusWarning,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
                    color = ArgusTextPrimary
                )
                Text(
                    text = "Sumber: ${item.source} (${item.content.length} karakter)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArgusTextMuted
                )
            }
        }
    }
}

@Composable
fun CompletedItemCard(item: TrainingData) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ArgusSurfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ArgusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = ArgusSuccess,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
                    color = ArgusTextPrimary
                )
                val lossStr = item.finalLoss?.let { String.format(Locale.US, "Loss: %.4f", it) } ?: "Loss: belum ada"
                Text(
                    text = "$lossStr | Waktu: ${item.stepDurationMs}ms | ${item.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArgusTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun EmptyQueueCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ArgusSurfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
            color = ArgusTextMuted,
            fontFamily = FontFamily.Monospace
        )
    }
}
