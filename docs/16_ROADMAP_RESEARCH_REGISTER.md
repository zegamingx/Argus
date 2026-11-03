# Roadmap and Technology Research Register

**Review date:** 2026-11-04 · **Status:** Planning baseline · **Branch:** main

## 1. Accepted product direction

Argus follows **Local Knowledge Genesis** and **One Origin, Independent Evolution**:

- Each installation creates its own model parameters locally.
- First launch performs model construction and initialization, then opens the application.
- General-corpus learning is a resumable background process, not a first-launch blocking task.
- Small C4 is the initial bundled bootstrap-corpus candidate, pending source/license/quality acceptance.
- Personal learning adapts to the user only through explicitly authorized data.
- Model configuration follows device capability; training workload follows live device conditions.
- The initial neural architecture remains subject to research.
- No central training server, federated training, cross-user model sharing or automatic model synchronization is planned.
- Each instance is intended to acquire permitted public knowledge for its own local use; accepted knowledge supports retrieval first and enters neural training only through a separate quality, rights and evaluation gate.

## 2. Phases

### Phase 0 — Documentation and research (current)
Maintain the source/build baseline and P0 audit. Complete architecture-family research, tokenizer research, corpus provenance/quality review, Genesis lifecycle, device capability and adaptive training specifications, and the autonomous acquisition source/policy contract, including the initial knowledge taxonomy, discovery-priority model, source registry and connector boundaries. No source implementation, build or workflow execution belongs to this phase unless separately authorized. External papers and benchmarks are evidence for existing research items, not permission to expand scope or reverse accepted product decisions. Record their applicability limits in the existing research documents; do not create a new workstream for every publication.

### Phase 1 — Architecture decision review (not started)
Choose the first architecture only after comparative evidence. Approve tokenizer contract, Small C4 revision and preprocessing, per-device configuration policy, training objective/optimizer profile, Genesis readiness contract, autonomous acquisition source eligibility and local data lifecycle, knowledge-taxonomy calibration, connector contracts, named source allowlist and source-specific rights review, activation decisions for retrieval versus training, open-text source selection and item-level license audit, curated seed ingestion/retrieval pilot, domain-balanced text expansion and source diversity review, Indonesian-language OER source expansion, personal data lifecycle and verification plan. Record decisions in ADRs.

### Phase 2 — Implementation planning (not started)
Create a dependency-ordered engineering plan with acceptance criteria, numerical tests, Android lifecycle tests, migration and rollback. Resolve how the current Kotlin prototype is isolated, replaced or retained for limited experiments. No broad implementation begins before P0 remediation is reviewed.

### Phase 3 — Genesis foundation implementation (not authorized)
Implement device profiling, architecture graph/registry, module initialization policies, model identity, complete model-state persistence and first-launch readiness. No pretraining is required before application unlock.

### Phase 4 — General background learning (not authorized)
Implement bundled-corpus ingestion, tokenizer, from-scratch training, scheduler, checkpoint/recovery and general evaluation. In a separate workstream, implement bounded public-source acquisition, local knowledge storage and retrieval, then a rights-checked training-corpus handoff. Background work begins only after initial Genesis commit and app readiness.

### Phase 5 — Personal learning (not authorized)
Implement inspectable memory, personal candidate consent/review, privacy controls, personal training and general-capability retention gates.

### Phase 6 — Architecture evolution (future research)
Investigate parameter growth, module addition/replacement, architecture migration, validation and rollback. Structural evolution is not automatic and cannot publish an unvalidated candidate.

## 3. Research register

| ID | Topic | Position | Follow-up |
|---|---|---|---|
| R-001 | Android LiteRT CompiledModel | ASSESS | Recheck API maturity, device coverage and NPU route |
| R-002 | Qualcomm QAIRT/QNN/GENIE | ASSESS | Verify licensing, APK packaging and exact SoC support |
| R-003 | MediaTek NeuroPilot | ASSESS | Verify current APK-accessible route and device coverage |
| R-004 | NNAPI lifecycle | HOLD for new design | Retain only justified legacy compatibility; recheck upstream lifecycle |
| R-005 | XRING ecosystem | HOLD | Require public runtime, compiler and third-party app access evidence |
| R-006 | UNISOC accelerator access | ASSESS | Separate CPU/GPU fallback from proven NPU support |
| R-007 | Embedded stacks | FUTURE RESEARCH | Maintain board-specific runtime profiles |
| R-008 | Arm SVE/SME and RISC-V Vector | FUTURE RESEARCH | Track shipping hardware and toolchain maturity |
| R-009 | Desktop AI compute | FUTURE RESEARCH | Separate platform adapters |
| R-010 | On-device training | ASSESS | Use E-001 through E-005 as prior evidence; quantify from-scratch feasibility separately from fine-tuning, including complete backward/optimizer memory, sustainable throughput, energy and thermal limits |
| R-011 | Decoder-only Transformer | RESEARCH CANDIDATE | Use E-006 and E-007 only within their scale/data limits; measure correctness, mobile memory, context cost and runtime coverage on a common workload |
| R-012 | State-space / recurrent sequence models | RESEARCH CANDIDATE | Compare against Transformer using the E-007 evidence as a server-scale reference only; measure sequence scaling, training stability, kernel maturity and inference state on target-class devices |
| R-013 | Hybrid neural architectures | RESEARCH CANDIDATE | Treat E-007 as motivation, not a mobile result; measure integration overhead and device-level outcomes at Argus-relevant model and token budgets |
| R-014 | Small C4 bootstrap corpus | CANDIDATE | Pin revision, inspect inherited C4/Common Crawl terms, re-evaluate split integrity and calculate tokens |
| R-015 | Tokenizer family | OPEN | Use E-008 through E-010 to define multilingual fairness, vocabulary-size and pre-tokenizer checks; compare candidate tokenizers on Argus language/domain fixtures rather than adopting published thresholds as universal rules |
| R-016 | Device-adaptive training controller | OPEN | Define measured capability profile and dynamic resource admission |
| R-017 | Architecture evolution | FUTURE RESEARCH | Define migration, candidate validation and rollback before structural mutation |
| R-018 | Autonomous knowledge acquisition | ACCEPTED DESIGN / OPEN IMPLEMENTATION | Define source allowlist, rights policy, deterministic bootstrap discovery, local storage, quotas, Android scheduling and quality gates |
| R-019 | Knowledge domain taxonomy and discovery planner | ACCEPTED INITIAL TAXONOMY / OPEN CALIBRATION | Validate sixteen-domain hierarchy, language coverage, breadth scheduling, topic-priority weights and source coverage |
| R-020 | Source registry and connector architecture | ACCEPTED ARCHITECTURE / OPEN SOURCE REVIEW | Define operation-scoped permissions, initial named source allowlist, connector contracts, provider costs and per-source terms |
| R-021 | Initial named source candidates | RESEARCH SHORTLIST / NOT ACTIVATED | Verify exact endpoints, current terms, item-level rights, API credentials, request limits and retrieval/training scope before enabling each source |
| R-022 | Open-text and educational source candidates | RESEARCH SHORTLIST / NOT ACTIVATED | Select item-level CC0/CC BY texts, review SA/NC restrictions, establish Indonesian-language OER/legal-source pathways and resolve model-training obligations |
| R-023 | Curated initial text seed set | RETRIEVAL PILOT CANDIDATE / TRAINING REVIEW PENDING | Validate exact title editions, landing-page license evidence, PDF hashes, third-party assets, extraction quality and cross-domain coverage before corpus inclusion |
| R-024 | Domain-balanced text source expansion | RESEARCH SHORTLIST / NOT ACTIVATED | Validate science, mathematics, geography, philosophy and history textbooks; measure token/domain balance and item-level rights before ingestion |
| R-025 | Indonesian-language OER source expansion | RESEARCH SHORTLIST / NOT ACTIVATED | Verify individual ITATS OER records, discover program-specific SPADA IMD outputs, diversify beyond technical subjects and audit item-level rights |

## 4. External evidence index

The following publications and technical references inform the existing research register. They do not establish that Argus implements the reported methods, and they do not by themselves select an Argus architecture or authorize a change to Local Knowledge Genesis.

| ID | Reference | What it supports | Applicability limit |
|---|---|---|---|
| E-001 | [MELON: Breaking the Memory Wall for Resource-Efficient On-Device Machine Learning](https://doi.org/10.1145/3498361.3538928), ACM MobiSys 2022 | Mobile DNN training memory techniques, including recomputation, micro-batching and memory adaptation | General DNN evidence; not LLM pretraining from random initialization |
| E-002 | [MobileFineTuner: A Unified End-to-End Framework for Fine-Tuning LLMs on Mobile Phones](https://arxiv.org/abs/2512.08211), 2025 | Full-parameter and parameter-efficient LLM fine-tuning on real mobile phones; system-level memory/energy techniques | Fine-tuning uses pretrained model parameters; it does not demonstrate foundation pretraining from scratch |
| E-003 | [Fine-Tuning a 3B-Parameter LLM on a Smartphone: Characterizing Sustained Training](https://arxiv.org/abs/2610.06325), 2026 | Complete sustained LoRA training measurements, memory, energy and thermal behavior on iPhone 17 Pro | iOS experiment using a quantized frozen pretrained base and adapters; not Android and not from-scratch pretraining |
| E-004 | [On-Device Training with LiteRT](https://developers.google.com/edge/litert/conversion/tensorflow/build/ondevice_training), Google AI Edge documentation | Official Android-capable on-device incremental training workflow and persistence concepts | Demonstrates training of a deployed model with a training signature; not evidence of general-purpose LLM pretraining from random initialization |
| E-005 | [On-device training in TensorFlow Lite](https://blog.tensorflow.org/2021/11/on-device-training-in-tensorflow-lite.html), TensorFlow team, 2021 | Historical engineering guidance distinguishing practical transfer learning/retraining from the data and compute demands of training from scratch | Dated guidance for the TensorFlow Lite stack; use as a caution, not a current quantitative feasibility result |
| E-006 | [TinyStories: How Small Can Language Models Be and Still Speak Coherent English?](https://www.microsoft.com/en-us/research/publication/tinystories-how-small-can-language-models-be-and-still-speak-coherent-english/), Microsoft Research, 2023 | Small models below 10M parameters can learn coherent generation on a deliberately narrow synthetic story distribution | Does not demonstrate broad general knowledge or smartphone training |
| E-007 | [An Empirical Study of Mamba-based Language Models](https://research.nvidia.com/publication/2024-06_empirical-study-mamba-based-language-models), NVIDIA and collaborators, 2024 | Controlled 8B Transformer, Mamba-2 and Hybrid comparison at up to 3.5T training tokens | Server-scale evidence; not directly transferable to small from-scratch Android workloads |
| E-008 | [Equity with Efficiency: An Empirical Study of Tokenizers for Multilingual Large Language Models](https://aclanthology.org/2026.emnlp-main.1783/), EMNLP 2026 | Tokenizer efficiency and language parity across 11 Southeast Asian languages, including a controlled 1.5B-parameter evaluation | Large-model training evidence; tokenizer outcomes must be remeasured on Argus's own data and scale |
| E-009 | [Scaling Laws or Threshold Effects: Exploring the Optimal Vocabulary Size for Balancing Performance and Efficiency in Low-Resource Languages](https://aclanthology.org/2026.findings-acl.1588/), Findings of ACL 2026 | Evidence that BBPE vocabulary-size behavior can be non-monotonic for the studied Mongolian, Tibetan and Uyghur setup | The reported 9K threshold is study-specific, not a universal minimum vocabulary rule |
| E-010 | [Vowel Signs Are Not Letters: A Pre-tokenization Ceiling on Multilingual Tokenizer Fertility](https://arxiv.org/abs/2608.26449), 2026 | Shows that a letters-only pre-tokenizer regex can impose structural fertility penalties on abugida scripts | Script-specific finding; use to require pre-tokenizer audits, not to assume the same penalty for Indonesian or English |

**Evidence rule:** distinguish (a) model inference, (b) fine-tuning/adaptation, (c) narrow-domain from-scratch learning, and (d) broad-language pretraining from random initialization. Evidence in one category must not be cited as proof of another.

## 5. Technology radar

- **ADOPT:** selected for a defined scope with evidence.
- **TRIAL:** limited proof-of-concept planned or underway.
- **ASSESS:** investigate; no commitment.
- **HOLD:** blocked by maturity, access, licensing or resource limits.
- **RETIRE:** remove an existing path under a migration plan.

Tags apply to a specific version and platform, not globally to a vendor or architecture family.

## 6. Review cadence and exit conditions

Review SDKs before implementation decisions; hardware entries at least quarterly; Android API lifecycle at major platform releases; and corpus terms before every distribution revision. Record source URL, revision/publication date and last-checked date.

End the documentation phase only when the gate in [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md) passes. Completing a document or checking a roadmap item does not authorize coding, builds, CI workflows or artifact publication.
