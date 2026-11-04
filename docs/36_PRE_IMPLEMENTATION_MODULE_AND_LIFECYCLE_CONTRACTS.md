# Pre-Implementation Module and Lifecycle Contracts

**Status:** Proposed architecture-neutral contract baseline; architecture-specific bindings remain open  
**Review date:** 2026-11-04  
**Scope:** Documentation only. This document authorizes no source changes, build, test, workflow execution, device experiment or artifact publication.  
**Authority:** Logical module/API boundaries, lifecycle state machines, event/error contracts, storage entities, concurrency rules and cross-system sequences. Exact neural architecture, tokenizer, runtime providers, numeric budgets and product-language scope remain governed by their research documents and ADRs.

## 1. Purpose and design boundary

Document 02 identifies module/API contracts, error taxonomy, event/message schema, model lifecycle, concurrency rules, data schema, deployment diagrams and sequence diagrams as required pre-implementation outputs. This document supplies the first architecture-neutral version of those contracts.

The contracts define **what each subsystem owns and guarantees**, not how its implementation is written. They are independent of a specific Transformer/SSM/RWKV/hybrid selection, tokenizer family, Kotlin package layout, native training framework or accelerator.

The following labels are used:

- **DESIGN REQUIREMENT:** required behavior for a conforming implementation.
- **CONTRACT PLACEHOLDER:** type or interface shape is defined, but architecture-specific fields remain to be supplied.
- **IMPLEMENTATION BINDING OPEN:** exact module/package/library/runtime is not yet selected.
- **POST-IMPLEMENTATION VERIFICATION:** behavior must be proved by implementation tests; this document does not claim it is verified.

No interface below should be read as a current class or API in the prototype. Kotlin-like signatures are illustrative contract notation, not compilable source.

## 2. System invariants

Every implementation must preserve these invariants:

1. **Instance independence:** every installation owns its instance ID, model generations, neural weights, optimizer state, memory, source registry, acquisition jobs and personal data.
2. **Local origin:** the initial neural parameters are initialized locally. No external pretrained foundation weights are required or silently substituted.
3. **Genesis-before-learning:** no training job may start until a complete initial generation is durably committed and APPLICATION_READY has been published.
4. **Immutable active generation:** inference reads an immutable active generation. Training writes an isolated candidate generation; it never mutates the active generation in place.
5. **Exact identity binding:** inference, evaluation, dataset shards, training sessions and checkpoints identify the exact architecture/configuration and tokenizer fingerprints they consume.
6. **Separate learning provenance:** general-corpus and personal-learning candidates remain distinguishable through storage, permission, evaluation, retention and deletion.
7. **Retrieval is not training:** storing or retrieving a knowledge record does not grant permission to include it in a neural-training corpus.
8. **Fail closed:** missing permission, invalid identity, checksum mismatch, unsupported operator, corrupt checkpoint or unknown required policy state cannot be treated as success.
9. **Bounded execution:** all long-running operations have cancellation, resource admission, durable state and visible status.
10. **No false capability claims:** constructed, initialized, training, trained, evaluated, promoted and device-verified are different facts.

## 3. Logical module boundaries

These are logical ownership boundaries. They may map to Gradle modules, Kotlin packages or a combination after implementation planning; no one-to-one Gradle module structure is mandated.

| Logical module | Owns | Must not own / must not bypass |
|---|---|---|
| Experience/UI | User controls, consent/review surfaces, status presentation, user-visible errors | Direct tensor mutation, connector permission decisions, background worker internals |
| Application orchestrator | Use-case coordination, readiness, job dispatch, cancellation and app lifecycle | Model mathematics, source licensing decisions or direct storage-file mutation |
| Trust and policy | Permission scopes, policy evaluation, approval/revocation, network/data rules and fail-closed decisions | Model inference or source fetching implementation |
| Device capability service | Versioned stable capability evidence and confidence/unknown states; exposes a separate live runtime-condition snapshot | Guessing hardware execution from chipset strings or treating transient conditions as permanent hardware capability |
| Genesis coordinator | Initial profile admission, model construction orchestration, local initialization and first-generation transaction | Corpus pretraining on the startup critical path |
| Model-generation repository | Immutable manifests/tensors, generation state, active pointer, candidate isolation, validation and rollback | Training mathematics or implicit model selection |
| Tokenizer service | Versioned tokenizer load, deterministic encode/decode and fingerprint checks | Runtime vocabulary registration or changing token IDs in an active generation |
| Inference service | Read-only model snapshot, request execution, cancellation and result provenance | Updating weights or optimizer state |
| Learning coordinator | Session creation, training admission, candidate lifecycle, pause/resume, checkpoints and promotion | Direct ingestion of unapproved raw input |
| Training engine/backend | Forward, objective, backward, optimizer update and numerical status for a declared architecture contract | Source rights, personal consent, generation promotion or user-facing lifecycle policy |
| Corpus and knowledge store | Source/item provenance, canonical content where retention permits, indexes and immutable dataset manifests | Treating retrieval eligibility as training eligibility |
| Acquisition/connectors | Approved network fetch, parsing and connector-specific rate/response behavior | Granting themselves source permission or selecting training eligibility |
| Evaluation/promotion | General and personal evaluation, regression gates and promotion decision record | Mutating an active generation before a passing validation transaction |
| Event/diagnostic service | Typed lifecycle events and sanitized operational diagnostics | Raw private text, credentials, prompts or corpus contents in generic event payloads |

### 3.1 Dependency direction

Dependencies must point toward contracts, not concrete platform implementations:

- UI → application use cases.
- Application orchestrator → trust policy, device capability, Genesis, repositories and coordinators.
- Genesis → capability profile, architecture registry, initializer, tokenizer manifest and generation repository.
- Inference/training → read-only or candidate model snapshots plus declared backend interfaces.
- Acquisition → source registry/policy, connector interfaces, provenance store and resource admission.
- Promotion → evaluation report, candidate manifest and generation repository.

The trust/policy service is consulted by the application orchestrator and by each privileged boundary; it is not a UI-only check. Connectors and model backends cannot override it.

Circular ownership is prohibited. The training engine does not decide whether a dataset is legally eligible, and a connector does not decide whether fetched text can be retained or trained on.

## 4. Cross-module data contracts

The following are minimum logical records. A concrete serialization (Proto, DataStore, Room, flatbuffer or custom binary) is not selected here.

| Record | Required identity and content | Integrity / lifecycle requirement |
|---|---|---|
| InstanceManifest | instance ID, schema version, creation time, local storage namespace, initial policy version | Created once per installation; never reused across instances |
| DeviceCapabilityProfile | profile ID, Android build/API, ABI/ISA, memory class and measured baseline/headroom evidence, CPU/GPU/NPU provider/runtime/driver/operator evidence, tokenizer-training capability evidence, supported telemetry availability, confidence and unknown fields | Timestamped, source-labelled and versioned; unknown is not converted to supported; contains stable capability evidence rather than a live workload snapshot |
| RuntimeConditionProfile | sample ID/time, current available memory/pressure, OS thermal status, battery/charging/power-saver state, current storage reserve, foreground/background and scheduler constraints | Short-lived observation with source/confidence; re-read for workload admission and never treated as a permanent hardware capability |
| ModelConfig | architecture family/version, graph/module config, dimensions, tensor dtypes/layouts, context/state shape, precision, selected configuration-profile ID and backend constraints | Canonical fingerprint; unresolved architecture fields remain placeholders until ADR selection |
| TokenizerManifest | tokenizer ID/revision, profile ID, preparation mode (precomputed/local-trained/algorithmic), algorithm/trainer/runtime versions, normalization/pre-tokenizer/decoder, vocabulary and special IDs, artifact digest, trainer configuration, seed-corpus manifest digest and provenance/license | Immutable within one generation; referenced by digest; no private content is embedded in the manifest |
| ModelGenerationManifest | instance ID, generation ID, parent generation, ModelConfig fingerprint, TokenizerManifest digest, initialization policy/version/seed metadata, tensor inventory, checksums, schema/storage versions | Immutable after commit; complete tensor inventory and digest verified before ACTIVE |
| TrainingSessionManifest | session ID, target candidate generation, parent digest, objective/loss version, trainable parameter groups, optimizer/schedule, precision, dataset manifest digest, sampler/cursor policy, accumulation/update semantics, RNG policy, resource profile | Immutable session definition; changes to mathematical semantics require a new session/revision |
| CheckpointManifest | checkpoint ID, session ID, candidate generation, parent digest, model/optimizer/scheduler/scaler/RNG state, data cursor/sampler, committed update sequence, checksum and storage version | Atomic transaction; exact-resume validation before use |
| SourceRegistryEntry | source ID/version, endpoint/connector type, jurisdiction/region, source owner, terms/license evidence, allowed operation scopes, rate limits, review date and status | Default NOT-ACTIVATED; operation-specific permission |
| KnowledgeRecord | record ID, source/item revision, canonical URI or stable source identity, retrieved time, content digest, language/domain, rights state, attribution, retention scope, extraction version and sensitivity | Retrieval/retention rights recorded separately from training rights |
| TrainingCandidateManifest | candidate ID, general/personal partition, exact record IDs/revisions, rights/consent approvals, preprocessing/split/dedup version, tokenizer fingerprint if tokenized, checksum | Immutable after approval; revoked/held/deleted inputs excluded |
| ConsentGrant | grant ID, subject/instance, purpose/scope, source categories, processing/retention permissions, policy text/version, timestamp and revocation state | Scoped, revocable and auditable; OS permission alone is not a consent grant |
| AcquisitionJob | job ID, source ID, operation, request fingerprint, resource/network budget, state, retry policy, cursor and provenance refs | Idempotent; bounded retries and durable transitions |
| EvaluationReport | candidate generation, parent, evaluation set revision, metric definitions, general/personal results, regression checks, resource evidence and disposition | Immutable, tied to exact candidate digest |
| AuditEvent | event envelope and policy/lifecycle facts, actor/operation, target IDs, timestamp and outcome | Append-only within retention policy; no raw private payloads |

No document may claim a complete database schema until exact fields, indexes, encryption/key ownership, migrations, deletion cascades and retention/backup policy are specified for the chosen storage engine.

## 5. Core interface contracts

The signatures are conceptual. Exact Kotlin types, suspendability, callback style and dependency-injection mechanism remain an implementation binding.

### 5.1 Device capability and configuration

~~~text
CapabilityProvider.inspect(requestedWorkload) -> DeviceCapabilityProfile
RuntimeConditionProvider.inspect() -> RuntimeConditionProfile

ConfigurationSelector.select(
    capabilityProfile,
    runtimeConditionProfile,
    researchedConfigurationCandidates,
    resourceBudget
) -> ConfigurationDecision
~~~

Requirements:
- Each capability claim carries evidence source, version, timestamp and confidence.
- A SoC/device name is not evidence that an API, runtime, operator or precision is executable.
- Unsupported or unavailable fields remain UNKNOWN.
- Stable DeviceCapabilityProfile facts are distinct from RuntimeConditionProfile observations; current thermal, battery or memory pressure may reject/defer a workload but does not redefine the device's permanent capability.
- A configuration must include analytical memory and compute accounting with explicit excluded terms and safety reserve.
- If no candidate satisfies the documented envelope, Genesis returns a typed UNSUPPORTED_CONFIGURATION result; it must not silently downgrade to a different architecture or claim a model was created.
- Configuration selection is deterministic for the same manifest inputs unless an explicitly versioned policy says otherwise.

### 5.2 Genesis and model-generation repository

~~~text
GenesisCoordinator.createInitialGeneration(instanceId, policy)
    -> GenesisResult

ModelGenerationRepository.stageInitial(manifest, tensorSource)
    -> StagedGeneration

ModelGenerationRepository.commitInitial(stagedGeneration)
    -> CommittedGeneration

ModelGenerationRepository.openActiveReadSnapshot()
    -> ImmutableModelSnapshot

ModelGenerationRepository.createCandidate(parentGeneration, sessionManifest)
    -> CandidateGeneration

ModelGenerationRepository.commitCandidate(candidate, checkpoint)
    -> CommittedCandidate

ModelGenerationRepository.promote(candidate, evaluationReport)
    -> PromotionResult

ModelGenerationRepository.rollbackTo(generationId, reason)
    -> ActiveGeneration
~~~

Requirements:
- A temporary/staged generation is not visible as active.
- Initial commit and candidate commit validate every required file/tensor, manifest digest and schema.
- Commit uses an atomic publication boundary or a recoverable journaled equivalent.
- Promotion is a separate operation from checkpoint commit.
- A failed candidate never damages the active generation or last-known-good generation.
- Read snapshots remain stable for their lifetime; promotion changes the active pointer, not an existing inference snapshot.
- Instance ID and generation ID are verified on every read/write.

### 5.3 Tokenizer

~~~text
TokenizerProvider.prepare(profileRequest, resourceBudget)
    -> TokenizerPreparationDecision

TokenizerTrainer.train(seedCorpusManifest, trainerConfig, resourceBudget)
    -> TokenizerArtifact

TokenizerProvider.load(tokenizerManifest) -> TokenizerInstance

TokenizerInstance.encode(text, options) -> TokenSequence
TokenizerInstance.decode(tokenIds, options) -> DecodedText
TokenizerInstance.fingerprint() -> Digest
~~~

Requirements:
- The prepare operation is invoked before ModelConfig finalizes vocabulary-dependent tensor shapes and before Genesis allocates embeddings/output-head tensors.
- The profile decision is constrained by the approved product language/domain policy, rights-cleared tokenizer data, the versioned device capability profile and an explicit tokenizer-preparation resource budget.
- TokenizerTrainer is optional and capability-gated. It may use only a bounded, immutable, rights-cleared tokenizer seed-corpus manifest; tokenizer-training permission does not imply neural-model training permission.
- Account for tokenizer-training CPU time, peak memory, storage, energy and cancellation. Do not assume GPU/NPU acceleration without a separately verified trainer/backend path.
- If the selected local-training profile cannot satisfy its declared budget, use only a pre-approved compatible precomputed/algorithmic fallback selected before model tensor allocation, or return a typed failure. No silent profile switch after generation construction begins.
- No API for implicit vocabulary registration is exposed to production inference/training.
- Encoding is deterministic for a fixed artifact/configuration and explicit options.
- Special-token parsing is separated from ordinary user text so untrusted text cannot inject control structure.
- UTF-8 byte offsets and Kotlin UTF-16 offsets must never be mixed without a documented conversion.
- Token IDs, normalization and decoder behavior are checked against the exact manifest fingerprint.
- Any changed token-to-ID mapping is a generation migration.

### 5.4 Inference

~~~text
InferenceService.generate(
    snapshot,
    request,
    inferenceOptions,
    cancellation
) -> InferenceResult
~~~

Requirements:
- Request carries the instance and generation identity, but not mutable references to training state.
- Backend selection reports actual backend, operator coverage, graph partitions, fallback reason and precision.
- Cancellation releases request-owned workspace and cache state safely.
- Inference errors do not mutate or invalidate the model generation.
- A request may use a previously acquired immutable snapshot while a new candidate is evaluated, but memory admission must account for both snapshots and all workspaces.

### 5.5 Training and learning coordinator

~~~text
LearningCoordinator.submit(sessionRequest) -> SessionHandle
LearningCoordinator.pause(sessionId, reason) -> PauseResult
LearningCoordinator.resume(sessionId) -> ResumeResult
LearningCoordinator.stop(sessionId, reason) -> StopResult
LearningCoordinator.status(sessionId) -> SessionStatus
~~~

Requirements:
- Session admission verifies Genesis readiness, active parent digest, immutable tokenizer, approved immutable dataset manifest, configuration and resource envelope.
- Training engine receives a declared model configuration, immutable training data iterator and candidate-only mutable state.
- A training step returns a typed numerical result (finite/invalid), gradient norm/status, optimizer update sequence and resource telemetry; a UI event is not the source of truth for committed progress.
- The optimizer update sequence is monotonic and committed exactly once.
- Pause is acknowledged only at a declared safe boundary. Stop is distinct from pause.
- Resume verifies the exact parent/candidate/session/dataset/tokenizer/optimizer/RNG/cursor identity before applying another update.
- A change to optimizer, precision, packing, gradient reduction or objective creates a new session configuration; it is not a silent adaptive-workload adjustment.
- The session records and verifies the exact tokenizer manifest digest, profile ID and model configuration fingerprint. A tokenizer profile or vocabulary change requires a new generation/migration contract, never an in-place session mutation.
- Training never consumes raw acquisition responses or unreviewed personal captures.

### 5.6 Policy and data governance

~~~text
PolicyEngine.evaluate(operation, resource, actor, context) -> PolicyDecision
ConsentRepository.check(scope, subject, resource) -> ConsentDecision
CandidateReview.approve(candidateId, reviewer, scope) -> ApprovalRecord
CandidateReview.revoke(approvalId, reason) -> RevocationRecord
~~~

Requirements:
- Permission is operation-specific: discover, fetch, parse, retain, retrieve, train, export and redistribute are separate scopes.
- General-corpus source rights and personal-data consent are different authorities.
- A missing or indeterminate decision is DENY for the protected operation.
- Revocation prevents future use and triggers defined deletion/lineage actions. The effect on already trained weights requires an explicit model-lineage/deletion policy; do not promise exact unlearning without a validated method.
- Personal records may not silently enter general corpus or general training.
- Policy decisions are versioned and audit-linked.

### 5.7 Acquisition, corpus and retrieval

~~~text
AcquisitionCoordinator.discover(plan) -> DiscoveryBatch
AcquisitionCoordinator.fetch(approvedJob) -> FetchResult
KnowledgeRepository.ingest(validatedRecord) -> KnowledgeRecordId
CorpusBuilder.build(approvedRecordSet, splitPolicy) -> TrainingCandidateManifest
RetrievalService.query(query, filters) -> RankedEvidence
~~~

Requirements:
- A source registry entry and source-specific operation scope must permit the requested action before a connector runs.
- Fetched content is untrusted data, never executable policy or system instructions.
- Provenance/rights/quality processing completes before a record is accepted.
- Retrieval eligibility does not imply training eligibility.
- Corpus building uses pinned revisions and deterministic preprocessing; it may not consume a live queue.
- Training/evaluation/test split membership is stable and leakage checks are recorded.
- Records that cannot be retained as full text must not be promised as available for future retokenization or retraining.

## 6. Lifecycle state machines

### 6.1 Application and Genesis

~~~text
APP_CREATED
  -> CAPABILITY_ASSESSMENT
  -> RUNTIME_CONDITION_ASSESSMENT
  -> CONFIGURATION_SELECTED
  -> TOKENIZER_PREPARATION
  -> MODEL_CONSTRUCTION
  -> PARAMETER_INITIALIZATION
  -> INITIAL_GENERATION_STAGED
  -> INITIAL_GENERATION_VALIDATING
  -> GENESIS_READY
  -> APPLICATION_READY
~~~

Failure at any pre-commit state transitions to GENESIS_RECOVERY_REQUIRED or a typed terminal unsupported state. A partial/invalid initial generation is never published. GENESIS_READY means initialized, persisted and recoverable; it does not mean trained or validated.

### 6.2 Training session

~~~text
CREATED -> POLICY_CHECK -> RESOURCE_ADMISSION -> QUEUED -> RUNNING
RUNNING -> PAUSE_REQUESTED -> PAUSED -> RESUME_CHECK -> QUEUED
RUNNING -> CHECKPOINTING -> RUNNING
RUNNING -> STOP_REQUESTED -> STOPPED
RUNNING -> FAILED
RUNNING -> COMPLETED -> CANDIDATE_EVALUATION
CANDIDATE_EVALUATION -> PROMOTION_PENDING -> PROMOTED
CANDIDATE_EVALUATION -> REJECTED
~~~

- PAUSED means a committed resumable state exists and no update is in flight.
- STOPPED is a user/owner-requested terminal state; it does not imply failure.
- FAILED includes a typed cause and recovery disposition.
- COMPLETED means the declared training schedule finished, not that the candidate is useful.
- PROMOTED means a validated candidate atomically became active.
- A candidate can be trained and still be rejected.

### 6.3 Knowledge item and training-candidate lifecycle

~~~text
DISCOVERED -> FETCH_AUTHORIZED -> FETCHED -> PARSED -> PROVENANCE_CHECK
  -> RIGHTS_CLASSIFIED -> QUALITY_CHECK -> RETENTION_APPROVED
  -> RETRIEVAL_ELIGIBLE -> TRAINING_REVIEW -> TRAINING_ELIGIBLE
~~~

Any state may transition to HELD, REJECTED, EXPIRED or DELETED according to policy. RETRIEVAL_ELIGIBLE and TRAINING_ELIGIBLE are independent outcomes; neither implies the other.

Personal candidates use a separate path:

~~~text
CAPTURED -> SENSITIVITY_CLASSIFIED -> REDACTED -> USER_REVIEW
  -> EXPLICITLY_APPROVED -> PERSONAL_TRAINING_ELIGIBLE
~~~

Revocation/deletion must invalidate pending candidate membership and follow the documented model-lineage policy if already trained.

## 7. Typed errors and recovery policy

Every error has: stable code, category, severity, retryable, userActionRequired, safeToResume, correlationId, optional causeCode, and a sanitized diagnostic context. Raw prompts, private file contents, credentials and full acquired text are excluded from generic error/event payloads.

| Error family | Example codes | Default disposition |
|---|---|---|
| Capability/configuration | CAPABILITY_UNKNOWN, CAPABILITY_UNSUPPORTED, CONFIGURATION_NO_FIT | Do not guess; report unsupported or require a narrower workload |
| Genesis/model identity | GENESIS_INIT_FAILED, GENERATION_MANIFEST_INVALID, GENERATION_CHECKSUM_MISMATCH | No initial/active publish; recover last valid state |
| Tokenizer | TOKENIZER_FINGERPRINT_MISMATCH, TOKENIZER_DECODE_INVALID, TOKENIZER_ARTIFACT_MISSING | Fail closed; no silent fallback or checkpoint resume |
| Backend/runtime | BACKEND_UNAVAILABLE, OPERATOR_UNSUPPORTED, BACKEND_NUMERICAL_MISMATCH, BACKEND_FALLBACK | Retry another explicitly compatible backend only when numerical contract permits; report actual fallback |
| Training | TRAINING_NONFINITE_UPDATE, TRAINING_CANCELLED, TRAINING_RESOURCE_DENIED, TRAINING_SEMANTICS_MISMATCH | Do not commit invalid updates; pause/stop or require new session |
| Checkpoint/storage | CHECKPOINT_INCOMPLETE, CHECKPOINT_CORRUPT, CHECKPOINT_IDENTITY_MISMATCH, STORAGE_RESERVE_LOW | Reject checkpoint; preserve last-known-good generation |
| Data/rights | SOURCE_NOT_AUTHORIZED, RIGHTS_UNKNOWN, PROVENANCE_MISSING, CORPUS_SPLIT_LEAKAGE | Reject/hold candidate; no training |
| Privacy/consent | CONSENT_MISSING, CONSENT_REVOKED, CAPTURE_SECURE_SURFACE, DATA_SCOPE_MISMATCH | Deny protected operation; invalidate pending candidate links |
| Acquisition/network | SOURCE_RATE_LIMITED, FETCH_INTERRUPTED, CONTENT_INVALID, SOURCE_REVOKED | Bounded retry for transient errors; otherwise hold/reject |
| Android lifecycle | PROCESS_RECLAIMED, BACKGROUND_RESTRICTION, SERVICE_START_DENIED, PERMISSION_REVOKED | Persist safe state; rely on OS-compliant rescheduling; never evade platform policy |

Retry classification is operation-specific. BACKEND_UNAVAILABLE may permit fallback for inference but must not permit training fallback unless the backend supports the complete declared forward/backward/optimizer semantics. A permission or rights denial is not a transient network error.

## 8. Event and message envelope

Every lifecycle event uses a versioned envelope:

| Field | Meaning |
|---|---|
| schemaVersion | Event schema version |
| eventId | Unique immutable event ID |
| eventType | Stable namespaced type |
| occurredAt | UTC timestamp |
| instanceId | Local instance identity |
| generationId | Optional model-generation identity |
| sessionId | Optional training/acquisition job identity |
| correlationId | Correlates a use case and its child operations |
| sequence | Monotonic sequence within its owning session/aggregate |
| actor | System subsystem or user action |
| outcome | Started, progress, committed, paused, rejected, failed, etc. |
| payload | Typed, versioned, minimal metadata only |

Event families include genesis.*, model.generation.*, inference.*, training.session.*, training.update.*, checkpoint.*, evaluation.*, policy.*, consent.*, acquisition.*, knowledge.record.*, storage.* and backend.*.

Requirements:
- Events are status/diagnostic facts, not a second state database. Durable repositories remain authoritative.
- UI progress may be coalesced/throttled; committed optimizer-update counters are persisted independently.
- Do not emit one event per tensor element or token. Define bounded progress intervals by session policy.
- Never include raw user prompts, screen text, personal files, access tokens, API keys or full corpus content in generic event payloads.
- A consumer must tolerate duplicate delivery using eventId/sequence and must ignore unknown optional fields in forward-compatible schema versions.
- A missing sequence or digest must never be interpreted as a committed optimizer update.

## 9. Concurrency, ownership and transaction rules

1. **Single writer per candidate:** at most one training writer owns a candidate generation/session at any instant.
2. **Immutable inference snapshots:** inference holds read-only snapshots. It cannot see partially updated tensors.
3. **Active pointer isolation:** only the generation repository can update the active-generation pointer, after a verified promotion transaction.
4. **Checkpoint boundary:** checkpoint publication is coordinated with optimizer-update sequence and dataset cursor in one logical transaction. The checkpoint must not point beyond committed parameter state.
5. **Exactly-once commit:** replay after crash may repeat uncommitted compute, but no committed optimizer update may be duplicated or skipped.
6. **Idempotent acquisition:** each fetch/ingest operation has a stable request/content identity; retries cannot duplicate accepted records or lose provenance.
7. **Cancellation:** cancellation is cooperative and reaches a safe boundary. Storage transactions either commit completely or leave a recoverable uncommitted state.
8. **Bounded locks:** no network, parsing or long model computation runs while holding global model/store locks.
9. **Resource reservation:** inference, training, acquisition, indexing and checkpoint operations use separately accounted budgets. A controller cannot allocate memory already reserved for recovery or user data.
10. **Promotion isolation:** evaluation reads a fixed candidate digest; promotion rejects a candidate whose digest changed after evaluation.
11. **Consent revocation race:** a candidate cannot enter an optimizer update if its authorization is revoked before the training admission/update boundary. Revocation during a running session stops future use at the next safe update boundary and invalidates pending candidate membership.
12. **No hidden concurrent optimizer:** inference/training weight sharing, if ever allowed, requires an explicit snapshot/copy-on-write contract and measured memory admission. It is not the default.

## 10. Storage and transaction boundaries

Minimum logical storage partitions:

- instance/: instance manifest and policy versions.
- model/generations/<generation-id>/: immutable manifests and tensor shards.
- model/active: atomic reference to the active generation.
- training/sessions/<session-id>/: immutable session manifest, state and progress cursor.
- training/checkpoints/<checkpoint-id>/: checkpoint payload and checksum manifest.
- training/candidates/<candidate-id>/: immutable candidate corpus manifests.
- knowledge/sources/: source registry and reviewed policy records.
- knowledge/records/: retained records and provenance metadata.
- knowledge/indexes/: rebuildable indexes, each bound to source-record revisions.
- privacy/consents/: scoped grants/revocations and policy versions.
- audit/events/: sanitized lifecycle and policy audit events.

These names are logical namespaces, not committed filesystem paths.

A durable publication uses: (1) write to an isolated temporary location; (2) flush/close; (3) verify size, checksum and manifest; (4) commit a journal/manifest marker; (5) atomically publish a pointer or recoverable transaction record; (6) garbage-collect unreferenced temporary data only after recovery confirms it is not needed. Same-filesystem atomic rename may be used where the selected storage layer guarantees it; otherwise use a journaled equivalent. Do not claim ACID semantics without validating the chosen storage engine and crash behavior.

Deletion is a lineage operation: source record → parsed text → excerpts → summaries/embeddings/indexes → pending corpus candidates → training shards. If trained model weights contain influence from the deleted source, apply the separately approved model-retraining/rollback policy; do not assert exact machine unlearning.

## 11. Deployment and platform boundary

The initial deployment target is one Android application and one local installation namespace. Logical modules may ship in one APK/AAB; architecture boundaries are code-ownership and dependency boundaries, not mandatory separate processes.

- The core Genesis, model-state, tokenizer, training and memory paths must be designed to operate without a central service.
- Public-source acquisition is an optional, policy-controlled network path; offline inference and existing local knowledge must continue when the network is unavailable.
- Google Play services runtimes, vendor SDKs and external APIs are separately audited dependencies. Their telemetry, service-update, account, availability and offline characteristics must not be described as zero-network unless proven.
- CPU/GPU/NPU are execution backends selected by verified operator/runtime evidence. Device names or marketing TOPS do not create backend capability.
- Android Context, permissions, WorkManager, foreground services, PowerManager, SAF and accessibility/media projection APIs remain in platform adapters.
- No process priority, background execution or service restart policy may bypass Android platform restrictions.
- Other platforms require independent packaging, runtime, permissions, storage, privacy and verification contracts. Android deployment does not imply portability.

## 12. Sequence diagrams

### 12.1 First launch and readiness

~~~mermaid
sequenceDiagram
    participant UI
    participant App as App Orchestrator
    participant Cap as Capability Provider
    participant Sel as Config Selector
    participant Gen as Genesis Coordinator
    participant Store as Generation Repository

    UI->>App: launch()
    App->>Cap: inspect(required workload)
    Cap-->>App: versioned capability profile
    App->>Sel: select(profile, candidate set, budgets)
    Sel-->>App: configuration or typed no-fit
    App->>Gen: createInitialGeneration(instance, config)
    Gen->>Store: stage(manifest, initialized tensors)
    Store->>Store: validate tensor inventory and checksums
    Store->>Store: atomic commit
    Store-->>Gen: committed generation ID/digest
    Gen-->>App: GENESIS_READY
    App-->>UI: APPLICATION_READY
    Note over App,UI: General pretraining is not on this startup path
~~~

### 12.2 General learning and candidate promotion

~~~mermaid
sequenceDiagram
    participant Scheduler
    participant Policy
    participant Corpus as Corpus Builder
    participant Learn as Learning Coordinator
    participant Store as Generation Repository
    participant Engine as Training Engine
    participant Eval as Evaluation Service

    Scheduler->>Policy: authorize general-learning session
    Policy-->>Scheduler: allow/deny + policy revision
    Scheduler->>Corpus: obtain immutable eligible dataset manifest
    Corpus-->>Scheduler: dataset digest and split manifest
    Scheduler->>Learn: submit(session request)
    Learn->>Store: create isolated candidate(parent, session)
    Store-->>Learn: candidate generation
    Learn->>Engine: train candidate
    Engine-->>Learn: update/checkpoint status
    Learn->>Store: commit candidate checkpoint transaction
    Learn->>Eval: evaluate fixed candidate digest
    Eval-->>Learn: immutable evaluation report
    Learn->>Store: promote(candidate, report) or reject(candidate)
    Store-->>Learn: promotion/rejection result
~~~

### 12.3 Public knowledge acquisition and training handoff

~~~mermaid
sequenceDiagram
    participant Planner
    participant Policy
    participant Connector
    participant Record as Knowledge Repository
    participant Review as Rights/Quality Review
    participant Corpus as Corpus Builder

    Planner->>Policy: authorize discovery/fetch scope
    Policy-->>Planner: allowed source/operation policy
    Planner->>Connector: fetch approved job
    Connector-->>Record: untrusted fetched material + source metadata
    Record->>Review: provenance, rights, quality and sensitivity
    Review-->>Record: retrieval/retention/train dispositions
    Record-->>Planner: local retrieval-eligible record
    Record->>Corpus: explicitly training-eligible records only
    Corpus-->>Corpus: deterministic split, dedup and manifest commit
~~~

### 12.4 Personal learning consent and revocation

~~~mermaid
sequenceDiagram
    participant Capture
    participant Policy
    participant Redact
    participant User
    participant Candidate
    participant Learn

    Capture->>Policy: request operation-scoped authorization
    Policy-->>Capture: allow capture or deny
    Capture->>Redact: short-lived candidate
    Redact-->>User: redacted preview and purpose
    User->>Candidate: explicit approve with scope
    Candidate->>Policy: verify active consent at admission
    Policy-->>Candidate: allow/deny
    Candidate->>Learn: immutable approved candidate manifest
    User->>Candidate: revoke/delete
    Candidate->>Learn: invalidate future use at safe boundary
~~~

These diagrams are logical sequences. They must not be used as evidence that the current prototype implements the participants or transactions.

## 13. Traceability to existing documents

| Contract area | Primary authority | This document's role |
|---|---|---|
| Product requirements and ADRs | [01 — System Requirements and Decisions](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md) | Maps implementation contracts to accepted product constraints |
| Logical layers and trust boundaries | [02 — System Architecture](02_SYSTEM_ARCHITECTURE.md) | Fills previously listed missing architecture outputs |
| Model and tensor abstractions | [09 — Model Architecture](09_MODEL_ARCHITECTURE.md), [20 — Genesis Engine](20_ARGUS_GENESIS_ENGINE.md) | Specifies identity, generation and snapshot boundaries without choosing architecture |
| Learning, optimizer and lifecycle | [10 — Training and Learning](10_TRAINING_LEARNING_ARCHITECTURE.md), [19 — P0 Remediation](19_P0_REMEDIATION_DESIGN.md) | Defines generic session/candidate contracts; selected math remains architecture-dependent |
| Device configuration and live workload | [21 — Device-Adaptive Model and Training](21_DEVICE_ADAPTIVE_MODEL_AND_TRAINING.md), [14 — Performance and Power](14_PERFORMANCE_POWER_THERMAL.md) | Defines profile/admission interfaces and separation of the two control loops |
| General/personal data separation | [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md), [13 — Security and Data Governance](13_SECURITY_PRIVACY_DATA_GOVERNANCE.md) | Defines policy and candidate lifecycle boundary |
| Acquisition and connectors | [24 — Autonomous Knowledge Acquisition](24_AUTONOMOUS_KNOWLEDGE_ACQUISITION.md), [26 — Source Registry](26_SOURCE_REGISTRY_AND_CONNECTOR_ARCHITECTURE.md) | Defines generic connector and provenance handoff |
| Architecture and tokenizer | [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md), [34 — Tokenizer Evaluation](34_TOKENIZER_EVALUATION_AND_DECISION.md), [35 — Android Component Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md) | Keeps unresolved architecture/tokenizer values as versioned contract placeholders |
| Empirical verification | [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md), [33 — From-Scratch Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md) | Maps invariants to later test suites, including E1–E7 |

## 14. Implementation verification obligations

The following are deliberately later tests. Their methods and acceptance criteria must be detailed in the implementation plan before coding is authorized.

| Test family | Required verification | Related contract |
|---|---|---|
| Contract/schema | Serialization round-trip, version migration, unknown field handling, malformed manifest rejection | Sections 4 and 8 |
| Genesis transaction | Crash at every stage, corrupt/truncated tensors, checksum mismatch, no partial active generation | Sections 5.2 and 6.1 |
| Instance isolation | Repeated independent installs/IDs; no cross-instance paths, state or gradient exchange | Sections 2 and 4 |
| Tokenizer conformance | Full reference/Android token-ID parity, Unicode, byte fallback, offsets, control-token injection | Section 5.3 and G1-V in document 34 |
| Inference isolation | Read snapshot remains stable during candidate training and promotion; memory admission under overlap | Sections 5.4 and 9 |
| Training correctness | Forward/loss/backward/optimizer reference parity, finite-difference checks, finite updates and exact update sequence | Section 5.5 and document 19 |
| Checkpoint recovery | Crash/corruption/low-storage injection; exact model/optimizer/RNG/sampler/cursor/idempotency resume | Sections 5.2, 5.5, 9 and E6 |
| Data rights/consent | Deny by default, operation-scope mismatch, revocation race, deletion cascade, training-candidate immutability | Sections 5.6–5.7 and 6.3 |
| Connector safety | SSRF/redirect/host validation, response-size limits, rate limits, prompt injection, invalid MIME/archive and retry idempotency | Sections 5.7 and 11 |
| Lifecycle/concurrency | Process death, background restriction, duplicate job, cancellation at safe boundary, competing inference/acquisition/training budgets | Sections 6 and 9 |
| Runtime/backend | Actual backend/operator report, numerical parity, fallback safety, unavailable provider and Play services/offline/telemetry behavior | Section 5.4, 11 and documents 08/15 |
| Candidate evaluation | Fixed digest, reproducible evaluation, regression failure, promotion race, rollback to last-known-good | Sections 5.2 and 6.2 |
| Device feasibility | E1–E7 on named representative physical devices after implementation; no broad support claim before pass | Document 33 |

## 15. Open decisions that remain legitimately open

This document intentionally does not close decisions that depend on architecture, corpus rights or product scope:

1. Exact neural architecture family, module graph, tensor layouts and per-device configuration portfolio.
2. Exact tokenizer algorithm and profile portfolio, product-language weights, device-conditioned vocabulary budgets/preparation modes, artifact releases and Android runtime.
3. Initial product language/domain mix and the minimum useful-learning target.
4. Initial training objective, parameter groups, optimizer, precision and mathematical update contract.
5. Concrete storage engine, encryption/key management, retention periods, migrations, backup/export and deletion policy.
6. Exact source activation allowlist and item-level neural-training permissions.
7. Runtime/provider choices, vendor license/terms, telemetry policy and device-specific operator coverage.
8. Minimum supported Android device tier, numeric memory/session/energy budget and thermal admission thresholds.
9. User consent wording, review defaults and the model-lineage policy after personal-data revocation.
10. Promotion thresholds and general-capability regression policy.

A decision may remain open without making the surrounding module boundary or lifecycle contract incomplete. Every open item must have an owner, required evidence, decision forum/ADR and latest acceptable resolution point before the dependent implementation work.

## 16. Contract readiness criteria

The pre-implementation contract baseline is ready for review when:

- Every subsystem has a single owner and explicit dependency direction.
- Every cross-module handoff has a typed identity, version and failure result.
- Genesis, training, knowledge acquisition, candidate review and promotion have explicit state transitions.
- The active-generation immutability, single-writer, checkpoint and cancellation rules are consistent across documents.
- Data/consent states cannot be bypassed by connector, UI or worker implementation.
- All unresolved architecture/tokenizer/runtime values are clearly marked rather than guessed.
- Device-conditioned model/tokenizer selection is deterministic from versioned evidence and policy, and every generation pins one exact tokenizer artifact fingerprint.
- Requirements in document 01 map to a contract and a planned verification family.
- The later verification suite is explicitly separated from documentation completion.
- No statement implies these contracts already exist in the prototype.

**Current disposition:** this document supplies the missing architecture-neutral contract draft. It does not by itself close the overall documentation phase, accept the architecture/tokenizer ADRs, approve corpus sources, authorize implementation or establish empirical Android feasibility.
