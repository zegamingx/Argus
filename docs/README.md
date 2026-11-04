# Argus Engineering Documentation

**Review date:** 2026-11-04 · **Status:** Design baseline · **Branch:** main · **Scope:** Documentation only

This directory is the engineering source of truth for Argus planning. It distinguishes accepted design direction, current repository facts, unresolved research and future implementation. A design document does not prove a capability exists in the application.

The repository-wide authority and change-control rules are defined in [44 — Documentation Governance and Change Control](44_DOCUMENTATION_GOVERNANCE_AND_CHANGE_CONTROL.md). The Android package hierarchy, Gradle module strategy and project file-placement rules are defined in [45 — Android Project Structure and File-Placement Rules](45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md). The numbered files remain physically flat and stable; the workstream grouping below is for navigation and ownership, not a directory migration.

## Project vision

Argus is a personal, local-first intelligence intended to be created on the user's Android device and to evolve independently through local learning and explicitly authorized personal experience.

The accepted principle is **One Origin, Independent Evolution**. Each installation initializes its own model parameters, owns its own model and memory state, and learns without a central training server or cross-user model exchange.

## Accepted design decisions

1. **Local Knowledge Genesis:** initialize Argus model parameters locally; do not use an externally pretrained model as the foundation.
2. **First-launch readiness:** first launch performs device assessment, configuration, model construction, parameter initialization and initial-state persistence. It does not wait for pretraining.
3. **Background general learning:** bundled general-corpus training is eligible to run after the app becomes ready and must be pausable and recoverable.
4. **Bundled bootstrap corpus:** Small C4 is the current initial corpus candidate, pending exact revision, license, provenance, quality and packaging verification.
5. **Personal learning:** adapt to the user's experience over time; access to data is not training consent.
6. **Device adaptation:** initial model configuration follows measured device capability; runtime workload follows live device conditions.
7. **Architecture research first:** no initial neural architecture family has been selected.
8. **Independent instances:** no central learning server, federated training, automatic model synchronization, shared gradients or cross-user weight exchange.
9. **Autonomous knowledge acquisition:** each installation is intended to discover, collect, validate and retain permitted public knowledge for its own local instance; retrieval and neural training remain separate.
10. **Broad knowledge discovery:** the initial general-knowledge taxonomy spans sixteen extensible domains; acquisition prioritizes reliable breadth before excessive depth in a narrow subject.
11. **Device-adaptive knowledge acquisition:** each instance adapts corpus volume, download/parse concurrency, retention, indexing and scheduling to measured device resources while preserving shared quality and coverage principles.

## Documentation rules

1. **Documentation first:** this phase changes documentation only. No source, native code, device probing, runtime, training engine or feature implementation is authorized.
2. **Separate fact from plan:** use IMPLEMENTED, VERIFIED, AVAILABLE, PLANNED, EXPERIMENTAL, FUTURE RESEARCH, DEPRECATED and UNSUPPORTED precisely.
3. **Evidence required:** hardware, SDK and dataset claims need primary sources, version/date and last-verified information.
4. **No chipset-name routing:** identity is only a hint. A backend is usable only after runtime, driver, operator, precision, memory and execution checks.
5. **No false portability:** Android, embedded Linux, desktop and vendor firmware have different APIs, permissions, ABIs and deployment models.
6. **Observable fallback:** report the backend actually used and why fallback occurred. Never simulate acceleration.
7. **Device-adaptive resource policy:** advertised RAM or peak TOPS alone cannot determine a workload budget.
8. **No pretrained foundation:** the accepted Genesis direction creates local weights; third-party pretrained weights are not the Argus foundation.
9. **No cross-instance learning:** no model, gradient, optimizer state or personal training data is shared between user instances.
10. **No unsupported product claims:** the current app remains an early prototype until tests establish the documented behavior.

## Logical workstreams

| Workstream | Documents | Responsibility |
|---|---|---|
| Product charter and system boundaries | 01–02 | Requirements, accepted decisions and system flows |
| Hardware, runtime and platform evidence | 03–08, 12 | Processor/GPU/NPU, SoC, memory, runtime and platform constraints |
| Model, learning, safety and verification foundations | 09–15 | Model, training, knowledge, governance, performance and verification |
| Program control and implementation truth | 16–19 | Roadmap, research register, repository baseline, implementation audit and P0 remediation |
| Genesis and core learning direction | 20–23 | Initial model creation, device adaptation, general/personal learning and architecture research |
| Knowledge acquisition and corpus governance | 24–32 | Discovery, domain coverage, source rights, corpus candidates and device-adaptive acquisition |
| Feasibility and pre-implementation foundations | 33–37 | From-scratch feasibility, tokenizer, Android evidence, module/lifecycle contracts and optimizer research |
| Architecture comparison and G4 preparation | 38–43 | Cross-family synthesis, component/init/state/tensor contracts and G4 framework |
| Documentation governance | 44 | Document authority, evidence/status semantics and change control |
| Android project structure | 45 | Kotlin package ownership, Gradle module strategy, native/JNI boundary and file-placement rules |

The grouping is descriptive only. Each document retains its own declared scope and the authority rules in document 44.

## Complete document index

| Document | Scope |
|---|---|
| [01 — System Requirements and Decisions](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md) | Product constraints, measurable requirements and accepted decisions |
| [02 — System Architecture](02_SYSTEM_ARCHITECTURE.md) | System layers, trust boundaries and data/control flows |
| [03 — Processor Architecture](03_PROCESSOR_ARCHITECTURE.md) | CPU families, ISA, SIMD, topology and detection |
| [04 — GPU Architecture](04_GPU_ARCHITECTURE.md) | GPU families, compute APIs, drivers and constraints |
| [05 — NPU Acceleration](05_NPU_ACCELERATION.md) | NPU/DSP, vendor SDKs, compiler and third-party access |
| [06 — SoC Hardware Matrix](06_SOC_HARDWARE_MATRIX.md) | Mobile, embedded, desktop and server watchlist |
| [07 — Memory and Storage](07_MEMORY_STORAGE_ARCHITECTURE.md) | RAM, cache, bandwidth, KV cache, mmap and persistence |
| [08 — AI Runtime Backends](08_AI_RUNTIME_BACKENDS.md) | LiteRT, QAIRT/QNN, NeuroPilot, Vulkan and other runtimes |
| [09 — Model Architecture](09_MODEL_ARCHITECTURE.md) | Model identity, architecture abstraction, initialization and artifacts |
| [10 — Training and Learning](10_TRAINING_LEARNING_ARCHITECTURE.md) | General learning, personal learning, training controls and validation |
| [11 — Memory and Knowledge](11_MEMORY_KNOWLEDGE_ARCHITECTURE.md) | Inspectable memory and its boundary with model parameters |
| [12 — Platform Expansion](12_PLATFORM_EXPANSION.md) | Android, SBC, embedded Linux, desktop and future platforms |
| [13 — Security and Data Governance](13_SECURITY_PRIVACY_DATA_GOVERNANCE.md) | Threat model, consent, secrets, network and deletion |
| [14 — Performance and Power](14_PERFORMANCE_POWER_THERMAL.md) | Benchmarks, memory budgets, thermal and energy |
| [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md) | Tests, evidence and implementation completion gate |
| [16 — Roadmap and Research Register](16_ROADMAP_RESEARCH_REGISTER.md) | Phases, technology radar and open research |
| [17 — Current Repository Baseline](17_CURRENT_REPOSITORY_BASELINE.md) | Inspected facts and current implementation gaps |
| [18 — Implementation Audit](18_IMPLEMENTATION_AUDIT.md) | Static P0/P1/P2 source findings |
| [19 — P0 Remediation Design](19_P0_REMEDIATION_DESIGN.md) | Correctness, checkpoint and consent acceptance contracts |
| [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md) | First-launch construction, initialization and readiness |
| [21 — Device-Adaptive Model and Training](21_DEVICE_ADAPTIVE_MODEL_AND_TRAINING.md) | Device profiling, configuration and live workload control |
| [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md) | Small C4 bootstrap, data governance and personal adaptation |
| [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md) | Candidate families and evidence-based selection |
| [24 — Autonomous Knowledge Acquisition](24_AUTONOMOUS_KNOWLEDGE_ACQUISITION.md) | Self-directed public-source discovery, source policy, local knowledge and training handoff |
| [25 — Knowledge Domain and Discovery Strategy](25_KNOWLEDGE_DOMAIN_AND_DISCOVERY_STRATEGY.md) | Initial taxonomy, coverage states, breadth-to-depth planning and topic prioritization |
| [26 — Source Registry and Connector Architecture](26_SOURCE_REGISTRY_AND_CONNECTOR_ARCHITECTURE.md) | Source permissions, connector types, fetch-job contracts and network safety |
| [27 — Initial Source Registry Candidates](27_INITIAL_SOURCE_REGISTRY_CANDIDATES.md) | Initial named-source shortlist, item-level rights, API constraints and domain coverage |
| [28 — Open Text and Educational Source Candidates](28_OPEN_TEXT_AND_EDUCATIONAL_SOURCE_CANDIDATES.md) | Full-text OER candidates, content-rights matrix and Indonesian-language source strategy |
| [29 — Curated Initial Text Seed Set](29_CURATED_INITIAL_TEXT_SEED_SET.md) | Exact title-level seed candidates, license evidence, PDF sources and retrieval-pilot selection |
| [30 — Domain-Balanced Text Source Expansion](30_DOMAIN_BALANCED_TEXT_SOURCE_EXPANSION.md) | Additional full-text candidates mapped to underserved knowledge domains |
| [31 — Indonesian-Language OER Source Expansion](31_INDONESIAN_LANGUAGE_OER_SOURCE_EXPANSION.md) | Indonesian OER candidates, item-level rights path and SPADA 2022 program boundary |
| [32 — Device-Adaptive Knowledge Acquisition and Corpus Budgeting](32_DEVICE_ADAPTIVE_KNOWLEDGE_ACQUISITION.md) | Device capability profile, corpus/storage tiers, dynamic admission and verification |
| [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md) | Evidence classification, Android resource model, sustained thermal/energy constraints and E1–E7 feasibility protocol |
| [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md) | Device-conditioned tokenizer profile portfolio, multilingual fixtures, Unicode/special-token conformance, vocabulary/runtime costs and decision gates |
| [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md) | Android prior art, Genesis constraints, component-level disposition, framework reuse and G0–G4 gates |
| [36 — Pre-Implementation Module and Lifecycle Contracts](36_PRE_IMPLEMENTATION_MODULE_AND_LIFECYCLE_CONTRACTS.md) | Architecture-neutral module/API boundaries, data records, state machines, typed errors/events, concurrency, transactions and later verification obligations |
| [37 — Optimizer Research and Lion Assessment](37_OPTIMIZER_RESEARCH_AND_LION_ASSESSMENT.md) | AdamW/Lion/Adafactor comparison, Lion state-memory accounting, device-adaptive batch semantics and exact optimizer recovery |
| [38 — G2 Architecture Comparative Synthesis](38_G2_ARCHITECTURE_COMPARATIVE_SYNTHESIS.md) | Cross-family trade-offs for Transformer, Mamba-2 SSD, RWKV-7 and hybrid; matched comparison rules and G4 deliverables |
| [39 — Transformer Component Research and Pre-G4 Design Contracts](39_TRANSFORMER_COMPONENT_RESEARCH_AND_DESIGN_CONTRACTS.md) | Evidence and matched-comparison contracts for attention variants, position, FFN, normalization/residual topology, depth/width and embeddings |
| [40 — Initialization and Width-Scaling Research](40_INITIALIZATION_AND_WIDTH_SCALING_RESEARCH.md) | Random-initialization contracts, residual/depth scaling, T-Fixup, DeepNorm, effective-theory initialization and μP/μTransfer across device-conditioned model widths |
| [41 — Recurrent State and Chunked-Training Lifecycle Contracts](41_RECURRENT_STATE_AND_CHUNKED_TRAINING_CONTRACTS.md) | Mamba-2 dual-state buffers, RWKV-7 WKV/shift state, chunk/BPTT semantics, document resets and atomic recurrent-state recovery |
| [42 — Architecture-Family Tensor and Operator Inventory](42_ARCHITECTURE_TENSOR_AND_OPERATOR_INVENTORY.md) | Symbolic parameter shapes, forward/backward operators, vocabulary/logit and inference-state memory formulas for Transformer, Mamba-2, RWKV-7 and hybrid profiles |
| [43 — G4 Architecture Decision and Device-Profile Contract](43_G4_ARCHITECTURE_DECISION_AND_PROFILE_CONTRACT.md) | Hard-gate/compare method, immutable model-profile schema, multi-profile portfolio decision and G4 ADR acceptance checklist |
| [44 — Documentation Governance and Change Control](44_DOCUMENTATION_GOVERNANCE_AND_CHANGE_CONTROL.md) | Document authority, repository structure, evidence/status rules, ADR process and documentation-only change control |
| [45 — Android Project Structure and File-Placement Rules](45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md) | Target Kotlin package hierarchy, dependency direction, Gradle module extraction criteria, Android/native source boundaries, tests and resource placement |

## Status vocabulary

- **IMPLEMENTED:** code exists; correctness is not implied.
- **VERIFIED:** a defined test passed on a named device/software configuration.
- **AVAILABLE:** a public standard, product, API or SDK exists; Argus integration is not implied.
- **PLANNED:** accepted design intent, not implemented.
- **EXPERIMENTAL:** prototype or limited integration, not guaranteed.
- **FUTURE RESEARCH:** worth monitoring; no delivery commitment.
- **DEPRECATED:** upstream discourages or sunsets it.
- **UNSUPPORTED:** explicitly outside the current support scope.

## Change control and completion

Material decisions belong in [01 — System Requirements and Decisions](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md). Unresolved questions and phase/gate status belong in [16 — Roadmap and Research Register](16_ROADMAP_RESEARCH_REGISTER.md). Repository-wide authority, evidence, status and change rules belong in [44 — Documentation Governance and Change Control](44_DOCUMENTATION_GOVERNANCE_AND_CHANGE_CONTROL.md). Detailed Genesis, device adaptation, corpus, architecture, autonomous acquisition, knowledge-domain, source-connector, source-candidate, open-text, curated seed, domain-balanced text, Indonesian OER, device-adaptive acquisition, from-scratch feasibility, tokenizer, component-level Android architecture, cross-system implementation contracts, optimizer research, G2 synthesis, Transformer component comparisons, initialization/width-scaling research and recurrent-state lifecycle contracts are maintained in documents 20–43.

The documentation phase ends only when requirements, architecture, model and tokenizer research, corpus governance, resource budgets, cross-system module/lifecycle contracts, verification strategy and cross-document consistency have been reviewed. The contracts in document 36 are a proposed baseline and remain subject to ADR reconciliation and explicit closure of their open bindings. Passing that gate authorizes an implementation-plan review only. It does not automatically authorize coding, builds or workflow execution.
