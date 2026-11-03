# Device-Adaptive Knowledge Acquisition and Corpus Budgeting

**Status:** Accepted design direction; measurements, quotas and device admission thresholds remain open  
**Last reviewed:** 2026-11-03

## 1. Purpose

Argus adapts the model configuration to the device that owns the instance. The knowledge acquisition and local corpus pipeline must adapt to that same device as well.

A model configuration that fits a device is not sufficient if the acquisition system downloads more text, creates larger indexes, allocates parser buffers or schedules more training data than the device can safely store and process.

The device therefore receives two related but separately budgeted profiles:

1. **Device-Adaptive Model Profile:** model architecture/configuration, inference and training workload envelope.
2. **Device-Adaptive Acquisition Profile:** discovery, network intake, parsing, retention, indexing and corpus-preparation envelope.

The profiles share measured device evidence and resource admission. They do not share one fixed capacity number or assume that the model's RAM budget is also available to the acquisition engine.

## 2. Core invariants

1. **Same knowledge objectives:** device capability changes acquisition volume, pace, concurrency and retention depth; it must not remove broad-domain coverage from the product's knowledge objectives.
2. **Quality is not adaptive downward:** low-capability devices may process fewer items, but may not skip rights checks, provenance, security validation, deduplication or minimum quality gates.
3. **No universal corpus size:** each instance receives a locally feasible corpus and acquisition budget; no global document count, token count or storage size is hard-coded for every phone.
4. **Model and acquisition budgets are independent:** memory, storage, CPU, network, battery and thermal budgets must be partitioned rather than double-counted.
5. **Runtime conditions override static capacity:** a powerful device under thermal pressure, low battery or storage pressure may run a smaller acquisition workload or pause.
6. **Local ownership remains:** each installation maintains its own collected records, tokenized caches, indexes, corpus versions and training lineage.
7. **Canonical text and model-specific preparation are separate:** when rights permit, retain a normalized source record independent of tokenizer-specific training shards. Regenerate derived token caches only with a compatible model/tokenizer fingerprint.
8. **User policy wins:** connection type, background activity, data limits, storage caps and pause/disable controls constrain automatic acquisition.
9. **No resource pressure bypass:** acquisition must not starve inference, the operating system, user data, model checkpoints or an active training step.
10. **No false progress:** the UI reports actual queued, fetched, accepted, indexed and training-eligible counts; it does not equate downloaded bytes with learned knowledge.

## 3. Shared device capability evidence

Both profiles use a common, evidence-bearing device capability record:

| Resource | Evidence used | Acquisition implications |
|---|---|---|
| Memory | App memory class, current headroom, observed pressure and process limits | Parser buffer sizes, worker count, concurrent document count, index batch size |
| CPU | ABI/ISA and sustained measured parsing, decompression and hashing throughput | Worker concurrency, document scheduling and bounded processing throughput |
| GPU/NPU | Verified runtime/operator path, not chipset branding | Optional acceleration only for a validated indexing/embedding operation; never presumed necessary for web fetching |
| Storage | Available app-private space, user cap, model/checkpoint reserve, index reserve and recovery reserve | Corpus retention cap, shard selection, full-text versus metadata retention and cleanup threshold |
| Network | Connectivity state, connection type, metering/data-saver policy, latency and failure rate | Wi-Fi-only policy, request concurrency, download cap, retry/backoff and transfer scheduling |
| Power | Battery level, charging state and system power restrictions | Whether large downloads, parsing or index compaction may run |
| Thermal | OS thermal status and sustained performance trend | Parsing concurrency, decompression rate, compaction and deferral |
| Platform | Android version, lifecycle restrictions, background execution eligibility | Job size, execution windows, foreground disclosure where required and recovery strategy |

Unknown telemetry remains unknown. Do not substitute advertised total RAM, peak TOPS or a device name for measured available headroom and sustained capability.

## 4. Device-Adaptive Acquisition Profile

The profile is produced from shared capability evidence plus user preferences, model storage requirements, source restrictions and current local knowledge state.

It should define bounded outputs:

| Control | Meaning |
|---|---|
| Max concurrent fetches | Maximum simultaneous source requests |
| Max concurrent parsers | Maximum parsing/decompression workers |
| Max response bytes | Maximum compressed response size per request |
| Max expanded bytes | Maximum decompressed/extracted size per item |
| Max queued bytes | Maximum downloaded but not yet processed data |
| Network budget | Allowed data volume per policy period and connection type |
| Corpus storage budget | Maximum storage for retained corpus content |
| Index storage budget | Maximum storage for lexical/vector/metadata indexes |
| Processing budget | CPU/memory/time allowance for parsing, deduplication and indexing |
| Retention policy | Full text, excerpt, metadata-only, expiry and eviction behavior |
| Indexing batch size | Bounded records processed per index transaction |
| Tokenization batch size | Bounded text records converted using the active tokenizer |
| Training-candidate budget | Maximum versioned candidate material staged for a training cycle |
| Execution window | Conditions and time windows when automatic work is eligible |

These are logical controls, not approved numeric constants. Exact values must be calibrated by representative device tests and source terms.

### 4.1 Capability dimensions

Do not reduce the profile to a single low/mid/high label. Two devices with similar RAM may differ significantly in free storage, sustained CPU, network allowance, thermal behavior or Android background restrictions.

The profile may use internal capability bands for individual resources, but its final decision must retain the underlying evidence and confidence.

### 4.2 Model resource reservation

Before allocating acquisition storage or memory, reserve capacity for:
- Active model weights and inference/runtime workspaces.
- Model checkpoints and recovery generations.
- Optimizer, gradient and activation storage during training, if training is admitted.
- User memory and user-authored/imported data.
- Application databases and mandatory operating reserve.

The acquisition system may only consume its assigned remainder. It must not assume that free storage measured before model construction remains available later.

## 5. Same knowledge coverage policy across devices

All devices share the same initial sixteen-domain taxonomy and discovery-priority framework. Device capability must not cause the planner to permanently focus only on domains that are cheap to collect.

A small-storage instance should not learn only programming because a few technical PDFs were easiest to download. Its scheduler should select smaller, high-value foundational materials across domains, maintain domain coverage state and defer expensive specialist material.

The planner must distinguish:

- **Coverage objective:** what breadth and depth Argus is trying to acquire.
- **Device feasibility:** what the current device can safely process and retain.
- **Source eligibility:** what it is legally and technically permitted to collect.
- **Execution admission:** what can run now under live conditions.

A topic can remain a high-priority knowledge gap even when its full-text source is too large for the current device. The planner may seek a smaller permitted source, retain only allowed metadata/excerpts, defer the task or offer an explicit user-selected external storage/export path if separately designed. It must not silently replace the item with a low-quality source.

## 6. Acquisition behavior by device capability

This illustrates behavior, not fixed device tiers or thresholds.

| Capability situation | Intended acquisition behavior |
|---|---|
| Constrained memory | Very small fetch/parse batches, streaming decompression, bounded text windows and no whole-corpus RAM loading |
| Limited storage | Compact, high-value foundation packs; full text only where permitted and needed; metadata/provenance retained; corpus/index caps enforced |
| Limited compute | Defer expensive OCR, large-format parsing, near-duplicate comparison or embedding work; use validated lightweight alternatives where available |
| Strong sustained compute | Increase bounded parsing/index concurrency when memory and thermal budgets permit |
| Fast unmetered network | Allow larger queued transfers within user budgets and source rate limits |
| Metered or restricted network | Metadata-only discovery, Wi-Fi-only deferral or user-approved transfers |
| Low battery / power saver | Defer bulk downloads, parsing and index maintenance unless the user selects an allowed policy |
| Thermal pressure | Reduce workers or pause at safe job boundaries |
| Background execution constrained | Persist small resumable jobs and continue only within OS-permitted execution windows |
| Insufficient storage reserve | Stop new intake before affecting checkpoints, app integrity or user data |

A lower-capability device is not required to finish the same acquisition work at the same time as a higher-capability device. Progress is incremental and resumable.

## 7. Corpus packaging and storage tiers

Use a layered, content-addressed storage model:

### Tier A — Essential knowledge metadata

Small records required for source provenance, taxonomy, topic status, source identifiers, licenses, hashes and acquisition-job recovery. Preserve these independently of optional full-text caches.

### Tier B — Core local text records

Selected, rights-approved, high-value foundational texts or structured knowledge needed for offline retrieval. The device profile determines how much full text can be retained within its corpus budget.

### Tier C — Expanded domain collections

Additional textbooks, publications, language packs and specialist material. Acquire incrementally when storage, network and policy budgets permit.

### Tier D — Derived artifacts

Lexical indexes, embeddings, summaries, parsed document structures and tokenizer-specific caches. These are rebuildable only when source records and applicable rights permit; their storage must be budgeted separately.

### 7.1 Shards and content-addressed records

- Package large collections into independently verifiable shards.
- Each shard has a manifest, exact source/item identities, rights state, taxonomy version, size, checksum and dependency metadata.
- Download and validate one bounded shard at a time when required by the device profile.
- Make shard installation atomic; a partial download is not an installed corpus.
- Deduplicate bytes only within the same local installation while retaining distinct rights and provenance records.
- Do not use a single large archive requiring full extraction into RAM or temporary storage.

### 7.2 Retention and eviction

When the corpus budget is reached:
1. Stop scheduling new intake.
2. Finish or safely cancel the current bounded item.
3. Preserve model weights, checkpoints, user data and source-rights metadata.
4. Evict only records covered by an explicit cache/retention policy.
5. Cascade deletion to summaries, embeddings, retrieval indexes and pending training candidates.
6. Record the action and reclaimed storage.
7. Recompute the acquisition profile before resuming.

Do not silently delete training lineage or user-approved memory to make room for general corpus data.

## 8. Tokenizer and model-generation compatibility

The local source record should be normalized independently of the neural tokenizer where retention is permitted. Tokenized shards, sequence blocks and training caches are derived artifacts tied to:

- Model generation ID.
- Architecture and training profile.
- Tokenizer ID and immutable fingerprint.
- Normalization/preprocessing version.
- Sequence packing and masking policy.
- Dataset/corpus manifest version.

When the tokenizer changes, invalidate or rebuild incompatible token caches from retained canonical text. Never reinterpret token IDs under a different tokenizer fingerprint.

If the source license or retention policy prevents storing canonical text, Argus must not promise future retokenization or retraining from that item. Record the limitation and use only permitted operations.

## 9. Runtime acquisition controller

The initial profile is not static for the lifetime of the installation. A runtime controller re-evaluates admission when meaningful resource conditions change.

It may:
- Lower or raise fetch/parser concurrency within validated bounds.
- Defer large items or choose smaller eligible items serving the same coverage objective.
- Pause download and parsing on metered-network policy, low battery, thermal or memory pressure.
- Delay indexing or tokenization until resource headroom returns.
- Resume from durable item/shard boundaries.
- Stop intake at a configured storage reserve.
- Coordinate acquisition with model training so they do not compete for unreserved memory or CPU capacity.

It must not:
- Change source rights or bypass a policy gate.
- Reduce quality/provenance checks under resource pressure.
- Change the model tokenizer, sequence semantics, optimizer or architecture.
- Promote a training candidate or model generation solely because a resource budget was reached.
- Assume Android keeps the process alive continuously.

A workload change affecting neural-training semantics remains governed by the model/training profile and its versioning rules.

## 10. Offline behavior and owner controls

Network acquisition requires connectivity, but local inference and retrieval over committed records should remain usable offline.

The owner should be able to:
- Set automatic acquisition to disabled, manual, Wi-Fi-only or an explicitly chosen network policy.
- Set a storage cap and review actual corpus/index usage.
- Pause/resume acquisition.
- Select preferred knowledge domains without silently disabling general breadth.
- Inspect pending, downloaded, processed, accepted, held and rejected counts.
- Remove selected source records and their derived artifacts.
- See whether work is deferred for network, battery, thermal, memory, storage, rights or Android scheduling reasons.

Personal files and conversations remain separate from the general acquisition profile and require the personal-learning permission path.

## 11. Observability

The acquisition dashboard should expose:
- Current acquisition profile version and the resource evidence that constrained it.
- Configured network/storage budgets and remaining allowances.
- Fetch/parser concurrency and bounded queue sizes.
- Downloaded, validated, indexed and training-eligible bytes/documents/tokens as separate metrics.
- Domain/language coverage changes.
- Deferred items and exact reason codes.
- Eviction and cleanup history.
- Last successful acquisition and next eligible execution window.

Do not display "learning X tokens" when the system has only downloaded X tokens. Collection, validation, indexing and neural training are separate states.

## 12. Verification plan

Test multiple device classes and resource conditions, not only different RAM sizes:
- Low memory with adequate storage.
- Adequate memory with low storage.
- Strong CPU with metered/slow network.
- Weak CPU with fast network.
- Battery saver and low battery.
- Thermal pressure during parsing/indexing.
- Background process death during download, extraction and indexing.
- Storage exhaustion during shard installation.
- Tokenizer/model-generation change with cached tokens.
- Two jobs competing with model inference or training.
- User disables networking while a fetch is active.
- User deletes a source record already represented in indexes or pending training data.

Acceptance requires bounded peak memory, no corruption of committed records, correct permission enforcement, safe recovery, accurate UI status and preserved model/user-data reserves.

No numeric threshold or device tier is finalized until reproducible benchmarks are available.

## 13. Open calibration decisions

- Device-class memory, storage, CPU, network and thermal thresholds.
- Minimum reserved app storage for model/checkpoint recovery.
- Core text package selection and per-device shard budgets.
- Download byte budgets and user defaults for metered/Wi-Fi-only policy.
- Parser/embedding memory profiles.
- Corpus retention and expiry rules.
- Maximum queue sizes and concurrent jobs.
- How acquisition competes with inference/training on each architecture/backend.
- Whether external removable storage is supported on a future platform.
- User-facing defaults for automatic acquisition and background operation.

Derive these values from representative Android devices and source constraints, not arbitrary RAM-based assumptions.
