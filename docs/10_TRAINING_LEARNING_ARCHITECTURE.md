# Training and Learning Architecture

**Status:** Accepted learning direction; algorithms and architecture-specific hyperparameters remain under research  
**Last reviewed:** 2026-11-03

## 1. Two distinct learning paths

Argus has two local learning paths with separate data provenance, permission and evaluation:

1. **General foundation learning** uses the bundled offline bootstrap corpus.
2. **Personal learning** adapts the instance to the user's own experience and explicitly authorized information.

Both may eventually update the same instance-local model through a validated learning coordinator. They must not be treated as one undifferentiated queue.

## 2. Genesis and application readiness

The first launch performs device assessment, configuration selection, model construction, parameter initialization and initial-generation persistence. It does not perform full corpus pretraining.

The application becomes usable after the initial generation is safely committed. Only after this readiness boundary may the general-learning scheduler enqueue or start bootstrap training. Full training must never be required to finish the splash screen.

A newly initialized model may have very limited language capability. The UI and status model must distinguish constructed, initialized, training, trained and validated.

## 3. General foundation learning

General learning consumes the bundled corpus locally without requiring network access at training time.

The current bootstrap candidate is Small C4. It is an initial learning source, not a pretrained model, not an exhaustive knowledge base and not a promise of broad language competence. Its exact revision, license, preprocessing, split quality and checksum must be pinned before distribution.

The scheduler may automatically make general learning eligible after Genesis readiness, subject to visible user controls and Android resource/lifecycle policy. It must be pausable, resumable and recoverable. Training progress must not block chat or ordinary app navigation.

## 4. Personal learning and consent

Personal learning may use conversations, corrections, user-selected documents or other sources only when the corresponding permission and retention scope are enabled.

Platform permission to access a source is not consent to retain or train on its contents. Every personal candidate must retain source provenance, timestamp, consent revision, sensitivity classification, redaction result, review status and deletion lineage.

Required candidate lifecycle:

~~~text
SOURCE AVAILABLE
      |
      v
PERMISSION + PURPOSE CHECK
      |
      v
CLASSIFICATION / REDACTION
      |
      v
REVIEWABLE CANDIDATE
      |
      +--> REJECTED / REVOKED / DELETED
      |
      v
EXPLICIT APPROVAL
      |
      v
TRAINING CANDIDATE
      |
      v
VALIDATION + CHECKPOINT
      |
      v
PROMOTED GENERATION or ROLLBACK
~~~

Unapproved or revoked candidates must never reach an optimizer update. Raw captures must not be directly enqueued into training.

## 5. Memory is not parameter training

Memory/retrieval can preserve inspectable facts, preferences and history without changing model weights. Parameter training is a separate operation with numerical risk, resource cost and more difficult deletion semantics.

Use memory as the first-line adaptation when it satisfies the need. Use personal parameter learning only when explicitly allowed and supported by evaluation and rollback.

## 6. Training mathematics and optimizer

The initial optimizer and training algorithm are not selected until architecture research is complete. Do not hard-code Q/V LoRA as the only trainable parameter set. A from-scratch foundation-training path must implement a mathematically complete backward graph for every parameter claimed to be trainable.

The selected profile must define objective and loss reduction; sequence packing, masking and context semantics; trainable/frozen parameter groups; optimizer and version; learning-rate schedule; gradient accumulation/reduction and clipping; weight decay; precision and numerical stability; determinism; and general/personal evaluation sets.

The loss, forward graph, gradients and optimizer updates must be checked against a trusted reference and finite-difference fixtures before training is enabled.

## 7. Device-adaptive workload

The initial model configuration is chosen using measured device capability. During training, a separate controller adapts active workload to live conditions such as available memory, memory pressure, CPU/GPU/NPU runtime availability and measured execution, thermal status and sustained performance, battery/power mode, Android foreground/background restrictions, user controls, and storage headroom.

Possible controls include worker concurrency, CPU thread count, microbatch size, accumulation strategy where mathematically equivalent, backend choice, checkpoint timing and pause/resume. Changing optimizer semantics, precision, sequence packing or gradient reduction must be versioned and validated. It is not an ordinary thermal response.

## 8. Background scheduling and recovery

General learning is background work after app readiness. Android may stop, defer or constrain work; no scheduler guarantees uninterrupted execution.

The design must include durable job/session identity, user-visible status and pause/stop controls, correct Android background/foreground execution policy, periodic recoverable checkpoints, process-death recovery, atomic promotion and last-known-good rollback, and typed failure reasons.

Do not start a training worker as a constructor side effect. The Genesis coordinator or scheduler must start it only after the initial model generation is committed and readiness has been published.

## 9. Instance isolation

Every general and personal training run belongs to one local instance. No central training server, federated update, cross-user data collection, model synchronization, optimizer-state exchange or weight exchange is part of the accepted design.

## 10. Evaluation and anti-forgetting

General and personal capabilities require separate evaluation:
- General held-out loss and language-quality checks.
- Language coverage and robustness checks relevant to the corpus and actual usage.
- Personal task/fact recall checks for explicitly approved data.
- Regression tests for capabilities that existed before the update.
- Safety, privacy and runtime/resource checks.

A lower training loss alone does not prove improved useful capability. Candidate generations are promoted only after evaluation, checkpoint validation and rollback readiness.
