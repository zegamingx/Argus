package com.argus.agi.feature.learning.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.argus.agi.ui.ArgusViewModel
import com.argus.agi.ui.design.ArgusCardBorder
import com.argus.agi.ui.design.ArgusCyanPrimary
import com.argus.agi.ui.design.ArgusError
import com.argus.agi.ui.design.ArgusSurface
import com.argus.agi.ui.design.ArgusSurfaceVariant
import com.argus.agi.ui.design.ArgusTextMuted
import com.argus.agi.ui.design.ArgusTextPrimary
import com.argus.agi.ui.design.ArgusTextSecondary
import java.util.Locale

@Composable
fun LocalDatasetImporterCard(viewModel: ArgusViewModel) {
    val learningState by viewModel.learningState.collectAsState()
    val importState = learningState.datasetImport
    val stagedPreview = importState.stagedPreview

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            viewModel.stageLocalDataset(selectedUris)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = ArgusSurface,
        border = BorderStroke(1.dp, ArgusCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LOCAL DATASET CORPUS",
                style = MaterialTheme.typography.titleMedium,
                color = ArgusCyanPrimary
            )
            Text(
                text = "Import your own corpus files or a ZIP archive. Data is parsed and stored on this device only.",
                style = MaterialTheme.typography.bodyMedium,
                color = ArgusTextSecondary
            )

            DatasetPurposeUi.entries.forEach { purpose ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = purpose in importState.selectedPurposes,
                        onCheckedChange = { enabled ->
                            viewModel.setDatasetPurpose(purpose, enabled)
                        },
                        enabled = !importState.isBusy &&
                            purpose != DatasetPurposeUi.ALIGNED_TOKENIZER_DEVELOPMENT
                    )
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = if (purpose == DatasetPurposeUi.ALIGNED_TOKENIZER_DEVELOPMENT) {
                                "Aligned tokenizer development (not supported by this importer yet)"
                            } else {
                                purpose.label
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ArgusTextPrimary
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::stageBundledArgusId,
                enabled = !importState.isBusy && importState.selectedPurposes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import ArgusID corpus (Indonesian TXT)")
            }

            Button(
                onClick = viewModel::stageBundledDigiTransId,
                enabled = !importState.isBusy && importState.selectedPurposes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import DigiTransID corpus")
            }

            OutlinedButton(
                onClick = {
                    documentPicker.launch(arrayOf("*/*"))
                },
                enabled = !importState.isBusy && importState.selectedPurposes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose other local dataset files")
            }

            if (importState.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = importState.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ArgusTextMuted
            )

            importState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = ArgusError
                )
            }

            if (stagedPreview != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = ArgusSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Import preview",
                            style = MaterialTheme.typography.titleSmall,
                            color = ArgusTextPrimary
                        )
                        PreviewMetric("Selected files", stagedPreview.selectedSourceCount.toString())
                        PreviewMetric("Parsed documents", stagedPreview.parsedDocumentCount.toString())
                        PreviewMetric("Accepted records", stagedPreview.acceptedRecordCount.toString())
                        PreviewMetric("Duplicate records", stagedPreview.duplicateRecordCount.toString())
                        PreviewMetric("Rejected records", stagedPreview.rejectedRecordCount.toString())
                        PreviewMetric("Skipped files", stagedPreview.skippedFileCount.toString())
                        PreviewMetric("Expanded text", formatImportBytes(stagedPreview.importedBytes))
                        PreviewMetric(
                            "Train / validation / test",
                            "${stagedPreview.trainRecordCount} / ${stagedPreview.validationRecordCount} / ${stagedPreview.testRecordCount}"
                        )

                        if (stagedPreview.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${stagedPreview.warnings.size} import warning(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = ArgusTextSecondary
                            )
                            stagedPreview.warnings.take(3).forEach { warning ->
                                Text(
                                    text = "• $warning",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArgusTextMuted
                                )
                            }
                            if (stagedPreview.warnings.size > 3) {
                                Text(
                                    text = "Additional warnings are saved in the local manifest.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArgusTextMuted
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = viewModel::commitStagedLocalDataset,
                                enabled = !importState.isBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Commit corpus")
                            }
                            OutlinedButton(
                                onClick = viewModel::discardStagedLocalDataset,
                                enabled = !importState.isBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Discard")
                            }
                        }
                    }
                }
            }

            if (importState.committedCorpora.isNotEmpty()) {
                Text(
                    text = "LOCAL CORPUS REVISIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = ArgusCyanPrimary
                )
                importState.committedCorpora.take(5).forEach { corpus ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = ArgusSurfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Corpus ${corpus.corpusId.take(8)} · revision ${corpus.revision}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ArgusTextPrimary
                            )
                            Text(
                                text = "${corpus.recordCount} records · train ${corpus.trainRecordCount} · validation ${corpus.validationRecordCount} · test ${corpus.testRecordCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArgusTextSecondary
                            )
                            Text(
                                text = when (corpus.lastAdmissionDecision) {
                                    "PENDING" -> "Last admission decision: Pending review"
                                    "APPROVED" -> "Last admission decision: Approved"
                                    "REJECTED" -> "Last admission decision: Rejected"
                                    "LEGACY_UNVERIFIED" -> "Legacy corpus: re-import for manifest verification"
                                    else -> "Admission state unavailable"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusTextMuted
                            )
                            Text(
                                text = corpus.purposeLabels.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = ArgusTextMuted
                            )
                        }
                    }
                }
                if (importState.committedCorpora.size > 5) {
                    Text(
                        text = "${importState.committedCorpora.size - 5} more local corpus revision(s) stored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArgusTextMuted
                    )
                }
            }

            Text(
                text = "Import prepares data only. It does not start training or modify model weights.",
                style = MaterialTheme.typography.labelSmall,
                color = ArgusTextMuted
            )
        }
    }
}

@Composable
private fun PreviewMetric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ArgusTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = ArgusTextPrimary
        )
    }
}

private fun formatImportBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L -> String.format(Locale.US, "%.2f MiB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(Locale.US, "%.1f KiB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
