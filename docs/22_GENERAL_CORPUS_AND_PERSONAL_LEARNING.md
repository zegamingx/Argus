# General Corpus and Personal Learning

**Status:** Accepted general/personal learning split; user-controlled local dataset import selected as the primary general-corpus intake; importer and training implementation remain open  
**Last reviewed:** 2026-11-05

## 1. Purpose

Argus evolves through two distinct learning paths:

- **General corpus learning:** the user selects and imports local dataset files for general model development.
- **Personal learning:** the instance adapts from the owner's personal experience through the separate consent, retention, review and revocation lifecycle in document 48.

The first path does not depend on a bundled public dataset, an Argus-operated web crawler, or an externally curated article collection. The user supplies the corpus. Small C4 is no longer a default/bootstrap requirement; it remains optional historical research only.

Both paths remain local to the owning Argus instance. General-corpus records and personal-experience records must never be mixed merely because their text formats are similar.

## 2. Primary intake: user-controlled local dataset import

The initial general-corpus intake is a user-initiated local import.

### 2.1 User flow

1. The user opens the dataset-import action and selects one or more local files or a supported archive using Android's system document picker.
2. The user declares that the selected material is a **general dataset**, not personal experience, then selects its local purposes independently: neural-model training, tokenizer frequency fitting, aligned tokenizer development, or a chosen combination. The user confirms that they have authority for each selected purpose.
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

### 2.3 User declaration and rights metadata

Argus does not need to perform source-by-source web research or an external license investigation before accepting a user-directed local import. At import time, the user confirms authority separately for each selected local operation: general model training, tokenizer frequency fitting, and/or aligned tokenizer development. The system records those operation-specific declarations with the import identity and timestamp.

The declaration is not a legal determination by Argus. Missing license metadata is recorded as unknown; it is not silently converted to public-domain status. Imported material is kept local and is not automatically bundled into Argus, published, redistributed, synced or sent to another service. Any future redistribution or Argus-operated public-source acquisition is a separate operation with its own requirements.

This keeps public-corpus curation and license research from blocking the user's ability to import their own chosen dataset, without making unsupported claims about the legal status of third-party material.

## 3. Dataset preparation and manifest

A local import becomes a corpus revision only after parsing, validation, duplicate handling and the user's explicit confirmation.

The processing path is:

~~~text
User-selected local files / bounded archive
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

Tokenizer-dependent token counts and tokenized shards are finalized against the exact tokenizer fingerprint selected for the model generation. An imported corpus may also be selected as tokenizer-frequency input only when the user explicitly declared that operation. An aligned development split is required only for tokenizer algorithms/configurations that use it; if the user does not import an appropriate aligned set, Argus must use a valid no-development-data variant or exclude that candidate. Until G1-D is closed, the importer preserves validated raw text and record-based split membership without inventing tokenizer identity or final token counts.

### 3.1 Minimum corpus manifest

Record at least:

- Corpus ID, immutable revision and owning local instance ID.
- Import timestamp and importer/parser version.
- Each input file's name, size, media type, SHA-256 digest and import outcome.
- User-supplied source label/URL or publisher when provided.
- User declaration: selected operation scopes (neural-model training, tokenizer frequency fitting, aligned tokenizer development), authority confirmation for each scope, declaration version and timestamp.
- Record IDs/digests, duplicate and rejected-record summaries.
- Language/quality/safety diagnostic versions and outcomes.
- Normalization and deduplication version/configuration.
- Split policy, seed, source record membership and split digests.
- Tokenizer ID/fingerprint and token counts once the tokenizer is selected.
- Known limitations, warnings and excluded records.

No full document text, credentials or private article content should be copied into generic logs or diagnostic events. The local corpus store owns the content; manifests and logs reference it by identity and digest.

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
- User import is not permission for Argus to publish, redistribute, sync or upload the files. Model-training and tokenizer-fitting purposes are separately declared.
- Missing license metadata is not described as public-domain status or as proof of permission.
- A dataset import does not itself start a training worker or change model weights.
- User conversations, file attachments and screen observations are not automatically general-training data.
- Public-source crawling remains an optional, separate capability.
- Personal training does not upload data or weights to a central service.
