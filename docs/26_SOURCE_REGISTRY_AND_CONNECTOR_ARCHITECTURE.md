# Source Registry and Connector Architecture

**Status:** Accepted architecture direction; initial source allowlist and source-by-source rights review remain open  
**Last reviewed:** 2026-11-03

## 1. Purpose

This document defines how Argus discovers eligible public sources, decides what each source may be used for, and retrieves information through replaceable connector implementations.

The source registry is a policy-controlled catalog, not an unrestricted list of URLs. It lets the acquisition engine use many kinds of public sources while applying consistent access, safety, provenance, licensing, resource and privacy rules.

Each Argus installation owns its own registry state, acquisition history and collected knowledge. The design does not create a shared residential-proxy network or provide participant bandwidth to external AI customers. Argus uses the device owner's network connection for the owner's Argus instance.

## 2. Architecture overview

~~~text
TOPIC / KNOWLEDGE GAP
        |
        v
DISCOVERY PLANNER
        |
        v
SOURCE RESOLVER
        |
        v
SOURCE REGISTRY + POLICY ENGINE
        |
        +---- ineligible / unknown ----> HOLD OR REJECT
        |
        v
CONNECTOR ROUTER
        |
        +---- Search / Discovery Connector
        +---- Feed Connector
        +---- Sitemap Connector
        +---- Public API Connector
        +---- Web Document Connector
        +---- Repository / Dataset Connector
        |
        v
FETCH JOB + NETWORK SAFETY GATE
        |
        v
CONTENT PARSER / NORMALIZER
        |
        v
PROVENANCE + RIGHTS + QUALITY PIPELINE
        |
        +---- Retrieval-only record
        +---- Training-eligible candidate
        +---- Held / rejected record
~~~

The registry and policy engine decide whether an operation is permitted. Connectors only implement the approved transport and extraction method. A connector must never grant itself permission to crawl, retain, index or train on content.

## 3. Source families and connector types

The first version should support a small number of generic connector contracts rather than implement a separate crawler for every website. The currently researched named-source shortlist and domain mapping are recorded in [27 — Initial Source Registry Candidates](27_INITIAL_SOURCE_REGISTRY_CANDIDATES.md); entries there remain candidates until this registry's policy review is complete.

| Connector family | Discovery / acquisition role | Typical output |
|---|---|---|
| Search provider | Discover candidate URLs for a topic or query | Search result metadata and canonical candidate URLs |
| Official index / catalog | Discover documents in a known institutional collection | Document metadata, identifiers and permitted links |
| RSS / Atom feed | Detect newly published or updated items | Feed entries, timestamps and canonical links |
| Sitemap | Discover pages exposed by a source | Candidate URLs and optional last-modified hints |
| Public API | Retrieve structured data under documented API terms | JSON, XML, CSV or other documented payloads |
| Web document | Fetch and extract permitted public HTML or static documents | Normalized text, metadata and source references |
| Repository / dataset | Discover or retrieve public source-controlled material | Versioned metadata, files, manifests and checksums |
| Curated seed | Start discovery from reviewed, versioned source/topic entries | Deterministic initial jobs |

Search and discovery connectors find candidates; they are not automatically authoritative knowledge sources. The underlying publisher or dataset still needs to be resolved and checked.

### 3.1 Initial implementation boundary

Start with:
- Curated seeds.
- Search/discovery metadata.
- RSS/Atom.
- Sitemaps.
- Documented public APIs.
- Static public web documents.

Repository archives, large datasets and complex document formats should be enabled only when their parser, size limits, provenance and license handling are verified. Full browser automation, login-driven crawling and unrestricted recursive crawling are not part of the initial design.

## 4. Source registry data model

A source entry should be stable, versioned and auditable. At minimum it contains:

| Field group | Required information |
|---|---|
| Identity | Stable source ID, publisher, source name, canonical host and source category |
| Scope | Covered domains/subdomains, regions or jurisdictions, languages, allowed paths and excluded paths |
| Connector | Connector type, endpoint pattern, parser profile, accepted content types and supported operations |
| Authority | Primary/secondary/community classification, subject-specific authority notes and known limitations |
| Access policy | Terms URL/revision, robots-policy observation, API terms, authentication requirements and allowed request behavior |
| Rights | Copyright/license evidence, attribution, redistribution, retention and training-use conditions |
| Limits | Rate/concurrency limits, maximum response size, content-type constraints, cache policy and retry rules |
| Security | Redirect policy, TLS requirements, domain allowlist and credential boundary |
| Lifecycle | Review status, last review date, last successful fetch, last policy change and suspension/revocation reason |
| Evidence | Reviewer/source of policy evidence, evidence URL, checked timestamp and policy version |

A source entry must not imply that all content on a domain shares one license. Rights may vary by page, document, dataset, author or embedded third-party asset. Where the applicable rights cannot be resolved, the item remains held or restricted.

## 5. Permissions are operation-specific

Source access is not a single yes/no permission. Argus must track permissions separately for each operation.

| Operation | Meaning |
|---|---|
| Discover | Use source metadata, index or search results to find candidate items |
| Fetch | Request a particular public item |
| Transiently process | Parse content in memory for permitted extraction |
| Retain full text | Store a durable full-text copy |
| Retain excerpt / metadata | Store limited excerpts and bibliographic metadata |
| Build retrieval index | Create embeddings, lexical indexes, summaries or other retrieval derivatives |
| Use for neural training | Include content in a training-corpus candidate |
| Redistribute | Export or republish content outside the local instance |

Each operation should be represented as **ALLOWED**, **CONDITIONAL**, **DENIED** or **UNKNOWN**, with evidence and applicable conditions. UNKNOWN must fail closed for the affected operation.

Examples:
- Permission to discover a URL does not automatically permit fetching it.
- Permission to fetch does not automatically permit durable full-text storage.
- Permission to store or index for retrieval does not automatically permit neural training.
- Permission to train does not automatically permit redistribution.
- An open API does not mean every linked underlying document is openly licensed.

The policy engine evaluates source-level and item-level rights. More restrictive item-level terms override broader assumptions about the host.

## 6. Review lifecycle and policy gates

The source lifecycle is:

~~~text
CANDIDATE
   |
   v
REVIEW_REQUIRED
   |
   +---- evidence incomplete / unclear ----> HELD
   |
   v
APPROVED_WITH_SCOPED_PERMISSIONS
   |
   +---- policy violation / terms change ----> SUSPENDED
   |
   +---- source withdrawn / prohibited ------> REVOKED
~~~

Approval must record the exact permitted operations, domains or paths, content types, limits and review evidence. A source approved for discovery only must not be routed to a content fetcher.

Before every fetch, the policy engine re-checks the current source state, operation permission, URL scope, user setting and resource limits. Cached approval must not silently survive a known revocation or policy change.

Source review should consider:
- Authority for the specific claim type.
- Publisher identity and editorial/review process.
- License and terms clarity.
- Stability, provenance and update metadata.
- Language and jurisdiction coverage.
- Content quality, duplication and known bias.
- Automated access constraints and rate limits.
- Security and malware risk.

No publisher is universally authoritative for every subject. Source quality is contextual to the claim being evaluated.

## 7. Source authority and corroboration

Use source roles rather than one universal ranking:

- **Primary / authoritative:** source owner, official institution, regulator, standards body, original dataset or first-party documentation for claims within its authority.
- **Scholarly / institutional:** peer-reviewed work, academic institution, research organization or established scientific index; evaluate study quality and publication status.
- **Curated educational:** textbooks, reputable educational institutions, reference works and carefully edited explanations.
- **Community / discussion:** forums, Q&A and social content; useful for discovering questions, practical experience or candidate references, but not sufficient alone for high-impact claims.

Corroboration must account for source independence. Several pages copied from one press release are not independent confirmation. Preserve disagreements between credible sources rather than silently choosing one.

Medical, legal, financial, safety and other high-impact topics follow the stricter requirements in [25 — Knowledge Domain and Discovery Strategy](25_KNOWLEDGE_DOMAIN_AND_DISCOVERY_STRATEGY.md).

## 8. Source selection and connector routing

For a topic, Source Resolver filters the registry using:

1. Domain and topic coverage.
2. Language, region and jurisdiction requirements.
3. Operation-specific permissions.
4. Authority appropriate to the claim type.
5. Freshness and source update characteristics.
6. Connector availability and supported content types.
7. Rate, size, network and storage budgets.
8. Prior quality outcomes and known source limitations.

Only eligible sources proceed to connector routing. When a primary source is available, prefer it for the claims it owns. Additional independent sources may be selected to explain, contextualize or corroborate.

Routing must be deterministic and inspectable. Record why a source was selected or rejected. Do not allow model-generated URLs to bypass registry resolution.

## 9. Fetch job contract

Every network operation should be represented by a durable, bounded job. A job should include:

- Job ID and acquisition-run ID.
- Topic and taxonomy version.
- Source ID and source-policy version.
- Canonical URL or API resource identifier.
- Connector and parser profile versions.
- Requested operation and permitted-use scope.
- Priority, creation time, retry count and expiry.
- Request and response byte budgets.
- Redirect and content-type constraints.
- User-setting snapshot relevant to this job.
- Result status, timestamps and typed failure reason.

Jobs must be idempotent where possible. A retry must not duplicate an accepted source record or reset its provenance. The job queue must persist state before an operation is treated as scheduled.

## 10. Network and content safety

All connectors share one network safety boundary. At minimum:

- Accept only explicitly supported URL schemes, normally HTTPS for public web sources.
- Enforce host/path allowlists and source-specific endpoint constraints.
- Validate every redirect against policy; do not let an approved URL redirect to an unapproved host.
- Block loopback, private, link-local, multicast and other non-public address ranges for public-source fetches.
- Defend against DNS rebinding and resolve/connect inconsistencies.
- Require valid TLS certificate verification; do not provide a permissive fallback.
- Enforce response-size, decompression-size, time, redirect-count and content-type limits.
- Parse content as data; never execute page scripts, downloaded code, macros or embedded instructions.
- Treat retrieved text as untrusted and isolate it from system policy and tool instructions.
- Reject unsupported archives and suspicious file types.
- Keep credentials out of query strings, logs and model context; use credentials only for explicitly approved APIs.
- Avoid collecting cookies, browser profiles or authenticated user sessions.

Do not bypass paywalls, CAPTCHA, login requirements, DRM, access controls, robots directives or applicable source terms. If an automated-access rule or permission is unclear, hold the source for review.

For public-source acquisition, the initial implementation should use direct, bounded requests through approved connectors. It should not masquerade as a residential proxy or route third-party AI customers' traffic through user devices.

## 11. Local policy and privacy

The device owner must be able to inspect source categories and configured destinations, enable/disable network acquisition, restrict connection type, pause/resume jobs and clear eligible cached knowledge.

General acquisition queries must be derived from the public knowledge taxonomy, source metadata and non-personal coverage gaps. They must not silently include private chats, files, screenshots, browsing history, contacts or personal memories.

Network requests expose ordinary connection metadata to remote destinations. The user-facing privacy information must explain configured discovery providers and source destinations. Argus must not upload collected corpora, personal information, model weights, gradients or checkpoints to a central Argus service or an external AI customer.

## 12. Resource controls and scheduling

Connector and source limits must be enforced centrally and per source. Controls include:

- Per-source rate and concurrency limits.
- Global network concurrency.
- Maximum response and decompressed bytes.
- Maximum redirects and request duration.
- Download budget per period.
- CPU time and parser concurrency.
- Storage reserve and retained-data cap.
- Retry/backoff budget and job expiry.
- Thermal, memory, battery and Android lifecycle admission.

The operating system may defer background execution. Jobs must be resumable and bounded; no permanent hidden service or scheduling workaround is allowed. Network acquisition and neural training have separate queues and coordinated resource admission.

No numeric quotas, concurrency counts or refresh intervals are fixed here. They must be calibrated by device class, source terms and measured results.

## 13. Source update and revocation handling

For sources with reliable update metadata, use conditional requests or source-native update mechanisms when supported and permitted. Otherwise, use a domain-specific freshness policy rather than repeatedly fetching the entire source.

When terms change, a source is compromised, access is revoked or rights become uncertain:
1. Suspend new jobs for the affected scope.
2. Record the policy change and affected source items.
3. Re-evaluate retained content and derived records.
4. Remove or invalidate records that are no longer permitted to retain or use.
5. Remove affected retrieval derivatives.
6. Exclude affected material from pending training corpora.
7. Apply model-lineage and rollback policy if content has already influenced a promoted generation.

Source freshness and rights review are different controls. A recently fetched item may still be prohibited; an old item may still be valid but stale.

## 14. Observability

The owner and engineering diagnostics should be able to distinguish:

- Candidate source discovered.
- Source held for review.
- Source approved with scoped permissions.
- Job queued, deferred, running, complete or failed.
- Item fetched, parsed, validated, indexed or training-eligible.
- Job skipped due to policy, rate limit, storage, thermal or network state.
- Source suspended or revoked.

Logs should contain IDs, status codes, timings, byte counts and typed reasons, not private page contents, credentials or full user context. Progress must not generate per-byte or per-token log spam.

## 15. Implementation sequence

1. Define registry and policy schemas with explicit operation permissions.
2. Add curated seed-source support and registry review tooling.
3. Implement one generic search/discovery connector and one bounded HTTPS content connector.
4. Add feed, sitemap and public API connectors behind the same contract.
5. Implement fetch-job persistence, retry, redirect validation and resource limits.
6. Add domain-specific parser profiles and source-level provenance.
7. Add rights-aware retention, retrieval and training eligibility.
8. Expand the allowlist only after each source has a reviewed policy record.

Do not implement dozens of site-specific scrapers before proving the registry, policy and connector contracts.

## 16. Acceptance criteria

- Every fetched item resolves to an approved source record and a permitted operation.
- UNKNOWN or missing permissions fail closed for the affected operation.
- Discovery, fetch, retention, retrieval, training and redistribution permissions are independently represented.
- Source and item-level rights are preserved with evidence and review timestamps.
- Connectors cannot bypass policy or broaden their own permissions.
- Redirects, DNS, TLS, response sizes and content types are validated centrally.
- Jobs are durable, bounded, observable and safely retryable.
- The first connectors work without login automation, paywall bypass or hidden browser sessions.
- Source selection records its rationale and can prefer primary sources when appropriate.
- No participant bandwidth is exposed as a third-party residential proxy service.
- Network acquisition uses only the local instance's approved public-source requests.
- Owner controls cover destination visibility, network disablement, pause/resume and storage management.

## 17. Open decisions

- The initial named source allowlist for each of the sixteen knowledge domains.
- Source-specific terms, licenses, attribution and training-use eligibility.
- Search/discovery provider choices, costs and request-metadata implications.
- Connector implementation libraries and parser support matrix.
- Exact source rate limits, response limits and device-tier budgets.
- Policy-review process and schedule for source terms changes.
- Retention defaults for full text, excerpts, metadata and retrieval derivatives.
- Jurisdiction-specific source selection for legal, medical and financial topics.

These must be resolved source by source before enabling broad acquisition. A general source category is not approval for every website or dataset in that category.
