# Optimizer Research and Lion Assessment

**Status:** Comparative research; no optimizer selected  
**Last reviewed:** 2026-11-04  
**Scope:** From-scratch local language-model training, device-adaptive execution, exact optimizer-state recovery  
**Implementation status:** Documentation only. No Argus optimizer code, build, benchmark, or device experiment is authorized or claimed.

## 1. Executive conclusion

Lion is a credible optimizer candidate for Argus because it stores one momentum tensor per trainable parameter rather than AdamW's first- and second-moment tensors. Under an FP32 optimizer-state policy, this reduces the persistent moment-state term from 8 bytes to 4 bytes per parameter. It does **not** halve total training memory: model weights, gradients, activations, logits, temporary buffers, runtime workspaces, data staging and allocator overhead remain.

The research does not justify replacing AdamW with Lion globally:

- The original NeurIPS 2023 paper reports comparable or better results on several language-model tasks, but also reports cases where gains are small and observes that Lion's benefit depends on batch size and learning-rate selection.
- An ICLR 2025 autoregressive-language-model study finds Adam, Lion, Adafactor and Sophia broadly comparable across its tested model sizes and settings, with no single winner in optimal performance or hyperparameter robustness.
- A 2025 limited-budget pretraining comparison reports different winners for loss, training GPU-hours and downstream score: Sophia had the lowest training/validation loss, Lion was fastest in GPU-hours, and AdamW had the best downstream evaluation results.

Those results establish Lion as worth comparing, not as the Argus winner. None demonstrates Lion training a broadly capable, randomly initialized language model on a physical Android phone.

**Current disposition:** **DEFER** the optimizer algorithm decision. Keep AdamW as a reference/control, add Lion as a first-class memory-sensitive candidate, and retain Adafactor as a secondary memory-oriented comparator. Sophia may be considered later if its additional state/compute and implementation cost are justified. Do not select optimizer semantics before architecture, tokenizer profile, precision, parameter groups, accumulation policy and recovery contract are explicit.

## 2. Argus constraints governing optimizer selection

An optimizer candidate must satisfy all of the following:

1. Train locally from Genesis-initialized model weights; pretrained-base fine-tuning is not a substitute.
2. Operate on the full declared trainable parameter set for the selected architecture, including embedding/output parameters and architecture-specific trainable state.
3. Expose deterministic, well-defined update semantics with finite-value checks and a CPU correctness reference.
4. Have a complete, versioned state contract that can be committed and restored with model weights, scheduler, precision/scaler state, RNG state and dataset cursor.
5. Support safe pause/resume and Android process-death recovery without duplicate or skipped committed updates.
6. Fit a device-specific memory and energy envelope without relying on installed RAM or theoretical peak TOPS alone.
7. Permit explicit parameter-group and weight-decay masks. No heuristic masking based on parameter-name suffixes is an acceptable contract.
8. Be compared on matched model/data/token/compute conditions with optimizer-specific hyperparameter tuning.
9. Preserve local-instance isolation. No cloud optimizer state, central optimizer or cross-user parameter aggregation is introduced.

Optimizer selection is independent of the G2 backbone shortlist, but it is not independent of the chosen architecture, precision, parameter inventory, tokenization profile, batch/accumulation semantics or device resource envelope.

## 3. Lion algorithm and exact update semantics

Lion means **EvoLved Sign Momentum**. It was discovered through symbolic program search rather than designed as a conventional adaptive second-moment optimizer.

For gradient \(g_t\), parameter \(\theta_t\), momentum \(m_t\), learning rate \(\eta_t\), decay factors \(\beta_1,\beta_2\), and decoupled weight-decay coefficient \(\lambda_t\):

\[
m_0 = 0
\]

\[
u_t = \operatorname{sign}\left(\beta_1 m_{t-1} + (1-\beta_1)g_t\right)
\]

\[
\theta'_t = (1-\eta_t\lambda_t)\theta_{t-1}
\]

\[
\theta_t = \theta'_t - \eta_t u_t
\]

\[
m_t = \beta_2m_{t-1} + (1-\beta_2)g_t
\]

The weight-decay term is applied decoupled from the signed update. The order and algebra must be pinned to the selected reference variant; an implementation must not silently change it to coupled L2 regularization.

Important properties:

- Lion keeps one exponential moving-average momentum tensor, \(m\), per trainable parameter.
- It uses \(\beta_1\) to construct the current signed update and \(\beta_2\) to update persistent momentum.
- It does not maintain Adam's second-moment estimate and does not perform Adam-style per-coordinate division by a variance estimate.
- The sign operation makes the update magnitude per coordinate largely independent of gradient magnitude; the sign determines direction.
- The original reference algorithm does not use Adam-style first/second-moment bias correction.
- Decoupled weight decay, parameter masks, precision and scheduler semantics must be specified exactly rather than inherited accidentally from an unrelated optimizer.

The Google Research PyTorch reference applies decoupled weight decay, creates one exp_avg tensor with zeros_like(parameter), computes the signed interpolation of momentum and gradient, updates the parameter, and then advances the momentum EMA. Its state dtype therefore follows the parameter dtype in that simple reference. Argus must not copy that dtype behavior blindly into a mixed-precision engine: moment dtype is a separate design variable and must be measured for stability and memory.

### 3.1 Learning-rate and weight-decay implications

Lion's sign-based update commonly has a larger norm than AdamW's adaptive update. Official implementation documentation (Optax, Keras and MLX) gives general starting guidance of a learning rate around 3–10 times smaller than AdamW and weight decay around 3–10 times larger to maintain a comparable product \(\eta\lambda\). This is a published tuning heuristic, **not** a fixed Argus hyperparameter prescription.

The following must be tuned independently for each architecture/configuration:

- learning-rate peak, warmup and schedule;
- \(\beta_1,\beta_2\);
- decoupled weight decay and exact decay mask;
- gradient clipping order and threshold;
- optimizer-state dtype;
- effective batch size and gradient accumulation;
- embedding/output parameter groups and architecture-specific parameter groups.

Using the same AdamW learning rate and weight decay for Lion is not a fair comparison and may destabilize the result. Conversely, blindly applying the 3–10× heuristic is not acceptable without a small, controlled search.

## 4. Memory accounting for Android device profiles

For \(P\) trainable parameters, the optimizer-state term is:

\[
M_{\text{optimizer}} = P \times b_{\text{state}}
\]

With FP32 moments:

- AdamW: one FP32 first moment plus one FP32 second moment = 8 bytes/parameter.
- Lion: one FP32 momentum = 4 bytes/parameter.
- Difference: 4 bytes/parameter, or 40 MB for 10M parameters and 200 MB for 50M parameters (decimal MB).

This is a reduction of **50% of the FP32 moment buffers only**. It is not a 50% reduction in total training RAM.

### 4.1 Illustrative tensor inventory

The table includes only weights, gradients, optional FP32 master weights and optimizer moments. It excludes activations, output logits, temporary update tensors, allocator fragmentation, runtime/kernel workspaces, data staging, checkpoint overlap and Android process overhead.

| State policy | AdamW bytes/parameter | Lion bytes/parameter | AdamW at 10M / 50M | Lion at 10M / 50M |
|---|---:|---:|---:|---:|
| FP32 weights + FP32 gradients + FP32 moments | 16 B | 12 B | 160 / 800 MB | 120 / 600 MB |
| BF16 weights + BF16 gradients + FP32 moments; no master copy | 12 B | 8 B | 120 / 600 MB | 80 / 400 MB |
| BF16 weights + BF16 gradients + FP32 master weights + FP32 moments | 16 B | 12 B | 160 / 800 MB | 120 / 600 MB |
| BF16 weights + FP32 gradients + FP32 master weights + FP32 moments | 18 B | 14 B | 180 / 900 MB | 140 / 700 MB |

These are analytical tensor-inventory estimates only. Actual allocation depends on the implementation. A low-precision Lion momentum buffer can reduce memory further, but changes numerical behavior and must not be assumed stable. AdamW can also store low-precision moments in some runtimes, so the comparison must use explicit state-dtype policies on both sides.

### 4.2 What Lion does not reduce

Lion does not directly reduce model-weight storage, gradient storage, forward/backward activations, attention KV cache or recurrent state, vocabulary-sized output logits, dataset/token staging, native runtime and backend workspaces, checkpoint serialization buffers or simultaneous inference snapshot memory.

A low-RAM phone can still OOM with Lion if activations, logits or runtime workspaces dominate. Admission must use measured/analytically bounded total peak memory, not optimizer-state savings alone.

### 4.3 Fused execution and temporary buffers

The algorithm is simple enough to express as fused native tensor operations: decay eligible parameters, form the signed direction, update parameters, and update persistent momentum. A naive eager implementation may allocate temporary tensors for the direction. A fused implementation can reduce transient allocation and memory traffic, but its peak scratch requirement and numerical parity still need measurement.

No CPU, Vulkan, GPU or NPU acceleration is implied by selecting Lion. It is an optimizer equation, not a hardware backend.

## 5. Published evidence and limitations

| Evidence | What it reports | What it does not establish for Argus |
|---|---|---|
| Chen et al., NeurIPS 2023, *Symbolic Discovery of Optimization Algorithms* | Lion was discovered by program search; it stores momentum rather than Adam's two moments. The paper reports similar or better results in several autoregressive/masked-LM/fine-tuning tasks, and explicitly notes larger update norms, need for smaller learning rates, and task/batch-dependent gains. | No Argus Android result, no universal optimizer win, and no proof of broad random-init language pretraining on a phone. |
| Zhao et al., ICLR 2025, *Deconstructing What Makes a Good Optimizer for Autoregressive Language Models* | Across tested model sizes, hyperparameters and architecture variants, Adam, Lion, Adafactor and Sophia were broadly comparable; no one method was a clear overall winner in performance or hyperparameter robustness. Signed momentum was also studied as a simplification. | Results do not select a device-specific optimizer, nor prove exact convergence for Argus's model/data/resource envelope. |
| Schlotthauer et al., 2025, *Pre-Training LLMs on a Budget: A Comparison of Three Optimizers* | Compares AdamW, Lion and Sophia using two base architectures, single-/multi-epoch settings, fixed token counts and smaller proxy models with separately tuned hyperparameters. Abstract findings: Sophia achieved the lowest training/validation loss, Lion used the fewest training GPU-hours, and AdamW led downstream evaluations. | GPU-hours are not Android wall-clock/energy; this study does not establish a mobile workload or Argus-scale winner. |
| Google Research official PyTorch reference | Shows the algorithm's update order and one persistent momentum tensor; file is Apache-2.0. | A concise PyTorch reference is not an Android C++/JNI training engine, does not define Argus precision policy or checkpoint transaction, and does not establish current upstream maintenance. The google/automl repository was archived on 2026-05-06. |

### 5.1 Batch size and mobile accumulation

The original Lion paper reports that Lion benefits more from larger batches and discusses cases where its advantage may be small at very small batch sizes. Its accompanying ViT/ImageNet result reports an optimal batch size of 4,096 for Lion versus 256 for AdamW, while also showing that Lion remained robust at batch 64. These are task-specific vision results, not universal minimum-batch requirements for language models.

Argus therefore must not hardcode a “Lion requires batch 64” rule. It must measure the relevant **effective batch per optimizer update**, while preserving the distinction between:

- **Microbatch:** examples or tokens processed in one forward/backward pass.
- **Accumulation count:** number of microbatches whose gradients are combined before an optimizer step.
- **Effective batch:** the examples/tokens contributing to one committed update.
- **Optimizer update:** one application of Lion/AdamW to parameters and optimizer state.

For a single-device run, effective batch is determined by microbatch and accumulation policy, with token-level loss weighting handled correctly. Gradient accumulation can raise effective batch without holding all microbatch activations simultaneously, but it does not provide the same hardware parallelism as a truly large batch and may increase time to each update.

**Critical invariant:** Lion's momentum must be updated once per logical optimizer update using the aggregated gradient, not once per microbatch, unless a deliberately different algorithm is specified and tested. Clipping, loss scaling, token weighting and gradient reduction must occur at explicitly documented points. A pause during accumulation must either persist the accumulation buffer/cursor or discard uncommitted work and replay from the last committed update.

## 6. Candidate comparison for Argus

| Optimizer | Persistent state characteristic | Potential Argus benefit | Main risk / cost | Research role |
|---|---|---|---|---|
| **AdamW** | First + second moment per parameter, plus step metadata | Strong reference baseline, broad language-model usage and adaptive per-parameter scaling | Larger optimizer-state memory; two moment tensors must be checkpointed | **Reference/control** |
| **Lion** | One momentum tensor per parameter | Approximately half the FP32 moment-state memory; simple fused update candidate | No second-moment adaptivity; sign update; lower LR and separately tuned decay; batch-sensitive evidence; precision sensitivity | **Primary memory-sensitive candidate** |
| **Adafactor** | Factorized second-moment state for suitable matrix-shaped tensors; optional first-moment state depending on variant | Can reduce state growth for large matrices | Factorization/shape semantics and parameter-specific behavior; implementation and checkpoint complexity | **Secondary comparator** |
| **Sophia** | Curvature/Hessian-related state and update work, depending on variant | Published pretraining comparison reports competitive loss | More complex estimator/update path and additional compute/state; not justified until simpler baselines are characterized | **Optional later comparator** |
| **SGD with momentum** | One momentum tensor | Simple reference for optimizer-state/gradient behavior | Autoregressive-LM comparison reports it below adaptive/sign-momentum alternatives in the studied settings | **Diagnostic baseline only** |

This table is not a winner ranking. The first controlled comparison should focus on AdamW versus Lion, with Adafactor included only if its factorization maps cleanly to the selected parameter shapes and its state cost is materially relevant.

## 7. Interaction with the four G2 architecture families

Optimizer results must be compared within each retained architecture candidate. A single Transformer result cannot be generalized to Mamba-2 SSD, RWKV-7 or a hybrid.

| G2 family | Optimizer-specific implications to audit |
|---|---|
| Decoder-only Transformer | Include embeddings/output head, attention, FFN and normalization groups. Test output-head/embedding update behavior and norm-parameter stability. AdamW remains the control; compare Lion with the same tokenizer, sequence packing and token budget. |
| Selective SSM / Mamba-2 SSD | Include selective-scan and recurrent/state-related parameters. Verify gradient finiteness and update parity for scan/convolution/state projections; no claim that Lion makes specialized backward kernels cheaper. |
| Recurrent delta-rule / RWKV-7 | Include matrix-valued recurrent-state parameter groups and architecture-specific scale parameters. Confirm the sign update does not create instability in recurrent dynamics or long-sequence training. |
| Attention–SSM/recurrent hybrid | Test each subsystem's parameter groups and global clipping behavior; mixed optimizer policies are a new design and must not be introduced without an explicit experiment/ADR. |

The architecture comparison must keep tokenizer-dependent dimensions fixed within each profile. Vocabulary/head cost is part of total parameter count and optimizer memory, so the optimizer comparison cannot omit embeddings or output projection.

## 8. Checkpoint, recovery and exact-resume contract

Optimizer state is part of the candidate model's mathematical state, not disposable cache.

For Lion, a resumable checkpoint must include:

- optimizer algorithm ID and implementation/schema version;
- exact \(\beta_1,\beta_2\), learning-rate schedule state, weight-decay values and parameter-group mapping;
- persistent momentum tensor for every trainable parameter;
- optimizer step/update sequence and any parameter-group step metadata;
- momentum dtype and tensor layout;
- gradient-scaler/precision metadata where applicable;
- clipping and accumulation semantics;
- model-generation, architecture, tokenizer, dataset and parameter-inventory fingerprints;
- RNG and sampler/cursor state;
- checksum and atomic checkpoint transaction metadata.

A resume under another optimizer, different beta values, different parameter groups, changed tensor inventory or changed state dtype is not an exact resume. It requires a new session/configuration and an explicit migration/restart policy; do not reinterpret AdamW moments as Lion momentum.

A checkpoint transaction must bind model tensors, optimizer state, scheduler/scaler, RNG, data cursor and committed update sequence. The state must not be published ahead of the corresponding parameter update. Crash recovery must never duplicate or skip a committed update.

## 9. Controlled comparison protocol

No optimizer may be selected by comparing untuned default settings or unrelated papers' wall-clock numbers.

Minimum design for later separately authorized verification:

1. Use the same randomly initialized model generation/configuration, tokenizer fingerprint, training/validation split, corpus manifest and sample order for each optimizer comparison.
2. Tune each optimizer's learning-rate schedule, weight decay and beta parameters independently within declared search ranges. Record the search budget and do not give one optimizer more tuning compute.
3. Keep token count, objective, sequence packing, loss weighting, gradient clipping policy and target effective-batch semantics equal.
4. Compare at least a low-effective-batch and a higher-effective-batch regime using mobile-compatible microbatching/accumulation. Do not impose a fixed batch threshold from the original vision experiment.
5. Record loss versus training tokens and optimizer updates, held-out loss/perplexity, downstream task probes, finite/non-finite gradients, update/gradient norms, convergence variance across seeds and general-capability regression.
6. Record actual state bytes, peak and steady process/native/accelerator memory, scratch allocation, update latency, tokens/sec, energy/update, thermal trajectory, checkpoint size/time and resume time.
7. Compare fused and reference implementations only after exact one-step parameter/state parity is established.
8. Repeat across selected model/resource profiles. A profile may select a different optimizer only through an explicit, versioned configuration policy; never switch optimizer state silently during a training session.

Suggested correctness tests include one-step equation parity against a deterministic FP64/FP32 reference; exact parameter and momentum-state comparison over multiple updates; decoupled weight-decay mask tests; accumulation equivalence under declared token weighting; precision/state-dtype stress tests; checkpoint save/restore before and after committed updates; fault injection during update/checkpoint publication; and OOM/thermal pause/recovery behavior.

All physical Android claims belong to later E1–E7/device verification. This document does not authorize or claim those experiments have occurred.

## 10. Preliminary G3 component disposition

| Component | Current disposition | Reason |
|---|---|---|
| AdamW | **DEFER** as the selected product optimizer; retain as reference/control | Strong comparison anchor, but optimizer-state memory and device fit remain profile-dependent. |
| Lion | **DEFER** as a selected optimizer; admit to primary comparison | Meaningful optimizer-state reduction and credible LM evidence, balanced by batch, LR/WD, precision and downstream-quality trade-offs. |
| Adafactor | **DEFER** | Possible memory reduction, but shape-dependent factorization and semantics need a fair comparison. |
| Sophia | **DEFER / optional** | Do not widen the minimum comparison until its additional state/compute cost is justified. |
| Optimizer state schema and transactional recovery | **CUSTOM** | Argus requires exact model/optimizer/RNG/cursor identity, atomic publication and crash recovery independent of the chosen optimizer. |

These statements refine the research content within the existing G3 baseline. They do not change ADR-026's one-primary-disposition rule, select Lion, select AdamW, or authorize implementation. The optimizer algorithm row in document 35 therefore remains **DEFER**.

## 11. Open questions and closure criteria

The optimizer component cannot close until:

- the architecture comparison defines parameter inventory and optimizer-visible tensor groups for each shortlisted family;
- G1-D/G4 fix the tokenizer profile and vocabulary/head dimensions for each compared model configuration;
- precision and gradient/accumulation semantics are defined;
- AdamW and Lion have equal-budget, optimizer-specific hyperparameter comparisons with reported uncertainty;
- memory accounting includes persistent state and transient update buffers;
- exact checkpoint and recovery semantics are reconciled with documents 19 and 36;
- results disclose the lowest profile where each optimizer fits, without assuming a universal device tier.

**Current decision:** Lion is **research-qualified, not adopted**. Keep it in the comparator set; do not promote it to the default solely because it uses one fewer moment tensor.

## 12. Primary references

1. Chen et al., 2023, [Symbolic Discovery of Optimization Algorithms](https://proceedings.neurips.cc/paper_files/paper/2023/hash/9a39b4925e35cf447ccba8757137d84f-Abstract-Conference.html), NeurIPS 2023. Algorithm origin, memory property, language-model results and limitations.
2. Google Research, [official PyTorch Lion reference implementation](https://github.com/google/automl/blob/master/lion/lion_pytorch.py). Update order, decoupled decay and one momentum tensor. The file header declares Apache-2.0; the repository is archived and is a reference, not an Android dependency.
3. Zhao et al., 2025, [Deconstructing What Makes a Good Optimizer for Autoregressive Language Models](https://proceedings.iclr.cc/paper_files/paper/2025/hash/07ea874e9e4f71ec6680a3574a485a36-Abstract-Conference.html), ICLR 2025. Controlled language-model optimizer comparison.
4. Schlotthauer et al., 2025, [Pre-Training LLMs on a Budget: A Comparison of Three Optimizers](https://arxiv.org/abs/2507.08472). AdamW, Lion and Sophia comparison with two base architectures and smaller proxy models.
5. [Optax Lion documentation](https://optax.readthedocs.io/en/latest/api/generated/optax.lion.html), [Keras Lion documentation](https://keras.io/api/optimizers/lion/) and [MLX Lion documentation](https://ml-explore.github.io/mlx/build/html/python/optimizers/_autosummary/mlx.optimizers.Lion.html). Practical notes on sign-update norm, LR/weight-decay tuning and state semantics.
6. [Bitsandbytes Lion documentation](https://huggingface.co/docs/bitsandbytes/main/en/reference/optim/lion). 8-bit/paged implementation options; availability does not imply suitability or Android support.
