# Roadmap and Technology Research Register

**Status:** Planning baseline; future products/dates are subject to change  
**Last reviewed:** 2026-11-03

## 1. Phases

### Phase 0 — Documentation and evidence (current)
Establish vision, inspect source/build baseline, write requirements and architecture, research CPU/GPU/NPU/SoC/memory/storage, define model/runtime/learning/memory/security, and design performance/verification plans. **No implementation work belongs to this phase.**

### Phase 1 — Architecture review (not started)
Approve requirements and ADRs; define interface contracts and error taxonomy; select support tiers/device shortlist; agree data lifecycle/resource budgets; approve test strategy and risk register.

### Phase 2 — Implementation planning (not started)
Create a dependency-ordered plan with acceptance criteria, tests, migration and rollback. Explicitly identify deferred features.

### Phase 3 — Implementation and validation (not authorized)
Begins only after the user explicitly opens the implementation gate. Unsupported hardware remains unsupported rather than simulated.

## 2. Research register

| ID | Topic | Position on 2026-11-03 | Follow-up |
|---|---|---|---|
| R-001 | Android LiteRT CompiledModel | Current Google direction for managed CPU/GPU inference | Recheck maturity, API, device coverage and NPU roadmap |
| R-002 | Qualcomm QAIRT/QNN/GENIE | Public docs and SDK ecosystem exist | Verify licensing, APK packaging and exact SoC support |
| R-003 | MediaTek NeuroPilot | Public delegate route and vendor-partition restrictions documented | Verify current LiteRT route and device coverage |
| R-004 | NNAPI lifecycle | Deprecated in Android 15; removal timeline announced for 2027 | Avoid new dependency except justified legacy path |
| R-005 | XRING ecosystem | New products/claims need evidence and public SDK | Wait for compiler/runtime and third-party app access |
| R-006 | UNISOC accelerator access | SKU-specific claims; public APK SDK unclear | Separate CPU/GPU fallback from proven NPU support |
| R-007 | Embedded stacks | RKNN, eIQ, TIDL, DRP-AI, OpenVX are platform-specific | Maintain separate board/OS profiles |
| R-008 | Arm SVE/SME and RISC-V Vector | Future CPU optimization families | Track shipping CPU, compiler and probe maturity |
| R-009 | Desktop AI compute | OpenVINO, ROCm, CUDA/TensorRT and Core ML evolve | Separate platform adapters only after approval |
| R-010 | On-device training | Backward ops, memory and thermal limits remain material | Benchmark LoRA and optimizer budgets on target devices |

## 3. Technology radar

- **ADOPT:** selected for a defined scope with evidence.
- **TRIAL:** limited proof-of-concept planned/underway.
- **ASSESS:** investigate; no commitment.
- **HOLD:** blocked by maturity, access, licensing or resource limits.
- **RETIRE:** remove an existing path under a migration plan.

Tags apply to a specific version and platform, not globally to a vendor.

## 4. Review cadence

Review SDKs before each implementation decision; hardware entries quarterly; OS/API deprecations at major platform releases; and any product-copy claims before release. Record source URL, publication/revision date and last-checked date.

## 5. Exit conditions

End the documentation phase only when the gate in [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md) passes. Number of documents or a checked checklist alone is not authorization to code.
