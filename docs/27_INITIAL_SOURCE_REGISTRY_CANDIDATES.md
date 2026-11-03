# Initial Source Registry Candidates

**Status:** Research shortlist only; no source is activated by this document  
**Research date:** 2026-11-03  
**Scope:** Candidate sources for the first Argus public-knowledge acquisition profile

## 1. Purpose and decision boundary

This document records a first named-source shortlist for the sixteen-domain knowledge map. It is not a blanket allowlist, legal opinion or authorization to scrape every page on a listed domain.

Each source must receive a registry entry with an exact endpoint or dataset, current terms evidence, permitted operations, rate/resource limits and review date before the connector may use it.

The source registry distinguishes:

- **Discovery metadata:** bibliographic, catalog or entity metadata used to find useful material.
- **Content retrieval:** a particular page, article, record or dataset fetched for local reference.
- **Local retention:** full text, excerpts, metadata or structured records stored locally.
- **Neural training:** material selected for a versioned training corpus.
- **Redistribution:** material exported or republished outside the Argus instance.

A public endpoint or no-cost API does not automatically authorize all five operations.

## 2. Candidate status vocabulary

| Status | Meaning |
|---|---|
| CANDIDATE | Worth including in the first source registry review |
| METADATA-FIRST | Intended initial use is source metadata/discovery; underlying content has separate rights |
| ITEM-LEVEL-REVIEW | Each record, file or dataset must be checked for its own license and terms |
| CONDITIONAL | Additional terms, credentials, rate limits or rights constraints need resolution |
| NOT-ACTIVATED | No production acquisition job may use it until a reviewed registry policy exists |

All sources below remain NOT-ACTIVATED until their individual registry entries are reviewed. Status labels describe the research shortlist, not a runtime permission.

## 3. Named source candidates

### SRC-001 — OpenAlex

- **Coverage:** scholarly discovery across many academic disciplines.
- **Candidate role:** work, author, institution, source and topic metadata; discovery and citation/related-work expansion.
- **Access:** documented REST API; documentation states it is free to start without an API key. A free key increases the daily budget and heavier usage is pay-as-you-go.
- **Data rights:** OpenAlex API documentation states that its data are CC0.
- **Initial scope:** metadata-first. Use metadata to discover papers and identify candidate open-access locations. A paper's full text must still be checked against the license of that specific copy.
- **Important boundary:** OpenAlex metadata is not a substitute for reading and evaluating the underlying research. Metadata records alone are not a general language-training corpus.
- **Evidence:** [OpenAlex API reference](https://help.openalex.org/api/) and [OpenAlex license vocabulary](https://help.openalex.org/data/licenses/).

### SRC-002 — Crossref REST API

- **Coverage:** scholarly publication discovery across disciplines.
- **Candidate role:** DOI resolution, bibliographic metadata, publication dates, funders, license metadata and post-publication updates.
- **Access:** public REST API; Crossref states that no sign-up is required.
- **Data rights:** Crossref states that most bibliographic metadata are facts not subject to copyright and that Crossref-generated data are CC0. Some abstracts remain subject to publisher or author copyright.
- **Initial scope:** metadata-first. Abstract text must retain its own rights status; do not assume all abstracts are reusable or trainable.
- **Important boundary:** use retraction and post-publication metadata to flag affected works. A discovered paper must not be treated as current valid evidence without publication-status checks.
- **Evidence:** [Crossref REST API](https://www.crossref.org/documentation/retrieve-metadata/rest-api/) and [Crossref metadata licensing](https://www.crossref.org/documentation/retrieve-metadata/).

### SRC-003 — Wikidata

- **Coverage:** broad entities, concepts, identifiers, aliases and cross-domain relationships.
- **Candidate role:** entity disambiguation, knowledge-graph bootstrap, cross-domain links and multilingual labels.
- **Data rights:** Wikidata identifies structured data in its main, Property, Lexeme and EntitySchema namespaces as CC0. Text in other namespaces has different terms, including CC BY-SA 4.0.
- **Initial scope:** structured data only. Retain entity identifiers and provenance/references. Treat individual claims as evidence-bearing statements, not unquestionable facts.
- **Important boundary:** community-maintained structured data is useful for discovery and entity relationships; it does not replace primary sources for medical, legal, scientific or other high-impact claims.
- **Evidence:** [Wikidata licensing policy](https://www.wikidata.org/wiki/Wikidata:Licensing) and [Wikidata data access](https://www.wikidata.org/wiki/Wikidata:Reuse).

### SRC-004 — Europeana APIs

- **Coverage:** European cultural heritage, history, art, literature, archives and collections.
- **Candidate role:** discover cultural objects and retrieve normalized collection metadata.
- **Access:** Europeana states that an API key is required for most APIs, with SPARQL as an exception. API credential handling must be designed without embedding private secrets in source code.
- **Data rights:** Europeana states that API metadata are CC0. Linked partner objects must be used according to the rights statement carried in each object's metadata.
- **Initial scope:** metadata-first. Item media, scans, images, texts and other linked objects remain item-level review.
- **Important boundary:** CC0 metadata does not make the linked digitized object CC0.
- **Evidence:** [Europeana API FAQ](https://europeana.atlassian.net/wiki/spaces/EF/pages/2360508417/Europeana+API+FAQ).

### SRC-005 — World Bank Data / Indicators API

- **Coverage:** economic and social indicators, population, development, education, health, energy and related public statistics.
- **Candidate role:** country/region indicator metadata and structured time-series data.
- **Access:** World Bank Indicators API documentation describes V2 and states API keys are not required.
- **Data rights:** World Bank-produced open datasets generally use CC BY 4.0, but the World Bank notes that many datasets have other licenses and are labeled accordingly. Additional terms apply to the default CC BY 4.0 datasets.
- **Initial scope:** item/dataset-level review with explicit attribution metadata. Check the license attached to the exact dataset and any third-party contribution before retention or training.
- **Important boundary:** preserve indicator unit, source note, country/region, date, methodology and revision context. Never treat an indicator as an undated universal fact.
- **Evidence:** [World Bank Indicators API](https://datahelpdesk.worldbank.org/knowledgebase/articles/889392) and [World Bank data licenses](https://datacatalog.worldbank.org/public-licenses).

### SRC-006 — USDA FoodData Central

- **Coverage:** food composition, nutrients and food reference data.
- **Candidate role:** structured food/nutrient records for agriculture, food science and practical knowledge.
- **Access:** API requests require a data.gov API key and are rate-limited. USDA also publishes downloadable CSV/JSON datasets. Download releases vary in size; the full October 2026 CSV collection is listed at approximately 3.1 GB uncompressed, so Argus should select a suitable subset rather than download everything.
- **Data rights:** USDA states FoodData Central data are public domain and released under CC0 1.0. USDA requests source attribution.
- **Initial scope:** prefer selected official bulk datasets where practical; API use requires an approved credential plan. Keep release date, data type and source citation.
- **Important boundary:** branded product records and nutrient reference data have different usefulness and update patterns. Do not ingest the largest dataset merely because it is available.
- **Evidence:** [FoodData Central API guide and licensing](https://fdc.nal.usda.gov/api-guide/) and [FoodData Central downloadable datasets](https://fdc.nal.usda.gov/download-datasets/).

### SRC-007 — PubMed Central Open Access (PMC OA)

- **Coverage:** biomedical and life-science research.
- **Candidate role:** scholarly metadata discovery and eligible full-text research retrieval.
- **Access:** PMC documents OAI-PMH, E-Utilities, BioC and its cloud service as approved automated retrieval routes. The OAI-PMH documentation specifies request-rate limits and advises against concurrent requests for high-volume retrieval.
- **Data rights:** not all PMC articles are available for text mining or reuse. Full-text use depends on the license or usage-rights statement of each article.
- **Initial scope:** metadata discovery may cover the wider collection; full-text retrieval is restricted to articles whose item-level terms permit the intended operation.
- **Important boundary:** free-to-read is not the same as open license. Never bulk-fetch all PMC articles through an unapproved route.
- **Evidence:** [PMC OAI-PMH API](https://pmc.ncbi.nlm.nih.gov/tools/oai/) and [NLM explanation of the PMC Open Access subset](https://support.nlm.nih.gov/knowledgebase/article/KA-03253/en-us).

### SRC-008 — NASA Science Data Repositories

- **Coverage:** space science, astronomy, Earth science, climate observations and mission data.
- **Candidate role:** discover and retrieve scientific datasets with source-native metadata.
- **Data rights:** NASA's science-data licensing page describes item-level license labeling. Data from NASA-led missions are CC0 unless a restrictive notice/license applies; other data may originate from third parties and require separate rights verification.
- **Initial scope:** item-level review. NASA-led mission data with a clear CC0 designation may be considered for broader reuse after checking dataset metadata and restrictions.
- **Important boundary:** do not generalize scientific-data policies to all text, imagery or media on NASA websites. Preserve mission, instrument, calibration, units, provenance and collection version.
- **Evidence:** [NASA Science Data Licenses](https://science.data.nasa.gov/about/license).

### SRC-009 — NOAA National Weather Service

- **Coverage:** weather observations, forecasts, warnings and selected hydrological information within the National Weather Service's remit.
- **Candidate role:** time-sensitive public weather and warning information, plus historical and explanatory records.
- **Data rights:** the NWS states that its web information is public domain unless specifically noted otherwise, subject to conditions such as no false ownership claim or implied endorsement.
- **Initial scope:** NWS-specific material only; record observation/issue/valid times and source identifiers. Other NOAA data products require their own source policy.
- **Important boundary:** forecasts and warnings are time-critical. Expired content must not appear as current. Do not alter NWS material and present it as official government output.
- **Evidence:** [National Weather Service disclaimer](https://www.weather.gov/disclaimer).

### SRC-010 — FAO Corporate Statistical Databases

- **Coverage:** agriculture, food, nutrition, forestry, fisheries and related global statistics.
- **Candidate role:** structured statistical indicators and reference datasets.
- **Data rights:** FAO corporate statistical databases generally use CC BY 4.0 unless metadata/page terms specify otherwise. FAO adds terms, including restrictions on uses that promote a commercial enterprise, and warns that third-party datasets may carry different restrictions.
- **Initial scope:** CONDITIONAL. Review the exact dataset and additional terms before any durable retention, redistribution or neural-training use.
- **Important boundary:** a database-level default license does not clear every third-party component. Preserve attribution, update year, source and methodology.
- **Evidence:** [FAO Statistical Database Terms of Use](https://www.fao.org/contact-us/terms/db-terms-of-use/en).

## 4. Initial domain coverage mapping

This is a candidate routing map, not a claim that every domain is fully covered.

| Knowledge domain | Initial source candidates | Current coverage limitation |
|---|---|---|
| KD-01 Language and Communication | Wikidata structured labels; institution and dictionary sources still to be reviewed | Need authoritative language/grammar sources across regions and languages |
| KD-02 Mathematics and Statistics | OpenAlex and Crossref metadata; licensed educational material still to be selected | Metadata discovers research but does not supply validated teaching content |
| KD-03 Physical Sciences | OpenAlex/Crossref metadata; NASA science datasets | Dataset terms and individual paper licenses still apply |
| KD-04 Life Sciences | OpenAlex/Crossref metadata; eligible PMC OA items | Full text only where article rights and PMC retrieval rules permit |
| KD-05 Computing and Digital Technology | Official first-party documentation and repositories remain source-review candidates | Terms and repository license must be evaluated per source/item |
| KD-06 Engineering and Applied Technology | Scholarly metadata; public institutional datasets and documentation remain candidates | Many standards may be paywalled or restricted |
| KD-07 Health and Medicine | PMC OA, OpenAlex and Crossref | High-impact evidence review; item-level licenses and publication status |
| KD-08 Earth, Climate and Environment | NASA science data; NOAA NWS data | NASA item-level rights; NWS scope is not all NOAA |
| KD-09 Geography and Geospatial Knowledge | Wikidata structured data; official geospatial agencies remain candidates | Need regional authority and dataset-specific rights review |
| KD-10 History and Archaeology | Wikidata structured data; Europeana metadata and item-linked collections | Digitized media rights differ per item |
| KD-11 Society, Governance and Law | World Bank indicators; official jurisdiction-specific portals remain candidates | Laws require jurisdiction, effective date and source-of-law validation |
| KD-12 Economics, Business and Finance | World Bank indicators; OpenAlex/Crossref metadata | Dataset license and date context required; not a financial-advice source |
| KD-13 Philosophy, Religion and Ethics | OpenAlex/Crossref metadata; institutional and primary-text collections remain candidates | Need source selection that respects regional and interpretive diversity |
| KD-14 Arts, Literature and Culture | Europeana metadata and item-linked collections | Media/text rights are item-specific |
| KD-15 Education and Research Methods | OpenAlex and Crossref metadata; openly licensed course materials remain candidates | Metadata does not establish curriculum quality or full-text rights |
| KD-16 Agriculture, Food and Practical Skills | USDA FoodData Central; FAO statistics (conditional) | USDA subsets must be scoped; FAO additional terms and third-party rights apply |

The first source set deliberately combines broad discovery metadata with a few structured-data collections. It is not yet enough to claim balanced, multilingual or comprehensive knowledge coverage.

## 5. Initial activation recommendation

Do not enable all sources at once. The initial technical pilot should exercise a small set of low-complexity, documented interfaces:

1. OpenAlex or Crossref for scholarly metadata discovery.
2. Wikidata structured data for entity identifiers and relationships.
3. World Bank Indicators API for versioned structured indicators, after dataset-level license policy is recorded.
4. USDA FoodData Central selected downloads for a bounded CC0 structured-data case, avoiding an embedded API key.
5. One time-sensitive official source profile, such as NWS, with strict timestamp and expiry behavior.

Europeana, PMC OA, NASA and FAO should remain scoped candidates until their source-specific credentials, item-level rights, rate limits and data-lineage rules have been implemented and tested.

The pilot should verify the full pipeline: discover → resolve source → policy check → fetch → parse → provenance → quality → local store → retrieval. Neural training must remain disabled for these new feeds until the separate training eligibility and evaluation gates are complete.

## 6. Credential and API-key policy

- Never commit API keys or place private credentials in the Android source tree.
- Do not assume that an API key embedded in an APK is secret; distributed application packages can be inspected.
- Prefer documented no-key access or static public downloads when allowed and practical.
- If a provider requires a key, decide whether it is a public application identifier, a user-supplied key, or requires a service architecture. This decision must be explicit and reviewed against the local-first/no-central-training-server boundary.
- Enforce provider rate limits centrally and expose typed rate-limit status.
- Do not share one hidden credential across untrusted installations unless the provider explicitly permits that usage and the security/operational model has been reviewed.

## 7. What is not approved by this shortlist

- Unrestricted crawling of entire domains.
- Scraping content behind login, subscription or access control.
- Downloading all documents from academic repositories regardless of license.
- Treating abstracts, excerpts, images, books or media as covered by a dataset's metadata license.
- Training directly from search-result snippets or live web responses.
- Using public access as proof of redistribution rights.
- Sending the user's private context to discovery providers.
- Routing unrelated third-party traffic through Argus devices.

## 8. Required source-review record

Before a candidate is activated, its registry record must include:

- Exact source and API/endpoint or dataset identifier.
- Terms/license URL and review date.
- Allowed operations: discovery, fetch, transient processing, full-text retention, excerpt/metadata retention, retrieval index, training and redistribution.
- Item-level rights strategy and UNKNOWN handling.
- Authentication and secret-handling decision.
- Rate/concurrency and response-size limits.
- Region, language and freshness scope.
- Parser/profile version and source-specific safety constraints.
- Provenance fields retained and deletion/revocation behavior.
- A test fixture and expected policy outcome for allow, conditional and deny cases.

## 9. Research evidence register

The following official pages were checked on 2026-11-03. Terms and API behavior can change; re-check them before implementation and before source activation.

| Source | Official evidence |
|---|---|
| OpenAlex | [API reference](https://help.openalex.org/api/); [license vocabulary](https://help.openalex.org/data/licenses/) |
| Crossref | [REST API](https://www.crossref.org/documentation/retrieve-metadata/rest-api/); [metadata licensing](https://www.crossref.org/documentation/retrieve-metadata/) |
| Wikidata | [Licensing](https://www.wikidata.org/wiki/Wikidata:Licensing); [data access](https://www.wikidata.org/wiki/Wikidata:Reuse) |
| Europeana | [API FAQ](https://europeana.atlassian.net/wiki/spaces/EF/pages/2360508417/Europeana+API+FAQ) |
| World Bank | [Indicators API](https://datahelpdesk.worldbank.org/knowledgebase/articles/889392); [data licenses](https://datacatalog.worldbank.org/public-licenses) |
| USDA FoodData Central | [API guide](https://fdc.nal.usda.gov/api-guide/); [download datasets](https://fdc.nal.usda.gov/download-datasets/) |
| PubMed Central | [OAI-PMH API](https://pmc.ncbi.nlm.nih.gov/tools/oai/); [PMC OA subset explanation](https://support.nlm.nih.gov/knowledgebase/article/KA-03253/en-us) |
| NASA Science Data | [Science data licenses](https://science.data.nasa.gov/about/license) |
| NOAA National Weather Service | [NWS disclaimer](https://www.weather.gov/disclaimer) |
| FAO | [Statistical database terms](https://www.fao.org/contact-us/terms/db-terms-of-use/en) |

## 10. Remaining source-selection decisions

- Which candidates become the first actively approved registry entries.
- Which additional authoritative sources fill the remaining gaps in all sixteen domains.
- Indonesian and other non-English official sources and their regional coverage.
- Rights interpretation for each planned neural-training use, especially where attribution, share-alike, database or additional contractual terms apply.
- API credential distribution model for providers that require keys.
- Device-tier download quotas and minimum offline storage.
- Parser and document-format support for each source family.
- Update schedules, freshness rules and source deprecation handling.

No source is considered approved merely because it appears in this research shortlist.
