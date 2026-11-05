# Pre-Implementation Module and Lifecycle Contracts

**Status:** Proposed architecture-neutral contract baseline; architecture-specific bindings remain open  
**Review date:** 2026-11-05  
**Scope:** Documentation only. This document authorizes no source changes, build, test, workflow execution, device experiment or artifact publication.  
**Authority:** Logical module/API boundaries, lifecycle state machines, event/error contracts, storage entities, concurrency rules and cross-system sequences. Exact neural architecture, tokenizer, runtime providers, numeric budgets and product-language scope remain governed by their research documents and ADRs.

## 1. Purpose and design boundary

Document 02 identifies module/API contracts, error taxonomy, event/message schema, model lifecycle, concurrency rules, data schema, deployment diagrams and sequence diagrams as required pre-implementation outputs. This document supplies the first architecture-neutral version of those contracts.

The contracts define **what each subsystem owns and guarantees**, not how its implementation is written. They are independent of a specific Transformer/SSM/RWKV/hybrid selection, tokenizer family, Kotlin package layout, native training framework or accelerator. The proposed physical Kotlin package hierarchy and file-placement rules are defined in [45 — Android Project Structure and File-Placement Rules](45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md). Document 36 remains authoritative for generic logical API/lifecycle ownership; document 45 governs package and source-tree placement and includes a crosswalk for every subsystem defined here. The personal-capture-specific ownership, routing, consent, retention and review specialization is defined in [48 — Personal Capture and Experience Intake Contract](48_PERSONAL_CAPTURE_AND_EXPERIENCE_INTAKE_CONTRACT.md), which must preserve the generic identity, policy, revocation, transaction and training lifecycle contracts in this document. The package crosswalk must not alter this document's owner/guarantee contracts. Package boundaries do not imply one Gradle module per package. Family-specific recurrent state requirements for Mamba-2 SSD and RWKV-7 are detailed separately in [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md), while this document retains the architecture-neutral transaction boundary.

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
11. **Generation-scoped structural evolution:** a change to the model graph, dimensions, module inventory or parameterization is staged as a child generation. A committed active generation is never widened, deepened or structurally rewritten in place.
12. **Checkpointed sparse topology:** any dynamic connectivity change occurs only in candidate state at a declared safe boundary; the topology, active-parameter optimizer state, update schedule and RNG identity are part of exact recovery.
13. **Local evolution:** architecture selection, growth decisions, weights, optimizer state and topology remain within one installation. Federated aggregation and cross-instance parameter exchange are outside the accepted product boundary.

## 3. Logical module boundaries

These are logical ownership boundaries. They may map to Gradle modules, Kotlin packages or a combination after implementation planning; no one-to-one Gradle module structure is mandated.

| Logical module | Owns | Must not own / must not bypass |
|---|---|---|
| Experience/UI | User controls, consent/review surfaces, status presentation, user-visible errors | Direct tensor mutation, connector permission decisions, background worker internals |
| Application orchestrator | Process-independent use-case coordination, readiness, job/session lifecycle, resource admission, dispatch and cancellation | Android process/service callbacks, OS scheduling APIs, model mathematics, source licensing decisions or direct storage-file mutation |
| Trust and policy | Permission scopes, policy evaluation, approval/revocation, network/data rules and fail-closed decisions | Model inference or source fetching implementation |
| Device capability service | Versioned stable capability evidence and confidence/unknown states; exposes a separate live runtime-condition snapshot | Guessing hardware execution from chipset strings or treating transient conditions as permanent hardware capability |
| Genesis coordinator | Initial profile admission, model construction orchestration, local initialization and first-generation transaction | Corpus pretraining on the startup critical path |
| Model-generation repository | Immutable manifests/tensors, generation state, active pointer, candidate isolation, validation and rollback | Training mathematics or implicit model selection |
| Tokenizer service | Versioned tokenizer load, deterministic encode/decode and fingerprint checks | Runtime vocabulary registration or changing token IDs in an active generation |
| Inference service | Read-only model snapshot, request execution, cancellation and result provenance | Updating weights or optimizer state |
| Learning coordinator | Session creation, training admission, candidate lifecycle, pause/resume, checkpoints and promotion | Direct ingestion of unapproved raw input |
| Architecture evolution controller | Capacity assessment, growth evidence, approved mutation planning, growth budgets and child-generation transition coordination | Active-generation tensor mutation, numerical training, sparse-kernel execution or direct candidate promotion |
| Training engine/backend | Forward, objective, backward, optimizer update and numerical status for a declared architecture contract | Source rights, personal consent, generation promotion or user-facing lifecycle policy |
| Corpus and knowledge store | Source/item provenance, canonical content where retention permits, indexes and immutable dataset manifests | Treating retrieval eligibility as training eligibility |
| Acquisition/connectors | Approved network fetch, parsing and connector-specific rate/response behavior | Granting themselves source permission or selecting training eligibility |
| Evaluation/promotion | General and personal evaluation, regression gates and promotion decision record | Mutating an active generation before a passing validation transaction |
| Event/diagnostic contracts | Typed lifecycle-event schemas and sanitized diagnostic metadata; emitting state remains owned by each workflow | A mandatory global event bus, duplicate state database, raw private text, credentials, prompts or corpus contents in generic event payloads |

### 3.1 Dependency direction

Dependencies must point toward contracts, not concrete platform implementations:

- UI → application workflow APIs.
- Application orchestrator → trust policy, device capability, Genesis, repository ports and workflow coordinators.
- Genesis → capability profile, architecture registry, initializer, tokenizer manifest and generation repository.
- Inference/training → read-only or candidate model snapshots plus declared backend interfaces.
- Acquisition → source registry/policy, connector interfaces, provenance store and resource admission.
- Promotion → evaluation report, candidate manifest and generation repository.
- Architecture evolution → evaluation history, capability/runtime conditions, approved mutation catalog and model-generation repository; tensor transforms remain in the selected engine, and promotion remains with evaluation/repository.
- Android entry adapters → narrow application-facing ingress/control interfaces; application workflows never depend on Android service implementations.
- Concrete data, platform and engine adapters → the contracts they implement; they do not form a default dependency chain between one another.

The trust/policy service is consulted by the application orchestrator and by each privileged boundary; it is not a UI-only check. Connectors and model backends cannot override it.

Circular ownership is prohibited. The training engine does not decide whether a dataset is legally eligible, and a connector does not decide whether fetched text can be retained or trained on.

The application layer owns process-independent workflow state such as Genesis readiness and training-session pause/resume. Android process callbacks, foreground-service lifecycle, WorkManager integration, permission APIs and OS scheduling constraints belong to platform adapters. Platform adapters may call narrow application entry-point contracts, but application code must not import Android service or worker classes.

Lifecycle events are typed facts, not a second state store. Each workflow owns its authoritative state and may expose a state/result stream to observers. Audit persistence is a repository concern. A global mutable event bus is not required by this contract.

### 3.2 UI observation and immutable feature state

- The application workflow remains the authoritative owner of process-independent state. For long-lived observable workflow status, its outward contract is a read-only `StateFlow<WorkflowSnapshot>`; the mutable state holder remains private to the workflow.
- A feature ViewModel maps that snapshot into a feature-owned immutable `FeatureUiState` and exposes it read-only to Compose. Screen-specific state types remain small and purpose-built; do not create one global UI state object for unrelated features.
- A snapshot is keyed by the owning identity (for example, `sessionId`) and carries a monotonic revision/sequence. Where relevant, it exposes the current lifecycle phase, committed progress with an explicit unit, display metrics, recoverable failure and permitted user actions. A field is omitted when it has no meaning for that workflow.
- Progress displayed as committed work comes from the workflow/repository authority. UI sampling may coalesce intermediate updates, but a ViewModel or notification must never maintain a competing step, batch, epoch or optimizer-update counter.
- ViewModels send typed commands to application-facing APIs. They do not mutate repositories, model objects, training engines or platform adapters directly. Database rows, network DTOs, engine tensors, raw personal content and Android framework objects are not UI state.
- Compose collection is lifecycle-aware. Recreating a screen re-observes the latest workflow snapshot; it does not recreate or reset the underlying job/session.

This defines the observation pattern and state guarantees. Concrete per-feature Kotlin data classes are implementation declarations, not additional architecture decisions.

### 3.3 Notification navigation contract

- `app/navigation` owns the canonical internal destination `argus://app/learning/session/{sessionId}` and its mapping to the learning-session screen. The route is an internal application route, not an externally browsable data-ingress surface.
- `platform/android/notification` creates an explicit, immutable PendingIntent through the host-owned navigation contract. Its identity is unique per session so one session notification cannot overwrite another session's destination.
- Tapping a notification brings the existing Argus task forward when present. On a cold launch, the normal application root is created before the requested session destination; navigation must not create duplicate Activity instances.
- The destination treats the route's `sessionId` as an identifier only: it re-queries `application/learning`, validates that the session exists and is visible to the current local instance, then renders the latest snapshot. Notification extras are not authoritative session state or authorization.
- Opening a session does not implicitly start, resume, stop or otherwise mutate training. Those remain explicit user commands routed through `application/learning`.

The URI is stable route identity; concrete Android PendingIntent construction and navigation-library wiring are platform implementation details and must preserve these semantics.

### 3.4 Composition and dependency-lifetime contract

- The default composition strategy for the current single-`:app` topology is explicit manual constructor injection from `app/bootstrap`. A DI framework is not required for the first implementation or package-boundary migration.
- The composition root is the only place that binds concrete data, platform and engine adapters to the ports owned by core/application contracts. Framework-created Android entry components receive narrow factories/dependencies through the Application-owned composition boundary; ordinary Kotlin classes use constructor injection.
- The graph has explicit lifetimes: process scope for repositories and stable registries; workflow/session scope for coordinators, cancellation and candidate ownership; ViewModel scope for presentation state only; Android-component scope for platform adapters tied to that component. No UI or global object becomes the owner of a training session.
- Expensive model/tokenizer/runtime instances are created lazily after the relevant profile and resource admission, not eagerly while the process graph is constructed.
- Static companion-object service locators, reflection-based discovery, global mutable registries and arbitrary cross-layer lookups are prohibited. A framework may be reconsidered only with measured graph complexity or a concrete module boundary that justifies it; that does not reopen the constructor-injection contract.


## 4. Cross-module data contracts

The following are minimum logical records. A concrete serialization (Proto, DataStore, Room, flatbuffer or custom binary) is not selected here.

| Record | Required identity and content | Integrity / lifecycle requirement |
|---|---|---|
| InstanceManifest | instance ID, schema version, creation time, local storage namespace, initial policy version | Created once per installation; never reused across instances |
| DeviceCapabilityProfile | profile ID, Android build/API, ABI/ISA, memory class and measured baseline/headroom evidence; CPU features; GPU/Vulkan loader and driver identity, reported API version, compute queue, queried features/extensions/limits and precision/storage capabilities; optional NPU/DSP provider/runtime/driver/operator evidence; tokenizer-training capability evidence; telemetry availability, confidence and unknown fields | Timestamped, source-labelled and versioned; unknown is not converted to supported; contains stable capability evidence rather than a live workload snapshot |
| RuntimeConditionProfile | sample ID/time, current available memory/pressure, OS thermal status, battery/charging/power-saver state, current storage reserve, foreground/background and scheduler constraints | Short-lived observation with source/confidence; re-read for workload admission and never treated as a permanent hardware capability |
| ModelConfig | architecture family/version, graph/module config, dimensions, tensor dtypes/layouts, context/state shape, precision, selected configuration-profile ID, backend/operator routing map, Vulkan shader/kernel contract where selected, explicit CPU/NPU fallback constraints, and any declared sparse connectivity format/capacity | Canonical fingerprint; unresolved architecture fields remain placeholders until ADR selection; a changed graph or tensor shape produces a new configuration fingerprint |
| TokenizerManifest | tokenizer ID/revision, profile ID, preparation mode (precomputed/local-trained/algorithmic), algorithm/trainer/runtime versions, normalization/pre-tokenizer/decoder, vocabulary and special IDs, artifact digest, trainer configuration, seed-corpus manifest digest and provenance/license | Immutable within one generation; referenced by digest; no private content is embedded in the manifest |
| ModelGenerationManifest | instance ID, generation ID, parent generation, ModelConfig fingerprint, TokenizerManifest digest, initialization policy/version/seed metadata, tensor inventory, optional evolution-transition and sparse-topology digests, checksums, schema/storage versions | Immutable after commit; complete tensor inventory and digest verified before ACTIVE |
| ArchitectureEvolutionManifest | transition ID, parent generation ID/digest, child ModelConfig fingerprint, mutation class, reason/evidence window, inherited/transformed/new tensor mapping, initialization and optimizer-state migration policy, resource envelope, validation policy and rollback parent | Immutable for one transition; child cannot be promoted without validating the exact transition and parent |
| SparseTopologyState | candidate generation/session, parameter-group IDs, logical tensor shapes, sparse format/pattern, capacity and active count, mask/index payload digest, topology revision/update sequence, growth-prune schedule, RNG and active-parameter optimizer-state identity | Checkpointed atomically with model/optimizer/cursor state; absent for dense profiles; a density/topology change cannot be inferred from zero-valued dense tensors |
| DatasetManifest | dataset ID/revision, schema version, source-artifact references and SHA-256 digests, canonical normalized-dataset SHA-256 digest, parser/normalization/preprocessing/deduplication versions, record/document counts, provenance and rights references, tokenization state, split-manifest digest | Immutable content identity; canonical digest covers the exact versioned canonical representation consumed by readers; source digest and canonical dataset digest are distinct |
| DatasetSplitManifest | parent dataset digest, split-policy/version, deterministic seed where applicable, grouping boundary (document/conversation/source), per-split membership reference, count and SHA-256 digest for train/validation/test, leakage-check result | Immutable split identity; no record/group may cross incompatible splits; final test data is not used for gradient updates or training-time selection |
| DatasetAdmissionDecision | decision ID, dataset manifest digest, validator/policy version, checks and evidence references, decision (PENDING/APPROVED/REJECTED), reasons, reviewer/automated actor and timestamp | Append-only decision bound to one immutable manifest digest; a changed dataset requires a new manifest and decision; revocation/withdrawal is a separate policy event |
| TrainingSessionManifest | session ID, target candidate generation, parent digest, objective/loss version, trainable parameter groups, optimizer/schedule, precision, dataset manifest digest, admission-decision digest, split-manifest digest and train-split identity, sampler/cursor policy, accumulation/update semantics, RNG policy, sequence/document reset and recurrent-state carry policy, chunk size and BPTT mode where applicable, resource profile, engine/runtime compatibility identity | Immutable session definition; changes to mathematical semantics or any pinned dataset/split/tokenizer/model identity require a new session/revision |
| ModelCandidateManifest | candidate ID, instance ID, candidate generation ID/digest, parent active generation ID/digest, source session ID, dataset/admission digests, training-result digest, architecture/tokenizer/runtime fingerprints | Immutable candidate identity and lineage; candidate assessment and promotion transaction are separate records, not mutable flags on this manifest |
| EvaluationProtocolManifest | protocol ID/version, evaluation dataset and test-split digests, tokenizer/model compatibility fingerprints, metric definitions/implementation versions, prompt/template revision, decoding/sampling parameters and seeds where generation is evaluated, scoring configuration, runtime/backend/precision requirements and reproducibility classification | Immutable evaluation recipe; each report pins the exact protocol and all data/model identities |
| CandidateVerificationRecord | candidate digest, evaluation-report digest, verification-policy/version, baseline active-generation ID/digest captured at evaluation start, pass/fail and reason set, verifier version and timestamp | Immutable decision for the exact candidate/report/baseline tuple; metrics alone do not confer verification |
| PromotionTransaction | transaction ID, idempotency key, candidate generation/digest, verification-record digest, expected active-pointer revision and parent digest, prior/new pointer values, transaction state and audit metadata | Idempotent compare-and-swap publication; retry with same key and same payload returns the original result; key reuse with different payload is rejected |
| ActiveGenerationPointer | instance ID, active generation ID/digest, monotonically increasing pointer revision, last promotion/rollback transaction ID | Single authoritative active-generation reference; pointer changes only through repository transaction; prior generations remain immutable for rollback |
| CheckpointManifest | checkpoint ID, session ID, candidate generation, parent digest, model/optimizer/scheduler/scaler/RNG state, data cursor/sampler, committed update sequence, optional recurrent-state and sparse-topology manifests/payload digests, dataset/admission/split digests, tokenizer/configuration/engine/runtime fingerprints, checksum and storage version | Immutable checkpoint snapshot; all payloads and cursors represent one safe boundary; exact-resume validation before use |
| CheckpointValidationRecord | checkpoint ID/digest, validator version, payload-integrity result, session/model/tokenizer/dataset/optimizer/runtime compatibility checks, validation state (UNVALIDATED/VALID/CORRUPT/INCOMPATIBLE), reasons and timestamp | Per-checkpoint immutable validation result; restorable eligibility is derived from VALID plus current policy/resource admission, never from a session or candidate status |
| RecurrentStateManifest | exact model/candidate generation and training-session identity, architecture/state-layout fingerprint, state component IDs, sequence/document identity, chunk cursor, tensor shapes/dtypes/layouts/checksums, reset/carry policy, state-production optimizer-update index | Conditional record for stateful Mamba-2/RWKV-style streams; absent only when the declared training mode resets at independent-sequence boundaries; never store a partial set of required state components |
| CheckpointManifest | checkpoint ID, session ID, candidate generation, parent digest, model/optimizer/scheduler/scaler/RNG state, data cursor/sampler, committed update sequence, optional recurrent-state and sparse-topology manifests/payload digests, checksum and storage version | Atomic transaction; model, optimizer, topology, data cursor and any required recurrent carry must refer to one consistent commit boundary; exact-resume validation before use |
| SourceRegistryEntry | source ID/version, endpoint/connector type, jurisdiction/region, source owner, terms/license evidence, allowed operation scopes, rate limits, review date and status | Default NOT-ACTIVATED; operation-specific permission |
| KnowledgeRecord | record ID, source/item revision, canonical URI or stable source identity, retrieved time, content digest, language/domain, rights state, attribution, retention scope, extraction version and sensitivity | Retrieval/retention rights recorded separately from training rights |
| TrainingCandidateManifest | candidate ID, general/personal partition, exact record IDs/revisions, rights/consent approvals, preprocessing/split/dedup version, tokenizer fingerprint if tokenized, checksum | Immutable after approval; revoked/held/deleted inputs excluded |
| ConsentGrant | grant ID, subject/instance, purpose/scope, source categories, processing/retention permissions, policy text/version, timestamp and revocation state | Scoped, revocable and auditable; OS permission alone is not a consent grant |
| AcquisitionJob | job ID, source ID, operation, request fingerprint, resource/network budget, state, retry policy, cursor and provenance refs | Idempotent; bounded retries and durable transitions |
| EvaluationReport | candidate generation, parent, evaluation set revision, metric definitions, general/personal results, regression checks, resource evidence and disposition | Immutable, tied to exact candidate digest |
| AuditEvent | event envelope and policy/lifecycle facts, actor/operation, target IDs, timestamp and outcome | Append-only within retention policy; no raw private payloads |

No document may claim a complete database schema until exact fields, indexes, encryption/key ownership, migrations, deletion cascades and retention/backup policy are specified for the chosen storage engine.

## 5. Core interface contracts

The signatures are conceptual. Exact feature-specific Kotlin types, suspendability and callback style remain implementation bindings. The composition strategy is no longer open: use explicit manual constructor injection through app/bootstrap as defined in section 3.4; a DI framework is optional future tooling, not a prerequisite.

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

ModelGenerationRepository.promote(
    candidateDigest,
    verificationRecordDigest,
    expectedActivePointerRevision,
    idempotencyKey
) -> PromotionResult

ModelGenerationRepository.rollbackTo(
    generationId,
    expectedActivePointerRevision,
    reason,
    idempotencyKey
) -> ActiveGeneration
~~~

Requirements:
- A temporary/staged generation is not visible as active.
- Initial commit and candidate commit validate every required file/tensor, manifest digest and schema.
- Commit uses an atomic publication boundary or a recoverable journaled equivalent.
- Promotion is separate from checkpoint commit, candidate evaluation and candidate verification.
- The repository verifies the exact candidate digest and verification-record digest, then compares the current active-pointer revision/parent digest with the expected values before publication. A changed pointer produces a typed conflict; the system must not promote against a stale baseline.
- Promotion is idempotent. Repeating an identical request with the same idempotency key returns the original committed result. Reusing the key with different transaction content is rejected.
- The repository atomically publishes a new ActiveGenerationPointer revision and a durable PromotionTransaction record. The prior pointer and generation remain available until rollback/retention policy explicitly permits removal.
- Rollback is a new audited pointer transaction to an existing validated generation; it does not rewrite or delete model tensors and does not mutate inference snapshots already opened.
- A failed candidate never damages the active generation or last-known-good generation.
- Read snapshots remain stable for their lifetime; promotion changes the active pointer, not an existing inference snapshot.
- Instance ID and generation ID are verified on every read/write.

### 5.2.1 Generation-scoped structural evolution and sparse topology

Argus may be designed to evolve model structure over an installation's lifetime, but the initial architecture-selection contract and the later growth mechanism are separate decisions. Growth is not an in-place edit to the active model and does not occur merely because new data arrived or a device reports more currently available RAM.

Conceptual contracts:

~~~text
GrowthController.assess(
    activeGeneration,
    evaluationHistory,
    learningDiagnostics,
    capabilityProfile,
    runtimeConditionProfile,
    growthBudget
) -> GrowthDecision

ArchitectureEvolutionPlanner.plan(
    activeGeneration,
    growthDecision,
    approvedMutationCatalog
) -> ArchitectureEvolutionManifest

SparseTopologyController.proposeUpdate(
    candidateState,
    topologyPolicy,
    gradientEvidence
) -> SparseTopologyUpdate

ModelGenerationRepository.createCandidate(
    parentGeneration,
    evolutionManifest,
    trainingSession
) -> CandidateGeneration
~~~

**Structural expansion and topology evolution are different operations:**

| Mutation class | Meaning | Identity/lifecycle requirement |
|---|---|---|
| Additive module expansion | Add a bounded adapter, expert, residual branch or other explicitly declared module | New child ModelConfig and tensor inventory; parent remains immutable; router semantics and extra inference cost must be evaluated |
| Width/depth expansion | Change hidden dimensions, block count, FFN width, head/group shape or block map | New child generation and complete tensor/state mapping; all affected projection, normalization, embedding/head and cache/state shapes must be accounted for |
| Fixed-budget sparse rewiring | Prune and regrow connections while keeping the declared active-parameter budget approximately/factually fixed | Candidate session owns topology revisions; each safe-boundary checkpoint includes the exact mask/index representation and optimizer state for active parameters |
| Sparse-capacity growth | Increase the number of active connections beyond the previous active budget within an explicit maximum capacity | A separately budgeted capacity mutation; new optimizer state and topology revision are created; this is not the same claim as fixed-budget RigL-style rewiring |

**Growth trigger requirements:**

- New data, higher advertised RAM, a transient rise in free memory or a single validation fluctuation is not sufficient evidence to expand.
- The controller must use a versioned evaluation window and repeated evidence that the current capacity is a material bottleneck. It must first rule out data quality/coverage, tokenizer mismatch, objective errors, optimization instability, inadequate training exposure, evaluation leakage and backend faults.
- Growth policy must state which metric indicates capacity saturation, the persistence/confidence rule, the maximum added parameter/active-edge budget, the cooldown/reassessment interval and the conditions that defer or reject growth.
- Resource admission uses stable device capabilities and current runtime conditions separately. A temporary resource improvement may permit scheduling of an already-approved child operation; it does not silently redefine the selected architecture.
- If the parent-plus-candidate, inference overlap, staging, optimizer state, topology-update workspace and checkpoint/rollback peaks do not fit, growth is deferred or rejected. The active generation is never modified to evade the memory gate.

**Child-generation transition requirements:**

1. Record the exact parent generation digest, reason/evidence window, mutation type, child graph/configuration fingerprint and resource worksheet before allocating the new tensors.
2. Preserve unchanged tensors exactly where the mapping contract permits. Any transformed tensor requires a deterministic, versioned mapping. New tensors require a family-correct initializer and recorded seed/PRNG policy.
3. Preserve tokenizer identity and token IDs for a structural-only expansion. A tokenizer or vocabulary change is a separate migration with new embedding/output dimensions and its own compatibility gate.
4. Copy optimizer state only when parameter identity, shape, dtype and update semantics are compatible. Transformed or new parameters require explicit state migration or a documented fresh-state policy. Newly introduced or regrown parameters must use a validated optimizer warm-up policy; silently inheriting stale moments is prohibited.
5. Train and evaluate the child as an isolated candidate. Compare general and personal evaluation partitions, regression/forgetting measures, inference routing, memory, latency, energy and thermal behavior against the parent.
6. Promote only through the existing evaluation/promotion transaction. Keep the parent as last-known-good until promotion and retention policy permit release. Failed migration or candidate evaluation rolls back without damaging the parent.
7. Preserve per-installation ownership. The growth controller may operate autonomously within its approved local policy; it must not upload weights, gradients, optimizer state or personal training data for cross-user aggregation.

**Sparse-training constraints:**

- A dense tensor containing zeros is still dense storage. Sparsity saves memory only when values and required metadata are stored in a genuinely sparse/structured representation and gradient/optimizer state is bounded to active parameters or another explicitly costed scheme.
- Fixed-budget dynamic sparse training changes which connections are active; it does not automatically increase the number of active parameters. Monotonic density/capacity growth is a separate algorithm and resource policy.
- Unstructured sparsity may reduce arithmetic counts without delivering real acceleration on hardware. Prefer evaluating hardware-executable structured patterns (for example a declared N:M or block pattern), but never assume the Android GPU, Vulkan driver or NPU supports a sparse pattern without a matching kernel and measured speedup.
- The memory worksheet must include sparse values, indices/masks/pattern metadata, active gradients, optimizer states, topology scores/temporary gradients, rewire scratch, activations, backend conversions and checkpoint serialization. If topology scoring requires dense or broad inactive-gradient work, include that temporary peak.
- A topology update is applied only at a declared safe update boundary. If validation, numerical checks, checkpointing or device execution fails, discard the uncommitted topology revision and resume from the last durable topology/model/optimizer/cursor checkpoint.
- Record deterministic topology-update schedule, tie-breaking, pruning/growth score definition, RNG, optimizer warm-up, density scaling, pattern constraints and backend/kernel identity. An inference-only sparse kernel is not sufficient evidence for sparse training support.

**Research disposition:** treat additive module/expert expansion as the lower-blast-radius structural-growth candidate for investigation, not as an approved architecture. Progressive columns, DEN-style selective expansion and function-preserving network morphism are evidence sources with different assumptions; their results do not establish Transformer-from-random-initialization growth on Android. Keep width/depth mutation as a higher-cost alternative because it touches many tensor and runtime shapes. Structured dynamic sparse training is a separate research candidate and is not a required first-generation feature.

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
- Session admission verifies Genesis readiness, the expected active parent digest, immutable tokenizer/configuration identities, the exact immutable DatasetManifest digest, an APPROVED DatasetAdmissionDecision for that same digest, the exact DatasetSplitManifest digest, required provenance/rights and consent, and the full resource envelope.
- The DatasetManifest is the content identity; DatasetAdmissionDecision is a separate append-only decision. Approval of one digest never authorizes changed bytes, a new parser/preprocessing revision or a different split.
- Training engine receives a declared model configuration, a streaming reader bound to the approved manifest/train split and candidate-only mutable state. It does not receive an unbounded in-memory list as the authoritative dataset identity.
- A training step returns a typed numerical result (finite/invalid), gradient norm/status, optimizer update sequence and resource telemetry; a UI event is not the source of truth for committed progress.
- The optimizer update sequence is monotonic and committed exactly once.
- **Safe optimizer-update boundary:** update N has completed all declared gradient-accumulation microsteps; the optimizer has applied the update; gradient buffers are empty (or an exact pending accumulation state is explicitly serialized by a versioned session contract); scheduler/scaler state reflects update N; RNG state reflects all random draws consumed through N; and the sampler/data cursor identifies the first not-yet-committed work unit. All model, optimizer, scheduler/scaler, RNG, cursor, recurrent carry and sparse-topology values must describe this same logical point.
- **Pause semantics:** pause requests a cooperative stop at the next safe update boundary, then commits a complete checkpoint before publishing PAUSED. The request has a policy-defined wait bound. If that bound expires before a safe boundary, return a typed pending/deadline-exceeded result and keep the state truthful; never interrupt an in-flight numerical update and label it PAUSED.
- **Stop/cancel semantics:** stop is terminal for that session and distinct from pause. It requests a cooperative safe-boundary stop, then applies the session's declared final-checkpoint policy before publishing STOPPED. A stopped session is not resumed under the same session ID; continuing from its checkpoint, where policy permits, creates a new session with explicit lineage. Coroutine cancellation is a control signal, not permission to serialize arbitrary mid-update tensors.
- **Resume** verifies the exact parent/candidate/session/dataset/admission/split/tokenizer/model/optimizer/scheduler/RNG/cursor/backend-compatibility identity before applying another update. Current policy and resource admission are rechecked.
- For recurrent/SSM families, resume also verifies the complete recurrent carry set (notably Mamba-2 convolution + SSM state, or RWKV WKV + TimeMix/ChannelMix shift state), its sequence/chunk identity, and the optimizer update that produced it. See [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md).
- A change to optimizer, precision, packing, gradient reduction, objective, chunk-carry/reset policy or full-vs-truncated BPTT creates a new session configuration; it is not a silent adaptive-workload adjustment.
- A chunk boundary is not automatically an independent document boundary or an optimizer-update boundary. The declared semantics determine whether recurrent state carries, resets, or carries gradients across that boundary.
- The session records and verifies the exact tokenizer manifest digest, profile ID and model configuration fingerprint. A tokenizer profile or vocabulary change requires a new generation/migration contract, never an in-place session mutation.
- Training never consumes raw acquisition responses or unreviewed personal captures.

### 5.5.1 Backend execution and Vulkan compute contract

The Training Engine consumes a versioned backend execution map, not a boolean "GPU available" flag.

~~~text
BackendCapabilityProvider.inspect(modelProfile, workload) -> BackendCapabilityReport
BackendExecutionPlanner.plan(operatorGraph, capabilityReport, precisionContract) -> BackendExecutionPlan
BackendRuntime.execute(plan, immutableInputs, mutableCandidateState) -> BackendExecutionResult
~~~

Requirements:
- CPU reference is the portable correctness baseline. It remains available when Vulkan/NPU discovery or execution fails, subject to the selected model's CPU resource envelope.
- Vulkan GPU compute is an independent backend from NPU. Discovery must report the actual loader/driver, physical-device identity, API version, compute queue, queried features/extensions/limits, supported storage/arithmetic dtypes and driver/pipeline cache identity.
- A Vulkan backend may be selected without an NPU when its compute queue and complete required operator path pass profile admission. A device without a Vulkan compute-capable driver uses CPU or another separately verified provider.
- FP32 is the baseline shader path. FP16 arithmetic and FP16 storage are independent optional capabilities; each pipeline declares both requirements and provides a verified FP32 or CPU fallback when its optional precision requirements are absent.
- The backend execution plan names every graph partition and each operator's provider, dtype, layout, synchronization points, host/device transfers and fallback reason. Unknown coverage remains UNKNOWN and cannot pass a required training gate.
- A full training plan includes forward, objective/loss, backward, gradient accumulation/reduction, clipping, optimizer update, state serialization and recovery. Inference-only Vulkan or NPU coverage is not training coverage.
- Backend numerical parity is measured against the CPU reference at the selected dtype/tolerance. A fallback must be visible in the execution result and telemetry; silent CPU fallback is prohibited.
- Device loss, command submission failure, pipeline compilation failure or numerical mismatch invalidates the affected step. Do not commit the optimizer update or data cursor; recover from the last durable checkpoint and re-plan only through an explicit compatible session policy.
- Memory admission includes GPU allocations/workspaces, shared-process residency, synchronization/transfer buffers, pipeline cache and any concurrent inference snapshot. A supported API version alone does not prove that the model graph fits or performs adequately.

Vulkan compute pipeline creation uses a compute-capable queue and compute dispatch commands; it does not provide automatic differentiation or optimizer semantics. Those remain Argus engine/operator responsibilities. See [21 — Device-Adaptive Model and Training](21_DEVICE_ADAPTIVE_MODEL_AND_TRAINING.md) for Android feature discovery and [42 — Architecture-Family Tensor and Operator Inventory](42_ARCHITECTURE_TENSOR_AND_OPERATOR_INVENTORY.md) for the kernel-level resource worksheet.


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
CREATED -> POLICY_CHECK -> DATASET_ADMISSION_CHECK -> RESOURCE_ADMISSION -> PREPARING -> QUEUED -> RUNNING

RUNNING -> PAUSE_REQUESTED -> SAFE_BOUNDARY_REACHED -> PAUSE_CHECKPOINT_COMMIT -> PAUSED
PAUSED -> RESUME_CHECK -> RESOURCE_ADMISSION -> QUEUED

RUNNING -> CHECKPOINTING -> RUNNING

RUNNING -> STOP_REQUESTED -> SAFE_BOUNDARY_REACHED -> FINAL_CHECKPOINT_COMMIT -> STOPPED

RUNNING -> INTERRUPTED
INTERRUPTED -> RECOVERY_VALIDATING -> RECOVERY_READY -> RESUME_CHECK -> RESOURCE_ADMISSION -> QUEUED
RECOVERY_VALIDATING -> RECOVERY_REJECTED -> FAILED

RUNNING -> FINALIZING -> FINAL_CHECKPOINT_COMMIT -> COMPLETED
RUNNING -> FAILED
~~~

The training session state machine ends at COMPLETED, STOPPED or FAILED. Candidate assessment, checkpoint validation and active-generation publication are independent records/state axes; they are not additional training-session states.

- **Safe optimizer-update boundary** is defined in section 5.5. A chunk, batch or epoch boundary is not automatically safe unless it satisfies the full optimizer/cursor/RNG/state consistency contract.
- **PAUSED** is acknowledged only after the next safe boundary has been reached and a complete resumable checkpoint has been committed. A pause request or an in-flight checkpoint is not yet a pause acknowledgement.
- Pause requests have a policy-defined wait bound. If the bound expires before a safe boundary, the workflow reports a typed pending/deadline-exceeded result; it does not force-cancel the numerical update or publish PAUSED falsely.
- **STOPPED** is an explicit terminal user/owner-requested outcome, distinct from PAUSED, INTERRUPTED and FAILED. It cannot be resumed under the same session ID. A continuation, when permitted, is a new session with explicit parent-checkpoint lineage.
- **INTERRUPTED** records that normal session ownership ended without a terminal commit, such as process reclamation, device restart or an unrecoverable backend interruption. It is not equivalent to a user-requested STOP.
- **RECOVERY_VALIDATING** checks a specific durable checkpoint before any training update is allowed. **RECOVERY_READY** means its integrity and exact session, candidate/parent, model, tokenizer, dataset/admission/split, optimizer/scheduler/scaler, RNG, sampler/cursor and required recurrent/sparse state identities match. A missing, partial or mismatched required component transitions to RECOVERY_REJECTED; the system must not guess a cursor or silently restart from initialization.
- **RESUME_CHECK** revalidates current policy, backend compatibility and resource admission. **RUNNING** is published only after the engine has actually resumed from the validated committed state.
- **COMPLETED** means the declared training schedule finished and its final checkpoint transaction committed. It describes that bounded run, not a permanently finished or universally capable model.
- **FAILED** carries a typed cause and recovery disposition. A failed final-checkpoint commit cannot be presented as completed; recovery starts from the last known durable checkpoint.

### 6.2.1 Independent candidate, checkpoint and active-generation states

The following are orthogonal facts with separate authoritative records. They must not be collapsed into one status field.

| Axis | Authoritative record/state | Allowed lifecycle or interpretation |
|---|---|---|
| Training session | application/learning session state | CREATED, policy/admission/preparation, QUEUED, RUNNING, PAUSED, INTERRUPTED, RECOVERY_VALIDATING/READY/REJECTED, COMPLETED, STOPPED or FAILED |
| Candidate assessment | Immutable ModelCandidateManifest plus append-only assessment records | STAGED -> TRAINED -> EVALUATED -> VERIFIED, or EVALUATED -> REJECTED. A verified candidate is not necessarily active. |
| Checkpoint validation | One CheckpointValidationRecord per checkpoint | UNVALIDATED -> VALID, CORRUPT or INCOMPATIBLE. Validation is checkpoint-specific, not candidate-wide. |
| Checkpoint restorability | Derived capability for a particular checkpoint | RESTORABLE only when the checkpoint is VALID, exact identities are compatible, required policy remains valid and current resource admission permits restoration. It is not a session or candidate lifecycle state. |
| Promotion transaction | PromotionTransaction | PREPARED -> COMMITTED, or ABORTED/CONFLICT. The transaction is idempotent and compares the expected active-pointer revision. |
| Active model | One ActiveGenerationPointer per local instance | The pointer alone determines the generation used for new inference snapshots. Existing immutable snapshots remain bound to their original generation. |

- The product may display **PROMOTED** only as a derived fact: the active-generation pointer currently references the candidate generation and its committed promotion transaction. PROMOTED is not a mutable candidate approval status.
- A checkpoint may be VALID but not currently RESTORABLE because current runtime conditions, policy or storage/resource admission do not permit restoration. A saved file alone proves neither.
- **TRAINED** does not mean EVALUATED; **VERIFIED** does not mean PROMOTED; checkpoint validity does not imply candidate quality.
- Training progress and recovery progress are independent measurements. Training progress uses committed optimizer updates or another declared work unit with an explicit numerator, denominator and unit. Recovery progress describes checkpoint discovery, integrity/identity validation or restore work. They must never share a counter, denominator or label.
- A percentage is shown only when its denominator is known and meaningful. For open-ended background learning, show committed units/rate or session-local work rather than inventing a percentage or implying the model is globally complete.
- A recovery screen may retain the last committed training position (for example, 64% of a declared session) while showing separate checkpoint-validation/restore work. Recovery reaching 100% does not mean the training session reached 100%.
- Status changes are rendered from the owning workflow snapshot and committed repository facts. UI animation, notification delivery, a local timer or a backend process callback cannot advance authoritative progress or lifecycle state.
- Backend and precision labels are evidence-bound. Show CPU/NEON, Vulkan GPU or NPU only when the selected training execution report confirms that provider for the relevant operators. Report training precision separately from inference precision; an INT8 inference path is not evidence of INT8 training.
- A “resumed” message is emitted only after RECOVERY_READY, RESUME_CHECK and the engine's transition to RUNNING. A “checkpoint saved” message is emitted only after its transaction commits and must not be used as a synonym for COMPLETED, VERIFIED or PROMOTED.

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

### 5.8 Dataset admission, candidate evaluation and promotion evidence

- The source artifact digest identifies imported bytes. The canonical dataset digest identifies the exact versioned normalized records exposed to the training reader. Both use a declared cryptographic digest (SHA-256 in the initial contract) over a canonical byte representation; hashCode(), filenames, row counts and display labels are not integrity fingerprints.
- Dataset admission is an immutable decision record bound to one DatasetManifest digest and validator/policy version. It records schema/integrity, parser/preprocessing, deduplication, source provenance/rights, consent where relevant, tokenizer compatibility, data-quality and leakage evidence. PENDING/APPROVED/REJECTED are decision outcomes, not mutable fields on the immutable dataset identity.
- Admission rules are task- and corpus-specific. Classification label counts/balance checks may be appropriate for a classifier but are not universal foundation-language-model requirements. Do not hardcode arbitrary minimum row counts, label counts, balance percentages or quality thresholds without evidence for the selected objective and corpus.
- Training, validation and test membership must be pinned by a DatasetSplitManifest with a digest for each split. Split at a semantically appropriate boundary (for example, document or conversation/source group) and record leakage checks. Validation may support explicitly declared model selection/early stopping; the final test split must not enter gradient updates or training-time selection.
- Candidate evaluation is a separate operation over an immutable candidate digest and pinned EvaluationProtocolManifest. For language-model objectives, select metrics appropriate to the objective (for example held-out NLL/perplexity and task-specific quality/regression checks); classification accuracy/F1 is not a universal gate. Do not adopt illustrative numeric thresholds as product policy without measured evidence and an approved verification policy.
- Reproducibility metadata includes evaluation dataset/test-split digests, tokenizer and model/configuration fingerprints, metric implementation versions, prompt/template revision, generation parameters and seeds where applicable, and runtime/backend/precision evidence. Each metric declares whether deterministic reproduction is expected and its accepted tolerance.
- The comparison baseline is the Active Generation captured at evaluation start, identified by generation ID and digest. Do not compare against an unpromoted candidate merely because it was trained immediately before the current candidate.
- Candidate verification creates a CandidateVerificationRecord bound to the exact candidate digest, evaluation report, policy version and pinned baseline. A passing decision establishes local policy eligibility; it is not remote attestation or a cryptographic proof of model quality, data rights or authorship.
- Promotion is a separate idempotent compare-and-swap transaction. It must fail with CONFLICT if the active pointer no longer matches the expected revision/parent used for verification; the candidate must be re-evaluated against the new active baseline or explicitly re-approved under a versioned policy.
- The active pointer is the authority for inference. A candidate, checkpoint or export artifact is not automatically eligible for inference merely because training finished, its dataset was approved or a checkpoint is restorable. Export is a separately authorized operation with its own policy and artifact identity.

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
7. **Safe-boundary cancellation:** user pause/stop and cooperative cancellation are commands to the session owner, not immediate coroutine/job destruction. They are applied at the next declared safe optimizer-update boundary. A process kill may bypass all cleanup, so recovery depends on prior durable checkpoints.
8. **Bounded finalization:** a process-scoped application workflow/session owner, not a ViewModel, owns training control. A short already-prepared commit may use a bounded non-cancellable section, but long serialization, tensor computation or checkpoint creation must not be made non-cancellable. Such finalization is best-effort only and never substitutes for periodic durable checkpoints.
9. **Bounded locks:** no network, parsing or long model computation runs while holding global model/store locks.
10. **Resource reservation:** inference, training, acquisition, indexing, dataset shards and checkpoint operations use separately accounted budgets. Admission includes peak candidate/optimizer/activation/workspace memory, current RAM headroom, thermal/battery/power constraints, dataset I/O, staged checkpoint size, retained previous checkpoint and storage reserve. A controller cannot allocate memory or storage already reserved for recovery or user data.
11. **Promotion isolation:** evaluation reads fixed candidate and baseline-active-generation digests; promotion rejects a candidate whose digest changed or whose expected active-pointer revision is stale.
12. **Consent revocation race:** a candidate cannot enter an optimizer update if its authorization is revoked before the training admission/update boundary. Revocation during a running session stops future use at the next safe update boundary and invalidates pending candidate membership.
13. **No hidden concurrent optimizer:** inference/training weight sharing, if ever allowed, requires an explicit snapshot/copy-on-write contract and measured memory admission. It is not the default.

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

A durable publication uses: (1) write to an immutable/isolated temporary object; (2) flush and sync file contents; (3) close; (4) verify size, schema, identity and cryptographic checksum by read-back; (5) commit a journal/manifest marker; (6) atomically publish a small pointer or recoverable transaction record; (7) retain the previous committed object until recovery confirms the new transaction; and (8) garbage-collect unreferenced temporary data only after recovery confirms it is not needed. Prefer immutable checkpoint IDs plus an atomic latest-committed-checkpoint reference so a failed replacement cannot destroy the last known-good checkpoint.

AndroidX AtomicFile.finishWrite() documents a file sync followed by rename/commit and explicitly provides no file-locking semantics; all concurrent access must be serialized by the caller. It does not document a parent-directory fsync guarantee. Linux fsync(2) documentation distinguishes syncing file content/metadata from syncing the containing directory entry. Therefore, the selected Android storage adapter must state its directory-entry durability assumptions, use parent-directory sync only where the platform/filesystem API supports it, and verify the behavior on target filesystems before claiming power-loss durability stronger than the documented AtomicFile contract. Read-back checksum and exact-identity validation are required before a checkpoint is considered committed/valid.

Deletion is a lineage operation: source record → parsed text → excerpts → summaries/embeddings/indexes → pending corpus candidates → training shards. If trained model weights contain influence from the deleted source, apply the separately approved model-retraining/rollback policy; do not assert exact machine unlearning.

## 11. Deployment and platform boundary

The initial deployment target is one Android application and one local installation namespace. Logical modules may ship in one APK/AAB; architecture boundaries are code-ownership and dependency boundaries, not mandatory separate processes.

- The core Genesis, model-state, tokenizer, training and memory paths must be designed to operate without a central service.
- Public-source acquisition is an optional, policy-controlled network path; offline inference and existing local knowledge must continue when the network is unavailable.
- Google Play services runtimes, vendor SDKs and external APIs are separately audited dependencies. Their telemetry, service-update, account, availability and offline characteristics must not be described as zero-network unless proven.
- CPU/GPU/NPU are execution backends selected by verified operator/runtime evidence. Device names or marketing TOPS do not create backend capability.
- Android Context, permissions, WorkManager, foreground services, PowerManager, SAF and accessibility/media projection APIs remain in platform adapters.
- No process priority, background execution or service restart policy may bypass Android platform restrictions.
- The accepted product trust boundary is local: dataset admission, evaluation, verification and promotion are decisions made by the local installation using locally stored evidence. SHA-256 binds bytes/identity; it does not prove source ownership, legal rights, semantic quality, honest execution or remote attestation. Do not imply that local evaluation is remotely certified or cryptographically attested.
- No central training service, remote weight exchange, gradient exchange or cross-instance promotion participates in the production learning path. Any future experimental remote adapter would require a separate explicit product/security decision and must not be wired into the accepted production composition graph.
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
| Personal capture and experience intake | [48 — Personal Capture and Experience Intake Contract](48_PERSONAL_CAPTURE_AND_EXPERIENCE_INTAKE_CONTRACT.md) | Specializes the capture-to-personal-experience record, consent, review, retention, revocation and training handoff without changing generic policy/training transactions |
| Acquisition and connectors | [24 — Autonomous Knowledge Acquisition](24_AUTONOMOUS_KNOWLEDGE_ACQUISITION.md), [26 — Source Registry](26_SOURCE_REGISTRY_AND_CONNECTOR_ARCHITECTURE.md) | Defines generic connector and provenance handoff |
| Architecture and tokenizer | [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md), [34 — Tokenizer Evaluation](34_TOKENIZER_EVALUATION_AND_DECISION.md), [35 — Android Component Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md), [38 — G2 Architecture Synthesis](38_G2_ARCHITECTURE_COMPARATIVE_SYNTHESIS.md), [39 — Transformer Component Research](39_TRANSFORMER_COMPONENT_RESEARCH_AND_DESIGN_CONTRACTS.md), [40 — Initialization and Width Scaling](40_INITIALIZATION_AND_WIDTH_SCALING_RESEARCH.md), [41 — Recurrent State Lifecycle](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md), [42 — Architecture Tensor/Operator Inventory](42_ARCHITECTURE_TENSOR_AND_OPERATOR_INVENTORY.md) | Keeps unresolved architecture/tokenizer values as versioned contract placeholders; family-specific research documents supply evidence and symbolic tensor inventories without overriding this architecture-neutral transaction contract |
| Empirical verification | [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md), [33 — From-Scratch Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md) | Maps invariants to later test suites, including E1–E7 |

## 14. Implementation verification obligations

The following are deliberately later tests. Their methods and acceptance criteria must be detailed in the implementation plan before coding is authorized.

| Test family | Required verification | Related contract |
|---|---|---|
| Contract/schema | Serialization round-trip, version migration, unknown field handling, malformed manifest rejection | Sections 4 and 8 |
| Genesis transaction | Crash at every stage, corrupt/truncated tensors, checksum mismatch, no partial active generation | Sections 5.2 and 6.1 |
| Instance isolation | Repeated independent installs/IDs; no cross-instance paths, state or gradient exchange | Sections 2 and 4 |
| Tokenizer/profile conformance | Profile selection and fallback before model tensor allocation; local tokenizer-training determinism/resource bounds where enabled; full reference/Android token-ID parity, Unicode, byte fallback, offsets and control-token injection | Section 5.3 and G1-V in document 34 |
| Inference isolation | Read snapshot remains stable during candidate training and promotion; memory admission under overlap | Sections 5.4 and 9 |
| Training correctness | Forward/loss/backward/optimizer reference parity, finite-difference checks, finite updates and exact update sequence | Section 5.5 and document 19 |
| Checkpoint recovery | Crash/corruption/low-storage injection; exact model/optimizer/RNG/sampler/cursor/idempotency resume | Sections 5.2, 5.5, 9 and E6 |
| Data rights/consent | Deny by default, operation-scope mismatch, revocation race, deletion cascade, training-candidate immutability | Sections 5.6–5.7 and 6.3 |
| Connector safety | SSRF/redirect/host validation, response-size limits, rate limits, prompt injection, invalid MIME/archive and retry idempotency | Sections 5.7 and 11 |
| Lifecycle/concurrency | Process death, background restriction, duplicate job, cancellation at safe boundary, competing inference/acquisition/training budgets | Sections 6 and 9 |
| Runtime/backend | CPU-reference parity; Vulkan loader/driver/compute-queue and per-feature discovery; FP32 fallback and optional FP16 arithmetic/storage validation; full training-operator coverage; actual graph partition/transfer/fallback report; device-loss and checkpoint-safe recovery; optional NPU provider behavior | Sections 5.4, 5.5.1, 11 and documents 08/15/21/42 |
| Candidate evaluation | Fixed digest, reproducible evaluation, regression failure, promotion race, rollback to last-known-good | Sections 5.2 and 6.2 |
| Structural evolution | Parent immutability, tensor mapping, initialization, optimizer-state migration, parent/candidate peak memory, candidate validation, promotion and rollback | Sections 5.2 and 5.2.1 |
| Dynamic sparse topology | Fixed-budget rewiring versus active-capacity growth, exact topology checkpoint/resume, optimizer warm-up, sparse representation accounting, deterministic update and backend parity | Section 5.2.1 and document 42 |
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
11. Structural-growth trigger metrics, allowed Transformer mutation catalog, growth/cooldown budgets and parent-child resource admission.
12. Sparse format, active-density/capacity policy, topology-update cadence, inactive-gradient scoring strategy, optimizer-state warm-up and Android sparse-kernel support.

A decision may remain open without making the surrounding module boundary or lifecycle contract incomplete. Every open item must have an owner, required evidence, decision forum/ADR and latest acceptable resolution point before the dependent implementation work.

## 16. Contract readiness criteria

The pre-implementation contract baseline is ready for review when:

- Every subsystem has a single owner and explicit dependency direction.
- Every cross-module handoff has a typed identity, version and failure result.
- UI observes workflow-owned immutable snapshots through the read-only state contract; notification navigation resolves a stable session route without owning session state; app/bootstrap uses an explicit composition root and constructor injection.
- Genesis, training, knowledge acquisition, candidate review and promotion have explicit state transitions.
- The active-generation immutability, single-writer, checkpoint and cancellation rules are consistent across documents.
- Data/consent states cannot be bypassed by connector, UI or worker implementation.
- All unresolved architecture/tokenizer/runtime values are clearly marked rather than guessed.
- Device-conditioned model/tokenizer selection is deterministic from versioned evidence and policy, and every generation pins one exact tokenizer artifact fingerprint.
- Requirements in document 01 map to a contract and a planned verification family.
- The later verification suite is explicitly separated from documentation completion.
- No statement implies these contracts already exist in the prototype.



## 17. Architecture-growth and dynamic-sparsity evidence

The following primary research establishes that the mechanisms exist, while their transfer to Argus's locally initialized Android language model remains unproven:

1. Rusu et al., [Progressive Neural Networks](https://arxiv.org/abs/1606.04671), 2016. Adds a new column for each task, freezes previous columns and uses lateral connections. It is a continual-transfer architecture, not a drop-in incremental Transformer block-growth algorithm; retained columns also accumulate inference/storage cost.
2. Yoon et al., [Lifelong Learning with Dynamically Expandable Networks](https://arxiv.org/abs/1708.01547), 2017. Selectively retrains shared units and expands/splits units when sequential-task capacity is insufficient. Its task-incremental benchmarks do not prove general-purpose language-model growth on phones.
3. Wei et al., [Network Morphism](https://proceedings.mlr.press/v48/wei16.html), ICML 2016. Studies depth/width and other network transformations intended to preserve the parent function. The reported scope is classic and convolutional networks; Transformer-specific equivalence and optimizer migration require separate derivation and verification.
4. Evci et al., [Rigging the Lottery: Making All Tickets Winners](https://proceedings.mlr.press/v119/evci20a.html), ICML 2020. Dynamic topology updates can train sparse networks under a fixed parameter/computation budget. Fixed-budget rewiring must not be described as monotonic capacity growth.
5. Lasby et al., [Dynamic Sparse Training with Structured Sparsity](https://proceedings.iclr.cc/paper_files/paper/2024/hash/8c5f30296296d2ae402ebbd09aaa9c12-Abstract-Conference.html), ICLR 2024. Structured RigL targets hardware-executable N:M-like patterns and reports acceleration on its evaluated CPU/GPU systems; those measurements are not Android Mali/Vulkan evidence.
6. Xiao et al., [Memory-Efficient LLM Training with Dynamic Sparsity: From Stability to Practical Scaling](https://proceedings.mlr.press/v306/xiao26t.html), ICML 2026. Reports topology-update loss spikes from newly regrown parameters under naive Adam state and proposes optimizer warm-up, density-aware learning-rate scaling and active-only gradient/optimizer state. It is not a mobile-device benchmark; its optimizer findings are a required risk to investigate, not a ready-made Argus optimizer.
7. Wang et al., [CoPE: Continual Probe-guided Expansion for Large Vision-Language Models](https://proceedings.mlr.press/v306/wang26le.html), ICML 2026. Uses probe experts to decide when/where to add experts and a task locator for routing across eight VQA tasks. It is adjacent evidence for selective expert expansion, not proof of random-init local language pretraining or mobile feasibility.
8. Zhang et al., [Grow-on-Demand: Sparse and Adaptive Expert Expansion for Continual Instruction Tuning](https://doi.org/10.1609/aaai.v40i34.40077), AAAI 2026. Adds experts only when a demand detector calls for them during continual instruction tuning. Treat its scope as task-adaptation evidence; it does not settle Argus's from-scratch Genesis or local-device training requirements.

**Current disposition:** this document supplies the missing architecture-neutral contract draft. It does not by itself close the overall documentation phase, accept the architecture/tokenizer ADRs, approve corpus sources, authorize implementation or establish empirical Android feasibility.
