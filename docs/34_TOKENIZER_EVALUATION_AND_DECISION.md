# Tokenizer Evaluation and Decision Framework

**Status:** Evidence synthesis with provisional device-conditioned tokenizer portfolio; user-imported local corpus is the primary frequency-data path; final tokenizer profile policy and ADR remain OPEN  
**Last reviewed:** 2026-11-05  
**Research stage:** Stage 4.1–4.2 — Tokenizer evaluation and comparative research  
**Authority:** Candidate definitions, evaluation controls and decision gates. No tokenizer, vocabulary size, normalization profile, packaging policy or runtime is selected by this document.

## 1. Purpose and decision boundary

Argus must choose a tokenizer before its model generation is initialized because the tokenizer's vocabulary and token-ID mapping define the input embedding and output projection dimensions. The tokenizer also influences sequence length, context use, training compute, inference cost, multilingual coverage and model-state compatibility.

This document establishes a controlled method to compare tokenizer candidates. It does not select a universal winner or require one vocabulary shared by every installation.

Argus is device-adaptive. The product language/domain policy and the corpus selected by the user define the tokenizer data scope; each installation's measured capability profile determines which tokenizer configuration and preparation mode is feasible. A local import records the selected workflow purposes: tokenizer frequency fitting and, separately, aligned development when an algorithm requires it. The resulting tokenizer may be a shared precomputed artifact selected from a profile portfolio or a locally trained, versioned artifact. Either way, its exact fingerprint is fixed before model tensors are initialized and remains immutable for that model generation.

The current evidence supports four cautions:

1. Tokenizer choice can change downstream model quality and training cost, not just compression.
2. Fertility and parity are useful diagnostics but are not reliable standalone predictors of downstream performance.
3. Byte-level coverage can eliminate unknown-character failure modes, but byte sequences may be longer and more expensive to process.
4. Vocabulary size creates a trade-off between sequence length and embedding/output-head cost. The shortest token sequence is not automatically the best mobile training configuration.

The detailed from-scratch resource and device feasibility protocol remains in [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md). Tokenizer experiments do not close E1–E7 or establish that broad general-purpose pretraining is feasible on Android.

This is a documentation-only research phase. It authorizes no tokenizer implementation, training run, Android device benchmark, build, test, CI workflow or artifact publication.

## 2. Relationship to Local Knowledge Genesis

A tokenizer is not a pretrained neural model and does not itself confer language capability. It is a deterministic representation layer that maps text to token IDs and back according to a versioned algorithm, vocabulary, normalization, pre-tokenization, special-token and decoder configuration.

The accepted Genesis rule remains:
- Model parameters are initialized locally for each Argus instance.
- No externally pretrained neural weights are used as the foundation.
- Tokenizer assets and model weights are separate artifacts with separate provenance.
- The use of a precomputed vocabulary does not, by itself, introduce pretrained neural weights. A locally derived vocabulary is bound to the user-imported corpus revision and the corpus's selected tokenizer-frequency purpose. Redistribution or bundling of that artifact is a separate operation and is not implied by local import.
- If Argus trains a tokenizer locally, that tokenizer-training operation is distinct from neural pretraining and must not be presented as model learning.

The tokenizer must be finalized before the initial model's embedding/output dimensions are allocated. Its ID and cryptographic fingerprint become part of the model generation identity and training/checkpoint compatibility contract. A vocabulary or token-ID mapping change is a generation migration, not an ordinary settings change. See [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md) and [19 — P0 Remediation Design](19_P0_REMEDIATION_DESIGN.md).

## 3. Candidate families

The initial comparison should cover the following candidates. Their inclusion means research eligibility, not endorsement.

### T0 — Raw UTF-8 byte sequence (control baseline)

- Input is the original UTF-8 byte sequence.
- The base symbol set is the 256 possible byte values, plus separately reserved control/special IDs when required by the model interface.
- There is no learned subword vocabulary or BPE merge table.
- It is a useful universal-coverage and sequence-length baseline, not an assumed production choice.

[ByT5](https://doi.org/10.1162/tacl_a_00461) demonstrates that byte-level multilingual modeling can be competitive, but also reports higher computational cost: the paper's raw-byte Transformer incurred about 33% more pretraining time and inference slowdown up to 10× in the worst evaluated case. These numbers belong to its T5 workloads and are not Argus estimates. The study reinforces that universal byte coverage and reduced tokenizer complexity can trade off against longer sequences and runtime cost.

### T1 — Byte-level BPE

- Start from the 256-byte alphabet and learn BPE merges.
- The exact byte-to-visible-character mapping, pre-tokenizer/regex, merge ordering, vocabulary, decoder and special-token policy form one immutable pipeline.
- GPT-2-style byte-level processing can represent arbitrary UTF-8 content without an unknown token, but pre-tokenization rules still define where merges may occur.

Byte-level BPE is not equivalent to raw byte input: learned merges shorten common byte sequences. It is also not equivalent to every BPE implementation over Unicode characters. The pre-tokenizer and normalizer must be treated as part of the candidate, not as incidental defaults.

### T2 — SentencePiece BPE over raw Unicode

- Train subword pieces directly from raw sentences rather than requiring a word-tokenized input stream.
- Record the exact normalizer, whitespace options, character-coverage policy and trainer version.
- Evaluate both the selected normalization profile and a less aggressive alternative.
- Treat optional `byte_fallback` as a separately measured variant.

SentencePiece's `byte_fallback` decomposes out-of-vocabulary characters into UTF-8 byte tokens. This is an unknown-character fallback mechanism; it is not the same pipeline as byte-level BPE, which begins from byte units for its base alphabet.

### T3 — SentencePiece Unigram over raw Unicode

- Train a fixed subword vocabulary using the Unigram language-model segmentation method.
- Evaluate with deterministic canonical/Viterbi segmentation for benchmark repeatability.
- Record the exact normalization, whitespace, character coverage and byte-fallback configuration.
- Consider sampled alternative segmentation only as a later, separate training-regularization experiment; do not mix stochastic segmentation into intrinsic tokenizer comparisons.

The SentencePiece paper describes raw-sentence training and language-independent subword segmentation. Subword regularization research shows that sampling multiple segmentations can be useful during model training, but that is a separate training method and must not confound the tokenizer's deterministic evaluation.

### T4 — WordPiece (secondary reference candidate)

WordPiece may be included as a secondary comparator if an equivalent, reproducible training configuration and Android deployment path are available. It is not required for the first comparison if it introduces a materially different preprocessing stack or makes the experiment matrix unmanageable.

The 2026 low-/medium-resource study compares WordPiece, BPE and Unigram under shared model/data settings. It is useful evidence that algorithm, vocabulary size and task interact, not a universal ranking for Argus.

### Diagnostic controls

- **Unicode-scalar/character segmentation:** optional diagnostic to measure sequence length and vocabulary pressure without byte fallback. It is not a default deployment candidate.
- **Existing public tokenizers:** may be measured as external reference baselines only. They cannot be adopted as the Argus tokenizer merely because they benchmark well; exact license, data provenance, configuration, version and model-compatibility requirements must be reviewed.

## 4. Comparison fixture and data controls

### 4.1 Fixture objectives

The tokenizer fixture must reflect intended Argus inputs rather than only clean English prose. It must be versioned and immutable for each comparison round.

At minimum, separate these fixture strata:

| Stratum | Required content |
|---|---|
| Core language | Indonesian and English text, with exact source/sample records |
| Additional language/script stress | Selected Latin and non-Latin scripts that expose Unicode, combining-mark and script-boundary behavior; these tests do not automatically create product-language support commitments |
| General knowledge domains | Representative samples from the approved knowledge taxonomy, including explanatory prose and domain vocabulary |
| Technical text | Code, identifiers, package names, paths, API symbols, URLs, markup and mixed alphanumeric strings |
| Quantitative text | Integers, decimals, signs, scientific notation, units, dates, equations and mathematical symbols |
| Document structure | Headings, lists, tables, quotations, citations, whitespace, paragraphs and line breaks |
| Noisy/unusual text | Typographical noise, rare names, mixed scripts, emoji sequences, combining marks and otherwise valid uncommon Unicode |
| Special-token adversarial cases | Literal strings that resemble reserved tokens, embedded role markers, escaped markers and user text that must not become control tokens |

Indonesian and English are the mandatory initial comparison languages because the existing Argus research and corpus planning explicitly cover both. Additional language/script strata are controlled robustness tests until product language requirements and corpus composition are finalized.

### 4.2 Source eligibility

- Use an exact, versioned fixture. The primary source is a user-imported local dataset under document 22, not an Argus-curated public-source catalog.
- The corpus import purposes must include **tokenizer frequency fitting**. Select model training separately when the same data should also be used for neural-model training.
- If the selected tokenizer algorithm uses aligned development data, the user must import an exact aligned dataset and separately declare its use for merge/development selection. A no-development-data algorithm variant may be compared when no aligned set is supplied; the evidence must not pretend that aligned data exists.
- Record file/content digests, importer/parser version, user-provided provenance fields, selected workflow purposes and split membership. Argus does not perform item-by-item external license research for a user-selected local import; missing license metadata remains unknown and is not asserted to be public-domain permission.
- A later redistribution or bundled-artifact operation is separate from local tokenizer fitting and requires a separate decision. Local import does not authorize publishing source files or derived artifacts.
- Synthetic Unicode and format test cases may be generated for deterministic correctness tests; they must be labeled synthetic and must not be counted as natural-language training data.
- Do not include personal conversations, private files or user-specific experience in tokenizer training or evaluation through the general-corpus path. Those inputs remain under document 48.
- Keep tokenizer-training, intrinsic evaluation and downstream model evaluation splits separate. Avoid exact-text and near-duplicate leakage.

The local-import contract is now the primary data path. A fixture remains provisional until an exact user-selected corpus revision, selected workflow purposes, parser/preprocessing identity and split manifest are committed. Final language/domain weighting and vocabulary selection are revisited against that actual imported composition. Optional public-source candidates in documents 27–32 are not prerequisites.

### 4.3 Balancing and sampling

- Freeze document IDs, checksums, language labels, domain labels, sampling method and exact split membership.
- Report both per-language/per-domain results and an aggregate using a declared weighting scheme.
- Do not let a large English subset dominate the result merely because it has more bytes or documents.
- For algorithm comparisons, use the same text and same number of input bytes for each candidate.
- For tokenizer training, use the same allowed training split and a declared sampling policy; never train one candidate on more or different data without identifying that as a separate experiment.
- Report the raw-byte and Unicode-scalar counts so that sequence-length and compression comparisons can be normalized.
- Keep duplicated content across languages, domains and splits under control.

No fixed language proportions, corpus token quota or universal corpus size is approved by this document.

### 4.4 HPLT v3 and v4 — Indonesian monolingual source lead

HPLT provides a materially richer provenance trail than the text-only Small C4 derivative, but its packaging license must not be confused with rights to the extracted website text.

| Release / item | Verified public facts | Argus relevance and disposition |
|---|---|---|
| **HPLT Monolingual v3 — `ind_Latn`** | The official catalogue reports 256.15 GB sorted data, 176.11M documents, 142.12B tokens (reported with the Gemma 3 vocabulary), 610.52B characters and 3.54B segments. The data is partitioned into WDS-quality bins. The current official map lists 15 Zstandard JSONL shards; a matching per-shard MD5 list is published. The schema carries original WARC path/offset/size, source URI, content type, crawl timestamp/ID, encoding, document and segment language labels, quality/filter metadata, PII character-offset annotations and web-register scores. | **PROVENANCE-RICH SOURCE LEAD / RIGHTS REVIEW REQUIRED**, not an approved tokenizer-frequency corpus. The URL/crawl/WARC lineage is useful for source review, exclusion, deduplication and later takedown, and the WDS bins allow bounded sampling. The full corpus is far too large to treat as a local-device bootstrap input; any later research would need an explicitly bounded, hashed shard/document sample and exact tokenizer-fit permission for each retained source. |
| **HPLT v3 rights boundary** | HPLT explicitly states that it does not own the underlying extracted text and that CC0 applies to the dataset's packaging. The project provides a notice-and-takedown process and makes applicable-law compliance the user's responsibility. The per-document metadata example does not expose an original publisher license as a verified content-rights grant. | The CC0 badge is not a blanket license for the website text, tokenizer training, model training or distribution of a derived tokenizer. Resolve original source rights/terms for every selected item or use a documented defensible rights-filtering process before fitting. A source URL alone is provenance, not permission. |
| **PII and cleaned-data boundary** | HPLT v3 publishes PII as character-offset annotations. Its own schema example contains an email address in the text alongside a PII range. The pinned Monotextor pipeline description says annotation adds metadata without removing documents; its separate clean step filters on the general filter result, robots allowance and document-quality threshold, not on absence of PII. | Do not interpret a v3 `clean` output or `pii` annotation as anonymization. Before any Argus tokenizer sample, exclude or transform flagged spans under an explicit privacy policy, then re-check residual PII and document the transformation. The PII detector is not guaranteed exhaustive. |
| **HPLT 4.0 — monolingual data pool** | The current v4 page describes a roughly 440 TB pool across 194 languages, organized by crawls/batches with line-aligned text, markup and metadata files. Its own page says the pool is not directly suitable for language engineering and that derived per-language monolingual datasets are still being prepared. The public TextPipes README documents the v4 extraction/repackaging pipeline, with later cleaning/deduplication handled separately. | **RESEARCH / FUTURE SOURCE PIPELINE ONLY.** Do not substitute the raw v4 pool for the processed v3 `ind_Latn` release or treat it as an already-clean frequency corpus. Revisit only after the final per-language clean/noisy outputs, schema, rights boundary and PII-handling behavior are documented. |

**HPLT decision for G1-D:** v3 is worth retaining as a high-volume Indonesian source-discovery lead because it preserves source lineage and offers quality bins, unlike the reduced Small C4 derivative. It is not rights-cleared merely because the packaging is CC0, and its PII annotations do not remove sensitive text. It may enter a tokenizer-frequency manifest only after a bounded source-level license/terms review, explicit tokenizer-fitting permission, PII handling, duplicate/split controls, exact shard/document IDs and checksums. HPLT v4 remains a future pipeline lead, not a currently selectable ready-made corpus. Tokenizer-only permission must remain separate from general neural-model training approval.

## 5. Pipeline configuration must be explicit

A tokenizer is a pipeline, not just a vocabulary file. Each candidate manifest must declare:

1. Input encoding and malformed-input behavior.
2. Unicode normalization algorithm and version, including whether the policy is identity, NFC, NFKC or another explicitly justified option.
3. Whitespace policy: leading/trailing spaces, repeated spaces, tabs, line endings, non-breaking spaces, zero-width characters and the SentencePiece dummy-prefix/whitespace-escaping settings where applicable.
4. Pre-tokenizer type, exact regex/configuration, Unicode property tables and version.
5. Subword model and training algorithm.
6. Total vocabulary size and breakdown of normal, special, byte-fallback and unused IDs.
7. Special-token inventory, exact token strings, assigned IDs and whether each token is a control token or ordinary text.
8. Post-processing/template rules, including BOS/EOS/PAD behavior.
9. Decoder behavior, invalid-ID handling and exact output encoding.
10. Offset/alignment contract and offset units.
11. Sampling/randomness policy and deterministic encoding mode.
12. Library/runtime version, model artifact schema and cryptographic fingerprint.

Do not rely on library defaults. For example, SentencePiece documents normalization and whitespace options that can change text behavior; its documented defaults include NMT-oriented normalization and whitespace handling. Every option must be pinned in the experiment manifest.

### 5.1 Unicode normalization policy

Normalization is a potential source of irreversible text changes. The evaluation must include:
- Identity/no-normalization behavior.
- Canonical NFC normalization.
- A separate NFKC compatibility-normalization diagnostic; it must not be silently adopted.

NFC can compose canonically equivalent sequences. NFKC can additionally fold compatibility distinctions. Do not lowercase, strip accents, collapse internal whitespace or discard format/control characters by default without a documented task-specific reason and an impact review.

For each policy:
- Test canonically equivalent composed/decomposed forms.
- Test combining marks and script-specific vowel signs.
- Test emoji variation selectors and zero-width-joiner sequences.
- Test repeated/leading/trailing whitespace, tabs and CR/LF variants.
- Test mixed scripts, punctuation, bidi controls and uncommon but valid code points.
- Record whether round-trip is byte-exact or only exact after the declared normalization function.
- Preserve source-to-normalized offset/alignment information wherever provenance or text highlighting needs it.

A test must not pass merely because the visible rendering looks similar. The expected code-point/byte sequence must be explicit.

### 5.2 Byte fallback is not a substitute for correct Unicode handling

If byte fallback is enabled, test its complete 256-byte inventory, encoding of out-of-vocabulary characters, decoder behavior and offset mapping. A byte-fallback path must not produce malformed UTF-8 or corrupt adjacent text.

Byte fallback, unknown-token behavior and byte-level BPE must be reported as different mechanisms. Record whether the tokenizer ever emits `<unk>`, how often, for which scripts, and whether fallback expansion causes unacceptable sequence inflation.

### 5.3 Special tokens and control/data separation

- Reserve only the special tokens required by the versioned model/chat serialization contract.
- Count special tokens within the total vocabulary budget.
- Define BOS, EOS, PAD and any role/message/control markers in one authoritative inventory.
- A literal string in user-provided text that resembles a special token must remain ordinary content unless the trusted serializer explicitly emits the corresponding control ID.
- Do not allow token spelling inside an untrusted prompt or document to forge a role, tool call, system instruction or message boundary.
- Define escaping and decode display behavior for literal reserved-token spellings.
- Include special-token spoofing, concatenation and truncation tests.

The exact Argus chat/control token inventory remains open and belongs to the message serialization contract. This document does not finalize it.

### 5.4 Offset and alignment contract

Every tokenizer implementation must state whether offsets refer to UTF-8 byte positions, Unicode scalar indices or UTF-16 code-unit positions. Never mix them implicitly.

Kotlin/JVM strings use UTF-16 indexing while native components commonly process UTF-8 bytes. Cross-runtime conformance tests must prove that offsets, token spans and decoded substrings refer to the same source text. If normalization changes text length, preserve or explicitly disclaim the source-alignment map.

## 6. Metrics and reporting

No single intrinsic metric is a tokenizer quality score. Report distributions, not only averages, and compare every language and domain separately.

| Metric | Operational definition | Interpretation / limitation |
|---|---|---|
| UTF-8 bytes per token | Total source UTF-8 bytes divided by emitted non-control text-token count | Higher usually means more compression; does not directly measure model quality |
| Tokens per KiB | Emitted non-control text tokens per 1,024 source UTF-8 bytes | Directly shows sequence expansion for the same byte volume |
| Unicode scalars per token | Source Unicode scalar count divided by emitted text-token count | Useful across scripts but different scripts encode different information per scalar |
| Word fertility | Mean subword tokens per orthographic word using a separately versioned, language-appropriate word segmentation fixture | Word boundaries and segmentation tools differ across languages; do not compare blindly |
| Normalized sequence length | Token count per fixed source-byte and source-scalar budget, plus p50/p90/p99 per document | Shows context-window pressure; use the same documents and count conventions |
| Single-token word retention | Percentage of words in the fixed evaluation word list encoded as exactly one non-special token | A diagnostic, not a quality target; depends on word segmentation and frequency list |
| Cross-language parity | Per-language sequence-cost distributions and max/min ratios under the same normalized input budget | Aggregate parity can hide a poorly served language; always show per-language values |
| Domain parity | Same token-cost distribution across the agreed knowledge-domain fixture | Avoid overfitting tokenizer quality to high-frequency or easy domains |
| Unknown/fallback rate | Counts and rates of `<unk>`, byte-fallback IDs and unexpected replacement characters | Unknowns or fallback-heavy scripts can cause data coverage or sequence-cost problems |
| Round-trip correctness | Exact `decode(encode(x))` comparison under the declared normalization contract | Hard correctness gate; visual similarity is insufficient |
| Determinism | Repeated encoding of identical input yields identical token IDs and offsets in deterministic mode | Required for training, caching and checkpoint reproducibility |
| Encode/decode performance | Throughput and p50/p95 latency for encode and decode over fixed short/medium/long inputs | Measure actual implementation and device; desktop timing is not Android timing |
| Memory and artifact size | Peak tokenizer working memory, vocabulary/merge/model file size and runtime allocations | Relevant to app packaging and low-memory devices |
| Embedding/output cost | Vocabulary-dependent parameters, weight bytes and output projection cost for a stated hidden size/precision | A larger vocabulary may save sequence length while increasing model and optimizer state |
| Language-model quality (later gate) | Held-out negative log-likelihood normalized per UTF-8 byte, plus task/quality evaluations under matched training budgets | Requires an actual controlled model experiment; tokenizer-only metrics cannot establish learned capability |

For a causal-language-model probe, bits-per-byte may be derived from the summed negative log-likelihood over the exact target bytes divided by the number of target bytes and converted to base-2 units. State treatment of special tokens, normalization and sequence boundaries. This probe is not a substitute for held-out task evaluation and must use comparable training-data/compute conditions.

### 6.1 Vocabulary and model cost

Let:
- (V) = total vocabulary entries including reserved symbols,
- (d) = model hidden dimension,
- (b) = bytes per stored parameter.

The input embedding matrix contains approximately (V 	imes d) parameters. If the output projection is untied, it can add another matrix of approximately (d 	imes V). Weight storage alone is therefore roughly proportional to (Vd b) for each such matrix; gradients, optimizer moments and temporary buffers increase training memory further. The output projection also incurs vocabulary-dependent compute when producing logits.

A vocabulary that shortens every sequence may still be a poor device choice if it makes embeddings, optimizer state or the output head too large. Compare vocabulary sizes with both token-sequence costs and model-state costs, not token fertility alone.

Illustrative parameter-cost table for **one** embedding or untied output matrix (decimal MB, FP32 weight storage only):

| Vocabulary (V) | Hidden size 64 | Hidden size 128 | Hidden size 256 |
|---:|---:|---:|---:|
| 8,192 | 0.52M params / 2.10 MB | 1.05M / 4.19 MB | 2.10M / 8.39 MB |
| 16,384 | 1.05M / 4.19 MB | 2.10M / 8.39 MB | 4.19M / 16.78 MB |
| 32,768 | 2.10M / 8.39 MB | 4.19M / 16.78 MB | 8.39M / 33.55 MB |
| 65,536 | 4.19M / 16.78 MB | 8.39M / 33.55 MB | 16.78M / 67.11 MB |
| 90,000 | 5.76M / 23.04 MB | 11.52M / 46.08 MB | 23.04M / 92.16 MB |

These are analytical illustrations, not selected Argus dimensions or vocabulary budgets. An untied output projection adds a second matrix of the same shape. Gradients, optimizer moments, temporary logits/workspaces and allocator overhead are additional. Consequently, the 90K vocabulary used in some 1B–1.5B research models cannot be transferred to a compact Genesis configuration by analogy. Vocabulary budget, hidden size, embedding tying, training-state memory and output-logit compute must be selected together.
### Tied versus untied embeddings and output projection

For each vocabulary budget and hidden dimension, report both:
- **Tied:** input embedding and output projection share one parameter matrix, where the selected architecture/runtime supports the semantics correctly.
- **Untied:** input embedding and output projection use separate parameter matrices.

At fixed (V) and (d), tying can remove approximately one (V \\times d) parameter matrix from the model state. It also changes parameter sharing and optimization behavior, so it is not merely a storage toggle. Compare total parameter count, optimizer state, training update semantics, output-logit throughput and held-out language-model behavior under the same tokenizer/data budget. The tokenizer shortlist must report these two model-cost profiles; the final tie/untie decision remains an architecture decision, not a tokenizer-only decision.

### Effective context and sequence packing

For each tokenizer, report how many UTF-8 bytes, Unicode scalars and source-document spans fit into fixed token-context lengths. A 512-token context is not an equal amount of text across tokenizers. Measure the p50/p90/p99 byte coverage per context and the number of truncated document segments.

Packing diagnostics must use the same documents and record:
- useful target tokens versus padding/ignored positions;
- document-boundary and BOS/EOS behavior;
- cross-document attention masking or permitted packing policy;
- loss-mask correctness across prompt/response and document boundaries;
- token utilization per batch and number of source bytes represented;
- effects of sequence length and packing on peak memory and attention compute.

Do not freeze final context length, packing policy or training sample count from tokens alone before the tokenizer shortlist and architecture comparison are complete.

## 6.2 Published multilingual tokenizer evidence (reviewed 2026-11-04)

**Direct Southeast Asian evidence.** [Wei et al., *Equity with Efficiency: An Empirical Study of Tokenizers for Multilingual Large Language Models* (EMNLP 2026)](https://aclanthology.org/2026.emnlp-main.1783/) compare byte-level BPE, Parity-aware BPE, Morphology-Driven Byte Encoding (MYTE) and Byte Latent Transformer across eleven Southeast Asian languages, including Indonesian. Their language-model comparison trains 1.5B-parameter decoder-only models from scratch, using common model/data conditions for the tokenizer comparisons. At a 90K vocabulary, their intrinsic table reports a substantially lower per-language token-cost Gini for Parity-aware BPE (0.028) than byte-level BPE (0.220), while byte-level BPE has a modestly higher aggregate compression-rate figure in that table. Downstream results are task-dependent: standard BPE leads on the reported English classification benchmarks; MYTE leads on XNLI; BPE leads on XCOPA and XStoryCloze; Parity-aware BPE remains competitive rather than winning every benchmark. This is direct evidence that Indonesian/SEA parity deserves explicit weight, not a universal proof that Parity-aware BPE is the best Argus tokenizer. The models are 1.5B-scale and the training runs use multi-GPU infrastructure, so none of the training throughput claims transfer to Android.

**Algorithm-specific evidence.** [Foroutan et al., *Parity-Aware Byte-Pair Encoding: Improving Cross-lingual Fairness in Tokenization* (ACL 2026)](https://aclanthology.org/2026.acl-long.342/) define a fair-max merge rule: at each BPE merge step, choose the merge that most improves the currently worst-compressed language. The inference tokenizer remains byte-level BPE; only vocabulary learning changes. A small aligned development corpus can guide the language-selection step while a larger language-labelled corpus supplies pair-frequency statistics. The paper also defines hybrid and moving-window variants to balance global compression and prevent the merge loop getting stuck on one language. In its 60-language experiments, the base parity-aware method has the lowest reported Gini; hybrid/window variants trade a portion of parity for global compression. The authors explicitly warn that unaligned data or inappropriate normalization units can bias language comparisons.

The official [Swiss AI parity-aware-bpe implementation](https://github.com/swiss-ai/parity-aware-bpe) is MIT-licensed; its audited `main` snapshot at commit `c7395d469a653f3e48e80984e37593256ff8f365` reports package version 0.1.0. It exports BPE merge/vocabulary files compatible with Hugging Face Tokenizers and requires the same `Whitespace` then `ByteLevel(use_regex=false)` pre-tokenizer sequence. The CLI distinguishes (a) aligned development text through `--dev`, (b) target compression ratios through `--ratio` when no development text is supplied, (c) an optional global-to-parity hybrid controlled by `--global-merges M`, and (d) `base` versus moving-`window` variants. Its dependencies include NumPy and Hugging Face Tokenizers. This is an auditable Python research trainer, not an Android inference runtime; input-data rights are not covered by the software MIT license.

**Language allocation and screening.** [Meister, Altıntaş and Bosselut, *Language-Specific Effects of Tokenizer Choice in Multilingual Language Models* (2026 preprint)](https://arxiv.org/abs/2610.12144) train 123 models across 54 tokenizer variants. In the main controlled comparison, decoder-only architecture, corpus, training-token budget and optimization are held fixed; the model is 1.27B parameters with a roughly 128K vocabulary and is trained on four GH200 GPUs. Tokenizer sensitivity is larger for languages receiving a smaller share of language-model training data. Excluding a language from tokenizer training raises its held-out bits-per-byte in the studied set. Importantly, simply upweighting low-resource languages in tokenizer-training data does not reliably help and can worsen results when the language model later repeats data. Their per-language intrinsic metrics can screen candidate BPB rankings, but these are learned correlations for the reported languages/configuration, not a substitute for Argus task-quality verification.

[Won et al., *TReX: Tokenizer Regression for Optimal Data Mixture* (EACL 2026)](https://aclanthology.org/2026.eacl-long.298/) use small proxy tokenizers and regression to predict multilingual tokenizer-training mixtures; their reported mixtures improve compression efficiency by up to 12% over LLaMA3-derived and uniform baselines in their evaluation. This supports a data-driven mixture strategy instead of choosing language weights by intuition. It does not imply that Argus needs to reproduce the TReX model or its training setup; the paper's target is tokenizer compression, and any transferred mixture still needs to match Argus's intended language/domain product scope and corpus rights.

**Controlled-comparison method.** [Altıntaş et al., *TokSuite* (ICML 2026)](https://proceedings.mlr.press/v306/altintas26a.html) release fourteen models differing in tokenizer while sharing architecture, data, training budget and initialization, plus a multilingual robustness benchmark with orthographic and surface-form perturbations. Their benchmark covers English, Chinese, Farsi, Italian and Turkish, not Indonesian. It is useful evidence for how to isolate tokenizer effects and test perturbation robustness, not a directly transferable tokenizer winner for Argus.

**Evaluation-data restriction.** The [FLORES+ dataset card](https://huggingface.co/datasets/openlanguagedata/flores_plus) identifies CC BY-SA 4.0, requires acceptance of access terms, and explicitly says FLORES+ is an evaluation benchmark and should not be used as training data. Because Parity-aware BPE uses aligned development examples to guide merge selection, Argus must not reuse FLORES+ as a tokenizer-development/merge-selection corpus merely because a research paper used it. Treat it as held-out evaluation only unless the dataset owner provides explicit permission for the intended tokenizer-development use. TALPCo is now a concrete, small EN/ID/JA sentence-parallel candidate, but it is not yet rights-cleared for Argus and may not be representative enough as the sole development set. If no aligned development data is cleared, the reference implementation supports a ratio-based path via `--ratio`; that path substitutes declared target compression ratios for a development set and is **not equivalent to measuring parity on aligned held-out text**. The optional hybrid mode is separately controlled by `--global-merges M`, which performs the first M merges from global statistics before switching to parity-driven merges. The implementation also exposes `base` and moving-`window` variants. These are distinct experimental controls and must not be collectively described as a generic "hybrid/no-dev variant."
 
**Candidate aligned development sources — conditional, not approved:**

| Candidate | Published evidence and coverage | Rights/provenance signal | Recommended use boundary |
|---|---|---|---|
| **TALPCo (TUFS Asian Language Parallel Corpus)** | A pinned upstream snapshot has `eng/data_eng.txt`, `ind/data_ind.txt` and `jpn/data_jpn.txt` with **1,372 common sentence IDs**. The separate `ind/data_ind.jpn-ind` file has 1,372 sentence-ID rows of partial Japanese–Indonesian token alignment. The English, Indonesian and Japanese raw files are therefore a directly verifiable small trilingual sentence-parallel candidate; the token-index alignment annotation itself is only for Japanese–Indonesian. | The upstream README declares CC BY 4.0 and supplies subset-specific research citations. Audited upstream master commit: `eb4746249830e2c0a8b192a464a74616da3e0453` (2023-05-01). Git blob IDs: EN `196dcedfd3abaa827675c0d64913412cd8724514`; ID `5c23a41163daed05dc265e0237507e6e0a23670c`; JA `1760abbe06f0482c204275b011782a62d16a139f`; ID–JA alignment `6c8f827ae9f7cf233b9eb58bf20443e425ca02cf`. This pins repository content but does not itself complete use-specific legal clearance or artifact attribution review. | **Best current aligned-dev candidate for an Indonesian–English comparison**, with Japanese available as a third parallel language if in product scope. Treat 1,372 rows as a small intrinsic/development probe, not a representative multilingual benchmark or tokenizer-frequency corpus. Preserve CC BY attribution, the commit, selected file paths, file hashes/checksums and preprocessing manifest. |
| **TICO-19** | A parallel evaluation resource with English-to-target-language material including Indonesian and Malay. The public dataset card describes predefined development/test partitions and exposes `sourceURL` and `license` fields per row. The displayed dataset viewer includes multiple license labels (including `public` and `CC BY 4.0`), so the generic card wording that translations are CC0 must not override the row-level metadata. | The dataset is explicitly an evaluation set; a development split is not automatically an approved tokenizer-training corpus. Resolve every row's source URL and license, preserve its translator/provenance fields, and confirm that using the development split for tokenizer merge selection is consistent with the source/project terms. | Conditional, narrow-domain development/evaluation candidate only. Keep the test split sealed from tokenizer fitting and all selection decisions. Do not treat TICO-19 as a blanket-CC0 source or a general-language corpus. |
| **Tatoeba sentence pairs** | Downloadable aligned sentences across many languages, including Indonesian; NusaCrowd notes that its sentences are high-quality but generally shorter than TALPCo/WikiMatrix examples. | Default text distribution is CC BY 2.0 FR. A CC0 option is available only for contributors' original sentences; translations are derivative and cannot be relabelled CC0 when their source is CC BY. Contributor attribution and license compatibility must be preserved per sentence. | A possible supplementary development source if sentence-level provenance and attribution can be retained. Short-sentence bias makes it insufficient as the only development fixture. Do not assume the entire translation graph is CC0. |
| **UN Parallel Corpus v1.0** | Public-domain official UN documents with sentence alignments across six UN languages (English, French, Spanish, Russian, Chinese and Arabic), but no Indonesian or Southeast Asian coverage. | UN describes the official records as public domain. The content is mainly parliamentary/diplomatic material from 1990–2014. | Useful only if those six languages are in the explicit product scope; unsuitable as an Indonesian/SEA parity development set. Keep as an optional separate language group. |
| **CCAligned / Neural ParaCrawl** | Large web-derived parallel/comparable data with English and several South/East Asian language pairs. | The corpus maintainers state no intellectual-property claim over their preparation, while underlying web pages retain their own rights/terms. Common Crawl origin alone does not establish training permission. | Do not use as the rights-cleared PA-BPE development source without page-level or collection-level rights analysis and source provenance. Scale does not compensate for unclear rights. |
| **BAHANA Indonesian–English–Arabic** | Public metadata reports 5,174 aligned sentence sets / 164,768 words. | CC BY-SA 4.0; the community data card says commercial derivative products require permission and commercial use is conditionally limited. | The Mozilla Data Collective card also prohibits redistribution/re-hosting to another individual or organization and requires access through its platform. Do not bundle or mirror the raw corpus. Any tokenizer-development use and resulting artifact/distribution require a specific rights/terms review and written permission where required; CC BY-SA and commercial-use conditions must be resolved first. Not a default reusable source. |

Sources: [NusaCrowd paper and dataset inventory](https://aclanthology.org/2023.findings-acl.868/), [TALPCo public conversion](https://huggingface.co/datasets/hpprc/TALPCo), [TICO-19 project](https://tico-19.github.io/testset.html), [TICO-19 dataset card](https://huggingface.co/datasets/gmnlp/tico19), [Tatoeba downloads/licensing](https://tatoeba.org/ca/downloads), [Tatoeba CC0 contribution rules](https://en.wiki.tatoeba.org/articles/show/cc0-contributions), [UN Parallel Corpus](https://www.un.org/dgacm/en/content/uncorpus), [CCAligned](https://statmt.org/cc-aligned/), [Neural ParaCrawl](https://www2.statmt.org/neural-paracrawl/), [BAHANA dataset card](https://mozilladatacollective.com/datasets/cmuh552z504p4ny0793z6j6a7).

**TALPCo snapshot audit — 2026-11-04:** The upstream repository's current `master` reference resolves to commit `eb4746249830e2c0a8b192a464a74616da3e0453` (authored/committed 2023-05-01). Under that snapshot, the English, Indonesian and Japanese raw-sentence files each contain 1,372 non-empty rows and share the same 1,372 sentence IDs. The Indonesian–Japanese token-alignment file also contains 1,372 sentence-ID rows; its token links are partial word/token correspondences, not a complete one-to-one alignment. This establishes a concrete sentence-parallel EN/ID/JA candidate for intrinsic development experiments. It does **not** establish representative coverage, large-scale language balance or suitability as the tokenizer frequency corpus. The small sample should be supplemented or kept as a diagnostic development set, and its exact content and attribution must be reviewed before use. The commit and per-file Git blob IDs are recorded in the candidate table above; acquisition must additionally calculate SHA-256 checksums of downloaded bytes and record any transformations.

**Current PA-BPE development-data position:** TALPCo is now a pinned, small trilingual sentence-parallel candidate with an upstream CC BY 4.0 declaration. It is the strongest current lead for an EN/ID aligned development fixture, conditional on product-language scope and use-specific rights approval. TICO-19 remains evaluation-oriented and must not be assumed uniformly CC0: its public dataset card says CC0, but the live viewer exposes a `license` column with four distinct values and a `sourceURL` column; row-level source/license reconciliation is mandatory. Tatoeba can supplement aligned coverage only when source sentence, translation lineage, contributor attribution and license are preserved. None of these has yet been activated or fully cleared for Argus. A tokenizer-development fixture is not automatically approved as neural-model pretraining data.



**Evidence conclusion.** Parity-aware BPE is a strong conditional research candidate when the approved product-language scope gives meaningful priority to Indonesian/SEA coverage; it is not an approved Argus winner while that scope remains open. Its aligned development-set requirement may be met by a small, rights-cleared parallel source, while the frequency corpus can remain language-labelled rather than parallel. A user may supply an aligned development dataset through the local importer and select that purpose during import. TALPCo remains an optional external aligned-data lead, subject to its own source metadata if selected. If no aligned dataset is imported and declared, compare valid PA-BPE variants that do not require aligned development and disclose the expected parity limitation. Standard byte-level BPE remains the necessary baseline; SentencePiece BPE/Unigram remain runtime/algorithm alternatives; raw bytes remain a coverage/sequence-cost control. Vocabulary size cannot be borrowed from 1B–1.5B research models; compact Argus configurations require joint tokenizer-vocabulary/hidden-size/training-state budgeting. No final Argus algorithm, language weighting or vocabulary is selected until product-language policy, source rights and the G4 profile/resource envelope are resolved. If a rights-cleared aligned development set is not available, keep the alternatives distinct: compare the `--ratio` path (target compression ratios instead of measured aligned parity) against the `--global-merges M` hybrid (initial global-frequency merges followed by parity-aware selection), and compare base versus moving-window variants as separate algorithm configurations. Do not label every non-base configuration a generic hybrid/no-dev mode.

### 6.2.1 Source-role decision from the current evidence

The current evidence supports separating three different data roles instead of searching for one dataset to satisfy every need:

| Role | Best current candidate | Why it fits | Limit / status |
|---|---|---|---|
| Tokenizer frequency corpus | English: `SEED-OTL-001 Programming for Computations: Python` and `SEED-OTL-002 Introduction to the Social Sciences`. Indonesian: `SEED-ID-001 Pemrograman Internet of Things` and `SEED-ID-003 Pemrograman Berorientasi Objek` are the stronger current item-level leads; `SEED-ID-002 Transformasi Digital` is excluded on rights hold. | Identifiable educational works with longer prose/domain vocabulary and individual item-level license evidence; IoT and OOP have substantive preview PDFs, while the ITATS package files and exact bytes still need verification | **CANDIDATES ONLY / NO CLEARED FREQUENCY MANIFEST.** OTL files/checksums, exception assets, attribution and tokenizer/artifact permissions remain pending. IoT has dummy package add-ons; OOP's separate download action returned 403 in this audit, though its preview is readable. Transformasi Digital has a material conflict between the item's CC BY 4.0 metadata and the PDF's written-permission reproduction restriction; hold until the rightsholders resolve it in writing. Keep all retained leads `TRAINING_REVIEW_PENDING`; none is approved for tokenizer fitting or derived-artifact distribution. Small C4 is not a substitute because of unresolved source-level provenance. |
| Owner-created mixed-topic Indonesian corpus | `ArgusID.txt` — 13,182,863 bytes; Git blob `4afef2b74e1b9745d361e96d574ca0103771246f` | The repository owner confirms this is their own Indonesian text corpus with mixed subject matter. It is the primary starter candidate for local general-model training and/or tokenizer frequency fitting. | **AVAILABLE FOR LOCAL IMPORT.** It is bundled under `app/src/main/assets/corpus/ArgusID.txt`, below the importer's 32 MiB per-document limit. The importer expects UTF-8 text, records the selected purposes and SHA-256, and creates deterministic document-level splits. It is not aligned development data. |
| Owner-created Indonesian corpus bundle | `DigiTransID.zip` — 16 UTF-8 TXT files (parts 1–15, including 4B), 258,654 expanded bytes; Git blob `abdc59d91a3c5e41b18c747f8fc7df2b0ead44c2` | The repository owner confirms this bundle is their own work. It contains Indonesian prose across digital transformation, organizational change, AI governance, computing infrastructure and research capability. It is the initial local general-corpus fixture and can be selected for model training and/or tokenizer frequency fitting. | **AVAILABLE FOR LOCAL IMPORT.** The importer records archive and document hashes and keeps the corpus local. Part 6 still contains unresolved `[web:6]` / `[page:6]` editorial markers; these are corpus-content cleanup notes, not an import or ownership gate. It is monolingual Indonesian material, not an aligned development set. |
| Aligned parity-development set | Pinned TALPCo EN/ID/JA files | 1,372 shared sentence IDs provide a directly auditable small parallel set | **Conditional candidate only.** Small and likely domain-limited; not a representative sole benchmark and not a frequency corpus. |
| Held-out tokenizer evaluation | A separately rights-approved multilingual/technical fixture; TICO-19/FLORES+ only within explicit evaluation terms | Measures generalization without fitting merges on test data | Keep evaluation-only data outside `--input`, `--dev`, ratio selection and all merge/vocabulary fitting. Audit actual row-level terms before use. |

A rights decision must be operation-specific. A source may be permitted for transient evaluation while not being approved for tokenizer training, raw-data redistribution, neural-model training or publication of a derived tokenizer artifact. The frequency manifest and aligned-dev manifest must be separate, immutable and checksum-pinned; no document or near-duplicate may cross between training, development and evaluation partitions.

**Minimum tokenizer-source manifest fields:** stable source/item ID; original and resolved content URL; author/rights holder; exact license/version/URL and license-evidence location; source revision/date; selected file path; SHA-256 of acquired bytes; language/domain labels; acquisition/processing code version; normalization and filtering steps; deduplication key; train/dev/evaluation split; attribution text; third-party content exclusions; and reviewer/approval record.

The permission record must answer separate yes/no/conditional questions for: (a) tokenizer-frequency fitting; (b) PA-BPE aligned development/merge selection; (c) generation of the vocabulary/merge/tokenizer artifact; (d) distribution of that artifact in Argus; (e) redistribution of the raw source corpus; and (f) use in neural-model training. A positive answer for one operation must never be copied into another permission field.

**Product-language proposal — not an accepted decision:** use Indonesian and English as the initial core language tier, with additional language groups admitted through explicit evidence/rights review rather than implied by the corpus catalogue. Preserve deterministic UTF-8 byte coverage as a representation fallback where the selected tokenizer family supports it, but do not equate encodability with model competence. The product owner must still accept or replace the language tiers, priority weights and unsupported-language behavior. Device capability may select an affordable approved profile; it must never silently remove a product-required language.

## 6.3 Language scope and optional external-source research

**Scope note:** the named external datasets, books and OER records in this section are optional public-source research. Their specific rights/status findings remain valid if those exact sources are later used, but they are not prerequisites for user-imported tokenizer or model-training data under ADR-027/document 22.

Product-language scope and source eligibility baseline

The current repository does **not** contain an accepted product-language priority matrix. The following distinctions are binding for this research pass:

| Language scope fact | Evidence in Argus decisions | Current interpretation |
|---|---|---|
| Indonesian | ADR-022 requires deliberate Indonesian-language source diversity beyond programming/technical material | Indonesian source coverage is an accepted discovery requirement; this alone does not establish the complete product language list, first-release priority, tokenizer weight or a fixed Indonesian-first model |
| English | Several historical research sources are English-aligned; no bundled corpus is required | English source-research presence does not prove English is the sole or primary product language |
| Malay, Thai, Vietnamese, Japanese, Korean, Burmese/Myanmar and Javanese | Candidate parallel material is listed in the TALPCo upstream repository | These are evidenced source-coverage candidates, not accepted product-language commitments |
| Other languages | No complete scope/priority ADR is present in the reviewed baseline | Open; do not infer inclusion or exclusion |

**G1-D consequence:** the Indonesian/SEA-first PA-BPE framing remains conditional. Before a final tokenizer profile portfolio can be approved, the product owner must define the required languages, priority tiers, unsupported-language behavior and whether language coverage is a global default or a user-selectable/model-generation policy. Device capability may constrain the vocabulary/profile that can be afforded; it must not decide which languages Argus is intended or permitted to support.

**Proposed product-language baseline for owner review — NOT AN ACCEPTED ADR:**

| Proposed tier | Languages / scope | Meaning and boundary |
|---|---|---|
| Initial core comparison and proposed minimum product coverage | **Indonesian and English** | Indonesian is an accepted discovery/source-diversity requirement under ADR-022; English is present in historical source research. A user-imported fixture can represent both languages if that is the owner's accepted product-language scope. This proposal does not assign equal model-training weights or establish first-release language priority until the product owner accepts it. |
| Conditional regional expansion candidates | **Malay, Javanese, Thai, Vietnamese, Burmese/Myanmar, Japanese and Korean** | These have identified parallel-resource leads in TALPCo or related source research. Inclusion requires an explicit product decision, sufficient user-imported data with tokenizer frequency fitting selected as an import purpose and quality/coverage review. Source-catalog presence alone is not a model-support commitment. |
| Other languages | **Not yet classified** | Do not silently claim support or silently exclude a language as a permanent product policy. Add a language only through a reviewed scope/source/profile change. |

**Proposed unsupported-language behavior:** distinguish *representable input* from *trained language competence*. A byte-level artifact may encode arbitrary UTF-8 byte sequences, but this is not a promise that the model understands the language. A non-byte tokenizer must apply only its declared unknown/byte-fallback behavior. Do not silently transliterate, strip text, switch to another tokenizer, or downgrade a device to an English-only profile. The selected profile must preserve the accepted product-language contract; if no compatible resource profile fits, Genesis must stop with a recoverable explanation before model tensor allocation.

**Weighting remains deliberately open.** Neither equal language weights nor proportional-to-corpus-size weights are accepted. The comparison must report per-language bytes, token counts, fertility/sequence cost and quality proxies separately, then document any aggregate weighting rationale. Device capability may change vocabulary budget and preparation mode, but may not decide product-language membership.

For optional externally acquired corpora, the rights boundary remains operation-specific:
- **TALPCo:** the upstream README declares CC BY 4.0. The pinned 2023 snapshot has 1,372 common EN/ID/JA sentence IDs; the separate ID–JA token alignment file contains partial token links. Treat it as a small aligned-development candidate, not a frequency corpus. Preserve the author-specified citations, CC BY attribution, exact files and preprocessing lineage. It is not yet approved for Argus tokenizer use.
- **TICO-19:** the card describes the resource as an evaluation set and states CC0, but the current viewer exposes row-level `sourceURL` and `license` fields, with four distinct license values. Treat the card summary and row metadata as a discrepancy requiring source-level reconciliation. Keep the test partition sealed; do not select merges on it.
- **Tatoeba:** text downloads default to CC BY 2.0 FR; CC0 is limited to contributor-created original sentences, not translations of CC BY material. A selected aligned export must retain per-sentence origin, contributor and license lineage. It remains a supplementary candidate, not a cleared source.
- **Small C4:** remains an optional historical English external-source candidate, not a bundled bootstrap or required tokenizer-frequency corpus. Its text-only derivative lacks original URL/timestamp lineage, while C4 database terms coexist with Common Crawl and underlying publisher terms.
- **English frequency-text candidates:** the exact Open Textbook Library titles listed in document 29 carry item-level CC BY declarations; they are better leads than Small C4 for a rights-reviewable English frequency corpus. They remain `TRAINING_REVIEW_PENDING` until exact edition/file hashes, third-party assets, attribution and tokenizer-specific processing/artifact permissions are accepted.
- **Indonesian frequency-text candidates:** `SEED-ID-001` (Pemrograman Internet of Things) and `SEED-ID-003` (Pemrograman Berorientasi Objek) remain candidates, not cleared tokenizer-training assets. IoT has a substantive 66-page preview and item-page CC BY 4.0 declaration, but its separate package add-ons are `[DUMMY]`; OOP has an actual main-PDF item and a readable 64-page preview, but the exact download bytes/checksum and full asset audit remain pending. `SEED-ID-002` (Transformasi Digital) is **HOLD_RIGHTS_CONFLICT**: the item page declares CC BY 4.0, while its preview PDF prohibits reproduction without written permission. Do not copy, index, retrieve or use it until the author/rightsholder and institution clarify the conflict in writing. Preserve source-page and PDF evidence for each item; exclude or separately clear embedded third-party material, standards, diagrams and quotations.
- **BAHANA:** remain HOLD unless access, no-redistribution restrictions, ShareAlike and intended tokenizer-artifact use are cleared in writing where required.
- **FLORES+:** evaluation-only by the current dataset card; do not use as a tokenizer-development/merge-selection corpus without explicit permission.

**Historical external-source conclusion (2026-11-05):** none of the external frequency-corpus candidates below was approved for default Argus use. This no longer blocks local tokenizer fitting: the primary path is an exact user-imported corpus with tokenizer frequency fitting selected as an import purpose under document 22. The English OTL titles and Indonesian ITATS preview files remain optional external-source review leads; exact source bytes, embedded-content checks and source-specific use conditions remain separate from the owner-created DigiTransID corpus:

- English: `SEED-OTL-001` — Springer publisher chapter pages explicitly state CC BY 4.0; third-party exceptions may be separately credited and must be checked across the downloaded edition. [Publisher rights example](https://link.springer.com/chapter/10.1007/978-3-030-16877-3_4).
- English: `SEED-OTL-002` — UQ Press confirms CC BY 4.0 **except where otherwise noted**; the book includes text, video and online materials, so every exception/embedded asset must be inventoried. [Publisher title page](https://uq.pressbooks.pub/introduction-social-sciences/).
- Indonesian: `SEED-ID-001` — the ITATS item page declares CC BY 4.0 and a substantive 66-page preview PDF is readable, but separate package handout/material entries are `[DUMMY]`; use only the identified preview file as a candidate and keep the checksum, embedded-content and operation-specific rights review pending. [IoT item](https://itats.ac.id/sisteminformasi/oer/pemrograman-internet-of-things/).
- Indonesian: `SEED-ID-003` — the individual ITATS item page and catalogue show CC BY 4.0 and identify an actual main-PDF item; its 64-page preview is readable, while the separate download action returned 403 in this audit. The original-file checksum and complete embedded-content/use-specific rights review remain pending. [OOP item](https://itats.ac.id/sisteminformasi/oer/pemrograman-berorientasi-objek/).
- `ArgusID.txt` is the owner-created mixed-topic Indonesian TXT corpus. It is available through the Learning screen's bundled-corpus import action for model-training and/or tokenizer-frequency purposes.
- `DigiTransID.zip` is the owner-created Indonesian local corpus and remains available through its own bundled-corpus import action. Citation markers in part 6 are editorial cleanup notes, not import prerequisites.
- Indonesian: `SEED-ID-002` — **HOLD_RIGHTS_CONFLICT**. Its landing page declares CC BY 4.0, but the preview PDF includes a written-permission requirement for reproducing part or all of the work. The two statements are inconsistent; obtain written clarification before any copying, storage, indexing, retrieval or corpus use. [Transformasi Digital item](https://itats.ac.id/sisteminformasi/oer/transformasi-digital/).

For the optional English OTL titles and retained Indonesian public-source candidates, exact original-file edition/bytes, SHA-256, embedded-content exclusions, attribution and source-specific rights remain pending. They remain optional candidates and are not required for the user-imported corpus path. `SEED-ID-002` remains excluded on rights hold and must not be counted as an eligible source. TALPCo is a separate, small aligned-development candidate and is also not use-cleared for Argus. License declarations, permission to fit tokenizer frequencies, permission to generate/distribute a derived vocabulary/tokenizer artifact, raw-source redistribution and neural-model training are distinct issues and must not be conflated.

## 6.4 Device-conditioned vocabulary and preparation-cost method

The analytical comparison must be repeated for each retained model/tokenizer profile rather than choosing one vocabulary size for all devices.

For each candidate profile, record:
- Candidate algorithm and exact pipeline (normalizer, pre-tokenizer, merge/trainer settings, byte fallback, special IDs and decoder).
- Total vocabulary budget and its breakdown (learned subwords, reserved/control IDs, byte fallback and unused IDs).
- Model hidden dimension and whether embeddings/output projection are tied.
- Weight, gradient, optimizer-moment and temporary-output precision/storage rules from the actual candidate training core.
- p50/p90/p99 bytes and Unicode scalars represented per context window, token utilization, truncation and output-logit cost.
- Tokenizer artifact size, load/encode memory, and optional local-training peak memory, wall time, energy and cancellation boundary.
- User-imported tokenizer-frequency manifest with tokenizer frequency fitting selected as an import purpose; if the retained PA-BPE variant requires aligned development, a separate user-imported aligned manifest, each pinned by revision and checksum.

The minimum weight-only storage estimate remains (V \times d \times b) per embedding/output matrix. An untied output matrix adds a second (V \times d) matrix. This is only the weight term: gradient storage, optimizer moments, master weights, logits/workspaces, allocator overhead and runtime memory must be derived from the selected core and precision, not hidden behind a universal multiplier. The full output-logit working set depends on microbatch, sequence length, vocabulary and loss-kernel implementation; record whether the core materializes logits or uses a fused/recomputed path.

Tokenizer local training is a separate preparation workload. Its peak memory/time/energy must fit before model tensor allocation and before Genesis readiness, or a previously approved compatible artifact profile must be selected. Do not assume GPU/NPU acceleration for tokenizer training. The neural model's later optimizer state and training workload are budgeted separately; tokenizer-training cost must not be silently counted as model-training cost or vice versa.

The vocabulary sweep should use at least three points **per architecture/resource envelope**, selected from the calculated embedding/head/training-state and sequence-cost trade-off. Existing examples (8K, 16K, 32K, 65K, 90K) are comparison points only, not fixed device tiers or user-visible defaults. A smaller device must not automatically receive raw bytes or an English-only vocabulary; a larger device must not automatically receive a larger vocabulary. Select only among profiles whose language scope, rights, runtime, model dimensions and resource envelope are all compatible.

### 6.4.1 Deterministic profile-selection procedure — proposed

Use a staged eligibility-and-fit procedure rather than a single weighted score or chipset lookup:

1. **Apply product-language and selected-purpose eligibility first.** Require the imported corpus manifest to record tokenizer frequency fitting as a selected purpose and, only where needed, aligned development as a separate selected purpose. Do not make an Argus-performed source-by-source license investigation a prerequisite for local import. Any later public redistribution/bundling of a derived artifact is a separate gate.
2. **Apply runtime/format eligibility.** Require an available, version-pinned runtime candidate for the exact artifact format, declared pre-tokenizer/normalizer/decoder, special-token inventory and offset contract. Do not choose a different tokenizer family merely because one library is already packaged.
3. **Construct the full candidate bundle.** Bind tokenizer ID/fingerprint, vocabulary budget, model hidden dimension, tied/untied output head, precision, context/packing profile, model architecture and supported execution backends. A tokenizer profile cannot be evaluated independently of its model/head dimensions.
4. **Evaluate Genesis preparation fit.** Calculate tokenizer artifact storage, tokenizer load memory, optional local-training peak memory/time/storage/energy budget, and the memory/storage reserve required for atomic model generation publication. A candidate that exceeds the declared startup-preparation envelope is ineligible.
5. **Evaluate post-Genesis model fit.** Calculate parameter/head, gradient, optimizer, activation/logit/workspace, context and checkpoint-reserve costs under the selected model/training profile. Keep tokenizer preparation and neural-model training peak memory as separate phases unless the implementation overlaps them.
6. **Form the feasible Pareto set.** Remove candidates that are strictly dominated across relevant costs/coverage dimensions. Do not collapse language parity, sequence cost, vocabulary/head memory, artifact size and preparation burden into one unexplained score.
7. **Choose deterministically among feasible profiles.** Apply a versioned tie-break policy declared in the profile registry (for example: preserve required language coverage first; then satisfy the model's target context/sequence envelope; then minimize the maximum resource pressure under explicit safety margins; finally prefer the smaller maintenance/runtime surface). Record rejected alternatives and the reason.
8. **Commit identity before model tensors.** Persist the chosen tokenizer ID, artifact/runtime versions, full pipeline fingerprint and device capability evidence in the Genesis configuration record before allocating vocabulary-dependent tensors.

If local tokenizer preparation fails its time/memory/storage admission or is cancelled, select only a pre-approved precomputed artifact that satisfies the same language, model and runtime contract. Otherwise fail Genesis recoverably. A fallback may change vocabulary/profile only before tensor allocation; it may never silently mutate an active generation.

## 7. Controlled comparison protocol

### 7.1 Candidate matrix

The initial required comparison is:

| Candidate | Base representation | Learned vocabulary | Unknown handling | Role |
|---|---|---|---|---|
| T0 Raw UTF-8 bytes | UTF-8 bytes | No; fixed 256 byte IDs | All byte values representable | Universal-coverage/sequence-cost control |
| T1 Byte-level BPE | Byte alphabet | BPE merges | Byte representation | Main byte-first subword candidate |
| T1-P Parity-aware BPE | Same byte-level BPE inference; fair-max vocabulary learning | BPE merges selected using per-language development-set compression | Byte representation | Conditional candidate for any owner-approved multilingual profile with an explicit language-parity objective; Indonesian/SEA-first positioning remains unapproved and depends on the product-language matrix; requires suitable aligned development text |
| T2 SentencePiece BPE | Raw Unicode characters | BPE pieces | Unknown token or byte-fallback variant | Main Unicode-subword candidate |
| T3 SentencePiece Unigram | Raw Unicode characters | Unigram pieces | Unknown token or byte-fallback variant | Main probabilistic-subword candidate; deterministic inference evaluation |
| T4 WordPiece | Implementation-specific character/subword base | WordPiece vocabulary | Depends on exact configuration | Secondary reference if reproducible |

T2/T3 byte-fallback variants must be listed separately from their no-fallback configurations. They are not separate tokenization families, but they change vocabulary allocation, unknown behavior and sequence length.

### 7.2 Controlled dimensions

For every trainable-vocabulary candidate:
- Compare at least three vocabulary budget points selected from the device/model-state estimate; values are experiment variables, not a selected Argus vocabulary.
- Use the same total vocabulary accounting convention, including special and fallback symbols.
- Use identical tokenizer-training text, source weights, normalization input policy, sampling and held-out evaluation fixtures.
- Record both total IDs and effective learned subword IDs. Byte fallback consumes vocabulary slots and must not be hidden from the comparison.
- Freeze training algorithm/version and all trainer parameters.
- Train independent candidate artifacts rather than mutating one vocabulary in place.
- Report all per-language/domain distributions and runtime measurements.

To isolate factors, compare:
1. BPE versus Unigram under a matched raw-Unicode pipeline where supported.
2. Raw-byte control versus byte-level BPE to measure the effect of learned byte merges.
3. SentencePiece BPE versus byte-level BPE to reveal the combined effect of base units, pre-tokenization and normalization; do not attribute the entire difference to the BPE algorithm alone.
4. Byte fallback on/off for each compatible SentencePiece configuration.
5. NFC, identity and any justified alternative normalization as explicitly separate ablations.

### 7.3 Repetition and uncertainty

- Pin random seeds and deterministic trainer settings where supported.
- Repeat tokenizer training when the trainer or sampling algorithm is nondeterministic; report variance in vocabulary, compression and metric distributions.
- Use a fixed evaluation set not used to build the vocabulary.
- Report confidence intervals or bootstrap intervals for document-level metrics when sample size permits.
- Keep raw per-document/per-language results so that aggregate rankings can be reconstructed.
- Do not cherry-pick one metric, one language or one vocabulary point.

## 8. Runtime, Android and implementation-readiness checks

### 8.1 Static runtime/API audit — 2026-11-05

The version audit below uses the upstream release APIs and pinned source tags checked on 2026-11-05. It identifies candidate surfaces; it does not establish an Argus Android integration, successful ABI build or final dependency selection.

| Runtime path | Verified upstream surface | Android / Argus implication | Current disposition |
|---|---|---|---|
| **SentencePiece C++ runtime** | Apache-2.0 upstream. **v0.2.2 (2026-07-12) is the latest stable release.** Its tagged CMake project sets C++17, contains Android-specific linking for `log`, and defines separate processor (`sentencepiece`) and trainer (`sentencepiece_train`) libraries. The default build uses an Abseil module and bundled protobuf-lite sources. The newer **v0.2.3pre3 (2026-11-04) is explicitly a pre-release** and raises the new release line to C++20. | There is concrete upstream Android-aware CMake support for the stable processor, but this is not an Argus NDK build or a ready-made Kotlin/JNI API. The processor and trainer can be considered separately: an inference-only app need not include tokenizer-training code; local tokenizer preparation would add a separate native library and resource/dependency budget. Pin and audit Abseil/protobuf build inputs. Do not silently rely on pre3-only trainer behavior in a stable v0.2.2 implementation. | **CANDIDATE** — stable C++17 processor path with Android-specific CMake conditions; not adopted or Argus-verified. |
| **SentencePiece Lite** | v0.2.3pre3 contains a standalone experimental C++20 inference runtime over memory-mapped FlatBuffers `.spm.fb` artifacts. The upstream release describes a zero-dependency implementation of approximately 45 KB stripped and under 2,000 lines, with no runtime Protobuf or Abseil dependency. | This is an inference/runtime path, not evidence that Lite supplies tokenizer fitting or the full SentencePiece trainer. It remains pre-release, has no verified Argus ABI/JNI binding, and requires independent artifact-conversion, Android packaging and maturity review. | **EXPERIMENTAL PRE-RELEASE CANDIDATE** — not the default implementation path. |
| **Hugging Face Tokenizers stable Rust core** | The latest stable Rust crate is **v0.23.2** (2026-09-03). v0.23.3 (2026-10-09) is explicitly Python-only; its release says other packages are unchanged. The v0.23.2 crate is Apache-2.0, loads the serialized tokenizer pipeline and exposes native Rust encode/decode. Its Cargo feature set includes optional `parity-aware-bpe`; the default feature set also enables progress UI, Oniguruma and the C++ suffix-array optimization. The tagged repository also contains an Android ARM64 Node N-API package whose entry point is a `.node` addon. | Rust officially lists `aarch64-linux-android` as a Tier 2 target with standard-library support and NDK cross-compilation. The N-API Android package is evidence of a Node-addon packaging path, **not** a Kotlin/JNI binding or a drop-in Android library. Therefore v0.23.2 remains a **stable inference candidate** only behind an Argus-owned Rust C ABI/JNI façade. A custom build can evaluate disabling unneeded default features and network support, but the core crate/dependency graph has not been built or run for Argus. | **STABLE REFERENCE CANDIDATE** — stable HF-format inference core identified; Android Java integration remains a custom, unverified path. |
| **Hugging Face Tokenizers 1.0 RC line** | v1.0.0-rc.3 (2026-10-19) remains explicitly pre-release. Its own notes say feature parity with 0.23 remains a release milestone. It reorganizes the Rust crate into selectable components and records a fix for Hugging Face's own `ParityBpeTrainer` output behavior. | The RC is useful for tracking the trainer/runtime direction, but it is not a stable dependency. The RC3 output fix applies to the Tokenizers crate's built-in `ParityBpeTrainer`; it must not be generalized to a separate third-party training implementation without tracing that implementation's code path. | **PRE-RELEASE RESEARCH CANDIDATE** — not the production baseline. |
| **PA-BPE research trainer** | The MIT-licensed `swiss-ai/parity-aware-bpe` project (package version 0.1.0 at pinned commit `c7395d469a653f3e48e80984e37593256ff8f365`) implements merge selection in its own Python code. It uses Hugging Face Tokenizers pre-tokenizer/model APIs to produce a compatible artifact; it does **not** call the crate's built-in `ParityBpeTrainer`. Its declared pre-tokenizer is `Whitespace` + `ByteLevel(use_regex=false)`. | The CLI distinguishes aligned `--dev` data, ratio-based `--ratio`, initial global merges, and `base` / moving-`window` variants. Its `pyproject.toml` leaves `tokenizers` unpinned and does not declare `transformers`, although `HF_tokenizer.py` imports it. The export helper does not explicitly set a ByteLevel decoder; the serialized artifact must be inspected and round-trip-tested before acceptance. The Python project is an offline research trainer, not an Android runtime. | **RESEARCH CANDIDATE / EXPORT AUDIT OPEN** — separate from HF's built-in ParityBpeTrainer; reproducible dependencies and artifact pipeline remain unverified. |

**Runtime conclusion for G1-D:** two stable, format-specific inference candidates now have identifiable upstream versions: SentencePiece C++ v0.2.2 for SentencePiece artifacts and Hugging Face Tokenizers Rust v0.23.2 for HF-compatible artifacts. This is not a decision to ship both or either. The v0.2.3pre3 Lite and Tokenizers v1.0.0-rc.3 lines remain pre-release. The profile decision must pair each retained tokenizer format with an explicit runtime/API/ABI contract. A PA-BPE/Hugging Face byte-level artifact must preserve its declared Whitespace + ByteLevel(use_regex=false) pipeline; it must not be silently loaded as a SentencePiece model. SentencePiece BPE/Unigram artifacts and HF-compatible byte-level BPE artifacts are not interchangeable merely because they both use BPE terminology.

**Scope of the Hugging Face trainer fix:** the v0.23.2 source contains its own `ParityBpeTrainer` behind the `parity-aware-bpe` feature, and v1.0.0-rc.3 records a fix for that implementation's output difference from the ordinary BPE trainer. The pinned `swiss-ai/parity-aware-bpe` project is a separate Python implementation of parity merge selection and does not call this built-in trainer. The RC3 fix therefore does not, by itself, invalidate or validate the external trainer. That external route has its own unresolved exporter checks: `tokenizers` is unpinned, `transformers` is imported but not declared in `pyproject.toml`, and the helper does not explicitly configure a ByteLevel decoder. Verify the resulting serialized artifact's model/vocabulary/merge consistency, decoder, special-token IDs, deterministic load and exact encode/decode behavior before selecting the PA-BPE training path.

### 8.2 Candidate Android runtime contract — proposed, not selected

The evidence supports a **format-matched runtime portfolio**, not one universal tokenizer engine:

| Artifact family | Preferred research path | Rationale | Limit / unresolved evidence |
|---|---|---|---|
| Hugging Face-compatible byte-level BPE, including PA-BPE output | Stable Hugging Face Tokenizers Rust crate v0.23.2 behind an Argus-owned C ABI/JNI façade | Preserves the upstream tokenizer JSON pipeline and avoids creating a second independent BPE/pre-tokenizer implementation. Rust officially supports Android targets such as `aarch64-linux-android` with the standard library and NDK cross-compilation. | The upstream Android ARM64 package is a Node N-API `.node` addon, not a Java/JNI library. The core crate and full dependency graph have not been built for Argus Android ABIs. Rust Tier 2 target availability is not proof that this crate builds, packages or runs correctly for Argus. PA-BPE training/export from the v0.23.2 trainer remains subject to the output-validation caveat in section 8.1. |
| SentencePiece BPE or Unigram | Stable SentencePiece C++ v0.2.2 behind an Argus-owned JNI façade | Direct native processor path for SentencePiece `.model` artifacts; its upstream tagged CMake source has an explicit `ANDROID` branch and separates processor from trainer libraries. | No official Kotlin binding, Argus JNI layer or Argus ABI build is verified. Build/dependency inputs (Abseil and protobuf-lite) need pinning. Keep the experimental v0.2.3pre3 Lite path outside the primary path until its C++20/pre-release maturity and `.spm.fb` artifact conversion are independently justified. |
| Raw UTF-8 byte control | Deterministic byte-to-ID mapping through the shared logical interface | The byte-to-ID mapping is algorithmic and can serve as a portable reference/control without a learned vocabulary artifact. | It does not remove model-level sequence cost. Special-token, normalization and malformed-UTF-8 behavior must still be explicitly specified. |

**Common logical API contract.** Regardless of native engine, the Kotlin-facing interface should expose versioned operations and metadata:

1. `open(artifactDescriptor, expectedFingerprint, runtimeConfig)` — load an immutable artifact from an app-private path or bounded file descriptor/mapped region; validate its format, schema and fingerprint before use.
2. `encode(utf8Input, options)` — return token IDs, optional source spans and explicit truncation/normalization status. Native input is UTF-8; Kotlin UTF-16 offsets must never be silently substituted for UTF-8 byte offsets.
3. `decode(tokenIds, options)` — return UTF-8 bytes/text and an explicit invalid-ID policy. Never silently replace errors with another tokenizer's unknown-token behavior.
4. `getMetadata()` — return tokenizer ID/schema, vocabulary size, special-token IDs, algorithm, normalizer/pre-tokenizer/decoder fingerprint, offset unit, runtime identifier/version and artifact digest.
5. `close()` — release the handle and native allocations deterministically.

**ABI and lifecycle requirements.** Use opaque native handles and fixed-width integer buffers at the FFI boundary. Never expose Rust/C++ object layouts, STL containers, Rust references or allocator ownership across JNI/C ABI. Define one owner for every input/output buffer and its release path. Batch document calls rather than crossing JNI per token. Native failures must map to typed Kotlin errors (format mismatch, fingerprint mismatch, invalid input, invalid token ID, allocation failure, unsupported feature and internal runtime failure). The runtime must be thread-safe or explicitly serialize calls per handle; cancellation must be cooperative at bounded document/batch boundaries. Process restart must reopen the immutable artifact and verify the same fingerprint.

**Selection rule.** Select runtime by artifact format, not SoC brand. Device capability may choose among pre-approved artifact/model profiles, but may not switch engines in a way that changes token IDs or tokenization semantics. If a profile runtime is unavailable or fails compatibility checks, fail before model tensor allocation or select an already approved profile with its own complete artifact/runtime contract. Never fall back from SentencePiece to byte-level BPE (or vice versa) under one generation identity.

**Status:** this is a documentary interface proposal and a preferred research path. It is not an adopted dependency, compiled Android library, verified ABI matrix, or G1-V conformance result.


The upstream SentencePiece Lite documentation describes its runtime as a standalone C++ library and says adoption as the core SentencePiece engine is planned. Treat that wording and the v0.2.3pre3 pre-release state as maturity constraints, not as a guarantee of stable ABI or first-party Android support. Its Lite runtime is an inference path over `.spm.fb` artifacts; it must not be confused with the separate full SentencePiece trainer or used as proof that tokenizer fitting is available through Lite. The stable v0.2.2 CMake source's Android branch and separate processor/trainer targets are useful integration evidence, but Argus still needs its own native build, JNI façade and dependency pinning. Hugging Face's Rust crate is a candidate custom integration, not an official Android binding; the published Android ARM64 Node package is a N-API addon, not a Kotlin/JNI API. aarch64 Android Tier 2 target support alone does not prove the complete crate dependency graph works on Argus. No upstream speed, binary-size or memory claim in this subsection is an Argus Android benchmark.
Tokenizer training and tokenizer inference are different workloads. A tokenizer can be practical to train offline yet unsuitable for repeated Android inference, or vice versa.

For each deployment candidate, assess:
- Android API 33+ compatibility and ABI coverage for the existing application support floor.
- Pure Kotlin/JVM, native C++ or other runtime options only as research alternatives; no implementation language is chosen here.
- JNI/native dependency complexity, package size, licensing and maintenance status.
- Cold-start load time, tokenizer initialization allocations and repeated encode/decode latency.
- Thread-safety, instance lifetime, concurrent calls and cancellation behavior.
- Input-size limits and graceful handling of large documents or malformed input.
- Unicode offset consistency across Kotlin UTF-16 and native UTF-8 representations.
- Stable serialization/loading of the tokenizer artifact, vocabulary, merge ranks and normalization rules.
- Exact equivalence between the reference research implementation and Android implementation on the full conformance fixture.
- Clear failure rather than silent fallback to a different tokenizer or unknown token policy.

A library's availability or benchmark speed is not proof of compatibility with the Argus toolchain, Android ABI, license or future training runtime. Record exact versions and test evidence before adopting a dependency.

## 9. Tokenizer artifact lifecycle and distribution policy

Three lifecycle strategies remain open:

### Option A — Precomputed, versioned tokenizer profile portfolio

Train tokenizer artifacts outside the app from rights-cleared, versioned corpora and distribute a small portfolio of compatible artifacts/configurations. Genesis selects one profile only after evaluating the device capability envelope and the product language/domain policy.

Potential advantages:
- Several reviewed vocabulary budgets and pipeline variants can serve different model/resource envelopes without forcing one global vocabulary.
- A selected artifact is ready before Genesis allocates vocabulary-dependent tensors.
- Installations selecting the same profile can share the same immutable tokenizer artifact and fingerprint while retaining independent neural weights.
- The first-launch path avoids local tokenizer-training cost.

Risks/open questions:
- The profile portfolio must be maintained, versioned and supported; it must not become an uncontrolled matrix of hardware-specific builds.
- Every artifact needs its own provenance, license, attribution and reproducible training record.
- A profile may not match a particular installation's later knowledge distribution.
- Changing profile or token mapping requires a new model-generation contract and migration strategy.

### Option B — Capability-bounded local tokenizer training

An installation may train its own tokenizer from an approved, locally available seed corpus when its measured capability profile can satisfy the preparation budget. Local training is an optional profile, not a requirement that every phone produce a unique vocabulary.

Potential advantages:
- Vocabulary and merge behavior can be selected for the declared language/domain scope and the chosen device/model resource envelope.
- Vocabulary budget can be evaluated together with embedding/output-head, optimizer-state and sequence costs.
- No single centrally distributed vocabulary is required for installations using this mode.

Risks/open questions:
- Tokenizer training consumes CPU time, memory, storage and energy; it must be separately budgeted and measured. Do not assume GPU/NPU acceleration.
- It must finish before vocabulary-dependent model tensors are initialized.
- The training seed corpus must be local/offline or fetched through a separately authorized flow and legally eligible for tokenizer training and artifact creation.
- Different installations may produce different token IDs. That is acceptable for independent local generations but prevents direct interchange of their tokenized datasets/checkpoints/model weights.
- A deterministic fallback profile must be selected before model construction if local preparation exceeds its declared budget.
- Personal conversations and private captures are excluded unless a distinct, explicit consent and governance contract authorizes their use.

### Option C — Algorithmic byte-level tokenizer

Use a fixed byte mapping without a learned merge table as a universal-coverage control or as a deployment candidate only where the selected model/resource envelope supports its sequence cost.

Potential advantages:
- Full byte coverage and predictable base alphabet.
- No learned tokenizer vocabulary training is required.
- Stable algorithmic behavior when fully specified.

Risks/open questions:
- Raw bytes can increase sequence length, context consumption and training compute; the ByT5 paper's results are workload-specific and are not Argus estimates.
- It may be infeasible for a device/model envelope that cannot afford the longer sequences.
- Runtime, context and training-cost results must be compared on the same eligible fixture.

**Device-conditioned lifecycle policy — proposed, not yet an accepted ADR:** Argus should define a bounded tokenizer profile portfolio and a deterministic selection policy, not declare one vocabulary universally optimal. Product-language requirements and source rights first define the allowed data and required coverage. The capability profile then selects among the eligible profiles by accounting for tokenizer preparation cost, sequence compression, vocabulary/head/optimizer memory, context coverage and runtime support. Depending on the profile, Genesis may load a precomputed artifact or run bounded local tokenizer training. A high-end phone is not automatically assigned a larger vocabulary, and a low-end phone is not automatically assigned raw bytes; both choices require evidence and a compatible model configuration.

Tokenizer preparation is a prerequisite to model construction, not neural-model learning. It must not turn first launch into an unbounded training screen. If local tokenizer training cannot complete within its declared Genesis envelope, use only a pre-approved compatible precomputed/algorithmic profile selected before model tensor allocation, or fail with a recoverable reason. Do not silently change token IDs after generation creation.

This is a proposed selection policy, not an accepted tokenizer ADR. Exact language scope, candidate profiles, vocabulary budgets, rights, trainer/runtime and Android conformance remain G1-D/G4/G1-V decisions.

## 10. Versioning, identity, checkpoints and migration

Every tokenizer artifact must have:
- Stable tokenizer ID and schema version.
- Cryptographic digest of the exact model/config/vocabulary/merge assets.
- Algorithm, trainer and runtime versions.
- Normalizer, pre-tokenizer, whitespace and decoder fingerprints.
- Vocabulary size and complete token-ID mapping.
- Special-token inventory and IDs.
- Byte-fallback configuration and unknown-token semantics.
- Offset/alignment convention.
- Source corpus manifest, revision, preprocessing and license/provenance record for any learned vocabulary.

The tokenizer fingerprint is part of:
- Genesis generation manifest.
- Model configuration and model-state digest.
- Training session configuration.
- Dataset/tokenized-shard manifest.
- Checkpoint compatibility metadata.
- Inference and evaluation reports.

A mismatch must fail closed. Never load cached token IDs, resume a checkpoint or promote a candidate under a different tokenizer fingerprint.

Changing algorithm, vocabulary, ID assignment, special-token mapping, normalization, pre-tokenizer or decoder behavior requires a new version and explicit migration. If a token ID's meaning changes, embedding/output rows cannot be assumed compatible. The migration must define how tensors are mapped or rebuilt, how new IDs are initialized, how old IDs are handled, how optimizer state is migrated or reset, and how the candidate is validated. No in-place vocabulary mutation is permitted for an active generation.

## 11. Decision gates and unresolved questions

### G1-D — Documentary tokenizer shortlist/design gate

G1-D may be completed from published research, official documentation, audited runtime/API evidence, the user-imported corpus contract and analytical accounting. Source/license evidence remains relevant to optional external libraries or optional public-source datasets, not as an Argus-run per-article review requirement for a user import. It does **not** require Argus application tests, physical Android benchmarks, or new downstream model training. It may close when:

1. The initial product-language scope and explicit unsupported-language/fallback policy are documented.
2. An exact, revision-pinned user-imported tokenizer-frequency corpus is available with tokenizer frequency fitting selected as an import purpose. If the retained PA-BPE variant uses aligned development, a separate user-imported aligned manifest is available with that purpose selected in its import manifest; otherwise compare a valid no-development-data variant. Evaluation-only records are excluded from frequency fitting, `--dev` merge selection and ratio calibration.
3. The evidence matrix compares raw-byte, standard byte-level BPE, PA-BPE (including hybrid/no-dev variants where relevant), SentencePiece BPE and SentencePiece Unigram within their actual evaluation scope.
4. The provisional candidate shortlist and rationale account for per-language/domain quality evidence, compression, vocabulary/model-state costs and data availability.
5. A device-conditioned tokenizer profile policy is documented: profile selection inputs, candidate vocabulary budgets, precomputed versus local-training modes, bounded preparation/fallback behavior, and the rule that the chosen fingerprint is fixed before model tensor allocation.
6. The exact pre-tokenization/normalization contract, special-token policy, versioning and generation-migration boundary are defined for each retained profile.
7. A plausible Android inference runtime has been identified from current public APIs/source/licensing, with unsupported capabilities and provider limits explicitly recorded.
8. The ADR states which conclusions are research-backed, analytically established, implementation-dependent and still empirically unverified, including the fact that physical per-device support remains a post-implementation claim.

G1-D does not claim exact Argus corpus fertility results, downstream quality, Android runtime conformance or device performance.

### G1-V — Post-implementation tokenizer validation

After tokenizer integration is separately authorized, verify:

1. Deterministic encode/decode, no silent corruption, and the exact declared round-trip/normalization behavior.
2. Unicode, malformed input, whitespace, offset/alignment, byte fallback and special-token injection boundaries.
3. Exact token-ID parity between the frozen reference implementation and the Android runtime for the entire conformance fixture.
4. Stable fingerprint/artifact load, thread safety, cancellation and large-input failure behavior.
5. Cold/warm latency, peak allocations, artifact size and memory on named device tiers.
6. Candidate-language/domain sequence-cost distributions on the approved Argus fixture.
7. Downstream bits-per-byte and task-level evaluation when the corresponding architecture/training path is implemented, using matched data/compute and held-out data.
8. Compatibility between tokenizer fingerprint, model generation, dataset shards and checkpoint recovery.

A failed G1-V item blocks the affected implementation/runtime support claim; it does not retroactively invalidate completion of the documentary research phase.

No candidate may be selected solely because it has:
- The lowest mean token count.
- The highest fertility or bytes-per-token score.
- The smallest tokenizer file.
- The best English-only score.
- A fashionable algorithm or a high profile public deployment.
- A favorable result in a large-model study without Argus-scale validation.

Use a Pareto comparison rather than an opaque one-number weighted score. Hard correctness and security failures are non-compensatory. For optional external sources or redistribution, their operation-specific rights constraints remain hard gates; local user imports use the workflow purposes selected in the manifest. Among eligible candidates, present trade-offs across sequence cost, vocabulary/model cost, language/domain parity, downstream probe quality and Android runtime behavior.

Open decisions:
1. Required product languages and relative language/domain sampling weights.
2. Exact user-imported tokenizer-training corpus revision and tokenizer frequency fitting selected as an import purpose; aligned development input only if required by the selected algorithm.
3. The bounded device-conditioned policy for selecting among precomputed tokenizer profiles and optional per-installation local training.
4. Whether raw bytes remain only a control or are viable as a deployment path.
5. Which subword families and vocabulary budget points remain in the final sweep.
6. Normalization and whitespace policy, including compatibility behavior.
7. Exact special-token inventory and structured message serialization.
8. Required offset semantics and cross-runtime behavior.
9. Android tokenizer implementation/runtime and dependency/license choice.
10. Migration policy for tokenizer revisions and vocabulary changes.

The **documentary shortlist (G1-D)** can close using published research, official documentation, audited runtime/API evidence and analytical accounting without downstream model training or Android-device results. The product-language matrix and format-matched runtime/API/ABI contract remain owner/design decisions. The exact tokenizer-frequency input is supplied through the user-controlled local import path in document 22, with tokenizer frequency fitting selected as its import purpose; an aligned development manifest is needed only if the retained algorithm variant requires it. The external-source candidates and detailed source reviews below remain optional references, not blockers for a user-provided local corpus. Missing license metadata is recorded as unknown, not as public-domain permission; local import does not authorize external redistribution. Android conformance and downstream model-quality checks remain G1-V obligations after implementation. G4 still requires its own profile/resource and architecture ADR.

## 12. Relationship to the wider Argus research plan

- [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md) defines the device/resource and E1–E7 gates. Tokenizer work does not establish broad from-scratch feasibility.
- [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md) uses the exact tokenizer profile selected for each model configuration as a versioned input contract for controlled architecture comparisons.
- [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md) defines component-level research and identifies decisions blocked by tokenizer G1.
- [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md) defines user-imported local corpus input and selected workflow purposess. Documents 27–32 contain optional public-source research and do not gate the primary tokenizer-data path.
- [20 — Argus Genesis Engine](20_ARGUS_GENESIS_ENGINE.md) requires the tokenizer identity before the initial model generation is committed.
- [19 — P0 Remediation Design](19_P0_REMEDIATION_DESIGN.md) binds checkpoints and candidates to exact tokenizer fingerprints.
- [14 — Performance, Power and Thermal Engineering](14_PERFORMANCE_POWER_THERMAL.md) and [15 — Verification and Compatibility](15_VERIFICATION_COMPATIBILITY.md) define device measurement and verification.

## 13. Research references

| Evidence ID | Primary source | Relevance and limitation |
|---|---|---|
| E-015 | [SentencePiece: A simple and language independent subword tokenizer and detokenizer for Neural Text Processing](https://aclanthology.org/D18-2012/), Kudo and Richardson, EMNLP 2018 | Raw-sentence subword training; BPE and Unigram implementations; does not select an Argus normalization or vocabulary |
| E-016 | [Neural Machine Translation of Rare Words with Subword Units](https://aclanthology.org/P16-1162/), Sennrich, Haddow and Birch, ACL 2016 | BPE subword segmentation for open-vocabulary handling; original task is NMT |
| E-017 | [Subword Regularization: Improving Neural Network Translation Models with Multiple Subword Candidates](https://aclanthology.org/P18-1007/), Kudo, ACL 2018 | Unigram segmentation sampling as a training regularizer; not a reason to make benchmark tokenization stochastic |
| E-018 | [ByT5: Towards a Token-Free Future with Pre-trained Byte-to-Byte Models](https://doi.org/10.1162/tacl_a_00461), Xue et al., TACL 2022 | Raw UTF-8 byte modeling and its quality/compute/latency trade-offs; T5-specific results are not Android measurements |
| E-019 | [Tokenizer Choice For LLM Training: Negligible or Crucial?](https://aclanthology.org/2024.findings-naacl.247/), Ali et al., Findings of NAACL 2024 | Controlled 2.6B-scale study; tokenization affects performance/cost, fertility/parity are not sufficient proxies |
| E-020 | [TokLens: A Multilingual Lens on Tokenizer Quality for LLMs](https://aclanthology.org/2026.acl-srw.18/), Chiu, ACL 2026 Student Research Workshop | Six intrinsic multilingual metrics across 24 tokenizers/15 languages; reported correlations remain confounded and are not causal selection rules |
| E-008 | [Equity with Efficiency: An Empirical Study of Tokenizers for Multilingual Large Language Models](https://aclanthology.org/2026.emnlp-main.1783/), Wei et al., EMNLP 2026 | Eleven Southeast Asian languages and controlled 1.5B model comparisons; parity-aware/morphology-driven candidates are research leads, not direct mobile results |
| E-022 | [Subword Tokenization for Low- and Medium-Resource Languages: A Systematic Evaluation](https://ijcai.org/proceedings/2026/633), Daðason and Loftsson, IJCAI 2026 | WordPiece/BPE/Unigram and vocabulary sweeps across six languages; findings vary by language/task and are not an Indonesian result |
| E-023 | [SentencePiece training options](https://google.github.io/sentencepiece/doc/options/) and [byte fallback behavior](https://github.com/google/sentencepiece/blob/master/doc/options.md) | Exact normalization, whitespace, special-token and byte-fallback controls; library defaults must be pinned |
| E-024 | [Hugging Face Tokenizers components](https://huggingface.co/docs/tokenizers/main/en/components) | Normalizer, pre-tokenizer, byte-level alphabet and decoder pipeline concepts; API/version support must be checked before runtime selection |
| E-025 | [tiktoken BPE design notes](https://github.com/openai/tiktoken/blob/main/README.md) | Byte-pair encoding reversibility/open-text properties as implementation reference; not a dependency selection |
| E-026 | [Parity-Aware Byte-Pair Encoding: Improving Cross-lingual Fairness in Tokenization](https://aclanthology.org/2026.acl-long.342/), Foroutan et al., ACL 2026 | Fair-max language-specific merge selection; aligned development corpus can be small and separate from language-labelled pair-count corpus; base, hybrid and moving-window variants; 60-language fairness result. Requires careful normalization/alignment and does not choose Argus language coverage |
| E-027 | [Parity-aware BPE reference implementation](https://github.com/swiss-ai/parity-aware-bpe), reviewed 2026-11-04 | Public MIT-licensed research implementation; exports vocabulary/merge artifacts with a Hugging Face Tokenizers pre-tokenizer recipe. Python training code is not an Android runtime or license audit of transitive dependencies |
| E-028 | [Equity with Efficiency: An Empirical Study of Tokenizers for Multilingual Large Language Models](https://aclanthology.org/2026.emnlp-main.1783/), Wei et al., EMNLP 2026 | Eleven SEA languages including Indonesian; controlled 1.5B decoder-only models from scratch; PA-BPE improves cost parity, but downstream winners vary by task and server-GPU results do not transfer to Android |
| E-029 | [Language-Specific Effects of Tokenizer Choice in Multilingual Language Models](https://arxiv.org/abs/2610.12144), Meister et al., 2026 preprint | 123 controlled models/54 tokenizers; sensitivity differs by language/training share; naive tokenizer-data upweighting is not a universal fix; metric screening is promising but not a causal quality guarantee |
| E-030 | [TokSuite](https://proceedings.mlr.press/v306/altintas26a.html), Altıntaş et al., ICML 2026 | Fourteen controlled tokenizer variants with same model/data/budget/initialization and perturbation benchmark; methodological reference, but language coverage does not include Indonesian |
| E-031 | [FLORES+ dataset card](https://huggingface.co/datasets/openlanguagedata/flores_plus), current v5.0 | CC BY-SA 4.0 and gated terms; intended for evaluation and explicitly not training data. Do not use as PA-BPE merge-selection/development data without permission for that purpose |
| E-032 | [TReX: Tokenizer Regression for Optimal Data Mixture](https://aclanthology.org/2026.eacl-long.298/), Won et al., EACL 2026 | Small proxy tokenizers plus regression can screen language-mixture choices; reported up to 12% compression-efficiency gain versus selected heuristic baselines. Compression-only result; not direct quality or Argus corpus evidence |

| E-033 | [NusaCrowd: Open Source Initiative for Indonesian NLP Resources](https://aclanthology.org/2023.findings-acl.868/), dataset inventory appendix; [TALPCo upstream](https://github.com/matbahasa/TALPCo), pinned 2023 snapshot reviewed 2026-11-04 | The primary upstream source declares CC BY 4.0. Commit `eb4746249830e2c0a8b192a464a74616da3e0453` contains 1,372 common sentence IDs across English, Indonesian and Japanese raw files, plus a separate 1,372-row partial Indonesian–Japanese token-alignment file | The source snapshot is now located and structurally counted, but is a small aligned-development candidate only—not a frequency corpus or representative multilingual benchmark. Use-specific rights, attribution, exact byte checksums and tokenizer-artifact obligations remain open |
| E-034 | [TICO-19 project](https://tico-19.github.io/testset.html) and [public dataset card](https://huggingface.co/datasets/gmnlp/tico19), reviewed 2026-11-04 | Parallel COVID-19 evaluation set with Indonesian and Malay, dev/test partitions and per-row `sourceURL`/`license` fields. The dataset card states CC0 translations, while the live viewer exposes four distinct row-level license values | Card-level and row-level metadata are not reconciled. Keep the test split held out; do not use any dev rows for merge selection until each source and license is reconciled with the original terms. Narrow COVID-19 domain limits representativeness |
| E-035 | [Tatoeba official export index](https://downloads.tatoeba.org/exports/), [Indonesian per-language export index](https://downloads.tatoeba.org/exports/per_language/ind/), [text reuse/attribution guidance](https://en.wiki.tatoeba.org/articles/show/using-the-tatoeba-corpus), and [CC0 contribution rules](https://en.wiki.tatoeba.org/articles/show/cc0-contributions), reviewed 2026-11-05 | The 2026-10-31 export snapshot lists Indonesian sentence, base-lineage, detailed and translation-link files; `ind-eng_links.tsv.bz2` is 162,561 bytes. The global export contains `sentences_CC0.csv`, but the Indonesian directory does not list a separate `ind_sentences_CC0.tsv.bz2`. Tatoeba's default text license is CC BY 2.0 FR; CC0 is limited to contributor-created original sentences, not translations. | Conditional aligned-development candidate, not a frequency-corpus approval. A missing per-language CC0 file does not prove there are zero Indonesian CC0 rows; the global CC0 export must be filtered by language and pinned. Preserve sentence IDs, language, author/attribution lineage, translation links, exact snapshot and checksums. CC BY material needs its required attribution; do not treat bilingual pairs or translations as blanket CC0. Short-sentence, contributor and conversational-domain biases remain material. Audio is outside this text-only review. |
| E-036 | [UN Parallel Corpus v1.0](https://www.un.org/dgacm/en/content/uncorpus), reviewed 2026-11-04 | Public-domain official UN records with sentence alignments across six UN official languages | No Indonesian/SEA coverage and a narrow formal diplomatic/parliamentary domain |
| E-037 | [CCAligned](https://statmt.org/cc-aligned/) and [Neural ParaCrawl](https://www2.statmt.org/neural-paracrawl/), reviewed 2026-11-04 | Large English-aligned web-derived corpora include Asian language pairs; preparation corpus makes no claim to ownership of source documents | Underlying pages retain original rights/terms; not rights-cleared by virtue of the corpus pipeline |
| E-038 | [SentencePiece v0.2.3pre3 release](https://github.com/google/sentencepiece/releases/tag/v0.2.3pre3), [stable v0.2.2 release](https://github.com/google/sentencepiece/releases/tag/v0.2.2), [v0.2.2 CMake configuration](https://github.com/google/sentencepiece/blob/v0.2.2/CMakeLists.txt), [v0.2.2 native library targets](https://github.com/google/sentencepiece/blob/v0.2.2/src/CMakeLists.txt) and [SentencePiece Lite runtime documentation](https://google.github.io/sentencepiece/lite/), reviewed 2026-11-05 | Stable v0.2.2 sets C++17, includes an Android-specific `log` link path, and separates processor and trainer libraries; v0.2.3pre3 (published 2026-11-04) is a C++20 pre-release with Lite inference over memory-mapped FlatBuffers `.spm.fb` artifacts and separate trainer/vocabulary-selection changes | Upstream Android-aware CMake is not an Argus JNI integration. The processor and optional trainer have separate binary/dependency costs; pin Abseil/protobuf inputs. Lite remains experimental and is not a tokenizer-training interface |
| E-039 | [Hugging Face Tokenizers releases](https://github.com/huggingface/tokenizers/releases), [stable Rust crate v0.23.2](https://docs.rs/crate/tokenizers/0.23.2), [crate feature metadata](https://docs.rs/crate/tokenizers/0.23.2/features), [Android ARM64 Node package manifest](https://github.com/huggingface/tokenizers/blob/v0.23.2/bindings/node/npm/android-arm64/package.json), and [Rust Android target support](https://doc.rust-lang.org/rustc/platform-support/android.html), reviewed 2026-11-05 | Stable Rust core v0.23.2; v0.23.3 is Python-only; v1.0.0-rc.3 is pre-release. The repository's Android ARM64 package is a Node N-API `.node` addon, not a Kotlin/JNI library. `aarch64-linux-android` is an official Tier 2 Rust target with std and NDK cross-compilation support | Stable HF-format inference is a credible custom Rust/C ABI/JNI candidate, not an official Android binding. The crate/dependency graph remains unbuilt and unverified for Argus; Tier 2 and Node N-API packaging are not runtime proof for the Argus Android app |
| E-040 | [Parity-aware BPE reference implementation](https://github.com/swiss-ai/parity-aware-bpe), pinned commit `c7395d469a653f3e48e80984e37593256ff8f365`, [project metadata](https://github.com/swiss-ai/parity-aware-bpe/blob/c7395d469a653f3e48e80984e37593256ff8f365/pyproject.toml), [merge-selection implementation](https://github.com/swiss-ai/parity-aware-bpe/blob/c7395d469a653f3e48e80984e37593256ff8f365/parity_aware_bpe/parity_aware_learn_bpe.py), and [HF artifact exporter](https://github.com/swiss-ai/parity-aware-bpe/blob/c7395d469a653f3e48e80984e37593256ff8f365/parity_aware_bpe/HF_tokenizer.py), reviewed 2026-11-05 | MIT-licensed Python trainer, package version 0.1.0; custom merge-selection loop uses Whitespace followed by ByteLevel(use_regex=false), then builds a standard HF BPE model from base alphabet and merges | Separate from Hugging Face's built-in ParityBpeTrainer. The package leaves `tokenizers` unpinned, omits the `transformers` dependency used by its exporter, and does not explicitly set a ByteLevel decoder; exact artifact reproducibility, special-token contract and byte-exact decode remain unverified |
| E-042 | [Hugging Face Tokenizers v0.23.2 tagged ParityBpeTrainer source](https://github.com/huggingface/tokenizers/blob/v0.23.2/tokenizers/src/models/bpe/parity_trainer.rs), [v0.23.2 feature metadata](https://docs.rs/crate/tokenizers/0.23.2/features), and [v1.0.0-rc.3 release notes](https://github.com/huggingface/tokenizers/releases/tag/v1.0.0-rc.3), reviewed 2026-11-05 | Stable v0.23.2 includes its own `ParityBpeTrainer` behind the `parity-aware-bpe` feature. RC3 records an output-consistency fix for this built-in trainer | This finding applies only when using Hugging Face's built-in trainer; it does not apply automatically to the separate `swiss-ai/parity-aware-bpe` Python merge-selection implementation. Treat these two trainer paths as distinct |
| E-043 | [HPLT Monolingual Datasets v3.0](https://hplt-project.org/datasets/v3.0), [official Indonesian shard map](https://data.hplt-project.org/three/sorted/ind_Latn.map), [per-shard MD5 manifest](https://data.hplt-project.org/three/sorted/ind_Latn.md5), [HPLT v4.0 pool page](https://hplt-project.org/datasets/v4.0), [HPLT TextPipes pinned repository commit](https://github.com/hplt-project/HPLT-textpipes/tree/4bbfb80ed0c4b83c47860872904b75cfffacbd2f), and [Monotextor pinned pipeline commit](https://github.com/hplt-project/monotextor-slurm/tree/57c92953b0b62fb99d3068ba1a6a67e3a1423919), reviewed 2026-11-05 | HPLT v3 `ind_Latn`: 256.15 GB, 176.11M documents, 142.12B Gemma-3-vocabulary tokens, 610.52B characters, 3.54B segments; 15 official Zstd JSONL shards with corresponding MD5 entries. Schema includes source URL and WARC/crawl lineage, WDS/register metadata, language labels and PII offsets. HPLT states CC0 covers packaging only, not extracted source text. HPLT v4 is a 440-TB/194-language raw pool, not yet a ready-to-use language-engineering dataset | High-value Indonesian source-discovery lead only. Source-level content rights remain unresolved; PII is annotated, not removed by the documented cleaning criteria. Require bounded exact shard/document manifest, original-source rights/terms, tokenizer-fitting permission and PII controls before use; do not equate package CC0 with text rights or neural-training approval |
| E-041 | [BAHANA parallel corpus](https://mozilladatacollective.com/datasets/cmuh552z504p4ny0793z6j6a7), reviewed 2026-11-04 | Indonesian–English–Arabic, CC BY-SA 4.0, 5,174 aligned sentence sets; data owner requires permission for commercial derivative products | Conditional source only; written permission and ShareAlike/attribution review required |



**Current research shortlist:** retain raw bytes as a coverage/sequence-cost control, standard byte-level BPE as the baseline, SentencePiece BPE/Unigram as alternative subword/runtime families, and Parity-aware BPE as a conditional candidate where the owner-approved language portfolio has an explicit parity objective. Indonesian/SEA-first positioning is not an accepted product decision. G1-D remains open for the product-language matrix, an exact user-imported tokenizer-frequency corpus with tokenizer frequency fitting selected as an import purpose, aligned development data only if required by the selected PA-BPE variant, and the runtime/API/ABI contract. External-source license research is not a prerequisite for user-provided local data. G4 must approve a bounded device-conditioned tokenizer profile portfolio and deterministic selection/fallback policy; each local model generation binds to one immutable tokenizer ID and fingerprint. Android conformance and downstream model-quality checks remain G1-V obligations after implementation. Tokenizer-neutral architecture research proceeds in parallel.
