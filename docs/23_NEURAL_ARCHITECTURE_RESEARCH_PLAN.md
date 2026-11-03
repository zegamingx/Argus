# Neural Architecture Research Plan

**Status:** Research required; no initial architecture selected  
**Last reviewed:** 2026-11-03

## 1. Research objective

Select an initial neural architecture for Argus based on measured suitability for local initialization, from-scratch learning, inference and long-term extensibility on Android devices.

The decision is deliberately open. A familiar architecture must not be selected merely because it is familiar, and a newer architecture must not be selected merely because it is novel.

## 2. Candidate families

### A. Decoder-only Transformer

Evaluate dense attention, grouped-query attention and other attention variants where justified.

Research questions:
- How much training memory is required for weights, activations, gradients and optimizer state?
- How does context length affect training and inference memory?
- Which kernels are required for a correct complete backward path?
- Can the implementation sustain useful throughput under mobile thermal limits?

### B. State-space and recurrent sequence models

Evaluate selective/state-space sequence models and other recurrent designs.

Research questions:
- What is the persistent recurrent state and how is it represented?
- Can stable training and backward propagation be implemented with available mobile kernels?
- How does long-sequence throughput compare under realistic memory movement?
- What are the quality and context-retention trade-offs?

### C. Hybrid architectures

Evaluate combinations of attention and state-space/recurrent or other specialized neural modules.

Research questions:
- Does each module contribute a measurable capability or efficiency gain?
- What is the extra cost of synchronization, conversion and graph boundaries?
- Can training and checkpointing preserve exact module state?
- Does hybridization improve sustained device-level outcomes rather than headline peak speed?

### D. Additional research candidates

Other architectures may be added when a credible paper, reproducible implementation, licensing path and mobile feasibility argument exist. Candidate inclusion is not an implementation commitment.

## 3. Required comparison matrix

| Criterion | Required measurement |
|---|---|
| From-scratch trainability | Complete forward/loss/backward/optimizer correctness |
| Initialization | Stable module-specific initialization and reproducibility |
| Parameter efficiency | Capability per parameter and per training token |
| Training memory | Peak and sustained memory including optimizer state |
| Training throughput | Tokens/sec and optimizer updates/sec on target devices |
| Inference | Prefill, decode latency and sustained throughput |
| Context behavior | Memory/compute scaling and long-context quality |
| Mobile thermal | Sustained performance, pause frequency and energy |
| Runtime portability | CPU kernels first, then verified GPU/vendor paths |
| Numerical stability | Gradient checks, finite values and reference parity |
| Serialization | Complete model/checkpoint round-trip |
| Extensibility | Ability to register new modules without breaking old generations |
| Evolution migration | Tensor-shape migration, compatibility and rollback |
| Research maturity | Public specification, reproducibility, implementation and license |

## 4. Experimental method

1. Define common tokenizer candidates and controlled text fixtures so architecture comparisons use the same input representation where valid.
2. Build minimal, mathematically correct prototypes for each candidate family.
3. Validate forward outputs, objective values, gradients and optimizer updates against trusted reference implementations.
4. Measure training memory separately from inference memory.
5. Benchmark at comparable parameter and compute budgets on named Android devices.
6. Record backend actually used, including fallback and graph partitioning.
7. Run sustained thermal tests; do not rank architectures on a short peak benchmark.
8. Evaluate general text modeling and sequence behavior with held-out data.
9. Inspect checkpoint recovery and model serialization.
10. Publish trade-offs, uncertainty and rejected candidates before selecting the first architecture.

The research corpus must be used consistently. Any corpus or tokenizer change during comparison must be versioned and documented.

## 5. Architecture-agnostic foundation requirements

AGE should separate:
- Model graph and module registry.
- Tensor specification and layout.
- Parameter initialization policies.
- Forward and backward operator contracts.
- Optimizer/trainable-parameter definitions.
- Runtime backend capability interfaces.
- Serialization and checkpoint formats.
- Evaluation and generation promotion.

This abstraction enables controlled research and future module families. It does not mean every graph can be executed efficiently on every backend.

## 6. Selection gate

An architecture may be selected only when:
- The minimal implementation has mathematically validated training.
- Memory and compute fit at least a defined target device tier.
- Sustained training/inference results are reproducible.
- Complete model and optimizer states can be checkpointed and restored.
- Tokenizer and corpus contracts are compatible.
- Known trade-offs and unsupported operations are documented.
- A reviewed architecture decision record is added to document 01.

## 7. Future architecture evolution

Future growth may add parameters, modules or structural variants. Every candidate must be built as a new generation with an explicit parent, migration plan, resource admission, validation and rollback. The active model must not mutate its own graph without an externally enforced validation gate.

## 8. Current decision

**No initial architecture family is selected.** Research candidates are open. Model dimensions and parameter count are per-device outputs after architecture and capability analysis, not global constants.
