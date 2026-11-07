# Argus Package Interface and Dependency Contract Matrix

**Status:** Proposed package-to-contract mapping; no concrete Kotlin API implementation exists by virtue of this document  
**Review date:** 2026-11-04  
**Repository baseline:** `main` at `afb8b30214f44620f1ff17e6735560fb2a706b0a`  
**Scope:** Ownership and allowed dependency mapping for cross-subsystem interfaces, application workflow APIs, outbound ports, platform/data/engine adapters, lifecycle state and verification obligations  
**Upstream authority:** Document 01 for accepted product constraints; document 36 for logical subsystem/API/lifecycle contracts; document 45 for target Kotlin package ownership and forbidden dependency direction; document 46 for observed prototype dependencies and the confirmed source-level package cycle  
**Boundary:** Documentation only. This document does not create Kotlin interfaces/classes, select a DI framework, create packages or Gradle modules, move files, change build configuration, run builds/tests/workflows, or authorize implementation.

## 1. Purpose

Documents 36, 45 and 46 answer three different questions:

- **Document 36:** What each logical subsystem owns, guarantees and exposes.
- **Document 45:** Where those responsibilities belong in the proposed Kotlin package hierarchy and which dependency directions are forbidden.
- **Document 46:** Which dependencies exist in the current prototype and which source-level couplings must be removed.

This document connects those authorities into one implementation-planning matrix. It assigns a contract owner, permitted consumers, implementation/adaptor owner and authoritative state boundary to each important cross-package handoff. Its purpose is to make the dependency graph actionable before any source movement or implementation begins.

This document is a **mapping and gap register**, not a replacement for the logical contracts in document 36. Where a method shape is already specified in document 36, this document reuses it. Where no method shape exists, the row identifies a port responsibility only; it does not invent a Kotlin signature.

## 2. Interpretation and contract status

### 2.1 Terms

| Term | Meaning |
|---|---|
| Contract owner | The package/layer that owns the stable meaning, rules and public shape of a contract |
| Inbound workflow API | An application-facing operation invoked by a feature, startup host or platform callback |
| Outbound port | An abstraction required by a workflow/domain owner and implemented by a data, platform or engine adapter |
| Adapter / provider | A concrete implementation of a contract using persistence, Android APIs, network transport or numerical execution |
| Consumer | A package permitted to call the contract; this does not imply direct knowledge of its implementation |
| Composition root | `app/bootstrap`, which constructs concrete objects and wires them to contracts |
| Authoritative state owner | The workflow or repository whose state is the source of truth; observers and UI state are projections |
| Contract gap | A required handoff whose owner, semantics or failure behavior remain unresolved |

### 2.2 Status rules

1. **LOGICAL CONTRACT DEFINED** means document 36 defines the responsibility and required guarantees. It does not mean a Kotlin interface exists.
2. **PACKAGE OWNER PROPOSED** means document 45 provides a target package, but exact Kotlin API names or types may remain open.
3. **IMPLEMENTATION BINDING OPEN** means the concrete provider, library, storage format, backend or DI mechanism has not been selected.
4. **CONTRACT GAP** means a necessary cross-layer handoff is not yet sufficiently owned to permit a safe implementation.
5. **POST-IMPLEMENTATION VERIFICATION** means correctness must be proven against the stated contract and exact source revision/device environment.
6. **DESIGN CONTRACT CLOSED** means ownership, route/state semantics and the allowed handoff are sufficiently selected for implementation planning. It does not mean code exists, runtime behavior is verified or tests have passed.

Names shown in this document are contract labels unless explicitly marked as an existing prototype declaration. No row is evidence that a corresponding production interface or class already exists.

## 3. Rules for assigning and consuming interfaces

### 3.1 Contract ownership

1. Domain records, identity invariants and provider-neutral policies belong in the relevant `core/*` package.
2. Process-independent use-case orchestration and inbound workflow APIs belong in the owning `application/<workflow>` package.
3. Persistence, serialization, source fetching and indexing implementations belong in `data/<domain>`.
4. Android framework, permission, service, capture, storage and provider-runtime integration belongs in `platform/android/*`.
5. Numerical reference operations and selected model/tokenizer implementations belong in `engine/*`, subject to the architecture/tokenizer gates.
6. Feature presentation owns immutable presentation state and maps user intent to application APIs. It does not own domain policy, repository access or engine execution.
7. `app/bootstrap` wires implementations. It must not absorb workflow policy, state machines, storage semantics or numerical operations.
8. Do not introduce a generic `common/contracts`, `manager`, `utils` or global service-locator package. Every contract has one named owner.

### 3.2 Dependency rules

- `feature/*/presentation` → owning `application/*` API and `ui/design`.
- `application/*` → `core/*` contracts and narrowly defined workflow ports. It must not import Android, Compose, `data/*`, `platform/android/*` or numerical engine implementations.
- `core/*` → platform-neutral Kotlin/domain dependencies only. It must not import Android, Compose, Room, Retrofit/OkHttp, vendor SDKs or JNI implementation code.
- `data/*`, `platform/android/*` and `engine/*` implement contracts. They must not form a default direct dependency chain between one another.
- `platform/android/*` may call narrow application-facing ingress/control APIs for Android callbacks. The application layer must not call back into the concrete Android adapter.
- `app/bootstrap` may reference concrete implementations from each adapter family to construct the graph. This is the intentional composition exception, not a permission for other packages to import implementations.
- No package or Gradle-module dependency cycle is permitted.
- Cross-layer outcomes are typed and identity-bound. A global mutable registry, reflection-based service locator or broad static singleton is not a substitute for a contract.

### 3.3 API and data rules

1. Keep Android `Context`, `Activity`, `Service`, `Intent`, `Uri`, `Bitmap`, CameraX objects and Android lifecycle owners out of `core/*` and application-domain contracts.
2. Keep Room entities, database handles, Retrofit/Moshi DTOs, OkHttp response objects and vendor runtime handles out of feature presentation and core contracts.
3. Use stable IDs and versioned manifests across boundaries: instance, generation, session, candidate, source, consent, event and correlation identities must not be inferred from display labels.
4. A repository returns domain-facing records/results. It does not expose storage rows or network DTOs.
5. Expected operational failures use typed outcomes and the error categories in document 36. Unexpected exceptions are sanitized at the owning boundary; raw private content, prompts, credentials and full corpus text do not enter generic errors/events.
6. Cancellation and resource budgets cross the workflow boundary explicitly. A UI event or notification refresh is never the source of truth for committed work.
7. A consumer may observe a state/result stream, but the owning workflow remains responsible for the authoritative state and durable transition.

## 4. Package-to-interface contract matrix

The following matrix is the proposed mapping for the logical subsystem APIs in document 36. “Implementation owner” identifies the target package responsible for the concrete adapter or workflow, not a class that currently exists.

| ID | Contract surface | Contract owner | Permitted consumers | Implementation/provider owner | Authoritative state or transaction |
|---|---|---|---|---|---|
| IF-01 | Instance, generation, session, candidate and correlation identity records | `core/identity` | All owning core/application workflows; data repositories validate identity | Value objects remain platform-neutral; persistence is owned by the relevant data domain | The owning repository/workflow validates identity on every read/write |
| IF-02 | `CapabilityProvider.inspect(requestedWorkload)`; `RuntimeConditionProvider.inspect()` | Port/record contracts in `core/device` | `application/genesis`, `application/learning`, `application/acquisition` and other resource-admitted workflows | `platform/android/device` gathers Android evidence and live observations | Capability evidence is versioned; runtime conditions are short-lived observations |
| IF-03 | `ConfigurationSelector.select(...)` and configuration decision | Domain constraints in `core/device` / model config in `core/model`; selection workflow in `application/genesis` | Genesis only, through the declared workflow | `application/genesis`; researched candidates and resource accounting are inputs | Decision is reproducible from versioned capability, runtime, candidate and budget inputs |
| IF-04 | `GenesisCoordinator.createInitialGeneration(instanceId, policy)` | Inbound workflow API in `application/genesis`; records in `core/model`, `core/device`, `core/tokenizer` | `app/entry` startup/readiness host; controlled lifecycle entry points only | `application/genesis` orchestrates providers; initializer is supplied through a model/engine contract; `data/model` commits generation state | Genesis workflow owns readiness; `data/model` owns durable initial-generation publication |
| IF-05 | `ModelGenerationRepository`: stage, validate, commit, open immutable snapshot, create/commit candidate, promote and rollback | Port and identity rules in `core/model` | `application/genesis`, `application/inference`, `application/learning`, `application/evaluation` | `data/model`; Android file/key operations are isolated in `platform/android/storage` | Repository transaction owns manifests, tensors, checksums, candidate isolation and active-generation pointer |
| IF-06 | `TokenizerProvider.prepare/load`, `TokenizerTrainer.train`, `TokenizerInstance.encode/decode/fingerprint` | `core/tokenizer` | Genesis, inference, learning and corpus preprocessing through declared contracts | Selected implementation in `engine/tokenizer` only after G1-D; tokenizer artifact persistence/reference is handled by the approved data repository | Exact tokenizer manifest/fingerprint is immutable within a generation; changed token IDs require migration |
| IF-07 | `InferenceService.generate(snapshot, request, inferenceOptions, cancellation)` | Inbound workflow API in `application/inference`; request/result/snapshot identity in `core/inference` and `core/model` | `feature/chat/presentation` and other approved application workflows through the application API | `application/inference` orchestrates; `engine/reference` is the correctness path, with a selected family backend only after G4; runtime integration is in `platform/android/compute` | Inference uses an immutable generation snapshot; it never writes model or optimizer state |
| IF-08 | Learning session API: `submit`, `pause`, `resume`, `stop`, `status` | Inbound workflow API in `application/learning`; session/update/recovery records in `core/training` | `feature/learning/presentation`; `platform/android/lifecycle` through a narrow control/observation contract | `application/learning` owns admission, process-scoped session control and lifecycle; `data/model` persists candidate/checkpoint state; `engine/reference` or a later selected local engine executes numerical updates | Pause/stop requests are honored at the declared safe optimizer-update boundary; pause publishes PAUSED only after checkpoint commit. Resume validates exact dataset/admission/split/tokenizer/model/optimizer/scheduler/RNG/cursor/backend identity and current resource admission. Candidate, checkpoint validation and active-generation pointer remain separate state axes |
| IF-09 | Training backend and numerical update contract | `core/model`, `core/training`, `core/compute` | `application/learning` through the declared engine contract only | `engine/reference` initially for correctness-oriented work; selected local `engine/model` only after G4; native adapter only after separate ABI/memory/toolchain approval | Engine mutates candidate-only state and reports typed finite/invalid result plus committed update identity; it must cover the selected forward/loss/backward/gradient/optimizer/checkpoint/recovery path. Production bindings are local CPU/Vulkan/NPU providers only; remote execution is outside the accepted product path and cannot be wired into production composition. Engine cannot authorize data or promote a generation |
| IF-10 | `PolicyEngine.evaluate(operation, resource, actor, context)`; `ConsentRepository.check(...)` | Rules/port contracts in `core/policy` | Acquisition, capture/knowledge, corpus, learning and other privileged workflows; policy is not a UI-only check | Pure decision rules remain in `core/policy`; durable grants, revocations, exclusion and audit links are implemented in `data/policy` | Policy version and operation-specific decision are auditable; missing/unknown permission is DENY |
| IF-11 | Candidate review, approval and revocation workflow | Scope/invariant contract in `core/policy`; personal-intake review workflow in `application/knowledge` under document 48 | User review presentation and `application/learning` through explicit APIs | `application/knowledge` owns intake review orchestration; `data/policy` persists consent/review evidence; `application/learning` independently validates approval and owns training admission | Approval/revocation records are durable and versioned; the capture surface cannot self-approve; revocation invalidates pending candidate membership at a safe boundary |
| IF-12 | `AcquisitionCoordinator.discover(plan)` and `fetch(approvedJob)` | Inbound workflow API in `application/acquisition`; source/job/operation contracts in `core/acquisition` | Approved discovery/scheduling workflows and user-visible acquisition controls | `application/acquisition` orchestrates; `data/acquisition` implements source connectors, HTTP fetch and parsing; connectivity observations come from `platform/android/connectivity` | Acquisition job state is idempotent, bounded and durable; connector response is untrusted until validation |
| IF-13 | Source connector and source registry interfaces | Port/records in `core/acquisition`, authorization in `core/policy` | Acquisition coordinator only, after source and operation checks | `data/acquisition` owns versioned `SourceRegistryEntry` records and concrete connector/parser implementations; `data/policy` owns user consent, revocation and exclusion records | Registry version and item-level rights evidence are checked for each operation; connector cannot authorize itself, change retention policy or decide training eligibility |
| IF-14 | Knowledge repository, personal-experience record, provenance, index and retrieval evidence contracts | `core/knowledge` and `core/acquisition` | `application/knowledge`; inference/context workflows through a declared evidence/context port | `data/knowledge` implements personal-experience and inspectable-memory persistence/indexes; `application/knowledge` coordinates intake, retention routing and retrieval | Personal-experience custody is distinct from retrieval visibility; record revision, provenance, rights, retention and index lineage remain bound to each result |
| IF-15 | `RetrievalService.query(query, filters)` and memory workflow | Inbound workflow API in `application/knowledge`; records/results in `core/knowledge` | Feature/application consumers through explicit workflow or context ports | `application/knowledge` orchestrates; `data/knowledge` supplies retained records and indexes | Retrieval result is evidence, not training approval; retrieval does not mutate model weights |
| IF-16 | User-selected local dataset import, corpus construction, staged preview and immutable dataset manifest/reader | Dataset identity, split and import/store ports in `core/training`; workflow API in `application/learning` | `feature/learning/presentation` through the application service; `app/bootstrap` binds concrete providers; platform/data adapters implement only the declared core ports | `application/learning/LocalCorpusImportService` coordinates `CorpusImportStager` and `LocalCorpusStore`; `platform/android/storage/LocalDatasetImporter` implements the stager and owns Android SAF/assets, streaming decode, ZIP parsing and disk-backed deduplication; `data/corpus/LocalCorpusRepository` implements the store and owns manifest/admission-log persistence, immutable revision commit, listing and integrity verification. The repository has no Android Context dependency and the platform importer does not import data-layer classes. | Local-only; user selects workflow purposes. Immutable DatasetManifest binds source-artifact SHA-256 and canonical normalized-dataset SHA-256, parser/preprocessing/dedup versions, provenance/rights and tokenizer state. DatasetAdmissionDecision is a separate append-only PENDING/APPROVED/REJECTED decision bound to that exact digest. DatasetSplitManifest pins train/validation/test membership and a digest per split at document/conversation/source boundaries, with leakage checks; test data is not used for gradient updates. User source bytes are streamed; ZIP data is staged and parsed directly from ZipInputStream, never extracted by archive path. Existing importer limits remain 64 source files, 4,096 archive entries, 262,144-character TXT chunks and 1,048,576-character JSONL rows; there is no fixed per-file or aggregate source-byte ceiling, so storage availability and explicit recovery/model reserves govern admission; deduplication is disk-backed in temporary SQLite. Import does not start training. Personal material remains under document 48 |
| IF-17 | Evaluation report, regression gate, verification and promotion transaction | Records/invariants in `core/evaluation` and `core/model`; workflow API in `application/evaluation` | Learning workflow after training completion; authorized status presentation | `application/evaluation` pins the EvaluationProtocolManifest, evaluates an immutable candidate and captures the Active Generation baseline; `data/model` stores reports/verification and executes the idempotent pointer transaction | Report binds candidate, evaluation protocol, test-split/data digest, tokenizer/model/runtime and metric implementation identity. Baseline is the Active Generation at evaluation start. CandidateVerificationRecord is separate from candidate identity. Promotion uses expected active-pointer revision plus idempotency key; stale baseline returns conflict. Rollback is a new audited pointer transaction; a candidate is active only when the authoritative pointer references it |
| IF-18 | Typed lifecycle/event envelope and diagnostic schema | `core/events` | Owning workflows emit; UI/OS adapters observe bounded projections | Workflow-owned state/result APIs emit facts; `data/audit` persists sanitized audit records | Event is not a second state store; workflow/repository state remains authoritative |
| IF-19 | Android lifecycle, foreground execution, scheduling and notification adapters | Platform API constraints and decision matrix in document 21; session semantics in `core/training` / `application/learning` | Android OS and app entry host; adapter calls only narrow session control/observation APIs | `platform/android/lifecycle` and `platform/android/notification`; concrete direct-FGS/WorkManager selection remains a documented platform binding | OS service/worker state is not training-session state. FGS type/permission/start restrictions and WorkManager/JobScheduler quotas must follow document 21. Neither adapter owns authoritative session state or guarantees process survival; stop callbacks are signals, while periodic committed checkpoints are the recovery guarantee |
| IF-20 | Android permissions, accessibility/camera capture and installed-app discovery | Platform contracts in `platform/android/permissions` and `platform/android/capture`; policy rules in `core/policy` | Android OS callbacks hand bounded observations to the `application/knowledge` intake workflow defined in document 48 | `platform/android/permissions` and `platform/android/capture`; exclusions and consent records are persisted through `data/policy` | Platform permission is separate from surface/purpose consent; adapters do not own retention, review or training eligibility |
| IF-21 | Compute capability, backend selection evidence and execution result | Provider-neutral contract in `core/compute` | `application/inference`, `application/learning`, Genesis/backend admission | Runtime/provider discovery and Android/vendor calls in `platform/android/compute`; numerical backend in `engine/*` | Report actual provider, runtime, driver, precision, operator coverage, fallback and execution evidence |
| IF-22 | Shared Compose visual primitives | UI-only ownership in `ui/design` | Feature presentation and app navigation | `ui/design` | Visual state is presentation-only and cannot own domain or workflow state |
| IF-23 | Growth assessment and child-generation planning: `GrowthController.assess(...)`, `ArchitectureEvolutionPlanner.plan(...)` | Transition invariants in `core/model`; capability/resource evidence in `core/device`; evaluation evidence in `core/evaluation` | `application/evolution`, invoked by approved local learning/evolution workflows | `application/evolution` coordinates; selected `engine/model` performs declared tensor transforms; `data/model` persists transition manifests and candidate generations | Growth decision and transition manifest are versioned; parent remains immutable; only `data/model` can publish/promote the child after `application/evaluation` passes |
| IF-24 | Sparse topology policy and safe-boundary update: `SparseTopologyController.proposeUpdate(...)` | `core/training`, `core/model` and `core/compute` | `application/learning` and a selected training engine through the declared candidate-state contract | `engine/model` applies topology/optimizer kernels; `data/model` persists sparse topology and active optimizer state in checkpoints | Candidate topology revision, active count, pattern/index digest, optimizer state, RNG and data cursor commit atomically; no active-generation mutation |

The method names reused above are conceptual names already present in document 36. Rows that describe a responsibility without a method name do not authorize a new public API shape by implication.

## 5. Allowed dependency matrix

This table defines compile-time package dependencies. It is not a call-order diagram.

| Source package | May depend on | Must not depend on |
|---|---|---|
| `app/entry` | `app/navigation`, narrow app bootstrap API, Android framework | Concrete engine/data repositories or workflow internals |
| `app/navigation` | Feature destinations and navigation contracts | Data, engine, repository or policy implementations |
| `app/bootstrap` | All selected application APIs and concrete data/platform/engine adapters needed to construct the graph | Unbounded business workflow logic or duplicate state ownership |
| `feature/*/presentation` | Owning application API/state contracts, `ui/design`, Compose UI libraries | `data/*`, `platform/android/*` implementation classes, `engine/*`, concrete repositories, global service locators |
| `application/*` | `core/*`, other explicitly declared workflow ports where the dependency remains acyclic | Android framework, Compose, `data/*`, `platform/android/*`, concrete engine implementation |
| `core/*` | Platform-neutral Kotlin/domain primitives and other lower-level core contracts where acyclic | Android, Compose, persistence/network libraries, vendor SDKs, JNI and feature/application implementations |
| `data/*` | Core contracts and required persistence/network libraries | Feature presentation, Android UI components, numerical engine internals or sibling adapters by default |
| `platform/android/*` | Android APIs, core contracts, narrow application ingress/control APIs | UI implementation details, domain policy ownership, engine internals or direct data repository internals |
| `engine/*` | Core model/tokenizer/training/compute contracts and numerical dependencies | Android, Compose, Activity/Service/ViewModel, data repositories, network connectors and policy decisions |
| `ui/design` | Compose UI/runtime primitives and design tokens | Feature workflows, application APIs, repositories or platform services |

### 5.1 Composition and permitted adapter relationships

The only package whose job is to know multiple concrete adapter families is `app/bootstrap`. It builds the object graph and passes dependencies through explicit constructors/factories or a separately selected DI framework.

The composition root does not make the following relationships legal:

- `application/learning` importing `platform/android/device` directly;
- `feature/chat` importing `engine/model`;
- `platform/android/lifecycle` reaching into `data/model` to read training state;
- `data/acquisition` calling `data/policy` to approve its own request;
- `engine/model` reading Android files or deciding personal-data eligibility;
- `application/evolution` importing `engine/model` or `data/model` directly instead of using declared ports;
- `engine/model` changing the active parent generation, choosing growth policy or promoting a candidate;
- `ArgusApp` static properties becoming the permanent dependency API.

Adapters may use their own implementation details internally, but cross-adapter coordination remains with the application workflow or composition boundary.

### 5.2 Required source-cycle removal

The current package cycle recorded by document 46 is:

~~~text
com.argus.agi.MainActivity
        -> com.argus.agi.ui
        -> com.argus.agi.service
        -> com.argus.agi (ArgusApp / MainActivity)
~~~

The target direction is:

~~~text
app/entry + app/navigation
        -> feature/*/presentation
        -> application/* APIs
        -> core/* contracts

data/* -----------┐
platform/android -┼-> implement core/application ports
engine/* --------┘

app/bootstrap -> constructs and wires the selected implementations
~~~

The cycle is considered removed only when all of these source conditions hold:

1. Feature presentation has no import/reference to concrete Android service implementations.
2. Android services do not read static/global `ArgusApp` objects.
3. Foreground notification navigation does not require a service-to-`MainActivity` implementation dependency.
4. Application workflows do not import Android framework/service or concrete adapter packages.
5. Engine code has no Android Context/file/policy/presentation dependency.
6. Static dependency analysis finds no path returning from an adapter or feature to its own upstream consumer through concrete types.

Do not claim cycle removal from folder names or package moves alone. Verify the post-change import/reference graph.

## 6. Authoritative state and transaction ownership

State ownership is separate from who displays or triggers an operation.

| State or record | Authoritative owner | Observers / projections | Boundary rule |
|---|---|---|---|
| Genesis readiness and initialization outcome | `application/genesis` workflow | App entry and status UI | Ready only after initial generation is durably committed and recoverable |
| Active model generation and immutable snapshots | `data/model` repository | Inference, learning, evaluation | Only repository transaction can publish or switch active generation |
| Tokenizer identity and artifact fingerprint | `core/tokenizer` contract plus immutable generation manifest in `data/model` | Genesis, inference, corpus and training | No mutable vocabulary registration in a live generation |
| Live training-session state and control | `application/learning` | Feature UI and Android lifecycle adapter | OS service/notification does not own pause/resume/checkpoint truth |
| Checkpoint and candidate publication | `data/model` repository transaction coordinated by learning workflow | Learning and recovery | Optimizer update, RNG, dataset cursor and required recurrent state share one commit boundary |
| Acquisition job lifecycle | `application/acquisition` with durable records in its selected data store | UI/scheduler | Retry is bounded and idempotent |
| Source rights, consent, revocation and app exclusions | `core/policy` rules; `data/policy` durable records | All privileged workflows and platform adapters | Unknown is DENY; OS permission does not equal training consent |
| Knowledge records and retrieval indexes | `data/knowledge` | Knowledge and inference workflows | Record revisions remain traceable to provenance/rights |
| Corpus manifest and split/cursor | `data/corpus` for immutable dataset artifacts; `application/learning` for session use | Learning/evaluation | No live mutable acquisition queue as a training dataset |
| Candidate evaluation and promotion decision | `application/evaluation` with report bound to candidate digest; commit in `data/model` | Learning and status UI | Evaluation and promotion are separate; promotion is atomic |
| Growth decision and structural transition plan | `application/evolution`; immutable transition manifest in `data/model` | Learning/evolution workflow and status projection | Assessment may propose a child only; parent stays active until evaluation and repository promotion pass |
| Sparse topology and active-parameter state | Candidate training state owned by `application/learning` and the selected engine contract; durable checkpoint in `data/model` | Recovery and evaluation | Topology revision, active optimizer state, RNG and cursor share one checkpoint boundary |
| Lifecycle event schema | `core/events` | Owning workflows and observers | Event is a fact, not a source of truth |
| Durable sanitized audit record | `data/audit` | Authorized diagnostics | No raw prompt, private capture or full corpus content in generic audit payload |
| Transient runtime conditions | `platform/android/device` observations shaped by `core/device` | Resource admission workflows | Short-lived; must be re-read before workload admission |
| Feature UI state | Feature-owned ViewModel/state holder | Compose screens | Projection only; must not become a second durable session/model database |

The exact persistence split for training session metadata, source registry records and evaluation reports remains subject to the storage/schema decision. The logical owner above must not be treated as a final database-table or file-layout decision.

## 7. Android adapter handoffs

Android components are entry adapters, not workflow owners.

| Adapter | Permitted responsibility | Application-facing handoff | Explicitly forbidden shortcut |
|---|---|---|---|
| Accessibility service | Receive Android accessibility callbacks; apply platform safeguards and bounded capture limits | Submit a typed, sanitized capture observation only through the approved capture-ingress contract | Calling a raw-string training queue/worker (the legacy implementation is retired), mutating WorldModel, or starting training directly |
| Camera integration | Permission and lifecycle-aware preview/capture integration | Submit a bounded observation through the same approved capture/policy boundary where applicable | Passing CameraX objects, `Bitmap` or Android `Context` into core/application domain contracts |
| Foreground service / worker | Satisfy OS execution, foreground notification, start/stop and rescheduling rules | Invoke a narrow learning-session control/observation API | Reading the training manager/repository/global application singleton as an alternate state store |
| Permission adapter | Request/read OS permission and expose the platform result | Pass the permission observation to the owning policy/application workflow | Treating granted Android permission as user consent for retention or training |
| Device adapter | Read Android build, ABI, hardware, battery, thermal and runtime observations | Return versioned capability/runtime-condition records through `core/device` contracts | Inferring verified GPU/NPU execution from chipset names |
| Notification adapter | Render sanitized status and user actions; create an app route/deep-link PendingIntent through a host-owned navigation contract | Observe workflow-owned status and send user command to the appropriate application API | Importing `MainActivity` as the owner of training control or maintaining a second progress counter |

### 7.1 Personal capture and experience intake — PC-OPEN-01

**Status: DESIGN CONTRACT DOCUMENTED AS A PROPOSAL in [48 — Personal Capture and Experience Intake Contract](48_PERSONAL_CAPTURE_AND_EXPERIENCE_INTAKE_CONTRACT.md). Exact API and implementation bindings remain open.**

Document 48 resolves the design-level ownership and routing gap:

- `application/knowledge` is the proposed accountable owner of personal-experience intake, retention routing, provenance and review orchestration.
- `platform/android/capture` supplies bounded, registered capture observations only; the current AccessibilityService remains a provisional capture ingress, not a consent/policy owner.
- `core/policy` owns purpose/scope rules; `data/policy` persists grant, revocation, exclusion and review evidence.
- `data/knowledge` owns retained personal-experience records and inspectable-memory storage. The custody/provenance record is distinct from retrieval visibility.
- `application/learning` owns training-session admission and independently validates approved, batch-specific personal-training authorization before accepting an immutable candidate manifest.
- The memory/experience record boundary is mandatory before a retained training-intended input can be handed to learning. Capture cannot call a training queue directly.

The remaining implementation bindings are tracked in document 48 as PC-IMPL-01 through PC-IMPL-07. They include the initial surface allowlist, retention durations, user-facing review/consent flow, exact envelope/API, post-training lineage, severance reconciliation and storage schema. Until the required bindings for the first enabled surface are resolved, it must not be enabled beyond its currently authorized prototype status or replaced by an unreviewed capture path.

### 7.2 Notification navigation handoff

The design-level route and task-stack semantics are now closed:

- Canonical internal route: `argus://app/learning/session/{sessionId}`, owned and resolved by `app/navigation`.
- `platform/android/notification` creates an explicit immutable PendingIntent with a unique identity per session; it does not import or own `MainActivity`.
- A notification tap reuses the existing Argus task where possible. A cold launch creates the normal app root before opening the session destination, without creating duplicate Activity instances.
- The destination validates the session identifier and re-queries `application/learning`; notification extras are never authoritative state or authorization.
- Opening the route does not automatically start or resume training. Session mutations remain explicit user commands.

The route is internal and is not registered as an external browsable data-ingress route. Android-specific PendingIntent/navigation-library wiring remains implementation work, but it must preserve this contract.

### 7.3 UI state and observation handoff — PC-OPEN-02

The design-level observation contract is closed in document 36 section 3.2:

- Each owning `application/<workflow>` exposes a read-only `StateFlow<WorkflowSnapshot>` for long-lived status; the mutable state holder remains private.
- Each feature ViewModel maps that snapshot to a feature-specific immutable `FeatureUiState` and exposes it read-only to lifecycle-aware Compose collectors.
- Snapshots carry owning identity and a monotonic revision/sequence. Progress uses explicit units and committed workflow/repository counters; display updates may be coalesced without changing committed progress.
- Feature presentation never receives database/network DTOs, engine tensors, raw personal content or platform objects. Commands return through typed application APIs.
- The workflow state is re-observed after screen/process recreation; UI and notification adapters do not create competing state stores.

Exact feature data classes and field selection are implementation declarations constrained by this contract, not unresolved architecture or ownership decisions.

### 7.4 Composition and dependency injection — PC-OPEN-06

The default composition decision is explicit manual constructor injection through `app/bootstrap` for the current single-`:app` topology:

- `app/bootstrap` is the only composition root and binds concrete data/platform/engine adapters to core/application ports.
- Framework-created Android entry components receive narrow factories/dependencies through the Application-owned composition boundary; ordinary Kotlin classes use constructor injection.
- Process, workflow/session, ViewModel and Android-component lifetimes remain distinct. Expensive model/runtime instances are created lazily after profile/resource admission.
- Static companion-object service locators, reflection-based discovery and global mutable registries are prohibited.
- Hilt/Koin/Dagger or another DI framework is not selected or required. Reconsider one only if measured graph complexity or an actual module boundary justifies it.

The concrete object graph still requires code, but DI framework selection and layer ownership are no longer blockers.

## 8. Existing prototype mapping

This table maps the inspected files from document 46 to the contract work required. It is not a move list and does not authorize a rewrite.

| Existing prototype file/package | Current coupling | Required future contract boundary |
|---|---|---|
| `ArgusApp.kt` | Android Application host delegates to process-scoped `app/bootstrap/ArgusContainer` through `ArgusRuntimeOwner`; no global companion-object service locator remains | Keep Application entry in `app/entry`; construct and bind dependencies in `app/bootstrap` |
| `MainActivity.kt` | Hosts Activity, creates the shared transitional ViewModel through `ArgusViewModelFactory`, and starts the foreground service; navigation composition is delegated to `app/navigation` | `app/entry` hosts Activity; `app/navigation` wires feature destinations and state; service lifecycle remains an Android adapter |
| `ArgusViewModel.kt` | Still coordinates multiple features and reaches concrete model, data and platform implementations. Chat, Settings, Learning and Perception have feature-owned immutable UI state projections. Known raw-string auto-enqueue calls have been removed, and the legacy manager/worker plus `ArgusNeuralEngine.trainStep(String)` are retired. File/image inference and intake remain unimplemented. | Split feature ViewModels after workflow APIs are implemented; each consumes its feature state and sends commands through the owning application workflow API. Do not recreate the raw-string queue/worker shortcut. |
| `ArgusNeuralEngine.kt` | Combines numerical operations, tokenizer mutation/usage, Android Context and checkpoint file I/O | Core model/inference/training contracts; `engine/reference` numerical implementation; `data/model` persistence; `platform/android/storage` platform file/key access |
| `Tokenizer.kt` | Concrete mutable tokenizer is colocated with experimental engine | `core/tokenizer` identity/API; selected implementation only in `engine/tokenizer` after G1-D |
| Former `TrainingManager.kt` (removed 2026-11-05) | Raw-string queue, constructor-started coroutine worker, device guard and direct engine update; removed because it bypassed candidate review/admission | Future `application/learning` coordinator over `core/training`, immutable approved candidate manifests, resource admission, candidate-only engine execution, checkpoint/recovery and evaluation; no current implementation exists |
| `WorldModel.kt` | Android Context, in-memory memory records and transient screen/camera context are combined | Separate `core/knowledge` records, `application/knowledge` workflows, `data/knowledge` persistence/index, and transient context ingress |
| `DeviceGuard.kt` / `HardwareProfile.kt` | Android observations and workload/device interpretation are mixed | `platform/android/device` providers with stable/live records in `core/device`; admission stays in application workflows |
| `ExcludedAppsManager.kt` | PackageManager discovery and exclusion state are combined | Platform installed-app discovery plus `core/policy` rules and `data/policy` durable exclusions |
| `WebSearchEngine.kt` | HTTP transport/parser is labelled as a sensor | `data/acquisition` connector behind `core/acquisition` and application authorization |
| `ArgusAccessibilityService.kt` | Uses `ArgusRuntimeOwner`/capture ingress and action registry; current handoff updates transient screen context only and does not enqueue personal text directly for training | `platform/android/capture` adapter behind the personal-intake workflow; explicit consent, review, retention and learning admission remain separate |
| `ArgusForegroundService.kt` | Reads training/device/hardware snapshots and sends pause control through `ArgusRuntimeOwner`; launch intent is resolved by package manager rather than importing MainActivity | `platform/android/lifecycle` + notification adapter; application-owned learning controls and route contract |
| Compose screens | Chat, Settings, Learning and Perception consume feature-owned immutable UI state; Perception still directly integrates CameraX/Settings/permission APIs, and commands still route through the transitional shared ViewModel | Feature presentation consumes application state/API; Android UI-edge permissions/preview remain narrowly scoped |

## 9. Dependency and interface verification plan

The following checks are required when implementation is separately authorized. They are not claimed to have run.

| Verification | Required assertion | Test/static-analysis placement |
|---|---|---|
| Package graph acyclicity | No dependency cycle across target package roots | Architecture/dependency test over production imports |
| Core isolation | No Android, Compose, Room, Retrofit/OkHttp, vendor SDK or JNI implementation import in `core/**` | Static forbidden-dependency rule |
| Application isolation | No Android framework, Compose, `data/**`, `platform/android/**` or concrete engine implementation import in `application/**` | Static forbidden-dependency rule |
| Feature isolation | No concrete repository, data DTO, engine or Android service implementation import in `feature/**` | Static forbidden-dependency rule plus feature unit tests |
| Adapter implementation | Data/platform/engine implementations satisfy the exact owned port and typed outcomes | Contract tests for each adapter |
| Composition root | Every required port is bound exactly once or has an explicit optional/missing-provider result; no hidden global locator | Graph-construction test |
| State authority | UI, notification and events cannot independently mutate/claim committed model or training state | Workflow tests and state-transition tests |
| Identity propagation | Instance/generation/session/tokenizer/dataset/candidate fingerprints survive every relevant handoff | Contract and repository tests |
| Policy enforcement | Missing/unknown consent or rights denies protected capture, retention, retrieval, export or training operation as applicable | Policy matrix tests, including revocation races |
| Genesis transaction | Application readiness is not published until an initial generation is valid, durable and recoverable | Genesis/repository crash-boundary tests |
| Training recovery | Candidate, optimizer update, RNG, data cursor and required recurrent state agree at checkpoint/resume | Learning lifecycle and checkpoint tests; recurrent requirements from document 41 |
| Inference isolation | Inference consumes a fixed immutable snapshot and releases request-owned resources on cancellation | Inference/backend tests |
| Promotion isolation | Evaluated candidate digest is unchanged when the repository atomically promotes it | Evaluation/promotion transaction tests |
| Evolution isolation | Growth assessment cannot mutate the active generation; child tensor map, resource envelope, validation and rollback bind to the exact parent digest | Growth/repository contract tests |
| Sparse topology recovery | Topology mask/index revision, active optimizer state, RNG and data cursor resume exactly after interruption; unsupported sparse kernels reject or route explicitly | Training/checkpoint/backend contract tests |
| Android lifecycle | OS stop/reclaim/permission/service-denial paths preserve workflow-owned safe state without bypassing platform rules | Android instrumentation/device tests |
| Capture intake | Raw capture cannot reach training before policy, classification, redaction and explicit approval | Capture/policy/learning integration tests |

A static pass proves only the dependency rule in the inspected revision. Runtime correctness, transaction durability, Android lifecycle behavior, privacy behavior, performance and device/backend capability require their own specified verification evidence.

## 10. Contract decision register and remaining blockers

| ID | Open item | Why it remains open | Required owner/evidence | Blocks |
|---|---|---|---|---|
| PC-OPEN-01 | Personal capture and experience intake contract | Design contract documented in document 48; `application/knowledge` is the proposed accountable intake owner and `application/learning` owns training admission. Exact API/retention/surface bindings remain open | Resolve PC-IMPL-01–PC-IMPL-07 in document 48 against documents 13, 22, 36 and 45 | Enabling a new capture surface or safely replacing/retiring the current AccessibilityService path |
| PC-OPEN-02 | UI-facing immutable state and observation streams | **DESIGN CONTRACT CLOSED** in document 36 section 3.2: workflow-owned read-only StateFlow snapshots, feature-specific immutable UI projections, revision identity and committed-progress semantics | Implementation maps each feature to its own state type; no domain/data/engine DTO exposure | No remaining design blocker; feature ViewModel decomposition is implementation work |
| PC-OPEN-03 | Notification deep-link/route contract | **DESIGN CONTRACT CLOSED** in document 36 section 3.3: stable internal session route, immutable per-session PendingIntent, task-stack and no-implicit-resume semantics | `app/navigation` owns route; `platform/android/notification` adapts it | No remaining design blocker; Android wiring and later lifecycle verification remain implementation work |
| PC-OPEN-04 | Repository transaction interfaces and storage schema | Exact store, schema, encryption/key ownership, migration, deletion and atomic publication mechanism remain open | Documents 13, 15, 19, 36 plus storage decision/ADR | Data adapter implementation and safe extraction |
| PC-OPEN-05 | Compute provider injection and backend capability binding | Provider-neutral port and ownership exist; actual runtime/provider/operator coverage and fallback evidence remain unverified. This does **not** block the correctness-first CPU reference path; it blocks selecting or claiming an accelerated provider | Documents 04–08, 14, 35, 42 and 43; later G4 and named-device evidence | A named accelerated backend and its support claims only |
| PC-OPEN-06 | DI/composition mechanism | **DESIGN CONTRACT CLOSED**: explicit `app/bootstrap` composition root with manual constructor injection and scoped factories is the default; no framework is required | Document 36 section 3.4; document 45 sections 4, 5 and 14 | No remaining design blocker; building the concrete graph is implementation work |
| PC-OPEN-07 | Tokenizer implementation and API binding | Tokenizer G1-D remains open; tokenizer artifact/profile selection is not finalized | Document 34 and G1-D evidence/decision | Concrete `engine/tokenizer` implementation |
| PC-OPEN-08 | Selected model family and backend binding | G4 remains blocked and no architecture family is selected | Document 43 acceptance and required comparative evidence | Selected `engine/model` implementation |
| PC-OPEN-09 | Session/report persistence split | Logical owners are known, but exact data-domain partition and durable schema are open | Storage/transaction contract review under document 36 | Final repository module boundary |
| PC-OPEN-10 | Structural evolution and sparse-training feature binding | The generation-scoped lifecycle contract exists, but no growth trigger, mutation catalog, concrete tensor migration, sparse format, optimizer-state policy or Android backend kernel is selected | Documents 16, 36, 42 and 43; resolve only for a release/profile that enables model growth or sparse training | Growth/sparse feature implementation only; explicitly deferred features do not block a structurally fixed dense first profile |
| PC-OPEN-11 | Android background-training execution binding | The platform decision matrix is documented, but the production FGS type and direct-FGS versus WorkManager/hybrid binding cannot be closed until the distribution route, exact user-visible purpose and platform/policy eligibility are confirmed | Document 21; current app baseline is minSdk 33, compileSdk 36.1, targetSdk 36; verify exact Android FGS and Google Play policy against the intended release channel | Continuous background training only; session, checkpoint and foreground-only training contracts are not blocked |

The dataset identity/admission/split contract, safe optimizer-update boundary, orthogonal session/candidate/checkpoint/active-pointer state axes, reproducible evaluation record and idempotent compare-and-swap promotion semantics are **DESIGN CONTRACT CLOSED** in document 36 sections 4, 5.2, 5.5, 5.8, 6.2 and 6.2.1. Their concrete storage, engine and test bindings remain implementation/verification work. PC-OPEN-11 is intentionally limited to the Android production scheduling binding; it does not reopen the platform-neutral session lifecycle.
 
PC-OPEN-02, PC-OPEN-03 and PC-OPEN-06 remain in this register for traceability but are closed at the design-contract level. Their implementation and verification obligations remain. PC-OPEN-01 and PC-OPEN-04/07/08/09 remain scoped blockers for the personal-capture path, durable storage, concrete tokenizer and selected model family respectively. PC-OPEN-05 is deferred only for accelerated-provider selection; it is not a reason to block a correctness-first CPU reference path. PC-OPEN-10 is a separately scoped future-feature blocker and does not block a first profile that explicitly defers growth and sparse training.

The remaining open items must not be used to block unrelated subsystems. They do prevent inventing production interfaces, creating speculative packages/modules or claiming that the affected implementation surface is ready.

## 11. Relationship to existing documents

| Document | Authority retained | This document adds |
|---|---|---|
| 01 — System Requirements and Decisions | Accepted product constraints and ADRs | No new product decision |
| 13 — Security and Data Governance | Privacy, consent, rights and deletion rules | Mapping of enforcement and persistence boundaries |
| 22 — General Corpus and Personal Learning | General/personal learning separation | Handoff boundary from approved candidate to learning |
| 24 and 26 — Acquisition/Source Registry | Discovery, source permissions and connector behavior | Application coordinator versus concrete connector ownership |
| 34 — Tokenizer Evaluation | Tokenizer research and G1-D | Package/consumer/provider mapping only |
| 36 — Pre-Implementation Module and Lifecycle Contracts | Logical API, lifecycle, identity, transaction and error contracts | Package-level port ownership, consumers and adapter mapping |
| 21 — Device-Adaptive Model and Training | Device/resource adaptation, Android execution policy and backend capability | Scheduler/FGS decision matrix and resource admission mapping |
| 41 — Recurrent State and Chunked Training | Stateful training/recovery semantics | Checkpoint boundary is carried into the package matrix |
| 43 — G4 Architecture Decision | Architecture decision gate and profile criteria | No architecture selection or engine implementation binding |
| 44 — Documentation Governance | Source-of-truth, evidence and authorization rules | This document follows the existing document hierarchy |
| 45 — Android Project Structure | Target package hierarchy and allowed dependency direction | Contract-by-contract package, consumer and provider crosswalk |
| 46 — Android Source-Tree Audit | Observed files, dependency couplings and package cycle | Maps each legacy coupling to the contract boundary needed to remove it |
| 48 — Personal Capture and Experience Intake | No separate product authority; specializes accepted privacy/personal-learning rules | Personal capture ownership, consent, routing, review, retention, revocation, deletion and AccessibilityService continuity |

If a method shape or lifecycle guarantee differs between this mapping and document 36, document 36 is authoritative for the logical contract. If a package assignment differs from document 45, document 45 is authoritative for target package ownership until formally revised. This document must then be reconciled; it must not silently override either source.

## 12. Completion and next work

The package-interface mapping is complete enough for a bounded contract review when:

- every existing cross-subsystem handoff has one proposed owner and a permitted consumer set;
- every adapter family has an explicit contract it implements;
- state, persistence and transaction authority are not assigned to UI, notification or generic event infrastructure;
- the known root/UI/service cycle has an explicit inversion path;
- personal capture has one proposed intake owner, a purpose-specific consent boundary and a mandatory record/review handoff before training;
- all unresolved API, storage, DI, tokenizer, architecture and runtime bindings remain explicit;
- forbidden dependency checks and later verification obligations are listed;
- no text implies that any proposed interface/package already exists in the prototype.

**Current disposition:**
- Package-to-interface/consumer/provider mapping: documented as a proposal.
- General-corpus intake: user-controlled local import is the primary path under document 22. The first transitional importer exists at `platform/android/storage/LocalDatasetImporter.kt` and is called from the Learning screen through `ArgusViewModel`; it streams common text formats, stages ZIP input on disk and parses archive entries incrementally, stages previews and commits app-private manifests/records. The final `data/corpus` repository and `application/learning` workflow boundaries remain future work.
- Autonomous public-source acquisition: optional later capability under documents 24–32; source-by-source permission review does not block local user-imported datasets.
- Root/UI/service cycle removal: planned, not implemented or verified.
- Personal capture intake: design contract documented in document 48; PC-IMPL-01–PC-IMPL-07 are Phase 5 bindings and do not block tokenizer/architecture design or unrelated foundation implementation.
- UI state, notification navigation and composition strategy: design contracts closed; immutable UI state declarations now exist for all four features. Feature-specific ViewModels and workflow/API wiring remain implementation work.
- Exact feature-specific Kotlin signatures: implementation bindings; no additional package-level contract gap is identified here.
- Compute provider selection: deferred for accelerated execution; CPU reference remains the correctness baseline.
- Durable storage schema and session/report partition: open under PC-OPEN-04/09.
- Gradle module extraction: deferred.
- Source movement, package creation, build changes and implementation: not authorized.
- Build, tests, GitHub Actions, artifact publication and device execution: not performed.

**Next documentation task:** close G1-D in document 34, then prepare the G4 family/profile synthesis under documents 35 and 38–43. Do not create another numbered document; record decisions in document 34 and the existing ADR section of document 01, then synchronize only direct dependents. PC-IMPL-01–PC-IMPL-07 belong to the later Phase 5 personal-learning workstream and must not be pulled forward as a prerequisite for architecture selection or foundation implementation.

## 13. Change history

- 2026-11-04 — Initial package/interface crosswalk prepared against `main` HEAD `afb8b30214f44620f1ff17e6735560fb2a706b0a`, using documents 36, 45 and 46. No source, build configuration, test or workflow was changed.
- 2026-11-04 — Cross-checked source-registry ownership and personal-capture requirements against documents 13, 22, 24 and 26. The source registry is assigned to `data/acquisition`, while user consent/revocation remains in `data/policy`; the capture-ingress gap is narrowed to API ownership/routing, not missing privacy principles.
- 2026-11-05 — PC-OPEN-01 is now documented by document 48. Updated IF-11, IF-14 and IF-20 to assign intake/review to `application/knowledge`, candidate/session admission to `application/learning`, and the capture adapter to the application intake boundary. Concrete bindings remain open.
- 2026-11-05 — Closed PC-OPEN-02, PC-OPEN-03 and PC-OPEN-06 at the design-contract level by binding immutable workflow snapshots, the internal learning-session notification route and manual constructor injection to documents 36 and 45. Reclassified PC-OPEN-05 as an accelerated-provider-only blocker; no code or build files were changed.

- 2026-11-05 — Reconciled the existing-prototype matrix with the current `ArgusContainer`/`ArgusRuntimeOwner` source and the new Settings-owned immutable UI projection. No new API or workflow contract was claimed; application command ownership and full ViewModel decomposition remain open.

- 2026-11-05 — Updated the prototype map for feature-owned Learning and Perception UI-state projections. This is presentation-boundary progress only; application workflow command ownership and per-feature ViewModels remain open.

- 2026-11-05 — Updated the existing-prototype mapping to record the Chat feature's immutable `ChatUiState` projection. Application inference workflow ownership remains open.


- 2026-11-05 — Removed the known direct raw-string training auto-enqueue calls from chat, file/image selection and Settings web-search results. This was an interim guard; the legacy manager/worker and engine raw-text training entry point were subsequently retired. No replacement application/learning workflow is claimed; no build/test result is claimed.


- 2026-11-05 — Updated the prototype map after removing `TrainingManager.kt` and `ArgusNeuralEngine.trainStep(String)`. The current app has no training worker or Learning session coordinator; the UI and foreground notification state that the capability is unavailable. The design contracts in documents 36 and 48 remain the requirements for any future implementation.


- 2026-11-05 — Implemented the first transitional local corpus importer under IF-16. The Learning screen can import the bundled owner-created ArgusID TXT and DigiTransID archive or user-selected local TXT/Markdown/JSONL/NDJSON/ZIP files; preview, deduplication, SHA-256 metadata, document-level deterministic splits and app-private commit are included. The Android storage adapter currently combines parsing and storage; final `data/corpus` / `application/learning` separation remains open. Import does not create a training session, and no training coordinator is claimed.
- 2026-11-05 — Reworked IF-16 intake to stream user-selected text sources, stream ZIP entries after disk-backed archive staging, use a temporary SQLite digest index for deduplication, and use `Long` record counters. Removed fixed per-document, archive and aggregate byte limits and the in-memory unique-record cap; bounded memory now depends on fixed read buffers and a bounded individual record/JSONL row. No build or device verification is claimed.
