# Knowledge Domain and Discovery Strategy

**Status:** Accepted initial taxonomy and discovery policy; implementation and numerical calibration remain open  
**Last reviewed:** 2026-11-03

## 1. Purpose

This document defines how Argus organizes general knowledge and decides what public information to acquire next.

The goal is broad, continuously expandable general knowledge for each independent Argus instance. The initial taxonomy provides a navigable map, not a closed curriculum or a claim that Argus already understands every listed subject.

The taxonomy is used by the Autonomous Knowledge Acquisition Engine to identify coverage gaps, create discovery topics, prioritize work and track acquisition progress. It is independent of the neural architecture and must remain usable before the local model is capable of directing its own research.

## 2. Accepted design rules

1. **Broad baseline:** general acquisition must cover multiple major areas of human knowledge instead of optimizing only for programming, technology or the owner's most frequent topic.
2. **Expandable taxonomy:** domains, subdomains and topics can be added, split, merged or retired through versioned taxonomy changes.
3. **Hierarchical and connected:** knowledge is organized in a hierarchy but may have dependencies and cross-domain relationships.
4. **Breadth before excessive depth:** first establish reliable core coverage across domains, then deepen important topics and update time-sensitive knowledge.
5. **Evidence before volume:** source quality, provenance, rights and usefulness matter more than the number of downloaded pages or tokens.
6. **Deterministic bootstrap:** initial discovery uses versioned topic seeds and reviewed source policies; it does not depend on a capable neural model.
7. **Model-assisted proposals later:** the model may propose topics and questions after passing defined quality gates, but it cannot bypass acquisition policy.
8. **General/personal separation:** private user data and personal preferences do not silently influence the general knowledge map or general web queries.
9. **Local ownership:** the taxonomy state, topic queue, coverage map and acquired records belong to the individual Argus instance.

## 3. Initial general knowledge domains

The initial taxonomy uses sixteen broad top-level domains:

| ID | Domain | Illustrative subdomains |
|---|---|---|
| KD-01 | Language and Communication | Linguistics, grammar, writing, reading, rhetoric, translation, communication |
| KD-02 | Mathematics and Statistics | Arithmetic, algebra, geometry, calculus, discrete mathematics, probability, statistics |
| KD-03 | Physical Sciences | Physics, chemistry, materials science, optics, thermodynamics, particle and space physics |
| KD-04 | Life Sciences | Biology, genetics, ecology, microbiology, evolution, neuroscience |
| KD-05 | Computing and Digital Technology | Programming, algorithms, software engineering, operating systems, databases, networks, AI, cybersecurity |
| KD-06 | Engineering and Applied Technology | Electrical, electronic, mechanical, civil, chemical, industrial, robotics and systems engineering |
| KD-07 | Health and Medicine | Anatomy, physiology, public health, clinical specialties, diagnostics, pharmacology and health research |
| KD-08 | Earth, Climate and Environment | Geology, meteorology, oceanography, climate science, ecosystems, conservation and environmental systems |
| KD-09 | Geography and Geospatial Knowledge | Physical and human geography, cartography, geospatial data, regions and place-based knowledge |
| KD-10 | History and Archaeology | World and regional history, archaeology, heritage, chronology, historical methods and material culture |
| KD-11 | Society, Governance and Law | Sociology, anthropology, political systems, public policy, civic institutions, legal systems and rights |
| KD-12 | Economics, Business and Finance | Micro/macro economics, accounting, markets, organizations, management, trade and personal finance concepts |
| KD-13 | Philosophy, Religion and Ethics | Logic, epistemology, metaphysics, moral philosophy, comparative religion and applied ethics |
| KD-14 | Arts, Literature and Culture | Visual arts, music, architecture, design, film, literature, languages in cultural context and cultural studies |
| KD-15 | Education and Research Methods | Learning science, pedagogy, scientific method, statistics in research, research design, evidence appraisal and information literacy |
| KD-16 | Agriculture, Food and Practical Skills | Agronomy, horticulture, animal husbandry, food science, cooking, textiles, repair and everyday practical systems |

These are organizational categories, not separate neural models. Topics may belong to more than one domain. For example, medical imaging can connect health, physics, computing and engineering.

The taxonomy must support regional, cultural and language diversity. It must not treat one country's institutions, legal system, history or terminology as globally universal.

## 4. Taxonomy structure

The canonical hierarchy is:

~~~text
KNOWLEDGE DOMAIN
    |
    +-- SUBDOMAIN
           |
           +-- TOPIC
                  |
                  +-- CONCEPT / QUESTION / LEARNING OBJECTIVE
                         |
                         +-- SOURCE RECORDS
                         +-- CLAIMS / RELATIONSHIPS
                         +-- RETRIEVAL INDEX
                         +-- TRAINING ELIGIBILITY
~~~

Each taxonomy node should have:

- Stable identifier and node type.
- Parent identifiers and optional cross-domain links.
- Canonical label, aliases and language metadata.
- Definition and scope boundary.
- Version, lifecycle status and change rationale.
- Prerequisite or dependency topics where useful.
- Coverage state and last assessment timestamp.
- Freshness policy where the subject changes over time.
- Links to source records and evidence used to assess coverage.

The hierarchy must not force all knowledge into a single parent. Cross-domain relationships are first-class links, with explicit relationship types such as prerequisite, application, related concept, contradiction or shared method.

Language is an attribute of a topic and its source records, not a top-level knowledge domain. Coverage reports must be able to distinguish a topic's language coverage instead of assuming English coverage proves global coverage.

## 5. Breadth-to-depth acquisition plan

Acquisition should progress through a staged coverage model:

| Level | Target | Example outcome |
|---|---|---|
| L0 — Domain orientation | Establish scope, core terminology and major branches | Argus can identify the subject's structure and vocabulary |
| L1 — Core foundations | Collect authoritative introductory explanations and prerequisite concepts | Foundational concepts and relationships are available for retrieval |
| L2 — Methods and applications | Add established methods, systems, processes and practical applications | The knowledge base can support more specific questions |
| L3 — Advanced specialization | Acquire deeper technical or scholarly material where justified | Specialized concepts and evidence become available |
| L4 — Current developments | Track new standards, discoveries, revisions and time-sensitive changes | The knowledge base can identify what changed and when |

L0–L2 breadth across the taxonomy takes precedence over collecting large volumes of L3 material in a small number of easy-to-index domains. This is a scheduling principle, not a fixed quota or a guarantee of equal knowledge depth.

The initial pass must prioritize clear, reliable foundations and prerequisite relationships. Specialist depth grows after the engine can measure baseline coverage and identify meaningful gaps.

## 6. Discovery planning loop

The planner operates on a durable local knowledge-coverage map.

~~~text
TAXONOMY + CURRENT KNOWLEDGE INVENTORY
                 |
                 v
          COVERAGE ASSESSMENT
                 |
                 v
       GAP / FRESHNESS DETECTION
                 |
                 v
       TOPIC AND QUESTION EXPANSION
                 |
                 v
        DISCOVERY PRIORITIZATION
                 |
                 v
        SOURCE RESOLUTION + POLICY
                 |
                 v
          BOUNDED ACQUISITION
                 |
                 v
       QUALITY / PROVENANCE REVIEW
                 |
                 v
        KNOWLEDGE MAP REASSESSMENT
                 |
                 +------> REPEAT
~~~

The planner should assess coverage at topic and language level. A topic is not "covered" merely because one page mentions its name. Coverage requires enough high-quality, attributable material to support the declared learning objective.

Coverage states should distinguish at least:

- **Unknown:** no useful evidence has been collected.
- **Discovered:** relevant sources or candidate records have been identified.
- **Collected:** candidate material has been fetched.
- **Validated:** provenance, rights and quality checks passed.
- **Indexed:** approved knowledge is available for retrieval.
- **Training eligible:** material is approved for a separately versioned training corpus.
- **Stale:** previously valid material needs a freshness review.
- **Contested:** credible sources disagree and the conflict remains unresolved.
- **Excluded:** policy, rights, sensitivity or quality rules prohibit use.

These are knowledge-pipeline states, not claims about the neural model's mastery.

## 7. Topic prioritization

The planner must not choose topics only because they are popular, easy to search or likely to produce many pages.

A topic priority score may combine:

- **Coverage gap:** how much of the topic's declared learning objective remains unsupported.
- **Foundational value:** whether the topic is a prerequisite for many other topics.
- **Cross-domain value:** how many useful domain relationships depend on it.
- **Freshness need:** how likely existing information is to be outdated.
- **Source quality and availability:** whether eligible, authoritative sources can be reached.
- **Current evidence quality:** whether existing sources are weak, incomplete or contradictory.
- **Acquisition cost:** expected network, CPU, memory, parsing and storage work.
- **Risk and policy constraints:** rights uncertainty, sensitivity, source restrictions and quality concerns.

An abstract scoring model is:

~~~text
priority(topic) =
    coverage_gap
  + foundational_value
  + cross_domain_value
  + freshness_need
  + evidence_improvement_potential
  - acquisition_cost
  - policy_and_quality_risk
~~~

Each component must be normalized and calibrated using observed results before numerical weights are selected. Do not hard-code arbitrary universal percentages at the design stage.

The scheduler must preserve broad-domain exploration so that a high-volume domain cannot monopolize acquisition. It should also avoid repeatedly fetching near-duplicate material when other topics have larger evidence gaps.

## 8. Discovery inputs and source strategy

The initial planner uses deterministic inputs:

- The versioned taxonomy and topic seed catalog.
- A curated source registry with source-specific access and rights policies.
- Known prerequisite and cross-domain topic relationships.
- Local coverage gaps and stale-record notices.
- Source update feeds, sitemaps and documented public APIs.
- Explicitly requested public topics, when the owner asks Argus to research them.

Candidate discovery mechanisms include search-provider interfaces, official documentation indexes, RSS/Atom feeds, sitemaps, public APIs, open repositories and reviewed open datasets. Each mechanism is a replaceable connector behind the source policy.

Search results are discovery hints, not automatically trusted knowledge. The engine must resolve and validate the underlying source before storing a record. A public URL is not automatically redistributable or training-eligible.

The initial source catalog should prefer primary and authoritative material for the subject. Secondary sources can add explanation or corroboration. Community content may help discover questions or candidate references but must not silently replace primary evidence for claims requiring high confidence.

## 9. Model-assisted discovery maturity

Model assistance is introduced gradually:

### Stage A — Deterministic bootstrap

A static, versioned taxonomy, reviewed topic seeds and curated source registry start acquisition. The model is not trusted to direct network activity.

### Stage B — Evidence-assisted planning

The planner uses the coverage map, retrieval failures, contradiction records and source freshness information to generate follow-up topics using deterministic rules. Human-curated policy remains authoritative.

### Stage C — Model-proposed discovery

After the model and retrieval pipeline meet defined quality gates, the model may propose missing concepts, follow-up questions, relationships and candidate sources. Proposals are treated as untrusted data, checked against taxonomy and source policy, and placed into a reviewable queue.

### Stage D — Continuous maintenance

The engine revisits changing topics based on source update events, freshness policies, known contradictions and measured coverage gaps. It may add new subdomains or cross-domain links through a versioned taxonomy change.

At every stage, the model may suggest work but cannot grant itself network permission, ignore source policy, expand quotas or use private personal data as general-search context.

## 10. High-impact and time-sensitive subject handling

Medicine, law, finance, public safety and other high-impact topics require stricter source and freshness rules.

For these topics, the engine should prefer relevant official authorities, primary publications and qualified institutional sources; retain jurisdiction and publication/update dates; distinguish established evidence from preliminary findings; preserve disagreements; and avoid presenting retrieved content as individualized professional advice.

Legal information must be indexed with jurisdiction and effective dates. Medical guidance must preserve issuing authority, publication date and evidence context. Financial information must retain market/date context. Old material must not be silently presented as current.

## 11. Language and regional coverage

The acquisition pipeline should be language-aware from the beginning, even while the bootstrap corpus candidate is currently English-only.

The planner must track:

- Detected language and script.
- Original language of the source.
- Whether a translation is present and how it was produced.
- Regional or jurisdictional scope.
- Whether retrieval can preserve the original text and source attribution.
- Whether the current tokenizer and model can reliably process that language.

Translation must not erase the original source or be treated as independent corroboration. The engine must not claim language competence simply because it has stored documents in that language.

Supported-language priorities and minimum coverage targets remain open until tokenizer research, source quality and device capability are evaluated.

## 12. Separation from personal learning

The general taxonomy and discovery planner serve instance-local general knowledge. They do not automatically mine the owner's conversations, files, screens, contacts, browsing history or personal profile.

The owner may explicitly request research on a public topic. Such a request can create a general public-source acquisition task without turning the user's private context into training data.

Personal preferences, private documents and user-specific facts remain in the separate personal-learning and memory lifecycle. Any later design that uses personal context to personalize research requires a distinct permission scope and visible user control.

## 13. Acceptance criteria

The domain and discovery design is ready for implementation planning when:

- The initial taxonomy is versioned, hierarchical and extensible.
- All sixteen initial domains can be represented in the coverage map.
- Topics support subdomain membership and cross-domain relationships.
- Discovery starts from deterministic seeds without requiring model competence.
- Topic prioritization accounts for coverage, foundations, freshness, evidence quality, cost and risk.
- Breadth scheduling prevents one high-volume domain from monopolizing acquisition.
- Coverage states distinguish discovery, collection, validation, indexing and training eligibility.
- Language and regional coverage are tracked independently from domain coverage.
- High-impact and time-sensitive information retains source, jurisdiction and date context.
- Model-generated proposals cannot bypass source policy, network controls or resource budgets.
- General discovery remains separate from personal data and personal learning.
- All accepted records link to provenance and the taxonomy version used when they were classified.

## 14. Open calibration and research items

The taxonomy and broad-coverage policy are accepted as the starting design. These details remain open and must be calibrated with evidence:

- Exact subdomain inventory and taxonomy maintenance workflow.
- Supported language priorities and minimum evidence coverage.
- Numerical priority weights and starvation-prevention policy.
- Topic freshness intervals by domain.
- Initial authoritative source registry for every domain and jurisdiction.
- Source-specific rights and training-use eligibility.
- Quality-evidence thresholds and corroboration rules.
- Search-provider strategy, request metadata and operating cost.
- The model/retrieval quality gate required before Stage C.

Do not convert these open items into universal constants without source, device and evaluation evidence.
