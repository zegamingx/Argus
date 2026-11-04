# P0 Remediation Design

**Status:** Proposed engineering contract; pending review  
**Branch:** `main`  
**Review date:** 2026-11-04  
**Scope:** Documentation only. No source changes, build, tests, workflow execution or artifacts are authorized by this document.

This document translates the confirmed P0 findings into an implementation contract. It is intentionally narrower than the complete Argus target architecture. GPU/NPU acceleration, multi-agent orchestration, multimodal inference and persistent semantic memory are outside this P0 remediation.

## 1. Immediate containment

The current implementation must not be treated as a safe continual-learning system.

Before training is enabled in a future implementation:

- Remove implicit training from chat, web retrieval, file import, image import and AccessibilityService callbacks.
- Do not start the training worker as a side effect of constructing `TrainingManager`.
- Keep the train operation unavailable unless the Genesis model-state identity, tokenizer, gradient, checkpoint and applicable data-governance gates in this document are satisfied.
- Do not label a sample or model as trained when the optimizer update or checkpoint write failed.
- Preserve the current implementation as an experimental prototype until these conditions are met.

These are required changes, not safeguards already present in the current source.

## 2. P0 dependency order

The P0 work has hard dependencies. The recommended order is:

1. **Genesis identity contract:** define the architecture-agnostic per-instance and generation identity, immutable manifest boundary and atomic first-generation publication (AUD-014). This contract can be designed now; concrete tensor inventory and configuration are instantiated only after the upstream research gates.
2. **From-scratch feasibility research:** define the useful-learning target, summarize published from-scratch and mobile-training evidence, derive analytical model/data/compute/memory bounds, state provisional device assumptions, and document stop criteria now. Do not wait for Argus device measurements. The physical feasibility and support claims are verified later through E1–E7 after implementation. Follow [16 — Roadmap and Research Register](16_ROADMAP_RESEARCH_REGISTER.md) and [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md).
3. **Tokenizer and architecture design decisions:** establish a research-backed, device-conditioned tokenizer profile portfolio and selection contract from published results, rights-reviewed source options and analytical vocabulary/model costs; compare tokenizer-neutral architecture families in parallel. G4 approves the portfolio and deterministic selection/fallback policy; each local model generation binds to the exact selected tokenizer ID and fingerprint. Runtime conformance and downstream model-quality measurements are post-implementation validation obligations, not prerequisites for completing this documentary design work (AUD-013).
4. **Learning mathematics:** define the trainable parameter groups, loss reduction, complete backward graph and optimizer semantics only for the architecture that passes the feasibility and selection gates (AUD-001).
5. **State durability:** persist the exact candidate-generation, optimizer and resumable-session state with atomic recovery (AUD-002).
6. **Data governance:** enforce source rights/provenance for general corpus inputs and consent/classification/redaction/review/revocation/deletion for personal inputs (AUD-003/AUD-004).
7. **Worker integration:** only then connect immutable eligible general-corpus shards and explicitly approved personal candidates to the validated training path.

A later item must not bypass an earlier prerequisite. A correct checkpoint cannot make invalid gradients correct; correct gradients cannot authorize unlicensed or unapproved data; and a passing unit-level kernel test cannot establish that a complete model can be trained within a target device's sustained resource envelope.

## 3. Genesis model-state identity contract — AUD-014

The first trainable model generation must have a stable, independently verifiable identity even though its parameters originate from local initialization rather than an imported pretrained artifact.

The Genesis manifest must include:

- Unique instance ID and generation ID, plus an optional parent-generation ID.
- Architecture family/version, module versions, dimensions and operator-relevant configuration fingerprint.
- Tokenizer ID/revision, tokenizer payload or immutable asset reference, special-token IDs and fingerprint.
- Tensor names, shapes, data types, layouts, initialization policy/version and integrity checksums.
- Initialization seed metadata sufficient for audit/recovery without forcing identical production weights across installations.
- Device capability/configuration evidence used for admission.
- Model-state schema version and storage format version.

Genesis must initialize the complete required tensor inventory, validate shapes/dtypes/ranges and reject non-finite values. It must persist the initial generation through an isolated temporary state and atomic commit. The active generation is published only after the manifest and every tensor have passed validation. An incomplete generation must never replace the last-known-good generation.

A deterministic seed is appropriate for numerical fixtures and reproducible tests. Production instances must receive independent initialization state. Random initialization is the intended origin of a new Argus generation; it must not be described as learned language capability.

The current prototype's incomplete checkpoint is a persistence/identity defect, not evidence that a third-party pretrained artifact is missing. Architecture and tokenizer decisions remain governed by documents 09, 16, 20 and 23.

### 3.1 Genesis-to-training handoff

Genesis readiness is a hard transaction boundary, not a training milestone:

1. Genesis creates and commits the complete initial generation and its immutable manifest.
2. Only after that commit succeeds may the app publish GENESIS_READY and APPLICATION_READY.
3. A learning session references the exact active parent-generation ID and manifest digest. It must not modify the committed parent generation in place.
4. Training operates on an isolated child candidate generation (or an equivalent copy-on-write state) with its own generation ID, parent reference, configuration fingerprint and candidate-state manifest.
5. Checkpoints bind the candidate state to the exact parent digest, tokenizer revision, training session and immutable input-data revision.
6. A candidate becomes active only after its model state and manifest are durably committed and all required numerical, general-evaluation, personal-evaluation, compatibility and resource gates pass.
7. A rejected, interrupted or regressing candidate must not replace the active generation. The last-known-good generation remains loadable.

The active generation may remain available for inference while a candidate is being trained only when the device-admission profile proves that the combined memory and execution budget is safe. Concurrency is not assumed; a device may serialize inference and training or pause one workload.

GENESIS_READY therefore means **constructed, initialized, persisted and recoverable**. It does not mean trained, evaluated, promoted or generally capable. The background learning scheduler is downstream of APPLICATION_READY, as specified in [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md) and [10 — Training and Learning Architecture](10_TRAINING_LEARNING_ARCHITECTURE.md).

## 4. Tokenizer and vocabulary contract — AUD-013

The recommended first implementation uses one immutable tokenizer artifact/vocabulary for each model generation:

- The tokenizer payload and fingerprint are part of the model-generation manifest.
- Inference, evaluation and training use the same tokenizer instance/configuration and do not register new tokens at runtime.
- Unknown input follows the tokenizer's explicit unknown-token or byte-fallback policy.
- Model tensors and optimizer state are dimensioned from the validated generation tokenizer vocabulary, not from a mutable runtime map.
- A vocabulary change creates a new model revision and requires an explicit migration/reinitialization of all dependent tensors, optimizer moments and checkpoint metadata.

The current `ensureVocabCapacity()` behavior, which expands embeddings and output-head rows without expanding optimizer moments, must not be retained. A successful forward pass does not prove optimizer-state compatibility.

## 5. Training mathematics and parameter policy — AUD-001

The trainable parameter set is deliberately unresolved until the architecture and from-scratch training-feasibility gates have been completed. This document defines correctness requirements, not a LoRA or full-model training selection.

### 5.1 Parameter-group declaration

Before a training implementation is approved, its architecture-specific contract must enumerate:

- Every trainable tensor group, including tensor names, shapes and dtypes.
- Every frozen tensor group and why it is frozen.
- Whether embeddings and output projection are tied or separate.
- Which forward operations must retain backward state to propagate gradients to every trainable group.
- The optimizer and scheduler state required for exact recovery.
- Any operation that lacks a verified backward implementation; such a path must be excluded from training.

No Q/V LoRA-only, adapter-only or full-model training strategy is selected by this document. The selected strategy must follow the architecture decision and measured device-feasibility results.

### 5.2 Required gradient behavior

For each valid next-token target position, compute the full vocabulary distribution and the cross-entropy derivative:

- `dL/dlogit[v] = softmax(logits)[v] - 1(v = target)`.
- The output projection gradient must use the exact final hidden state that produced that logit row.
- Gradients must propagate through the output projection, feed-forward path, normalization, residual path and causal attention into every declared trainable parameter group.
- Frozen tensors receive no optimizer updates, but their operations remain in the backward graph wherever needed to propagate gradients to trainable tensors.
- All valid token-position gradients in a batch are accumulated and reduced using one explicitly documented mean/sum rule before the optimizer update.

Updating only the target vocabulary row, using the input embedding as the output-head feature, or omitting the attention backward path is not acceptable.

### 5.3 Optimizer semantics

The optimizer specification must be explicit and serialized with the model revision/training session:

- Optimizer algorithm and version.
- Learning-rate schedule and warm-up/decay parameters.
- Gradient reduction denominator and ignored-token mask.
- Global-norm clipping threshold.
- Weight-decay policy and parameter mask.
- Bias-correction semantics and optimizer step count.

One optimizer step represents one completed optimizer update, not one token position. Moment state is updated once per trainable tensor per optimizer step. Non-finite loss, gradients, moments or updated weights abort the update before the candidate state is published.

## 6. Checkpoint and recovery contract — AUD-002

A resumable training checkpoint must reconstruct the exact state needed to continue:

- Model instance ID, generation ID, model-state digest and architecture/configuration fingerprint.
- Tokenizer ID, revision, vocabulary size and fingerprint.
- Every mutable/trainable model tensor, including exact shape and data type; immutable tensor groups must be covered by the generation manifest and verified digest.
- Optimizer moments for every trainable tensor, optimizer step and schedule state.
- Training session ID, configuration fingerprint, immutable corpus/dataset manifest revision and digest, data-shard identity, sampler/shuffle state and next uncommitted work-item cursor.
- RNG state for every stochastic operation in the supported training profile, or an explicit declaration that deterministic replay is not supported for that operation/backend.
- Precision/scaler state where applicable, backend/runtime identifier and deterministic-operation profile.
- Idempotency information sufficient to prove which work items or batches are already included in the last committed update.
- Checkpoint schema version, payload length and checksum.

For the initial P0 contract, checkpoints are committed only at completed optimizer-update boundaries. An interruption during a microbatch or gradient-accumulation interval discards that uncommitted work and resumes from the last durable cursor. If mid-accumulation checkpoints are introduced later, partial gradients and every associated accumulation counter become mandatory checkpoint state.

A training checkpoint is distinct from the immutable Genesis/model-generation manifest:
- The generation manifest identifies the committed parent or promoted generation and its complete tensor inventory.
- The candidate checkpoint records the working child-generation tensor state plus the optimizer/scheduler and session state required to resume.
- A checkpoint may reference immutable parent tensors by digest only when the loader verifies that exact parent generation and every referenced tensor before restoration.
- An architecture migration or tokenizer change is a new generation/migration transaction, not an ordinary in-place checkpoint update.

### 6.1 Write protocol

Use an atomic same-filesystem replacement protocol, such as Android's `AtomicFile` or a rigorously implemented temporary-file, flush/sync and rename sequence. Never overwrite the last-known-good checkpoint in place.

The logical durable transaction must bind together the candidate model tensors after a complete optimizer update, optimizer/scheduler and applicable RNG/precision state, the data cursor, and the idempotency record for the work item or batch. Persist a self-consistent checkpoint bundle to isolated temporary storage, validate it, then atomically publish one commit manifest/pointer that identifies all components. Do not advance the durable data cursor independently of the model/optimizer update: otherwise process death can cause an already-applied sample to be applied a second time.

The save operation must return a typed success/failure result. A failed save must not be swallowed and must not result in a completed/promoted training record. Keep a last-known-good checkpoint separate from an unpromoted candidate checkpoint.

### 6.2 Load protocol

Read and validate the entire checkpoint into temporary state before changing live model or optimizer state. Reject:

- Truncated, oversized or trailing-data files.
- Unknown schema versions.
- Invalid checksums or non-finite values.
- Architecture, generation, tokenizer or tensor-shape mismatches.
- Missing optimizer moments or inconsistent step counters.

Publish restored state atomically only after every check succeeds. Otherwise retain the last-known-good state and surface a recoverable error.

## 7. Candidate, consent and data-governance contract — AUD-003/AUD-004

### 7.1 Separate capability from consent

Enabling Android AccessibilityService grants a platform capability; it is not consent to train on captured content. Keep separate user-controlled settings for:

1. Screen perception/context access.
2. Retaining screen-derived text or creating a reviewable candidate.
3. Training from a candidate after classification, redaction and explicit approval.

All three settings default to off. Screen perception permission alone must not authorize retention or training. When context-only perception is explicitly enabled but retention is not, any text may be used only as short-lived in-memory context for the active interaction; it must not be written to the WorldModel entity store, a candidate repository, the training queue, a log or a checkpoint. Revocation takes effect immediately for new capture and for any queued candidate not yet committed to an optimizer update.

### 7.2 Fail-closed screen capture

Before traversing an accessibility tree:

- Require screen perception to be explicitly enabled.
- Require the event package and active root package/window identity to match.
- Reject password-designated nodes and secure or unclassifiable content.
- Use an explicit package allowlist for eligible screen capture; an exclusion blacklist is not a security boundary.
- Apply deterministic secret/credential/OTP redaction before candidate persistence.
- If classification, identity or redaction is uncertain, discard the content rather than enqueue it.

The user must see when screen perception is active and be able to pause or revoke it without navigating through hidden controls. The WorldModel context-write path must independently enforce the retention setting; preventing optimizer enqueue alone is insufficient because the current screen snippet is also stored in WorldModel.

### 7.3 Candidate lifecycle

Replace raw-text training enqueue with a provenance-bearing candidate record. At minimum, retain:

- Candidate ID and source type.
- Source/package/document reference and capture timestamp.
- Consent scope and consent-policy revision.
- Classification and redaction result.
- Content digest and dataset revision.
- Review status, approval timestamp and rejection/revocation status.

Required personal-candidate approval lifecycle:

`CAPTURED → CLASSIFIED → REDACTED → PENDING_REVIEW → APPROVED`

Alternative terminal states include `REJECTED`, `REVOKED`, `FAILED` and `DELETED`. Approval makes a personal candidate eligible for the separately tracked training-work lifecycle; it does not mean an optimizer update has happened.

The training-work lifecycle is:

`ELIGIBLE → QUEUED → APPLYING → APPLIED_TO_CANDIDATE`

It may instead end in `FAILED`, `REVOKED` or `DEFERRED`. Model-generation validation and promotion are a third, separate lifecycle defined in section 8. Only `APPROVED` personal candidates with current consent and matching policy/dataset revisions may enter the personal training queue. A boolean flag supplied by an arbitrary caller is not sufficient authorization. Queue labels, screen copy and notifications may say “approved” only when the candidate store has persisted and verified that status; presentation text must not imply an approval gate that the runtime does not enforce.

Training admission must preserve the distinction between two data classes rather than treating every input as a generic user-approved record:

- **General corpus input:** require immutable source/item provenance, a verified training-use permission decision, applicable notices/attribution, quality and sensitivity checks, deterministic preprocessing, deduplication/split integrity, and an immutable corpus manifest/digest. General public-source eligibility does not depend on personal screen-capture consent. An unknown or incompatible rights state fails closed.
- **Personal input:** use the candidate lifecycle above. Require source-specific consent, classification, redaction, explicit user approval, current policy revision and revocation/deletion lineage before an optimizer update.

Chat prompts, screen-derived content, personal files, imports and image-derived personal metadata are personal inputs unless an independently reviewed policy establishes another classification. Retrieved web pages are untrusted evidence; retrieval alone never makes them training inputs. Public web material intended for general learning must enter through the general source-registry and rights/quality pipeline in documents 24–28, not through the personal-candidate approval shortcut.

### 7.4 Revocation and post-training deletion

Track every general-corpus manifest, personal-candidate ID, consent-policy revision and training-session input digest that contributed to each candidate or promoted generation. Keep committed generations immutable and train into isolated, versioned child generations. The lineage must identify a known eligible ancestor and the exact input set needed to reconstruct a generation.

If a personal candidate is deleted or consent is revoked after promotion, do not claim that the information has been removed from model weights merely because its source record was deleted. Where the retained lineage and compute budget permit, rebuild a clean descendant from an eligible ancestor using only the remaining permitted inputs, then validate and promote it. Otherwise disclose the limitation and preserve a clearly identified rollback path; do not describe irreversible weight changes as deletable user-controlled memory.

General-corpus rights withdrawal or source-license changes require a separate source-policy response. Do not treat a personal-consent revocation as the legal basis for general-source data, or vice versa.

## 8. Worker and failure behavior

The worker may start only after APPLICATION_READY, through the post-Genesis scheduler under visible owner policy, and never as a constructor or application-singleton side effect. General-corpus learning may be scheduled according to the general-learning policy and user controls. Personal-learning work requires a repository-issued approved candidate and current explicit consent. The worker must accept typed items from immutable eligible general-corpus manifests or approved personal candidates, not arbitrary raw strings.

For each work item or batch:

1. Resolve its immutable identity, source class, content/dataset digest and provenance.
2. Revalidate the correct admission policy: source-rights and corpus-manifest eligibility for general data; current consent, classification, redaction, approval and revocation state for personal data.
3. Verify active parent-generation, candidate-generation, tokenizer and training-session compatibility.
4. Derive a stable idempotency key from the session, data revision and work-item/batch identity.
5. Execute the update in isolated candidate state. Do not mutate the active parent generation.
6. Validate loss, gradients, optimizer state and all updated tensors before committing the update.
7. Atomically persist the candidate model/optimizer state, session cursor and idempotency result at the supported checkpoint boundary.

A work item may be marked APPLIED_TO_CANDIDATE only after the checkpoint containing its update and idempotency record is durable. This status does not mean the candidate model generation passed evaluation or became active. Keep work-item outcomes separate from generation lifecycle:

CANDIDATE → VALIDATING → PROMOTED

or

CANDIDATE → VALIDATING → REJECTED

Only a generation that passes all promotion gates may become active. Do not label a dataset item, session or model as generally trained/validated solely because an optimizer update was computed or a checkpoint was written.

Expected item-level failures must be recorded without silently killing the worker. Model corruption, non-finite updates, incompatible state or checkpoint failure must pause the session and preserve the last-known-good model. Process-death recovery must not duplicate a committed update or skip an uncommitted one.

## 9. Required verification matrix before P0 closure

| Area | Required evidence |
|---|---|
| Genesis model state | Generation-manifest validation, digest mismatch rejection, missing/extra tensor rejection, independent instance identity, atomic first-generation commit and deterministic test-fixture reload |
| Genesis-to-learning handoff | No worker before APPLICATION_READY; the learning session references the exact immutable parent generation and creates an isolated child candidate |
| Candidate promotion | Active parent remains unchanged until a fully validated candidate is atomically promoted; rejection and rollback preserve the last-known-good generation |
| Tokenizer | Stable fingerprint, fixed vocabulary, unknown Unicode behavior and tensor-shape invariants |
| Gradients | Reference parity and finite-difference checks for every declared trainable parameter group |
| Optimizer | Correct accumulation/reduction, step count, bias correction, clipping and frozen-weight immutability |
| Checkpoint | Exact candidate tensor, optimizer/scheduler, RNG/precision, sampler-cursor and idempotency round trip; corruption/truncation rejection and interrupted-write recovery |
| Resume idempotency | Process death around update/checkpoint/cursor publication cannot duplicate a committed batch or skip uncommitted work |
| Data admission | General corpus requires rights/provenance/quality manifest; personal inputs require current consent, redaction and explicit approval |
| Consent | Default-off settings, independent consent scopes and immediate revocation behavior |
| Capture | Package/window mismatch, password fields, excluded/unknown app and redaction adversarial fixtures |
| Approval | Rejected/unreviewed personal candidates never reach the optimizer; general web sources require independent rights/provenance/quality admission, and retrieval snippets alone never qualify |
| Deletion | Candidate lineage, generation rollback and reconstruction without deleted/revoked samples |
| Worker | Bad sample isolation, fatal-error pause, process interruption and restart behavior |

The test suites must be authored before training is re-enabled. They must not be run during the current audit/build freeze.

## 10. Build and workflow gate

The existing repository rule remains in force:

- No Gradle build, test, lint, packaging or install task during P0 audit/remediation.
- No manual or implicit GitHub Actions workflow dispatch during this phase.
- No artifact upload or release publication.
- When the audit and remediation are complete, the user must explicitly accept the final verification gate. Only then may a build be initiated from an immutable Git tag pointing to the reviewed commit.
- Any future build workflow must be tag-triggered and must omit artifact-upload steps unless separately authorized.

A tag is a release/build boundary, not permission to skip source review or verification. No build success is implied by this design document.

## 11. Open decisions

The following decisions must be resolved before implementation:

1. Which architecture and tokenizer are selected after the feasibility and comparative research gates, and what are their exact versioned contracts?
2. Which parameter groups are trainable for the selected from-scratch architecture, and which optimizer semantics are correct for those groups?
3. What exact optimizer and hyperparameter configuration is the first supported training profile?
4. What screen-perception and learning-consent wording/scopes should the user approve?
5. What retention period and rollback policy apply to approved candidate data and trained model-generation revisions?

Until these are resolved and the P0 verification matrix is approved, all P0 findings remain OPEN.

## 12. Cross-document authority

This document defines P0 correctness, persistence, privacy and approval gates. It does not select the neural architecture, tokenizer, optimizer or per-device model dimensions.

The controlling design references are:
- [01 — System Requirements and Decisions](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md) for accepted product decisions and ADRs.
- [09 — Model Architecture](09_MODEL_ARCHITECTURE.md) and [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md) for local model origin, generation identity and first-launch readiness.
- [21 — Device-Adaptive Model and Training](21_DEVICE_ADAPTIVE_MODEL_AND_TRAINING.md) for capability admission and runtime workload adaptation.
- [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md) for the distinct general and personal learning data paths.
- [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md) for architecture/tokenizer research and trainable-parameter decisions.
- [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md) for the research-backed/analytical feasibility boundary now and the E1–E7 physical implementation-verification protocol later; E1–E7 do not precede documentary architecture or tokenizer selection.
- [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md) for the tokenizer pipeline, artifact fingerprint, Unicode/special-token conformance and generation migration contract.
- [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md) for the Android prior-art evidence, component dispositions and framework-reuse audit boundary.
- [13 — Security and Data Governance](13_SECURITY_PRIVACY_DATA_GOVERNANCE.md) for cross-system privacy and data-governance requirements.
- [36 — Pre-Implementation Module and Lifecycle Contracts](36_PRE_IMPLEMENTATION_MODULE_AND_LIFECYCLE_CONTRACTS.md) for cross-module interfaces, state machines, event/error contracts, concurrency, storage transactions and later verification mapping.

**Supersession rule:** any remaining historical reference in this document to a pretrained base-model artifact or Q/V LoRA is not an Argus target requirement. Such wording must be read only as a description of the prototype's historical assumptions or defects. The accepted target is a locally initialized, versioned Genesis generation with an architecture-specific training contract.

All P0 findings remain OPEN until their evidence requirements are met. This document is a design contract, not implementation evidence.
