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

1. **Genesis identity:** define the per-instance model/generation identity and versioned manifest for locally initialized parameters (AUD-014).
2. **Tokenizer invariants:** select and freeze a tokenizer revision per generation; validate vocabulary and all dependent tensor shapes (AUD-013).
3. **Learning mathematics:** after architecture and training-feasibility decisions, define the trainable parameter groups, loss reduction and complete backward graph (AUD-001).
4. **State durability:** persist the exact model-generation, mutable tensor and optimizer state with atomic recovery (AUD-002).
5. **Data governance:** enforce provenance, rights, consent, classification, redaction, review, revocation and deletion according to general-versus-personal data policy (AUD-003/AUD-004).
6. **Worker integration:** only then connect eligible immutable general-corpus candidates and explicitly approved personal candidates to the validated training path.

A later item must not be used to bypass an earlier prerequisite. For example, a durable checkpoint cannot make an invalid gradient correct, and a correct gradient cannot make unapproved data permissible.

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

The current prototype's incomplete checkpoint is a persistence/identity defect, not evidence that a third-party pretrained artifact is missing. Architecture and tokenizer decisions remain governed by documents 20 and 23.

## 4. Tokenizer and vocabulary contract — AUD-013

The recommended first implementation uses an immutable vocabulary for each model revision:

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
- Training configuration, approved dataset revision/digest and completed update counters.
- Checkpoint schema version, payload length and checksum.

Any immutable tensor payload may be referenced by the active generation's immutable digest rather than duplicated, provided the loader verifies that exact generation before restoring mutable state.

### 6.1 Write protocol

Use an atomic same-filesystem replacement protocol, such as Android's `AtomicFile` or a rigorously implemented temporary-file, flush/sync and rename sequence. Never overwrite the last-known-good checkpoint in place.

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

Required lifecycle:

`CAPTURED → CLASSIFIED → REDACTED → PENDING_REVIEW → APPROVED → TRAINING → TRAINED`

Alternative terminal states include `REJECTED`, `REVOKED`, `FAILED` and `DELETED`. Only `APPROVED` candidates with current consent and matching policy/dataset revisions may enter training. A boolean flag supplied by an arbitrary caller is not sufficient authorization. Queue labels, screen copy and notifications may say “approved” only when the candidate store has persisted and verified that status; presentation text must not imply an approval gate that the runtime does not enforce.

Chat prompts, file contents, image-derived metadata and web snippets must use this same candidate lifecycle. Web results remain untrusted evidence and are never training data merely because retrieval occurred or model confidence was low.

### 7.4 Revocation and post-training deletion

Track which approved dataset revision contributed to each candidate adapter/model revision. Keep each committed generation immutable and train into isolated, versioned candidate generations. If a candidate is deleted or consent is revoked after promotion, Argus must be able to roll back to a known model-generation revision and rebuild from the remaining approved dataset.

Do not promise exact unlearning from weights that have been irreversibly merged. If provenance and rollback cannot be guaranteed, the system must disclose the limitation and must not promote that training output as a deletable user-controlled memory.

## 8. Worker and failure behavior

The worker must be started explicitly by a user-authorized training action, not from a constructor or application singleton initialization. It must accept only repository-issued approved records, not arbitrary raw strings.

For each record:

1. Revalidate approval, consent revision, dataset revision and model/tokenizer identity.
2. Build the update in isolated candidate state.
3. Validate loss, gradients, optimizer moments and updated parameters.
4. Persist a recoverable checkpoint.
5. Mark the record trained only after the required persistence and evaluation gates succeed.

Expected item-level failures must be recorded without silently killing the worker. Model corruption, non-finite updates, incompatible state or checkpoint failure must pause the session and preserve the last-known-good model. Never publish a partially applied optimizer update.

## 9. Required verification matrix before P0 closure

| Area | Required evidence |
|---|---|
| Genesis model state | Generation-manifest validation, digest mismatch rejection, missing/extra tensor rejection and deterministic reload |
| Tokenizer | Stable fingerprint, fixed vocabulary, unknown Unicode behavior and tensor-shape invariants |
| Gradients | Reference parity and finite-difference checks for every declared trainable parameter group |
| Optimizer | Correct accumulation/reduction, step count, bias correction, clipping and frozen-weight immutability |
| Checkpoint | Exact model-generation and optimizer-state round trip, corruption/truncation rejection and interrupted-write recovery |
| Consent | Default-off settings, independent consent scopes and immediate revocation behavior |
| Capture | Package/window mismatch, password fields, excluded/unknown app and redaction adversarial fixtures |
| Approval | Rejected/unreviewed samples never reach the optimizer; imported and web data require the same approval |
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
- [13 — Security and Data Governance](13_SECURITY_PRIVACY_DATA_GOVERNANCE.md) for cross-system privacy and data-governance requirements.

**Supersession rule:** any remaining historical reference in this document to a pretrained base-model artifact or Q/V LoRA is not an Argus target requirement. Such wording must be read only as a description of the prototype's historical assumptions or defects. The accepted target is a locally initialized, versioned Genesis generation with an architecture-specific training contract.

All P0 findings remain OPEN until their evidence requirements are met. This document is a design contract, not implementation evidence.
