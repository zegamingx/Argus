# G4 Architecture Decision and Device-Profile Contract

**Status:** Pre-decision framework; no backbone, model profile or component values selected  
**Last reviewed:** 2026-11-05  
**Scope:** G4 decision method, immutable model-profile schema, family-specific design inputs, analytical admission and ADR acceptance  
**Dependencies:** Documents 01, 15, 20–23, 33–42  
**Boundary:** This is a documentary decision framework. It does not authorize source implementation, builds, workflows, physical-device tests or claims of Android support.

## 1. Purpose

G4 is the pre-implementation architecture design gate. Its output is not a vague statement that Argus will use a Transformer, Mamba or RWKV. It must define a complete, versioned model bundle for each supported device-conditioned profile, explain why the chosen design is preferred over alternatives, and expose every unverified assumption.

G4 is also not a request to select the largest model that fits advertised RAM. Argus must account for the entire local lifecycle:

- bounded Genesis preparation and tokenizer resolution;
- local model construction and deterministic random initialization;
- post-readiness foundation-learning sessions;
- inference and concurrent request state;
- pause/resume and atomic recovery;
- candidate promotion, rollback and migration;
- Android memory, storage, thermal, energy and process lifecycle.

A selected design may proceed to a separately reviewed implementation plan while Android execution remains unverified. E1–E7 and G1-V remain hard post-implementation evidence gates before Argus claims a named device tier or broad from-scratch training capability.

## 2. Inputs that must exist before G4 review

G4 consumes, rather than silently re-decides, the accepted product and research contracts:

| Input | Authority | Required state at G4 |
|---|---|---|
| Product scope and local-instance boundary | Document 01 / ADRs | Accepted; no pretrained foundation, no central model/gradient aggregation, and clear user-facing capability claims |
| Documentary tokenizer contract | Document 34 / G1-D | Closed; product language/unsupported-language policy accepted, source-use decisions recorded, runtime/API/ABI and profile-selection rules specified |
| From-scratch feasibility boundary | Document 33 | Analytical resource model and E1–E7 plan complete; physical results remain explicitly unverified |
| Architecture shortlist and component dispositions | Document 35 / G2 / G3 | Four-family shortlist and one-primary component dispositions retained; DEFER values are not treated as decisions |
| Cross-family evidence and limitations | Document 38 | Evidence categories and transfer limits reviewed |
| Transformer component contract candidates | Document 39 | Attention, position, FFN, norm/residual and head choices reviewed if Transformer/hybrid is retained |
| Initialization and width-scaling contract | Document 40 | Candidate initializer/parameterization comparison and deterministic manifest requirements reviewed |
| Recurrent/chunk lifecycle | Document 41 | Complete carry, reset, chunk/BPTT and checkpoint semantics resolved if Mamba/RWKV/hybrid is retained |
| Parameter/operator/memory inventory | Document 42 | Exact shape formulas and all tensor/operator classes represented for every candidate |

G1-D closure is a documentary requirement. It does not require physical Android tests, but it must identify exact data permissions and runtime contracts. G4 must not bypass an open G1-D dependency by hardcoding a tokenizer temporarily.

## 3. Immutable candidate profile manifest

Each candidate intended for the first product release must be represented by a complete profile manifest. Multiple device profiles may differ in model size, precision, context, tokenizer vocabulary or backend only when the differences are explicitly reviewed and versioned. Device capability may choose among approved profiles; it must not silently mutate a profile.

The final manifest should be machine-readable. The field names below are a proposed schema contract, not a finalized JSON or database schema.

### 3.1 Identity and product contract

| Field | Required content |
|---|---|
| profile_schema_version | Version of the device/model profile schema |
| profile_id / profile_version | Stable identity and immutable revision |
| product_scope_id | Accepted language, domain, safety and unsupported-input policy |
| instance_scope | Explicitly local, independent user instance |
| compatibility_class | Declared capability constraints and permitted platform/ABI range, not a marketing chipset name |
| profile_status | PROPOSED, APPROVED_FOR_IMPLEMENTATION, IMPLEMENTED, VERIFIED, UNSUPPORTED or RETIRED |

### 3.2 Device and resource requirements

| Field | Required content |
|---|---|
| capability_evidence | Measured/reported RAM availability, ABI/ISA, supported APIs/drivers, storage availability, OS constraints and evidence source/date |
| runtime_condition_policy | Thermal, battery, charging, foreground/background, memory-pressure and cancellation policy |
| genesis_resource_envelope | Separate peak RAM/storage/CPU-time/energy budget for tokenizer resolution, model allocation, initialization and atomic initial commit; initial Genesis does not include later optimizer/gradient state |
| training_resource_envelope | Persistent candidate weights/gradients/optimizer state, activations, logits, state/workspace, data staging, concurrent parent-inference overlap and checkpoint-overlap estimate |
| inference_resource_envelope | Parameters, output logits, KV/recurrent carry per active request, scratch, batching and concurrency assumptions |
| reserve_policy | Application/system safety reserve and source of the chosen margin |
| resource_estimator_revision | Exact document 42 worksheet/formula revision and calculator implementation version when implemented |
| phase_peak_worksheet_ids | Separate tokenizer-preparation, Genesis, training-forward/backward, optimizer-update, inference and checkpoint/migration liveness worksheets |
| device_process_headroom_evidence | App/process limit, baseline resident footprint, measured allocation headroom, storage reserve, sampling conditions and confidence |
| sustained_compute_evidence | Exact backend/operator profile, measured sustained throughput/energy/thermal conditions; UNKNOWN before a matching device/backend run |
| generation_overlap_policy | Full-copy, verified copy-on-write/shared immutable payload, or serialized inference/training policy; all coexistence assumptions stated |
| checkpoint_retention_policy | Last-known-good, staged candidate, journal and rollback retention/release rules with peak storage overlap |
| admission_reason | Why this exact bundle is inside or outside the capability envelope |

Do not add all lifetime phases as if every allocation were live simultaneously. Compute phase-specific peak liveness, then include true overlap such as candidate checkpoint publication or simultaneous active inference.

### 3.3 Tokenizer and data contract

| Field | Required content |
|---|---|
| tokenizer_id / tokenizer_fingerprint | Exact immutable tokenizer artifact and complete pipeline digest |
| tokenizer_format / runtime_id | Exact artifact format and matching engine/API/ABI/version |
| normalizer / pre_tokenizer / decoder | Fully pinned behavior; no library defaults |
| special_token_inventory | Exact strings, IDs and trusted serializer roles |
| vocabulary_size | Includes all reserved IDs; must match embedding/head tensors |
| language_coverage_contract | Accepted product languages, measured data coverage and declared unsupported behavior |
| tokenizer_source_manifests | Frequency, aligned-development and evaluation manifests kept separate |
| rights_operation_record | Fitting, dev selection, artifact creation/distribution, raw-source redistribution and neural training separately recorded |
| profile_selection_policy_version | Exact deterministic eligibility/fit/tie-break rule |

A byte-level tokenizer may make arbitrary UTF-8 input representable; it does not prove model competence in every language. A lower-capability profile must not silently violate the product-language policy.

### 3.4 Model graph and tensor contract

| Field | Required content |
|---|---|
| architecture_family / architecture_version | Transformer, Mamba-2 SSD, RWKV-7 or explicit hybrid graph |
| block_map | Ordered layer/block family map; mandatory for hybrid |
| model_dimensions | Exact layer count, width, head/state/group dimensions, FFN widths, vocabulary and context |
| model_shape_candidate_id | Reviewed M0/MW/ML/MWL hypothesis ID and immutable tensor/profile revision |
| execution_envelope_id | X-SERIAL or X-OVERLAP concurrency policy and its liveness assumptions |
| backend_execution_map | CPU-reference, Vulkan compute and optional vendor-NPU routing; per-operator provider, precision, layout, queue, synchronization, fallback and transfer costs |
| vulkan_device_contract | Actual loader/driver/device/API version, compute queue, queried features/extensions/limits, shader/kernel revision and FP32/optional-FP16 arithmetic/storage capability evidence |
| npu_provider_contract | Optional provider/SoC/driver/SDK identity, verified training operator coverage and state/recovery limits; may be absent without invalidating a CPU/Vulkan profile |
| parameter_inventory_fingerprint | Ordered tensor names/roles/shapes/dtypes, biases, tied storage and trainable status |
| attention_contract | MHA/GQA/MQA, head layout, mask/position semantics, KV cache and attention kernel contract |
| position_contract | RoPE/ALiBi/learned position or family-native recurrent ordering |
| ffn_contract | Dense/gated FFN, dimensions and activation |
| normalization_residual_contract | Operator, placement, epsilon, affine parameters, residual branches/scales |
| embedding_head_contract | Tied/untied storage, output projection, special-token initialization and logit policy |
| family_state_contract | Transformer KV cache, Mamba conv+SSM state, RWKV WKV+shift state, or hybrid inventory/reset order |
| initialization_contract | Document 40 family, parameter groups, seed/PRNG, dtype/cast and chunk determinism |
| parameterization_contract | Standard/μP/depth scaling and base-shape fingerprint if applicable |
| width_depth_comparison_record | Matched M0/MW/ML comparison, seed/data/token controls, proxy-transfer evidence and MWL admission rationale |

Dimensions are values of the profile, not global constants. Do not put illustrative sizes from MobileLLM, Mamba, RWKV or any paper directly into an Argus profile without a separate sizing analysis.

### 3.5 Training and optimizer contract

| Field | Required content |
|---|---|
| training_objective_id | Exact next-token or other approved objective and loss version |
| data_manifest_id | Exact corpus revision, source rights, filtering, deduplication and language/domain sampling |
| sequence_packing_contract | Document boundaries, special-token placement, loss masks and sequence reset behavior |
| context_and_chunk_contract | Token length, chunk size, state carry, BPTT/truncated-BPTT semantics and rematerialization |
| optimizer_id / optimizer_state_schema | Exact update algorithm, state dtype, groups, masks and recovery schema |
| effective_batch_contract | Microbatch, accumulation, token weighting, clip/loss-scale order and one optimizer-update definition |
| precision_contract | Weight/compute/gradient/optimizer/recurrent/logit dtypes and scaling rules |
| schedule_contract | Learning-rate, warmup, weight decay, scheduler update count and stop/pause semantics |
| checkpoint_boundary | Atomic model/optimizer/RNG/data-cursor/recurrent-carry transaction boundary |

An adaptive workload controller may lower microbatch, add accumulation or pause within declared training-session semantics. It must not switch optimizer, precision semantics, BPTT mode, architecture or tokenizer ID mid-session without a new session/profile revision.

### 3.6 Backend/operator coverage

For every backend and operator, record one of:

- IMPLEMENTED_AND_VERIFIED — named build/device test passed;
- IMPLEMENTED_UNVERIFIED — code exists but no required device/reference proof;
- REFERENCE_ONLY — mathematical correctness implementation exists but not production path;
- AVAILABLE_UPSTREAM — public API/library exists; no Argus integration implied;
- EXPERIMENTAL — limited/maturity constraints;
- UNSUPPORTED — unavailable for the profile;
- FALLBACK — exact alternative actually executed, with explicit reason.

Coverage must be separate for forward, loss, backward, optimizer update, state update, serialization and recovery. Vulkan and NPU are independent optional execution paths; a device with no NPU may still qualify through B-VULKAN-COMPUTE, while a device without a Vulkan compute driver remains eligible for a CPU-only profile if that model/resource envelope fits. “Vulkan available”, “NPU present” or “GPU supports matmul” is not a complete training-graph capability statement. Vulkan API version does not prove optional FP16 arithmetic/storage or the full Argus graph; query each required feature and keep FP32 as the baseline path.

## 4. Hard-gate and comparison procedure

G4 must not begin with a single weighted score. Apply hard eligibility gates first, then compare feasible candidates on multiple axes.

### Gate A — Product and legal eligibility

Reject a candidate when it violates accepted language coverage, local-instance boundaries, provenance, source-use permissions, privacy, security or distribution constraints. An unresolved source permission is not a low score; it is a hard stop for using that source.

### Gate B — Mathematical completeness

Require exact graph and forward/loss/backward/update definitions. Include each family’s required operators and state:
- Transformer: attention, FFN, normalization, causal mask/position, logits and loss.
- Mamba-2: projection split, depthwise convolution, dt/A/D, gated norm, SSD chunk path, both recurrent states and backward through state boundaries.
- RWKV-7: TimeMix, generalized delta WKV, ChannelMix, both shift states, WKV backward and exact sequence reset.
- Hybrid: ordered family map and all operator/state contracts of each block.

### Gate C — Initialization and optimization stability

Require family-correct tensor initialization and complete parameter-group inventory. Compare candidate optimizer and precision policies without reusing unvalidated hyperparameters from unrelated families. If width profiles differ, state whether a standard parameterization, depth-scaled scheme or μP contract is used and how proxy transfer is validated.

### Gate D — Analytical resource admission

Use document 42 to fill exact parameter, gradient, optimizer, activation, logit, KV/recurrent-state, scratch, data-staging and checkpoint-overlap costs. For the Transformer-first control, use the first-pass parameter, FLOP, logits, saved-activation, KV and phase-liveness worksheet in document 42 section 7.2. Compute tokenizer preparation, first Genesis, post-readiness training, inference and checkpoint/migration peaks separately. Bind every estimate to the tokenizer vocabulary, graph tensor inventory, dtype policy, microbatch/context and actual backend assumptions. Reject candidates that cannot meet hard memory/storage/runtime requirements under the declared reserve. A missing backend/workspace or measured process-headroom value must remain UNKNOWN, not zero.

### Gate E — Matched comparative evidence

Report multiple views; do not pretend one matching method answers everything:

1. **Parameter-matched:** compare capacity/allocation at similar trainable parameter count.
2. **Token/data-exposure-matched:** compare the same exact data bytes/tokens/sampling schedule.
3. **Compute-matched:** compare actual FLOPs or measured reference-kernel work; state where kernel maturity prevents equality.
4. **Memory-matched:** compare within a realistic profile peak budget.
5. **Quality-matched:** where quality constraints exist, compare cost needed to reach the declared objective/quality level.

Keep inference and training measurements separate. Record published GPU results, analytical estimates, upstream source behavior and later Argus device evidence in different columns.

### Gate F — Operational recoverability

Require exact checkpoint/resume, process-death, cancellation, state reset, migration, candidate promotion and rollback contracts. Stateful families must satisfy document 41. Any profile that cannot restore a coherent model/optimizer/RNG/cursor/state transaction is not admitted.

## 5. Portfolio decision: one backbone or multiple families?

G4 must explicitly compare two product-architecture options:

- **Option A — One backbone family, multiple device profiles.** A single model family is maintained across profiles, with approved differences in width/depth, context, precision, tokenizer vocabulary and workload envelope. Benefits include smaller implementation/test/migration surface; risk is that one family may not provide a good fit for every capability envelope.
- **Option B — Bounded multi-family portfolio.** More than one backbone family may be selected for different profile classes. Benefits include different sequence/state trade-offs; costs include multiple training/backward kernels, checkpoint schemas, inference engines, quality baselines, migrations and fallback paths.

A multi-family portfolio must be justified by evidence that the extra family materially improves a profile’s feasible coverage or resource/quality trade-off. It must not be added merely because published results are interesting. Conversely, do not reject a family just because its first implementation path is harder; identify whether a correctness-first CPU reference and bounded support subset are viable.

The initial profile portfolio must be finite and maintainable. Do not generate arbitrary architecture settings from every SoC label. Device capability selects an already approved compatible profile; the runtime governor adjusts only workload controls inside that profile/session.

### 5.1 Preliminary design recommendation — not an owner-approved ADR

**Preferred first-generation candidate for G4 review: Option A, one decoder-only Transformer family with a small, versioned device-profile portfolio.** This is an engineering-risk recommendation, not a claim that Transformer is universally faster, more capable or more memory-efficient than SSM/recurrent alternatives.

Reasons for placing Transformer first:

1. A conventional causal Transformer has the most direct correctness-first CPU reference path among the retained candidates. Its core forward/loss/backward/optimizer contract is comparatively straightforward to specify and gradient-check before accelerator work.
2. The current Android evidence base is strongest around Transformer-family mobile deployment, native mobile LLM fine-tuning and mobile GPU tensor-layout optimization. This is adjacent evidence only: MobileFineTuner and FBLayout are fine-tuning/runtime evidence, not proof of random-initialization Android pretraining.
3. MobileLLM provides sub-billion Transformer design evidence, including deep/thin layouts and embedding sharing, but its from-scratch training used 32 NVIDIA A100 GPUs. It informs candidate structure and parameter allocation, not mobile throughput or feasibility.
4. Mamba-2 and RWKV-7 remain viable research families, but each adds specialized scan/recurrent update, backward, state serialization and reset/carry obligations that do not yet have an equally mature Argus Android implementation path. A hybrid inherits both operator/state surfaces and therefore has the highest first-generation integration burden.

**Recommended initial portfolio policy:** maintain one Transformer family across a finite set of device/resource profiles. Adapt width, depth, context, precision and workload envelope only through reviewed immutable profile definitions. Do not create a per-SoC architecture fork. Keep Mamba-2 and RWKV-7 as future evolution candidates; admit them when a complete local-initialization, CPU-reference, backward, optimizer, state/checkpoint and resource contract demonstrates a concrete capability or coverage gain. Keep hybrid as a later composition candidate rather than combining families in the first generation.

**First-pass Transformer reference candidate (control only, not a component selection):** use the document 39 semantic baseline of MHA, RoPE, dense GELU FFN and Pre-LN + LayerNorm for the initial correctness/reference graph. Compare GQA, ALiBi, parameter-matched SwiGLU and RMSNorm through the staged, one-factor-at-a-time ablation sequence in document 39. Compare tied and untied embedding/output-head variants only after the tokenizer vocabulary and hidden width are fixed. Use the conventional fan-aware initialization control from document 40, AdamW as the optimizer reference/control and Lion as the admitted memory-sensitive candidate from document 37. FP32 is the numerical reference path; lower-precision profiles require separate parity and stability evidence. None of these control choices becomes the production default by appearing in the reference graph.

This does not reverse the accepted Argus growth direction. A future architecture may add or reorganize modules through a new, versioned child generation with explicit tensor/state migration, validation and rollback. An active generation must never change its architecture or tensor identity in place.

**Evidence boundary:** the recommendation lowers initial implementation and verification risk; it does not establish random-init training quality, sustained Android feasibility, device support, or the final model dimensions. The exact tokenizer, vocabulary, graph components, initializer, optimizer, precision and per-device dimensions remain gated by G1-D and the remaining G4 contracts.

## 6. Device-adaptive profile selection and fallback

Use the profile-selection method proposed in document 34:

1. product language/rights eligibility;
2. artifact format/runtime/API/ABI eligibility;
3. complete tokenizer+model bundle construction;
4. Genesis-preparation admission;
5. post-Genesis model/training admission;
6. feasible Pareto set;
7. deterministic versioned tie-break;
8. immutable profile/tokenizer identity persisted before model tensor allocation.

The first compatible approved profile is not necessarily the largest one. The tie-break must reflect accepted product priorities, safety reserves, capability and maintenance cost. A lower-capability device must not receive reduced language policy or hidden reduced-quality labeling merely to make a model fit.

Fallback must be explicit:
- If the same language/model contract has another approved compatible profile, select it before tensor allocation and record why.
- If no approved profile fits, stop Genesis with a recoverable/actionable reason.
- Never change token IDs, output-head dimensions, architecture family, initializer or optimizer inside an active model generation/session.
- Never describe an algorithmic byte fallback as proof of linguistic competence.

## 7. Post-implementation verification plan

The G4 ADR must map each unverified assumption to a test owner, test method, pass/fail criterion and evidence artifact. At minimum:
- tokenizer Unicode, special-token, reference parity and offsets (G1-V);
- deterministic initialization and tensor-manifest checks;
- forward/loss/backward/optimizer finite-value and gradient parity;
- full and chunked sequence parity, recurrent state reset/carry and gradient behavior;
- exact checkpoint recovery with fault injection;
- peak memory by phase, storage and checkpoint overlap;
- sustained thermal/energy/throughput behavior;
- Android process death, foreground/background, pause/cancel and data integrity;
- model-quality and generalization evaluation on held-out rights-cleared data.

E1–E7 physical-device work begins only after the implementation phase is separately authorized. A failed physical test may narrow supported profiles or require design revision; it must not be retroactively disguised as a passing analytical estimate.

## 8. G4 ADR acceptance checklist

The architecture ADR is not ready unless all are true:

- [ ] G1-D is closed and the accepted product-language/unsupported-language policy is linked.
- [ ] Every tokenizer frequency/development/evaluation source has immutable manifests and operation-specific permission states.
- [ ] Each tokenizer family/profile has an exact runtime/API/ABI/artifact contract and fingerprint rule.
- [ ] The retained backbone family or bounded family portfolio is named and justified against rejected alternatives.
- [ ] Every profile has an ordered graph/block map and exact tensor inventory from document 42.
- [ ] Attention/position/FFN/norm/residual/head semantics are exact where applicable (document 39).
- [ ] Initialization/width parameterization and deterministic generation contract are exact (document 40).
- [ ] Optimizer, precision, accumulation, update semantics and recovery contract are exact (document 37).
- [ ] Recurrent/chunk/document reset and carry semantics are exact where applicable (document 41).
- [ ] Genesis, training, inference and checkpoint resource envelopes are separately analyzed.
- [ ] Runtime/backend coverage is stated by operator and forward/loss/backward/optimizer/state/recovery phase.
- [ ] Profile selection and fallback are deterministic, bounded and versioned.
- [ ] Rejected alternatives, transfer limits, risks and unverified assumptions are recorded.
- [ ] Every post-implementation assumption maps to an E1–E7/G1-V verification obligation.
- [ ] ADR clearly states DESIGN_SELECTED_FOR_IMPLEMENTATION_PLANNING, NOT_PHYSICALLY_VERIFIED.

G4 closure does not authorize coding, builds, Actions, artifact upload or physical testing by itself. Those require the separately approved implementation phase under the project's change-control rules.

## 9. Current disposition

- **G1-D:** OPEN. The owner-created Indonesian `ArgusID.txt` and `DigiTransID.zip` assets and owner-created English `Argus_story_Eng.txt` are available as separate starter-corpus sources. Importer-generated SHA-256/split manifests and the explicit tokenizer-frequency purpose are not yet recorded; the product-language/unsupported-language policy and final tokenizer/runtime/API/ABI profile contract also remain unresolved. The initial tokenizer sweep remains T0 raw bytes, T1 byte-level BPE, T2 SentencePiece BPE and T3 SentencePiece Unigram; T1-P PA-BPE is conditional and T4 WordPiece is deferred from the minimum sweep.
- **G2:** CLOSED only for the four-family comparison shortlist.
- **G3:** CLOSED only for the component disposition planning baseline; DEFER remains DEFER.
- **G4:** BLOCKED. A Transformer-first, single-family profile portfolio remains a preliminary engineering recommendation, not a selected ADR. Documents 39 and 42 now define the semantic reference, staged ablations, first-pass Transformer parameter/FLOP/memory equations, M0/MW/ML/MWL shape hypotheses, serialized/overlap execution envelopes and phase-specific resource-sweep method. Exact tokenizer/vocabulary, graph dimensions/components, per-device process/storage inputs, Vulkan feature/driver and per-operator workspace/throughput evidence, optional NPU coverage, optimizer/precision and final comparative rejection record remain incomplete.
- **Backbone/profile/component:** none approved for implementation planning.
- **Implementation/build/CI/physical testing:** not performed or authorized by this document.

## 10. Source-of-truth documents

- [01 — System Requirements and Decisions](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md): accepted product boundary and ADRs.
- [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md): first-launch construction and readiness.
- [21 — Device-Adaptive Model and Training](21_DEVICE_ADAPTIVE_MODEL_AND_TRAINING.md): capability/resource adaptation.
- [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md): feasibility evidence and later E1–E7.
- [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md): G1-D and profile/rights/runtime.
- [35 — Android Architecture Evidence and Component Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md): G2/G3.
- [36 — Pre-Implementation Module and Lifecycle Contracts](36_PRE_IMPLEMENTATION_MODULE_AND_LIFECYCLE_CONTRACTS.md): architecture-neutral module/checkpoint contract.
- [37 — Optimizer Research and Lion Assessment](37_OPTIMIZER_RESEARCH_AND_LION_ASSESSMENT.md): optimizer evidence and state.
- [38 — G2 Architecture Comparative Synthesis](38_G2_ARCHITECTURE_COMPARATIVE_SYNTHESIS.md): cross-family evidence.
- [39 — Transformer Component Research](39_TRANSFORMER_COMPONENT_RESEARCH_AND_DESIGN_CONTRACTS.md): Transformer component choices.
- [40 — Initialization and Width-Scaling Research](40_INITIALIZATION_AND_WIDTH_SCALING_RESEARCH.md): initialization and scaling.
- [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md): Mamba/RWKV state and recovery.
- [42 — Architecture-Family Tensor and Operator Inventory](42_ARCHITECTURE_TENSOR_AND_OPERATOR_INVENTORY.md): symbolic parameter/operator/memory accounting.
