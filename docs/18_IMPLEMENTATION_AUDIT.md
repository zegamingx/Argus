# Implementation Audit — Initial Static Review

**Repository:** `zegamingx/Argus`  
**Branch:** `master`  
**Audited commit:** `4d8104d4400dcd858e648d7b08ac0718385733d3`  
**Review date:** 2026-11-03  
**Review type:** Source/configuration inspection (static; no code changes)  
**Execution status:** Build, automated tests, emulator tests and physical-device tests were not run.

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

The exclusion manager has a small default package-name list and a user-editable exclusion set. A short package list is not a reliable guarantee that all banking, authentication, password-manager or other sensitive surfaces are excluded.

This conflicts with the documented learning pipeline, which requires redaction and explicit user approval before training, and with the security document's sensitive-data minimization requirements.

**Required action:** Default capture-to-learning to disabled. Separate capture, candidate creation, review, approval and training. Add deterministic redaction and fail-closed handling for unknown/sensitive contexts. Do not treat an exclusion list as a complete security boundary.

### AUD-004 — Other learning sources also bypass the documented review workflow

**Severity:** HIGH  
**Evidence:** `ui/ArgusViewModel.kt`, `sendMessage()`, `importFileContent()`, `importImageContent()`, `searchWebDirectly()`; `model/TrainingManager.kt`

Unresolved chat queries, retrieved web snippets, imported document text, image-derived placeholder text and direct web-search snippets are sent straight to the training queue. The queue worker trains every item automatically. No candidate approval state, provenance review, dataset inspection or redaction decision is enforced between enqueue and training.

**Required action:** Use a candidate store with provenance, sensitivity labels, preview, explicit approval/rejection and deletion. Web snippets must remain untrusted evidence and must not become training data merely because a query was unresolved.

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

1. **P0 — Learning correctness and checkpoint integrity:** AUD-001 and AUD-002.
2. **P0 — Privacy/data governance:** AUD-003 and AUD-004.
3. **P1 — Concurrency, lifecycle and durability:** AUD-008 and AUD-009.
4. **P1 — Hardware/thermal capability correctness:** AUD-006 and AUD-007.
5. **P1 — Product claims and user disclosure:** AUD-005 and AUD-010.
6. **P2 — Baseline/localization/test completeness:** AUD-011 and AUD-012.

Do not begin broad GPU/NPU or multi-agent implementation before the P0 items have a reviewed design, acceptance criteria and tests.

## 4. Scope and limitations

This review inspected the repository tree, Gradle configuration, manifest, core model/training code, hardware profile, world model, perception/accessibility services, ViewModel, selected UI screens, tests and architecture documents at the audited commit.

This is a static source review. It does not prove behavior on a device, compile success, runtime crashes, Android-version compatibility or vendor SDK availability. Those require build execution, automated tests and physical-device verification.

No source code or runtime behavior was changed as part of this audit report.
