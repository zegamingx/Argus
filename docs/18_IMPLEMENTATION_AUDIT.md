# Implementation Audit — P0 Static Verification

**Repository:** `zegamingx/Argus`  
**Historical source-audit branch:** `master`  
**Current documentation branch:** `main`  
**Source baseline audited:** `4d8104d4400dcd858e648d7b08ac0718385733d3`  
**Historical audit branch head:** `b1f41db3166e383b84ef0716ee4f43e7544744c5`  
**Current main cross-check:** `aec1f57077dcfa7bc32957f31d01e5454b5395b6`  
**Source continuity check:** No application-source paths changed between the audited source baseline and the cross-checked `main` HEAD.  
**Source audit date:** 2026-11-03 · **Documentation continuity review:** 2026-11-04  
**Review type:** Static source/configuration inspection; no code changes  
**Execution status:** Build, automated tests, emulator tests and physical-device tests were not run.

The two commits between the source baseline and the audit branch head changed documentation only. The application source was therefore re-read at the unchanged source baseline; the audit branch head is recorded separately to avoid treating a documentation commit as a source-code revision.

## 1. Executive summary

The current application is an early Android/Kotlin prototype, not yet an implementation of the complete target architecture. The highest-risk gaps are training correctness and persistence, consent/data governance for automatically captured screen content, and inaccurate hardware/runtime capability reporting.

**Audit disposition: NOT READY for implementation of the broader target architecture.** Resolve the correctness and privacy blockers first. Keep unsupported GPU/NPU, multi-agent, persistent-memory and multimodal capabilities explicitly classified as planned or unverified.

## 2. Findings

### AUD-001 — Training update is not a mathematically complete cross-entropy backpropagation path

**Severity:** CRITICAL  
**Evidence:** `app/src/main/java/com/argus/agi/model/ArgusNeuralEngine.kt`, `trainStep()`

The method computes a cross-entropy-like scalar loss, but the parameter update does not implement the corresponding full gradient:
- It updates only the output-head row for the target token; gradients for the other vocabulary logits are not applied.
- The output-head update uses the input embedding row for the current token. The output logits are produced from the final hidden state, so the output-head gradient should use that hidden state.
- No gradient is propagated back through the hidden state, attention, feed-forward layers, embeddings or LoRA projections.
- The LoRA matrices are not updated by this training method, despite the comments and product direction describing LoRA learning.

Consequently, the current method must not be presented as full language-model fine-tuning or as a correct LoRA training implementation. A decreasing scalar loss alone would not establish training correctness.

**Required action:** Replace or explicitly relabel this prototype update. Before using it for learning, implement the complete gradient path for the supported trainable parameters and validate analytical gradients against finite differences and a trusted reference implementation.

### AUD-002 — Checkpoint does not persist the parameters changed by training

**Severity:** CRITICAL  
**Evidence:** `ArgusNeuralEngine.kt`, `trainStep()`, `saveCheckpoint()`, `loadCheckpointIfExists()`

The training method mutates `wHead`, but the checkpoint serializes only `loraQa`, `loraQb`, `loraVa`, `loraVb` and `adamStep`. The current training method does not update those LoRA matrices. Base embeddings, output-head weights, attention/feed-forward weights, optimizer moments and dynamically registered tokenizer vocabulary are not persisted.

The result is that the parameter changes made by the current training path are lost when the process restarts. The engine also initializes its non-checkpointed weights randomly, so a fresh process does not reconstruct the same model state.

Checkpoint writing is a direct file overwrite with swallowed exceptions; there is no visible versioned manifest, checksum or temporary-file/atomic-replacement protocol.

**Required action:** Define a versioned model-state format covering every trainable/frozen tensor, tokenizer state and optimizer state required for resume. Write atomically, validate lengths/version/checksum, and test process-kill recovery before claiming durable learning.

### AUD-003 — Captured screen text enters automatic training without a review/approval gate

**Severity:** CRITICAL — privacy and data governance  
**Evidence:** `service/ArgusAccessibilityService.kt`, `model/TrainingManager.kt`, `sensor/ExcludedAppsManager.kt`

When the AccessibilityService is enabled, it traverses the active window's accessibility tree, extracts text, and passes up to 400 characters to `TrainingManager.enqueueData()`. The training worker starts automatically and consumes queued items without a separate user approval step. There is no PII/OTP/credential redaction stage in this path.

The exclusion manager has a small default package-name list and a user-editable exclusion set. A short package list is not a reliable guarantee that all banking, authentication, password-manager or other sensitive surfaces are excluded. The inspected Settings screen exposes this exclusion list but no separate app-level consent for screen perception, retention of captured text or training from captured text. The Perception screen reports whether AccessibilityService is active; that service state is not a training-consent record. In addition, `WorldModel.updateScreenContext()` stores a 120-character screen-content snippet in its in-memory entity map before any candidate review or training approval.

This conflicts with the documented learning pipeline, which requires redaction and explicit user approval before training, and with the security document's sensitive-data minimization requirements.

**Required action:** Default capture-to-learning to disabled. Separate capture, candidate creation, review, approval and training. Add deterministic redaction and fail-closed handling for unknown/sensitive contexts. Do not treat an exclusion list as a complete security boundary.

### AUD-004 — Other learning sources also bypass the documented review workflow

**Severity:** HIGH  
**Evidence:** `ui/ArgusViewModel.kt`, `sendMessage()`, `importFileContent()`, `importImageContent()`, `searchWebDirectly()`; `model/TrainingManager.kt`

Unresolved chat queries, retrieved web snippets, imported document text, image-derived placeholder text and direct web-search snippets are sent straight to the training queue. The queue worker trains every item automatically. No candidate approval state, provenance review, dataset inspection or redaction decision is enforced between enqueue and training. The Neural Core UI describes the queue as collecting approved inputs and waiting for approved input, but `TrainingManager` has no approval state or authorization check; the UI wording therefore overstates the enforcement that exists in code.

**Required action:** Use a candidate store with provenance, sensitivity labels, preview, explicit approval/rejection and deletion. Web snippets must remain untrusted evidence and must not become training data merely because a query was unresolved.

### AUD-013 — Runtime vocabulary growth can desynchronize optimizer-state dimensions

**Severity:** CRITICAL — tensor-shape and optimizer safety  
**Evidence:** `Tokenizer.encode(autoRegister = true)`, `ArgusNeuralEngine.ensureVocabCapacity()`, `trainStep()`

The tokenizer can register tokens at runtime and expand the embedding/output-head tensors, while the optimizer moment arrays remain allocated at the original vocabulary size. A newly registered target token can therefore index beyond the optimizer-state arrays and terminate the training worker.

**Required action:** Freeze the tokenizer vocabulary and fingerprint for each model generation, with explicit unknown-token or byte-fallback behavior. Any future vocabulary change must be a new, validated generation migration that updates every dependent tensor and optimizer state atomically. The detailed source evidence and acceptance criteria are in P0-D below.
### AUD-014 — Prototype model state is not bound to a complete, durable Genesis identity

**Severity:** CRITICAL — model identity and training validity  
**Evidence:** `app/src/main/java/com/argus/agi/model/ArgusNeuralEngine.kt`; inspected repository tree

The engine initializes its embedding, attention, feed-forward and output-head matrices with `randomVector(...)`. Its constructor calls `loadCheckpointIfExists()`, but that loader restores only the four LoRA matrices and `adamStep`; it does not restore the complete model state, tokenizer contract or required optimizer state. No complete model-generation manifest and tensor-state loader was found in the inspected repository tree.

**The absence of an externally pretrained base-model bundle is not a defect under the accepted Local Knowledge Genesis direction.** Local parameter initialization is intentional. The defect is that the prototype cannot durably identify, validate, restore and resume the complete locally initialized generation. A checkpoint cannot establish the exact architecture/tokenizer identity or reconstruct all model and optimizer state needed for valid continuation.

**Required action:** Define and implement a versioned Genesis manifest and complete model-state persistence contract. Bind the instance and generation IDs, architecture/configuration fingerprint, immutable tokenizer fingerprint, full tensor inventory, initialization policy/metadata and integrity checksums. Persist and restore all mutable model and optimizer state required by the selected training design through an atomic, validated generation/checkpoint transaction.

### AUD-005 — On-device and zero-cloud UI wording conflicts with actual network behavior

**Severity:** HIGH — privacy transparency  
**Evidence:** `ui/ArgusViewModel.kt`, `sendMessage()`; `sensor/WebSearchEngine.kt`; `ui/screens/ChatScreen.kt`

Local neural inference is performed on-device, but unresolved chat prompts are sent to public search endpoints through `WebSearchEngine`. The chat UI simultaneously displays labels such as `100% On-Device` and `ZERO CLOUD`. Those labels can be read as a claim that the entire interaction remains on-device, which is not true for the web-retrieval path.

**Required action:** Distinguish local inference from network retrieval in the UI. Disclose when a query will be sent externally, identify the destination/purpose, and require the user-controlled network action specified by the security requirements.

### AUD-006 — Hardware reporting is heuristic, not evidence of accelerator execution

**Severity:** HIGH  
**Evidence:** `sensor/HardwareProfile.kt`

NPU/APU labels are inferred from SoC manufacturer or board strings. The GPU label is inferred from the Vulkan hardware-version feature and a version comparison. The inspected code does not create a Vulkan compute device/queue, load a vendor NPU runtime, validate operator/precision coverage or execute a representative model on an accelerator.

The documentation already describes this as heuristic discovery; UI labels must preserve that distinction and must not imply a verified active backend.

**Required action:** Separate detected hardware identity, runtime availability, supported model/operator capability and the backend actually used for a specific operation. Only report acceleration after a representative execution and correctness check.

### AUD-007 — Thermal admission control can report a safe state when temperature data is unavailable

**Severity:** HIGH — long-running workload safety  
**Evidence:** `sensor/DeviceGuard.kt`

The guard reads battery temperature from the battery-sticky broadcast, substitutes 30°C when the field is unavailable, and applies a fixed 42°C threshold. It does not inspect Android's thermal status/severity signal. A missing battery-temperature value can therefore be interpreted as a safe temperature.

**Required action:** Treat unavailable telemetry as unknown, not as a measured safe temperature. Integrate platform thermal status where available, use conservative fallback behavior and test thermal throttling/recovery on physical devices.

### AUD-008 — Training and model state are mostly process-local

**Severity:** HIGH — reliability  
**Evidence:** `model/TrainingManager.kt`, `model/WorldModel.kt`, `model/ArgusNeuralEngine.kt`

The training queue, current item, completed items, loss history and step counter are in-memory state. `WorldModel` stores entities in a process-local concurrent map. There is no durable job ledger or persistent episodic/semantic memory implementation in these classes. A process restart loses the queue/history/world entities, while the current model checkpoint does not preserve the weights changed by training (see AUD-002).

**Required action:** Specify which state must survive process death, persist it transactionally and test cancellation, process kill, OS reclaim and recovery. Keep the current in-memory prototype clearly labelled until then.

### AUD-009 — Model inference and training can access shared mutable weights concurrently

**Severity:** HIGH — numerical consistency  
**Evidence:** `model/ArgusNeuralEngine.kt`

`trainStep()` is synchronized, but `forward()` is not guarded by the same synchronization or by an immutable parameter snapshot. Generation calls `forward()` while the training worker can mutate output weights. This allows inference to observe a partially updated parameter row or a mixed parameter state.

**Required action:** Introduce a consistent read/write policy (for example immutable parameter snapshots with atomic publication, or a carefully scoped read/write lock) and add concurrent training/inference stress tests.

### AUD-010 — Product and onboarding claims exceed the implementation evidence

**Severity:** HIGH — product accuracy  
**Evidence:** `README.md`; `ui/ArgusViewModel.kt`; current source tree

The README's product-direction table describes a running multi-agent system and continual learning, while the inspected source contains a single experimental neural engine, a queue worker and an in-memory world model. The current document/image import functions do not pass document contents or image pixels into a multimodal inference model: document text is queued for training, and image import creates descriptive metadata from image dimensions. No OCR/VLM or vector-retrieval implementation is evidenced in the inspected source tree.

The README's own prototype disclaimer and `docs/17_CURRENT_REPOSITORY_BASELINE.md` are more accurate than the stronger product-direction claims and checked roadmap item for RAG.

**Required action:** Make every feature statement use explicit implemented/prototype/planned/unverified status. Mark RAG, multi-agent orchestration, multimodal inference and continual learning as planned until the code and tests prove them.

### AUD-011 — Baseline document metadata is stale, and English-only cleanup is incomplete

**Severity:** MEDIUM  
**Evidence:** `docs/17_CURRENT_REPOSITORY_BASELINE.md`; `model/WorldModel.kt`

The baseline document identifies branch `main` and an earlier inspected commit rather than the current `master` audit head. The current `WorldModel.kt` also retains Indonesian user-facing strings, including `Deteksi` and `Ekspresi logika/aritmatika diverifikasi oleh reasoning engine.`, despite the repository's English-only cleanup objective.

**Required action:** Refresh the baseline record with the current branch/commit and rerun localization checks across all user-visible strings and comments that are in scope.

### AUD-012 — Automated test coverage does not cover the critical architecture

**Severity:** HIGH — verification gap  
**Evidence:** `app/src/test/java/com/argus/agi/ArgusAppRobolectricTest.kt`; repository tree

The inspected test tree contains one test file with two tokenizer tests. No tests were found for neural numerical correctness, gradient validation, checkpoint round-trip/recovery, training queue lifecycle, data redaction/approval, exclusion policy, world-model persistence, hardware capability reporting or concurrent inference/training.

The Gradle build and tests were not executed during this review, so compilation and runtime behavior remain unverified.

**Required action:** Establish a reproducible build first, then add focused unit, numerical, integration, security and device tests. Do not use the current tokenizer tests as evidence that the neural/training architecture is verified.

## 3. Recommended remediation order

1. **P0 — Architecture-agnostic Genesis identity and initial-generation transaction:** AUD-014. Define the instance/generation identity and atomic manifest boundary without selecting a backbone or tokenizer.
2. **P0 — From-scratch feasibility envelope:** complete the research-backed and analytically established model/data/compute/resource constraints. E1–E7 physical verification remains a later implementation gate.
3. **P0 — Immutable tokenizer/vocabulary contract:** complete G1 and bind vocabulary/tensor dimensions to the generation; address AUD-013. Tokenizer-sensitive architecture choices remain blocked until the selected contract is available.
4. **P0 — Architecture-specific learning mathematics:** define complete gradients and optimizer semantics for the architecture/configuration selected through the research gates; address AUD-001.
5. **P0 — Exact state durability and recovery:** define full model/optimizer/session checkpoint state and atomic recovery after the tensor inventory is known; address AUD-002.
6. **P0 — Data governance and approved-input lifecycle:** enforce source rights/provenance and personal-data consent/classification/redaction/review/revocation/deletion; address AUD-003 and AUD-004.
7. **P1 — Worker, concurrency, lifecycle and durability:** address AUD-008 and AUD-009.
8. **P1 — Hardware/thermal capability correctness:** address AUD-006 and AUD-007.
9. **P1 — Product claims and user disclosure:** address AUD-005 and AUD-010.
10. **P2 — Baseline/localization/test completeness:** address AUD-011 and AUD-012.

This is the dependency order for design and remediation contracts, not a rule that parallel research workstreams must wait. Source implementation remains unauthorized; physical-device feasibility verification is deferred to the separately authorized implementation phase.

Do not begin broad GPU/NPU or multi-agent implementation before the P0 items have a reviewed design, acceptance criteria and tests.

## 4. Scope and limitations

This review inspected the repository tree, Gradle configuration, manifest, core model/training code, hardware profile, world model, perception/accessibility services, ViewModel, selected UI screens, tests and architecture documents at the audited commit.

This is a static source review. It does not prove behavior on a device, compile success, runtime crashes, Android-version compatibility or vendor SDK availability. Those require build execution, automated tests and physical-device verification.

No source code or runtime behavior was changed as part of this audit report.


## 5. P0 deep-dive and acceptance gates

The following details narrow the P0 findings to observable code behavior and define what must be proven before source remediation is accepted.

### P0-A — Training gradient contract (AUD-001)

For mean next-token cross-entropy over N valid target positions, the expected output-head gradient is:

`dL/dz[t,v] = softmax(z[t])[v] - 1(v = target[t])`

`dL/dW_head[v,d] = (1/N) * sum_t(dL/dz[t,v] * hidden[t,d])`

The final hidden vector that generated each logit row is the relevant feature vector. In the current implementation:
- Only the target-token probability is used. The required gradients for all non-target vocabulary rows are omitted.
- The update multiplies by the input embedding rather than the final hidden representation used by the output projection.
- No gradient is propagated through the output projection into the transformer, embedding or LoRA parameters.
- `adamStep` increments once per text item, but the optimizer moments/weights are mutated repeatedly inside the token-position loop. Gradients are not accumulated and normalized as one sequence-level update, and all logits were computed before those in-loop mutations.
- The allocated embedding optimizer moments are not used by this update. The LoRA matrices are not updated.

This is not a valid implementation of the documented full cross-entropy/LoRA learning path. It must remain disabled or explicitly classified as a non-learning prototype until a supported trainable parameter set has a mathematically complete backward path.

**Acceptance gate:**
1. Freeze and document the trainable parameter set, loss reduction, optimizer semantics and sequence/batch behavior.
2. Use deterministic small tensors and compare forward/loss/gradients against a trusted reference.
3. Run finite-difference gradient checks for every trainable parameter group, including LoRA A/B if LoRA training is claimed.
4. Verify optimizer updates use the intended mean/sum reduction, bias-correction step count, clipping and update boundaries.
5. Verify a controlled training fixture changes the intended parameters and improves its held-out objective without corrupting unrelated tensors.
6. Add regression tests for empty input, one-token input, maximum sequence length, new vocabulary entries and non-finite values.

### P0-B — Model-state and checkpoint contract (AUD-002)

**Genesis alignment:** the following source observations remain valid, but the target contract is a locally initialized Genesis generation, not an imported pretrained base-model artifact.

The current constructor initializes embedding, positional, attention, feed-forward and output-head weights randomly. No pretrained model-weight loader is evidenced in the inspected class. The checkpoint writes only the four LoRA matrices and the Adam step, even though the current update mutates the output head. It also omits optimizer moments and dynamic tokenizer vocabulary.

The current checkpoint path additionally:
- overwrites the target file directly rather than writing a validated temporary file and atomically promoting it;
- accepts files whose length is greater than or equal to the expected length without a format/version/checksum contract;
- catches every exception and discards the failure, so callers cannot know whether persistence succeeded;
- can load fields into live arrays before the entire checkpoint has been validated.

A checkpoint that cannot reconstruct the exact model and optimizer state is not a resume checkpoint.

**Acceptance gate:**
1. Define an explicit versioned manifest: architecture/configuration fingerprint, tokenizer/vocabulary state, tensor names/shapes/dtypes, trainable/frozen state, optimizer and scheduler state, step counters, checksum and provenance.
2. Validate the complete file before mutating live model state.
3. Write to a temporary file, flush/sync as appropriate, and atomically replace the last-known-good checkpoint on the same filesystem.
4. Surface typed save/load failures; never silently report training completion when checkpoint persistence failed.
5. Test bit-identical state/logits after round-trip, plus truncated, oversized, corrupt, wrong-version, incompatible-architecture and interrupted-write cases.
6. Prove recovery after process termination at each checkpoint-write stage.

### P0-C — Capture consent, classification and approved training data (AUD-003/AUD-004)

The current `TrainingStatus` enum has only `QUEUED`, `TRAINING`, `COMPLETED` and `PAUSED`. `TrainingManager.init` starts the worker automatically; `enqueueData` accepts raw text; the worker consumes every queued item. There is no review/approval state in this path.

The AccessibilityService checks the event's package name against a manually maintained exclusion set, then reads `rootInActiveWindow` without verifying that the root node belongs to that same package. The recursive traversal reads node text/content descriptions without an explicit password-node filter. The observed flow has no PII/OTP/credential redaction before `WorldModel` storage or training enqueue.

The Android AccessibilityService enablement is not equivalent to informed consent for using captured text to update model parameters. An opt-in for screen awareness and a separate opt-in for learning from captured content are distinct choices.

**Acceptance gate:**
1. Make screen capture off by default and provide a clear, revocable in-app control with visible active state.
2. Separate permission to perceive from permission to retain and permission to train; training consent must be explicit and scoped.
3. Fail closed when event/window identity cannot be established. Require the active root package to match the event package before reading any node.
4. Exclude password-designated nodes and sensitive/secure surfaces; unknown or unclassifiable content must not be persisted or trained.
5. Redact secrets, credentials, OTPs and configured sensitive data before candidate storage. Redaction must be tested against adversarial fixtures.
6. Replace raw-text enqueue with a typed, provenance-bearing candidate. The training API must accept only a candidate that has passed classification, redaction, user approval and dataset-version checks.
7. Provide inspect, reject, revoke and deletion paths; prove rejected/unapproved content never reaches the optimizer.
8. Test package/window mismatch, password fields, app exclusion, consent revocation during capture, process restart and queue replay.

### P0-D — Detailed evidence and acceptance gate for AUD-013 vocabulary/optimizer-state shape

**Severity:** CRITICAL  
**Evidence:** `Tokenizer.encode(autoRegister = true)`, `ArgusNeuralEngine.ensureVocabCapacity()`, `trainStep()`

The tokenizer can dynamically register characters absent from its initial seed vocabulary. `ensureVocabCapacity()` expands `wEmbed` and `wHead`, but the optimizer moment arrays `mHead`, `vHead`, `mEmbed` and `vEmbed` are allocated once using the initial vocabulary size and are not expanded with the weights.

When a newly registered token is used as a target, `trainStep()` can index `mHead[target]` and `vHead[target]` beyond their array bounds. The worker does not catch failures around `neuralEngine.trainStep()`; such an exception can terminate the training coroutine instead of recording a recoverable failed item.

**Required action:** Make vocabulary and all dependent tensor/optimizer-state shapes change atomically, or adopt a fixed vocabulary with explicit unknown-token behavior. Validate IDs and shape invariants before every update. Convert per-item worker exceptions into explicit failure states without silently killing the worker.

**Acceptance gate:**
1. Freeze the vocabulary for each model revision and use explicit unknown-token behavior at runtime; or, if dynamic growth is retained, resize model tensors and every dependent optimizer tensor as one validated state transition.
2. Validate tokenizer fingerprint and every tensor/moment dimension before forward or optimizer execution.
3. Add deterministic tests for unseen Unicode, non-ASCII input, vocabulary/model-version mismatch and checkpoint compatibility.
4. Prove that an invalid sample is marked failed without killing the worker, while non-finite or partially applied updates stop the training session safely.

The recommended first implementation is an immutable vocabulary tied to the model-generation manifest and tokenizer fingerprint. Dynamic vocabulary expansion should remain unsupported until its state migration is designed and tested.

## 6. Build and Actions execution policy

The repository must not be built or have GitHub Actions workflows triggered while the architecture audit and required remediation are still in progress.

- Do not run Gradle build, test, lint or packaging tasks as a substitute for completing the audit.
- Do not manually dispatch workflows or trigger Actions on branch pushes/PRs during this phase.
- Do not create, upload or publish workflow build artifacts.
- Once the audit/remediation is complete and the final verification gate is explicitly accepted, build only from an immutable Git tag that identifies the exact reviewed commit.
- Any future build workflow must be tag-triggered and must not contain artifact-upload/publishing steps unless separately authorized.

This is a process constraint, not a claim that the current code has passed build or test verification.

## 7. P0 status

| P0 item | Status | Evidence required to close |
|---|---|---|
| AUD-001 training gradient correctness | OPEN | Reference parity, finite-difference checks and optimizer regression tests |
| AUD-002 checkpoint integrity | OPEN | Versioned atomic checkpoint and recovery test suite |
| AUD-003 capture privacy/consent | OPEN | Consent, identity validation, redaction and exclusion tests |
| AUD-004 approval workflow | OPEN | Candidate lifecycle tests proving only approved records train |
| AUD-013 vocabulary/optimizer shape | OPEN | Fixed-vocabulary or atomic resize contract, tensor invariants and worker-survival tests |
| AUD-014 Genesis model identity and deterministic state restoration | OPEN | Versioned Genesis manifest, complete tensor inventory, per-instance identity, initialization metadata, full-state persistence and model/tokenizer compatibility tests |

No P0 source remediation has been performed in this audit update. No build, test, lint, package task or GitHub Actions workflow was run.


## 8. Verification pass 2 — source-level confirmation

This historical pass re-opened the model, tokenizer, training manager, accessibility service, application wiring, ViewModel and perception UI on the then-audited `master` snapshot. It confirms the following implementation facts:

- `ArgusNeuralEngine` creates its base embedding, attention, feed-forward and output-head matrices from random values. Its checkpoint loader restores LoRA arrays and the optimizer step only; there is no complete base-model artifact loader in the inspected tree. This confirms AUD-014 and strengthens the model-state concern in AUD-002.
- `Tokenizer.encode(..., autoRegister = true)` can grow the vocabulary at runtime. `ensureVocabCapacity()` grows `wEmbed` and `wHead`, but `mHead` and `vHead` remain sized to the original vocabulary. A newly registered target token can therefore cause an out-of-bounds optimizer-state access during `trainStep()`. This confirms AUD-013.
- `TrainingManager.init` launches its worker immediately. `enqueueData(source, title, content)` accepts raw content, and the worker calls `trainStep()` without approval validation or per-item exception recovery. This confirms the core failure mode in AUD-003/AUD-004 and the worker-survival part of AUD-013.
- `ArgusAccessibilityService` checks only the event package against a short exclusion set before reading `rootInActiveWindow`. It does not verify that the active root belongs to the event package/window, does not reject password-designated nodes, and sends extracted text directly to the training manager and world model. The exclusion set is not an allowlist and cannot establish informed training consent.
- `PerceptionScreen` exposes the system Accessibility Settings entry and displays whether the service instance is active. No separate app-level consent for using captured screen text as training data was found.
- `ArgusViewModel` sends unresolved chat prompts, retrieved web snippets, imported document text, image-derived placeholder text and direct web-search snippets to the same raw-text enqueue API. There is no candidate-review gate on those paths.

### Additional P0 design constraint

Training must not be re-enabled merely by fixing the array bounds or checkpoint format. The complete prerequisite chain is:

1. A verified Genesis model identity/state manifest and matching immutable tokenizer.
2. A declared trainable-parameter set and mathematically complete backward pass.
3. A checkpoint that reconstructs the exact model generation, optimizer and tokenizer identity.
4. A typed candidate lifecycle that enforces classification, redaction, explicit approval and revocation.
5. A worker that consumes only approved, version-matched records and fails closed on corruption or non-finite updates.

The P0 correctness/remediation gates are specified in [19 — P0 Remediation Design](19_P0_REMEDIATION_DESIGN.md); the wider architecture-neutral module/API, lifecycle, state, event/error, concurrency and transaction contracts are consolidated in [36 — Pre-Implementation Module and Lifecycle Contracts](36_PRE_IMPLEMENTATION_MODULE_AND_LIFECYCLE_CONTRACTS.md). Both are proposed design artifacts, not implementation evidence or closed audit findings.

No application source was modified in this pass. All six P0 items remain OPEN. No build, test, lint, packaging task, GitHub Actions workflow or artifact upload was run.


### P0-C clarification — screen-context retention is a separate data path

The AccessibilityService writes captured text to both the training queue and `WorldModel.updateScreenContext()`. Blocking only `TrainingManager.enqueueData()` would therefore not fully contain unapproved screen-data retention. The remediation must gate the world-model/context write independently and distinguish transient context from retained candidate data. A UI status label or excluded-app checkbox is not a substitute for an enforceable consent state.

## 8. Architectural decision update — Local Knowledge Genesis

**Decision date:** 2026-11-03  
**Status:** Accepted design direction; implementation remains unverified.

The original AUD-014 wording assumed Argus would load a separately pretrained base-model artifact. That assumption has been superseded by the accepted **Local Knowledge Genesis** direction:

- Argus intentionally begins with locally initialized parameters for each independent instance.
- Absence of an externally pretrained base-model bundle is no longer a product defect.
- The actual defect remains: the current prototype does not persist and restore a complete, verifiable model state, tokenizer contract and required optimizer state.
- The replacement acceptance requirement is a versioned Genesis manifest, complete tensor inventory, architecture/tokenizer fingerprints, per-instance identity, initialization metadata and atomic full-state persistence.
- Random initialization must not be presented as learned language capability. General learning remains a separate background phase after app readiness.

The historical source observations in AUD-014 and P0-B remain valid for the inspected prototype. Their former recommendation to select an imported pretrained artifact is not a current Argus design requirement.

The audit's proposed Q/V LoRA-only training path is superseded as a target assumption. The initial trainable parameter set and optimizer must be researched against the architecture selected for from-scratch learning; no LoRA-only contract is approved.

This addendum changes the target design, not the inspected code, and does not close AUD-001, AUD-002, AUD-013, AUD-014 or any privacy finding.
