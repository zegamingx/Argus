# Contributing to Argus

Argus is an experimental local-first Android AI research project. The current repository contains an early prototype; major Genesis, learning, runtime and evolution capabilities remain design targets. Contributions must distinguish implemented behavior from planned architecture.

## Core project rules

1. **Local Knowledge Genesis:** do not introduce third-party pretrained weights as the foundation or identity of Argus.
2. **Independent instances:** do not add central training, federated learning, cross-user model/gradient exchange or automatic model synchronization.
3. **No silent collection:** source access is not consent to retain or train. Personal data requires purpose-specific permission and an enforceable candidate lifecycle.
4. **Local-first operation:** core model creation, learning, inference and personal memory are designed to remain on-device. Any user-requested network action must be disclosed.
5. **External data is untrusted:** web, file, screen and OCR content must not become instructions or training data merely because it was retrieved.
6. **No simulated acceleration:** fallback must be transparent. Report the backend that actually executed the workload.
7. **No unsupported claims:** do not describe a design target as implemented or verified without code and test evidence.

## Documentation and decision authority

Follow [44 — Documentation Governance and Change Control](docs/44_DOCUMENTATION_GOVERNANCE_AND_CHANGE_CONTROL.md) for documentation authority and change control, and [45 — Android Project Structure and File-Placement Rules](docs/45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md) plus [46 — Android Source-Tree Ownership and Dependency Audit](docs/46_ANDROID_SOURCE_TREE_OWNERSHIP_AND_DEPENDENCY_AUDIT.md) for package ownership, file mapping, dependency direction, Gradle boundaries and source/resource/test placement.

- Keep the numbered documents in docs/ at their current paths. Do not rename, renumber or move them without an approved link-migration plan.
- Keep application source within approved package ownership boundaries. Do not create a Gradle module, native source tree or cross-layer dependency without the review required by document 45 and the current audit findings in document 46.
- Record accepted product and cross-system decisions in document 01. A proposal, paper result, research synthesis or topic-specific recommendation is not an accepted decision by itself.
- Keep unresolved questions and phase/gate status in document 16; keep observed implementation facts and audit findings in documents 17–19.
- Update the canonical owner first, then only its affected dependents and indexes. Avoid contradictory copies and broad edits that merely repeat existing text.
- The active scope is documentation-only. Do not change application source/build configuration or run builds, tests, workflows or device experiments until a separate implementation phase is explicitly authorized.
- Completing a document or closing a research gate does not grant implementation authorization.

## Genesis and learning rules

- First launch creates the local model instance: device assessment, viable configuration, model graph construction, parameter initialization and initial-generation commit.
- Application readiness must not wait for full general-corpus pretraining.
- General learning uses the approved bundled bootstrap corpus and runs after readiness under Android scheduling/resource constraints.
- Personal learning is separate from general learning and requires explicit scope, provenance, classification, approval, revocation and deletion controls.
- Model configuration is device-adaptive; live training workload is condition-adaptive.
- Architecture, tokenizer and optimizer choices remain research decisions until an approved ADR is recorded.
- Do not hard-code Transformer or Q/V LoRA as Argus-wide requirements before research and review.

## Development setup

- **Language:** Kotlin 2.2.10 in the inspected application prototype.
- **JDK expectation:** 21 LTS.
- **Build:** Gradle 9.3.1, AGP 9.1.1 in the inspected repository snapshot.
- **Native toolchain:** NDK/CMake are research/development candidates; the inspected source tree has no native C++ module.
- **Architecture:** follow the current documentation and baseline; do not assume that all target layers exist in code.

## Pull request process

1. Branch from the agreed working branch and explain the design impact.
2. Include tests for numerical, lifecycle, persistence, security or device behavior changed by the PR.
3. Update requirements, architecture, audit and baseline documents when behavior changes.
4. Preserve the privacy and independent-instance rules.
5. Do not introduce build artifacts or model weights without documented provenance, licensing and explicit review.

## Testing expectations

- **Numerical:** trusted-reference parity and finite-difference gradient checks for trainable operations.
- **Persistence:** complete model/checkpoint round trips, checksum failures and interrupted-write recovery.
- **Privacy:** consent defaults, revocation, approval enforcement, data deletion and no hidden network paths.
- **Runtime:** verified execution, numerical tolerance and observable fallback.
- **Device:** memory pressure, thermal throttling, Android lifecycle and process-death recovery.

A test suite or build result is valid only for the exact source revision, toolchain and device configuration that were actually tested.
