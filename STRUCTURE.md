# Argus Repository Structure

```
Argus/
├── app/
│   ├── entry/                 # Android Application/Activity entry points
│   ├── bootstrap/             # Composition root and dependency graph construction
│   └── navigation/            # Routes and destination wiring
├── feature/
│   ├── chat/presentation/
│   ├── learning/presentation/ # User-facing model/training controls and status
│   ├── perception/presentation/
│   └── settings/presentation/
├── application/
│   ├── genesis/               # Initial generation workflow and readiness
│   ├── inference/             # Request/context workflow and cancellation
│   ├── learning/              # Training admission, session and candidate lifecycle
│   ├── acquisition/           # Discovery/fetch job orchestration and handoff
│   ├── knowledge/             # Retrieval and memory workflows
│   └── evaluation/            # Candidate evaluation and promotion coordination
├── core/
│   ├── identity/              # Instance, generation, session and stable IDs
│   ├── model/                 # Model config, generation and repository contracts
│   ├── tokenizer/             # Tokenizer identity, manifest and API contracts
│   ├── inference/             # Inference request/snapshot contracts
│   ├── training/              # Training session, update and recovery contracts
│   ├── compute/               # Execution capability and backend contracts
│   ├── device/                # Stable capability and live-condition contracts
│   ├── policy/                # Consent, purpose, authorization and retention rules
│   ├── knowledge/             # Knowledge, memory and provenance domain contracts
│   ├── acquisition/           # Source, operation-scope and job contracts
│   ├── evaluation/            # Metrics, gates and promotion invariants
│   └── events/                # Typed lifecycle/audit event envelopes and schemas
├── data/
│   ├── model/                 # Generation, tensor and checkpoint repositories
│   ├── corpus/                # Dataset manifests, splits and readers
│   ├── knowledge/             # Durable records and retrieval indexes
│   ├── acquisition/           # Concrete source connectors, fetch and parsing
│   ├── policy/                # Durable consent, exclusion and policy records
│   └── audit/                 # Sanitized durable audit/diagnostic records
├── platform/
│   └── android/
│       ├── lifecycle/         # WorkManager, foreground service and OS callbacks
│       ├── permissions/       # Android permission state and platform request flows
│       ├── capture/           # Accessibility, camera and authorized capture
│       ├── device/            # Android hardware and runtime-condition observations
│       ├── storage/           # SAF/filesystem/key APIs and platform storage access
│       ├── connectivity/      # Android connectivity and network-state observation
│       ├── notification/      # Android notification adapters
│       └── compute/           # Android/Vulkan/vendor runtime and capability adapters
├── engine/
│   ├── reference/             # Correctness-first numerical reference path
│   ├── model/                 # Selected family graph/initialization implementation; after G4
│   └── tokenizer/             # Selected concrete tokenizer implementation; after G1-D
└── ui/
    └── design/                # Shared Compose design system and visual primitives
```
