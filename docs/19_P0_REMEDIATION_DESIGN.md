# P0 Remediation Design

**Status:** Proposed engineering contract; pending review  
**Branch:** `master`  
**Review date:** 2026-11-03  
**Scope:** Documentation only. No source changes, build, tests, workflow execution or artifacts are authorized by this document.

This document translates the confirmed P0 findings into an implementation contract. It is intentionally narrower than the complete Argus target architecture. GPU/NPU acceleration, multi-agent orchestration, multimodal inference and persistent semantic memory are outside this P0 remediation.

## 1. Immediate containment

The current implementation must not be treated as a safe continual-learning system.

Before training is enabled in a future implementation:

- Remove implicit training from chat, web retrieval, file import, image import and AccessibilityService callbacks.
- Do not start the training worker as a side effect of constructing `TrainingManager`.
- Keep the train operation unavailable unless the model artifact, tokenizer, gradient, checkpoint and candidate-approval gates in this document are satisfied.
- Do not label a sample or model as trained when the optimizer update or checkpoint write failed.
- Preserve the current implementation as an experimental prototype until these conditions are met.

These are required changes, not safeguards already present in the current source.

## 2. P0 dependency order

The P0 work has hard dependencies. The recommended order is:

1. **Model identity:** select and verify a base-model artifact and its tokenizer (AUD-014).
2. **Vocabulary invariants:** freeze the vocabulary per model revision and validate every dependent tensor shape (AUD-013).
3. **Learning mathematics:** define the trainable parameters, loss reduction and complete backward graph (AUD-001).
4. **State durability:** persist the exact model/adapter and optimizer state with atomic recovery (AUD-002).
5. **Data governance:** enforce explicit consent, classification, redaction, review and revocation for all learning sources (AUD-003/AUD-004).
6. **Worker integration:** only then connect approved records to the validated training path.

A later item must not be used to bypass an earlier prerequisite. For example, a durable checkpoint cannot make an invalid gradient correct, and a correct gradient cannot make unapproved data permissible.

## 3. Base-model artifact contract — AUD-014

The first trainable model must have a stable identity. A model package should include a manifest containing:

- Model ID, revision and immutable artifact digest.
- Architecture family/version and all dimensions and operator-relevant configuration.
- Tensor names, shapes, data types, layout and quantization metadata.
- Tokenizer ID/revision, complete vocabulary or tokenizer payload, special-token IDs and fingerprint.
- Model provenance, source, license and permitted-use metadata.
- Expected weight-file sizes and cryptographic checksums.

The loader must read into an isolated candidate state, validate the complete manifest and every tensor, reject missing/extra/incompatible tensors, reject non-finite values, and publish the state only after the entire load succeeds. An incomplete or incompatible package must leave the last-known-good model unchanged.

Randomly initialized weights may be used for deterministic numerical tests with an explicit seed. They must not be represented as a pretrained model, a restored model or a production model available for fine-tuning.

No base-model artifact is currently evidenced in the inspected repository tree. Selecting the model and confirming its license and provenance remain unresolved decisions; the implementation must not fabricate a bundled model or assume a vendor format.

## 4. Tokenizer and vocabulary contract — AUD-013

The recommended first implementation uses an immutable vocabulary for each model revision:

- The tokenizer payload and fingerprint are part of the base-model artifact.
- Inference, evaluation and training use the same tokenizer instance/configuration and do not register new tokens at runtime.
- Unknown input follows the tokenizer's explicit unknown-token or byte-fallback policy.
- Model tensors and optimizer state are dimensioned from the validated artifact vocabulary, not from a mutable runtime map.
- A vocabulary change creates a new model revision and requires an explicit migration/reinitialization of all dependent tensors, optimizer moments and checkpoint metadata.

The current `ensureVocabCapacity()` behavior, which expands embeddings and output-head rows without expanding optimizer moments, must not be retained. A successful forward pass does not prove optimizer-state compatibility.

## 5. Training mathematics and parameter policy — AUD-001

The first learning implementation should have a deliberately narrow, auditable trainable set.

### 5.1 Proposed initial trainable set

Provided that the selected base model actually contains compatible Q/V LoRA adapters:

- **Trainable:** Q and V LoRA A/B matrices.
- **Frozen:** token embeddings, positional representation, base Q/K/V/O matrices, feed-forward matrices and output-head weights.
- **Not supported initially:** full-model fine-tuning, dynamic vocabulary resizing, training on an unverified random base, and accelerator training without a validated backward backend.

This proposal avoids mutating the output head while claiming LoRA training. If the selected model does not match these adapter shapes or semantics, the architecture decision must be revisited before implementation.

### 5.2 Required gradient behavior

For each valid next-token target position, compute the full vocabulary distribution and the cross-entropy derivative:

- `dL/dlogit[v] = softmax(logits)[v] - 1(v = target)`.
- The output projection gradient must use the exact final hidden state that produced that logit row.
- Gradients must propagate through the output projection, feed-forward path, normalization, residual path and causal attention into the trainable Q/V LoRA matrices.
- Frozen base weights receive no optimizer updates, but their operations remain part of the backward graph so gradients can reach the adapters.
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

- Base-model ID, immutable digest and architecture fingerprint.
- Tokenizer ID, revision, vocabulary size and fingerprint.
- Every trainable adapter tensor, including exact shape and data type.
- Optimizer moments for every trainable tensor, optimizer step and schedule state.
- Training configuration, approved dataset revision/digest and completed update counters.
- Checkpoint schema version, payload length and checksum.

Frozen base weights may be referenced by immutable artifact digest rather than duplicated, provided the loader verifies that exact artifact before restoring the adapter.

### 6.1 Write protocol

Use an atomic same-filesystem replacement protocol, such as Android's `AtomicFile` or a rigorously implemented temporary-file, flush/sync and rename sequence. Never overwrite the last-known-good checkpoint in place.

The save operation must return a typed success/failure result. A failed save must not be swallowed and must not result in a completed/promoted training record. Keep a last-known-good checkpoint separate from an unpromoted candidate checkpoint.

### 6.2 Load protocol

Read and validate the entire checkpoint into temporary state before changing live model or optimizer state. Reject:

- Truncated, oversized or trailing-data files.
- Unknown schema versions.
- Invalid checksums or non-finite values.
- Architecture, base-model, tokenizer or tensor-shape mismatches.
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

Track which approved dataset revision contributed to each candidate adapter/model revision. Keep the base model immutable and train into versioned adapter candidates. If a candidate is deleted or consent is revoked after promotion, Argus must be able to roll back to a known adapter revision and rebuild from the remaining approved dataset.

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
| Base model | Artifact manifest validation, digest mismatch rejection, missing/extra tensor rejection and deterministic reload |
| Tokenizer | Stable fingerprint, fixed vocabulary, unknown Unicode behavior and tensor-shape invariants |
| Gradients | Reference parity and finite-difference checks for every trainable LoRA tensor |
| Optimizer | Correct accumulation/reduction, step count, bias correction, clipping and frozen-weight immutability |
| Checkpoint | Exact adapter/optimizer round trip, corruption/truncation rejection and interrupted-write recovery |
| Consent | Default-off settings, independent consent scopes and immediate revocation behavior |
| Capture | Package/window mismatch, password fields, excluded/unknown app and redaction adversarial fixtures |
| Approval | Rejected/unreviewed samples never reach the optimizer; imported and web data require the same approval |
| Deletion | Candidate lineage, adapter rollback and reconstruction without deleted/revoked samples |
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

1. Which base-model artifact is selected, and what are its license, provenance, architecture and tokenizer?
2. Does that base model contain compatible Q/V LoRA adapters, or must the adapter architecture be redesigned?
3. What exact optimizer and hyperparameter configuration is the first supported training profile?
4. What screen-perception and learning-consent wording/scopes should the user approve?
5. What retention period and rollback policy apply to approved candidate data and trained adapter revisions?

Until these are resolved and the P0 verification matrix is approved, all P0 findings remain OPEN.

## 12. Superseding architecture decision — Local Knowledge Genesis

**Decision date:** 2026-11-03  
**Status:** Accepted design direction. This section supersedes conflicting assumptions in earlier sections of this draft.

The earlier version of this remediation document assumed an externally pretrained base-model artifact and Q/V LoRA as the initial trainable set. Those assumptions are no longer the Argus target.

### 12.1 Replacement model-origin contract

Argus follows Local Knowledge Genesis:
- Each installation constructs its own model and initializes its parameters locally.
- No externally pretrained weights are required or used as the Argus foundation.
- Architecture family is not selected yet; it must pass the research gate in [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md).
- The initial model configuration is derived from the device capability profile, not a global parameter-count target.
- Genesis initialization is completed and committed before the application is marked ready. General pretraining does not block first launch.

The replacement for the old base-model artifact contract is a **Genesis model-state contract**. Its manifest must include instance/generation IDs, architecture/module versions, configuration fingerprint, tokenizer revision/fingerprint, tensor names/shapes/dtypes/layouts, module-specific initializer metadata, tensor checksums, device configuration evidence and generation lineage.

The implementation must persist every initialized tensor. A random initialization routine without a durable manifest and full-state checkpoint does not satisfy this contract.

### 12.2 Replacement initial training contract

Q/V LoRA is not the approved initial training strategy. The trainable parameter set depends on the architecture selected for from-scratch learning. Before enabling any training:
- Define every trainable and frozen tensor group.
- Implement a mathematically complete backward path for every trainable parameter.
- Validate gradients and optimizer updates against a trusted reference and finite-difference fixtures.
- Serialize all optimizer and scheduler state needed for exact resume.
- Prove that a controlled training fixture changes only the intended parameter state.

The current incomplete prototype update remains disabled or explicitly experimental until these gates pass.

### 12.3 General and personal learning worker policy

Do not start workers as constructor or singleton side effects.

- **General learning:** after the initial generation is committed and the application is ready, the Genesis coordinator may schedule background learning from the bundled offline corpus according to visible user controls and Android lifecycle/resource policy. It must be resumable and must never block first-launch readiness.
- **Personal learning:** may run only from repository-issued candidates with current, explicit permission and approval. Screen, chat, file and web access do not imply training consent.
- The two paths retain separate provenance, evaluation and deletion lineage.

### 12.4 Device-adaptive training policy

Genesis-time capability assessment selects the feasible model configuration. A separate live controller adjusts workload or pauses work for current memory, thermal, battery, backend, storage and Android scheduling conditions. It must not mutate architecture, tensor shapes or optimizer semantics without a versioned candidate configuration and validation.

### 12.5 Revised unresolved decisions

The following replace the old external-base-model and Q/V-LoRA decisions:
1. Initial architecture family and supported module set.
2. Tokenizer family, revision and Unicode policy.
3. Exact Small C4 source revision, license/notice review and packaging choice.
4. Corpus preprocessing, token count and split construction.
5. Device capability profile and measurable admission thresholds.
6. Module-specific initialization and randomness policy.
7. Architecture-specific objective, optimizer and training profile.
8. Android background scheduling and user controls.
9. Checkpoint cadence, full-state recovery and generation promotion.
10. Personal-learning consent, retention and post-training revocation strategy.

### 12.6 P0 acceptance interpretation

The P0 findings remain open until verified. In particular:
- AUD-001 remains a critical training-mathematics defect.
- AUD-002 remains a critical full-state persistence/recovery defect.
- AUD-003/AUD-004 remain privacy and approval blockers.
- AUD-013 remains a tokenizer/vocabulary invariant problem.
- AUD-014 is reinterpreted as a missing Genesis identity and complete model-state contract; absence of a pretrained third-party artifact is intentional.

Verification must follow documents 20–23 and the gates in document 15. No build, tests, workflow dispatch or artifact upload is authorized by this addendum.
