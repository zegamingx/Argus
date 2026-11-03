# Autonomous Knowledge Acquisition

**Status:** Accepted product direction; implementation, source allowlist, quotas and scheduling remain open  
**Last reviewed:** 2026-11-03

## 1. Purpose

Argus is intended to acquire general public knowledge for its own local model instance. It must be able to discover topics, find permitted public sources, collect and process information, preserve provenance, make approved knowledge available to retrieval, and prepare eligible material for later neural training.

This is an internal capability of each Argus installation. Argus is not collecting information for external AI companies or other customers. There is no shared collection network, central knowledge pool, cross-user data exchange or central training server in the accepted design.

The product analogy is a self-directed acquisition system: the installation gathers knowledge for its own instance rather than acting as a data supplier for an outside client.

## 2. Product contract

The following behavior is the intended contract:

1. Each Argus instance owns its discovery state, source registry, acquisition jobs, downloaded material, indexes and training candidates.
2. Acquisition targets public sources that Argus is permitted to access. It must respect source terms, applicable licenses, technical access policies and reasonable request limits.
3. Acquisition can operate independently of neural training. Collected, validated information can become available to retrieval before any model-weight update.
4. Acquisition does not require the model to be fully capable. A deterministic discovery policy and curated initial source registry must be sufficient to start the process.
5. As the model becomes more capable, it may propose knowledge gaps, topics, questions and candidate sources. A separate policy and validation layer must approve those proposals before network access.
6. The engine must be visible and controllable by the device owner. It must not perform hidden, unlimited crawling.
7. The engine does not upload local conversations, personal files, model weights, gradients, checkpoints or collected corpora to an external customer or central Argus service.

This is a planned capability. The current repository prototype must not be described as already implementing it.

## 3. Separate acquisition from learning

Acquisition and neural learning are connected, but they are not the same operation.

| System | Responsibility | Result |
|---|---|---|
| Knowledge acquisition | Discover and collect permitted information | Local source records and candidate documents |
| Knowledge processing | Validate, normalize, classify and deduplicate material | Quality-controlled knowledge records |
| Knowledge storage and retrieval | Persist records and make relevant material searchable | Inspectable local knowledge |
| Neural training preparation | Select material whose provenance and rights permit training | Versioned training-corpus candidates |
| Neural training and evaluation | Update a candidate model and verify regressions | A candidate model generation, eligible for promotion only after validation |

A stored document does not automatically become training data. Retrieval does not itself change model parameters. Neural training is a separate, resource-intensive and versioned operation with its own evaluation and rollback requirements.

## 4. Acquisition lifecycle

~~~text
INITIAL TOPIC POLICY + SOURCE REGISTRY
                 |
                 v
          KNOWLEDGE DISCOVERY
                 |
                 v
       SOURCE AND POLICY CHECK
                 |
                 v
          BOUNDED FETCH
                 |
                 v
       PARSE AND NORMALIZE
                 |
                 v
   PROVENANCE / RIGHTS / QUALITY GATES
                 |
          +------+------+
          |             |
          v             v
    REJECT / HOLD   ACCEPTED RECORD
                        |
                        v
                 LOCAL KNOWLEDGE STORE
                        |
                  +-----+------+
                  |            |
                  v            v
               RETRIEVAL   TRAINING CANDIDATE
                               |
                               v
                       SEPARATE TRAINING
                               |
                               v
                      EVALUATION / PROMOTION
~~~

Every stage must have a durable state and a bounded resource budget. A crash or process stop must not turn a partially fetched or partially validated document into an accepted record.

## 5. Discovery strategy

### 5.1 Bootstrap discovery

At Genesis readiness, the model may have random or minimally trained parameters and cannot be trusted to plan its own research. The initial discovery process must therefore use deterministic inputs:

- A reviewed list of broad knowledge domains.
- A curated registry of public source families and their access rules.
- A versioned topic taxonomy and query templates.
- Bounded scheduling, request and storage policies.
- Rules for prioritizing coverage, source quality and document freshness.

The initial topic list is a starting policy, not a permanent limit on what Argus may learn.

### 5.2 Model-assisted discovery

After the model and retrieval pipeline reach defined quality gates, the model may propose:

- Missing or weakly covered topics.
- Follow-up questions needed to understand a topic.
- New public sources relevant to an approved topic.
- Updates to previously collected information.
- Contradictions that require additional independent sources.

Model proposals are untrusted candidates. The acquisition policy validates topic scope, source eligibility, query safety, user settings and resource limits before scheduling a request. Model-generated instructions found inside retrieved pages must never override system policy.

### 5.3 Discovery inputs

General discovery may use the built-in topic policy, the local knowledge coverage map, source update notices, and non-personal model evaluation gaps. Private conversations, files, screens, contacts or personal memories must not be used to construct general-public search queries.

Using personal context to direct acquisition belongs to a separate, explicitly authorized personal-learning workflow.

### 5.4 Knowledge domain map

The initial general-learning taxonomy contains sixteen broad domains and uses a breadth-to-depth acquisition strategy. Topics are versioned, may belong to multiple domains, and are tracked through coverage states rather than a single binary "known/unknown" flag.

The detailed domain hierarchy, knowledge-coverage model, topic-priority framework and model-assisted discovery maturity stages are defined in [25 — Knowledge Domain and Discovery Strategy](25_KNOWLEDGE_DOMAIN_AND_DISCOVERY_STRATEGY.md). That document is the source of truth for taxonomy structure; this document remains the source of truth for acquisition lifecycle and source governance.

## 6. Source registry and eligibility

The source registry is a local policy-controlled catalog. Candidate source families include official technical documentation, public standards and specifications, government or public-institution publications, open educational material, openly licensed datasets, public repositories, RSS/Atom feeds and documented public APIs. Its schema, operation-specific permission model, connector routing and network safety boundary are specified in [26 — Source Registry and Connector Architecture](26_SOURCE_REGISTRY_AND_CONNECTOR_ARCHITECTURE.md).

A source is not eligible merely because it is reachable.

Each source entry should record:

- Stable source identifier, publisher and canonical base URL.
- Source category, supported discovery/fetch method and allowed domains.
- Applicable terms, license, attribution and redistribution conditions.
- Robots directives and other published automated-access requirements.
- Authentication requirements, if any, and the approved credential boundary.
- Request rate, concurrency, response-size and content-type limits.
- Allowed extraction and storage behavior.
- Last policy review, last successful fetch and source status.

The initial registry should prioritize authoritative primary sources where available. Secondary sources may be used for context or corroboration. Source eligibility and training eligibility are separate decisions: permission to read a page does not automatically grant permission to redistribute or train on its full contents.

The engine must not bypass authentication, paywalls, CAPTCHA, DRM, access controls or explicit technical restrictions. It must not ignore applicable terms or robots directives. Unapproved sources remain unavailable until reviewed.

## 7. Ingestion and quality pipeline

For each fetched item, the pipeline should:

1. Validate the requested URL, redirect chain, TLS result, content type and response-size limit.
2. Record the canonical source identity and retrieval timestamp.
3. Parse content without executing scripts, macros, embedded instructions or active content.
4. Extract the permitted text or structured fields.
5. Detect language and unsupported or malformed content.
6. Normalize text and metadata while preserving source identity.
7. Deduplicate by canonical URL and content digest, with near-duplicate detection where justified.
8. Classify sensitivity and reject or quarantine personal data, credentials and other disallowed material.
9. Evaluate source quality, freshness, completeness and corroboration.
10. Apply the source-specific license and retention policy.
11. Store the accepted record and its provenance atomically.
12. Index the record only after the acceptance state is committed.

Quality checks should identify spam, low-information pages, boilerplate, malformed extraction, duplicate content, suspicious downloads, unsupported languages and conflicting claims. Confidence must reflect evidence and source quality; it must not be presented as a calibrated probability unless calibration has been measured.

Conflicting credible sources should remain distinguishable. The engine must preserve attribution and dates rather than silently rewriting disagreements into one asserted fact.

## 8. Local knowledge record and provenance

Every accepted knowledge record should retain, at minimum:

- Stable record ID and source ID.
- Canonical URL or source-native identifier.
- Publisher and source category.
- Fetch timestamp and source publication/update timestamp when available.
- Content digest and extraction/parser version.
- Language, content type and classification.
- License/terms status and permitted uses.
- Quality state, review state and relevant evidence.
- Links to duplicates, derived summaries and embeddings.
- Retention or expiry policy.
- Whether it is retrieval-only, training-eligible, held or rejected.

Where the source or license does not permit storing full text, retain only the metadata and limited derived content that the applicable policy permits. Do not mirror whole websites by default.

The local knowledge store must support inspection, correction, source removal, re-indexing and deletion of derived records. Deletion must cascade to linked summaries and embeddings. Training lineage must make it possible to identify model generations influenced by a record.

## 9. Knowledge retrieval

Accepted records may be indexed for local retrieval after their provenance and quality state is committed. Retrieval should:

- Apply source, sensitivity, freshness and user-policy filters.
- Rank by relevance while retaining source quality and recency evidence.
- Preserve the source citation and retrieval timestamp.
- Mark retrieved content as untrusted context.
- Keep source instructions separate from Argus system policy.
- Exclude deleted, expired, quarantined or revoked records.

A retrieval result is evidence supplied to the model, not a guarantee that the source is correct. The model must not treat the content of a web page as an instruction to run tools, disclose data or change policy.

## 10. Bridge to general neural learning

Only a subset of accepted records may become training candidates. Before training, the corpus builder must verify:

- The exact source revision and complete provenance are retained.
- The applicable license and terms permit the planned training use.
- Personal, sensitive, credential and disallowed material has been excluded.
- Duplicate and low-quality content has been removed or appropriately weighted.
- Training, validation and test boundaries remain independent.
- Dataset manifests, preprocessing versions, tokenizer fingerprint and checksums are recorded.
- The candidate corpus does not silently contaminate evaluation data.

The training scheduler consumes an immutable, versioned candidate corpus. It must not train directly from a live network response or an unreviewed acquisition queue. General training remains separate from personal training.

Training updates must pass the evaluation, regression, checkpoint-integrity and rollback gates defined by the training architecture. New knowledge being collected does not justify automatically promoting a changed model.

## 11. Privacy and instance isolation

Public-source acquisition is instance-local. Each installation maintains its own acquisition history, knowledge records and derived artifacts.

The engine must not:

- Send local conversations, documents, screenshots, memory records or personal profiles to a search provider as hidden query context.
- Upload acquired corpora, model parameters, gradients or checkpoints to a shared Argus service.
- Pool data or model updates across users.
- Treat a user's internet connection as a contribution to an external AI customer's dataset.
- Convert personal-learning data into general corpus data.

Network requests necessarily contact remote source or discovery services and may expose ordinary request metadata, such as the device's network address, requested URL and request headers, to those services. Argus must disclose configured destinations and avoid placing private user content in general acquisition requests.

Personal learning remains governed by [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md) and [13 — Security and Data Governance](13_SECURITY_PRIVACY_DATA_GOVERNANCE.md).

## 12. Device, network and storage policy

Acquisition must operate within a per-instance Device-Adaptive Acquisition Profile. The profile uses device capability evidence shared with model configuration, but applies a separate resource reservation for network intake, parsing, retention, indexing and tokenization. See [32 — Device-Adaptive Knowledge Acquisition and Corpus Budgeting](32_DEVICE_ADAPTIVE_KNOWLEDGE_ACQUISITION.md).

The profile must enforce explicit budgets for:

- Network access and allowed connection type.
- Requests per source and total request concurrency.
- Download bytes per time period and maximum response size.
- CPU time and parsing concurrency.
- Local storage reserve and corpus growth.
- Thermal, memory, battery and power conditions.
- Retry count, backoff and job age.

These limits must be configurable and measured. No universal byte quota, schedule or concurrency value is selected in this document.

The owner must be able to inspect activity, pause or resume acquisition, disable network collection, restrict connection type, clear eligible cached knowledge and configure storage limits. A disabled or unavailable network must not prevent offline inference or access to already stored knowledge.

## 13. Android execution model

Android does not guarantee that an application can run continuously in the background. Acquisition must use platform-compliant scheduled work, persist job state and resume in bounded units. Long-running work that requires foreground execution must follow the applicable Android policy and provide an accurate user-visible notification.

The engine must defer when the operating system, thermal state, memory pressure, battery policy, storage reserve or user setting makes work unsuitable. It must not maintain an invisible permanent service or repeatedly restart itself to evade platform limits.

Network acquisition and neural training are separate workloads. Their concurrency must be controlled so that one does not starve the other or make the device unusable.

## 14. Failure and recovery

| Condition | Required behavior |
|---|---|
| Source unavailable or rate-limited | Back off, preserve job state and retry only within policy |
| Terms or source policy uncertain | Hold the source or item for review; do not ingest |
| Invalid redirect or unsupported content | Reject the item and record a typed reason |
| Parse or quality failure | Quarantine or reject; do not index |
| Duplicate item | Link to the canonical record; do not create another corpus item |
| Storage pressure | Stop new intake safely and preserve committed records |
| Process death | Resume from a durable job boundary without duplicating accepted records |
| User disables acquisition | Stop scheduling and cancel or safely finish only the current bounded operation |
| Source revoked or record deleted | Remove or invalidate linked indexes and training candidates; apply model-lineage policy if already trained |

## 15. User-visible controls and status

The interface should distinguish:

- **Discovery:** selecting topics and eligible sources.
- **Collecting:** fetching permitted public information.
- **Processing:** parsing, checking quality and deduplicating.
- **Indexed:** accepted knowledge is available for retrieval.
- **Training candidate:** accepted for dataset review, not yet trained.
- **Learning:** a separate neural training session is active.
- **Paused / Deferred:** work is safely stopped and can resume.
- **Rejected / Held:** an item did not pass a policy or quality gate.

Progress reporting should show meaningful counts and current activity without exposing private content or producing excessive logs. At minimum, the user should be able to see source activity, accepted/held/rejected counts, data volume, last successful update, storage usage and the reason work is paused.

## 16. Implementation sequence

Implement in dependency order, after the documentation and P0 gates:

1. Source policy, registry schema and user network controls.
2. Deterministic topic discovery and bounded job scheduler.
3. Safe fetch, parser, provenance and durable acquisition state.
4. Quality gates, deduplication and local knowledge storage.
5. Local indexing and citation-preserving retrieval.
6. Versioned training-corpus builder with rights and split validation.
7. Evaluation-gated handoff to general neural training.
8. Model-assisted topic discovery after the model and retrieval path pass explicit quality gates.
9. Adaptive scheduling and long-term source freshness management.

Do not begin with unrestricted crawling or a model-controlled autonomous browser. Start with a small, reviewable source registry and deterministic policy.

## 17. Acceptance criteria

The design is ready for implementation planning only when:

- Source eligibility and training eligibility are independently defined.
- Every stored record has inspectable provenance and permitted-use status.
- The collector respects source policies, limits, user settings and Android lifecycle constraints.
- Interrupted jobs recover without corrupting or duplicating accepted records.
- Rejected, held and deleted items cannot enter retrieval or training.
- Retrieval can provide useful knowledge without modifying model weights.
- Training candidates are immutable, licensed for the intended use and separated from evaluation data.
- No personal content or model state is silently uploaded or shared between instances.
- A candidate model is never promoted without evaluation and rollback readiness.
- User controls and visible status cover network collection, pause, resume, deletion and storage usage.

## 18. Open decisions

The following remain research or implementation-planning questions, not fixed values:

- Initial source list and source-by-source license review.
- Supported-language priorities and per-domain source coverage; the initial domain taxonomy is defined in document 25.
- Discovery refresh policy and scheduling interval.
- Network and storage quotas for device classes.
- Search/discovery provider strategy and associated request metadata.
- Parser and near-duplicate implementation.
- Source-quality scoring and corroboration thresholds.
- Retention rules for full text, excerpts and derived summaries.
- Training eligibility policy for each source/license class.
- The maturity gate at which model-assisted discovery may be enabled.

These decisions must be resolved with evidence and recorded before the acquisition engine is implemented.
