# General Corpus and Personal Learning

**Status:** Accepted learning split; corpus acceptance and tokenizer remain open  
**Last reviewed:** 2026-11-04

## 1. Purpose

Argus begins general foundation learning with a bundled offline bootstrap corpus and is intended to expand its general knowledge through autonomous, policy-controlled acquisition from permitted public sources. It also adapts to the individual user's experience through a separate personal-learning path. The bootstrap corpus is neither the complete knowledge of Argus nor a pretrained model.

General foundation learning and personal learning remain separate in provenance, permission, evaluation and retention.

## 2. Small C4 bootstrap candidate

Current candidate: [brando/small-c4-dataset](https://huggingface.co/datasets/brando/small-c4-dataset), derived from the English subset of [allenai/c4](https://huggingface.co/datasets/allenai/c4).

The dataset card reports:
- 30,000 text passages in total.
- 10,000 training passages.
- 10,000 validation passages.
- 10,000 test passages.
- Approximately 40 MB download size and 64.8 MB dataset size in its metadata.
- English-only text.
- Parquet data files.

The card describes train as the first 10,000 examples from C4 train, validation as the first 10,000 examples from C4 validation, and test as the next 10,000 from that same C4 validation split. Test examples are distinct by the described ordering, but validation and test originate from the same upstream partition. Do not treat them as independent source-level test sets without documenting and addressing that limitation.

These values are dataset-card metadata, not a substitute for inspecting the exact dataset revision and bytes used by Argus.

## 3. Why Small C4 is suitable only as bootstrap

Small C4 is useful for exercising the offline data pipeline, tokenizer and sequence preparation, early training experiments, and checkpoint/recovery mechanics.

It is not sufficient evidence for broad general-language competence from random initialization. It is a small English-only sample of web text and should not be described as exhaustive, balanced or comprehensive language foundation data.

Argus does not need to force multilingual breadth into the first bootstrap release. The personal instance is intended to adapt to the owner's actual usage over time. Language ability still depends on meaningful, sufficient and permitted language exposure; a handful of chats cannot be assumed to teach a language from scratch.

## 4. License and provenance gate

The Small C4 dataset card states that its license inherits from original C4. The original C4 dataset card identifies ODC-BY for the C4 dataset/database and separately states that Common Crawl terms apply to the underlying crawled content. Common Crawl's current Terms of Use (last updated 2024-03-07) say that crawled material may also be subject to the original website owner's separate terms; they also prohibit uses such as privacy invasion and harvesting personally identifiable information for separate use. Therefore, the ODC-BY label on C4 must not be interpreted as a blanket license from every website publisher represented in the corpus.

The current Small C4 dataset card confirms the 30,000-row sampling method: first 10,000 rows from C4 train; first 10,000 rows from C4 validation; next 10,000 rows from that same C4 validation split. This makes the 10,000-row test partition distinct by row order from validation, but not independent at the upstream split level. Retain this limitation in all evaluation claims.

The dataset viewer also demonstrates heterogeneous web content rather than a curated textbook corpus. The reduced card exposes only passage text; the original C4 includes source URL/timestamp fields, but those are not present in the small-c4 text-only derivative described here. This loss of source identity weakens item-level rights review, privacy remediation and deletion lineage. It is a material drawback for Argus even though the smaller artifact is convenient.

Before bundling or redistributing:
1. Pin the exact Small C4 repository commit, source C4 revision, Parquet files and file list.
2. Preserve the C4 ODC-BY database attribution and the applicable Common Crawl Terms of Use; separately assess the original website/content terms and the intended Argus training/redistribution use.
3. Determine attribution and redistribution obligations for both the reduced derivative and the source.
4. Establish whether the lack of original URL/timestamp makes item-level rights and privacy assurance insufficient for the intended use; do not fabricate missing provenance.
5. Run versioned language, quality, spam, personal-data/sensitive-content and safety screening on the exact pinned bytes, with documented false-negative limitations and an item-level hold/quarantine path.
6. Record preprocessing and filtering changes; preserve dataset and shard checksums.
7. Produce a corpus manifest, record the source-level validation/test overlap, and include required notices in the app/package.
8. Require an explicit source approval before this dataset becomes TRAINING_ELIGIBLE; until then it remains a bootstrap candidate only.

The word “bundled” means the approved corpus is available locally for training without a network requirement at training time. Whether it is packaged inside the APK or delivered as a separately installable offline asset is a packaging decision; it must not become a required online model-weight download.



Evidence reviewed 2026-11-04: [Small C4 dataset card](https://huggingface.co/datasets/brando/small-c4-dataset), [original C4 dataset card and licensing note](https://huggingface.co/datasets/allenai/c4), and [Common Crawl Terms of Use](https://commoncrawl.org/terms-of-use/).

## 5. Corpus preparation pipeline

~~~text
Pinned Source Revision
        |
        v
License / Provenance Review
        |
        v
Decode + Text Validation
        |
        v
Language and Quality Diagnostics
        |
        v
Privacy / Sensitive-Content Filtering
        |
        v
Normalization + Deduplication
        |
        v
Tokenizer-based Token Counting
        |
        v
Train / Validation / Test Construction
        |
        v
Sharding + Manifest + SHA-256
        |
        v
Bundled Offline Corpus Package
~~~

Preprocessing must be deterministic and versioned. Keep a source document identifier or stable digest for provenance. Do not silently mix training, validation and test examples. Validation/test data must never be fed to the optimizer.

Because Small C4 exposes only a text field in the reduced dataset, original URL/timestamp provenance may not be available in that derived package. The corpus manifest must disclose this limitation rather than fabricate missing metadata.

## 6. Corpus manifest

At minimum record:
- Corpus ID and immutable revision.
- Original source dataset and revision.
- License, notices, attribution and applicable terms.
- Split names, source split and sampling method.
- Shard names, exact sizes and SHA-256 values.
- Preprocessing pipeline version and configuration.
- Language-detection and quality-filter versions.
- Deduplication method and statistics.
- Tokenizer ID/fingerprint used for token counts.
- Per-split document count and token count.
- Known exclusions, bias and data-quality limitations.

Token count must be measured using the selected Argus tokenizer. File size and document count are not reliable substitutes for training volume.

## 7. General learning lifecycle

The local package is validated before use. The scheduler reads it in bounded shards and never requires the full corpus to be loaded into RAM.

General training becomes eligible after the initial model generation is committed and the application is ready. It runs in resumable background work and follows the device-adaptive training controller. It can be deferred by system conditions or user policy without blocking the app.

## 8. Autonomous general knowledge acquisition

Small C4 is the initial bootstrap candidate, not the permanent or exclusive source of general knowledge. After Genesis readiness, each instance is intended to run a separate acquisition engine that discovers and collects permitted public information for its own local knowledge store.

The first discovery cycle uses deterministic topic policies and a reviewed source registry because a newly initialized model cannot yet be trusted to direct its own research. Model-assisted discovery may be introduced only after defined model and retrieval quality gates pass. Model suggestions remain subject to source eligibility, network policy, user controls and resource budgets.

Acquisition output is processed for provenance, rights, quality, language, sensitivity and duplication. Accepted records may become searchable through local retrieval without changing model weights. Only records with verified provenance and permitted training use may be included in an immutable general-training corpus candidate.

This general acquisition path must not ingest private user interactions or personal files. It does not collect information for external AI companies, share corpora across installations or upload model state. Personal context used to direct learning belongs to the separate personal-learning permission lifecycle.

See [24 — Autonomous Knowledge Acquisition](24_AUTONOMOUS_KNOWLEDGE_ACQUISITION.md) for source eligibility, processing, storage, safety, scheduling and acceptance requirements.

## 9. Personal learning

Personal learning is intended to adapt to the individual:
- The user's language and communication patterns.
- Explicit corrections and preferences.
- User-selected documents or knowledge sources.
- Repeated tasks and approved procedural examples.

The source must have a clear permission scope. Platform permission, an open chat or a visible screen does not automatically authorize retention or parameter training. Personal candidates require provenance, sensitivity classification, appropriate redaction, review/approval, revocation and deletion lineage.

Use inspectable memory first when it meets the use case. Parameter learning is a separate, validated batch operation. General-corpus training must not ingest personal records, and personal training must not silently alter the general corpus.

## 10. Evaluation separation

General evaluation measures performance on held-out general data and relevant language-quality checks. Personal evaluation measures only user-approved tasks/facts within the same instance.

Track both after personal updates to detect catastrophic forgetting. A candidate that improves personal metrics but materially regresses general capability must not be promoted without an explicit reviewed policy.

## 11. Explicit non-goals

- Small C4 is not a complete world-knowledge dataset.
- Small C4 is not a pretrained model.
- The first release does not promise balanced multilingual competence.
- User conversations are not automatically training data.
- Personal training does not upload data or weights to a central service.
