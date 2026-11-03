# Argus Engineering Documentation

**Baseline date:** 2026-11-03 · **Status:** Design draft · **Scope:** Documentation only

This directory is the engineering source of truth for Argus planning. It distinguishes verified facts, current repository behavior, proposed architecture and future research. A document here does not mean its capability is implemented.

## Project vision

Argus is intended to become a privacy-first, user-controlled, on-device intelligent assistant framework. Its long-term design explores perception, reasoning, memory, tool use and continual adaptation. "AGI" is an aspiration in the product identity, not evidence of human-level general intelligence; capabilities must be described and tested measurably.

## Working rules

1. **Documentation first:** this phase changes documentation only. No runtime, native code, hardware probing, training engine or feature implementation is authorized.
2. **Separate fact from plan:** use `IMPLEMENTED`, `VERIFIED`, `AVAILABLE`, `PLANNED`, `EXPERIMENTAL`, `FUTURE RESEARCH`, `DEPRECATED` and `UNSUPPORTED` precisely.
3. **Evidence required:** hardware and SDK claims need primary sources, version/date and last-verified date.
4. **No chipset-name routing:** identity is a hint. A backend is usable only after runtime, driver, operator, precision, memory and model compatibility checks.
5. **No false portability:** Android, embedded Linux, desktop and vendor firmware have different APIs, permissions, ABIs and deployment models.
6. **Observable fallback:** report the backend actually used and why a fallback occurred. Never simulate acceleration.
7. **Design for change:** use versioned capability contracts instead of predicting every future chipset.
8. **No unsupported product claims:** "local-first" must disclose user-requested network exceptions; "NPU accelerated" needs real execution evidence.

## Document map

| Document | Scope |
|---|---|
| [01 — System Requirements and Decisions](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md) | Vision constraints, requirements, priority and initial ADRs |
| [02 — System Architecture](02_SYSTEM_ARCHITECTURE.md) | Layers, trust boundaries, interfaces and data flows |
| [03 — Processor Architecture](03_PROCESSOR_ARCHITECTURE.md) | CPU families, ISA, SIMD, topology and detection |
| [04 — GPU Architecture](04_GPU_ARCHITECTURE.md) | GPU families, compute APIs, drivers and constraints |
| [05 — NPU Acceleration](05_NPU_ACCELERATION.md) | NPU/DSP, vendor SDKs, compiler and third-party access |
| [06 — SoC Hardware Matrix](06_SOC_HARDWARE_MATRIX.md) | Mobile, embedded, desktop and server watchlist |
| [07 — Memory and Storage](07_MEMORY_STORAGE_ARCHITECTURE.md) | RAM, cache, bandwidth, KV cache, mmap and persistence |
| [08 — AI Runtime Backends](08_AI_RUNTIME_BACKENDS.md) | LiteRT, QAIRT/QNN, NeuroPilot, Vulkan and other runtimes |
| [09 — Model Architecture](09_MODEL_ARCHITECTURE.md) | LLM/VLM/audio/embedding, formats and quantization |
| [10 — Training and Learning](10_TRAINING_LEARNING_ARCHITECTURE.md) | LoRA, optimizer, data lifecycle, evaluation and rollback |
| [11 — Memory and Knowledge](11_MEMORY_KNOWLEDGE_ARCHITECTURE.md) | Working, episodic, semantic, procedural and vector memory |
| [12 — Platform Expansion](12_PLATFORM_EXPANSION.md) | Android, SBC, embedded Linux, desktop and future platforms |
| [13 — Security and Data Governance](13_SECURITY_PRIVACY_DATA_GOVERNANCE.md) | Threat model, consent, secrets, network and deletion |
| [14 — Performance and Power](14_PERFORMANCE_POWER_THERMAL.md) | Benchmarks, memory budgets, thermal and energy |
| [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md) | Tests, evidence and implementation completion gate |
| [16 — Roadmap and Research](16_ROADMAP_RESEARCH_REGISTER.md) | Phases, technology radar and open research |
| [17 — Current Repository Baseline](17_CURRENT_REPOSITORY_BASELINE.md) | Facts and gaps in inspected `main` |

## Status vocabulary

- **IMPLEMENTED:** code exists; correctness is not implied.
- **VERIFIED:** a defined test passed on a named device/software configuration.
- **AVAILABLE:** public standard, product, API or SDK exists; Argus integration is not implied.
- **PLANNED:** accepted design intent, not implemented.
- **EXPERIMENTAL:** prototype or limited integration, not guaranteed.
- **FUTURE RESEARCH:** worth monitoring; no delivery commitment.
- **DEPRECATED:** upstream discourages or sunsets it.
- **UNSUPPORTED:** explicitly outside the current support scope.

A compatibility record must include exact device/SKU, OS build, ABI, driver/runtime/compiler versions, model and precision, operator coverage, test date, evidence, limitations and fallback.

## Change control and completion

Material decisions belong in the requirements/decision document. Unresolved questions belong in the research register. Fast-moving vendor SDKs must be rechecked before selecting dependencies and before implementation. Update the repository baseline whenever code or build configuration changes.

The documentation phase ends only when requirements, architecture, hardware/runtime matrices, data governance, resource budgets, verification plan and cross-document consistency have been reviewed. Passing the gate authorizes an implementation-plan review only; it does not automatically authorize coding.
