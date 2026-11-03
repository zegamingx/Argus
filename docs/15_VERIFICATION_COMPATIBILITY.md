# Verification, Validation and Compatibility

**Status:** Proposed verification plan  
**Last reviewed:** 2026-11-03

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
- **Recovery:** process kill, OS reclaim, partial writes, adapter promotion and rollback.

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

For each requirement in [01 — System Requirements](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md), track design section, test ID, environment, result, evidence, defect and status. A MUST requirement is not complete without test evidence or a formally accepted waiver.

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

Passing the gate authorizes an implementation-plan review only, not automatic coding or production readiness.
