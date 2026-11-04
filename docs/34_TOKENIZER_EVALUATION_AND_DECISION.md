# Tokenizer Evaluation and Decision Framework

**Status:** Research protocol; tokenizer decision remains OPEN  
**Last reviewed:** 2026-11-04  
**Phase:** 4.1–4.2 — Tokenizer evaluation and comparative research  
**Authority:** Candidate definitions, evaluation controls and decision gates. No tokenizer, vocabulary size, normalization profile, packaging policy or runtime is selected by this document.

## 1. Purpose and decision boundary

Argus must choose a tokenizer before its model generation is initialized because the tokenizer's vocabulary and token-ID mapping define the input embedding and output projection dimensions. The tokenizer also influences sequence length, context use, training compute, inference cost, multilingual coverage and model-state compatibility.

This document establishes a controlled method to compare tokenizer candidates. It does not select a winner.

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
- The use of a precomputed vocabulary does not, by itself, introduce pretrained neural weights. It is nevertheless a data-derived asset whose training corpus, license, provenance, reproducibility and distribution terms must be reviewed.
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

- Use only exact, versioned samples with documented permission for the intended evaluation.
- The final representative fixture must be derived from source records accepted through the corpus/provenance process in [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md) and documents 27–32.
- A source listed as a discovery or retrieval candidate is not automatically approved for tokenizer training or redistribution.
- Synthetic Unicode and format test cases may be generated for deterministic correctness tests; they must be labeled synthetic and must not be counted as natural-language training data.
- Do not include personal conversations, private files or user-specific content in tokenizer training or evaluation without the separate consent and data-governance contract.
- Keep the tokenizer-training sample, intrinsic evaluation sample and any downstream model evaluation split separate. Avoid exact-text and near-duplicate leakage.

The corpus phase has not yet finalized all source rights or composition. Therefore any early fixture is provisional and may support technical diagnostics only. Final language/domain weighting and vocabulary selection must be revisited after corpus provenance and composition are approved.

### 4.3 Balancing and sampling

- Freeze document IDs, checksums, language labels, domain labels, sampling method and exact split membership.
- Report both per-language/per-domain results and an aggregate using a declared weighting scheme.
- Do not let a large English subset dominate the result merely because it has more bytes or documents.
- For algorithm comparisons, use the same text and same number of input bytes for each candidate.
- For tokenizer training, use the same allowed training split and a declared sampling policy; never train one candidate on more or different data without identifying that as a separate experiment.
- Report the raw-byte and Unicode-scalar counts so that sequence-length and compression comparisons can be normalized.
- Keep duplicated content across languages, domains and splits under control.

No fixed language proportions, corpus token quota or universal corpus size is approved by this document.

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

## 7. Controlled comparison protocol

### 7.1 Candidate matrix

The initial required comparison is:

| Candidate | Base representation | Learned vocabulary | Unknown handling | Role |
|---|---|---|---|---|
| T0 Raw UTF-8 bytes | UTF-8 bytes | No; fixed 256 byte IDs | All byte values representable | Universal-coverage/sequence-cost control |
| T1 Byte-level BPE | Byte alphabet | BPE merges | Byte representation | Main byte-first subword candidate |
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

### Option A — Precomputed, versioned tokenizer artifact

Train the vocabulary outside the app from a rights-cleared, versioned corpus and distribute the tokenizer model/configuration as a standalone asset.

Potential advantages:
- Fixed vocabulary and token IDs before Genesis allocates embedding/output tensors.
- Reproducible model identity across installations that use the same tokenizer revision.
- No tokenizer-training delay or tokenizer-training memory spike during first launch.
- Lower implementation complexity for the first Genesis path.

Risks/open questions:
- The training corpus and vocabulary may reflect a distribution that does not match every installation's later knowledge mix.
- Requires provenance, licensing, attribution and reproducible training records for the tokenizer asset.
- Updates to the vocabulary require a new tokenizer/model generation contract and migration strategy.

### Option B — Per-installation local tokenizer training

Each installation trains its tokenizer from a locally available and rights-cleared seed corpus before constructing the initial neural model.

Potential advantages:
- Tokenizer vocabulary can reflect the local seed distribution and product policy.
- Does not require a shared vocabulary asset if the whole algorithm and initial text are available locally.

Risks/open questions:
- Adds CPU, memory, storage and startup complexity before Genesis can finalize embedding dimensions.
- Different installations may produce different vocabularies and token-ID mappings, increasing model/tooling divergence.
- A vocabulary change invalidates direct embedding/output-head compatibility.
- The tokenizer-training corpus must be available offline and legally eligible on every device.
- Deterministic reproducibility and fallback behavior must be specified.

### Option C — Algorithmic byte-level tokenizer

Use a fixed byte mapping, optionally with a fixed byte-level merge artifact if the implementation requires it. The pure raw-byte baseline itself has no learned vocabulary.

Potential advantages:
- Full byte coverage and predictable base alphabet.
- No tokenizer vocabulary training is required for raw bytes.
- Stable algorithmic behavior when fully specified.

Risks/open questions:
- Raw bytes can increase sequence length and compute; the ByT5 paper documents this trade-off for its own model.
- A merge vocabulary is still a learned data-derived artifact and requires provenance.
- Runtime, context and training-cost results must be measured on Argus's target devices.

None of these options is selected. A tokenizer artifact, whether bundled or per-installation, must not be confused with pretrained neural model weights. The final packaging and ownership policy belongs in a reviewed ADR after tokenizer and corpus evidence are available.

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

A tokenizer candidate can advance only if it:
1. Passes deterministic encoding/decoding and declared round-trip behavior.
2. Has no silent text corruption or unacceptable unknown-token behavior.
3. Passes Unicode, whitespace, offset and special-token security conformance tests.
4. Has documented source/trainer provenance and a viable license/redistribution path.
5. Demonstrates acceptable per-language and per-domain sequence costs on the exact fixture.
6. Has an acceptable vocabulary/embedding/output-head memory and compute trade-off for a declared model budget.
7. Has a feasible Android deployment path with exact reference/Android conformance.
8. Produces a stable, fingerprinted artifact compatible with Genesis/checkpoint contracts.
9. Is evaluated with downstream model experiments before any claim that intrinsic metrics predict learned quality.
10. Includes tied-versus-untied embedding/output-head cost profiles and byte/document coverage at candidate context lengths, with final choices explicitly deferred to architecture G1/G3 review.

No candidate may be selected solely because it has:
- The lowest mean token count.
- The highest fertility or bytes-per-token score.
- The smallest tokenizer file.
- The best English-only score.
- A fashionable algorithm or a high profile public deployment.
- A favorable result in a large-model study without Argus-scale validation.

Use a Pareto comparison rather than an opaque one-number weighted score. Hard correctness, rights and security failures are non-compensatory. Among candidates that pass hard gates, present trade-offs across sequence cost, vocabulary/model cost, language/domain parity, downstream probe quality and Android runtime behavior.

Open decisions:
1. Required product languages and relative language/domain sampling weights.
2. Exact rights-cleared tokenizer-training corpus and its revision.
3. Whether tokenizer artifacts are precomputed/bundled or trained per installation.
4. Whether raw bytes remain only a control or are viable as a deployment path.
5. Which subword families and vocabulary budget points remain in the final sweep.
6. Normalization and whitespace policy, including compatibility behavior.
7. Exact special-token inventory and structured message serialization.
8. Required offset semantics and cross-runtime behavior.
9. Android tokenizer implementation/runtime and dependency/license choice.
10. Migration policy for tokenizer revisions and vocabulary changes.

The decision record remains **OPEN** until these questions are resolved with the required corpus, feasibility, downstream and Android evidence.

## 12. Relationship to the wider Argus research plan

- [33 — From-Scratch Training Feasibility](33_FROM_SCRATCH_TRAINING_FEASIBILITY.md) defines the device/resource and E1–E7 gates. Tokenizer work does not establish broad from-scratch feasibility.
- [23 — Neural Architecture Research Plan](23_NEURAL_ARCHITECTURE_RESEARCH_PLAN.md) uses the selected, versioned tokenizer as a shared input contract for controlled architecture comparisons.
- [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md) defines component-level research and identifies decisions blocked by tokenizer G1.
- [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md), documents 27–32, and the corpus phase define source rights, language/domain distribution and tokenizer-training data eligibility.
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
| E-021 | [Equity with Efficiency: An Empirical Study of Tokenizers for Multilingual Large Language Models](https://aclanthology.org/2026.emnlp-main.1783/), Wei et al., EMNLP 2026 | Eleven Southeast Asian languages and controlled 1.5B model comparisons; parity-aware/morphology-driven candidates are research leads, not direct mobile results |
| E-022 | [Subword Tokenization for Low- and Medium-Resource Languages: A Systematic Evaluation](https://ijcai.org/proceedings/2026/633), Daðason and Loftsson, IJCAI 2026 | WordPiece/BPE/Unigram and vocabulary sweeps across six languages; findings vary by language/task and are not an Indonesian result |
| E-023 | [SentencePiece training options](https://google.github.io/sentencepiece/doc/options/) and [byte fallback behavior](https://github.com/google/sentencepiece/blob/master/doc/options.md) | Exact normalization, whitespace, special-token and byte-fallback controls; library defaults must be pinned |
| E-024 | [Hugging Face Tokenizers components](https://huggingface.co/docs/tokenizers/main/en/components) | Normalizer, pre-tokenizer, byte-level alphabet and decoder pipeline concepts; API/version support must be checked before runtime selection |
| E-025 | [tiktoken BPE design notes](https://github.com/openai/tiktoken/blob/main/README.md) | Byte-pair encoding reversibility/open-text properties as implementation reference; not a dependency selection |

**Current decision:** no tokenizer is selected. Tokenizer evaluation proceeds in parallel with tokenizer-neutral architecture component research under [35 — Android Architecture Evidence and Component Comparison Matrix](35_ANDROID_ARCHITECTURE_EVIDENCE_AND_COMPONENT_MATRIX.md); G1 blocks final vocabulary/head/context/packing-dependent decisions. The required next evidence is a versioned fixture, controlled candidate vocabulary sweeps, Unicode/special-token conformance, cross-language and domain metrics, model-state cost analysis, and an eventual Android runtime comparison. Only then may a tokenizer ADR be proposed.
