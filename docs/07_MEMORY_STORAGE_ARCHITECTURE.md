# Memory and Storage Architecture

**Status:** Proposed design  
**Last reviewed:** 2026-11-03

## 1. Memory domains

Distinguish CPU caches; system DRAM; Android managed heap; native heap; mapped pages; GPU memory; NPU/DSP local memory; persistent storage; and OS memory pressure. Java heap size is not total process/device memory. Mobile GPU/NPU allocations may use shared system DRAM but still have separate ownership and synchronization rules.

## 2. Model memory accounting

Preflight:
- weights and quantization metadata;
- tokenizer and model metadata;
- KV cache;
- activation/workspace buffers;
- runtime/compiler caches;
- adapters and optimizer state during training;
- temporary conversion/upload copies;
- app and OS headroom.

For a decoder-only transformer, a simplified estimate is:

`KV bytes ≈ 2 × layers × tokens × KV-heads × head-dimension × bytes-per-element × concurrent-sequences`

Use actual KV-head count, cache precision, sliding-window policy, batch and architecture. This is not a guarantee of peak allocation.

## 3. Allocation and data movement

Prefer bounded reusable buffers in hot paths. Avoid unnecessary CPU↔GPU/NPU transfers and tensor layout conversions. mmap requires defined lifetime, alignment, page-fault and integrity behavior. Shared physical memory does not imply unified API or coherency semantics.

Preflight allocations; handle failure without corrupting model state; release accelerator resources on unload, cancellation and device loss.

## 4. Persistent storage classes

Keep model artifacts/manifests, tokenizers, user memories, conversations, approved training data, checkpoints/adapters and diagnostics logically separate. Use app-private storage by default. Use Android Storage Access Framework for user-selected import/export; do not require broad storage permission as a shortcut.

## 5. Model lifecycle

Stage imports/downloads; validate checksum, schema, format, tokenizer, license and runtime compatibility; estimate memory; then promote atomically. Preserve a last-known-good model and adapter. Deletion must include derived indexes, caches and temporary copies according to disclosed retention rules.

## 6. Resource profiles

Low/medium/high may summarize a profile in UI, but the router needs actual numerical budgets for usable memory, thermal state, accelerator support and concurrent workload. Avoid using advertised total RAM as available model memory.
