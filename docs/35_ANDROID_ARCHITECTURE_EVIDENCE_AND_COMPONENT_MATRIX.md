# Android Architecture Evidence and Component Comparison Matrix

**Status:** Research synthesis and decision framework; no architecture selected  
**Last reviewed:** 2026-11-04  
**Research stage:** Stage 4 — Parallel architecture comparative research  
**Parallel inputs:** research-backed and analytical feasibility boundary plus later E1–E7 protocol in [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md); documentary tokenizer shortlist G1-D plus later runtime conformance G1-V in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md); Transformer-specific component comparisons and ablation contracts in [39 — Transformer Component Research and Pre-G4 Design Contracts](39_TRANSFORMER_COMPONENT_RESEARCH_AND_DESIGN_CONTRACTS.md); random-initialization/width-scaling research in [40 — Initialization and Width-Scaling Research](40_INITIALIZATION_AND_WIDTH_SCALING_RESEARCH.md); Mamba-2/RWKV-7 recurrent state, chunk, document-reset and recovery contracts in [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md); and the exact symbolic parameter/operator/memory inventory for all four retained families in [42 — Architecture-Family Tensor and Operator Inventory](42_ARCHITECTURE_TENSOR_AND_OPERATOR_INVENTORY.md). G1-D informs token-sensitive design; it does not block tokenizer-neutral research or require Argus app/device tests. Documents 39–42 add evidence and contracts but do not alter G3 dispositions. The consolidated hard-gate, profile-manifest, comparison and ADR acceptance method is in [43 — G4 Architecture Decision and Device-Profile Contract](43_G4_ARCHITECTURE_DECISION_AND_PROFILE_CONTRACT.md).  
**Authority:** G0 research scope/evidence controls, G2 family shortlist and G3 component-disposition baseline are accepted planning outputs under ADR-024/025/026 in document 01. G1-D and G4 remain open research/design gates; this document is not an implementation authorization or architecture freeze.

## 1. Purpose

Architecture research should proceed in parallel with tokenizer comparison, but it must not treat whole-model results as proof that every component inside that model is optimal.

The mobile literature contains practical evidence for native training runtimes, memory-aware execution, activation checkpointing, gradient accumulation, parameter sharding, mobile-GPU tensor layouts, thermal/energy scheduling and local adapter updates. Most of those studies evaluate fine-tuning of an already trained model. They do not isolate the relative merits of positional encoding, normalization, residual topology, FFN family, attention variant or backbone family for a locally initialized Argus model.

This document separates:
1. **Android prior art:** what physical-device papers and implementations actually measured.
2. **Genesis constraints:** the product and lifecycle conditions that every candidate must satisfy.
3. **Component comparison:** tokenizer-neutral and tokenizer-sensitive choices, evidence gaps and preliminary dispositions.
4. **Existing-framework reuse:** what should be reused, adapted, custom-built or rejected.
5. **Decision gates:** what may proceed before tokenizer freeze and what must wait.

The objective is not to reimplement a published mobile training framework. It is to reuse mature methods where they fit, add the minimum Argus-specific adapters/contracts, and validate only the unresolved questions.

## 2. Accepted Genesis constraints

These constraints are inputs to architecture research, not outcomes that a candidate may silently change.

| ID | Constraint | Architecture/runtime consequence |
|---|---|---|
| G-01 | **Local Knowledge Genesis:** model weights are initialized locally; no external pretrained weights define the foundation model | An architecture candidate must support a valid random-initialization path and full training graph. A fine-tuning-only demonstration is supporting systems evidence, not Genesis proof. |
| G-02 | **Independent instance evolution:** every installation owns its model, checkpoints, memory and learning state | No central model, federated aggregation, automatic cross-user weight exchange or hidden synchronization. |
| G-03 | **Local training computation:** the model's training forward/backward/update path remains on the owning device | Server-assisted backward, cloud side-network computation, cross-device activations or gradients are outside the accepted baseline. |
| G-04 | **First-launch readiness:** app readiness follows successful local construction, initialization and durable initial-generation commit; pretraining does not block startup | Architecture construction and initialization must be bounded; corpus training is post-readiness background work. |
| G-05 | **From-scratch general learning:** the architecture must define complete forward, loss, backward and optimizer semantics for locally initialized parameters | Inference-only accelerator support is not sufficient. |
| G-06 | **Device-adaptive configuration:** no universal parameter count, width, depth, batch, sequence length, tokenizer vocabulary or training duration is assumed | Candidate bundles must be evaluated against a measured capability/resource envelope; tokenizer eligibility remains governed by product language scope and source rights. |
| G-07 | **CPU correctness reference:** the supported model subset must have a numerically validated execution path; GPU/NPU execution is separately verified | Accelerator routing requires real backward/operator coverage, precision checks and fallback evidence. |
| G-08 | **Android-first lifecycle:** background learning is pausable, resumable and responsive to memory, thermal, battery, storage and OS scheduling | Architecture/runtime choices must support bounded work, cancellation, safe checkpoint boundaries and observable fallback. |
| G-09 | **General and personal learning are separate** | LoRA/adapter studies may inform later personal adaptation but cannot silently replace the from-scratch general-learning path. |
| G-10 | **Versioned generations:** active model state is immutable; training writes isolated candidate generations that are validated before promotion | Architecture, tokenizer and tensor-layout changes need explicit fingerprints, migration and rollback rules. |
| G-11 | **Current implementation is a prototype** | The inspected Argus repository does not currently contain the production native C++/JNI training runtime or verified complete backpropagation/checkpoint path. Research documents do not imply those capabilities exist. |

The current repository snapshot declares Android minSdk 33 / targetSdk 36. These values describe the inspected prototype, not a final device-support matrix. Minimum RAM, named SoC tiers and verified accelerator support remain open. See [README](../README.md), [17 — Current Repository Baseline](17_CURRENT_REPOSITORY_BASELINE.md), [19 — P0 Remediation Design](19_P0_REMEDIATION_DESIGN.md) and [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md).

## 3. Android training prior-art evidence table

The papers below are evidence for specific techniques and test configurations. The labels in this table are intentionally narrower than “Android supports training.”

| Evidence | Device/workload actually studied | Measured contribution | Evidence category | Argus relevance and boundary |
|---|---|---|---|---|
| **MobileFineTuner** — [paper](https://arxiv.org/abs/2512.08211), [Apache-2.0 repository LICENSE](https://github.com/Edge-Intelligence-Lab/MobileFineTuner/blob/main/LICENSE), [Android SDK docs](https://github.com/Edge-Intelligence-Lab/MobileFineTuner/blob/main/docs/ANDROID_SDK.md) | Paper device table includes Huawei P50 Pro / Android 11 / Kirin 9000 / 8 GB; iQOO 15 / Android 16 / Snapdragon 8 Elite Gen 5 / 16 GB+16 GB; also Huawei Nova 9 Pro / HarmonyOS and MacBook Air. Full-FT evaluation uses GPT-2 124M on iQOO 15 at sequence 128, batch 8. PEFT evaluations cover five models/six datasets on iQOO 15. | Native C++ framework; model/parameter sharding, gradient accumulation, activation checkpointing, memory-efficient attention and energy-aware scheduling. The paper shows that the 8 GB Huawei P50 Pro requires combinations of these optimizations for selected larger tasks. | **Fine-tuning + runtime optimization** | Closest reference for mobile-native LLM training systems. Reuse its measured lessons and inspect code/contracts, but do not treat the published evaluations as random-init Genesis proof. The current Android SDK documentation separately exposes an arm64-v8a AAR/JNI API with a LoRA `trainStep` entry point and a one-step synthetic GPT-2 smoke test; the documented Android Java API does not provide a complete dataset loop or checkpoint manager. Paper capability and current Android SDK surface must not be conflated. |
| **FBLayout** — [paper](https://arxiv.org/abs/2607.21624), [MobiSys 2026 public review](https://sigmobile.org/mobisys/2026/accepted_papers/); paper metadata states CC BY-NC-ND 4.0 | Seven Transformer fine-tuning models across physical phones with ARM Mali and Qualcomm Adreno mobile GPUs. | R-Tile tensor layout, index transformation to avoid physical movement, activation-guided global layout selection; reports 2.2×–5.7× over evaluated MNN/TFLite/TVM baselines. | **Fine-tuning + mobile-GPU runtime optimization** | Strong evidence that tensor organization and forward/backward memory access are critical on mobile GPUs. It does not show that a particular attention variant, positional encoding or Transformer backbone is best. The official review notes dependence on current GPU characteristics and relatively stable graphs. |
| **PocketLLM** — [ACL Anthology paper](https://aclanthology.org/2024.privatenlp-1.10/) | OPPO Reno 6 smartphone; local fine-tuning of pretrained RoBERTa-large and OPT-1.3B. | Derivative-free / zeroth-order optimization reduces the gradient and optimizer-state memory burden; reports approximately 4 GB and 6.5 GB memory for its tested models. | **Fine-tuning + optimizer method** | Useful alternative to study where memory dominates. It has different convergence/compute behavior from standard backpropagation and must not be assumed to be the best method for general training from random initialization. |
| **Confidant** — [ACM MobiCom 2025](https://doi.org/10.1145/3680207.3723487) | Collaborative pipeline-parallel fine-tuning. The abstract says three consumer-grade mobile devices; the detailed 40.1-hour Phi-2.7B statement specifies one phone plus two laptops. The paper also reports separate phone experiments for BERT-Base and GPT-2 Medium. | Dynamic model partitioning, intra-device scheduling and fault tolerance across heterogeneous collaborating devices. | **Collaborative fine-tuning** | Useful for understanding partitioning and heterogeneous scheduling. Its cross-device activation/gradient exchange and mixed-device 40.1-hour setup conflict with G-02/G-03; do not present this as standalone training on three smartphones. |
| **Gboard production training** — [Google Research](https://research.google/blog/advances-in-private-training-for-production-on-device-language-models/) | Production mobile language-model updates on user devices; deployed model collection across multiple languages/regions. | Federated learning and differential privacy for model updates; base neural LMs are pretrained on multilingual C4 before client training. | **Production federated adaptation** | Strong evidence for mobile data policy, privacy, resource budgeting and update scheduling. Does not demonstrate random-init independent training; server aggregation and shared model lifecycle conflict with current Argus design. |
| **Online-SDFT** — [project](https://lin826.github.io/SLM-Online-SDFT/), [Android repository](https://github.com/lin826/SLM-Online-SDFT) | Physical Android proof of concept; frozen pretrained LFM2.5-230M with local rank-4 LoRA update, replay and adapter checkpoints. | Demonstrates a local continual-adaptation loop and persistence across restarts in a narrow project setup. | **Local adapter fine-tuning proof of concept** | Useful for future personal-learning lifecycle and replay patterns. It does not provide broad general pretraining evidence; the project reports that Android latency, memory, battery, thermal and background reliability remain unmeasured. |
| **MobiLLM** — [paper](https://arxiv.org/abs/2502.20421) | Phone plus high-performance server; server performs trainable side-network backward computation. | Server-assisted side-tuning reduces phone memory/compute. | **Server-assisted fine-tuning** | Excluded by G-03. Raw data remaining on-device does not make the training computation local-only. |
| **XPerT** — [ACM MobiSys 2025](https://doi.org/10.1145/3711875.3729132) | Smartphone personalization with personalized models and cloud model cache. | Model selection/cache can reduce data requirements for personalization. | **Server-assisted personalization** | Data-efficiency reference only. Cloud model-state exchange is not part of independent Argus evolution. |
| **Prism** — [ACM MobiCom 2026 proceedings](https://sigmobile.org/mobicom/2026/accepted_papers_schedule.html), [DOI](https://doi.org/10.1145/3795866.3844151) | MobiCom 2026 program identifies the paper in the on-device LLM session; publisher metadata reports pages 495–510 and online publication 2026-10-26. ACM full text was not accessible during this review, and author/public pages located in this pass provide bibliographic metadata rather than the full experimental method. | Zeroth-order on-device LLM fine-tuning is established as a current research topic; no verified device/model/memory/latency values have been extracted. | **Bibliographic verification complete; technical paper audit pending** | Do not use numerical results, device compatibility, convergence or optimizer claims until the complete paper and experimental setup are inspected. Keep it as a high-priority literature audit item, not a selected Argus optimizer. |

### 3.1 Important distinction: paper versus deployable SDK

The MobileFineTuner paper reports a broader set of research experiments than the currently documented Android SDK interface. The Android SDK's published surface includes:
- Android AAR and JNI bridge to a native C++ training core.
- `arm64-v8a` packaging.
- Runtime-supplied model assets stored as readable files.
- LoRA initialization and a `trainStep` API.
- A synthetic GPT-2 LoRA instrumentation smoke step.

Its Android documentation describes the Java API at step granularity and does not expose a full dataset scheduler/loop or checkpoint manager. Therefore:
- Treat paper measurements as published research evidence.
- Treat the SDK as a candidate native training-step component/reference implementation.
- Do not assume it can be dropped into Argus as a complete production training subsystem.
- Before any reuse, inspect exact source/license, operator coverage, random-init model construction, optimizer-state serialization, thread-safety, cancellation, error semantics and checkpoint APIs.

### 3.2 Prior-art synthesis

What is already evidenced:
- Native C++ LLM training steps can be exposed to Android applications.
- LLM fine-tuning has been demonstrated on physical Android smartphones.
- Memory-efficient attention, activation checkpointing, gradient accumulation and parameter sharding can be composed to fit selected phone/model combinations.
- Mobile GPU tensor layout can materially affect forward/backward performance on Adreno and Mali.
- Local adapter updates and checkpoint persistence are possible in a narrow Android proof of concept.
- Federated production systems demonstrate privacy/resource controls for mobile language-model updates.

What remains unproven for Argus:
- Broad general-purpose language pretraining from random initialization on one independent Android device.
- Whether the best backbone at small Argus scale is Transformer, SSM/recurrent or Hybrid.
- Which positional encoding, attention variant, FFN, normalization and residual topology is best for the Argus data/compute/device envelope.
- Full-backward support and numerical parity for the selected accelerator paths.
- An integrated Android dataset loop, complete optimizer/checkpoint lifecycle, lifecycle recovery and sustained thermal behavior for the Argus-specific model.

## 4. Component-level comparison matrix

The dispositions below are the accepted **G3 planning baseline**, not final architecture or implementation decisions. Each table row has one primary disposition. The five permitted labels have these meanings:

- **ADOPT:** an established invariant or technique is accepted for the defined role, with its boundary stated.
- **MODIFY:** reuse the principle or existing implementation, but adapt interfaces, semantics or device behavior to Genesis.
- **CUSTOM:** Argus needs a product-specific contract or mechanism not supplied by the external research.
- **DEFER:** keep the candidate open until a dependency or missing evidence is resolved.
- **REJECT:** incompatible with the accepted product boundary or unsupported by the intended scope.

| Component | Tokenizer relationship | Android evidence | Candidate space | Preliminary disposition | Expected benefit / cost or risk | Validation required |
|---|---|---|---|---|---|---|
| **Backbone family** | Indirectly sensitive through sequence length and vocabulary/head cost | Mobile LLM studies are dominated by pretrained Transformer fine-tuning; no reviewed Android study establishes small random-init SSM/Hybrid superiority | Decoder-only Transformer; SSM/recurrent; Hybrid | **DEFER** | Transformer has the strongest mobile fine-tuning prior art; SSM/Hybrid may change sequence-state and kernel costs but have less mobile training evidence | Compare minimal mathematically correct references under matched tokenizer, data, parameter and compute budgets; include sustained device tests |
| **Attention semantics / variant** | Tokenizer-neutral per token; tokenizer-dependent sequence length changes aggregate cost | GQA literature distinguishes MHA→GQA/MQA uptraining from scratch design; MobileLLM directly studies GQA in 125M/350M from-scratch models, but uses 32 A100 GPUs and is not Android training evidence | Dense causal MHA reference; GQA; MQA only as a quality-risk/memory extreme; other patterns only with direct evidence | **DEFER** | GQA/MQA can reduce KV-cache storage/bandwidth, but MQA may degrade quality; KV-cache reduction does not proportionally reduce all attention FLOPs or training activation memory. See [39 — Transformer Component Research](39_TRANSFORMER_COMPONENT_RESEARCH_AND_DESIGN_CONTRACTS.md) | Matched parameter and cache comparisons, causal/gradient parity, copy/retrieval tasks, context behavior, full forward/backward memory and actual backend behavior after G1 |
| **Attention kernel / memory layout** | Tokenizer-neutral semantics; shape and sequence length still affect layout performance | MobileFineTuner and FBLayout provide system/runtime evidence for memory-efficient attention and forward/backward tensor-layout optimization in pretrained fine-tuning | Tiled/memory-efficient kernels; layout-specific backend adapters such as R-Tile where compatible | **MODIFY** — optimization technique only | Reuse/adapt measured memory-layout principles after code/license/operator audit; speedups are device- and graph-specific and do not select the attention architecture | Forward/backward numerical parity, layout-conversion cost, peak memory, cache/memory behavior and sustained physical-device performance after implementation authorization |
| **Positional encoding** | Effective behavior depends on tokenizer-dependent sequence length/context | RoPE and ALiBi have primary-paper evidence for relative/distance-based positional behavior, but no reviewed Android study selects one for local random-init training | RoPE; ALiBi; learned absolute; architecture-native recurrent/state order | **DEFER** | No method guarantees arbitrary context extrapolation. Position indices, packing/reset semantics and precision become model-generation contracts. Compare evidence and limits in document 39 | Fixed-tokenizer train/longer-context evaluation, packed-document position reset/continuation, finite-value/gradient checks, cache/score cost and quality |

| **FFN / expert routing** | Mostly tokenizer-neutral per token; sequence length affects total compute | Shazeer reports gated FFN gains over ReLU/GELU in T5 transfer learning; MobileLLM reports a SwiGLU gain in 125M/350M models trained on 32 A100 GPUs, not Android | Dense GELU/ReLU control; parameter-matched SwiGLU/GEGLU; MoE remains outside initial G2 | **DEFER** | A gated FFN uses an additional projection; compare at matched parameter/compute budgets. MoE adds expert weights, routing, load imbalance and memory residency complexity. See document 39 | Compare parameter-matched and compute-matched dense variants; measure active/total params, routing stability (if later admitted), memory and sustained device use |

| **Normalization** | Tokenizer-neutral | RMSNorm reports task-specific runtime reductions vs LayerNorm; Pre-LN/Post-LN and Peri-LN papers study training stability/gradient propagation, but no Argus-scale Android result selects a winner | LayerNorm vs RMSNorm as operator choices; GroupNorm only where architecture contract calls for it; compare placement separately | **DEFER** | Norm algorithm and placement are separate dimensions. See document 39 for RMSNorm, GroupNorm, Pre-LN/Post-LN and Peri-LN evidence/limits | Layerwise activation/gradient statistics, finite differences, multi-seed convergence, warmup sensitivity, native op latency and allocation cost |

| **Residual topology** | Tokenizer-neutral | Pre-LN/Post-LN theory/experiments and 2025 Peri-LN study provide training-dynamics evidence at non-Argus scales; no Android from-scratch comparison exists | Pre-LN, Post-LN, Peri-LN; standard residual control; architecture-native residual/state updates | **DEFER** — standard residual may be used as a comparison control, not as an accepted Argus choice | Change norm placement independently from norm algorithm, residual scaling and initialization; do not infer small-model/mobile superiority from studies up to 3.2B. See document 39 | Isolated placement ablations; initialization gradient/activation variance, multi-seed stability, loss curves and exact residual/norm numerical parity after implementation authorization |

| **Depth / width / parameter allocation** | Indirect: embedding/head parameters scale with vocabulary, sequence cost depends on tokenizer | Mobile papers show device-specific memory limits but not from-scratch scaling laws for Argus | Width/depth sweeps; parameter allocation across blocks; per-device profiles | **DEFER** | Changes compute, memory, representation and training dynamics together | Separate parameter-matched, token-matched and compute-matched sweeps; report peak memory and sustained throughput |
| **Token embeddings / output head** | **Directly sensitive** to vocabulary size, token IDs and special-token inventory | Existing mobile fine-tuning papers use fixed pretrained tokenizer/model pairs | Tied vs untied embeddings; output projection; embedding dimension and initialization | **DEFER** (G1 dependency) | Vocabulary changes parameter count, logit cost, optimizer state and checkpoint shape; changing token IDs invalidates model semantics | Wait for tokenizer shortlist; measure V×d state/head cost, logits throughput, tying semantics and migration |
| **Tokenizer profile and preparation mode** | Directly defines token IDs, vocabulary/head size, token-to-byte coverage, sequence length and model-generation identity | Published tokenizer studies provide algorithm/language evidence; no Argus per-device tokenizer-training result exists yet | Precomputed profile portfolio; capability-bounded local tokenizer training; algorithmic byte baseline | **DEFER** (G1-D dependency) | Device capability changes feasible preparation and vocabulary budgets, but product language scope and source rights must not be inferred from hardware. Local training adds CPU, memory, storage and first-launch cost | Compare profile candidates across analytical device envelopes; audit seed rights, training/runtime/API, deterministic output and fallback; bind exact fingerprint before model tensor allocation |
| **Context length / sequence packing** | **Directly sensitive** to tokens per byte and segmentation behavior | Mobile training studies use fixed token sequence lengths; they do not define Argus effective context | Context-length profiles; example packing; document boundary masks; curriculum by length | **DEFER** (G1 dependency) | Same token length can represent different byte/document coverage across tokenizers; packing changes attention compute and data semantics | Freeze tokenizer; compare byte coverage, boundary correctness, packing efficiency, memory and long-sequence quality |
| **Genesis initialization and identity contract** | Tokenizer-neutral at the contract level; tensor shapes depend on selected model/tokenizer | No external mobile paper supplies Argus's independent instance/generation identity or atomic initialization record | Argus-owned instance seed policy, initialization metadata, tensor inventory and generation commit | **CUSTOM** | Required for independent local origin, reproducibility and safe creation; must not be confused with choosing a particular initializer formula | Independent production seeds; deterministic test fixtures; manifest/tensor round-trip; crash-safe initial generation transaction |
| **Initializer mathematics and scale** | Mostly tokenizer-neutral; embedding/output scales depend on vocabulary/head | T-Fixup, DeepNorm, effective-theory initialization and μP provide distinct depth/width/stability research paths; their tested Transformer scales/tasks do not define an Argus universal initializer | Fan-aware reference; residual/depth-scaled projections; T-Fixup/DeepNorm composite recipes; μP/μTransfer for width-profile experiments; architecture-native SSM/recurrent state initialization | **DEFER** | Initialization is coupled to graph, residual topology, normalization, width, parameter groups and optimizer. Do not transfer encoder–decoder formulas or one global standard deviation into unrelated families. See [40 — Initialization and Width-Scaling Research](40_INITIALIZATION_AND_WIDTH_SCALING_RESEARCH.md) | Deterministic manifest/seed reproduction; per-parameter-group distribution checks; layerwise forward/backward variance; finite-difference/finite-value tests; multi-seed stability; width/depth proxy transfer under the same family/tokenizer/data contract |
| **Optimizer algorithm and update semantics** | Indirect through sequence/token counts and model parameter inventory | MobileFineTuner measures update paths for pretrained fine-tuning; ICLR 2025 compares Adam, Lion, Adafactor and Sophia for autoregressive LMs; 2025 budget pretraining compares AdamW, Lion and Sophia on smaller proxies | AdamW reference/control; Lion one-momentum sign update; Adafactor factorized-state comparator; Sophia optional; SGD diagnostic; derivative-free methods remain separate | **DEFER** | No optimizer is selected for Genesis. Lion halves the FP32 moment-buffer term but not total training RAM; its sign update, batch sensitivity, LR/decay tuning and precision policy may affect convergence | See [37 — Optimizer Research and Lion Assessment](37_OPTIMIZER_RESEARCH_AND_LION_ASSESSMENT.md): matched-token and effective-batch comparisons; reference parity; finite-value/gradient checks; optimizer-state round trip; update idempotency; memory, energy and sustained-device evidence |
| **Optimizer primitive reuse boundary** | Tokenizer-neutral mathematical interface; state shape follows trainable parameter inventory | MobileFineTuner provides a native training-step reference, but its Android wrapper is not a full Argus optimizer/checkpoint service | Audited native tensor/autograd/optimizer primitives behind an Argus-owned adapter | **MODIFY** | Reuse can avoid reimplementing proven mathematics, but upstream behavior, licenses, precision and serialization must be adapted to Argus contracts | Exact source/API/license audit; state serialization; numerical parity; thread-safety; cancellation and error-path review |
| **Training precision and dtype policy** | Vocabulary-sensitive through embedding/output tensor volume; operator coverage is device-specific | Mobile training literature reports mixed precision/memory techniques for selected workloads, not a universal precision winner for random-init Argus models | FP32 reference; supported FP16/BF16 paths; mixed-precision master weights/scaler where justified; lower-precision inference as a separate contract | **DEFER** | Lower precision can reduce memory/bandwidth but may alter convergence, gradient stability, operator support and fallback behavior | Per-operator dtype coverage; reference tolerance; finite updates; loss/gradient parity; peak memory and sustained device tests |
| **Activation/memory optimization** | Indirect through sequence length | MobileFineTuner evaluates memory-efficient attention, activation checkpointing, accumulation and sharding; MELON provides DNN memory techniques | Activation checkpoint/recompute; microbatching; gradient accumulation; parameter paging/sharding | **ADOPT** as optimization candidates, not unconditional defaults | Reduces peak memory at additional compute/I/O/complexity cost; accumulation must preserve declared update semantics | Per-device A/B under same update semantics; peak RSS/PSS; extra compute; I/O; thermal and energy impact |
| **Tensor layout / mobile GPU kernels** | Indirect through dimensions and sequence sizes | FBLayout reports R-Tile layout and 2.2×–5.7× gains in its evaluated fine-tuning setup | Canonical tensor contract plus backend-specific physical layouts; fused/tiling variants | **MODIFY**; backend layout implementation may become **CUSTOM** only if needed | Reduces data movement/cache misses; risks vendor-specific graph assumptions and conversion overhead | Compare reference and optimized outputs/gradients; measure layout conversions, cache/memory and sustained device performance |
| **CPU correctness backend** | Indirect through tensor shapes, dtype and vocabulary/head | Android device training evidence does not remove the need for a deterministic portable reference path | Capability-gated CPU tensor/autograd implementation for the supported model subset | **ADOPT** | Provides a correctness oracle and fallback independent of unverified accelerators; CPU support still has explicit model/resource limits | Forward/loss/backward/optimizer parity; finite differences; deterministic test cases; bounded allocation and cancellation |
| **GPU backend adapters** | Indirect through tensor shapes, dtype and vocabulary/head | MobileFineTuner/FBLayout show selected GPU fine-tuning paths on specific devices; no universal vendor coverage is established | Backend-specific compute/driver adapters with explicit operator, precision and layout capabilities | **MODIFY** | Can accelerate validated operations but must expose fallback/transfer/layout costs and never imply full-backward support from inference | Per-operator forward/backward parity; precision checks; actual backend instrumentation; transfer/layout overhead; thermal and fallback behavior |
| **NPU training backend** | Indirect through tensor shapes, dtype and vocabulary/head | Reviewed NPU evidence is provider/runtime/operator-specific; inference availability is not training/backward evidence | NPU only for exact verified forward/loss/backward/optimizer coverage | **DEFER** | Premature routing risks unsupported backward operators, silent CPU fallback and false acceleration claims | Full train-step operator coverage; numerical parity; optimizer state; provider/driver stability; process/thermal/resource behavior |
| **Training scheduler / thermal governor** | Tokenizer-neutral at policy level; workload duration depends on token cost | MobileFineTuner energy-aware scheduling; iPhone sustained study; Thermo-FL thermal control on non-phone testbeds | Bounded scheduling, pause/resume, thermal-status governor, battery/charging policies | **MODIFY** for Android lifecycle and Argus owner policy | Improves safety and continuity; aggressive pause/resume can reduce progress or hide unstable sustained throughput | Sustained runs, thermal transitions, Android process reclaim, user cancellation, energy per update |
| **Checkpoint / recovery** | **Sensitive** to tokenizer and dataset fingerprints | Mobile SDK and Android prototypes provide step/checkpoint examples at differing maturity; complete Argus generation transactions are not established by papers | Versioned model+optimizer+RNG+cursor transaction; candidate generations | **CUSTOM** Argus P0 transaction and generation identity | Needed for independent evolution, exact resume and rollback; integration complexity is non-negotiable | Fault injection at update/checkpoint/cursor boundaries; no duplication/skipping of committed updates |
| **Personal adapter learning** | Tokenizer-bound to base model | Online-SDFT and MobileFineTuner demonstrate local LoRA fine-tuning of pretrained bases | LoRA/adapters and replay/feedback pipelines | **DEFER** to personal-learning path; not a Genesis foundation substitute | Plausible later personalization path; requires consent and general-capability regression gates | Separate personal candidate lifecycle, consent revocation, adapter persistence and general evaluation |
| **Federated / collaborative / server-assisted training** | Not a tokenizer decision | Gboard, Confidant, MobiLLM and XPerT use federated, peer or cloud-assisted system topologies | Federation; cross-device pipeline parallelism; server-side backward/model cache | **REJECT** for the current Argus core | Conflicts with one-origin independent evolution and local training computation | No integration planned unless product boundary is explicitly reconsidered by ADR |

### 4.1 How to interpret preliminary dispositions

- **ADOPT** does not mean copy a paper's implementation or assume its measured gain transfers to Argus. It accepts a narrow reference/control or optimization technique for evaluation.
- **MODIFY** is the expected disposition for much of the mobile runtime work: retain the principle, adapt its interfaces and semantics to Argus.
- **CUSTOM** is reserved for product-critical contracts such as Genesis identity, local instance isolation and atomic candidate/checkpoint lifecycle.
- **DEFER** is a deliberate decision not to freeze a choice before the required tokenizer, architecture or device evidence exists.
- **REJECT** applies to incompatible topology assumptions, not to the scientific value of the publication.

### 4.2 G2 family-level shortlist — closed for comparison scope only

G2 selects the families that deserve controlled design-level comparison. It does **not** select the Argus architecture, layer ratio, dimensions, optimizer or device support. The current shortlist is deliberately bounded to four families:

| Family | G2 role | Evidence basis | Main analytical trade-off | Evidence limitation / required next audit |
|---|---|---|---|---|
| **Decoder-only Transformer** | Mandatory reference/control; retain as a serious candidate | MobileLLM shows sub-billion deep/thin architecture, SwiGLU, embedding sharing and GQA benefits. Its models were trained from scratch, but the paper's training setup used 32 NVIDIA A100 GPUs and 0.25T exploratory / 1T reported training tokens—not phones. MobileFineTuner and FBLayout add selected physical-phone pretrained fine-tuning/layout evidence. | Parallel sequence training and mature model ecosystem; dense attention retains quadratic sequence compute and context-proportional KV cache, although memory-efficient kernels reduce intermediate memory traffic. | MobileLLM gives useful small-model allocation hypotheses (depth/width, tied embeddings and GQA), not an Android training result. Audit a complete local-initialization, backward, optimizer and checkpoint path, plus actual Android CPU/GPU operator coverage. |
| **Selective state-space model (Mamba / Mamba-2 SSD)** | Retain as a distinct sequence-model candidate | Mamba provides input-dependent state selection and scan-style training; Mamba-2 SSD formalizes a related attention/SSM structure. Dao–Gu's 2–8× result is an SSD-kernel comparison against Mamba fused scan at state expansion N=64 on A100 80GB PCIe; SSD is faster than FlashAttention-2 only at sequence lengths ≥2K in that tested setup. It is not a whole-model or Android speedup. | Recurrent inference state does not grow with prior context length, while scan/chunk training can parallelize sequence work; state/workspace still scales with model dimensions and execution schedule. Mamba-2's streaming state has two mandatory parts per layer: causal-convolution history [B, E+2GN, K] and SSM state [B, H, P, N]. Document/chunk boundaries must govern both. | NVIDIA's controlled evidence uses 125M–2.7B Pile models and a separate 8B/1.1T–3.5T-token multi-GPU comparison; the latter uses 70% English, 15% non-English, 15% code and a 256K SentencePiece vocabulary, which does not establish Indonesian/SEA coverage. Specialized selective-scan/backward kernels and Android provider support remain unverified. Reset/carry, chunk gradient and dual-state checkpoint requirements are detailed in [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md). |
| **Recurrent delta-rule model (RWKV-7 family)** | Retain as a separate recurrent candidate; do not hide it inside generic SSM | COLM 2025 paper defines RWKV-7's generalized delta-rule state evolution and parallelizable training. It reports 0.19B–2.9B released models upgraded from pre-existing RWKV-5/RWKV-6 checkpoints; this is not clean random-init RWKV-7 evidence. | No attention KV cache; the matrix-valued state is bounded with respect to history length but depends on layer/head/key/value dimensions (the paper illustrates 64×64 per head). Complete continuation also requires TimeMix and ChannelMix token-shift state. The paper's backward kernel stores WKV state every 16 timesteps (head size 64), and its H100 model reports 18 BF16 variable-equivalents (24 FP32) vs 10 for FlashAttention v3; bounded inference state does not mean low training memory. | Large external-compute schedules (BF16 training on 8× NVIDIA H800 GPUs per node, 4,096-token context and large phased batches; separate H100 kernel tests) do not establish Android feasibility. Audit random-init training stability, state-update precision, memory traffic, backward kernels, complete WKV+shift reset/checkpoint semantics and mobile runtime maturity. See [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md). |
| **Attention–SSM/recurrent hybrid** | Retain as a secondary candidate for a controlled composition study | Mamba-2/attention hybrid work reports task-dependent results in an 8B server-scale comparison; the reviewed study found advantages on some aggregate tasks, not universal superiority. | Can combine attention's direct token interaction with bounded recurrent state in selected blocks; retains both operator/state families and some context-proportional KV memory. | Additional graph boundaries, layout conversions, checkpoint state and backend fallback complexity. No direct Argus Android result or justified per-device layer ratio exists. |

The shortlist deliberately does not advance mixture-of-experts, multimodal fusion or encoder-decoder models into the initial G2 set. They may be reconsidered only if a documented product requirement or materially stronger evidence changes the scope. Their omission is a bounded first-model research choice, not a claim that those methods are scientifically invalid.

**Common admission rule:** every retained family must expose a complete random-initialization path and a mathematically correct forward/loss/backward/update definition, support a CPU correctness reference, and provide an auditable model/optimizer/RNG/cursor checkpoint contract before it can proceed to G4. A paper's inference or fine-tuning result cannot satisfy these requirements by itself. Candidate comparisons must pin the same tokenizer profile/fingerprint once G1-D is approved; before then, token-neutral operator proxies may proceed, but no token-sensitive winner may be frozen.

**G2 disposition:** **CLOSED — FAMILY SHORTLIST ESTABLISHED FOR DESIGN COMPARISON ONLY.** The four families above are the complete initial comparison set. No family is selected for product implementation. G3 dispositions are recorded separately under ADR-026; G4 architecture design freeze remains open. The cross-family trade-off synthesis, matched-comparison rules and G4 output requirements are maintained in [38 — G2 Architecture Comparative Synthesis](38_G2_ARCHITECTURE_COMPARATIVE_SYNTHESIS.md).

## 5. Tokenizer dependency map

Architecture research may continue before G1, but comparisons that depend on token IDs, vocabulary size or token-to-byte coverage must not be frozen.

| Decision / experiment | Tokenizer dependency | Can proceed before G1? | Required output |
|---|---|---|---|
| Backbone family high-level literature survey | Indirect | Yes | Evidence scope and candidate families; no final ranking |
| Attention algorithm/kernel correctness on synthetic tensor shapes | No fixed vocabulary required | Yes | Reference operator contracts and correctness tests |
| Mobile GPU layout/data movement study | Tensor shape matters; actual shapes later depend on tokenizer/model | Partially | Backend/layout candidate shortlist, not final performance claim |
| FFN/norm/residual reference comparisons | No direct vocabulary dependency at fixed shapes | Yes, as small controlled proxies | Isolated numerical/compute behavior; no product architecture freeze |
| Parameter/depth/width device budget | Vocabulary/head contribution not yet fixed | Partially | Non-vocabulary lower/upper cost bounds with explicit excluded embedding/head term |
| Vocabulary size and tokenizer preparation mode | Direct; budget and local-training feasibility depend on the selected device/resource envelope | No final selection | Profile-specific tokenizer comparison, rights-cleared seed policy, and vocabulary/head/optimizer cost |
| Device-profile tokenizer portfolio | Depends on product language scope, rights-cleared data and measured capability; no one vocabulary is universal | No final selection | A bounded profile registry and deterministic selection/fallback policy with exact fingerprint per generation |
| Embedding dimension and embedding matrix size | Direct through vocabulary and model width | No final selection | Vocabulary-aware parameter/memory budget |
| Tied vs untied embeddings/output head | Direct cost interaction with vocabulary; semantics need fixed token IDs | No final selection | Parameter/head compute comparison after tokenizer shortlist |
| Special-token IDs and chat control inventory | Direct | No | Final serializer/tokenizer contract |
| Effective context length and document coverage | Direct through tokens per byte and segmentation | No | Byte/document coverage per context window |
| Final sequence packing and boundary masking | Direct through tokenized examples | No | Packing, EOS/document-boundary and loss-mask contract |
| Final language-model ablation | Direct | No | Freeze the exact tokenizer profile ID/fingerprint and tokenized fixture for the model configuration being compared |

A tokenizer shortlist is sufficient to prepare the architecture matrix and estimate parameter/head costs, but G4 must approve the device-conditioned tokenizer profile portfolio, selection/fallback policy and exact contract/fingerprint for each retained profile. Each Genesis generation pins exactly one selected tokenizer manifest and fingerprint; no installation changes token IDs in place. A vocabulary revision after architecture freeze requires a new generation contract and migration review.

## 6. Existing framework reuse strategy

### 6.1 Minimal integration principle

Argus should not rewrite an established training loop merely to own every line of implementation. It should first evaluate whether an existing native framework can supply a mathematically correct operator/model/training-step core behind a thin adapter.

The reuse boundary should be:

```text
Argus Genesis / Session / Policy / Data
                |
                v
       Argus Training Adapter
                |
        Stable Tensor / Model /
       Optimizer / Checkpoint API
                |
        +-------+--------+
        |                |
        v                v
 Existing Native Core   Argus Custom
 (if audited/reusable)  Contracts
                        - Genesis identity
                        - local instance state
                        - candidate generation
                        - atomic recovery
                        - user/data policy
```

The diagram is a design boundary, not an existing implementation.

### 6.2 MobileFineTuner integration audit

Before selecting it as the reference native core, evaluate these items separately:

1. **License and redistribution:** the upstream repository root currently contains an Apache License 2.0 `LICENSE` file, which is a positive code-reuse signal subject to its notice/redistribution conditions. This does **not** independently clear every transitive dependency, separately licensed file, model/tokenizer asset, dataset or prebuilt AAR. Pin the exact upstream commit, inspect the complete dependency graph and NOTICE/attribution files, and establish the provenance/license of every shipped binary and asset before selecting or redistributing it.
2. **Android API maturity:** current AAR/ABI/API surface, lifecycle and error behavior. The published Android SDK documentation identifies arm64-v8a packaging and a Java `trainStep` entry point for LoRA, with a synthetic one-step device smoke test.
3. **Training-loop completeness:** determine which dataset iteration, batching, sampling, pause/cancel, scheduling and checkpoint capabilities exist in the native core versus the Android wrapper. Do not assume desktop features are exposed to Android.
4. **From-scratch support:** verify whether the model can be instantiated with locally initialized parameters without loading a pretrained state. The paper's reported experiments are fine-tuning experiments.
5. **Backward coverage:** inspect each selected architecture operator, gradient shape, numerical precision and reference parity.
6. **Optimizer semantics:** inspect state allocation, gradient accumulation, clipping, update count, learning-rate scheduling and optimizer-state persistence.
7. **Model/tokenizer contract:** inspect supported model configs, vocabulary/tokenizer loading, special-token and sequence-label semantics.
8. **Checkpoint/recovery:** establish whether the core exposes a full model+optimizer+RNG+cursor state or only parameter/adaptor serialization. Argus P0 transaction remains authoritative.
9. **Device behavior:** physical Android tests for memory, thermal, energy, cancellation, process death, storage pressure and actual CPU/GPU execution.
10. **Maintenance and portability:** build reproducibility, NDK/ABI scope, architecture support and upstream maintenance cadence.

**Preliminary classification:** **MODIFY / REFERENCE IMPLEMENTATION CANDIDATE**, not “ADOPT AS COMPLETE ARGUS TRAINING ENGINE.” The repository-level Apache-2.0 license supports further code-level evaluation, but does not close the binary/dependency/assets review or establish compatibility with Argus's selected architecture, tokenizer, from-scratch initialization, full-backward and checkpoint contracts.

### 6.3 FBLayout integration boundary

FBLayout is strong **published research evidence** for mobile-GPU physical tensor-layout optimization, not a currently cleared code dependency. The paper metadata identifies a CC BY-NC-ND 4.0 license; an implementation/source-code repository and separate software license were not established in this review. Do not copy, adapt, redistribute or vendor paper/code artifacts into Argus until the exact rights and intended use are reviewed. The underlying performance problem and independently derived design requirements may still inform Argus's own research. It must not be treated as a replacement for the model graph, optimizer, dataset loop or checkpoint system. A future independently licensed implementation or a separate permission grant would need an explicit integration review. A safe abstract integration boundary would preserve:
- A canonical logical tensor contract independent of physical backend layout.
- Explicit and measurable conversion/layout transformation boundaries.
- Exact numerical parity between reference and optimized forward/backward outputs.
- Backend capability checks and fallback.
- Device/runtime-specific tuning records.

Do not copy a GPU-specific layout into CPU tensor semantics or force it onto an unvalidated NPU backend.

### 6.4 Existing framework disposition

| Component | Direction | Reason |
|---|---|---|
| Model graph / architecture | Research candidate | No architecture selected |
| Native tensor/autograd/training core | Reuse or adapt after source/API/accuracy audit | Avoid reimplementing proven mathematical primitives |
| Android wrapper | Thin Argus adapter | Keep UI, lifecycle, policy and session identity outside native math |
| Dataset ingestion/rights filter | Argus-owned | Must preserve general/personal data governance and source provenance |
| Genesis identity and initialization record | Argus-owned | Product-specific independent model origin |
| Candidate generation + promotion | Argus-owned | P0 active-parent immutability and rollback |
| Complete checkpoint transaction | Argus-owned orchestration; core serialization may be reused | Model/optimizer/cursor/idempotency must commit together |
| Mobile GPU physical tensor layout | Reuse/adapt if compatible | Device-specific backend optimization, not global tensor semantics |
| Thermal/background scheduler | Argus-owned orchestration; techniques may be reused | Android lifecycle, user policy and observable progress |
| Federated/cloud/peer collaboration | Exclude | Conflicts with accepted local-instance boundary |

## 7. Benchmark and comparison controls

A valid component comparison changes one primary factor at a time and keeps the training semantics fixed.

### 7.1 Common benchmark manifest

Every benchmark record binds:
- Exact code commit and dependency/runtime versions.
- Device SKU, SoC, RAM/storage, Android build, ABI and drivers.
- Actual backend and operator coverage; never infer accelerator use from device name.
- Tokenizer ID/fingerprint after G1, or the declared synthetic shape fixture before G1.
- Dataset/source revision, split, bytes, token count, language/domain composition and preprocessing.
- Model component configuration, parameter count, initialization seed and precision.
- Optimizer/update semantics, batch/microbatch, accumulation and clipping.
- Sequence length/packing, target-token or target-byte count, and measured FLOPs/compute proxy where reliable.
- Warm-up, cold-start and sustained measurement intervals, thermal state, battery/charging and ambient conditions where available.
- Peak/steady memory, energy method, duration, pauses, errors and raw measurement location.

### 7.2 Required comparison modes

Use separate comparison modes; do not blend their conclusions:

1. **Parameter-matched:** compare capability/cost at approximately equal total parameters. State whether embedding/output parameters are included.
2. **Compute-matched:** compare under a fixed compute budget; token exposure may differ and must be reported.
3. **Memory-matched:** compare under the same device/process memory ceiling and reserve.
4. **Data-exposure-matched:** for tokenizer comparisons, use the same documents and target bytes while reporting each candidate's token count; for architecture comparisons, hold the exact tokenizer profile/fingerprint fixed.
5. **Device-matched:** same physical device, OS, thermal start state, power/charging and runtime build.
6. **Sustained-matched:** compare after the same plateau protocol, not one cold-start run.

When tokenizer changes, a fixed token count does not represent equal raw-text exposure. Report bytes and tokens. A tokenizer comparison must hold source documents/target bytes constant; an architecture comparison must hold the exact tokenizer profile/fingerprint constant. Do not claim a tokenizer or architecture win from a mismatched data budget.

### 7.3 Core metrics

- Training loss and held-out loss; learning progress per unit compute and per admitted data byte.
- Tokens/s, bytes/s, optimizer updates/s, wall time/update and time-to-validation target.
- Peak and steady process/native/accelerator memory, allocation churn and storage I/O.
- Energy/update and energy/byte or energy/token with method/uncertainty.
- Thermal status over time, time-to-plateau, throughput degradation and pause frequency.
- Numerical correctness: forward/loss/gradient parity, finite differences, finite state and deterministic replay where supported.
- Operational correctness: pause/cancel latency, checkpoint duration, resume recovery, fallback and user-visible state.

Do not reduce all metrics to a weighted one-number score before the required support/safety gates pass. Present a Pareto frontier and explain the trade-offs.

## 8. Decision gates and current status

These gates separate documentary design decisions from later physical verification. G0 is closed as a research-scope baseline only; the remaining gates retain their independent acceptance criteria and are not implied to be complete.

| Gate | Purpose | Required evidence | Permitted outcome | Current state |
|---|---|---|---|---|
| **G0 — Scope and constraints** | Define what is compared and what cannot change | Genesis constraints in section 2; explicitly open device targets; component inventory; benchmark/manifest schema; source/license audit scope; evidence categories and matched-comparison controls | Approve the research scope and component inventory only | **CLOSED — DOCUMENTARY SCOPE BASELINE** — Accepted by ADR-024 in document 01. Device classes and numerical thresholds remain intentionally open. No architecture/tokenizer selection, corpus activation, implementation, build, workflow or device-support claim is authorized by G0 |
| **G1-D — Documentary tokenizer shortlist** | Establish the representation shortlist and device-conditioned preparation policy for token-sensitive design decisions | Published tokenizer comparisons; explicit product-language scope; source/license review for tokenizer-frequency and (if base PA-BPE is retained) aligned development data; analytical vocabulary/head costs across device/resource envelopes; profile portfolio, local-training budget/fallback policy, exact pipeline and fingerprint contract | Approve a conditional tokenizer profile portfolio and design constraints; this is not neural-training corpus approval or Android implementation support | **PARTIALLY COMPLETE — DOCUMENTARY COMPARISON BASELINE PRESENT** — Candidate families, profile portfolio, analytical cost method and preparation modes are documented in document 34. The pinned TALPCo snapshot contains 1,372 shared EN/ID/JA sentence IDs and is a small aligned-development candidate only; its CC BY 4.0 declaration and use-specific artifact rights still require review. Exact CC BY Open Textbook Library titles and ITATS CC BY 4.0 OER items are frequency-corpus leads, not cleared sources; exact files, third-party components, attribution and tokenizer/artifact use remain pending. The product-language matrix remains unaccepted: document 34 proposes Indonesian+English as a core baseline with regional extension candidates, but ADR-022 establishes only Indonesian source diversity, not product-language priority. Runtime research proposes HF Tokenizers Rust behind an Argus C ABI/JNI for HF-compatible byte BPE (Rust Android targets are documented, but the crate is unbuilt); stable SentencePiece C++ v0.2.2 behind JNI for SentencePiece artifacts; experimental Lite remains held. A shared API/ABI contract and staged deterministic profile-selection procedure are proposed, not selected or verified. Physical-device tests are not G1-D prerequisites; exact conformance remains G1-V |
| **G2 — Architecture shortlist** | Narrow the candidate components/families | Prior-art evidence; tokenizer-neutral analysis; analytical resource bounds; numerical/runtime risk | Select candidates for design-level comparison, not product support | **CLOSED — FAMILY SHORTLIST ONLY** — Accepted by ADR-025 in document 01: decoder-only Transformer (mandatory reference), selective SSM/Mamba-2 SSD, recurrent delta-rule/RWKV-7, and attention–SSM/recurrent hybrid are retained for controlled design comparison. This does not select the Argus architecture or establish Android random-init feasibility; G3 is recorded separately and G4 remains open |
| **G3 — Component disposition** | Assign one primary ADOPT/MODIFY/CUSTOM/DEFER/REJECT disposition per component | Evidence scope/strength, expected benefit, cost/risk, implementation assumptions, validation need and owner | Accept the bounded planning disposition baseline; architecture-specific values and implementation support remain later decisions | **CLOSED — DISPOSITION BASELINE ONLY** — Accepted by ADR-026 in document 01. Tokenizer-sensitive and architecture-dependent choices remain DEFER where marked; no implementation, code reuse, build or device-support claim is authorized |
| **G4 — Pre-implementation architecture design freeze** | Freeze a versioned design contract for implementation planning | G1-D complete; G3 baseline reviewed; controlled comparisons from existing literature/reference implementations plus analytical side-by-side estimates; explicit model/tokenizer profile portfolio and deterministic device-conditioned selection policy; per-profile tokenizer fingerprint and resource envelope; unresolved risks and post-implementation verification plan; ADR in document 01 | Freeze the design contract and migration boundaries, explicitly marked **not yet physically verified** | **BLOCKED — G1-D product-language priority and tokenizer source-rights remain unresolved; runtime/API candidates are documented in document 34 but a per-profile runtime contract is not selected; final architecture comparisons and ADR are not complete** |

G0, G2 and G3 are closed for their limited planning outputs: research scope/evidence controls, the four-family comparison shortlist, and one primary disposition for each component. G3 closure is a planning baseline only; it does not force deferred choices or prove implementation support. G2 closure is not architecture selection and cannot freeze token-sensitive decisions before G1-D. G1-D narrows the tokenizer families and defines the profile-level comparison method from existing evidence. Final vocabulary budgets, tokenizer preparation modes, embedding/output-head dimensions, tied-weight, token-context and packing decisions are resolved per retained configuration profile together with the architecture/resource envelope at G4; Argus does not require one global tokenizer vocabulary for every installation. G1-V exact reference/Android conformance and downstream model-quality tests occur after implementation and do not block documentary shortlist closure. G4 comparisons at this stage mean comparisons of existing published/reference evidence and analytical estimates; they do not require Argus app/device experiments. G4 is a **design freeze only**; it does not claim that a random-initialized model trains successfully or that any target Android device is supported.

After implementation is separately authorized, the numerical, lifecycle, memory, thermal, energy, checkpoint and physical-device checks in document 33's E1–E7 protocol become mandatory verification gates. They must pass before Argus claims broad from-scratch feasibility or device support. They are not preconditions for approving a design to implement.

No gate authorizes implementation, builds or device experiments by itself. The repository's documentation/build freeze and the user's explicit authorization remain in force.

## 9. Immediate work sequence

1. **G0 CLOSED:** preserve the accepted research scope, component inventory, comparison methodology and evidence-confidence rules under ADR-024. Do not reopen G0 merely because G1/G2/G3 decisions remain unresolved.
2. **Parallel evidence extraction:** fill the Android evidence table and component matrix. Audit the complete MobileFineTuner paper/repository, FBLayout paper/review, relevant mobile kernel/runtime sources and any new Android papers. Mark inaccessible or incomplete papers as pending rather than inferring results from a title.
3. **Close documentary tokenizer G1-D:** obtain the product-owner-approved language priority and unsupported-language policy; approve an exact, immutable, language-labelled tokenizer-frequency manifest with source-specific training/derived-artifact permissions; if base PA-BPE is retained, approve a separate aligned-development manifest (TALPCo is a small candidate, not automatically cleared); choose a runtime/API/ABI/format contract for each retained tokenizer family; and complete the analytical profile/vocabulary sweep and deterministic fallback contract. Do not wait for Argus app/device tests or downstream neural training. Keep Android runtime conformance and downstream quality in G1-V for later authorized implementation.
4. **Prepare tokenizer-neutral architecture references:** define logical operator contracts and candidate ablations for attention, FFN, normalization, residual path, initialization and runtime layout. Any prototype implementation requires a separately approved engineering phase.
5. **G2 CLOSED:** retain the four-family comparison set in section 4.2 under ADR-025. Do not reopen the family list without new evidence or a documented product-scope change.
6. **G3 CLOSED:** preserve the one-primary-disposition baseline in section 4. Do not convert DEFER into ADOPT/MODIFY/CUSTOM without the evidence and ADR required by G4.
7. **G4 design memo:** produce the versioned architecture ADR only after G1/G3 and controlled design evidence pass. Record E1–E7 and physical-device tests as post-implementation verification obligations, not completed evidence.

### 9.1 Current stop conditions

Do not:
- Copy an implementation before license and API audit.
- Assume MobileFineTuner's paper capability is identical to its current Android SDK wrapper.
- Attribute FBLayout's speedup to a backbone/attention architecture choice; it is a tensor-layout/runtime result.
- select RoPE, RMSNorm, SwiGLU, GQA, MoE, tied embeddings or a context length from popularity alone.
- run whole-model comparisons with different tokenizers and infer architecture superiority.
- integrate server-assisted/federated/collaborative training into the current Argus baseline.
- initiate builds, CI workflows, device benchmarks or implementation without explicit authorization.

## 10. Required deliverables

| Deliverable | Artifact | Status |
|---|---|---|
| Android prior-art evidence table | Section 3 + evidence register in document 33 | **BASELINE PRESENT; selected source/API audits recorded; remaining source and license reviews remain open** |
| Genesis constraints | Section 2 | **DOCUMENTED** |
| Architecture component comparison matrix | Section 4 plus documents 38–41 | **DETAILED RESEARCH BASELINE PRESENT; no component, initialization, optimizer or backbone winner selected. Document 39 covers Transformer internals, document 40 initialization/width scaling, and document 41 recurrent state/chunk lifecycle. Architecture-family-specific full graph/operator/precision contracts and matched G4 synthesis remain open.** |
| Tokenizer comparison and device-conditioned profile policy | Document 34 | **CANDIDATE COMPARISON, STAGED PROFILE-SELECTION PROPOSAL AND STATIC FORMAT-MATCHED RUNTIME/API/ABI CONTRACT PRESENT; G1-D OPEN** — product owner must accept product-language/unsupported-language policy; exact frequency and (if base PA-BPE retained) aligned-development manifests must be approved for their distinct operations; exact file revisions/checksums, third-party exceptions and attribution remain pending; final per-profile runtime/API/ABI/format choices remain unselected. Runtime static audit is present; no Android ABI/runtime conformance is claimed. |
| Adopt/Modify/Custom/Defer/Reject decision log | Section 4 / ADR-026 | **BASELINE ACCEPTED FOR PLANNING; deferred architecture-specific choices remain open** |
| Controlled experiment plan | Section 7 and document 33 | **PROTOCOL PRESENT; ARGUS RESULTS NOT RUN** |
| Pre-implementation architecture design memo | ADR in document 01 using [43 — G4 Architecture Decision and Device-Profile Contract](43_G4_ARCHITECTURE_DECISION_AND_PROFILE_CONTRACT.md) | **G4 BLOCKED BY G1-D AND FINAL FAMILY-SPECIFIC COMPARATIVE SYNTHESIS / ADR**; G3 disposition baseline is closed but all architecture-specific DEFER choices remain unselected. Document 43 defines the required profile manifest, hard gates, matched comparison, portfolio decision and ADR checklist. Physical feasibility remains unverified and must be tested after implementation, not used to block documentary design completion |

## 11. Cross-document authority

Optimizer algorithm candidates, Lion state-memory accounting, update semantics and exact-resume requirements are detailed in [37 — Optimizer Research and Lion Assessment](37_OPTIMIZER_RESEARCH_AND_LION_ASSESSMENT.md). The optimizer disposition remains DEFER; AdamW is a comparison control and Lion is an admitted candidate, not a selected default.

Transformer attention/position/FFN/normalization and embedding contracts are developed in [39 — Transformer Component Research](39_TRANSFORMER_COMPONENT_RESEARCH_AND_DESIGN_CONTRACTS.md); Genesis initialization and width scaling are developed in [40 — Initialization and Width-Scaling Research](40_INITIALIZATION_AND_WIDTH_SCALING_RESEARCH.md); Mamba-2/RWKV-7 recurrent carry, chunk/BPTT, document reset and checkpoint contracts are in [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md). These are research/design references, not accepted component selections or implementation evidence.


- [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md): separates from-scratch, fine-tuning and runtime evidence; device constraints and E1–E7.
- [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md): candidate tokenizer families, device-conditioned profile portfolio, optional local tokenizer preparation, fixture, Unicode/special-token contract, vocabulary and artifact lifecycle.
- [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md): architecture family research and final selection gate.
- [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md): local initialization, first-launch readiness and generation identity.
- [19 — P0 Remediation Design](19_P0_REMEDIATION_DESIGN.md): parent/candidate/checkpoint/promotion transaction and instance/data boundaries.
- [14 — Performance, Power and Thermal Engineering](14_PERFORMANCE_POWER_THERMAL.md): benchmark methodology and sustained metrics.
- [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md): operator, device, lifecycle and verification requirements.
- [16 — Roadmap and Research Register](16_ROADMAP_RESEARCH_REGISTER.md): evidence register and phase ordering.

**G0 status:** CLOSED for research scope/evidence controls. **G2 status:** CLOSED for the four-family comparison shortlist. **G3 status:** CLOSED for the one-primary-disposition planning baseline. **G1-D status:** PARTIALLY COMPLETE, with language-scope and source-rights decisions open. **G4 status:** BLOCKED on G1-D and final comparative synthesis. **Current architecture decision:** OPEN. The prior art justifies reusing and adapting mature mobile training techniques; it does not justify freezing a backbone or internal architecture components. Tokenizer evaluation and component-level architecture research proceed in parallel. G1 remains the mandatory documentary dependency gate for token-sensitive decisions, but its output is a device-conditioned tokenizer profile policy rather than one global vocabulary.
