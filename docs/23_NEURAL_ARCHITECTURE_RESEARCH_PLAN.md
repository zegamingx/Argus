# Neural Architecture Research Plan

**Status:** Research required; no initial architecture selected  
**Last reviewed:** 2026-11-04

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

1. Complete the candidate and fixture protocol in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md); freeze one versioned tokenizer contract before controlled architecture comparisons.
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
- The tokenizer has passed the evidence and decision gates in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md), and the corpus contract is compatible.
- Known trade-offs and unsupported operations are documented.
- A reviewed architecture decision record is added to document 01.

## 7. Future architecture evolution

Future growth may add parameters, modules or structural variants. Every candidate must be built as a new generation with an explicit parent, migration plan, resource admission, validation and rollback. The active model must not mutate its own graph without an externally enforced validation gate.

## 8. Current decision

**No initial architecture family is selected.** Research candidates are open. Model dimensions and parameter count are per-device outputs after architecture and capability analysis, not global constants.


## 9. External evidence and scope boundaries

These references inform the existing candidate comparison. They do not select an architecture for Argus.

### 9.1 Small-model learning from random initialization

[Eldan and Li, TinyStories (Microsoft Research, 2023)](https://www.microsoft.com/en-us/research/publication/tinystories-how-small-can-language-models-be-and-still-speak-coherent-english/) show that models below 10 million parameters, including very simple Transformer configurations, can generate coherent multi-paragraph stories when trained on a deliberately constrained synthetic story dataset.

This establishes that useful behavior can emerge in a narrow task distribution at small parameter counts. It does not establish broad general-language competence, broad factual knowledge, or training feasibility on Android hardware. Argus must not use TinyStories results as a direct model-size prescription.

### 9.2 Transformer, SSM and hybrid comparison

[Waleffe et al., An Empirical Study of Mamba-based Language Models (NVIDIA and collaborators, 2024)](https://research.nvidia.com/publication/2024-06_empirical-study-mamba-based-language-models) compare 8B Transformer, Mamba, Mamba-2 and Mamba-2-Hybrid models trained on common data at up to 3.5 trillion tokens. The study reports task-dependent differences: pure SSM models lag on some copying, in-context-learning and long-context tasks, while the evaluated 8B hybrid outperforms the Transformer on the reported standard-task average and is predicted to improve inference generation speed.

These are large-scale server-training results. They justify retaining SSM and Hybrid as candidates, but they do not establish the best architecture, layer ratio, training efficiency or mobile performance for Argus-sized models.

### 9.3 Multilingual tokenizer evidence

Tokenizer evaluation must include more than vocabulary size. The controlled candidate families, exact pipeline manifest, multilingual fixture and decision gates are defined in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md).

- [Equity with Efficiency (EMNLP 2026)](https://aclanthology.org/2026.emnlp-main.1783/) evaluates tokenizer efficiency and language parity across eleven Southeast Asian languages and includes controlled 1.5B-parameter training. This supports evaluating Indonesian and neighboring languages explicitly rather than assuming an English-dominant tokenizer is adequate.
- [Scaling Laws or Threshold Effects (Findings of ACL 2026)](https://aclanthology.org/2026.findings-acl.1588/) reports vocabulary-size threshold behavior for BBPE in a specific Mongolian, Tibetan and Uyghur experiment. Its 9K threshold must not be generalized into a universal Argus minimum.
- [Vowel Signs Are Not Letters (2026)](https://arxiv.org/abs/2608.26449) identifies pre-tokenization regex behavior that can impose a structural fertility ceiling for abugida scripts. Argus must inspect the pre-tokenizer and Unicode handling, not only the merge vocabulary.

For every candidate tokenizer, record per-language fertility, bytes per token, normalized sequence length, bits-per-byte where a compatible language-model evaluation exists, special-token behavior, Unicode round-trip behavior, and embedding/output-head cost. Keep data composition and tokenizer revision fixed when comparing architecture candidates.

### 9.4 Research boundary

The current research order is:

1. Define the training-feasibility target and what constitutes useful learning for Argus.
2. Evaluate tokenizer candidates on the same versioned language/domain fixtures.
3. Freeze a common tokenizer/input fixture for controlled architecture prototypes.
4. Compare Transformer, SSM/recurrent and Hybrid with equivalent data and stated parameter/compute budgets.
5. Validate the leading candidates on representative Android devices under sustained thermal and memory conditions.

This order is an experimental dependency, not an architecture decision. Local Knowledge Genesis remains the accepted direction; external pretrained models are not substituted as the Argus foundation by this research plan.
