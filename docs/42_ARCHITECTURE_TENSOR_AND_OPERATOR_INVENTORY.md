# Architecture-Family Tensor and Operator Inventory

**Status:** Symbolic architecture inventory; no dimensions or backbone selected  
**Last reviewed:** 2026-11-05  
**Scope:** Parameter shapes, forward/backward operator inventory, logits/state accounting and profile-estimation inputs for the four G2 families  
**Dependencies:** Documents 33–41  
**Boundary:** Documentary and analytical contract only. This document does not authorize implementation, builds, workflows, device benchmarks or physical Android support claims.

## 1. Purpose

G4 must compare complete model bundles, not just parameter counts or inference-state formulas. This document defines a symbolic tensor/operator inventory for the retained families:

1. Decoder-only Transformer;
2. Selective SSM / Mamba-2 SSD;
3. Recurrent delta-rule / RWKV-7;
4. Attention–SSM/recurrent hybrid.

The formulas use symbolic dimensions. They intentionally do not prescribe one global parameter count, vocabulary, hidden width, layer count, context length or precision. A real candidate manifest supplies those values and produces an exact ordered tensor inventory.

Use these symbols:

| Symbol | Meaning |
|---|---|
| \(V\) | Vocabulary size, including every reserved special/control ID |
| \(D\) | Model/embedding width |
| \(L\) | Number of model blocks/layers |
| \(B\) | Training or inference batch size |
| \(T\) | Input sequence length in tokens |
| \(C\) | Declared model context length |
| \(d_h\) | Attention head dimension |
| \(H_q\) | Number of query heads |
| \(H_{kv}\) | Number of key/value heads |
| \(F\) | Conventional dense FFN intermediate width |
| \(s_x\) | Bytes per scalar for tensor category \(x\) |
| \(P\) | Total trainable parameter count, derived from all tensor shapes |

All dimensions must satisfy family-specific divisibility and shape constraints. Vocabulary is not an independent knob once tokenizer and special-token policy are fixed.

## 2. Shared accounting rules

### 2.1 Exact parameter inventory

The authoritative parameter count is the sum of the element counts of the actual trainable tensors:

\[
P = \sum_{i=1}^{n} \operatorname{numel}(\theta_i)
\]

Do not estimate P only from layer count × hidden width. Include embedding/head tying, bias policy, normalization vectors, recurrent/SSM parameters, gates, learned position tables and architecture-specific state parameters.

For every tensor record:

- stable logical name and parameter role;
- exact shape and trainable/frozen status;
- tied/shared storage identity;
- initializer and parameterization group;
- optimizer parameter group and weight-decay mask;
- storage/compute/gradient/optimizer-state dtype;
- serialization order and fingerprint.

A tied input embedding/output head is one trainable parameter tensor with two graph references, not two separately allocated copies. An untied head adds its own V × D matrix.

### 2.2 Separate memory domains

The profile estimator must report these independently:

1. model parameter storage;
2. gradient storage;
3. optimizer state;
4. optional FP32 master weights and precision/scaler state;
5. forward/backward saved activations;
6. full or tiled attention/SSM/recurrent intermediates;
7. output logits and loss buffers;
8. inference KV or recurrent carry per active request;
9. temporary operator workspaces and allocator fragmentation;
10. input/token/data staging;
11. checkpoint serialization buffers and atomic-publication overlap;
12. application/runtime/system reserve.

A useful first-order inventory is:

\[
M_{weights}=P\,s_w,\quad
M_{grad}=P\,s_g,\quad
M_{optimizer}=P\,s_o
\]

These are terms, not a complete peak-memory equation. Optimizer bytes per parameter depend on the selected algorithm and state dtype; see document 37. Peak memory is the maximum of the actual live tensor/workspace set across the forward, backward, optimizer and checkpoint phases, not the sum of every tensor that exists at different times.

### 2.3 Vocabulary cost never disappears

For any family with a conventional embedding and vocabulary output projection:

\[
M_{embedding}=V D\,s_w
\]

\[
M_{logits}=B T V\,s_{logit}
\]

The output head may be tied to embeddings, eliminating one V×D parameter matrix, but it does not eliminate the BTV output-logit computation/storage term. Chunked logits/loss can reduce peak buffer residency only if the loss and gradient semantics remain equivalent.

Tokenizer ID/fingerprint, vocabulary size, special-token inventory, hidden width and head tying must be pinned together in the candidate bundle.

## 3. Decoder-only Transformer inventory

### 3.1 Embedding and output

- Token embedding: E ∈ R^(V×D).
- Optional learned absolute-position embedding: P_abs ∈ R^(C×D).
- RoPE has frequency/configuration buffers but no learned C×D position table.
- ALiBi uses fixed head slopes/bias semantics; it is not a V×D parameter table.
- Output projection: tied Eᵀ or independent W_lm ∈ R^(D×V).
- Final normalization: parameter vectors of width D, depending on norm choice.

### 3.2 Attention block

For MHA/GQA/MQA:

\[
Q \in \mathbb{R}^{B \times T \times (H_q d_h)}
\]

\[
K,V \in \mathbb{R}^{B \times T \times (H_{kv} d_h)}
\]

\[
O \in \mathbb{R}^{B \times T \times D}
\]

Typical projection weights (orientation shown as input × output):

| Tensor | Shape |
|---|---|
| W_Q | D × (H_q d_h) |
| W_K | D × (H_kv d_h) |
| W_V | D × (H_kv d_h) |
| W_O | (H_q d_h) × D |

MHA has H_kv = H_q; GQA has 1 < H_kv < H_q; MQA has H_kv = 1. Include projection biases only when the selected graph declares them.

The logical KV-cache term per active session is:

\[
M_{KV}=2 L B T H_{kv}d_h\,s_{KV}
\]

This is inference cache only. It is not the training attention matrix or backward memory. Dense attention's score tensor is nominally B H_q T²; a tiled/flash implementation may avoid materializing that full tensor, but exact peak scratch and backward saved values depend on the kernel.

### 3.3 Feed-forward block

Conventional dense FFN:

- W_1 ∈ R^(D×F)
- W_2 ∈ R^(F×D)

Gated FFN (for example SwiGLU):

- W_g ∈ R^(D×F_g)
- W_v ∈ R^(D×F_g)
- W_o ∈ R^(F_g×D)

A parameter-matched first estimate uses F_g near two-thirds of F, but exact equality must account for biases, tied tensors and rounded dimensions. Do not compare the same intermediate width and then call it parameter-matched.

### 3.4 Transformer operator list

Forward and backward support must be listed independently for:
- embedding gather and output projection/logits;
- Q/K/V/O dense projections;
- RoPE or ALiBi score/mask path;
- causal attention and softmax;
- dense or gated FFN;
- normalization and residual additions;
- token-weighted loss and its logits/gradient path.

Each candidate must name its attention kernel semantics, mask representation, precision, dropout/training behavior, causal/document-boundary treatment and memory-efficient backward path. “GPU supports matrix multiply” is not proof that it supports the complete training graph.

## 4. Mamba-2 SSD inventory

### 4.1 Dimension derivation

For the pinned official Mamba-2 implementation, define:

- model width D = d_model;
- expansion e;
- inner width I = floor(eD) (exact divisibility required in the non-parallel implementation);
- SSM width S = d_ssm, defaulting to I;
- state dimension N = d_state;
- convolution width K = d_conv;
- number of groups G = ngroups;
- head width P = headdim;
- SSM head count H = S/P, requiring S mod P = 0.

For the unsharded reference path, the combined input projection output width is:

\[
D_{in}=2I+2GN+H
\]

The split order is [z,x,B,C,dt]. When S < I, the implementation also derives an MLP remainder width from the projection and splits extra z0/x0 branches; therefore the exact implementation manifest must record whether a partial-SSM/MLP branch is enabled.

### 4.2 Parameter tensors

| Tensor/component | Logical shape | Notes |
|---|---|---|
| Input projection | D × D_in | Splits z, x, B, C and dt; include declared bias |
| Depthwise causal convolution | weight [S+2GN, 1, K], optional bias [S+2GN] | Groups = input channels; one filter per channel |
| dt bias | [H] | Special initialization; no-weight-decay in reference |
| A_log | [H] | Reference initializes from positive A range then stores log; no-weight-decay |
| D skip parameter | [H] or [S] | Head-wise or per-channel according to D_has_hdim |
| Gated RMSNorm | width S | Present when selected; group size depends on G |
| Output projection | I × D | Returns block output to model width |
| Optional MLP remainder | derived from I-S | Must be enumerated, not omitted from parameter accounting |

The input projection and output projection are not the entire Mamba-2 parameter inventory. A_log, dt bias, D, convolution parameters and normalization/gating are part of the trainable graph.

### 4.3 Recurrent inference state and chunk operators

The reference's convolution width is:

\[
D_{conv}=S+2GN
\]

Per layer, the streaming carry is:

\[
M_{convState}\in\mathbb{R}^{B\times D_{conv}\times K}
\]

\[
S_{ssm}\in\mathbb{R}^{B\times H\times P\times N}
\]

Store their dtypes independently. In the pinned source, default convolution-state dtype follows convolution weights; SSM-state dtype follows input-projection weights; an explicit cache dtype can override both. Reset both states together at independent document boundaries.

The chunk training path includes:
- input projection and z/x/B/C/dt split;
- causal depthwise convolution with sequence-ID/reset semantics;
- SiLU activation;
- dt transformation/softplus and state decay;
- grouped B/C state interaction and SSD local/chunk scan;
- D skip and optional z/RMSNorm gate;
- output projection;
- backward through projections, convolution, local SSD and inter-chunk carried state.

The complete Mamba-2 graph requires forward and backward coverage for all of these. A decode-only step or an inference-only scan does not satisfy Genesis training.

## 5. RWKV-7 inventory

### 5.1 Block topology

The reviewed x070 reference has two residual sublayers per block:
1. TimeMix / generalized delta-rule WKV update;
2. ChannelMix / feed-forward update.

Each block normalizes before each sublayer, adds each sublayer result residually, and the model applies final normalization and a vocabulary output head. It uses recurrent state for order/history rather than a Transformer attention KV cache.

### 5.2 Parameter groups

For model width D, head width N and H = D/N heads in the referenced x070 layout:

- token embedding E ∈ R^(V×D);
- per-block LayerNorm parameters (ln1, ln2) and final ln_out; the demo checkpoint path also applies initial ln0 normalization to embedding output;
- TimeMix per-channel mixing vectors for receptance, decay, key, value, a/gate and related branches;
- TimeMix low-rank factors and projections for dynamic decay, in-context learning/state-update coefficients, gating and receptance/key/value/output transforms;
- per-head key normalization and receptance-key bonus parameters;
- ChannelMix interpolation vector, key projection and value projection;
- output head W_head ∈ R^(V×D) or its equivalent transposed convention.

The exact x070 tensor names/shapes are checkpoint/schema-specific. A candidate must use the selected upstream reference version's full state-dict inventory rather than approximate all TimeMix factors as one dense matrix. The per-family graph fingerprint must include low-rank widths, head size, per-head count, interpolation vectors and normalization/bonus parameters.

### 5.3 Recurrent state and operators

The audited RWKV-LM RNN-mode reference defines three state slots per layer:
- previous TimeMix input (att_x_prev) with shape [D];
- WKV matrix state (att_kv) with shape [H,N,N], FP32 in the demo;
- previous ChannelMix input (ffn_x_prev) with shape [D].

Thus, for one active batch stream the reference carry per layer is 2D + HN² scalars, with independent dtypes for shift and matrix state. The COLM paper and document 41 provide the optimized kernel's distinct backward-snapshot behavior; this RNN demo describes inference carry, not training memory.

Required forward/backward operators include:
- embedding and normalization;
- TimeMix input shifts and six projection/mixing branches;
- data-dependent decay and delta-rule WKV state update;
- per-head key normalization, group normalization and receptance-key bonus;
- output projection/gating and residual addition;
- ChannelMix shift, key projection, squared-ReLU activation and value projection;
- final normalization, logits and token-weighted loss.

A generic “RNN cell” or “matrix multiply” operator is not sufficient to claim RWKV-7 training support. The WKV update and its backward kernel are specialized and require their own CPU reference and gradient parity.

### 5.4 RWKV-7 inference carry formula

For equal key/value head dimensions N:

\[
M_{WKV}=B L H N^2\,s_{WKV}
\]

\[
M_{shift}=2 B L D\,s_{shift}
\]

\[
M_{RWKV7,carry}=M_{WKV}+M_{shift}
\]

These formulas match the audited x070 reference's two shift vectors and WKV matrix per layer. They must be revisited if an alternate RWKV version adds/removes state components or changes head dimensions. Do not confuse inference carry with the paper's training snapshots/activation memory.

## 6. Attention–SSM/recurrent hybrid inventory

A hybrid is a declared ordered list of block families, not a vague blend. The model manifest must provide:

- exact per-layer family map and block index;
- each block's own tensor inventory and initialization rules;
- attention layers' Q/K/V/O and optional KV-cache contract;
- SSM layers' convolution and SSM state contract, or RWKV layers' WKV and shift state;
- inter-block width adapters/norms if widths differ;
- per-block precision, layout and backend requirements;
- stateful request and document reset order;
- checkpoint state-component inventory and schema version.

Its parameters are the sum of the exact parameters of constituent blocks plus adapters, embeddings and output head. Its inference state is the sum of live attention cache plus recurrent state for each session. Training memory is not the sum of their separate peak estimates unless lifetimes truly overlap; compute the actual schedule's liveness and workspace peaks.

No hybrid layer ratio is selected. The NVIDIA 43/7/50 Mamba2/attention/MLP composition remains a study configuration only; see document 38.

## 7. Candidate-bundle memory worksheet

For every proposed model profile, produce a machine-readable worksheet with at least:

| Field | Formula/source |
|---|---|
| Parameter count | Exact sum from ordered tensor shapes |
| Weight bytes | Sum of per-tensor numel × storageBytes |
| Gradient bytes | Sum of trainable parameter gradient allocations under selected dtype policy |
| Optimizer bytes | Exact selected optimizer-state inventory; see document 37 |
| Optional master-weight bytes | Sum only where the precision policy requires it |
| Training logits | B × T × V × logitsBytes, adjusted only if chunked logits are proven semantically equivalent |
| Embedding/head parameters | Tied or untied exact tensor inventory |
| Attention KV cache | 2 × L_attention × B_requests × T_context × Hkv × d_head × bytes, only for attention layers |
| Mamba-2 carry | Both convolution and SSM states, every active layer/session |
| RWKV-7 carry | WKV matrix and both shift vectors, every active layer/session |
| Saved activations | Actual forward/backward liveness schedule, including rematerialization policy |
| Kernel scratch/workspace | Per operator and exact backend/version; no peak TOPS substitution |
| Data staging | Batch, prefetch, token IDs and pinned/native buffers |
| Checkpoint overlap | Concurrent candidate/model/optimizer/recurrent-state serialization and atomic publication |
| Application reserve | Explicit Android/process/allocator safety margin |

### 7.1 Admission policy

1. Apply product-language and source-rights eligibility before resource ranking.
2. Build the complete candidate bundle (tokenizer, vocabulary, architecture, dimensions, precision, context, optimizer and backend).
3. Calculate Genesis preparation peak and post-Genesis training peak separately.
4. Reject bundles that violate any hard memory/storage/runtime/correctness constraint.
5. Rank only the feasible Pareto set; do not collapse language coverage, quality, memory, speed and energy into a single unexplained score.
6. Record the selected bundle, all rejected candidates and each reason before tensor allocation.
7. Runtime workload controls may reduce microbatch, add accumulation or schedule/pause only within the session contract. They may not mutate model width, tokenizer IDs, block family or optimizer semantics inside an active generation/session.

The numeric safety margins and per-device thresholds remain open until the documented memory model is reconciled with Android app/system reserve evidence and later E1–E7 measurements. This document intentionally supplies formulas and inventory fields, not invented cutoff values.

## 7.2 First-pass Transformer resource envelope

This subsection turns the symbolic inventory into a repeatable analytical worksheet for the first-generation Transformer profile portfolio. It is a sizing method, not a selected set of model dimensions or a claim of Android feasibility. Use the document 43 reference graph for the initial control: decoder-only, MHA, RoPE, dense GELU FFN, Pre-LN + LayerNorm. Component alternatives remain separate ablations under document 39.

### 7.2.1 Required profile inputs

Every candidate row must pin these values before calculating bytes or FLOPs:

| Input | Meaning |
|---|---|
| \(V\) | Exact vocabulary including reserved IDs from the tokenizer fingerprint |
| \(D,L,F\) | Model width, block count and dense FFN intermediate width |
| \(H_q,H_{kv},d_h\) | Query/KV heads and head width; enforce the selected graph's exact projection dimensions |
| \(T_{train},B_\mu\) | Training sequence length and live microbatch |
| \(C,B_{req}\) | Inference context and simultaneous active requests |
| \(s_w,s_g,s_o,s_m,s_{act},s_{logit},s_{KV}\) | Bytes per element for weights, gradients, optimizer state, optional master weights, activations, logits and KV cache |
| \(R_{accum}\) | Gradient-accumulation count; changes effective batch/update exposure, not the number of simultaneously resident activation graphs |
| Runtime/backend | Exact CPU reference, Vulkan compute device/driver/queue/features, optional NPU provider, and per-operator forward/loss/backward/optimizer/serialization coverage |
| Device budget | Measured app/process headroom, storage reserve, baseline resident footprint and evidence confidence; do not substitute total physical RAM |

Training sequence length \(T_{train}\) and inference context \(C\) are different controls and must not be conflated. The tokenizer vocabulary \(V\) is a dependency from G1-D, not a width-scaling knob.

### 7.2.2 Transformer parameter and persistent-state estimate

Let \(D_q=H_qd_h\) and \(D_{kv}=H_{kv}d_h\). For the reference MHA graph, \(D_q=D_{kv}=D\). The projection matrix count per block before optional biases is:

\[
P_{attn,block}=2DD_q+2DD_{kv}
\]

The conventional dense FFN contributes:

\[
P_{ffn,block}=2DF
\]

For a Pre-LN block with two affine LayerNorms, the norm contribution is \(4D\) parameters per block; a final affine LayerNorm contributes \(2D\). Add declared biases, positional parameters, special learned tensors and any selected extras from the exact tensor inventory.

For a tied embedding/output head:

\[
P_{embed/head}=VD
\]

For an untied output head, add another \(VD\). RoPE adds no learned \(C\times D\) position table. A learned absolute-position table, if separately selected, adds \(CD\).

A useful first-order control estimate is therefore:

\[
P \approx VD(1+I_{untied})+
L(2DD_q+2DD_{kv}+2DF+4D)+2D+P_{bias}+P_{other}
\]

where \(I_{untied}=0\) for tied and \(1\) for untied. This is a sensitivity estimate only. The authoritative \(P\) is still the exact sum of tensor element counts in section 2.1. Do not use this approximation to omit biases, norm variants, adapters or other graph tensors.

Persistent training allocation, before transient activations/workspaces, is:

\[
M_{persistent}=P(s_w+s_g+s_o+s_m)
\]

Here \(s_o\) is the sum of the selected optimizer-state bytes per parameter, and \(s_m=0\) when no FP32 master copy is required. Under the document 37 FP32-state comparison, AdamW contributes 8 optimizer-state bytes/parameter and Lion contributes 4. Those are separate candidate rows, not a product optimizer decision. Gradient accumulation does not multiply persistent optimizer state by \(R_{accum}\); the exact implementation must state whether any uncommitted accumulation buffer or master copy is additionally live.

### 7.2.3 Logits, activations and attention workspace

Unchunked training logits require:

\[
M_{logits}=B_\mu T_{train}V s_{logit}
\]

The logits gradient, loss intermediates and any simultaneous cast/copy buffers must be listed as separate live tensors where the implementation retains them. Vocabulary chunking may reduce the peak only after token-weighted loss, gradient and exact-update equivalence are established; do not simply divide this estimate by a chunk count without an execution/liveness contract.

Activation memory must be calculated from the actual saved-tensor and rematerialization schedule:

\[
M_{saved}=\sum_{i\in SavedForwardTensors}\operatorname{numel}(A_i)s_{A_i}
\]

Do not substitute a universal “X bytes per parameter” or fixed activation multiplier. For a reference implementation that retains dense attention scores, the score allocation can scale as \(L B_\mu H_q T_{train}^2 s_{score}\); a causal-only compact representation can instead be related to \(N_{pairs}=T_{train}(T_{train}+1)/2\). A tiled/Flash-style path must report its actual tile/workspace and backward saved values. Avoiding a full score matrix reduces residency, not quadratic attention compute. If the backend does not yet exist, mark its activation/workspace term as an explicit unknown or bounded scenario rather than assigning it zero.

### 7.2.4 Forward compute and training-work estimate

Count multiply-add as two FLOPs. For one dense causal-attention layer, define \(N_{pairs}\) as the number of query-key positions actually evaluated per sequence. For ideal causal-only work, \(N_{pairs}=T_{train}(T_{train}+1)/2\); a dense masked implementation may evaluate \(T_{train}^2\) pairs.

With the selected projection widths, the forward matrix-multiply estimate is:

\[
F_{proj}=2B_\mu T_{train}D(2D_q+2D_{kv})
\]

\[
F_{ffn}=4B_\mu T_{train}DF
\]

\[
F_{attention}=4B_\mu D_qN_{pairs}
\]

The vocabulary projection adds approximately \(2B_\mu T_{train}DV\) FLOPs. Thus:

\[
F_{forward,matmul}\approx L(F_{proj}+F_{ffn}+F_{attention})+2B_\mu T_{train}DV
\]

For a first training estimate, forward plus backward dense matrix operations are approximately \(3F_{forward,matmul}\). This is not an exact operation count: softmax, normalization, activation, masking, loss, embedding lookup, optimizer update, padding, recomputation and kernel inefficiency are additional. Record the exact graph-level count when available and retain this formula as a transparent comparison baseline.

A theoretical TOPS rating is not a time estimate. A lower-bound runtime may be written as \(t\ge F_{work}/Q_{sustained}\), but \(Q_{sustained}\) must come from the exact operator/backend on the relevant device under sustained thermal conditions. Before such evidence exists, report FLOPs and leave wall-clock throughput unverified.

### 7.2.5 Phase-specific peak memory and storage

Use liveness by phase. The profile worksheet must show separate rows for:

| Phase | Included terms and overlap to calculate |
|---|---|
| Tokenizer resolution/preparation | Tokenizer artifact, trainer buffers and selected seed-corpus streaming window; do not count the entire corpus file as resident when the importer/trainer streams it |
| First Genesis | Model tensor allocation, initializer scratch, deterministic chunk buffers, staged checkpoint/write buffer and runtime baseline; state whether initialization is fully resident or chunk-streamed |
| Training forward/backward | Weights, gradients, optimizer state, optional master weights, saved activations, attention workspace, logits/loss tensors, data staging and any concurrent inference |
| Optimizer update | Persistent state plus fused/eager update scratch and temporary casts/copies |
| Inference | Active model weights, \(2LB_{req}CH_{kv}d_hs_{KV}\) KV bytes for attention layers, prefill/decode workspace, logits and concurrency |
| Checkpoint publication/recovery | Model + optimizer + RNG/scheduler/cursor state, serialization buffers, staged new checkpoint, retained prior checkpoint and journal; include only copies that the actual atomic protocol keeps live |
| Generation migration | Parent generation, candidate child generation, validation/evaluation state and rollback reserve when they genuinely coexist |

For a phase \(p\), calculate the peak from the tensors that are simultaneously live at time \(t\):

\[
M_{live}(p,t)=\sum_{i\in Live(p,t)}\operatorname{numel}(i)s_i+M_{workspace}(p,t)
\]

\[
M_{workload,peak}=\max_p\max_t M_{live}(p,t)
\]

\[
M_{baseline}+M_{workload,peak}+M_{safety}\le M_{app/process\ limit}
\]

The final inequality is the RAM admission condition. \(M_{baseline}\) is the measured Argus/app resident footprint before the candidate workload; \(M_{safety}\) is a declared reserve, not an unexplained multiplier. Include actual cross-workload overlap, such as foreground inference while background training is live. Do not add unrelated phase peaks together. For example, a chunk-streamed Genesis path may avoid holding a second complete model copy, while a child-generation migration may genuinely need parent and candidate storage at the same time.

Storage admission is separate from RAM admission. Calculate its own simultaneous-file peak:

\[
S_{peak}=\max_t\sum_{j\in RetainedOrStagedFiles(t)}Size(j)
\]

The storage set must include only files that coexist under the actual checkpoint/rollback protocol, plus the declared free-space reserve. Record the tokenizer artifact, model weights, resumable optimizer/checkpoint state, active generation, staged generation, retained rollback checkpoint, journals and temporary files independently. Corpus storage is governed by the corpus/acquisition budget and must not be silently charged to model in-memory staging or omitted from free-storage checks.

### 7.2.6 Bounded profile sweep and admission order

Use a finite candidate table, not a new configuration for every SoC label:

1. Fix an eligible tokenizer artifact/profile and its \(V\), special IDs and language fixture. Until G1-D closes, show a range or separate candidate rows for vocabulary uncertainty; do not freeze embedding/head dimensions.
2. Use exact graph divisibility constraints to enumerate a small set of \(D,L,F,H_q,H_{kv},d_h\) hypotheses. Begin with the document 43 MHA reference and keep GQA as a separately labelled ablation.
3. For each graph, evaluate a small declared set of \(T_{train}\), \(C\), \(B_\mu\), precision and optimizer-state policies. Do not mix FP32 correctness-reference results with lower-precision throughput claims.
4. Calculate exact tensor bytes, persistent optimizer state, logits, saved activations, workspaces and phase-specific peaks. Run both an analytical no-concurrency case and the intended background-training-plus-inference case if that is a product requirement.
5. Reject any candidate that fails tokenizer/runtime eligibility, full training-operator coverage, correctness, RAM headroom, storage/rollback reserve or a required operational gate. A candidate that fits weights alone is not admitted.
6. Retain a Pareto set across model capability proxies, language/tokenizer coverage, peak RAM, storage, compute, sustained energy/thermal cost and implementation complexity. Do not collapse these into one unexplained score.
7. Assign stable profile IDs only to a finite set that survives review. Device capability selects among those immutable profiles; runtime pressure may lower microbatch, accumulate gradients or pause only where the selected training contract permits it.

The device-side memory budget must be based on measured application/process limits and baseline headroom, less an explicit safety reserve. Total marketed RAM, memory-class metadata alone, SoC family names or theoretical accelerator TOPS are not sufficient admission evidence. Numeric profile cutoffs remain open until the evidence source and reserve policy are recorded; no threshold is invented in this research pass.

**Current analytical output:** equations, live-memory categories, phase boundaries and the finite sweep procedure are now defined. Exact numerical profiles remain blocked by the unresolved tokenizer vocabulary/runtime contract, final graph dimensions, precision/update choices, and measured per-device process/storage/throughput evidence. This does not prevent continuing the documentary comparison; it prevents claiming a numeric device profile is already supported.


### 7.2.7 Generation Repository and Genesis lifecycle binding

The memory/storage worksheet must follow the actual generation transaction rather than treating every model-related file as one permanent allocation:

1. **Initial Genesis generation:** account for tokenizer resolution, model weights/tensor initialization, immutable generation manifest, checksum verification and staged publication. The initial generation is not a trained session checkpoint; do not charge it for AdamW/Lion state, gradients, training activations or a data cursor that does not exist yet.
2. **Background training candidate:** after APPLICATION_READY, the Learning Coordinator creates a child candidate bound to the exact active parent digest. The child training session owns optimizer/scheduler/scaler/RNG/cursor state. The active parent remains immutable. Include the child model's changed/new tensors and all required optimizer state.
3. **Parent inference during child training:** if inference continues against the parent snapshot, its weights, KV cache, logits/workspaces and concurrency remain live alongside the candidate's training peak. This combined case is admitted only when the full overlap fits. Otherwise the scheduler must serialize or pause one workload; concurrent inference is not assumed.
4. **Candidate publication and rollback:** parent generation, candidate generation and last-known-good checkpoint may coexist until the candidate passes evaluation and promotion. Count only the files/pages/buffers that the selected repository protocol actually retains. A digest reference to an immutable parent tensor avoids a duplicate payload only when the storage layer truly shares that payload and verifies it on restore.
5. **Copy-on-write is not free:** if the candidate initially shares parent tensors, count cloned pages/tensors, copy-on-write metadata, write amplification and validation buffers at their actual peak. If it uses a full copy, count both parent and child model storage. Do not model the two policies as identical.
6. **Checkpoint recovery:** the candidate checkpoint binds child tensors, optimizer/scheduler/scaler/RNG, data cursor, committed update and any required recurrent state to one transaction. It is distinct from the Genesis manifest. A chunk-streamed serializer may bound RAM, but the staged checkpoint, checksum/journal and retained last-known-good checkpoint still consume storage until publication/recovery rules release them.
7. **Architecture/tokenizer migration:** a child with changed dimensions, tensor schema or tokenizer fingerprint is a migration candidate, not an in-place edit. Include parent + new candidate + conversion/initialization workspace + validation state + rollback reserve wherever they coexist.

These rules align with documents 19 (Genesis-to-training and P0 checkpoint contract), 20 (initial generation commit and post-readiness learning) and 36 (ModelGenerationRepository, CheckpointManifest, immutable inference snapshots and storage transactions). The repository interface names in document 36 are logical contracts, not proof that full-copy or copy-on-write storage has been implemented.


### 7.2.8 Profile-candidate matrix: model shape and execution envelope

Do not start by naming profiles “low”, “mid” or “high” from total RAM or a SoC list. Build a bounded matrix from a fixed tokenizer/graph contract and measured process budgets. The matrix has two independent axes: **model-shape candidates** and **execution envelopes**. A product profile is created only from a reviewed, feasible combination of both axes plus tokenizer, precision, optimizer and backend fingerprints.

#### Model-shape candidate sweep

| Candidate | Symbolic shape | What it isolates | Mandatory controls |
|---|---|---|---|
| M0 — reference proxy | \(D_0,L_0,F_0\) | Smallest analytical/reference point admitted by the declared objective and exact tensor constraints | Fixed \(V\), tokenizer fingerprint, head dimension, objective, data split, context, precision reference and operator graph |
| MW — width proxy | \(D_1,L_0,F_1\), \(D_1>D_0\) | Effect of width/parameter scale while depth is held fixed | Keep \(V,L_0,d_h\), context, data exposure and graph semantics fixed; derive \(H_q=D_1/d_h\) and \(F_1=r_{ffn}D_1\) only after declaring the FFN ratio |
| ML — depth proxy | \(D_0,L_1,F_0\), \(L_1>L_0\) | Effect of depth while width is held fixed | Keep \(V,D_0,d_h\), context, tokenizer, FFN ratio, data exposure and graph semantics fixed; count every attention and FFN residual branch for initialization |
| MWL — combined candidate | \(D_1,L_1,F_1\) | Interaction of width and depth after their independent effects are understood | Admit only after MW and ML have separate resource/quality evidence; re-run exact tensor, initialization, optimizer, activation and checkpoint estimates |

This is a controlled hypothesis matrix, not four approved model profiles. \(D_0,D_1,L_0,L_1,F_0,F_1\) must be selected by the analytical search; no numeric value is implied here. A width-transfer comparison requires at least two distinct widths; a depth-scaling comparison requires at least two distinct depths. Keep the other dimension fixed for each isolated comparison. A combined width+depth candidate must not be used as the only evidence for either effect.

For the MHA reference, enforce \(D=H_qd_h\) and \(H_{kv}=H_q\). If a chosen width is not divisible by the pinned head dimension, reject that shape or declare a different explicit head-dimension hypothesis. GQA/MQA are separate attention ablations, not a hidden width adjustment. Keep vocabulary \(V\) fixed for within-tokenizer width/depth comparisons. A different tokenizer/vocabulary creates a separate profile family because it changes embedding/head tensors, logits, sequence fertility and checkpoint compatibility.

Use an explicit FFN ratio \(r_{ffn}=F/D\) for the width sweep. Do not silently assume that one published model's FFN multiplier is the Argus default. If a later profile chooses SwiGLU, produce a parameter-matched gated width \(F_g\) rather than reusing the dense FFN intermediate width.

#### Execution-envelope candidates

| Envelope | Workload policy | Required admission evidence |
|---|---|---|
| X-SERIAL | Training and inference are serialized; parent inference is paused while the child candidate trains | Training peak and inference peak separately fit; durable pause/checkpoint transition; no simultaneous parent-inference allocation assumed |
| X-OVERLAP | Immutable parent inference continues while an isolated child candidate trains | Combined parent weights/KV/workspace + child training peak + runtime reserve fits; snapshot lifetime and promotion semantics are verified |
| B-CPU-REFERENCE | Correctness/reference execution on CPU | Exact forward/loss/backward/optimizer support, numerical parity, sustained CPU throughput and process-memory evidence for every required operator |
| B-VULKAN-COMPUTE | GPU compute pipelines via a queried Vulkan driver; does not require an NPU | Actual loader/driver and physical device; API version; compute queue; required core features/extensions/limits; FP32 baseline; optional FP16 arithmetic and storage queried independently; complete per-operator forward/loss/backward/optimizer/recovery coverage; shader/pipeline cache, transfer, synchronization, workspace, numerical parity and sustained thermal evidence |
| B-NPU-VENDOR | Optional vendor NPU/DSP execution path | Exact SDK/runtime, SoC/driver compatibility, available training operators and dtypes; full train-step parity and state/recovery coverage; a successful inference graph alone is insufficient |

These rows are orthogonal, not a ranked ladder. Vulkan GPU compute does not require an NPU. A device without an NPU may select B-VULKAN-COMPUTE when its actual driver exposes a compute queue and the required features/operators; otherwise it uses the CPU reference/fallback or another separately verified provider. An accelerator can be used under X-SERIAL without qualifying for X-OVERLAP. A CPU path may be the correctness reference but too slow for a useful workload; a GPU matmul path may be fast but fail admission because softmax, normalization, backward or optimizer operations lack support. A partial accelerator path must state exactly which operations fall back to CPU and include transfer/copy buffers and synchronization in the peak. Vulkan is a low-level compute API and does not supply automatic differentiation or optimizer semantics.

#### Bundle identity and comparison protocol

An eventual immutable profile is:

\[
Profile=(M_{shape},X_{execution},B_{backend},Tokenizer,Precision,Optimizer,DataObjective,CheckpointPolicy)
\]

Every tuple member is versioned and fingerprinted. Reject unsupported tuples rather than silently changing one field at runtime.

For each feasible tuple, record:
- Exact \(P\), parameter tensor inventory and parameterization/initializer manifest.
- Weight, gradient, optimizer, master-weight, logits and saved-activation bytes.
- Forward/backward/update FLOPs and any operations excluded from the estimate.
- Training and inference sequence/context lengths, microbatch, accumulation and active request count.
- Separate Genesis, training, optimizer-update, inference, overlap, checkpoint and migration RAM/storage peaks.
- CPU/Vulkan/NPU per-operator coverage, numerical tolerance, feature/driver identity, transfer/scratch cost, sustained compute evidence, energy and thermal status.
- Tokenizer language-stratum fertility/coverage and the data/objective revision used for any quality proxy.
- Rejected hard gates and the evidence/confidence attached to every accepted estimate.

Compare feasible candidates in separate parameter-, token/data-exposure-, compute-, memory- and quality-matched views as required by document 43 Gate E. Do not reduce all dimensions to one untraceable “device score”.

#### Deterministic selection rule

1. Filter by accepted language/data eligibility and exact tokenizer artifact/runtime.
2. Filter by complete graph correctness and backend/operator coverage for the required training path.
3. Filter by Genesis peak, training peak, inference peak, any required overlap peak, storage and checkpoint/rollback reserve.
4. Filter by sustained compute/thermal/power constraints when those are product requirements; UNKNOWN does not pass a required gate.
5. Rank only the remaining feasible tuples using the approved product priority and maintenance/quality trade-offs.
6. Persist the chosen immutable profile/tokenizer/model fingerprints before allocating vocabulary-dependent model tensors.
7. If no tuple passes, stop Genesis with a typed, recoverable reason. Never fall back to a smaller language contract, different tokenizer IDs or a silently different optimizer.

The model-shape sweep can be completed analytically before physical-device testing, but a profile cannot be marked VERIFIED or assigned a supported device class until the relevant E1–E7 measurements exist. Proposed candidate labels are not support promises.


### 7.2.9 Closed-form MHA reference sensitivity

For the document 43 reference graph, a useful simplified sensitivity form is available once \(V,D,L,F\) are supplied. The assumptions for this expression are explicit:

- Decoder-only MHA with \(D_q=D_{kv}=D\) and \(H_{kv}=H_q\).
- RoPE; no learned absolute-position table.
- Dense two-projection GELU FFN with \(F=r_{ffn}D\).
- Pre-LN with two affine LayerNorms per block and one affine final LayerNorm.
- Tied token embedding/output head.
- No projection biases, adapters or optional learned tensors in this simplified expression.

Under those assumptions:

\[
P_{MHA,tied}\approx VD + L\left(4D^2+2DF+4D\right)+2D
\]

Substituting \(F=r_{ffn}D\):

\[
P_{MHA,tied}\approx VD +(4+2r_{ffn})LD^2 +(4L+2)D
\]

An untied output head adds another \(VD\). Any declared biases, adapters, extra normalization parameters or other learned tensors must be added from the exact ordered tensor inventory; the expression is a sensitivity aid, not the authoritative count.

For the four shape candidates, apply the same formula without changing \(V\) or \(r_{ffn}\) inside the width/depth comparison:

\[
P_{M0}\approx VD_0 +(4+2r_{ffn})L_0D_0^2 +(4L_0+2)D_0
\]

\[
P_{MW}\approx VD_1 +(4+2r_{ffn})L_0D_1^2 +(4L_0+2)D_1
\]

\[
P_{ML}\approx VD_0 +(4+2r_{ffn})L_1D_0^2 +(4L_1+2)D_0
\]

\[
P_{MWL}\approx VD_1 +(4+2r_{ffn})L_1D_1^2 +(4L_1+2)D_1
\]

This makes the scaling effects visible: block matrices scale approximately with \(LD^2\), while the tied embedding/head parameter term scales with \(VD\). When \(V\) is large, the embedding/head can remain a material fraction even for a shallow/narrow proxy. A width increase therefore cannot be evaluated from block parameters alone.

For this MHA reference, the inference KV cache simplifies to:

\[
M_{KV}=2LB_{req}CDs_{KV}
\]

For an explicitly selected GQA ratio \(\rho_{kv}=H_{kv}/H_q\), use:

\[
M_{KV,GQA}=2LB_{req}CD\rho_{kv}s_{KV}
\]

The GQA expression is a separate ablation; it must not be used to understate the MHA reference cache.

The forward matrix-operation estimate also simplifies to:

\[
F_{forward,matmul}\approx
L\left((8+4r_{ffn})B_\mu T_{train}D^2
+4B_\mu D N_{pairs}\right)
+2B_\mu T_{train}DV
\]

where \(N_{pairs}\) is the actual evaluated query-key pair count per sequence. A dense masked implementation may use \(T_{train}^2\); an ideal causal-only calculation uses \(T_{train}(T_{train}+1)/2\). The corresponding first training estimate remains approximately three times forward matrix work for forward plus backward, excluding softmax, normalization, activation, loss, optimizer, rematerialization and runtime inefficiency.

The formulas show why \(V\), \(D\), \(L\), \(F\), context, microbatch and precision must be evaluated together. They do not establish a numeric model profile, sustained tokens/second, a RAM threshold or Android device support.


## 8. G4 deliverables and current disposition

G4 must produce one exact candidate bundle per supported profile, including:
- ordered layer/block graph and architecture fingerprint;
- complete parameter tensor inventory with exact shapes/tie identities;
- forward/loss/backward/optimizer operator graph and per-backend support state;
- initialization/parameterization contract from document 40;
- optimizer/update contract from document 37;
- tokenizer profile/runtime/fingerprint from document 34;
- sequence/document packing and loss contract;
- inference cache/recurrent state lifecycle and reset/carry semantics from document 41;
- analytical memory worksheet across Genesis, training, inference and checkpoint phases;
- rejected-alternative log and an architecture ADR.

**Disposition:** no backbone, layer ratio, dimensions, vocabulary, optimizer, precision, initializer or backend is selected. The symbolic contracts enable exact estimates once the product-language policy, tokenizer rights/runtime and G4 model hypotheses are defined. G1-D remains open and G4 remains blocked. No Android implementation or performance support is claimed.


7. [Android Vulkan implementation and feature requirements](https://source.android.com/docs/core/graphics/implement-vulkan), [Android Vulkan architecture](https://source.android.com/docs/core/graphics/arch-vulkan), [Vulkan compute shaders](https://docs.vulkan.org/guide/latest/compute_shaders.html), and [Android Vulkan reduced-precision guidance](https://developer.android.com/games/optimize/vulkan-reduced-precision). These define Vulkan compute/driver/precision capability checks; they do not establish Argus kernel or device performance support.
8. [Android NNAPI migration guide](https://developer.android.com/ndk/guides/neuralnetworks/migration-guide) and [LiteRT Android hardware acceleration](https://developers.google.com/edge/litert/android/java). NNAPI NDK API deprecation and current inference acceleration guidance; neither source is proof of a complete Argus training backend.

## 9. Primary audited references

1. Vaswani et al., [Attention Is All You Need](https://arxiv.org/abs/1706.03762). Transformer attention/FFN architecture.
2. Ainslie et al., [GQA: Training Generalized Multi-Query Transformer Models from Multi-Head Checkpoints](https://aclanthology.org/2023.emnlp-main.298/). MHA/GQA/MQA semantics and cache trade-offs.
3. Dao and Gu, [Transformers are SSMs: Generalized Models and Efficient Algorithms Through Structured State Space Duality](https://arxiv.org/abs/2405.21060). Mamba-2 SSD and chunk computation.
4. [Official state-spaces/mamba source](https://github.com/state-spaces/mamba/blob/e9594ce1c732d97440f0332fdc43170a2294dbfa/mamba_ssm/modules/mamba2.py), pinned commit e9594ce1c732d97440f0332fdc43170a2294dbfa. Exact Mamba-2 projection split, convolution channels, A/dt/D and state shapes.
5. Peng et al., [RWKV-7 "Goose" with Expressive Dynamic State Evolution](https://openreview.net/pdf?id=ayB1PACN5j), COLM 2025. Delta-rule state and training/backward behavior.
6. [Official RWKV-LM repository](https://github.com/BlinkDL/RWKV-LM), snapshot commit 846b08c1f192f93a72b1064c1637186f0e2c19af, RWKV-v7/rwkv_v7_demo_rnn.py and rwkv_v7_numpy.py. Reference x070 block and inference state structure. This code is not an Android runtime and a checkpoint demo is not proof of random-init training.
