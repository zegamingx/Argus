# Roadmap and Technology Research Register

**Review date:** 2026-11-03 · **Status:** Planning baseline · **Branch:** main

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
Maintain the source/build baseline and P0 audit. Complete architecture-family research, tokenizer research, corpus provenance/quality review, Genesis lifecycle, device capability and adaptive training specifications, and the autonomous acquisition source/policy contract. No source implementation, build or workflow execution belongs to this phase unless separately authorized.

### Phase 1 — Architecture decision review (not started)
Choose the first architecture only after comparative evidence. Approve tokenizer contract, Small C4 revision and preprocessing, per-device configuration policy, training objective/optimizer profile, Genesis readiness contract, autonomous acquisition source eligibility and local data lifecycle, personal data lifecycle and verification plan. Record decisions in ADRs.

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
| R-010 | On-device training | ASSESS | Measure backward operators, optimizer memory, throughput and thermal endurance |
| R-011 | Decoder-only Transformer | RESEARCH CANDIDATE | Measure training correctness, mobile memory, context cost and runtime coverage |
| R-012 | State-space / recurrent sequence models | RESEARCH CANDIDATE | Measure sequence scaling, training stability, kernel maturity and inference state |
| R-013 | Hybrid neural architectures | RESEARCH CANDIDATE | Measure integration overhead and device-level outcomes |
| R-014 | Small C4 bootstrap corpus | CANDIDATE | Pin revision, inspect inherited C4/Common Crawl terms, re-evaluate split integrity and calculate tokens |
| R-015 | Tokenizer family | OPEN | Compare byte-level, byte-level BPE and corpus-trained alternatives |
| R-016 | Device-adaptive training controller | OPEN | Define measured capability profile and dynamic resource admission |
| R-017 | Architecture evolution | FUTURE RESEARCH | Define migration, candidate validation and rollback before structural mutation |
| R-018 | Autonomous knowledge acquisition | ACCEPTED DESIGN / OPEN IMPLEMENTATION | Define source allowlist, rights policy, deterministic bootstrap discovery, local storage, quotas, Android scheduling and quality gates |

## 4. Technology radar

- **ADOPT:** selected for a defined scope with evidence.
- **TRIAL:** limited proof-of-concept planned or underway.
- **ASSESS:** investigate; no commitment.
- **HOLD:** blocked by maturity, access, licensing or resource limits.
- **RETIRE:** remove an existing path under a migration plan.

Tags apply to a specific version and platform, not globally to a vendor or architecture family.

## 5. Review cadence and exit conditions

Review SDKs before implementation decisions; hardware entries at least quarterly; Android API lifecycle at major platform releases; and corpus terms before every distribution revision. Record source URL, revision/publication date and last-checked date.

End the documentation phase only when the gate in [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md) passes. Completing a document or checking a roadmap item does not authorize coding, builds, CI workflows or artifact publication.
