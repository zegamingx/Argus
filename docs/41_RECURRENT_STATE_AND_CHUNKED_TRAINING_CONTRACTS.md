# Recurrent State and Chunked-Training Lifecycle Contracts

**Status:** Cross-family state/lifecycle research; no recurrent backbone selected  
**Last reviewed:** 2026-11-04  
**Scope:** Mamba-2 SSD and RWKV-7 recurrence, chunked training, sequence/document boundaries, backward state, inference cache and exact recovery  
**Dependencies:** Documents 20, 21, 33, 34, 35, 36, 38 and 40  
**Boundary:** Documentary research and analytical contracts only. No Argus implementation, build, workflow, benchmark or physical-device experiment is authorized or claimed.

## 1. Purpose

Documents 38 and 35 compare recurrent/state-space families at the architecture level. This document makes their state lifecycle explicit enough to reconcile with the architecture-neutral checkpoint and generation contracts in document 36.

The critical distinction is between:

1. **Persistent model parameters:** weights, normalization parameters and optimizer-managed tensors.
2. **Inference recurrent state:** request/session-local state used to continue a token stream without replaying all prior tokens.
3. **Training intermediates:** per-sequence/chunk activations, state snapshots and backward workspaces needed to calculate gradients.
4. **Training stream carry state:** optional recurrent state carried between training chunks or optimizer updates when multiple chunks intentionally represent one logical sequence.

These categories have different lifetimes and must not share one generic “model cache” record.

A state whose size does not grow with prior history length is not free, not independent of model dimensions, and not proof of lower peak training memory. Chunked execution changes where intermediate work is performed and how state crosses boundaries; it does not automatically authorize detaching gradients or resetting state at arbitrary batch/window boundaries.

## 2. Mamba-2 SSD state and chunk semantics

### 2.1 Two distinct streaming states per layer

The Mamba-2 block contains a causal depthwise-convolution branch over the SSM input and B/C coefficient channels, followed by the selective SSM recurrence. The official state-spaces/mamba implementation at commit e9594ce1c732d97440f0332fdc43170a2294dbfa declares:

- conv_dim = d_ssm + 2 * ngroups * d_state;
- convolution history shape [batch, conv_dim, d_conv];
- SSM state shape [batch, nheads, headdim, d_state].

In symbolic form, for batch B, SSM channels E, B/C groups G, state size N, convolution width K, number of SSM heads H, and head width P:

\[
M_{conv} \in \mathbb{R}^{B \times (E+2GN) \times K}
\]

\[
S_{ssm} \in \mathbb{R}^{B \times H \times P \times N}
\]

These are **two separate state objects with different update rules**. A checkpoint or inference session that persists only S_ssm but drops M_conv will not reproduce the same next-token computation. Conversely, saving the convolution buffer but not the SSM state is also incomplete.

The official Mamba-2 implementation's single-token step updates the convolution buffer by shifting in the new x/B/C channels, then updates the SSM state using the discretized decay and input-dependent B coefficients. The output is read from that updated SSM state using C, with the D skip term and optional gated normalization applied afterward.

### 2.2 SSD is a chunk algorithm, not token-wise inference disguised as training

Mamba-2's structured state-space duality expresses a restricted recurrent operator as a structured causal mixing matrix. Its chunk algorithm separates:

- **within-chunk local interaction**, computed as a matrix-style local block;
- **inter-chunk carried state**, which summarizes information from preceding chunks;
- **per-position carried contribution**, added back to local outputs;
- **true outgoing boundary state**, consumed by the next chunk.

For a chunk spanning positions s through e, the conceptual equations are:

\[
S_e = \left(\prod_{k=s}^{e} a_k\right)S_{s-1} + S_e^{local}
\]

\[
y_i = y_i^{local} +
\left(\prod_{k=s}^{i} a_k\right)S_{s-1}C_i,
\quad s \le i \le e
\]

The local zero-incoming-state results are not the final output when the incoming boundary state is nonzero. Correctness therefore requires both:
- output agreement across recurrent and chunked forms; and
- exact agreement of the final outgoing state that the next chunk consumes.

A compact reference listing may materialize a dense matrix across chunk boundaries for clarity. That should not be confused with the asymptotically linear chronological boundary-passing formulation. Production kernels may use specialized scans and tiled matrix products; their scratch and saved-intermediate policy must be audited separately.

### 2.3 Training backward and chunk boundaries

The chunk boundary is a computational partition, not automatically a gradient truncation boundary.

For exact full-sequence BPTT semantics:
- state dependencies across chunks must remain in the backward graph or be recomputed with mathematically equivalent saved boundary information;
- the gradient with respect to the incoming state of a chunk must be propagated to the preceding chunk;
- any activation/state rematerialization must reproduce the same forward and gradient values within declared numeric tolerance;
- a chunk must not silently start from zero when it represents continuation of a prior chunk.

Detaching the carried state at every chunk or optimizer update changes the training objective to truncated BPTT. That can be an intentional resource policy, but it is a different training-semantics configuration and must be recorded explicitly, not hidden as an implementation optimization.

### 2.4 Sequence/document boundary reset

The official Mamba-2 module forwards seq_idx to both the causal-convolution operation and the chunk scan, and also supports a variable-length cu_seqlens path for appropriate inference/state handling. This is relevant because document packing affects **both** stateful components, not only the SSM scan.

Argus boundary policy must state, for every data mode:

- whether a new document starts with zero convolution and SSM state;
- whether adjacent chunks from the same document carry both states;
- whether independent packed documents use reset/sequence identifiers through both convolution and scan;
- whether a variable-length batch uses explicit cumulative sequence lengths;
- whether a truncated training window deliberately resets or carries state.

A Transformer-style attention mask alone is not a correct Mamba-2 document boundary mechanism. If state is not reset, a later document can receive information from earlier unrelated text through convolution and SSM state.

## 3. RWKV-7 state and chunk semantics

### 3.1 WKV matrix state is only one part of model state

RWKV-7 uses a generalized delta-rule recurrence. A representative paper equation is:

\[
S_t = S_{t-1}\left(\operatorname{diag}(w_t) + z_t^\top b_t\right) + v_t^\top k_t
\]

where w_t is a data-dependent vector decay and z_t,b_t define a rank-one state-transition modification. The paper's Figure 2 explicitly illustrates a **64×64 matrix state per head** in the released configuration. State memory is independent of the number of historical tokens for a fixed model, but depends on the actual number of layers, heads and per-head key/value dimensions.

The COLM 2025 paper's reference model loop also carries additional shift buffers:
- timemix_shiftstate and timemix_wkvstate for TimeMix;
- channelmix_shiftstate for ChannelMix.

The TimeMix shift is a learned per-channel interpolation between the current and previous token. Therefore a complete continuation state includes the WKV matrix **and** the model's token-shift state. Saving only WKV is incomplete.

### 3.2 Parallelized training is not equivalent to a stateless chunk

The paper presents the recurrence in a parallel form and a naive forward/backward reference. The naive reference allocates a state cache of shape [B, T+1, H, N, N] and stores WKV state at every token for backward. The optimized kernel reduces this storage by saving WKV state at intervals (the paper's memory accounting uses a state snapshot every 16 timesteps for head size 64), together with the relevant forward variables and gradients.

This establishes three different footprints:

1. **Persistent inference carry:** final WKV matrix plus shift buffers per active sequence/layer.
2. **Training forward/backward intermediates:** WKV snapshots, projected inputs, gates, gradients and temporary state required by the kernel.
3. **Checkpoint-at-update-boundary carry:** only the state needed to continue a deliberately stateful data stream after the committed update, not every internal activation snapshot from the completed forward/backward pass.

The paper's H100 kernel comparison uses batch 8, head dimension 64 and model dimension 4096 (64 WKV heads). In that setup the authors count 18 BF16 variable-equivalents for RWKV-7 versus 10 for FlashAttention v3; the FP32 RWKV-7 kernel uses 24. The model authors used the FP32 kernel for World model training despite the faster BF16 variant. These are specific GPU-kernel accounting results, not mobile requirements, but they invalidate the assumption that bounded inference state necessarily means lower training memory.

### 3.3 Chunked prefill and training

The RWKV-7 paper states that prefill can be performed in chunks, with memory growing linearly with chunk size, allowing a speed/memory trade-off. The chunked recurrence must return the correct final WKV state, and the TimeMix/ChannelMix shift buffers must be consistent at every boundary.

For training, the selected algorithm must specify:
- the exact chunk-parallel transformation and state-transition equations;
- the chunk size and any state snapshots saved for backward;
- whether state gradients cross chunk boundaries;
- whether state is recomputed or stored in backward;
- whether sequence boundaries reset WKV and token-shift buffers together;
- whether a chunk is merely a kernel tile or a deliberate truncated-BPTT segment.

The reference paper's recurrence and backward code is an algorithmic reference. It does not prove that a particular Android CPU/GPU implementation exists or has acceptable memory, throughput or numerical behavior.

### 3.4 Sequence/document boundary reset

RWKV token-shift and WKV state both encode prior-token information. When independent documents are packed, Argus must reset **all** state components at the new document boundary:
- TimeMix shift state;
- WKV matrix state;
- ChannelMix shift state;
- any implementation-specific recurrent/gating carry.

Resetting only the WKV matrix while retaining a previous token-shift vector leaks information into the next document. Carrying all states across unrelated documents also changes the causal-training sample definition and creates unintended cross-document influence.

The exact reset/carry behavior for a stream of consecutive windows from the same document is a separate configuration from document packing. It must be declared in the immutable training session manifest.

## 4. What must be checkpointed — and what must not

### 4.1 Three lifecycle categories

| State category | Examples | Persistence rule |
|---|---|---|
| Model-generation state | Model parameters, architecture and initialization manifests, tokenizer fingerprint | Durable and immutable after a generation commit; training writes an isolated candidate |
| Optimizer/training transaction state | Optimizer moments, scheduler/scaler, RNG, sampler/data cursor, committed update sequence | Atomic checkpoint with candidate model weights |
| Recurrent stream carry state (conditional) | Mamba-2 conv+SSM boundary state; RWKV-7 WKV+TimeMix-shift+ChannelMix-shift state | Persist only when the chosen training data contract carries an identified logical stream across a committed checkpoint boundary; otherwise reset at the declared independent-sequence boundary |
| In-flight backward state | Chunk snapshots, per-token WKV snapshots, saved activations, temporary scan matrices and gradient accumulators | Normally transient to a forward/backward/update transaction. Do not serialize it as a model checkpoint by default. If mid-transaction recovery is ever required, define a separate complete graph/recompute contract; it is not implied by normal checkpointing |
| Inference request state | Active conversation's recurrent context, request/token cursor, runtime buffers | Session-scoped and bound to exact model-generation digest; not part of optimizer checkpoint or model-generation identity. Can be discarded and replayed from request history if allowed and bounded |

### 4.2 Exact-resume state identity

When recurrent carry is persisted, its manifest must include:

- model instance ID and exact candidate-generation/parameter digest;
- architecture family/version, block graph and state-layout fingerprint;
- training-session ID and objective/sequence-packing semantics;
- dataset manifest digest, shard/sample/document ID and exact next token/chunk cursor;
- sequence identity and whether the next segment is a continuation or a fresh document;
- chunk size and boundary-reset/carry policy;
- state component name and version (for example Mamba-2 conv, Mamba-2 SSM, RWKV-7 WKV, TimeMix shift or ChannelMix shift);
- tensor shape, dtype, layout/strides, device-independent serialized representation and checksum;
- the optimizer update sequence and state-production position relative to the last update;
- precision/scaler policy and any state-cast policy;
- checksum and atomic publication transaction ID.

**Parameter-version subtlety:** in a stateful truncated-BPTT training stream, a recurrent carry produced during the forward pass can be consumed by the next segment after an optimizer update. Recomputing that carry from the updated weights is not necessarily equivalent to the uninterrupted run. The checkpoint must preserve the exact carried tensor produced by the original schedule, or the session must define a replay/recomputation policy that is mathematically equivalent. Record which committed update/parameter snapshot produced the state and which update consumes it.

If a checkpoint is committed only at independent document boundaries and the next document starts from the declared zero state, no recurrent stream state needs to be persisted. This is preferable when compatible with the intended objective because it simplifies exact recovery and prevents accidental state leakage.

### 4.3 Atomic checkpoint rule

For stateful training, the following must be one atomic publication unit (or one recoverable transaction with a single commit point):

- candidate model weights and generation digest;
- optimizer state and committed optimizer update index;
- scheduler and loss-scaler state;
- RNG/PRNG and sampler state;
- durable data cursor and sequence/chunk boundary;
- all recurrent carry components required by the next segment;
- state fingerprints/checksums and state-production update metadata.

The system must never publish a later data cursor without the recurrent state corresponding to that cursor. It must never publish recurrent state for a chunk while leaving model/optimizer/RNG at an earlier committed update. On crash or checksum mismatch, restore the last complete transaction; otherwise discard the incomplete candidate transaction and replay from the last commit.

## 5. Memory accounting for recurrent state

The following formulas are inventory aids, not a complete training peak estimate.

### Mamba-2 streaming carry per layer

For the official implementation's shape definitions, the byte count is approximately:

\[
M_{M2,carry} =
B(E+2GN)K\,s_{conv} + BH P N\,s_{ssm}
\]

where E=d_ssm, G=ngroups, N=d_state, K=d_conv, H=nheads, P=headdim, and s is bytes per stored scalar. This accounts for both causal-convolution history and SSM state only. It excludes model parameters, optimizer state, full-sequence activations, chunk matrices, backward snapshots, output/logit tensors and workspace.

### RWKV-7 inference carry per layer

For H WKV heads, key dimension Nk, value dimension Nv, plus shift buffers M_shift:

\[
M_{RWKV7,carry} = B H N_v N_k\,s_{wkv} + M_{shift}
\]

The paper's equal key/value head example has a 64×64 WKV matrix per head; Argus must use the actual graph dimensions and add TimeMix/ChannelMix shift buffers. Training memory is substantially larger than this final carry inventory due to backward needs and state snapshots.

### Comparison rule

Always report separately:
- persistent weights and optimizer state;
- inference carry state per request;
- training forward/backward saved tensors;
- per-chunk local matrices and scratch;
- checkpoint serialization/publication overlap;
- batch-size and concurrent-request multipliers.

Do not compare a Transformer KV cache to only the Mamba/RWKV matrix state and call that a training-memory comparison. Compare complete inference and training paths separately.

## 6. Mandatory state-correctness and recovery checks

Future implementation verification must cover:

1. **Recurrent/chunk parity:** same inputs and nonzero initial carry produce matching outputs and final state in recurrent and chunked implementations.
2. **Gradient parity:** gradients for inputs, parameters and incoming-state tensors match the reference within declared dtype tolerances.
3. **Document reset:** after an independent-document boundary, all state components are zero/reset or replaced by an explicitly defined initial state; the next document's outputs must not depend on the previous document.
4. **Same-document continuation:** splitting one document into chunks and carrying state reproduces full-sequence outputs and final state.
5. **No accidental detachment:** compare gradient flow through chunk boundaries against the full-sequence reference; test deliberate truncated-BPTT as a distinct session mode.
6. **Mamba-2 dual-buffer recovery:** restore convolution and SSM state independently and together; prove neither can be omitted.
7. **RWKV-7 complete-state recovery:** restore WKV, TimeMix shift and ChannelMix shift; prove partial restoration fails the equality test.
8. **Checkpoint cursor atomicity:** crash-inject before/after model/optimizer/state/cursor publication; no skipped or duplicated committed token/chunk/update is allowed.
9. **Parameter-version consistency:** verify carry state is exactly the one produced by the uninterrupted training schedule relative to optimizer updates; don't silently recompute it using newer weights.
10. **Dtype/layout checks:** validate state quantization, cast behavior, strides, serialization and checksums for each supported backend.
11. **Memory accounting:** report full peak training memory and recurrent carry separately; include state snapshots, workspace, logits and checkpoint overlap.
12. **Bounded cancellation:** pause/stop only at a declared transaction boundary or preserve the entire uncommitted microbatch/chunk state necessary for exact continuation.

These are post-implementation requirements, not tests already executed.

## 7. Current position and G4 implications

- No Mamba-2, RWKV-7 or hybrid architecture is selected.
- Bounded inference-state complexity is a real advantage worth evaluating for long contexts, but it does not establish lower total training memory or Android training feasibility.
- Mamba-2 must be evaluated as a block with both convolution and SSM state, its SSD chunk algorithm, and explicit sequence-reset semantics.
- RWKV-7 must be evaluated with WKV matrix state and both token-shift buffers; WKV-only accounting is incomplete.
- A chunk boundary is not automatically a sequence/document boundary, and neither is automatically an optimizer-update boundary.
- Full-sequence BPTT, exact chunked BPTT and truncated BPTT are distinct training semantics. Device adaptation may choose an approved memory-bounded mode, but may not silently switch between them inside a session.
- G4 must choose the recurrent-state carry policy, chunk size, backward/rematerialization policy, reset mask/sequence-ID representation, checkpoint commit boundary and inference-session lifecycle together with the model configuration.
- Documents 36 and 40 provide the architecture-neutral transaction and initialization contracts; this document supplies the Mamba-2/RWKV-7 state-specific requirements.
- No physical Android support, native kernel, random-initialization convergence or quality claim is established.

## 8. Primary references and audited implementation source

1. Dao and Gu, ICML 2024, [Transformers are SSMs: Generalized Models and Efficient Algorithms Through Structured State Space Duality](https://arxiv.org/abs/2405.21060). Mamba-2/SSD recurrence, structured causal matrix, chunk decomposition and state-carry formulation.
2. [Official state-spaces/mamba repository](https://github.com/state-spaces/mamba), mamba_ssm/modules/mamba2.py, commit e9594ce1c732d97440f0332fdc43170a2294dbfa, audited 2026-11-04. Exact conv_dim, inference state shapes, causal-convolution/SSM state update, seq_idx and cu_seqlens forwarding.
3. Peng et al., COLM 2025, [RWKV-7 "Goose" with Expressive Dynamic State Evolution](https://openreview.net/pdf?id=ayB1PACN5j). Generalized delta-rule, matrix-valued WKV state, token-shift state, reference backward and H100 training memory/speed accounting.
4. [RWKV-LM training and inference reference](https://github.com/RWKV/RWKV-LM), repository snapshot reviewed 2026-11-04. Additional architecture/training implementation context; not an Android runtime.
