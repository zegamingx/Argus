# General Corpus and Personal Learning

**Status:** Accepted general/personal learning split; local corpus importer and bundled ArgusID/DigiTransID intake implemented; schema-v3 canonical dataset/split identity and initial PENDING admission logging are implemented in source; no generated v3 corpus revision has been runtime-verified; tokenizer fitting and training remain unavailable  
**Last reviewed:** 2026-11-07

## 1. Purpose

Argus evolves through two distinct learning paths:

- **General corpus learning:** the user selects and imports local dataset files for general model development.
- **Personal learning:** the instance adapts from the owner's personal experience through the separate consent, retention, review and revocation lifecycle in document 48.

The first path does not depend on a bundled public dataset, an Argus-operated web crawler, or an externally curated article collection. The user supplies the corpus. Small C4 is no longer a default/bootstrap requirement; it remains optional historical research only.

Both paths remain local to the owning Argus instance. General-corpus records and personal-experience records must never be mixed merely because their text formats are similar.

## 2. Primary intake: user-controlled local dataset import

The initial general-corpus intake is a user-initiated local import.

### 2.1 User flow

1. The user opens the dataset-import action and selects one of the two bundled corpora currently exposed by importer convenience actions (`ArgusID.txt` or `DigiTransID.zip`), or selects another accessible file using Android's system document picker. `Argus_story_Eng.txt` is also present as an owner-created repository asset, but the current source has no dedicated bundled-English import action/helper; do not present it as an already selectable bundled import option. Its current tokenizer-research role is a separate English diagnostic only, pending content-quality review and an explicit import path.
2. The user selects the intended local purposes: neural-model training, tokenizer frequency fitting, aligned tokenizer development, or a chosen combination. General-corpus files remain separate from personal experience; no source-by-source license investigation or per-record rights declaration is part of this local import flow.
3. Argus parses and validates the selected files locally. It does not fetch linked articles, crawl source websites, upload content or silently add the data to a training queue.
4. Argus presents an import summary: files/records discovered, accepted, skipped/rejected, detected structure/languages, duplicate counts, approximate text volume and validation warnings.
5. The user confirms corpus preparation. Argus creates a versioned local corpus manifest and deterministic split plan.
6. Training remains a separate explicit operation. The import does not start a worker or update model weights by itself.

The exact supported MIME types, file-size limits, archive limits and parser implementations are implementation bindings. The initial parser set should prioritize common plain-text and structured-text formats, with bounded archive support. Unsupported or malformed files must produce a visible reason rather than being silently ignored.

### 2.2 Local-only boundary

- Selected file contents stay on the device. The import path performs no network upload or remote processing.
- The importer does not download URLs contained in articles, HTML or dataset rows.
- A source label, publisher/name, original URL or other provenance may be supplied by the user and stored as metadata; missing optional source metadata is recorded as unknown rather than fabricated.
- Content and file digests are calculated for integrity, duplicate detection and immutable manifest identity.
- Archive extraction must enforce path safety, entry-count limits, expanded-size limits and cancellation. A malicious or oversized archive is rejected or partially held with an explicit report.
- A user can inspect the import summary and discard the staged import before committing a corpus revision.

### 2.3 Import-purpose metadata

The user selects which local workflow(s) may consume the imported corpus: general model training, tokenizer frequency fitting, aligned tokenizer development, or a combination. Argus records those selections with the corpus manifest so later workflows can distinguish the intended use. This is a workflow-selection record, not a source-by-source legal review or a per-word rights check.

The importer keeps selected files and derived records in app-private storage. It does not publish, redistribute, sync or upload corpus content. The owner can inspect the source-file names and SHA-256 digests stored in the manifest. Any future network acquisition or redistribution feature is a separate product operation and is not performed by this importer.

## 3. Dataset preparation and manifest

A local import becomes a corpus revision only after parsing, validation, duplicate handling and the user's explicit confirmation.

The processing path is:

~~~text
User-selected local files / streamed archive
                  |
                  v
         File and archive validation
                  |
                  v
        Decode + record-structure checks
                  |
                  v
        Content integrity + deduplication
                  |
                  v
      Language / quality / safety diagnostics
                  |
                  v
        Normalization + deterministic split
                  |
                  v
       Corpus manifest + SHA-256 identities
                  |
                  v
         Local immutable corpus revision
                  |
                  v
       Explicit later Learning-session admission
~~~

Validation should identify malformed encodings, empty/duplicate records, broken structured rows, unsupported records, obvious boilerplate/spam and content that cannot safely be processed. These diagnostics are local and explainable; a filter result must not be described as proof that the corpus is factually correct or free of sensitive content.

Train, validation and test partitions are deterministic and disjoint at the record/document level. A record must never appear in more than one split after deduplication. Validation and test partitions are never passed to the optimizer. Split policy and seed are included in the manifest so a corpus revision can be reproduced.

Tokenizer-dependent token counts and tokenized shards are finalized against the exact tokenizer fingerprint selected for the model generation. An imported corpus may be used for tokenizer-frequency input only when tokenizer frequency fitting was selected as an import purpose. An aligned development split is required only for tokenizer algorithms/configurations that use it; if the user does not import an appropriate aligned set, Argus must use a valid no-development-data variant or exclude that candidate. Until G1-D is closed, the importer preserves validated raw text and record-based split membership without inventing tokenizer identity or final token counts.

The current Android importer reads ordinary TXT/Markdown/JSONL/NDJSON sources incrementally. ZIP sources are streamed to an app-private temporary file for full-archive hashing, then parsed entry-by-entry through `ZipInputStream`; archive paths are never extracted. It imposes no fixed byte-size limit per source or for aggregate expanded content. Storage availability controls whether staging can finish. Memory use is bounded by stream buffers and individual record handling: long TXT paragraphs are emitted in 262,144-character chunks, while a single JSONL row above 1,048,576 characters is rejected with a warning. Record digests are indexed in a temporary SQLite database, so deduplication does not retain the complete set of hashes in heap. A selection is limited to 64 source files and 4,096 total ZIP entries. This is an import/staging implementation only; it does not tokenize data, fit tokenizer merges, train model weights or create a training session.

### 3.1 Minimum corpus manifest

Record at least:

- Corpus ID, immutable revision and owning local instance ID.
- Import timestamp and importer/parser version.
- Each input file's name, size, media type, SHA-256 digest and import outcome.
- User-supplied source label/URL or publisher when provided.
- Selected operation purposes (neural-model training, tokenizer frequency fitting, aligned tokenizer development) recorded with the import identity and timestamp.
- Record IDs/digests, duplicate and rejected-record summaries.
- Canonical normalized-dataset digest, bound to the exact decoding, normalization, preprocessing and deduplication versions.
- Per-split membership digests for train, validation and test, bound to the deterministic split policy/seed and source/document grouping.
- Language/quality/safety diagnostic versions and outcomes.
- Normalization and deduplication version/configuration.
- Split policy, seed, source record membership and split digests.
- Tokenizer ID/fingerprint and token counts once the tokenizer is selected.
- Known limitations, warnings and excluded records.

**Implementation-status boundary (reviewed 2026-11-06):** the current importer records source/file and record-level integrity identities, split membership and selected workflow purposes. The current audited implementation does not yet create the canonical normalized-dataset digest, per-split membership digests or a separate append-only `DatasetAdmissionDecision` required by document 36. These are design requirements, not implemented facts. No importer-generated immutable tokenizer-frequency manifest for the three owner-created starter assets is present in the reviewed repository snapshot; therefore no source SHA-256, canonical dataset digest, split digest or `TOKENIZER_FREQUENCY_FITTING` admission is claimed here. Git blob IDs identify repository objects and must not be substituted for importer-generated SHA-256 values.

No full document text, credentials or private article content should be copied into generic logs or diagnostic events. The local corpus store owns the content; manifests and logs reference it by identity and digest.


### 3.2 Current schema-v3 importer implementation

The Android importer now writes canonical corpus records using the versioned `argus-jsonl-canonical-v1` representation. Each record has a fixed field order (`recordId`, `sourceDocumentId`, `sourceName`, `text`, `split`), explicit JSON string escaping and an LF record terminator. The canonical dataset SHA-256 uses a domain-separated digest over the exact UTF-8 JSONL bytes.

The schema-v3 manifest binds:

- The canonical record-format identifier, SHA-256 algorithm, canonical dataset digest and record count.
- Parser, normalization and deduplication version identifiers.
- The explicit `NOT_TOKENIZED` state and null tokenizer-profile identity.
- The split policy (algorithm, seed, source-document grouping and target ratios), per-split record counts and membership SHA-256 digests, and a split-manifest digest bound to the canonical dataset digest.
- The append-only admission-log location and format.

The exact manifest bytes are accompanied by a `manifest.sha256` sidecar. The initial `admission-decisions.jsonl` entry is always `PENDING` and is bound to both the manifest digest and canonical dataset digest. Integrity checks for source-digest format, canonical encoding, record IDs, normalized text, document-to-split assignment, per-document counts, dataset bytes, split memberships and split-manifest identity run before the staged directory is committed. A source document whose records were all removed as duplicates remains visible in the source inventory with the explicit `EXCLUDED_DUPLICATE_ONLY` disposition; it is not counted as a train/validation/test member.

The Learning corpus list displays the latest recorded admission decision. A schema-v2 or older corpus is labelled `LEGACY_UNVERIFIED`; it is not silently treated as an approved dataset.

This implementation does **not** approve a corpus. Provenance and embedded-material review, tokenizer-profile compatibility, data-quality qualification and semantic split-leakage review remain pending. The importer does not produce tokenized shards, a final tokenizer-frequency fitting artifact, a training admission grant, a training session or model updates. The commit-time integrity checks are source implementation only; runtime-generated manifests and device behavior have not yet been verified. A SHA-256 sidecar detects accidental inconsistency but is not an authenticity signature against an actor able to rewrite both the content and its digest.


## 4. General learning lifecycle

General-corpus preparation is separate from a training session.

A corpus may be imported and inspected before the model is ready for training. A Learning session can consume it only after all of the following exist and pass validation:

- A committed and recoverable Genesis generation.
- The exact architecture/model configuration and tokenizer identity.
- An immutable corpus manifest with deterministic split membership.
- A supported numerical training backend and declared optimizer/update semantics.
- Resource admission for the user's device and current thermal/power state.
- Candidate-only model state and complete checkpoint/recovery semantics.
- Evaluation gates and a repository-controlled promotion/rollback path.

Learning runs as bounded, resumable background work after application readiness. It must not block app startup. Device capability changes may adjust safe work size and scheduling, but may not silently change the architecture, tokenizer, objective, precision or optimizer semantics of an active session.

**Current implementation status:** the legacy raw-string worker and `trainStep(String)` path were retired. No replacement Learning coordinator or training backend is currently available. The import direction is accepted, but importing a dataset cannot truthfully be presented as starting training until the implementation gates above are met.

## 5. Optional future public-source acquisition

Argus may later add an instance-owned autonomous acquisition capability that discovers public sources and collects material for its own local knowledge store. It is optional and does not block local dataset import or the first user-provided corpus workflow.

When enabled, network discovery/fetch, source-specific terms, retention, retrieval, training eligibility and redistribution remain separate operations. The source registry and acquisition contracts in documents 24–32 apply to that future network capability. Argus must not silently crawl arbitrary websites, bypass access controls or claim that a page is training-authorized merely because it is readable.

User-imported files are not fetched again from their source URLs. Their source metadata is descriptive provenance supplied by the user; local import does not authorize web crawling or redistribution.

## 6. Personal learning

Personal learning adapts the individual instance using user-approved personal experience, such as explicit corrections, preferences, repeated tasks or selected personal documents.

A selected personal file, chat, screen observation or camera-derived experience is not automatically a general-corpus record. When the intended purpose is personal retention or personal parameter learning, the intake path in document 48 applies: purpose-specific authorization, classification, retention, provenance, review and a separate training authorization bound to a specific candidate/session.

Use inspectable memory first when it meets the use case. Retrieval does not itself authorize parameter training. Personal training must not silently alter the general corpus, and general-corpus imports must not be relabelled as personal experience to bypass source or dataset policy.

## 7. Evaluation separation

General evaluation measures capability on held-out general data and relevant language-quality checks. Personal evaluation measures only user-approved tasks/facts within the same local instance.

Track both after any personal update to detect catastrophic forgetting. The candidate generation is not promoted merely because training loss falls or a personal metric improves. Evaluation binds to the exact corpus, split, tokenizer, model-generation and training-session identities.

## 8. Explicit non-goals

- Small C4 is not a required, bundled or default bootstrap dataset.
- Argus does not need to curate or download public articles before the user can import a local dataset.
- User import remains local; the importer does not publish, redistribute, sync or upload files. The selected workflow purposes are recorded in the corpus manifest.
- A dataset import does not itself start a training worker or change model weights.
- User conversations, file attachments and screen observations are not automatically general-training data.
- Public-source crawling remains an optional, separate capability.
- Personal training does not upload data or weights to a central service.
