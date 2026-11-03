# System Architecture

**Status:** Proposed logical architecture  
**Last reviewed:** 2026-11-03

## 1. Objective

Separate product behavior from model implementation, hardware vendors and operating systems. Shared logic expresses intent and policy; platform adapters perform work using documented OS/vendor interfaces.

## 2. Logical layers

1. **Experience:** chat, settings, permissions, approvals and transparency dashboards.
2. **Application orchestration:** use cases, lifecycle, scheduling, cancellation and workflow state.
3. **Agent/tool layer:** perception, reasoning, memory, action and learning as bounded roles using typed messages.
4. **Cognition services:** context assembly, planning, retrieval, evaluation, policy checks and deterministic utilities.
5. **Model services:** registry, tokenizer, inference sessions, embeddings, VLM/audio services and adapter lifecycle.
6. **Compute abstraction:** capability discovery, backend registry, compilation, allocation, dispatch, profiling and fallback.
7. **Platform adapters:** Android APIs, CPU kernels, Vulkan and vendor NPU SDKs; future platform bindings remain isolated.
8. **Data layer:** encrypted metadata, conversation/memory stores, model files, checkpoints and audit records.
9. **Trust/governance plane:** consent, permission state, network policy, approval, retention and kill switch.

The trust plane is cross-cutting; capture, memory, network, training and action flows must consult it.

## 3. Compute dispatch

`request → policy check → model requirements → capability discovery → compatible backend candidates → resource/thermal admission → dispatch → measured result → completion or fallback`

Candidate selection considers:
- operator coverage and graph partitioning;
- datatype, precision and quantization;
- compiler and model-conversion requirements;
- accelerator and system memory;
- driver/runtime version and known defects;
- transfer, synchronization and launch cost;
- latency, throughput, energy and thermal state;
- numerical correctness and fallback support.

Backend preference is not a capability guarantee. Every run reports actual backend and whether execution was partitioned or fell back.

## 4. Agent boundaries

Agents are logical responsibilities, not a requirement to run multiple large models. Shared sessions may be more efficient on mobile. The orchestrator owns dependency tracking, cancellation/deadlines, permission checks, bounded concurrency, recovery and provenance. Agents cannot bypass platform permissions or invoke privileged tools directly.

## 5. Data and trust boundaries

- Capture enters a short-lived buffer.
- Consent and secure-surface exclusion are checked before processing.
- Redaction/classification precedes persistent storage or learning-candidate creation.
- Retrieval retains provenance and sensitivity.
- Model output is untrusted until policy and deterministic checks pass.
- Consequential actions require a distinct authorization gate.
- Deletion propagates to derived indexes, caches and training artifacts under the retention policy.

## 6. Failure isolation

Isolate model-load, tokenizer, compiler, GPU/NPU driver, memory, thermal, storage, capture service, network and agent failures. A failing optional accelerator must not crash the whole app. All jobs must support bounded resource use and cancellation.

## 7. Platform boundary

Android is the first target. Android permissions, SAF, MediaProjection, AccessibilityService and foreground-service rules remain platform-specific. Embedded Linux and desktop require separate packaging, drivers, lifecycle and security models; they are not achieved by reusing an APK.

## 8. Architecture outputs still required

Before implementation, specify module/API contracts, error taxonomy, event/message schema, model lifecycle, concurrency rules, data schema, deployment diagrams and sequence diagrams. Diagrams must not imply an unimplemented component already exists.

## 9. Argus Genesis and independent-instance integration

The accepted target architecture adds a Genesis subsystem between application startup and long-running learning. It is a planned design, not an existing implementation.

~~~text
Application Launch
       |
       v
Genesis Coordinator
       |
       +--> Device Capability Assessment
       |
       +--> Architecture Candidate / Configuration
       |
       +--> Model Graph Construction
       |
       +--> Per-Instance Parameter Initialization
       |
       +--> Initial Generation Manifest + Atomic Commit
       |
       v
Application Ready
       |
       +--> Background General-Learning Scheduler
       |
       +--> Personal Interaction / Memory Services
                    |
                    +--> Explicitly Authorized Personal-Learning Candidates
                    |
                    +--> Validated Model-Generation Updates
~~~

### 9.1 Startup boundary

Only device assessment required for configuration, model construction, parameter initialization and initial-state persistence belong to the blocking Genesis phase. Full corpus tokenization and pretraining do not belong on the splash-screen critical path.

The application may enter its usable state only after a valid initial model generation has been committed. If initialization fails, the app must expose a recoverable state rather than silently creating a partially initialized model.

### 9.2 Learning boundaries

General learning consumes the bundled bootstrap corpus and is scheduled after the application is ready. Personal learning has an independent permission and candidate-review boundary. Both use the same instance-local model-generation manager, but retain separate provenance, objectives, evaluation and deletion lineage.

### 9.3 Instance isolation

An instance has its own instance identifier, model generations, weights, checkpoints, memory and authorized personal dataset. No central coordinator receives the model, gradient, optimizer state or personal learning corpus. Cross-user model aggregation and automatic synchronization are outside scope.

### 9.4 Device adaptation boundaries

The Genesis-time capability profile constrains the initial model configuration. A separate runtime controller constrains current workload based on live conditions. Neither controller may silently change architecture or model tensor shapes during a training step; structural changes require a candidate generation, migration and validation.

Detailed contracts are in [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md) and [21 — Device-Adaptive Model and Training](21_DEVICE_ADAPTIVE_MODEL_AND_TRAINING.md).
