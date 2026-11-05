package com.argus.agi.app.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.argus.agi.R
import com.argus.agi.app.bootstrap.ArgusStartupStage
import com.argus.agi.app.bootstrap.ArgusStartupUiState

@Composable
fun ArgusBootScreen(
    state: ArgusStartupUiState,
    onRetry: () -> Unit
) {
    val statusTitle: String
    val statusDescription: String

    when (state.stage) {
        ArgusStartupStage.INSPECTING_DEVICE -> {
            statusTitle = "CHECKING DEVICE PROFILE"
            statusDescription = "Reading the Android hardware profile."
        }

        ArgusStartupStage.PREPARING_RUNTIME -> {
            statusTitle = "PREPARING LOCAL RUNTIME"
            statusDescription = "Initializing the current experimental on-device neural runtime."
        }

        ArgusStartupStage.RUNTIME_PREPARED -> {
            statusTitle = "RUNTIME PREPARED"
            statusDescription = "Opening the Argus workspace."
        }

        ArgusStartupStage.FAILED -> {
            statusTitle = "STARTUP INTERRUPTED"
            statusDescription = state.errorMessage
                ?: "The local runtime could not be prepared."
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "A.R.G.U.S",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "LOCAL AI SYSTEM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(168.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isPreparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(150.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 2.dp
                        )
                    }

                    Surface(
                        modifier = Modifier.size(104.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_argus_logo),
                            contentDescription = "Argus",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(28.dp))

                Text(
                    text = "Argus",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "One Origin. Independent Evolution.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.size(42.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.size(9.dp))

                    Text(
                        text = statusDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    state.deviceModel?.let { model ->
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Device profile: $model",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.stage == ArgusStartupStage.FAILED) {
                        Spacer(modifier = Modifier.size(20.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry startup")
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "PROTOTYPE BUILD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Genesis generation storage and background learning are not available in this prototype.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
