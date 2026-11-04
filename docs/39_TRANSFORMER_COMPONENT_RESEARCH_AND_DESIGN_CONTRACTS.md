# Transformer Component Research and Pre-G4 Contracts

**Status:** Comparative evidence synthesis; no component selected for Argus  
**Last reviewed:** 2026-11-04  
**Scope:** Attention semantics, positional representation, feed-forward blocks, normalization/residual placement, depth/width and token embeddings/output head  
**Dependencies:** Documents 34, 35, 38 and 40  
**Boundary:** Published research and analytical contracts only. No Argus implementation, build, workflow, device test or benchmark is authorized or claimed.

## 1. Purpose and decision boundary

The four G2 backbone families remain the bounded research set. This document decomposes the high-impact Transformer components and records which findings transfer, which do not, and what a later matched comparison must control.

It does **not** select a Transformer configuration or make the Transformer family the G4 winner. Several mechanisms in this document are Transformer-specific; recurrent and state-space families require their own native contracts and must not be forced into a Transformer block template.

The main requirement is to avoid conflating three different questions:

1. **Mathematical semantics:** what transformation the model actually computes.
2. **Parameter/resource allocation:** how the choice changes weights, KV state, activations, logits, memory traffic and checkpoint size.
3. **Training dynamics:** whether a design is stable and learns well from random initialization at the target scale and data budget.

A result about inference KV-cache size does not prove lower training peak memory. A pretrained-checkpoint uptraining result does not prove the same choice is optimal from scratch. A large-GPU result does not establish Android feasibility.

## 2. Component decision summary

| Component | Main candidates | Strongest relevant evidence | Transfer limit | Argus disposition |
|---|---|---|---|---|
| Attention semantics | MHA, GQA, MQA | GQA paper shows an uptraining path from MHA checkpoints; MobileLLM directly studies GQA in 125M/350M models trained from scratch | GQA uptraining evidence is not scratch evidence; MobileLLM ran on 32 A100 GPUs, not phones | **DEFER** |
| Position representation | RoPE, ALiBi, learned absolute positions; recurrent state for SSM/RWKV | RoPE encodes relative position through rotations; ALiBi uses a distance-proportional score bias and reports one short-train/long-test result | Neither method guarantees arbitrary context extrapolation or mobile training efficiency; recurrence uses a different mechanism | **DEFER** |
| Dense FFN | GELU/ReLU MLP, SwiGLU/GEGLU | Shazeer reports gated FFN variants can improve on ReLU/GELU in T5; MobileLLM finds SwiGLU useful for 125M/350M | More projections must be parameter-matched; task/model/scale results are not universal | **DEFER** |
| Normalization operator | LayerNorm, RMSNorm, GroupNorm where architecture requires it | RMSNorm paper reports comparable performance and lower runtime in its tested models; Mamba-2 tensor-parallel implementation uses GroupNorm | Norm operator and placement are separate variables; no operator is established as the Argus winner | **DEFER** |
| Normalization placement / residual topology | Pre-LN, Post-LN, Peri-LN; architecture-native residual/state updates | Pre-LN improves initialization gradient behavior vs Post-LN in Xiong et al.; Peri-LN reports stability through 3.2B in its study | Large-model studies do not select an Argus small/mobile design; topology and norm algorithm must be ablated separately | **DEFER** |
| Depth / width | Deep-thin vs shallow-wide; family-specific scaling | MobileLLM finds deep-thin favorable for tested sub-billion models | 32×A100 training and fixed model/data; do not hardcode layer counts for Android | **DEFER** |
| Embedding/output head | Tied/shared vs untied; GQA/head dimensions | MobileLLM reports meaningful embedding savings in 125M/350M models | Tokenizer vocabulary is still open; tying affects capacity and the output projection, not just storage | **DEFER — G1 dependency** |

The existing one-primary-disposition matrix remains authoritative in document 35. This document adds evidence and acceptance contracts; it does not change G3 dispositions.

## 3. Attention semantics and KV-state accounting

### 3.1 Multi-head attention (MHA)

MHA assigns each query head its own key and value head. In a decoder-only model with (H_q) query heads, (H_{kv}=H_q).

MHA is the mandatory semantic reference for any Transformer comparison because it is the clearest baseline against which GQA/MQA can be defined. It does not follow that MHA is the lowest-memory or best inference configuration.

### 3.2 Multi-query attention (MQA)

MQA uses one shared key head and one shared value head for all query heads: (H_{kv}=1).

Its primary systems motivation is reducing the volume of key/value tensors loaded and retained during autoregressive decoding. This reduces KV-cache storage and memory traffic, but does not remove the query-head attention operation or guarantee equivalent model quality. The GQA paper explicitly notes that MQA can degrade quality and may be undesirable as a separate model design.

### 3.3 Grouped-query attention (GQA)

GQA uses an intermediate number of KV heads, with multiple query heads sharing each KV head:

[
1 < H_{kv} < H_q,qquad H_q mod H_{kv}=0.
]

For a batch (B), retained context (T), head dimension (d_h), and bytes per stored scalar (s), the logical KV-cache size is approximately:

[
M_{KV}=2,L,B,T,H_{kv},d_h,s
]

where (L) is the number of attention layers and the factor 2 accounts for keys and values. This is a cache estimate, not total inference memory or training activation memory. For otherwise identical head dimensions, the cache term scales linearly with (H_{kv}); reducing KV heads does not reduce every attention FLOP by the same ratio.

### 3.4 What the papers actually establish

- Ainslie et al. (EMNLP 2023) propose converting an existing MHA checkpoint by mean-pooling K/V projection heads, then uptraining the converted model with 5% of original pretraining compute. They report GQA quality close to MHA with speed comparable to MQA. This is valuable evidence for a **checkpoint-conversion/uptraining** path, not proof that GQA is automatically optimal when training Argus from random initialization.
- MobileLLM (ICML 2024) studies small-model design and does train from scratch. Its experiments report that reducing KV heads from 16 to 4 gave comparable accuracy for a 125M model and about a 0.2-point drop for a 350M model, while reducing model size by nearly 10%; adjusting embedding dimension while adopting GQA recovered further accuracy in the 125M setting. Its training used 32 A100 GPUs and 1T tokens; it is not phone training.

### 3.5 Required Argus comparison

Keep MHA as the control and compare at least one GQA ratio and MQA only when the resource benefit justifies its quality risk. Pin (H_q), (H_{kv}), (d_h), width, layer count, parameter count, context and precision in each record. Report:

- parameter count and K/V projection dimensions;
- KV-cache bytes per layer and total session;
- prefill and decode latency separately;
- attention forward/backward peak memory and scratch;
- numerical parity against the MHA semantic reference;
- validation loss and relevant copy/retrieval/in-context tests;
- actual backend and fallback path.

No MHA/GQA/MQA selection is made here.

## 4. Positional representation

Position representation must be compared only within a fixed tokenizer/profile and sequence-packing contract. The number of tokens per document is tokenizer-dependent; a positional result at a fixed token length is not automatically a result at a fixed byte or document length.

### 4.1 RoPE

RoPE applies a position-dependent rotation to query and key vectors. The attention score thereby carries relative-position information through the difference between positions. The original RoFormer paper describes decaying inter-token dependency with greater relative distance and flexibility across sequence lengths.

**Implications:** no learned absolute-position lookup table is required, but the rotation frequencies, position indexing, precision and any context-extension/scaling method become part of the model configuration. RoPE does not guarantee stable extrapolation to arbitrary lengths. Press et al. (ICLR 2022) directly compared rotary with ALiBi and sinusoidal positions in their WikiText-103 setup: rotary improved extrapolation over sinusoidal, but only to roughly 100–200 tokens beyond the trained sequence length in their tested 512/1,024-token settings, and their implementation ran more slowly. This is a study-specific result, not a statement about every optimized RoPE kernel or later scaling method.

### 4.2 ALiBi

ALiBi adds a distance-proportional bias to query-key attention scores rather than adding a positional embedding to token representations. Press et al. report a 1.3B model trained at length 1,024 that extrapolated to length 2,048 with the same perplexity as a sinusoidal-position model trained at 2,048, while using 11% less memory and training 11% faster in that study.

This is a specific experimental result, not a universal advantage over RoPE. ALiBi uses fixed, head-specific slopes; its attention-score bias is added before softmax and can be fused into a causal mask/kernel. However, the paper notes that materializing a separate bias mask per head can enlarge the mask tensor and consumed up to 100 MB extra memory in some of its experiments. The shorter training sequence can save substantially more memory in its demonstrated case, but an Argus implementation must measure both terms and must not claim zero overhead merely because the bias has no learned parameters. The slope/head-count rule, mask representation and kernel fusion are part of the configuration.

### 4.3 Learned absolute position embeddings

A learned table associates position indices with trainable vectors. Its parameter cost grows with configured maximum positions and hidden width, and its direct lookup contract is tied to the supported maximum length. Extending the table requires a defined initialization/training policy and is not equivalent to changing an inference setting.

### 4.4 SSM and recurrent models

Mamba/Mamba-2 and RWKV-7 model sequence order through their state-transition/recurrence mechanisms and, in the cited comparison configurations, use no explicit position embedding. This does not imply that position is absent from the computation: sequence order is carried by the recurrence and reset/chunk semantics. Do not insert RoPE or ALiBi into an SSM/recurrent family unless that hybrid variant is explicitly defined and compared.

### 4.5 Required Argus comparison

Compare RoPE and ALiBi only after the Transformer control is numerically correct. Include learned absolute positions only if their table cost and bounded context are justified. Test:

- train-length and longer-than-train-length evaluation separately;
- positions at packed-document boundaries and sequence resets;
- correct position continuation vs reset for each data mode;
- numerical stability in selected precision;
- materialized score/bias-mask memory versus fused/tiled bias application, score-kernel cost and prefill/decode behavior;
- retrieval/copying tasks as well as ordinary validation perplexity.

No positional method or context-extension recipe is selected.

## 5. Feed-forward network (FFN) and gated variants

### 5.1 Conventional dense FFN

A conventional position-wise FFN has two learned projections around an activation, for example:

[
operatorname{FFN}(x)=W_2,phi(W_1x).
]

Its compute scales with sequence positions and the intermediate width. GELU and ReLU are possible activation choices; the FFN projection dimensions are part of the parameter and activation budget.

### 5.2 SwiGLU / GEGLU

A gated FFN adds a second input projection and combines the gate and value branches element-wise before the output projection. A representative SwiGLU form is:

[
operatorname{SwiGLU}(x)=W_oleft(operatorname{SiLU}(W_gx)odot W_vxight).
]

It has three principal projection matrices instead of two. To keep parameter count comparable, the intermediate width must be reduced (a common first analytical approximation is about two-thirds of the ungated FFN width, before biases and exact rounding are included). Comparing the same (d_{ff}) across GELU and SwiGLU is therefore not parameter-matched.

### 5.3 Evidence and limits

- Shazeer (2020) reports that several GLU variants in Transformer FFN sublayers improve quality over the usual ReLU/GELU variants in the tested T5 transfer-learning setup.
- MobileLLM tests SwiGLU in small models and reports an average zero-shot reasoning score increase from 42.6 to 43.9 for its 125M model when changing the baseline FFN to SwiGLU. Its results are from the MobileLLM training setup (32 A100 GPUs), not Android.
- The NVIDIA Mamba-2-Hybrid study's MLP layers are a specific hybrid composition; its reported layer percentages do not select an Argus FFN width or activation.

### 5.4 Required Argus comparison

Start with a dense FFN control and compare GELU vs parameter-matched SwiGLU. Hold total model parameters and training tokens constant in one view, and compute budget constant in a separate view. Report active parameters, parameter bytes, forward/backward activation memory, throughput, gradient norms, loss stability and validation quality.

MoE is outside the initial G2 shortlist and is not introduced by this document. Its expert weights, routing, load balance and memory residency are not justified for the first Argus model without a product requirement and separate evidence.

## 6. Normalization operator and placement are separate decisions

### 6.1 Normalization operator

- **LayerNorm** subtracts the feature mean and divides by a standard-deviation term before learned affine scaling.
- **RMSNorm** divides by the root mean square and applies learned scaling without subtracting the mean. Zhang and Sennrich report comparable results to LayerNorm and 7–64% lower runtime on several model/task combinations in their experiments. The result is workload-specific; it does not establish an Android speedup.
- **GroupNorm** computes statistics over declared channel/groups. The NVIDIA Mamba-2 implementation uses GroupNorm in its tensor-parallel path; the authors report matching validation loss to full LayerNorm when the group size is sufficiently large (greater than 256 in their experience). This is an implementation-specific SSM/tensor-parallel detail, not a default Transformer norm.

### 6.2 Normalization placement and residual topology

The normalization operator does not determine where it is applied:

- **Post-LN:** normalization is applied after the residual addition.
- **Pre-LN:** normalization is applied inside the residual block before the sublayer.
- **Peri-LN:** a peripheral placement strategy around sublayers, studied as a third arrangement in 2025.

Xiong et al. (ICML 2020) analyze gradient behavior at initialization and show that Post-LN can produce large gradients near the output in deep Transformers, motivating learning-rate warmup. Their Pre-LN analysis finds better-behaved initialization gradients and reports comparable results without the same warmup requirement in their studied tasks.

Kim et al. (ICML 2025) analyze Peri-LN and report more balanced activation variance, steadier gradient flow and convergence stability for Transformers up to 3.2B parameters. This makes Peri-LN a legitimate comparison candidate; it does not automatically win at Argus's unknown scale or device budget.

A residual topology is a separate computational contract. Do not change Pre/Post/Peri placement, norm type, residual scaling and initialization simultaneously and then attribute the outcome to one variable.

### 6.3 Required Argus comparison

Treat operator and placement as a small factorial or staged ablation:

1. establish a stable Pre-LN + LayerNorm CPU reference as a **comparison control only**;
2. compare RMSNorm vs LayerNorm while holding placement fixed;
3. compare Pre-LN vs the best-supported alternative placement while holding norm operator fixed;
4. include Peri-LN only if the exact paper definition is transcribed and a small-scale stability hypothesis justifies its additional variant;
5. compare architecture-native normalization such as Mamba-2 GroupNorm only within that architecture's correct reference.

Measure initialization activation/gradient variance by layer, finite-value incidence, loss curves across multiple seeds, warmup sensitivity, time per update and peak temporary allocation. The control is not an accepted implementation choice.

## 7. Depth, width, embeddings and output head

MobileLLM reports that deeper/thinner architectures can perform better than shallower/wider variants at similar 125M/350M parameter counts. It also reports that embedding sharing reduces the 135M configuration to 119M parameters (roughly 11.8% fewer), while adding depth after sharing can recover or improve the measured score in its experiment.

This creates a relevant allocation trade-off for small models:

- input/output embedding matrices each scale as (V 	imes d), where (V) is vocabulary size and (d) is embedding width;
- tying them can remove one (V 	imes d) matrix from the parameter inventory;
- the output projection still computes vocabulary logits, so tying does not remove vocabulary-sized logit cost;
- saved parameter storage can be reallocated to depth or width, but that changes compute and optimization dynamics;
- tokenizer vocabulary selection and hidden width must be budgeted jointly.

**G1 dependency:** no final embedding/head dimensions, tied/untied decision, parameter count or vocabulary tier can be frozen before the tokenizer profile portfolio and G4 model bundle are approved. Do not borrow MobileLLM's layer counts or vocabulary to Argus.

## 8. Matched comparison protocol

For every component ablation, the comparison manifest must pin:

- backbone family, graph/version, layer count and exact tensor shapes;
- tokenizer ID/fingerprint, vocabulary and special-token inventory;
- training/validation data manifest, language/domain mix, order and deduplication;
- sequence length in tokens and bytes, packing, resets and loss masks;
- initialization method, seed set, residual/norm/position semantics;
- optimizer ID/configuration, precision and state dtype, effective tokens per update, clipping and schedule;
- parameter count, estimated FLOPs and actual tokens processed;
- persistent weights/gradient/optimizer memory, activations, logits, scratch/workspaces, runtime buffers and checkpoint overlap;
- quality measures relevant to both next-token modeling and retrieval/copying/in-context behavior.

No one matching method is sufficient. Report parameter-matched, token/data-exposure-matched, compute-matched and memory-matched results separately. Record where candidates cannot be compared under equal compute because their operators have different implementation maturity.

## 9. Current position and required outputs

- **No component winner is selected.** G3 continues to defer attention variant, positional representation, FFN, normalization, residual topology, initialization and depth/width.
- **MHA and conventional dense FFN** are semantic controls for Transformer comparisons, not accepted product selections.
- **GQA and SwiGLU** are serious small-model candidates because MobileLLM tests them in a from-scratch sub-billion model design, but its 32×A100 / 1T-token setup does not establish Android feasibility.
- **RMSNorm, RoPE and Pre-LN** are evidence-backed candidates, not defaults by popularity. ALiBi and Peri-LN are also retained for controlled comparison where the model profile justifies them.
- Mamba-2 GroupNorm and recurrent/SSM state semantics remain family-specific; they must not be represented as interchangeable Transformer normalization/position components.
- G4 must select one explicit component contract for each retained model profile and record the evidence/risks for every deferred alternative.
- Physical Android measurements, numerical parity and convergence verification remain post-implementation E1–E7/G1-V obligations. They are not reasons to leave this documentary comparison unfinished.

## 10. Primary references

1. Liu et al., ICML 2024, [MobileLLM: Optimizing Sub-billion Parameter Language Models for On-Device Use Cases](https://proceedings.mlr.press/v235/liu24ce.html). From-scratch 125M/350M small-model ablations for deep/thin, SwiGLU, embedding sharing and GQA; training used 32 A100 GPUs.
2. Ainslie et al., EMNLP 2023, [GQA: Training Generalized Multi-Query Transformer Models from Multi-Head Checkpoints](https://aclanthology.org/2023.emnlp-main.298/). GQA/MQA inference motivation and 5%-compute uptraining conversion method; not scratch-GQA proof.
3. Su et al., 2021, [RoFormer: Enhanced Transformer with Rotary Position Embedding](https://arxiv.org/abs/2104.09864). Rotation-based positional representation and relative-position behavior.
4. Press, Smith and Lewis, ICLR 2022, [Train Short, Test Long: Attention with Linear Biases Enables Input Length Extrapolation](https://openreview.net/pdf?id=R8sQPpGCv0). ALiBi and the reported 1,024-to-2,048 length extrapolation setup.
5. Shazeer, 2020, [GLU Variants Improve Transformer](https://arxiv.org/abs/2002.05202). Gated FFN variants including SwiGLU/GEGLU.
6. Zhang and Sennrich, NeurIPS 2019, [Root Mean Square Layer Normalization](https://proceedings.neurips.cc/paper/2019/hash/1e8a19426224ca89e83cef47f1e7f53b-Abstract.html). RMSNorm formulation and task-specific efficiency comparisons.
7. Xiong et al., ICML 2020, [On Layer Normalization in the Transformer Architecture](https://proceedings.mlr.press/v119/xiong20b.html). Pre-LN/Post-LN initialization gradients and warmup implications.
8. Kim et al., ICML 2025, [Peri-LN: Revisiting Normalization Layer in the Transformer Architecture](https://proceedings.mlr.press/v267/kim25u.html). Peri-LN analysis and experiments up to 3.2B parameters.
9. Dao and Gu, ICML 2024, [Transformers are SSMs: Generalized Models and Efficient Algorithms Through Structured State Space Duality](https://proceedings.mlr.press/v235/dao24a.html). Mamba-2 normalization/operator context; see document 38 for the full cross-family evidence and GPU benchmark transfer limits.
