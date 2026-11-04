# System Requirements and Initial Architecture Decisions

**Status:** Mixed baseline — requirements remain draft; each ADR's explicit status is authoritative. ADR-001 and ADR-009–023 record accepted product/design directions; ADR-002–008 remain proposed.  
**Last reviewed:** 2026-11-03

## 1. Product and system boundary

Argus aims to coordinate local AI capabilities under user control. It is not inherently a cloud LLM client, a universal hardware driver, a replacement for OS security, a guarantee of universal accelerator support, or a full-model training system on every phone.

Local inference and private memory are the default. User-requested web retrieval or model downloads may use the network, but must be explicit, purpose-limited and visible. Private captures, chats, files, weights and memory must not be silently uploaded.

## 2. Requirement rules

Every requirement needs a unique ID, rationale, priority, platform, dependencies, measurable acceptance criteria, verification method and status. Avoid "fast", "smart" or "supports all devices" without a testable definition.

Priority: **MUST** release-blocking; **SHOULD** strong default with documented exception; **MAY** optional.

## 3. Initial requirements

### Product and operation
- **SYS-001 MUST:** Core local tasks remain usable with networking disabled.
- **SYS-002 MUST:** Network tasks are user-initiated or explicitly enabled, with destination and purpose disclosed.
- **SYS-003 MUST:** No telemetry or analytics may transmit private user content.
- **SYS-004 MUST:** Users can pause/stop perception, learning and autonomous action from a visible control.
- **SYS-005 MUST:** Consequential external actions require explicit user confirmation.
- **SYS-006 MUST:** Captured/imported content is untrusted input, never trusted instructions.

### Model and compute
- **AI-001 MUST:** Model metadata includes architecture, format, tokenizer, context, quantization, license, checksum and runtime requirements.
- **AI-002 MUST:** Backend selection validates operator/precision support, memory, driver/runtime and device state.
- **AI-003 MUST:** A CPU correctness path exists for the supported model subset.
- **AI-004 MUST:** Runtime telemetry identifies the backend actually used.
- **AI-005 SHOULD:** Inference and training have separate capability and memory budgets.
- **AI-006 MUST:** Unsupported operators fail clearly or use a validated fallback.

### Hardware and platform
- **HW-001 MUST:** Reported hardware identity is distinct from verified accelerator availability.
- **HW-002 MUST:** CPU ISA features are discovered through supported OS/runtime mechanisms, not branding alone.
- **HW-003 MUST:** GPU checks validate compute queues, required features, storage formats and driver behavior.
- **HW-004 MUST:** NPU support is reported per runtime, model and operator set.
- **HW-005 SHOULD:** Device profiles derive from measured resources and thermal behavior.
- **HW-006 MUST:** Vendor SDKs stay behind platform-specific adapters.

### Resources and reliability
- **RES-001 MUST:** Model loading is preflighted against RAM/accelerator-memory budget.
- **RES-002 MUST:** Context, KV cache, batch and concurrency respect memory limits.
- **RES-003 MUST:** Long jobs react to thermal/memory pressure, battery policy and cancellation.
- **REL-001 MUST:** Model/adapter promotion is atomic and recoverable.
- **REL-002 MUST:** Interrupted training cannot corrupt the last known-good checkpoint.
- **REL-003 SHOULD:** Failures expose actionable reason codes.

### Security and governance
- **SEC-001 MUST:** Data classes, retention, access and deletion are documented.
- **SEC-002 MUST:** Secrets and signing materials never enter source control.
- **SEC-003 MUST:** Capture obeys platform consent, indicators, secure-surface rules and revocation.
- **SEC-004 MUST:** Model and memory artifacts are integrity-checked.
- **SEC-005 SHOULD:** Sensitive data at rest uses platform-backed encryption with documented key lifecycle.

## 4. Quantitative requirements

Universal tokens/sec, memory, temperature and battery limits are intentionally TBD. Define workload and device tiers first, then set measurable thresholds using the benchmark method in [14 — Performance and Power](14_PERFORMANCE_POWER_THERMAL.md).

## 5. Initial Architecture Decision Records

### ADR-001 — Documentation-first
**Status:** Accepted working rule for this planning phase. Build and review documentation on `main` before implementation. No source-code changes are authorized by this ADR.

### ADR-002 — Vendor-independent hardware abstraction
**Status:** Proposed. Shared logic depends on capability interfaces; vendor runtimes stay in adapters. This costs more discovery/test work but reduces vendor lock-in.

### ADR-003 — Report verified execution only
**Status:** Proposed. An NPU/GPU label requires successful representative model execution and output validation, not just SoC identification.

### ADR-004 — Separate inference and training capability
**Status:** Proposed. Forward inference does not prove support for gradients, backward ops or optimizer state.

### ADR-005 — NNAPI is not the long-term Android foundation
**Status:** Proposed. Prioritize current LiteRT APIs and validated vendor routes; retain NNAPI only as justified legacy compatibility. NNAPI was deprecated in Android 15; recheck upstream lifecycle before coding.

### ADR-006 — Explicit network exceptions
**Status:** Proposed. Local operation is default; web fetch/model download are distinct user-controlled network actions. Product wording must accurately explain this.

### ADR-007 — No performance promise before representative tests
**Status:** Proposed. Establish targets by device tier and workload after defining reproducible benchmarks.

### ADR-008 — Platform adapters are not interchangeable
**Status:** Proposed. Android, embedded Linux, desktop and Apple deployments need distinct OS/driver/packaging integrations.

## 6. Open decisions

Minimum RAM; initial ABI scope; first-class model/task families; persistent-data defaults and retention; whether first release permits only adapter training; signed model manifests; platform support tiers; and acceptance threshold for thermal stability.

## 7. Accepted Argus Genesis decisions — 2026-11-03

The following decisions supersede earlier open assumptions about importing a pretrained base model or selecting one fixed model size.

### ADR-009 — Local Knowledge Genesis
**Status:** Accepted direction.

Each Argus installation creates its own model state locally from architecture-specific random initialization. An externally pretrained model is not the foundation or identity of Argus. The initial model is not considered language-capable merely because it has been initialized; capability develops through training.

### ADR-010 — First-launch creation without training lockout
**Status:** Accepted direction.

First launch performs device assessment, configuration selection, model construction, parameter initialization and durable registration of the initial generation. The application becomes available after those steps. General-corpus pretraining is not a prerequisite for opening the application and must not run as a blocking splash-screen task.

### ADR-011 — Bundled bootstrap corpus
**Status:** Accepted direction; source acceptance gate remains open.

General foundation data is bundled for offline availability. Small C4 is the current initial bootstrap candidate, not the complete lifelong knowledge source. Exact source revision, license terms, attribution, data quality, split integrity and package checksums must be verified before distribution.

### ADR-012 — Independent model instances
**Status:** Accepted direction.

Each user's model state and evolution remain local to that instance. No central learning server, federated learning, shared gradient updates, automatic model synchronization or cross-user weight exchange is part of the design.

### ADR-013 — Device-adaptive model and training
**Status:** Accepted direction.

The initial model configuration is selected from measured device capability. During operation, training workload adapts to current memory, thermal, power, backend and Android lifecycle conditions. There is no universal parameter count or fixed training batch for all devices.

### ADR-014 — Architecture research before selection
**Status:** Accepted direction.

The initial neural architecture family remains undecided. Transformer, state-space/recurrent and hybrid families are research candidates. No candidate becomes the Argus architecture without comparative evidence and a reviewed decision record.

### ADR-015 — Separate general learning and personal learning
**Status:** Accepted direction.

Bundled general learning may be scheduled in the background after Genesis readiness. Personal learning uses only data covered by explicit user permission and a reviewable provenance/retention lifecycle. Memory updates and parameter updates are separate operations.

### ADR-016 — Instance-owned autonomous knowledge acquisition
**Status:** Accepted product direction; implementation and source policy remain open.

Each Argus installation is intended to discover, collect, validate and retain permitted public knowledge for its own local instance. The acquisition engine is not a data-collection service for external AI customers and does not pool results across installations. Acquisition starts with deterministic topic policies and a reviewed source registry; model-assisted discovery is enabled only after defined quality gates. Retrieved knowledge and neural training remain separate operations.

### ADR-017 — Broad, extensible general-knowledge taxonomy
**Status:** Accepted initial taxonomy; numerical scheduling calibration remains open.

The initial knowledge map contains sixteen broad domains and supports hierarchical subdomains, topics, prerequisites and cross-domain relationships. General acquisition prioritizes reliable breadth across the taxonomy before excessive depth in a narrow domain. The taxonomy is versioned and expandable; it is not a fixed limit on future knowledge.

### ADR-018 — Operation-scoped source permissions and replaceable connectors
**Status:** Accepted architecture direction; source allowlist and provider choices remain open.

Autonomous acquisition uses a local, versioned source registry and replaceable connector interfaces. Permissions are evaluated independently for discovery, fetch, transient processing, full-text retention, excerpt/metadata retention, retrieval indexing, neural training and redistribution. Unknown permission fails closed for the affected operation. Connectors cannot grant or broaden permissions. Initial collection uses bounded direct requests through approved connectors, not a shared residential-proxy network or third-party customer traffic.

### ADR-019 — Open-text source and training eligibility separation
**Status:** Accepted governance direction; item-level rights and model-training legal review remain required.

Open-text catalogs and educational repositories are used for discovery, but full-text fetch, local retention, retrieval indexing, neural training and redistribution require separate item-level permissions. Free-to-read, downloadable or open-access labels are not training authorization. Non-commercial and share-alike content is held from default general training until Argus's distribution model and use-specific legal obligations are resolved. No collection-level label overrides a more restrictive item-level notice.

### ADR-020 — Curated item-level text seed before bulk ingestion
**Status:** Accepted pilot direction; selected items remain unapproved for neural training.

The first document-ingestion pilot uses exact, individually identified books and modules with recorded landing-page rights evidence, source URLs, file checksums and attribution. It validates parsing, language/domain classification, local storage and retrieval before any training corpus is enabled. Catalog-level "open" status is not enough; every work and its third-party components require item-level review.

### ADR-021 — Domain-balanced text acquisition
**Status:** Accepted discovery direction; individual source activation remains open.

The text-acquisition plan must expand beyond computing and information systems. The candidate pool should include mathematics, physical and life sciences, geography, history, philosophy and other underrepresented domains. Selection is driven by the sixteen-domain coverage map, item-level rights and evidence quality. A title's CC BY declaration does not automatically approve neural training or remove the need to inspect third-party components.

### ADR-022 — Indonesian-language source diversity
**Status:** Accepted discovery direction; candidate institutions and item activation remain open.

Argus must deliberately seek Indonesian-language material beyond technical/programming sources. Institutional OER catalogs and program-scoped grants are discovery channels, not blanket collection licenses. Each title requires individual source provenance, license evidence, third-party-content review and separate retrieval/training permissions. No single institution's repository is considered representative of Indonesian language or knowledge diversity.

### ADR-023 — Device-adaptive knowledge acquisition
**Status:** Accepted architecture direction; resource budgets and thresholds remain open.

The autonomous acquisition pipeline adapts to each instance's measured device capability and live operating conditions, separately from model configuration. Each instance shares the broad sixteen-domain coverage objectives and minimum rights/quality requirements, while download volume, concurrency, parsing, retention, indexing, tokenization batches and background scheduling remain within a device-specific resource profile. Low-capability devices progress through smaller resumable work units; they do not receive weaker provenance, security, rights or data-quality checks. Model and acquisition resource budgets must not be double-counted.

## 8. Revised open decisions

These items remain open; they must not be filled with assumed values:

- Initial architecture family and first supported module set.
- Tokenizer family, vocabulary policy and Unicode behavior.
- Exact Small C4 revision and corpus acceptance/redistribution review.
- Preprocessing, deduplication, train/validation/test partition and language diagnostics.
- Device capability benchmark protocol and admission thresholds.
- Architecture-specific initializer policies.
- Optimizer, loss, sequence packing, batch and training hyperparameters.
- Android background scheduling, foreground-service use and user-facing controls.
- Checkpoint cadence, crash recovery and model-generation promotion policy.
- Personal-learning consent wording, candidate retention and post-training revocation strategy.
- Criteria and migration rules for later architecture evolution.
- Domain-specific source registry, language and regional coverage priorities.
- Topic-priority weights, freshness intervals and breadth-scheduling calibration for autonomous acquisition.
- Initial named source allowlist, source-specific use rights and discovery-provider selection.
- Which candidate sources may be activated for retrieval versus neural training, after item-level rights and provider terms review.
- Argus commercial-distribution decision and use-specific model-training review for CC BY, CC BY-SA, CC BY-NC and CC BY-NC-SA materials.
- Exact item-level rights for open textbooks, OER, public-domain texts and Indonesian educational repositories.
- Curated title-level seed selection and third-party asset audit before full-text acquisition.
- Indonesian OER source diversification beyond information systems, including individual item licenses and program-scoped grant provenance.
- Device-specific acquisition/storage/network/processing budgets and the model-resource reservation policy.
- Common knowledge-coverage objectives versus per-instance acquisition capacity and optional shard selection.

A fixed model parameter count is intentionally not an open global constant: it is an output of architecture research combined with each device's capability profile.

The initial sixteen-domain knowledge taxonomy is defined in [25 — Knowledge Domain and Discovery Strategy](25_KNOWLEDGE_DOMAIN_AND_DISCOVERY_STRATEGY.md). Acquisition priority weights, freshness intervals and per-domain source acceptance remain open for evidence-based calibration.
