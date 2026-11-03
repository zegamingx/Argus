# Memory and Knowledge Architecture

**Status:** Proposed data architecture  
**Last reviewed:** 2026-11-03

## 1. Memory categories

| Memory | Purpose | Example | Main concern |
|---|---|---|---|
| Working/context | Current task state | Conversation and tool results | Bounded tokens and short retention |
| Episodic | Time-stamped events | User-approved conversation summary | Provenance and deletion |
| Semantic | Generalized facts/relations | Entity-property graph | Confidence, conflict and source |
| Procedural | Validated procedures | Approved reusable task steps | Permission boundary and versioning |
| Vector | Approximate retrieval | Text/image embedding index | Model-version and rebuild |
| Model parameters | Learned statistical knowledge | Base weights and adapters | Not auditable memory |

Model weights must not silently replace inspectable and removable memory records.

## 2. Record metadata

Every persistent record should include stable ID/type, content or structured representation, provenance/capture method, created/updated/accessed times, confidence and verification state, sensitivity/consent, retention/expiry, links to derived embeddings/summaries, and the model/tokenizer version used to derive it.

## 3. Retrieval flow

`query → policy filter → lexical/vector/graph retrieval → ranking → provenance check → context budget → untrusted-context wrapper → model`

Deletion filters and model-version invalidation must apply to derived indexes. Similarity is not proof of truth.

## 4. Conflict resolution

Resolve conflicts using source quality, recency, explicit user correction and domain-specific verification. Preserve conflicting claims when needed instead of overwriting silently. Confidence is evidence metadata, not a calibrated probability unless calibration has been measured.

## 5. Persistence and privacy

Use encrypted, transaction-safe storage for sensitive memory. Keep embeddings linked to their source so deletion can cascade. Provide inspection, correction, export, retention controls and deletion. Define backup inclusion and encryption-key restore behavior.

## 6. Current prototype boundary

The current `WorldModel` uses an in-memory concurrent map and state flows. It is not yet a persistent episodic/semantic/procedural/vector-memory subsystem. See [17 — Current Repository Baseline](17_CURRENT_REPOSITORY_BASELINE.md).

## 7. Relationship to Local Knowledge Genesis and personal learning

Memory and neural parameter learning are separate systems with different latency, consent and verification requirements.

- A conversation may update short-lived working context without being retained.
- A user-approved fact may be stored as an inspectable episodic or semantic memory without changing model weights.
- A personal-learning candidate may be considered for parameter training only under a distinct permission and candidate lifecycle.
- A training candidate must preserve provenance to its source records and the model generation it influenced.
- Deleting a source must remove or invalidate its derived memory/index records and trigger the documented model rollback/rebuild policy where trained parameters were affected.

General-corpus training and personal training also retain separate dataset lineage. The general bootstrap corpus is not a license to train on personal conversations or captured screen content.

## 8. Instance-local memory

All memory, embeddings, summaries, skill records and model parameters belong to one Argus instance. No cross-user memory pool, central semantic store or automatic memory synchronization is planned. Any future user-directed export or device migration must be separately specified, encrypted, explicit and independently revocable; it must not introduce background sharing.
