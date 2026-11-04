# Architecture-Family Tensor and Operator Inventory

**Status:** Symbolic architecture inventory; no dimensions or backbone selected  
**Last reviewed:** 2026-11-04  
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

## 9. Primary audited references

1. Vaswani et al., [Attention Is All You Need](https://arxiv.org/abs/1706.03762). Transformer attention/FFN architecture.
2. Ainslie et al., [GQA: Training Generalized Multi-Query Transformer Models from Multi-Head Checkpoints](https://aclanthology.org/2023.emnlp-main.298/). MHA/GQA/MQA semantics and cache trade-offs.
3. Dao and Gu, [Transformers are SSMs: Generalized Models and Efficient Algorithms Through Structured State Space Duality](https://arxiv.org/abs/2405.21060). Mamba-2 SSD and chunk computation.
4. [Official state-spaces/mamba source](https://github.com/state-spaces/mamba/blob/e9594ce1c732d97440f0332fdc43170a2294dbfa/mamba_ssm/modules/mamba2.py), pinned commit e9594ce1c732d97440f0332fdc43170a2294dbfa. Exact Mamba-2 projection split, convolution channels, A/dt/D and state shapes.
5. Peng et al., [RWKV-7 "Goose" with Expressive Dynamic State Evolution](https://openreview.net/pdf?id=ayB1PACN5j), COLM 2025. Delta-rule state and training/backward behavior.
6. [Official RWKV-LM repository](https://github.com/BlinkDL/RWKV-LM), snapshot commit 846b08c1f192f93a72b1064c1637186f0e2c19af, RWKV-v7/rwkv_v7_demo_rnn.py and rwkv_v7_numpy.py. Reference x070 block and inference state structure. This code is not an Android runtime and a checkpoint demo is not proof of random-init training.
