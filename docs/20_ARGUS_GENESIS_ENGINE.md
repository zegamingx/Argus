# Argus Genesis Engine

**Status:** Accepted product flow and architecture direction; implementation not present  
**Last reviewed:** 2026-11-04

## 1. Purpose

The Argus Genesis Engine (AGE) creates a new, independent Argus model instance on the user's device. Genesis means constructing and initializing a model identity and its starting parameter state. It does not mean completing pretraining or producing an already capable language assistant.

The governing principle is **One Origin, Independent Evolution**.

## 2. Non-negotiable decisions

- Model parameters are initialized locally for each installation.
- An external pretrained model is not the foundation or identity of Argus.
- Device capability determines the initial model configuration.
- First launch does not wait for general-corpus training to finish.
- The application becomes usable after a valid initial generation is committed.
- General foundation learning is scheduled in the background after readiness.
- Personal learning is a separate, permission-governed path.
- Every instance evolves independently; no central learning server or cross-user model exchange exists.

## 3. First-launch lifecycle

~~~text
NOT_STARTED
    |
    v
ASSESSING_DEVICE
    |
    v
SELECTING_CONFIGURATION
    |
    v
CONSTRUCTING_MODEL
    |
    v
INITIALIZING_PARAMETERS
    |
    v
COMMITTING_INITIAL_GENERATION
    |
    v
GENESIS_READY
    |
    v
APPLICATION_READY
    |
    +----> GENERAL_LEARNING_SCHEDULED
~~~

Failure before the initial generation is committed must not be reported as Genesis success. Recovery may resume a verified initialization transaction or safely discard its incomplete temporary state and retry.

## 4. Genesis stages

### 4.1 Device assessment

Collect only data needed to choose a feasible configuration:
- Available memory headroom and Android memory class.
- CPU ABI, usable ISA features, topology hints and measured compute.
- GPU compute features, runtime/driver availability and validated operator coverage.
- NPU runtime availability and supported operator/model path, if any.
- Thermal and power state.
- Free storage and expected space for initial tensors, corpus, checkpoints and recovery.
- Android OS version and background execution constraints.

A vendor name or advertised accelerator is only a discovery hint. An accelerator is not usable until its runtime, operations and execution have been verified.

### 4.2 Configuration selection

Choose a configuration from architecture candidates that have passed the research/feasibility gate in [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md), the from-scratch feasibility protocol in [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md), the tokenizer contract gate in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md), and the component evidence/disposition gate in [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md), together with device memory and compute budgets, supported data types/operators, training memory needs, thermal sustainability and storage requirements.

There is no universal parameter count or fixed model tier. Record the selected configuration and the capability evidence used to select it.

### 4.3 Model construction

Build the logical model graph and allocate or stream parameter storage using bounded memory. Model construction and parameter initialization are part of the initial startup path, but they must not load or tokenize the full corpus.

### 4.4 Parameter initialization

Each module supplies its own initializer contract. Initialize all required tensors, validate shapes/dtypes/ranges and reject non-finite state.

Production initialization is independent per instance. A deterministic seed may be used in tests; production instances must not all receive identical weights. Persist enough initialization metadata for audit and recovery without exposing private user information.

### 4.5 Initial generation commit

Write the model state and manifest to a temporary generation directory. Validate tensor inventory, checksums, architecture/tokenizer fingerprints and storage completion. Commit the generation atomically. Incomplete temporary state must remain distinguishable from an active generation. The initial generation becomes immutable once published. Later learning must create an isolated child candidate generation and must not modify this committed generation in place; see [19 — P0 Remediation Design](19_P0_REMEDIATION_DESIGN.md) for the Genesis-to-training transaction and promotion contract.

### 4.6 Readiness publication

Only after commit succeeds may AGE publish the initial generation and allow the app to enter its ready state. Readiness means the model exists and is recoverable. It does not mean the model is trained or validated for general use.

## 5. Keep training off the startup critical path

The loading screen must not run full pretraining, corpus-wide preprocessing, multiple epochs, personal-data ingestion or long-running benchmarks.

The intended first-use experience:
1. Show meaningful Genesis stages.
2. Create and persist the local model.
3. Enter the application as soon as initialization is complete.
4. Schedule general foundation learning after readiness.
5. Let the user see, pause and resume background learning without blocking ordinary navigation.

Expensive deterministic corpus preparation should be preprocessed before distribution where lawful, or streamed in bounded work after readiness. It must not turn first launch into a long training gate.

## 6. Post-Genesis learning

General foundation learning begins from the bundled bootstrap corpus according to the background scheduler. The separate Autonomous Knowledge Acquisition Engine may also begin bounded public-source discovery after readiness and owner enablement; it stores accepted knowledge locally for retrieval and prepares training candidates through an independent rights and quality gate. Personal learning starts only under its own permissions and candidate-approval policy.

The acquisition engine does not depend on a capable neural model at startup. Its first discovery cycle uses deterministic topic policies and a reviewed source registry. Model-assisted topic proposals are a later capability and remain policy-controlled. See [24 — Autonomous Knowledge Acquisition](24_AUTONOMOUS_KNOWLEDGE_ACQUISITION.md).

The scheduler may defer work because of thermal pressure, low memory, battery/power mode, storage pressure, user preference or Android lifecycle restrictions. Deferral is not failure. The job must resume from a valid checkpoint whose parent-generation digest and durable data cursor still match. A resumed session must not replay an already committed optimizer update or skip an uncommitted work item. Active-generation promotion remains separate from checkpoint recovery; see [19 — P0 Remediation Design](19_P0_REMEDIATION_DESIGN.md).

## 7. Instance and generation identity

A model instance needs a unique instance ID, unique initial generation ID, optional parent-generation reference for successors, architecture/tokenizer fingerprints, complete tensor manifest, initialization policy/version, device-profile/configuration record and integrity checksums.

The identity manifest must not contain conversation text or sensitive user content.

## 8. Failure and recovery

| Failure | Required behavior |
|---|---|
| Device profile incomplete | Use a conservative recorded fallback or stop with an actionable reason |
| Insufficient memory/storage | Reduce only within validated configuration bounds; otherwise stop before partial commit |
| Unsupported operator/runtime | Select a verified fallback or reject the candidate configuration |
| Non-finite initialization | Discard incomplete candidate and report failure |
| Process death during initialization | Recover or clean up temporary transaction without publishing partial state |
| Checksum/schema mismatch | Reject state and preserve last-known-good generation |
| App stops after readiness | Preserve model and resume learning only from durable session state |

## 9. User-visible lifecycle terminology

- **Constructing:** model graph and tensor storage are being created.
- **Initializing:** parameter values are being generated.
- **Ready:** an initial model generation is safely committed.
- **Learning:** training is processing the general or personal path.
- **Paused / Deferred:** learning is not currently executing but can resume.
- **Trained:** a defined training phase completed.
- **Validated:** a candidate passed its declared evaluation gates.

Do not display “trained” or “intelligent” merely because Genesis is ready.

## 10. Explicit non-goals

Genesis is not a long blocking training splash screen, a pretrained-model download flow, a central account model synchronized across users, a promise that small random weights become generally competent from a few personal conversations, or permission to capture/train private content without user approval.
