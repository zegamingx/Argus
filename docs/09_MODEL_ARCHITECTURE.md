# Model Architecture, Genesis and Identity

**Status:** Accepted Genesis direction; initial architecture family under research  
**Last reviewed:** 2026-11-03

## 1. Model origin

Argus uses **Local Knowledge Genesis**. Each installation constructs its own model from an architecture selected for that device and initializes its parameters locally using module-specific initialization policies.

An external pretrained model is not the foundation of Argus. A downloaded model must not be relabeled as the Argus-origin model. The current repository prototype is not yet a complete implementation of this model-origin contract.

Random initialization is only an initial numerical state. It does not confer general language competence. General capability must be learned through sufficient, validated training data and computation.

## 2. Architecture research status

The initial neural architecture is deliberately undecided. Candidate families include decoder-only Transformers and attention variants, state-space and recurrent sequence models, hybrid architectures, and additional candidates where evidence supports them.

The pre-implementation design choice must be based on comparative published research, audited implementations and licenses, analytical parameter/compute/memory estimates, the available device/runtime evidence, numerical requirements and extensibility. Compare evidence only within its actual workload category (from-scratch learning, fine-tuning, inference or runtime optimization), and disclose when device, data, tokenizer, model size or training budgets are not matched.

Argus-specific training/inference benchmarks and sustained physical-device measurements are **post-implementation verification**, not prerequisites for completing this research or approving a clearly qualified design contract. They determine whether the implemented configuration meets its resource envelope and which named device tiers can be supported. Until those measurements pass, the design must remain marked unverified and must not be presented as proven Android feasibility. Do not assume a Transformer is the permanent Argus architecture.

## 3. Architecture-agnostic model description

The model description layer should represent:
- Module identity, family and version.
- Input/output tensor contracts.
- Tensor names, shapes, dtypes, layouts and parameter roles.
- Initialization policy and configuration.
- Forward and backward operator requirements.
- State/cache semantics and sequence behavior.
- Constraints required for serialization, migration and runtime dispatch.

A module registry and model graph are intended to separate architecture description from hardware execution. This is an extensibility contract, not a claim that arbitrary neural graphs can already be generated or trained.

## 4. Device-adaptive configuration

There is no universal parameter count, layer count, hidden size, context length, vocabulary size or batch size for every user.

Genesis derives a feasible model configuration from a measured device capability profile and the capabilities of the researched architecture. It must account for usable memory headroom rather than total advertised RAM, measured CPU capability and supported ISA, actually available and validated GPU/NPU runtime paths, training versus inference memory requirements, thermal/power sustainability and storage budget.

The selected architecture and dimensions are recorded in the generation manifest. A live workload controller may reduce work or pause training, but it cannot silently resize model tensors or change architecture during an update.

## 5. Parameter initialization

Every module declares its own initialization policy. Do not apply one initializer blindly to all tensors.

The policy must account for parameter role, fan-in/fan-out, normalization semantics, residual paths, embedding/output parameterization and architecture-specific stability requirements.

Each model instance receives independent initialization. Persist:
- Initializer algorithm and version per parameter group.
- Configuration and tensor-shape fingerprint.
- Instance and generation identifiers.
- Random-generator metadata sufficient for controlled tests and recovery.
- The committed initial parameter state.

A reproducible test fixture may use a fixed seed. Production instances must not all share a universal seed or identical initialized weights.

## 6. Tokenizer contract

Tokenizer selection remains open. Candidate approaches include byte-level encoding, byte-level BPE and a vocabulary trained from the accepted local corpus.

Once a model generation begins training, its tokenizer revision and token-ID mapping are immutable for that generation. Every checkpoint and model manifest must include the tokenizer fingerprint and vocabulary/special-token metadata. A tokenizer change creates a new compatible generation or requires explicit tensor migration; runtime vocabulary growth is not allowed.

The tokenizer must define Unicode, malformed byte sequence, whitespace, normalization, unknown-token and special-token behavior. Tokenizer research and language evaluation are required before final selection.

## 7. Model identity and generation manifest

Each installation owns a unique instance identity. Each successful model state is a generation with a unique generation identifier and immutable manifest.

The manifest should record:
- Instance ID and generation ID.
- Parent generation, if any.
- Architecture family, module versions and configuration fingerprint.
- Tokenizer ID, revision and fingerprint.
- Parameter names, shapes, dtypes, layouts and checksums.
- Initialization policy and version.
- Corpus/training provenance and training configuration.
- Device capability profile used for configuration.
- Validation results and promotion state.
- Checkpoint schema version.

Personal conversation content must not be embedded in a public or remotely transmitted model manifest.

## 8. Model state and artifacts

The native Argus model-state format must preserve the complete tensor set and metadata needed to reconstruct the exact generation. Optimizer and scheduler state belongs in resumable training checkpoints, not necessarily in an inference-only export.

GGUF, ONNX, TFLite/LiteRT, SafeTensors and vendor formats may be evaluated as interoperability or deployment/export formats. Their existence does not establish that Argus should import third-party pretrained weights or that a format preserves every training-state detail.

Every load or restore validates manifest schema, architecture fingerprint, tokenizer fingerprint, tensor inventory, tensor sizes, dtypes, checksums and finite values before publishing state.

## 9. Generation promotion and rollback

A candidate generation is isolated while training or structural migration occurs. It becomes active only after numerical and serialization integrity checks pass, general evaluation meets its regression threshold, personal evaluation passes where applicable, runtime/resource compatibility is satisfied, and a recoverable checkpoint/manifest is committed atomically.

Keep a last-known-good generation. Failed or regressing candidates must not replace it.

## 10. Explicit non-goals

- No fixed-size foundation model shared across every device.
- No external pretrained base model as Argus identity.
- No automatic cross-user weight or gradient sharing.
- No architecture self-modification without candidate validation.
- No claim that a newly initialized model is already an intelligent assistant.
