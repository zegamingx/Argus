# Initialization and Width-Scaling Research for Local Genesis

**Status:** Comparative evidence synthesis; no initializer or parameterization selected  
**Last reviewed:** 2026-11-04  
**Scope:** Random-initialization contracts, residual/depth scaling, Transformer initialization research, width parameterization and small-proxy hyperparameter transfer  
**Dependencies:** Documents 20, 21, 33, 35, 36, 38 and 39  
**Boundary:** Documentary research and analytical contracts only. No Argus implementation, build, workflow, benchmark or physical-device experiment is authorized or claimed.

## 1. Why initialization is a first-class Genesis decision

Argus is intended to create its neural parameters locally rather than load an externally pretrained foundation. Initialization is therefore not a one-line framework default. It is part of the model-generation identity and must be defined before the first tensor is published.

The initializer determines the initial distribution of embeddings, attention/SSM/recurrent projections, FFN matrices, normalization scales, output readout and any architecture-specific state. These choices interact with:

- residual topology and normalization placement;
- depth, hidden width, attention head dimensions and FFN width;
- tied versus untied input/output embeddings;
- tokenizer vocabulary and special-token inventory;
- activation functions and architecture-specific recurrent/state operators;
- optimizer, learning-rate schedule, precision and gradient clipping;
- the deterministic local instance/generation seed policy.

A stable forward pass is not sufficient. A candidate must also produce usable backward signal, finite early updates and reproducible model-generation metadata.

**No initialization method is selected here.** G3 continues to assign the initializer choice DEFER. This document defines the research options and the exact information a future G4 design contract must freeze.

## 2. Distinguish the mechanisms

Initialization work is often conflated across four different mechanisms:

1. **Base tensor initializer:** the distribution used to draw individual weight elements (for example Xavier/Glorot or Kaiming/He).
2. **Depth/residual scaling:** additional scaling of selected projections or residual branches so contributions do not accumulate unstably with depth.
3. **Normalization/residual architecture:** the location and form of LayerNorm/RMSNorm/DeepNorm/residual updates. Changing this changes the model computation, not only the random weights.
4. **Parameterization and width scaling:** the definition of how weights, embeddings, readouts and learning rates should change when model width changes (for example maximal update parameterization, μP).

One method may bundle multiple mechanisms. T-Fixup changes initializer scaling and removes normalization/warmup in its studied architecture; DeepNorm changes the residual/normalization function and initialization together. They must not be recorded as isolated initializer-only alternatives.

## 3. Candidate initialization families

### 3.1 Conventional fan-aware initialization — correctness control

Xavier/Glorot and Kaiming/He are well-known fan-in/fan-out initializers for linear transformations. A conventional initializer is useful as a transparent baseline, but the exact fan convention and parameter role must be pinned. A single global standard deviation for every tensor is not a complete initialization contract.

The future model manifest must treat these tensor groups separately:

- token embeddings and learned positional tables, if any;
- attention Q/K/V and output projections;
- FFN gate/value/output projections;
- convolution and input-dependent state projections for an SSM;
- recurrent/delta-rule matrices and decay/state parameters for RWKV-like blocks;
- normalization weights and biases;
- output/readout projection and any tied weights;
- special scalar/vector parameters, gates and architecture-specific state.

Biases and normalization parameters require explicit policies. A common candidate is zero bias and normalization scale initialized to one with bias zero, but this is not silently imposed on all architecture families.

**Role:** reference/control only. Its use does not establish adequate gradient flow for arbitrary depth, residual topology or width.

### 3.2 Residual-branch / depth-scaled initialization

Residual networks repeatedly add the outputs of attention and FFN sublayers to a stream. The variance and gradient effects of these contributions depend on the number of sequential residual branches and the normalization placement.

Depth-scaled approaches reduce the initial scale of selected residual-output projections as depth grows. The target is to prevent accumulated residual updates from dominating the stream or destabilizing early optimization. The precise factor depends on whether a block contains one or multiple residual branches, whether attention and FFN outputs are both scaled, and whether the model is pre-norm or post-norm.

The Hugging Face Transformers Mamba-2 implementation exposes an optional residual rescaling path that reinitializes selected residual output weights using a GPT-2-style depth factor based on the number of layers. This is implementation evidence for a specific code path, not a universal initialization theorem or an Argus decision. The factor must be derived from the exact residual branches and architecture rather than copied by suffix/name heuristic.

**Argus rule:** count update-bearing residual branches from the actual graph. Do not assume “one Transformer layer” equals one residual addition. Record exactly which weight tensors are scaled, the scale formula, layer/block count used in the formula, and the reason each tensor is included.

### 3.3 T-Fixup

Huang et al. (ICML 2020) analyze optimization instability in deep encoder–decoder Transformers and propose T-Fixup, a depth-dependent initialization intended to bound early model updates. Their reported recipe:

- uses Xavier initialization for parameters other than input embeddings;
- initializes input embeddings with a Gaussian whose standard deviation is based on d⁻¹ᐟ²;
- applies depth-dependent scale factors to selected encoder/decoder attention and MLP matrices and embeddings;
- removes LayerNorm from the studied blocks and trains without learning-rate warmup.

For an N-layer encoder–decoder setup in the paper, the scaling expressions are architecture-specific (including (9N)⁻¹ᐟ⁴ and approximately 0.67N⁻¹ᐟ⁴ for the parameter groups described by the authors).

**Transfer limit:** T-Fixup is a joint initializer/architecture/training recipe, not “just use a smaller standard deviation.” Removing LayerNorm and warmup changes several variables at once. Its reported machine-translation experiments and very deep encoder–decoder configuration do not establish a suitable decoder-only, small Android or SSM/RWKV initialization.

**Argus role:** retain as research evidence and, only if later justified, compare a faithfully transcribed decoder-only variant. Do not apply the encoder–decoder formula to a decoder-only graph by replacing N casually.

### 3.4 DeepNorm / DeepNet

Wang et al. propose DeepNorm, a residual normalization function with architecture-derived scaling, paired with initialization scaling. In simplified form, a sublayer update has the structure:

\[
x_{l+1} = \operatorname{LN}\left(\alpha x_l + G_l(x_l;\theta_l)\right)
\]

where α modifies the residual contribution and selected parameters in G are initialized with a depth-dependent coefficient β. The exact α and β are derived separately for encoder, decoder and encoder–decoder layouts.

The paper reports training Transformers up to 1,000 layers and a 200-layer, 3.2B-parameter multilingual translation model. The method addresses extreme depth and is a combined residual/normalization/initialization design.

**Transfer limit:** those results are large-scale translation experiments. DeepNorm is not equivalent to Pre-LN, RMSNorm or a simple residual projection scale, and its encoder/decoder coefficients must not be transplanted into an Argus decoder-only model without deriving the matching architecture case.

**Argus role:** retain as a depth-stability candidate if the G4 design proposes a sufficiently deep Transformer. Otherwise it may add unnecessary complexity compared with a simpler pre-norm baseline. No DeepNorm variant is selected.

### 3.5 Effective-theory initialization analysis

Dinan, Yaida and Zhang analyze forward/backward signal propagation in wide and deep Transformers with attention and MLP residual blocks. Their analysis derives width/depth scaling suggestions for initialization and training hyperparameters, then tests those suggestions in practical vision and language Transformer training.

This evidence supports treating initialization scale, residual depth, width and optimizer/training hyperparameters as a coupled system. It does not produce one universal standard deviation for every architecture, dtype or depth.

**Argus role:** use the analysis as a theoretical cross-check when the selected G4 model dimensions and residual topology are known; not as a replacement for the exact tensor-level initializer manifest and numerical verification.

### 3.6 Maximal Update Parameterization (μP) and μTransfer

Yang et al. show that the conventional parameterization can make optimal learning rates and other training hyperparameters vary as width changes. Under μP, parameter and learning-rate scaling is defined so that feature updates remain comparable across width in the large-width limit. Their μTransfer results tune smaller proxy models and transfer selected hyperparameters to wider target models.

The paper reports demonstrations including transfer from a 13M-parameter model to BERT-large (350M) and from a 40M model to GPT-3 6.7B. It also states that not all hyperparameters transfer: regularization depends on model/data size, and small-data fine-tuning settings may not benefit in the same way. Its experiments and guarantees apply to specified architectures, parameter groups and optimizers; they are not a promise that one set of hyperparameters works for arbitrary Argus devices.

Implementation requirements include:

- define a base shape and the exact width axes that scale;
- distinguish hidden, input, output/readout, embedding and attention parameter types;
- specify initialization variance and learning-rate scaling per parameter type;
- define output/readout multipliers and tied embedding behavior;
- keep regularization such as dropout/weight decay separate from width-transferable parameters;
- prove that the selected optimizer variant is compatible with the chosen μP convention;
- preserve the base-shape manifest and parameterization version in every model generation.

The μP reference implementation uses base-shape metadata and parameter-specific scaling, including special readout handling. It is not equivalent to adding one global width multiplier to the configuration.

**Argus role:** μP/μTransfer is a strong research candidate for tuning a bounded model-size portfolio from one or more smaller local/reference proxy configurations. It is not automatically selected. The portfolio must compare models within the same architecture family, tokenizer profile, data semantics and training objective; proxy-to-target transfer must be validated rather than assumed.

## 4. Device-adaptive initialization is not device-dependent randomness

The device profile may select model dimensions and therefore the appropriate initialization scales. It must not change the meaning of the initializer unpredictably.

The following principles apply:

1. **The model configuration determines the initializer.** Layer count, residual-branch count, width, head dimensions, FFN dimensions, parameter tying and architecture family are inputs to the initialization manifest.
2. **The generation seed determines the random draw.** A production generation uses a unique, securely generated seed recorded in protected generation metadata; deterministic fixed seeds are reserved for tests and reproducible research.
3. **The same manifest and seed reproduce the same tensor values.** The PRNG algorithm, version, seed derivation, tensor traversal order, distribution implementation and conversion/casting path must be pinned.
4. **Tensor order is not incidental.** Parallel/chunked initialization must use a counter-based or otherwise order-independent mapping, or persist a deterministic per-tensor/per-chunk seed derivation. Thread scheduling must not change the generated tensors.
5. **Do not use device brand as an initializer selector.** A device can choose a smaller/larger approved model profile; the model profile then determines the initialization policy.
6. **Do not reinitialize after readiness.** Any architecture/width/parameterization change creates a new candidate generation with a new manifest and identity; an active model generation is immutable.

A low-memory Genesis path may initialize and persist tensors in bounded chunks rather than materialize a second full copy. This is only a future storage/implementation design requirement: chunking must preserve deterministic values, parameter order, checksums and atomic publication semantics. It must not silently alter the distribution or produce different weights depending on available RAM.

## 5. Initialization manifest and generation contract

Every initialized model generation must carry a versioned record with at least:

| Field | Required meaning |
|---|---|
| initializer_schema_version | Schema version for the complete initializer policy |
| initializer_family | Conventional, residual-scaled, T-Fixup, DeepNorm, μP or custom composite |
| initializer_implementation_id | Exact Argus/reference implementation identifier and version |
| architecture_id / architecture_fingerprint | Exact graph and architecture contract being initialized |
| parameter_inventory_fingerprint | Ordered parameter names/roles/shapes/dtypes and tying relationships |
| initializer_parameter_groups | Explicit per-role distribution, fan convention, scale and bias policy |
| residual_scaling_contract | Residual branches counted and exact selected output tensors/scales |
| normalization_contract | Operator, placement, affine parameters and any architecture-coupled residual formula |
| parameterization_id | Standard parameterization or exact μP/depth-μP contract and base-shape fingerprint |
| optimizer_compatibility_id | Required optimizer family/parameter-group learning-rate convention where applicable |
| precision_and_cast_policy | Draw dtype, storage dtype, accumulation/cast behavior and rounding |
| production_seed_reference | Protected seed or secure seed derivation record; never expose it as user content |
| prng_algorithm_version | Exact PRNG and deterministic tensor/chunk stream definition |
| tensor_value_checksums | Per-tensor or deterministic chunk checksums before generation commit |

The generation manifest must not contain personal conversation text or sensitive content. A seed is not the same as a model identifier; a fingerprint of the resulting tensors is still required.

## 6. Required correctness and stability verification

These are future implementation acceptance criteria, not work performed in this documentation phase.

### 6.1 Deterministic reproduction

- Same architecture/configuration manifest + same fixed test seed + same initializer implementation/version yields identical tensor bytes on the declared reference path.
- Different production seeds do not accidentally reuse one global seed for every installation.
- Parallel and sequential chunk schedules produce the same values and tensor checksums.
- Tied input/output embedding references point to the same parameter storage; they are not initialized twice independently.

### 6.2 Distribution and scale checks

For every parameter group, inspect:

- empirical mean, variance, standard deviation and tail quantiles;
- expected vs measured scale under fan-in/fan-out and depth scaling;
- zero/nonzero counts and finite-value incidence;
- embedding, readout, Q/K/V, attention output, FFN and recurrent/state parameter groups separately;
- tied-weight identity and post-cast scale;
- layerwise activation RMS/variance and gradient RMS/variance at initialization.

### 6.3 Initial forward/backward checks

- Every retained family has a small deterministic reference graph.
- Forward output, logits, loss and backward gradients are finite.
- Gradient norms are logged by layer and parameter group, not only globally.
- Finite-difference checks validate selected parameters and operator paths.
- Check the sensitivity of the first updates to learning rate, warmup, clipping, precision and sequence length.
- Run multiple independent seeds; a single seed is not convergence evidence.
- Test the actual causal/SSM/recurrent reset and packed-sequence semantics so the initialization graph matches the intended training objective.

### 6.4 Scaling and profile-transfer checks

For candidate μP or depth-scaling use:

- establish a mathematically explicit base model and target family;
- sweep at least two or more widths/depths within the same family;
- keep tokenizer ID/fingerprint, data bytes, sample order and objective fixed;
- distinguish parameter-matched, token-matched and compute-matched results;
- compare learning-rate landscapes and layerwise update-to-parameter ratios;
- verify that regularization and optimizer parameters not covered by μTransfer are re-tuned;
- state the proxy/target limits and confidence; do not call transfer zero-shot until the required parameterization is implemented and verified.

## 7. Family-specific boundaries

| G2 family | Initialization contract that must be defined | What must not be borrowed blindly |
|---|---|---|
| Decoder-only Transformer | Embedding/readout, attention and FFN fan/scale, residual-output scaling, norm parameters, positional parameters and optional μP base shape | Encoder–decoder T-Fixup/DeepNorm coefficients, one global Gaussian std, or a depth factor that counts the wrong residual branches |
| Mamba-2 SSD | Input-dependent SSM projections, convolution parameters, A/D/state initialization, gating, output projection, GroupNorm or RMSNorm parameters and SSD-specific tensor shapes | Standard Transformer Q/K/V assumptions or Transformer residual scales applied to selective state parameters |
| RWKV-7 | Matrix recurrent state/decay parameters, receptance/key/value/gate projections, output projection and recurrence-specific stability constraints | Treating a matrix-valued recurrent state as an attention KV cache or initializing all state/decay parameters like dense FFN weights |
| Attention–SSM/recurrent hybrid | A complete per-block-family parameter-group inventory and explicit transition boundaries | One initializer formula applied globally across heterogeneous block types |

Each family needs its own CPU correctness reference and finite-value/gradient checks. The common initializer manifest schema may be shared, but the per-family parameter groups and equations are not assumed interchangeable.

## 8. Current disposition and G4 deliverables

- **No initializer or parameterization is selected.** G3 initializer mathematics and scale remains DEFER.
- Conventional fan-aware initialization remains a correctness control, not a product choice.
- Residual/depth scaling, T-Fixup, DeepNorm, effective-theory initialization and μP/μTransfer are retained as distinct research families.
- T-Fixup and DeepNorm are composite architecture/training recipes; do not compare either as if only one tensor standard deviation changed.
- μP is especially relevant to device-adaptive width portfolios, but only if the base-shape, readout/embedding and optimizer compatibility contracts are implemented exactly.
- G4 must choose the initialization and parameterization together with the backbone, normalization/residual topology, width/depth, output head, optimizer and precision policy. It must document why rejected options do not fit the target profile portfolio.
- Physical Android performance, training stability and broad general capability remain post-implementation verification requirements. This document does not close E1–E7, G1-V or any device-support claim.

## 9. Primary references

1. Huang et al., ICML 2020, [Improving Transformer Optimization Through Better Initialization](https://proceedings.mlr.press/v119/huang20f.html). T-Fixup analysis and depth-dependent initialization for deep encoder–decoder Transformers.
2. Wang et al., [DeepNet: Scaling Transformers to 1,000 Layers](https://arxiv.org/abs/2203.00555). DeepNorm residual normalization plus theoretically derived initialization; extreme-depth evidence.
3. Dinan, Yaida and Zhang, [Effective Theory of Transformers at Initialization](https://arxiv.org/abs/2304.02034). Forward/backward signal propagation and practical width/depth initialization/hyperparameter scaling.
4. Yang et al., [Tensor Programs V: Tuning Large Neural Networks via Zero-Shot Hyperparameter Transfer](https://arxiv.org/abs/2203.03466). μP and μTransfer for width scaling and proxy-to-target hyperparameter transfer; parameter-group/optimizer limitations apply.
5. [OpenAI GPT-2 model and training overview](https://openai.com/index/better-language-models/). Original autoregressive GPT-2 training context; not by itself a complete initializer specification.
6. [Hugging Face Transformers Mamba-2 implementation](https://github.com/huggingface/transformers/blob/main/src/transformers/models/mamba2/modeling_mamba2.py). Implementation reference for architecture-specific initialization and optional residual projection rescaling; not an Argus dependency or universal initialization standard.
