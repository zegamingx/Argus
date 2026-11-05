package com.argus.agi.feature.settings.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.argus.agi.ui.ArgusViewModel
import com.argus.agi.ui.design.ArgusBlueAccent
import com.argus.agi.ui.design.ArgusCardBorder
import com.argus.agi.ui.design.ArgusCyanGlow
import com.argus.agi.ui.design.ArgusCyanPrimary
import com.argus.agi.ui.design.ArgusDarkBg
import com.argus.agi.ui.design.ArgusSuccess
import com.argus.agi.ui.design.ArgusSurface
import com.argus.agi.ui.design.ArgusSurfaceVariant
import com.argus.agi.ui.design.ArgusTextMuted
import com.argus.agi.ui.design.ArgusTextPrimary
import com.argus.agi.ui.design.ArgusTextSecondary
import com.argus.agi.ui.design.ArgusWarning

@Composable
fun SkillsAndSettingsScreen(viewModel: ArgusViewModel) {
    val context = LocalContext.current
    val settingsState by viewModel.settingsState.collectAsState()
    val installedApps = settingsState.installedApps
    val worldEntities = settingsState.worldEntities
    val searchResults = settingsState.searchResults

    var isBatteryExpanded by remember { mutableStateOf(true) }
    var isSkillsExpanded by remember { mutableStateOf(true) }
    var isExcludeAppsExpanded by remember { mutableStateOf(true) }
    var isWorldModelExpanded by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var actionClickQuery by remember { mutableStateOf("") }
    var actionClickResult by remember { mutableStateOf<String?>(null) }
    var appFilterText by remember { mutableStateOf("") }

    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoringOpt = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ArgusDarkBg)
            .imePadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "SKILL AGENTS & PRIVACY",
                    style = MaterialTheme.typography.titleLarge,
                    color = ArgusCyanPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure app exclusions, battery policy, and action execution",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArgusTextSecondary
                )
            }
        }

        // 1. Battery Optimization Card (Collapsible)
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
                            .clickable { isBatteryExpanded = !isBatteryExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isIgnoringOpt) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                                contentDescription = "Battery",
                                tint = if (isIgnoringOpt) ArgusSuccess else ArgusWarning,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Battery & Background Optimization",
                                style = MaterialTheme.typography.titleMedium,
                                color = ArgusTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isIgnoringOpt) ArgusSuccess.copy(alpha = 0.15f) else ArgusWarning.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isIgnoringOpt) "EXEMPT" else "RESTRICTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isIgnoringOpt) ArgusSuccess else ArgusWarning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isBatteryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isBatteryExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "To help approved on-device learning and perception work continue in the background, you can request an exemption from battery optimization. Android may still restrict background execution.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusTextSecondary
                            )

                            if (!isIgnoringOpt) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(fallbackIntent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ArgusCyanPrimary,
                                        contentColor = ArgusDarkBg
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Request battery optimization exemption",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Skills Catalog & Tester (Collapsible)
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
                            .clickable { isSkillsExpanded = !isSkillsExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = "Skills",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "A.R.G.U.S Multi-Skill Engine",
                                style = MaterialTheme.typography.titleMedium,
                                color = ArgusTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = if (isSkillsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = ArgusCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isSkillsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            SkillBadge(title = "1. Web Search & Factual Retriever", desc = "Retrieves web pages directly over HTTP without using an external AI service.")
                            SkillBadge(title = "2. Autonomous Screen Perception", desc = "Extracts text from interactive application windows while the service is enabled.")
                            SkillBadge(title = "3. Action Agent Clicker", desc = "Finds and clicks screen text through Android accessibility nodes.")
                            SkillBadge(title = "4. Mathematical & Logic Reasoner", desc = "Evaluates symbolic operations and arithmetic using deterministic logic.")
                            SkillBadge(title = "5. World Model Memory", desc = "Stores entity relationships, timestamps, and application context as episodic records.")
                            SkillBadge(title = "6. Continuous On-Device Learner", desc = "Updates model weights and LoRA adapters locally.")

                            Spacer(modifier = Modifier.height(14.dp))

                            // Direct Web Search Tester
                            Text(
                                text = "Test On-Device Web Search:",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusCyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Web query (e.g. on-device AI)", color = ArgusTextMuted, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ArgusCyanPrimary,
                                        unfocusedBorderColor = ArgusCardBorder,
                                        focusedTextColor = ArgusTextPrimary,
                                        unfocusedTextColor = ArgusTextPrimary,
                                        focusedContainerColor = ArgusSurfaceVariant,
                                        unfocusedContainerColor = ArgusSurfaceVariant
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            viewModel.searchWebDirectly(searchQuery)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ArgusCyanPrimary,
                                        contentColor = ArgusDarkBg
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Search", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (searchResults.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    searchResults.take(3).forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ArgusDarkBg)
                                                .padding(8.dp)
                                        ) {
                                            Column {
                                                Text(text = item.title, style = MaterialTheme.typography.labelSmall, color = ArgusCyanPrimary, fontWeight = FontWeight.Bold)
                                                Text(text = item.snippet, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = ArgusTextSecondary)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Agent Click Tester
                            Text(
                                text = "Test Action Agent (Screen Text Click):",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusCyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = actionClickQuery,
                                    onValueChange = { actionClickQuery = it },
                                    placeholder = { Text("Screen button text", color = ArgusTextMuted, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ArgusCyanPrimary,
                                        unfocusedBorderColor = ArgusCardBorder,
                                        focusedTextColor = ArgusTextPrimary,
                                        unfocusedTextColor = ArgusTextPrimary,
                                        focusedContainerColor = ArgusSurfaceVariant,
                                        unfocusedContainerColor = ArgusSurfaceVariant
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (actionClickQuery.isNotBlank()) {
                                            val success = viewModel.executeActionClick(actionClickQuery)
                                            actionClickResult = if (success) "Success: Click action sent to '${actionClickQuery}'." else "Failed: Element '${actionClickQuery}' was not found on the active screen."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ArgusBlueAccent,
                                        contentColor = ArgusDarkBg
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Klik", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (actionClickResult != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = actionClickResult!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (actionClickResult!!.startsWith("Success")) ArgusSuccess else ArgusWarning,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Exclude App Management Section (Collapsible)
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
                            .clickable { isExcludeAppsExpanded = !isExcludeAppsExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Exclude",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select Excluded Apps",
                                style = MaterialTheme.typography.titleMedium,
                                color = ArgusTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val excludedCount = installedApps.count { it.isExcluded }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ArgusCyanPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$excludedCount protected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArgusCyanPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isExcludeAppsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = ArgusCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isExcludeAppsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Selected apps are excluded from screen perception (including banking, password managers, and private messaging apps) to reduce exposure of sensitive information.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusTextSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = appFilterText,
                                onValueChange = { appFilterText = it },
                                placeholder = { Text("Search app names...", color = ArgusTextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ArgusCyanPrimary,
                                    unfocusedBorderColor = ArgusCardBorder,
                                    focusedTextColor = ArgusTextPrimary,
                                    unfocusedTextColor = ArgusTextPrimary,
                                    focusedContainerColor = ArgusSurfaceVariant,
                                    unfocusedContainerColor = ArgusSurfaceVariant
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val filteredApps = installedApps.filter {
                                it.appName.contains(appFilterText, ignoreCase = true) ||
                                        it.packageName.contains(appFilterText, ignoreCase = true)
                            }.take(15)

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredApps.forEach { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ArgusSurfaceVariant)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.appName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp),
                                                color = ArgusTextPrimary
                                            )
                                            Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ArgusTextMuted
                                            )
                                        }

                                        Checkbox(
                                            checked = app.isExcluded,
                                            onCheckedChange = { viewModel.toggleAppExclusion(app.packageName) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = ArgusCyanPrimary,
                                                uncheckedColor = ArgusCardBorder,
                                                checkmarkColor = ArgusDarkBg
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. World Model Episodic Memory Card (Collapsible)
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
                            .clickable { isWorldModelExpanded = !isWorldModelExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "World Model & Episodic Memory Graph",
                            style = MaterialTheme.typography.titleMedium,
                            color = ArgusTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (isWorldModelExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = ArgusCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isWorldModelExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "A real-time representation of contextual information.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusTextSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                worldEntities.take(6).forEach { entity ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ArgusSurfaceVariant)
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = entity.key,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ArgusCyanPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = entity.source,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ArgusTextMuted
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = entity.value,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                                color = ArgusTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkillBadge(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = ArgusTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
            color = ArgusTextMuted
        )
    }
}
