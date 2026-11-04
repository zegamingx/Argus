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

1. Define the component inventory and tokenizer-neutral/tokenizer-sensitive boundaries using [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md).
2. Extract Android prior-art findings at component level. Distinguish model architecture results from optimizer, runtime, memory-layout and lifecycle results.
3. Run the tokenizer fixture and candidate comparison in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md) in parallel with tokenizer-neutral architecture research. Freeze the tokenizer contract before any token-sensitive ablation or final model comparison.
4. Define minimal mathematical reference configurations for each retained component candidate and audit reusable implementations/frameworks before considering integration. Source prototypes or new implementation code require a separately authorized implementation phase.
5. Record the required forward, objective, gradient, optimizer and numerical-parity checks for the later implementation-verification plan; do not describe those checks as passed before code is implemented and tested.
6. Measure training memory separately from inference memory, including activation and optimizer state.
7. Compare one component at a time under matched data, parameters/compute, device and sustained conditions. Report parameter-matched and compute-matched results separately.
8. Record backend actually used, including fallback and graph partitioning.
9. Inspect checkpoint recovery, model serialization, initialization and candidate-generation compatibility.
10. Publish evidence, uncertainty, component disposition (ADOPT/MODIFY/CUSTOM/DEFER/REJECT) and rejected alternatives before architecture freeze.

The research corpus must be used consistently. Any corpus or tokenizer change during comparison must be versioned and documented. Published Android fine-tuning speedups do not establish from-scratch architecture superiority.

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

## 6. Selection and verification gates

### 6.1 Pre-implementation architecture design selection

The architecture may be selected as a **design contract for implementation planning** when:
- The from-scratch feasibility evidence boundary, unresolved risks and provisional minimum-device assumptions in [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md) are explicit. This does not claim that E1–E7 passed or that broad on-device pretraining is proven.
- The tokenizer has passed the design-time G1 shortlist/contract gate in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md), including a permitted fixture and stable artifact/fingerprint contract.
- Component-level evidence and the proposed G0–G4 design gates in [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md) have been reviewed.
- Component/architecture comparisons state whether evidence is direct, indirect, server-scale, pretrained fine-tuning or from-scratch; budget and tokenizer differences are disclosed.
- Analytical model-state, memory, compute, sequence/context and runtime estimates are documented against a provisional device target, with unresolved assumptions visible.
- The corpus/source rights needed for the chosen architecture's design and evaluation fixture are recorded; full corpus acceptance and production neural-training eligibility remain under the separate data-governance gate.
- Unsupported operators, implementation risks, alternatives, migration requirements and the tests still needed are documented.
- A reviewed ADR in document 01 identifies the architecture as a design selection **not yet physically verified or approved for product support**.

Passing this gate authorizes an implementation-plan review only. It does not authorize code changes, builds, device experiments or a product capability claim.

### 6.2 Post-implementation feasibility and support verification

After implementation is separately authorized, the resulting implementation must pass the relevant numerical, resource, lifecycle, checkpoint and physical-device verification. Document 33's E1–E7 protocol defines the from-scratch/device evidence required for a broad feasibility or device-support claim. This later verification is intentionally not a precondition for choosing a design to implement; it remains a hard precondition for claiming that the implementation works on the declared device tier or that the target from-scratch workload is feasible.

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
