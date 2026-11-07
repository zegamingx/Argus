package com.argus.agi.application.learning

import com.argus.agi.core.training.CorpusImportPreview
import com.argus.agi.core.training.CorpusImportStager
import com.argus.agi.core.training.DatasetPurpose
import com.argus.agi.core.training.LocalCorpusIntegrityReport
import com.argus.agi.core.training.LocalCorpusStore
import com.argus.agi.core.training.LocalCorpusSummary

/**
 * Application-facing facade for local corpus preparation and revision storage.
 *
 * The service coordinates platform import staging with the corpus persistence
 * port. It does not grant corpus admission approval or start model training.
 */
class LocalCorpusImportService(
    private val stager: CorpusImportStager,
    private val store: LocalCorpusStore
) {

    suspend fun stageUserFiles(
        sourceUriReferences: List<String>,
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
        return stager.stageUserFiles(sourceUriReferences, purposes)
    }

    suspend fun stageBundledArgusId(
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
        return stager.stageBundledArgusId(purposes)
    }

    suspend fun stageBundledDigiTransId(
        purposes: Set<DatasetPurpose>
    ): CorpusImportPreview {
        return stager.stageBundledDigiTransId(purposes)
    }

    fun commit(stageId: String): LocalCorpusSummary {
        return store.commit(stageId)
    }

    fun verifyCommittedCorpusIntegrity(
        corpusId: String,
        revision: Int
    ): LocalCorpusIntegrityReport {
        return store.verifyCommittedCorpusIntegrity(corpusId, revision)
    }

    fun discard(stageId: String) {
        store.discard(stageId)
    }

    fun listCommitted(): List<LocalCorpusSummary> {
        return store.listCommitted()
    }
}
