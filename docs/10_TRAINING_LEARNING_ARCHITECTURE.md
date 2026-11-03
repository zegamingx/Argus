# Training and Continual Learning Architecture

**Status:** Proposed design; accelerator training is not assumed  
**Last reviewed:** 2026-11-03

## 1. Separate inference from learning

Inference, embeddings, evaluation and training have different operator, memory, latency and power profiles. A backend that runs forward inference may not support gradients, backward operators, optimizer states or parameter updates. Advertise these capabilities separately.

## 2. Learning pipeline

`candidate capture → consent/classification → redaction → deduplication → quality scoring → user approval → dataset version → training → evaluation → promote or rollback`

Raw screen, camera or file capture must never flow directly into training. A candidate needs provenance, consent, appropriate-use review and a retention policy.

## 3. Training strategies

| Strategy | Description | Mobile suitability |
|---|---|---|
| Memory/retrieval update | Store approved facts without changing weights | First-line adaptation |
| Prompt/context tuning | Improve context assembly and templates | Low compute |
| LoRA/adapters | Train a small parameter subset | Candidate for constrained local training |
| Partial fine-tuning | Update selected base layers | High resource/stability risk |
| Full fine-tuning | Update all model parameters | Not a default smartphone requirement |
| Distillation | Train a smaller model from a teacher | Usually offline/development workflow |

A GPU or NPU does not imply an efficient training path. NPU backward/optimizer support must be demonstrated; otherwise use a validated CPU/GPU path or do not offer that training mode.

## 4. Correctness and reproducibility

Specify loss, optimizer, schedule, gradient accumulation, clipping, weight decay, mixed precision, checkpoint semantics, masks, sequence packing and evaluation. Validate gradients against finite differences or trusted reference implementations for supported kernels. Record seed, kernel nondeterminism and environment.

Dataset splits must prevent train/evaluation leakage. Report token-weighted and sequence-weighted metrics where relevant, and define the distinction.

## 5. Resource and lifecycle controls

Budget weights, activations, gradients, optimizer moments, workspaces, batches and OS headroom. Support bounded batch/sequence sizes, cancellation/pause, periodic recoverable checkpoints, thermal/battery/memory admission control, foreground execution policy and process-death recovery.

Parallel execution is not assumed faster. Compare wall-clock throughput, memory and thermal cost for each scheduling mode.

## 6. Promotion and anti-forgetting

Candidate adapters pass held-out quality, regression, safety and resource checks. Promote atomically with version/provenance; retain last-known-good version; roll back on regression, corruption or runtime incompatibility. Replay/regularization must be justified by evaluation, not used as a slogan.

Users must be able to inspect, pause, reject and delete learning candidates, datasets and adapters. Use of an assistant does not imply consent to train.

## 7. Background consolidation

A sleep/consolidation workflow may run only under explicit scheduling and platform constraints. Charging, idle and thermal-safe states are useful heuristics, not guarantees of unlimited background execution.
