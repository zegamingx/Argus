# ARGUS

### One Origin. Independent Evolution.

Argus is an experimental, local-first Android AI research project exploring a personal intelligence created and developed on the user's own device.

The intended direction is **Local Knowledge Genesis**: Argus creates its own model instance from locally initialized parameters instead of adopting an externally pretrained model as its identity. Each installation develops independently from its own origin, local general-learning material and explicitly authorized personal experience.

> **Research status:** the repository currently contains an early Kotlin prototype. The Genesis Engine, reliable from-scratch training, adaptive training controller, persistent model generations, production hardware acceleration and architecture evolution are design targets, not verified capabilities.

## Accepted product direction

| Argus is intended to be | Argus is not intended to be |
|---|---|
| A personal model instance created locally on first launch | A wrapper around a downloaded pretrained model |
| Local-first, with the user's model state kept on that device | A central training service or shared global model |
| Independently evolved by each user | A system that exchanges weights, gradients or private training data between users |
| Adapted to the device's measured capability and current operating conditions | A fixed-size model or fixed training workload for every phone |
| Initialized before the user enters the app, then trained in the background | A first-launch screen that blocks until pretraining finishes |
| Capable of separating general learning, personal memory and personal parameter learning | A silent collector that treats every interaction as training consent |

These are approved architectural intentions. They do not mean the current application already implements them.

## Local Knowledge Genesis

The first-launch sequence is deliberately short and does not include a full training run:

1. Assess the device's available compute and memory capabilities.
2. Select a viable configuration from architecture research and the device budget.
3. Construct the model structure and initialize its parameters locally.
4. Persist a valid initial model identity and generation manifest.
5. Mark Genesis initialization complete and open the application.

**The application must not wait for general-corpus pretraining to finish.** General learning is scheduled after the application becomes available. It runs in the background, can be paused, and must recover from Android process or scheduling interruptions.

A newly initialized model is not already a competent language assistant. Random initialization creates the model's starting state; training data and computation develop its capabilities over time.

See [Argus Genesis Engine](docs/20_ARGUS_GENESIS_ENGINE.md).

## Autonomous knowledge acquisition

After Genesis, each Argus instance is intended to discover and collect permitted public knowledge for its own local use. It processes sources, checks provenance and quality, stores accepted knowledge locally, and makes it available through retrieval. Eligible material may later enter a separate, versioned general-training pipeline.

The initial collector does not depend on an immature neural model: deterministic topic policies and a reviewed source registry provide the starting point. Once the model and retrieval path pass defined quality gates, the model may propose new topics and knowledge gaps. Policy checks, source restrictions, resource budgets and user controls remain authoritative.

This is not a third-party data-collection service or a shared dataset network. Each installation owns its acquisition state and knowledge. Public-source retrieval does not authorize uploading personal conversations, local files, model weights or checkpoints. Collected information can be used for retrieval without changing model parameters; neural training remains a separate quality- and evaluation-gated operation.

The acquisition workload is also device-adaptive. The same broad knowledge-coverage objectives apply across installations, while download volume, concurrency, parsing, retention, indexing and background scheduling adapt to each device's measured memory, compute, storage, network, power and thermal conditions. Lower-capability devices progress in smaller resumable batches; quality and rights checks are never weakened to save resources.

See [Device-Adaptive Knowledge Acquisition and Corpus Budgeting](docs/32_DEVICE_ADAPTIVE_KNOWLEDGE_ACQUISITION.md).

See [Autonomous Knowledge Acquisition](docs/24_AUTONOMOUS_KNOWLEDGE_ACQUISITION.md), [Knowledge Domain and Discovery Strategy](docs/25_KNOWLEDGE_DOMAIN_AND_DISCOVERY_STRATEGY.md), and [Source Registry and Connector Architecture](docs/26_SOURCE_REGISTRY_AND_CONNECTOR_ARCHITECTURE.md), with an initial shortlist in [Initial Source Registry Candidates](docs/27_INITIAL_SOURCE_REGISTRY_CANDIDATES.md) and [Open Text and Educational Source Candidates](docs/28_OPEN_TEXT_AND_EDUCATIONAL_SOURCE_CANDIDATES.md), with a concrete sample in [Curated Initial Text Seed Set](docs/29_CURATED_INITIAL_TEXT_SEED_SET.md) and [Domain-Balanced Text Source Expansion](docs/30_DOMAIN_BALANCED_TEXT_SOURCE_EXPANSION.md), plus [Indonesian-Language OER Source Expansion](docs/31_INDONESIAN_LANGUAGE_OER_SOURCE_EXPANSION.md).

## Device-adaptive model and training

Argus does not have one universal parameter count, layer count, batch size or training duration.

The device capability profile determines the feasible initial model configuration. During later work, a separate training controller adapts workload to live conditions such as memory pressure, thermal status, battery policy, actual compute backend, storage and foreground/background restrictions.

The controller may adjust bounded workload parameters or pause and resume work. It must not mutate model architecture or optimizer semantics casually in the middle of a training session. Any structural model change is a separately validated generation transition.

See [Device-Adaptive Model and Training](docs/21_DEVICE_ADAPTIVE_MODEL_AND_TRAINING.md).

## General learning and personal evolution

Argus uses two distinct learning paths:

- **General foundation learning:** a bundled offline bootstrap corpus provides initial general text exposure. Small C4 is the current bootstrap candidate, subject to revision pinning, provenance, license review, quality filtering and packaging verification. It is not a pretrained model and is not intended to contain all future knowledge.
- **Personal learning:** the instance may adapt to the owner's language, corrections, preferences and selected information over time. Access to a conversation, file, screen or sensor does not itself authorize retention or training. Personal parameter updates require explicit permission and a reviewable data lifecycle.

Memory retrieval and model-parameter learning are separate mechanisms. Memory can preserve inspectable facts without immediately changing model weights.

See [General Corpus and Personal Learning](docs/22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md).

## Neural architecture research

The initial neural architecture has **not** been selected. Transformer, state-space/recurrent and hybrid families are research candidates, not settled implementation choices. Argus should keep model description, tensor contracts, module registration and execution backends sufficiently decoupled to permit future research, while requiring explicit compatibility, migration and evaluation for every architecture change.

See [Neural Architecture Research Plan](docs/23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md).

## Individual intelligence and privacy boundaries

- Every installation receives a distinct local instance identity and independently initialized parameters.
- There is no central training server, cross-user model aggregation, federated learning, automatic model synchronization or exchange of gradients.
- User data, memory, checkpoints and model weights remain local by default.
- User-triggered web retrieval is a separate network operation and must disclose its destination and purpose.
- Personal training sources require consent, provenance, classification, appropriate redaction, review, revocation and deletion controls.
- Bundled corpus data is governed by its own provenance and license obligations; it is not user personal data.

## Engineering documentation

The documents under [docs/](docs/README.md) are the planning source of truth. They distinguish accepted direction, research questions, existing implementation facts and future work. A design document does not prove that a feature exists in code.

| Document | Purpose |
|---|---|
| [01 — System Requirements and Decisions](docs/01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md) | Requirements and accepted design decisions |
| [02 — System Architecture](docs/02_SYSTEM_ARCHITECTURE.md) | System boundaries and data/control flows |
| [17 — Current Repository Baseline](docs/17_CURRENT_REPOSITORY_BASELINE.md) | Inspected implementation facts and gaps |
| [18 — Implementation Audit](docs/18_IMPLEMENTATION_AUDIT.md) | Static audit findings |
| [19 — P0 Remediation Design](docs/19_P0_REMEDIATION_DESIGN.md) | Correctness and privacy remediation contracts |
| [20 — Argus Genesis Engine](docs/20_ARGUS_GENESIS_ENGINE.md) | First-launch model creation and readiness |
| [21 — Device-Adaptive Model and Training](docs/21_DEVICE_ADAPTIVE_MODEL_AND_TRAINING.md) | Hardware capability and live workload control |
| [22 — General Corpus and Personal Learning](docs/22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md) | Bundled bootstrap data and user-specific learning |
| [23 — Neural Architecture Research Plan](docs/23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md) | Architecture candidate comparison and selection gates |
| [24 — Autonomous Knowledge Acquisition](docs/24_AUTONOMOUS_KNOWLEDGE_ACQUISITION.md) | Self-directed public-source discovery, local knowledge processing and training handoff |
| [25 — Knowledge Domain and Discovery Strategy](docs/25_KNOWLEDGE_DOMAIN_AND_DISCOVERY_STRATEGY.md) | General knowledge taxonomy, coverage mapping and autonomous topic prioritization |
| [26 — Source Registry and Connector Architecture](docs/26_SOURCE_REGISTRY_AND_CONNECTOR_ARCHITECTURE.md) | Source permissions, connector routing, fetch jobs and network safety |
| [27 — Initial Source Registry Candidates](docs/27_INITIAL_SOURCE_REGISTRY_CANDIDATES.md) | Named source shortlist, licensing constraints and first domain coverage mapping |
| [28 — Open Text and Educational Source Candidates](docs/28_OPEN_TEXT_AND_EDUCATIONAL_SOURCE_CANDIDATES.md) | Full-text OER candidates, item-level rights and Indonesian-language source strategy |
| [29 — Curated Initial Text Seed Set](docs/29_CURATED_INITIAL_TEXT_SEED_SET.md) | Exact English and Indonesian book/module records with license evidence and pilot status |
| [30 — Domain-Balanced Text Source Expansion](docs/30_DOMAIN_BALANCED_TEXT_SOURCE_EXPANSION.md) | Additional CC BY science, mathematics, geography, philosophy and history textbooks |
| [31 — Indonesian-Language OER Source Expansion](docs/31_INDONESIAN_LANGUAGE_OER_SOURCE_EXPANSION.md) | Additional Indonesian OER titles, ITATS source mapping and SPADA program-scoped CC BY path |
| [32 — Device-Adaptive Knowledge Acquisition and Corpus Budgeting](docs/32_DEVICE_ADAPTIVE_KNOWLEDGE_ACQUISITION.md) | Device-specific acquisition capacity, offline corpus tiers, storage and runtime scheduling |

## Current implementation status

The inspected application contains a Kotlin experimental neural engine, tokenizer, queue/worker prototype, preliminary hardware profile, accessibility and foreground services, Compose screens, and an in-memory world model. The audit found important correctness, state-persistence, concurrency, privacy and capability-reporting gaps.

In particular:
- The current training update is not a complete, verified backpropagation implementation.
- Current checkpoint behavior does not persist a complete model and optimizer state.
- Existing capture and training paths do not consistently enforce the intended consent and approval model.
- Hardware identification is heuristic and does not prove GPU or NPU execution.
- Persistent memory and the proposed multi-agent architecture are not verified in the inspected source.

Do not enable or describe the prototype training path as reliable model learning until the P0 acceptance gates have been implemented and verified. See [18](docs/18_IMPLEMENTATION_AUDIT.md) and [19](docs/19_P0_REMEDIATION_DESIGN.md).

## Platform and toolchain snapshot

The current repository is an Android project. The values below describe the checked-in prototype, not a promise of the final AGE runtime.

| Item | Current repository snapshot |
|---|---|
| Application ID | `com.argus.agi` |
| Language | Kotlin 2.2.10 |
| Android Gradle Plugin | 9.1.1 |
| Gradle wrapper | 9.3.1 |
| JDK expectation | 21 LTS |
| minSdk / targetSdk | 33 / 36 |
| Native C++ / JNI runtime | Not present in the inspected source tree |

Keep hardware backends behind capability-checked adapters. A SoC name, API feature flag or installed dependency is not proof that a model actually ran on an accelerator.

## Build and development

The commands below describe a local developer workflow. They are not evidence that the current branch has passed a build or test run.

~~~bash
git clone https://github.com/zegamingx/Argus.git
cd Argus
./gradlew :app:assembleDebug
~~~

The current application prototype must not be confused with the future Genesis flow. Do not add externally pretrained model weights as a substitute for Local Knowledge Genesis.

## License and data provenance

- **Code:** Apache-2.0; see [LICENSE](LICENSE).
- **General corpus:** Small C4 is a proposed bundled bootstrap source. Its dataset card says it inherits the original C4 license. The original C4 card identifies ODC-BY and also references Common Crawl terms. Record the exact source revision, attribution, applicable terms, preprocessing and checksums before distribution.
- **Model weights:** Argus's intended foundation is initialized locally. No externally pretrained model weights are required by the accepted Genesis direction.
- **Icon and brand:** See repository notices.

## Roadmap

1. Complete architecture-family research and tokenizer research.
2. Finalize corpus provenance and preprocessing.
3. Specify device capability profiling and adaptive model/training budgets.
4. Specify Genesis initialization, model identity, checkpoint and recovery contracts.
5. Resolve training correctness and personal-data governance P0 issues.
6. Implement and validate general background learning.
7. Add personal learning, generation-safe evolution and additional runtime backends only after their acceptance gates pass.

**Documentation-first work does not authorize implementation, builds, workflow dispatches or artifact publication.**
