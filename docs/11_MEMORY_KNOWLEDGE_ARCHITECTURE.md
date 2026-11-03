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
