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

Evaluate selective state-space models (including Mamba-family designs), recurrent linear-time architectures (including RWKV-family designs), and other sequence models that avoid or alter the conventional attention KV-cache pattern.

These are not one interchangeable architecture class. Their state update equations, training parallelization, inference cache/state, numerical behavior, tensor layouts and kernel requirements differ. Compare each family on its own mathematical contract rather than treating “linear-time” or “recurrent” as a complete performance description.

Research questions:
- What is the persistent recurrent state and how is it represented, serialized and updated?
- Which operations admit parallel/chunked training, and which require sequential recurrence at inference?
- What are prefill and single-token decode costs separately?
- How do state size, cache size, activation/workspace memory and memory traffic scale with context and model width?
- Can stable training and backward propagation be implemented with available mobile CPU/GPU kernels?
- What are the quality, recall, copying, in-context-learning and long-context trade-offs?
- What numerical precision and accumulation behavior does each recurrent update require?

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
2. Extract and critically compare existing publications, reference implementations, source code, licenses and Android/runtime documentation. Distinguish architecture results from optimizer, runtime, memory-layout and lifecycle results.
3. Run the tokenizer fixture and candidate comparison in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md) in parallel with tokenizer-neutral architecture research. Define and compare the device-conditioned tokenizer profile portfolio before token-sensitive design freeze. For every controlled architecture comparison, pin the exact tokenizer profile ID/fingerprint and tokenized fixture used by that model configuration; do not assume one global tokenizer across all device profiles.
4. Build an evidence matrix from published measurements and documented analytical estimates. Record the actual model scale, starting weights, dataset, training objective, hardware, runtime, metrics and limitations for every reported result. Do not reinterpret unlike workloads as a controlled head-to-head comparison.
5. Define minimal mathematical reference configurations for each retained component candidate and audit reusable implementations/frameworks before considering integration. Source prototypes or new implementation code require a separately authorized implementation phase.
6. Record the required forward, objective, gradient, optimizer and numerical-parity checks for the later implementation-verification plan; do not describe those checks as passed before code is implemented and tested.
7. Calculate training and inference memory separately, including activations, gradients, optimizer state, recurrent state/KV cache, temporary workspaces and runtime overhead. State all excluded terms and precision assumptions.
8. Compare one component at a time under matched data, parameter/compute, and workload assumptions where existing evidence permits. Report parameter-matched and compute-matched evidence separately. Do not label literature measurements from different devices as device-matched.
9. Record the backend actually used in each published implementation, including fallback and graph partitioning where reported.
10. Inspect checkpoint recovery, model serialization, initialization and candidate-generation compatibility.
11. Publish evidence maturity, uncertainty, component disposition (ADOPT/MODIFY/CUSTOM/DEFER/REJECT) and rejected alternatives before architecture freeze.

Physical Android measurements of Argus candidates are reserved for the post-implementation E1–E7 verification plan. Their absence must be disclosed as an implementation-support uncertainty, not used to leave the literature and analytical research incomplete.

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
- Documentary tokenizer G1-D in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md) must pass before this architecture-selection gate can pass. Its required outputs are an evidence-backed candidate shortlist, accepted product-language boundary, rights-cleared tokenizer-development data, device-conditioned profile and selection/fallback policy, bounded local-training mode, artifact lifecycle and per-profile fingerprint contract. **Current status (2026-11-04): G1-D has not passed.** The product-language priority matrix and exact rights for a tokenizer-training frequency corpus and (for base PA-BPE) an aligned development corpus remain open. The static runtime/API candidate review is now recorded in document 34, but the exact per-profile runtime/ABI contract is not yet selected. No Argus app/device conformance or downstream model training is required to complete G1-D; those are G1-V obligations after implementation.
- Component-level evidence and the proposed G0–G4 design gates in [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md) have been reviewed.
- Component/architecture comparisons state whether evidence is direct, indirect, server-scale, pretrained fine-tuning or from-scratch; budget and tokenizer differences are disclosed.
- Analytical model-state, memory, compute, sequence/context, tokenizer-preparation and runtime estimates are documented across provisional device capability envelopes, with unresolved assumptions visible.
- The corpus/source rights needed for the chosen architecture's design and evaluation fixture are recorded; full corpus acceptance and production neural-training eligibility remain under the separate data-governance gate.
- Unsupported operators, implementation risks, alternatives, migration requirements and the tests still needed are documented.
- A reviewed ADR in document 01 identifies the architecture as a design selection **not yet physically verified or approved for product support**.

Passing this gate authorizes an implementation-plan review only. It does not authorize code changes, builds, device experiments or a product capability claim.

### 6.2 Post-implementation feasibility and support verification

After implementation is separately authorized, the resulting implementation must pass the relevant numerical, resource, lifecycle, checkpoint and physical-device verification. Document 33's E1–E7 protocol defines the from-scratch/device evidence required for a broad feasibility or device-support claim. This later verification is intentionally not a precondition for choosing a design to implement; it remains a hard precondition for claiming that the implementation works on the declared device tier or that the target from-scratch workload is feasible.

## 7. Future architecture evolution

Future growth may add parameters, modules or structural variants. Every candidate must be built as a new generation with an explicit parent, migration plan, resource admission, validation and rollback. The active model must not mutate its own graph without an externally enforced validation gate.

## 8. Current decision

**No initial architecture family is selected.** The G2 comparison scope is bounded to four families: decoder-only Transformer as the mandatory reference/control, selective SSM (Mamba/Mamba-2 SSD), recurrent delta-rule (RWKV-7 family), and attention–SSM/recurrent hybrid as a secondary candidate. This is a shortlist for design comparison, not a ranking or implementation selection. Model dimensions and parameter count remain per-device outputs after architecture and capability analysis, not global constants.


## 9. External evidence and scope boundaries

These references inform the existing candidate comparison. They do not select an architecture for Argus.

### 9.1 Small-model learning from random initialization

[Eldan and Li, TinyStories (Microsoft Research, 2023)](https://www.microsoft.com/en-us/research/publication/tinystories-how-small-can-language-models-be-and-still-speak-coherent-english/) show that models below 10 million parameters, including very simple Transformer configurations, can generate coherent multi-paragraph stories when trained on a deliberately constrained synthetic story dataset.

This establishes that useful behavior can emerge in a narrow task distribution at small parameter counts. It does not establish broad general-language competence, broad factual knowledge, or training feasibility on Android hardware. Argus must not use TinyStories results as a direct model-size prescription.

### 9.2 Transformer, SSM and hybrid comparison

[MobileLLM (Liu et al., ICML 2024)](https://proceedings.mlr.press/v235/liu24ce.html) is useful architecture evidence for sub-billion on-device deployment targets: its study examines deep-and-thin configurations, embedding sharing and grouped-query attention. It is model-design/deployment evidence, not proof that the models are trained from random initialization on a phone.

[Waleffe et al., An Empirical Study of Mamba-based Language Models (NVIDIA and collaborators, 2024)](https://research.nvidia.com/publication/2024-06_empirical-study-mamba-based-language-models) compare 8B Transformer, Mamba, Mamba-2 and Mamba-2-Hybrid models trained on common data at up to 3.5 trillion tokens. The study reports task-dependent differences: pure SSM models lag on some copying, in-context-learning and long-context tasks, while the evaluated 8B hybrid outperforms the Transformer on the reported standard-task average and is predicted to improve inference generation speed.

The architecture mechanisms have separate primary references:
- [Mamba: Linear-Time Sequence Modeling with Selective State Spaces](https://arxiv.org/abs/2312.00752) describes input-dependent state selection and a hardware-aware recurrent parallel algorithm. Its throughput and scaling results are from the paper's evaluated GPU/model configurations, not Android measurements.
- [FlashAttention: Fast and Memory-Efficient Exact Attention with IO-Awareness](https://papers.nips.cc/paper_files/paper/2022/hash/67d57c32e20fd0a7a302cb81d36e40d5-Abstract-Conference.html) describes tiled exact attention that reduces memory IO and avoids full attention-matrix materialization; the quadratic self-attention compute dependence remains.
- [RWKV-7 Goose](https://arxiv.org/abs/2503.14456) and the [RWKV architecture reference](https://github.com/RWKV/RWKV-wiki/blob/main/docs/basic/architecture.md) describe dynamic matrix-valued recurrent state. The state is constant with respect to context length, not independent of model dimensions.
- The [Hugging Face KV-cache explanation](https://huggingface.co/docs/transformers/cache_explanation) documents per-layer key/value caching and context-proportional cache growth; its interface is an explanatory reference, not a required Argus runtime dependency.


These are large-scale server-training results. They justify retaining SSM and Hybrid as candidates, but they do not establish the best architecture, layer ratio, training efficiency or mobile performance for Argus-sized models.

### 9.3 State-space and recurrent evidence

[Dao and Gu, *Transformers are SSMs: Generalized Models and Efficient Algorithms Through Structured State Space Duality* (ICML 2024)](https://proceedings.mlr.press/v235/dao24a.html) formalize the relationship between structured state-space models and attention variants. Their Mamba-2 State Space Duality (SSD) core layer is reported as 2–8× faster than the compared Mamba selective-SSM core in the paper's evaluated setup, while remaining competitive with Transformers on the reported language-modeling benchmarks. This is a core-layer result, not a measured Android end-to-end training or decode result.

The large-scale [*An Empirical Study of Mamba-based Language Models*](https://research.nvidia.com/publication/2024-06_empirical-study-mamba-based-language-models) compares 8B Transformer, Mamba, Mamba-2 and a Mamba-2/attention hybrid under common large-scale training conditions. Its findings are task-dependent: pure SSM variants show weaknesses on some copying, in-context-learning and long-context tasks, while its evaluated hybrid is stronger on the paper's reported standard-task average. These server-scale findings justify keeping SSM and hybrid candidates in the research set; they do not determine an Argus-scale layer ratio or Android winner.

A separate [system analysis of Mamba-2 SSD](https://doi.org/10.1109/LCA.2025.3609283) identifies that recurrent computation can constrain prefill throughput and studies how SSD changes parallelism and compute/memory behavior. This is an important qualification: a recurrent architecture does not automatically make every prompt-processing workload faster. Argus must separate prefill, decode, training and checkpoint costs.

[Peng et al., *RWKV-7 “Goose” with Expressive Dynamic State Evolution* (2025)](https://arxiv.org/abs/2503.14456) describe a recurrent architecture with constant-size recurrent inference state and parallelizable training, and report models from 0.19B to 2.9B parameters trained on a 3.1-trillion-token multilingual corpus. The paper's scale and training resources are not evidence of smartphone training feasibility. RWKV is nevertheless a concrete recurrent candidate whose state serialization, training kernels, initialization, tokenizer coupling, memory traffic and Android implementation maturity should be assessed rather than hiding it inside a generic “SSM” label.

**Research implication:** the comparison set must distinguish at least (a) dense attention Transformer, (b) selective SSM/Mamba-family, (c) recurrent delta-rule/RWKV-family and (d) attention–SSM hybrid. A family may be excluded later with an evidence-backed rationale; none is preselected by this list.

### 9.4 Multilingual tokenizer evidence

Tokenizer evaluation must include more than vocabulary size. The controlled candidate families, exact pipeline manifest, multilingual fixture and decision gates are defined in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md).

- [Equity with Efficiency (EMNLP 2026)](https://aclanthology.org/2026.emnlp-main.1783/) evaluates byte-level BPE, Parity-aware BPE, morphology-driven byte encoding and Byte Latent Transformer across eleven SEA languages including Indonesian, with 1.5B decoder-only models. It supports direct Indonesian/SEA tokenizer research, while the task-specific winners and server-scale setting prevent a universal tokenizer claim.
- [Parity-Aware BPE (ACL 2026)](https://aclanthology.org/2026.acl-long.342/) specifies the fair-max merge objective and base/hybrid/window variants; it requires careful aligned development data and normalization. The official [reference implementation is MIT-licensed](https://github.com/swiss-ai/parity-aware-bpe), but its Python/Hugging Face workflow does not constitute an Android runtime integration.
- [Language-Specific Effects of Tokenizer Choice (2026 preprint)](https://arxiv.org/abs/2610.12144) shows tokenizer sensitivity varies with language-model data share and that simple over-weighting of tokenizer-training data for low-resource languages can backfire; evaluate per-language and avoid arbitrary equal weighting.
- [TokSuite (ICML 2026)](https://proceedings.mlr.press/v306/altintas26a.html) supplies a controlled 14-tokenizer model panel for English, Chinese, Farsi, Italian and Turkish perturbation robustness; it is a methodology reference, not Indonesian-specific evidence.
- [Scaling Laws or Threshold Effects (Findings of ACL 2026)](https://aclanthology.org/2026.findings-acl.1588/) reports vocabulary-size threshold behavior for BBPE in a specific Mongolian, Tibetan and Uyghur experiment. Its 9K threshold must not be generalized into a universal Argus minimum.
- [Vowel Signs Are Not Letters (2026)](https://arxiv.org/abs/2608.26449) identifies pre-tokenization regex behavior that can impose a structural fertility ceiling for abugida scripts. Argus must inspect the pre-tokenizer and Unicode handling, not only the merge vocabulary.

For every candidate tokenizer, record per-language fertility, bytes per token, normalized sequence length, bits-per-byte where a compatible language-model evaluation exists, special-token behavior, Unicode round-trip behavior, and embedding/output-head cost. Keep data composition and tokenizer revision fixed when comparing architecture candidates.

### 9.5 Research boundary and device-conditioned selection

The documentation-stage research sequence is:

1. Define the learning target and feasibility envelope from published evidence and analytical resource accounting.
2. Compare tokenizer families using the versioned language/domain fixture and record the tokenizer design-time decision.
3. Compare dense Transformer, selective SSM/Mamba, recurrent delta-rule/RWKV and hybrid candidates using available controlled literature, implementation audits and analytical estimates.
4. Identify which conclusions are general, which depend on the device/runtime, and which remain implementation-dependent.
5. Prepare the pre-implementation architecture contract with explicit configuration candidates, exclusions, assumptions and later E1–E7 verification obligations.

A key open design question is whether Genesis chooses (a) different dimensions/configurations within one supported architecture family, (b) among a small portfolio of architecture families according to the analytically derived capability envelope and later verified backend support, or (c) a staged combination of the two. The research must compare these policies for engineering cost, portability, model quality, kernel maturity, checkpoint/migration complexity and device coverage. Device adaptation must not mean maintaining duplicated model implementations for every SoC vendor. The chosen policy and exact support tiers must be recorded in a reviewed ADR.


### 9.6 Architectural mechanics and analytical trade-offs

The following comparison records structural properties that can be derived from the model definitions and cited papers. It is not a benchmark ranking. Exact memory must be calculated from the selected implementation's tensor shapes, precision and execution schedule.

| Family | Training sequence execution | Autoregressive inference state | Dominant sequence-length behavior | Main mobile implementation risk |
|---|---|---|---|---|
| Dense attention Transformer | Causal attention over a training sequence can be computed in parallel across token positions; backward retains/recomputes activations and attention intermediates | Per-layer K/V cache; storage grows linearly with context length | Standard full attention has quadratic sequence compute. Naive implementations can also materialize quadratic attention intermediates | Large activation/logit memory, K/V growth, and dependence on efficient attention/normalization/FFN kernels |
| Selective SSM / Mamba | Recurrent equations can be parallelized with scan-style algorithms; Mamba uses hardware-aware selective-scan kernels | Fixed recurrent state per layer plus convolution state; state size depends on layer/model dimensions, not the number of past tokens | Linear sequence scaling is the model objective, but scan, chunking, memory traffic and prefill behavior still matter | Specialized selective-scan kernels and stable backward path may be unavailable in a generic Android CPU/GPU/NPU runtime |
| RWKV-7 recurrent delta-rule | Training is parallelizable through its chunkwise/matrix formulation; it is not restricted to token-by-token training | Matrix-valued recurrent state per layer/head plus other recurrent states; independent of context length but scales with model/head dimensions | Recurrent decode processes state sequentially per token; fixed context-length state does not mean negligible state bytes | Matrix-state update kernel, precision/stability, checkpoint layout and mobile parallelization need direct implementation audit |
| Attention–SSM/recurrent hybrid | Depends on how blocks are interleaved; both parallel attention and recurrent scan/update paths are required | Combination of K/V caches for attention blocks and recurrent state for SSM/RWKV blocks | Mixed scaling: attention portions retain context-dependent K/V and attention cost; recurrent portions retain fixed-size state | More operator families, graph boundaries, memory ownership, checkpoint state and backend fallback paths |

**Transformer memory accounting.** For a conventional decoder cache, the K/V tensor storage can be estimated as:

M_KV = 2 × N_layers × B × S × N_KV_heads × D_head × bytes_per_element

where the leading 2 represents key and value, B is the number of concurrently cached sequences, S is cached context, and grouped-query/multi-query attention reduces N_KV_heads relative to query-head count. This is an inference cache estimate only; it excludes model weights, activation/workspace memory, temporary logits and allocator overhead. It must be calculated using the actual attention layout, not a marketing parameter count.

Standard self-attention has quadratic sequence compute. FlashAttention uses tiling and IO-aware execution to reduce intermediate memory traffic and avoid storing the full attention matrix, but does not turn dense exact attention into a linear-compute recurrent model. A mobile target can use such an optimization only if a compatible implementation/backend exists; the paper's GPU results do not prove an Android kernel is available.

**Recurrent-state accounting.** Mamba and RWKV state are independent of the number of prior tokens at inference, but are not zero-cost or universally small. State bytes scale with the model's layer count and internal state dimensions. RWKV-7 uses matrix-valued state; its storage therefore depends on the key/value state dimensions per head. Mamba-family variants include selective SSM state and convolution state, with their shapes depending on the particular variant. The implementation must enumerate each recurrent tensor and include cache/state snapshots in memory and checkpoint accounting.

**Training/inference distinction.** A model can have efficient recurrent inference and still require expensive backward kernels, large training activations, scan workspaces or a difficult parallel training implementation. Conversely, Transformer attention optimizations developed for GPU pretraining cannot be assumed available in a phone's runtime. Every architecture candidate must therefore maintain separate evidence rows for (1) from-scratch training, (2) pretrained fine-tuning, (3) inference prefill, and (4) autoregressive decode.

Physical Android benchmarking of Argus implementations occurs after implementation authorization. Those measurements validate the selected design and define actual device support; they do not gate completion of this evidence synthesis. Local Knowledge Genesis remains the accepted direction; external pretrained models are not substituted as the Argus foundation by this research plan.
