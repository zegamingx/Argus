# Verification, Validation and Compatibility

**Status:** Proposed verification plan  
**Last reviewed:** 2026-11-04

## 1. Evidence principle

Compatibility applies to an exact configuration. "Snapdragon supported" or "Mali supported" is too broad. Record device SKU, SoC revision, OS build, driver, SDK/runtime, model, quantization and test version.

## 2. Verification layers

- **Static:** build/dependency/license review, secret scanning, deprecated API audit and architecture rules.
- **Unit:** tokenizer, serialization, tensor math, manifest, capability selection, consent and redaction.
- **Numerical:** trusted-reference comparison, gradient checks, quantized deviations and edge cases.
- **Integration:** model load, inference, cancellation, fallback, checkpoint save/restore and driver failure.
- **Device:** thermal/memory pressure, lifecycle, permission revocation, low storage and background policy.
- **Security:** prompt injection, path traversal, malicious models, secure-surface exclusion, permission abuse and no-network assertions.
- **Performance:** reproducible workload and sustained thermal runs.
- **Recovery:** process kill, OS reclaim, partial writes, candidate-generation promotion and rollback.

## 3. Backend validation sequence

1. Verify runtime installation and ABI.
2. Query device and driver capability.
3. Load known test model.
4. Validate operator coverage and precision.
5. Compare outputs with trusted reference.
6. Run representative Argus workload.
7. Measure latency, throughput, memory and thermal behavior.
8. Inject runtime/driver failure and verify fallback.
9. Repeat after reboot and on supported OS builds.
10. Publish a compatibility record and limitations.

## 4. Device test tiers

- **A:** Android ARM64 device with verified CPU path.
- **B:** Qualcomm device with QAIRT/QNN test.
- **C:** MediaTek device with documented APK-accessible delegate.
- **D:** Adreno/Mali/Xclipse Vulkan driver variation.
- **E:** low-RAM and thermal-stress device.
- **F:** embedded Linux board and vendor SDK.
- **G:** desktop CPU/GPU/NPU after platform approval.

Emulator-only testing cannot complete a hardware tier.

## 5. Requirements traceability

For each requirement in [01 — System Requirements](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md), track design section, test ID, environment, result, evidence, defect and status. During the documentation phase, the requirement is design-complete when its intended behavior, rationale, measurable acceptance criteria and verification method are specified; do not claim it is implemented or verified. Once implementation begins, a MUST requirement is not implementation-complete without test evidence on its declared target environment or a formally accepted waiver.

## 6. Documentation completion gate

Before implementation planning:
- vision and scope are approved;
- requirements are measurable and traceable;
- architecture boundaries/failure domains are resolved;
- CPU/GPU/NPU/SoC entries have sources and uncertainty labels;
- runtime/model strategy is coherent;
- memory, security, power and thermal requirements exist;
- benchmark and verification methods are defined;
- known code gaps/security risks are acknowledged;
- ADRs and cross-document consistency are reviewed;
- links, source versions and review dates are checked.

Passing the gate authorizes an implementation-plan review only, not automatic coding or production readiness. The documentation gate reviews completeness of specifications and planned verification; it does not require implementation test results. Implementation verification remains a distinct later gate.


## 7. Build execution control

Build execution is gated separately from source inspection and audit work.

- During architecture audit and remediation, do not run Gradle build, test, lint or packaging tasks and do not trigger GitHub Actions.
- Do not upload or publish build artifacts.
- After the audit and remediation are complete and the final verification gate is explicitly accepted, perform the approved build from an immutable Git tag identifying the exact reviewed commit.
- Any future CI build workflow must be tag-triggered and must omit artifact-upload/publishing steps unless separately authorized.

A build result is valid only for the exact tagged commit and declared toolchain. A branch-head build is not a substitute for the approved tagged build.

## 8. Genesis and device-adaptive verification gates

The target architecture requires additional verification before it can be described as implemented:

| Area | Required evidence |
|---|---|
| First-launch readiness | Model initialization and initial generation commit complete before app-ready state; no corpus training on the blocking startup path |
| Genesis-to-training handoff | The immutable Genesis parent generation is identified by digest; training starts only after readiness and writes an isolated child candidate |
| Candidate promotion | A candidate is promoted only after required numerical, evaluation, compatibility and resource gates; failed candidates cannot replace the active generation |
| Resume transaction | Candidate tensors, optimizer/session state, RNG profile, data cursor and idempotency markers recover as one logical checkpoint transaction |
| Random initialization | Architecture-specific initializer tests, valid distributions/ranges, independent instance outputs and deterministic test fixtures |
| Model identity | Unique instance/generation IDs, architecture/tokenizer fingerprints and exact tensor manifest |
| Full-state persistence | Every model tensor and required optimizer/scheduler/RNG state round-trips; corruption and partial writes are rejected |
| Device profile | Stable hardware/runtime capability evidence and live Runtime Condition snapshots are recorded separately with source, timestamp and confidence; unknown values remain unknown |
| Adaptive configuration selection | The chosen model/tokenizer profile is reproducible from the versioned capability evidence, language/rights policy and resource budget; unsupported candidates and fallback reasons are explicit |
| Adaptive training | Workload responds to memory/thermal/power conditions without corrupting optimizer semantics or model structure |
| Background lifecycle | Pause, process death, Android reclaim, resume, notification and foreground-service policy are tested on named OS versions |
| Corpus | Source revision, provenance, license notices, split integrity, tokenizer token count, deduplication and checksum are reproducible |
| Tokenizer | Exact per-generation tokenizer profile/artifact fingerprint, Unicode and whitespace round-trip behavior, offset semantics, byte-fallback/unknown handling, special-token separation and reference/Android conformance are verified |
| Tokenizer preparation | Precomputed-profile selection or local-training reproducibility, bounded CPU/memory/storage/energy behavior, rights-cleared seed-corpus binding and pre-allocation fallback are verified; no token-ID changes occur after model tensor initialization |
| Architecture component comparisons | Evidence source, tokenizer sensitivity, disposition, matched budget, numerical checks and unresolved assumptions are recorded per component; no whole-model result is misattributed |
| Framework reuse | Exact source/license, Android API exposure, random-init support, operator/backward coverage, optimizer state, data-loop/checkpoint lifecycle and device-specific limitations are audited before integration |
| Personal learning | Unapproved/revoked candidates cannot update weights; general and personal data lineage remain separate |
| Instance isolation | No hidden model/data/gradient network path; no cross-user synchronization or shared training state |
| Architecture evolution | Candidate generation, tensor migration, regression gates and rollback are tested before promotion |

The documentary tokenizer shortlist/contract G1-D in [34 — Tokenizer Evaluation and Decision Framework](34_TOKENIZER_EVALUATION_AND_DECISION.md) and the pre-implementation component/design review in [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md) are required before an architecture design is frozen. The E1–E7 physical/device evidence in [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md) is a later implementation-verification requirement: it must pass before Argus claims broad from-scratch feasibility or support for a named device tier, not before approving a design for implementation. The application must not claim that a model has learned general language capability merely because Genesis initialization completed. Report constructed, initialized, trained and validated as separate lifecycle facts.
