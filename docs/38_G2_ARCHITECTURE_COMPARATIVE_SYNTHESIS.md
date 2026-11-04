# G2 Architecture Comparative Synthesis

**Status:** Comparative design synthesis; no Argus backbone selected  
**Last reviewed:** 2026-11-04  
**Research gate:** G2 family shortlist established; detailed synthesis feeds G4  
**Scope:** Decoder-only Transformer, selective SSM/Mamba-2 SSD, recurrent delta-rule/RWKV-7, and attention–SSM/recurrent hybrid  
**Boundary:** Published/reference evidence and analytical comparison only. No Argus implementation, build, workflow, or physical-device experiment is authorized or claimed.

## 1. Executive synthesis

The initial four-family shortlist remains appropriate for the first Argus model-design decision. The current evidence does not justify deleting any of the four, nor does it justify selecting one solely from inference complexity or a large server-scale result.

| Family | Strongest current reason to retain | Main unresolved cost/risk for Argus | Current position |
|---|---|---|---|
| Decoder-only Transformer | Mature causal-LM formulation and the strongest directly relevant sub-billion/mobile deployment and mobile fine-tuning ecosystem | Attention compute grows quadratically with sequence length; inference KV cache grows with retained context; Android from-scratch training remains unproven | Mandatory reference/control and serious candidate |
| Selective SSM / Mamba-2 SSD | Structured state-space duality permits parallel scan/chunk training while maintaining bounded recurrent inference state | Specialized selective-scan/backward kernels, numerical parity and Android operator coverage are not established | Serious research candidate |
| Recurrent delta-rule / RWKV-7 | Dynamic matrix-valued recurrent state with parallelizable training and constant history-dependent inference-state footprint | Recurrent-state update/backward kernels, WKV + token-shift state precision, chunk-boundary semantics and Android runtime maturity need independent audit; complete carry/reset/checkpoint contract is documented in [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md) | Serious research candidate |
| Attention–SSM/recurrent hybrid | Can combine direct attention paths with recurrent state; large-scale published Mamba-2 hybrid results are promising on the studied tasks | Carries both operator/state families, more complex checkpointing and fallback; published layer composition is not transferable to Argus | Secondary composition candidate |

The next decision must not be framed as “which architecture is fastest?” It is a multi-objective decision across from-scratch trainability, model quality, training/inference memory, sustained compute, kernel maturity, data semantics, exact recovery, maintainability and the number of device profiles the project can support.

**No G4 selection is made here.** The purpose is to expose the trade-offs and required comparisons before freezing a design.

## 2. Evidence categories and transfer limits

The reviewed evidence falls into different categories that must not be combined as if they measured the same thing:

- **Mobile deployment/model design:** MobileLLM studies sub-billion model design and reports deep/thin blocks, embedding sharing and grouped-query attention. It is relevant to parameter allocation and on-device inference, but does not establish that the training run itself occurred on a phone.
- **Mobile LLM fine-tuning/runtime:** MobileFineTuner and FBLayout show selected Android phone fine-tuning and memory/layout techniques. They do not prove from-random-initialization foundation training or select a backbone family.
- **Server-scale controlled architecture comparison:** Mamba/Mamba-2/Transformer/Hybrid studies compare common data and large token budgets at multi-billion scale. They provide stronger evidence about architectural behavior than isolated deployment claims, but their GPU kernels, batch sizes and training infrastructure are not Android measurements.
- **Architecture-specific papers and released implementations:** Mamba-2 SSD and RWKV-7 define useful mathematical and kernel references. A paper or CUDA implementation is not evidence of Kotlin, NDK, Vulkan, CPU or Android accelerator availability.
- **Argus evidence:** No reviewed source establishes successful broad general-language pretraining from random initialization on a single Argus Android device. That claim remains reserved for post-implementation E1–E7 verification.

This synthesis therefore uses papers to compare mechanisms and trade-offs, not to import their reported throughput or quality gains as Argus estimates.

## 3. Per-family technical comparison

### 3.1 Decoder-only Transformer

**Core mechanism.** Causal self-attention allows each position to attend to preceding positions, followed by feed-forward and residual/normalization blocks. Training parallelizes across token positions within a sequence; autoregressive inference maintains keys and values for prior context.

**Sequence and state implications.**

- Dense attention compute grows approximately quadratically with sequence length \(L\) for a fixed attention width/head layout.
- A naive training implementation may materialize large attention intermediates. Memory-efficient/tiled attention can reduce the stored intermediate footprint, but does not remove the fundamental attention interaction cost.
- Autoregressive inference KV memory grows with retained sequence length, layer count and KV width. Grouped-query attention and cache precision can reduce this term.
- Training activation memory is separate from inference KV memory; checkpointing/recomputation and microbatching target training activations, not the model's parameter/optimizer state.

**Evidence.** MobileLLM provides strong design evidence for sub-billion Transformer models through deep-and-thin configurations, SwiGLU FFNs, embedding sharing and grouped-query attention. It is important to classify the result correctly: the authors explicitly train MobileLLM **from scratch**, but they do so on **32 NVIDIA A100 GPUs**, first exploring 0.25T tokens and then training reported models for 1T tokens—not on a phone. For the 125M/350M variants, the paper finds deeper/thinner layouts outperform shallower/wider variants in its tested settings; embedding sharing reduces a 135M model to 119M parameters (about 11.8%) with a small accuracy trade-off that can be offset by reallocating saved parameters to depth; and 16 query heads with 4 KV heads gives comparable 125M accuracy / about 0.2-point drop at 350M while reducing model size by nearly 10%. These are valuable architecture hypotheses for Argus's small-model budget, not default settings or proof of Android training feasibility. MobileFineTuner/FBLayout add selected physical-phone **fine-tuning** and layout evidence, not from-scratch phone training.

**Argus risks and design questions.**

- Attention kernel availability and backward precision must be proven for every supported backend.
- Vocabulary/head parameters can be a large share of a small model; tokenizer profile and embedding tying therefore strongly affect total model and optimizer state.
- Long context may be expensive even when inference cache is compressed.
- Exact causal mask, sequence packing, document-boundary masking, normalization, positional encoding and output projection contracts must be frozen.

**Required comparison.** Use a correctness-first causal-attention CPU reference; compare tiled/memory-efficient paths only after parity. Include deep/thin, SwiGLU vs other FFNs, tied/shared vs untied embeddings, and MHA/MQA/GQA as separate design variants, not inherited defaults. For a given parameter budget, separately report the trade between shared embedding savings, extra depth, query/KV head counts, KV-cache size, output projection cost and throughput. Compare equal parameter, token and compute budgets separately; MobileLLM's 32×A100 training result is not a mobile runtime or power measurement.

### 3.2 Selective SSM / Mamba-2 SSD

**Core mechanism.** Selective state-space layers update a compact recurrent state from input-dependent parameters. Mamba-2 uses structured state-space duality (SSD) to connect SSM computation with attention-like matrix structure and to expose efficient chunk/scan algorithms.

**Sequence and state implications.**

- The recurrent inference state is bounded by selected layer/state dimensions rather than storing a key/value vector for every previous token.
- The SSD formulation enables parallel/chunk training instead of requiring a simple token-by-token training loop.
- The fact that inference state does not grow with history does not imply zero state, negligible workspace, or lower peak training RAM in every configuration.
- Training still needs activations/intermediates or recomputation across the sequence and correct gradients through the scan/recurrent formulation.

**Evidence.** Dao and Gu's Mamba-2/SSD paper reports a **kernel-level** result: in Figure 10, on an **A100 80GB PCIe**, the SSD implementation is 2–8× faster than the Mamba fused scan at large state expansion (N=64), and faster than FlashAttention-2 for sequence lengths of 2K and above. This is not a whole-model or Android speedup. The same paper explicitly cautions that a full Mamba-2 model may not train as efficiently as a Transformer at short sequence lengths such as 2K because its block/MLP composition changes the hardware work mix. Its autoregressive language-model comparisons span 125M–2.7B models on The Pile; a 350M, 48-layer ablation found a small attention fraction (about 10%) helpful in that particular setup, while a 2.7B/300B-token comparison also tested Mamba-2, MLP and attention combinations. These results are useful evidence that scan, attention and MLP choices interact; they do not set Argus's architecture ratio.

The NVIDIA/collaborator empirical study compares 8B Transformer, Mamba and Mamba-2 models, plus an 8B Mamba-2-Hybrid, on common datasets up to 3.5T tokens. The 1.1T/3.5T corpora are 70% English, 15% non-English and 15% code and use a 256K SentencePiece vocabulary; "non-English" does not establish Indonesian or Southeast Asian parity. Pure Mamba/Mamba-2 results are task- and token-horizon-dependent: at 1.1T tokens the 8B Mamba-2 model is 17 points behind the Transformer on 5-shot MMLU, narrowing to 1.37 points at 3.5T, but pure SSMs still struggle on synthetic copying/Phonebook tasks. At 3.5T the paper only trains the pure Mamba-2 model, not pure Mamba, due to efficiency issues. These are controlled, large-scale GPU results, not Android measurements or mobile feasibility evidence.

The 8B comparison is **approximately parameter-matched, not tensor- or topology-identical**: the paper's table lists an 8.53B Transformer with 32 layers, width 4,096, 32 attention heads and RoPE; an 8.15B Mamba with 56 layers, width 4,096 and state dimension 128; and an 8.24B Mamba-2 with 56 layers, width 4,096, state dimension 128 and eight groups. Mamba/Mamba-2 use no explicit positional embedding in that comparison. These are the paper's selected configurations, not a controlled isolation of one architectural variable.

**Argus risks and design questions.**

- Selective scan, convolution/state projections and backward kernels need a complete CPU reference and a verified Android execution strategy.
- Kernel implementations may rely on specialized CUDA primitives that do not map directly to NDK CPU, Vulkan or vendor NPU runtimes.
- State layout, chunk boundaries, reset behavior and exact recomputation policy must be specified.
- A backend that supports forward inference only is insufficient for Genesis training.

**Required comparison.** Implement/reference the recurrence and scan mathematically, then compare full sequence versus chunked execution for forward, loss and gradients. Measure state/workspace separately from parameter and optimizer state. Verify exact state carry/reset across packed document boundaries and checkpoint/resume boundaries.

### 3.3 Recurrent delta-rule / RWKV-7

**Core mechanism.** RWKV-7 generalizes a delta-rule-style recurrent update with dynamic state evolution and vector-valued gating. It is recurrent during inference but its training formulation remains parallelizable. A continuation state is not only its WKV matrix: the COLM reference loop also carries TimeMix WKV/shift state and ChannelMix shift state. Document 41 defines their reset and recovery contract.

**Sequence and state implications.**

- Inference retains a matrix-valued state per layer/head rather than a conventional Transformer KV entry for every prior token.
- The state footprint depends on the model's recurrent dimensions and layer/head configuration; “constant with context length” does not mean free or independent of model size.
- Training can use parallel/chunk formulations, but those introduce their own intermediate, scan and state-boundary requirements.
- A streaming training implementation must define when state is reset, carried between chunks, or detached. These choices change the objective and must not be hidden inside data-loader behavior.

**Evidence.** The COLM 2025 RWKV-7 paper describes constant inference-state memory and constant recurrent update work **with respect to prior history length**, while retaining parallelizable training through a diagonal-plus-low-rank/generalized-delta recurrence. This does not mean state is independent of model dimensions or negligible: the paper's depicted example uses a 64×64 matrix state per head. The released 0.19B–2.9B World v3 models were **upgraded from pre-existing RWKV-5/RWKV-6 checkpoints**, not clean random-initialization RWKV-7 runs. The paper reports 1.6T–5.6T total training tokens for those released models, BF16 training on nodes with 8× NVIDIA H800 GPUs each, a 4,096-token pretraining context and large phased batches (for example, 240×4,096 at 0.1B and up to 2,016×4,096 for the 2.9B schedule). Its separate kernel comparisons use an H100 GPU; the paper estimates 18 BF16 variable-equivalents for the RWKV-7 training path versus 10 for FlashAttention 3 in its stated setup (24 for the FP32 RWKV-7 path). Therefore bounded recurrent inference state must not be interpreted as automatically lower training activation/workspace memory. These results establish architectural and GPU-kernel evidence, not random-init Android feasibility, mobile memory requirements or an Argus optimizer/training recipe.

**Argus risks and design questions.**

- Matrix-valued recurrence and dynamic state evolution need explicit tensor shapes, dtypes, numerical stability and state serialization. Size state by the actual layer/head/key/value dimensions; do not call it "small" merely because it is history-bounded.
- Published RWKV-7 training-memory estimates include recurrent-state checkpoints and may exceed an optimized attention path for the paper's chosen kernel/shape. Argus must measure recurrent training state, activations and scratch independently rather than infer a training-memory win from inference-state scaling.
- Fused training kernels may have a narrower runtime/precision surface than ordinary dense matrix operations.
- Recurrent state must be associated with exact document/sequence/chunk identity; accidental cross-document state leakage is a correctness and privacy risk.
- The recurrent state must be restored or deterministically recomputed at a committed checkpoint boundary.

**Required comparison.** Build a small correctness reference for the generalized delta update, run finite-value/gradient checks, and compare full-sequence versus chunked/parallel formulations. Measure persistent inference state, training activations and scan scratch separately. Define state-reset and checkpoint semantics before using packed corpus sequences.

### 3.4 Attention–SSM/recurrent hybrid

**Core mechanism.** A hybrid composes attention and recurrent/state-space blocks so that selected layers retain direct token-to-token attention while other layers use bounded recurrent state.

**Sequence and state implications.**

- Inference memory includes both the attention KV cache for attention layers and recurrent/SSM state for recurrent layers.
- Training requires both attention and recurrent/scan backward implementations, plus consistent boundaries between block families.
- A hybrid can trade some attention layers for recurrent layers, but no universal optimal ratio follows from one published 8B experiment.
- Backend fallback becomes more complex: one unsupported operator can force a larger graph partition or CPU fallback, changing memory and runtime behavior.

**Evidence.** The NVIDIA/collaborator Mamba-2-Hybrid uses one 8B configuration with **43% Mamba-2 layers, 7% self-attention layers and 50% MLP layers** (by layer count). On its twelve reported standard tasks it scores 2.65 percentage points above the compared Transformer on average. Its "up to 8×" inference advantage is a **predicted** long-context generation result, not a phone measurement; the paper says short-context generation is roughly equivalent. Its measured training MFU is 29.9% versus 30.7% for the compared Transformer in a 1,024-H100 setup. The exact layer ratio, token horizon and training hardware are study-specific. They do not justify copying 43/7/50 into Argus or assuming hybrid is faster on Android.

**Argus risks and design questions.**

- More module types increase code, test, checkpoint and migration surface.
- The model can inherit the worst execution constraint of either subsystem if the backend cannot fuse or efficiently schedule both.
- Recurrent state and attention cache lifecycles must be unified in inference request identity and session recovery.
- It is not justified as the first implementation merely because it combines two families.

**Required comparison.** Keep hybrid as a secondary candidate. Compare only a small number of transparent compositions against their pure-family controls, with matched parameter/token/compute budgets and identical data. Report each block type's memory, latency, backward coverage and fallback cost separately.

## 4. Cross-family system matrix

| Axis | Transformer | Mamba-2 SSD | RWKV-7 | Hybrid |
|---|---|---|---|---|
| Training parallelism | Strong token-position parallelism through attention | Parallel scan/chunk formulation | Parallelizable recurrent training formulation | Both attention and scan/recurrent paths |
| History-dependent inference state | KV cache grows with retained context | Recurrent state bounded by layer/state dimensions | Matrix-valued recurrent state bounded by model dimensions | Both KV cache and recurrent state |
| Sequence-length compute pressure | Dense attention has quadratic sequence interaction | Linear/scan-style sequence structure, with dimension-dependent scan work | Recurrent/chunk formulation, with matrix-state update cost | Mixed; attention layers retain quadratic interaction |
| Training activation/workspace | Attention and FFN activations; tiled attention can reduce intermediate storage | Scan/convolution/state intermediates; SSD's 2–8× result is an A100 kernel comparison versus fused Mamba scan at N=64, not whole-model/mobile speedup; short-sequence full-model training may favor Transformer | Recurrent/chunk intermediates and matrix state; the RWKV-7 paper's stated H100 setup estimates 18 BF16 variable-equivalents (24 for its FP32 path) versus 10 for FlashAttention 3, so bounded inference state does not guarantee lower training memory | Combined attention and recurrent/scan workspaces |
| Android CPU reference | Feasible in principle with standard tensor ops; must be implemented and validated | Requires selective-scan reference and gradient validation | Requires delta-state reference and gradient validation | Requires both references and cross-block validation |
| Android GPU/NPU backward maturity | Best adjacent mobile fine-tuning/layout evidence, still not complete Argus support | No general Android full-backward guarantee | No general Android full-backward guarantee | Highest combined operator coverage burden |
| Checkpoint-specific state | Model/optimizer/RNG/data cursor; inference KV per active session | Model/optimizer/RNG/data cursor plus recurrent/chunk state at defined resume boundary | Model/optimizer/RNG/data cursor plus matrix recurrent state and chunk boundary | Both recurrent and attention session state |
| Migration complexity | Established family, but dimensions/tokenizer/head changes still require new generation | Specialized graph/state format | Specialized state/update format | Highest combined schema and backend migration surface |
| Current evidence transfer | Sub-billion deployment and mobile fine-tuning are adjacent evidence, not phone pretraining | Server-scale language-model comparison and SSD algorithm evidence | Published recurrent LM training and model evidence, not phone training | Server-scale hybrid evidence; no mobile random-init result |

This is a qualitative evidence matrix, not a performance ranking. Terms such as “linear” or “constant state” describe scaling against sequence history for the stated mechanism; they are not claims that actual runtime is faster or uses less total memory on every phone.

## 5. What the device-adaptive requirement actually changes

Argus must support different Android devices without turning every SoC into a separate model implementation.

The system needs two distinct decisions:

1. **Genesis configuration decision:** select a compatible architecture/profile bundle before allocating model tensors. This bundle includes architecture version, dimensions, tokenizer profile/fingerprint, precision, context and initialization policy.
2. **Runtime workload decision:** adapt microbatch, accumulation, sequence length within the approved contract, recomputation, scheduling and pause/resume to current memory, thermal, battery and OS conditions.

The runtime controller must not alter architecture, token IDs, parameter inventory or optimizer semantics in the middle of a training session. If a device cannot safely run the selected bundle, reject it or select another approved bundle before Genesis commit. Do not partially construct one architecture and silently fall back to another.

A small, maintainable profile portfolio is preferable to an unbounded architecture-per-chipset matrix. Whether the first release should use one backbone family with multiple model-size/resource profiles or a small portfolio of different families remains an explicit G4 decision. It must be resolved using implementation complexity, runtime coverage, quality evidence and device-resource estimates—not by assuming a newer architecture is automatically better.

## 6. Matched comparison rules for G4

The final design synthesis must compare candidates under controlled, reconstructable conditions:

### 6.1 Common model/data controls

- Use one exact tokenizer profile and fingerprint per compared configuration; G1-D must close before token-sensitive comparisons are frozen.
- Use the same immutable corpus revision, language/domain sampling manifest, train/validation/test split and document deduplication.
- Use the same causal objective and correctly token-weighted loss; record special-token and document-boundary behavior.
- Compare parameter-matched, token-matched and compute-matched views separately. No single matching rule is sufficient.
- Account for vocabulary embedding/output-head parameters as part of total model size and optimizer state.

### 6.2 Common runtime/resource controls

- Define one architecture-neutral tensor/state interface and a CPU correctness reference for each retained candidate.
- Record forward, loss, backward and optimizer operator coverage separately.
- Account for persistent model parameters, gradients, optimizer state, activations, logits, recurrent state, KV cache, scratch/workspaces, data staging, allocator overhead and checkpoint publication overlap.
- Keep inference memory and training memory separate; do not use KV-cache estimates as a proxy for training activation memory.
- Compare effective tokens per optimizer update and microbatch separately, with identical gradient accumulation/loss-weighting semantics.
- Use the same optimizer comparison set and optimizer-specific tuning budget. Lion is a candidate, not a prior winner; see [37 — Optimizer Research and Lion Assessment](37_OPTIMIZER_RESEARCH_AND_LION_ASSESSMENT.md).
- Separate published measurements, analytical estimates, reference implementation behavior and unverified Argus assumptions in every table.

### 6.3 Required synthesis outputs

For each architecture candidate, G4 must publish:

1. A versioned module/graph definition and exact tensor inventory, using [42 — Architecture-Family Tensor and Operator Inventory](42_ARCHITECTURE_TENSOR_AND_OPERATOR_INVENTORY.md) as the symbolic accounting contract and filling every dimension from the selected profile.
2. Initialization, normalization, positional/state and residual semantics.
3. Training objective, backward contract, optimizer/parameter groups and precision policy.
4. Tokenizer/profile binding, context/packing policy and special-token boundary.
5. Analytical per-profile parameter, optimizer, activation, recurrent/KV, workspace and checkpoint memory envelope.
6. CPU reference and candidate backend/operator coverage table, including known unsupported operations.
7. Model/optimizer/RNG/sampler/cursor checkpoint and exact-resume contract, including complete recurrent carry, sequence/document reset and chunk/BPTT semantics where applicable; see [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md).
8. Migration, generation isolation, rollback and model-quality evaluation plan.
9. An explicit list of assumptions to verify in E1–E7 after implementation.

A candidate may be selected as a design to implement while its Android physical support remains unverified. That uncertainty must be explicit and cannot be converted into a support claim.

## 7. Current G2/G3/G4 position

- **G2:** CLOSED only for the initial four-family comparison shortlist. This document expands the comparative synthesis and does not add or remove a family.
- **G3:** CLOSED only for the one-primary-disposition planning baseline. Architecture-dependent and tokenizer-sensitive components remain DEFER where marked.
- **Optimizer:** DEFER; AdamW remains the reference/control, Lion is admitted as a memory-sensitive candidate, and no optimizer is selected.
- **G4:** OPEN/BLOCKED until G1-D closes, the controlled design comparison is complete, each candidate's full random-init/backward/optimizer/checkpoint contract is documented, and a reviewed architecture ADR is issued. For Mamba-2/RWKV-7 candidates, document 41's complete state-component, reset/carry, chunk/BPTT and recurrent-state recovery contracts are mandatory inputs.
- **Physical Android verification:** E1–E7 remains a later implementation-verification gate. It is not required to finish this documentary synthesis.

## 8. Primary references

1. Liu et al., ICML 2024, [MobileLLM: Optimizing Sub-billion Parameter Language Models for On-Device Use Cases](https://proceedings.mlr.press/v235/liu24ce.html). Sub-billion architecture design, deep/thin blocks, embedding sharing and grouped-query attention.
2. Dao and Gu, ICML 2024, [Transformers are SSMs: Generalized Models and Efficient Algorithms Through Structured State Space Duality](https://proceedings.mlr.press/v235/dao24a.html). Mamba-2/SSD mathematical formulation; Figure 10 reports A100 80GB PCIe kernel timings and a 2–8× SSD-vs-Mamba-scan result only for its stated state-expansion/sequence conditions.
3. Waleffe et al., 2024, [An Empirical Study of Mamba-based Language Models](https://arxiv.org/abs/2406.07887). Controlled 8B Transformer/Mamba/Mamba-2/hybrid comparison up to 3.5T tokens; task/token-horizon sensitivity, 70/15/15 English/non-English/code data mixture, 256K SentencePiece vocabulary, and H100 training results.
4. Peng et al., COLM 2025, [RWKV-7 Goose with Expressive Dynamic State Evolution](https://openreview.net/pdf?id=ayB1PACN5j). Generalized delta-rule recurrence, bounded history-dependent inference state, parallelized training and 0.19B–2.9B released models; those checkpoints were upgraded from existing RWKV-5/RWKV-6 weights and are not clean random-init RWKV-7 evidence.
5. [RWKV-7 architecture and implementation documentation](https://github.com/RWKV/RWKV-wiki/blob/main/docs/basic/architecture.md). Architecture/version details; community documentation, not Android runtime evidence.
6. [MobileFineTuner Android SDK documentation](https://github.com/Edge-Intelligence-Lab/MobileFineTuner/blob/main/docs/ANDROID_SDK.md) and [FBLayout paper](https://arxiv.org/abs/2607.21624). Mobile training-step, memory-layout and fine-tuning evidence; not a from-scratch architecture winner.

