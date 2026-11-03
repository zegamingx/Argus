# Open Text and Educational Source Candidates

**Status:** Research shortlist; item-level permission review required before activation  
**Research date:** 2026-11-03  
**Purpose:** Identify reusable full-text sources for general knowledge acquisition, with explicit boundaries between discovery, retrieval, retention and neural training

## 1. Why this document exists

The initial source registry contains valuable metadata and structured-data services, but metadata is not enough to build broad language and subject knowledge. Argus also needs well-structured books, course materials, educational texts, reference content and primary documents.

This document identifies candidate collections and records what is known about their access and licensing. It does not declare every book, course, chapter, file or page in a collection safe for every use.

The governing rule is:

**A resource being free to read, downloadable, described as open access, or available through an API does not automatically mean Argus may retain its full text or use it for neural training.**

UNESCO's OER definition describes openly licensed or public-domain educational material as allowing no-cost access, reuse, repurposing, adaptation and redistribution. Argus must still inspect the exact license and conditions attached to each individual resource. See [UNESCO's OER mandate](https://www.unesco.org/en/open-educational-resources/mandate).

## 2. Separate content permissions

For every content item, the source registry must record independent permissions for:

1. Discovering its metadata or URL.
2. Fetching the content.
3. Transiently processing it in memory.
4. Retaining full text.
5. Retaining excerpts and bibliographic metadata.
6. Building retrieval indexes, summaries or embeddings.
7. Including content in a neural-training corpus.
8. Exporting or redistributing the content or a derivative.

An item's license may allow some operations and restrict others. A collection-level license statement must not override a more specific item-level notice.

### 2.1 License handling policy

| License / rights state | Registry handling |
|---|---|
| CC0 / verified public domain | Lowest rights friction; still verify item identity, source terms, third-party components and jurisdiction |
| CC BY | Record creator, title, source, license and changes; training use still receives a use-specific legal review |
| CC BY-SA | Record attribution and share-alike obligations; keep training use on legal hold until model-weight and output obligations are reviewed |
| CC BY-NC | Non-commercial restriction; exclude from default Argus training until intended product/use is confirmed non-commercial or separate permission is obtained |
| CC BY-NC-SA | Non-commercial plus share-alike; hold from default training and require use-specific legal review |
| CC BY-ND / other no-derivatives terms | Do not transform or train by default; only use narrowly permitted discovery/metadata operations until reviewed |
| All rights reserved / no license / UNKNOWN | No full-text retention, retrieval indexing or training by default; use only metadata or links where permitted |

This table is a conservative product policy, not legal advice. Whether a particular form of model training, model weights, retrieval derivative or generated output constitutes a licensed adaptation or triggers a specific license condition requires jurisdiction- and use-specific legal analysis. Do not infer a universal answer from the license badge alone.

The Argus product distribution and monetization model is not yet fixed. Therefore, non-commercial material must not be silently mixed into a general corpus intended for a potentially commercial product.

## 3. Named full-text and OER candidates

All candidates below remain **NOT ACTIVATED** until an exact item, license, access method and operation scope are recorded in the source registry.

### TXT-001 — Open Textbook Library (Open Education Network)

- **Coverage:** higher-education textbooks across computing, mathematics, science, engineering, education, humanities, law, business and other subjects.
- **Discovery:** the library publishes cumulative MARC/CSV catalog records and JSON API access; its catalog metadata is stated to be public domain / CC0.
- **Content:** the library is a referatory to full textbook files hosted by authors or publishers. The Open Textbook Library requires openly licensed complete textbooks and accepts GNU and Creative Commons licenses, excluding ND components for newly accepted titles.
- **Item-level rule:** retrieve each record's book license and the publisher's source file. Do not assume all books use CC BY simply because the catalog is an open-textbook library.
- **Initial role:** high-priority textbook discovery source. Prefer specific CC0 or CC BY 4.0 titles for the first legal review. Keep SA titles on a separate review path and NC titles out of default general training.
- **Evidence:** [Open Textbook Library criteria](https://open.umn.edu/opentextbooks/books), [FAQ](https://open.umn.edu/opentextbooks/faq), and [catalog/API exports](https://open.umn.edu/opentextbooks/discovery).

### TXT-002 — Directory of Open Access Books (DOAB)

- **Coverage:** scholarly open-access books and chapters across disciplines and languages.
- **Discovery:** supports OAI-PMH and downloadable metadata exports; DOAB states its metadata feeds are CC0 1.0.
- **Content:** DOAB indexes books and links to publisher or repository download locations; it does not host the book full text itself.
- **Rights:** DOAB's FAQ states that included books have licenses permitting free sharing, but the exact license for each title governs what reuse is permitted.
- **Initial role:** preferred book-discovery catalog for identifying shareable scholarly books. Follow each download link to the source publisher and preserve the title-level license and provenance.
- **Evidence:** [DOAB metadata harvesting](https://www.doabooks.org/en/article/metadata), [DOAB researcher FAQ](https://www.doabooks.org/en/researchers/full-faq).

### TXT-003 — OAPEN Library

- **Coverage:** scholarly open-access books and chapters with full-text availability.
- **Discovery:** offers OAI-PMH, CSV, JSON and other metadata exports; OAPEN states its metadata feeds are CC0 1.0.
- **Content:** OAPEN provides full-text download links for titles where available.
- **Rights:** the OAPEN collection contains both items that are free to read and items that are free to share. DOAB includes the shareable subset; OAPEN itself must not be treated as a blanket reuse license.
- **Initial role:** full-text retrieval candidate after metadata discovery. Prefer records also indexed in DOAB, then verify the exact license and publisher terms for the selected book or chapter.
- **Evidence:** [OAPEN metadata](https://www.oapen.org/article/metadata) and [DOAB FAQ explaining the distinction](https://www.doabooks.org/en/researchers/full-faq).

### TXT-004 — Wikimedia Indonesia Education Materials

- **Coverage:** Indonesian-language educational materials, presentations, learning resources and community education content.
- **Rights:** the Wikimedia Indonesia Education materials page states that its materials may be downloaded and used under CC BY-SA 4.0.
- **Initial role:** candidate for Indonesian-language retrieval and knowledge coverage, subject to retaining attribution, source and share-alike information.
- **Training status:** HOLD for use-specific legal review. CC BY-SA permits commercial sharing and adaptation subject to conditions, but the application of share-alike obligations to model training and resulting weights must not be assumed.
- **Evidence:** [Wikimedia Indonesia — Materi](https://pendidikan.wikimedia.or.id/materi) and [CC BY-SA 4.0 deed](https://creativecommons.org/licenses/by-sa/4.0/).

### TXT-005 — SPADA Indonesia Digital Learning Materials

- **Coverage:** Indonesian higher-education courses and learning modules.
- **Rights:** a 2022 ministry digital-module innovation program required modules created under that specific program to use CC BY. This is not a blanket license for every course or every SPADA item.
- **Initial role:** discover individual modules and record institution, author, course, program provenance and exact license statement.
- **Training status:** a module explicitly verified as CC BY may enter the legal-review queue for training eligibility; ordinary SPADA course access alone is not permission to copy or train.
- **Evidence:** [SPADA Indonesia](https://spada.kemdiktisaintek.go.id/) and [2022 Digital Module Innovation Program requirements](https://www.kemdiktisaintek.go.id/announcement/article/tawaran-program-bantuan-inovasi-modul-digital-tahun-2022).

### TXT-006 — Kemendikdasmen Institutional Repository

- **Coverage:** Indonesian education, culture, research and technology outputs from the ministry's work units.
- **Access:** repository describes itself as an open-access institutional repository.
- **Rights:** its information page states that all materials use CC BY-NC 4.0 and require attribution; the stated license excludes commercial use.
- **Initial role:** candidate for non-commercial research and retrieval only where the intended use complies with the item/repository terms.
- **Training status:** NOT APPROVED for default general training while Argus's commercial status and the legal treatment of model training remain unresolved. Obtain separate permission for commercial use if required.
- **Evidence:** [Kemendikdasmen repository information and policy](https://repositori.kemendikdasmen.go.id/information.html).

### TXT-007 — Universitas Terbuka SUAKA Open Educational Resources

- **Coverage:** Indonesian higher-education learning resources, especially distance education and university subjects.
- **Rights:** the UT library states that most SUAKA materials are created by UT lecturers and licensed CC BY-NC-SA 3.0. The word "most" matters; it is not an item-level license for every resource linked from the library.
- **Initial role:** discover selected items and check the source page for exact ownership and license.
- **Training status:** HOLD from default general training because of non-commercial and share-alike conditions. Any use requires confirmation that the exact item and intended Argus distribution comply.
- **Evidence:** [Universitas Terbuka OER / SUAKA](https://pustaka.ut.ac.id/lib/oer/) and [UT library overview](https://pustaka.ut.ac.id/lib/).

### TXT-008 — Universitas Indonesia Open Courseware

- **Coverage:** Indonesian and English university course materials from UI academics.
- **Rights:** the OCW UI site says its materials use CC BY-NC-SA unless otherwise stated.
- **Initial role:** discovery and possible non-commercial educational retrieval after item-level verification.
- **Training status:** HOLD for default training due to NC-SA terms and unresolved model-training treatment. An item-specific alternative license may change its status.
- **Evidence:** [OCW UI](https://ocw.ui.ac.id/).

### TXT-009 — OpenStax Textbooks

- **Coverage:** structured textbooks in mathematics, science, social science and other university/secondary subjects.
- **Rights:** OpenStax's current licensing guidance states CC BY-NC-SA 4.0, including attribution, non-commercial and share-alike conditions.
- **Initial role:** high-quality educational reference candidate for non-commercial retrieval where conditions can be met.
- **Training status:** NOT APPROVED for default Argus training while commercial status and share-alike implications remain unresolved. A commercial reuse request is required for commercial use.
- **Evidence:** [OpenStax textbook licensing](https://help.openstax.org/s/article/Openstax-textbook-licensing-and-customization).

### TXT-010 — MIT OpenCourseWare

- **Coverage:** university course notes, readings, assignments, lecture materials and learning resources.
- **Rights:** MIT OCW states CC BY-NC-SA 4.0, requiring attribution, non-commercial use and share-alike for distributed adaptations.
- **Initial role:** candidate for non-commercial learning retrieval under the specific course's notices.
- **Training status:** HOLD from default general training; do not presume model weights or training outputs satisfy the license.
- **Evidence:** [MIT OpenCourseWare Terms and License](https://ocw.mit.edu/pages/privacy-and-terms-of-use/).

### TXT-011 — Saylor University Original Course Content

- **Coverage:** self-paced education and course frameworks across multiple domains.
- **Rights:** Saylor states its own original course outlines, syllabi and framing notes are generally CC BY, but its courses also curate third-party resources with varied licenses, including all-rights-reserved material. Saylor does not provide blanket course downloads or blanket reuse permission.
- **Initial role:** discover original Saylor-authored items only where their ownership and CC BY notice can be verified.
- **Training status:** item-level review. Do not ingest the assembled course or linked third-party resources as one licensed dataset.
- **Evidence:** [Saylor open-content policy](https://www.saylor.org/OpenContent) and [Saylor licensing information](https://saylor.org/LicensingInformation).

### TXT-012 — Project Gutenberg

- **Coverage:** historical literature and older public-domain or individually authorized e-books.
- **Rights:** Project Gutenberg warns that U.S. public-domain status may not establish public-domain status in other countries, and its own terms place responsibility on non-U.S. users to check local law.
- **Automated access:** the main website is intended for human users; automated access is restricted. Project Gutenberg provides a separate robot/mirror process and offline catalogs for compliant bulk retrieval.
- **Initial role:** NOT part of the first automatic acquisition pilot. Consider individual titles only after Indonesia-specific copyright review and use the documented mirror/catalog process rather than scraping the main website.
- **Training status:** item-level and jurisdiction review required; exclude from automated training until cleared.
- **Evidence:** [Project Gutenberg terms](https://www.gutenberg.org/policy/terms_of_use.html), [robot access policy](https://www.gutenberg.org/policy/robot_access.html), and [license guidance](https://www.gutenberg.org/policy/license).

### TXT-013 — Standard Ebooks

- **Coverage:** carefully formatted ebooks selected from works that have entered the United States public domain.
- **Rights:** Standard Ebooks' collections policy explicitly bases selection on U.S. public-domain entry and warns that the same book may not be public domain in other countries.
- **Initial role:** candidate bibliographic discovery only until each title is checked against applicable Indonesian copyright rules.
- **Training status:** NOT APPROVED as a bulk source. Do not infer global public-domain status from the collection's U.S. criteria.
- **Evidence:** [Standard Ebooks collection policy](https://standardebooks.org/contribute/collections-policy).

### TXT-014 — Indonesian Laws and Court Decisions

- **Coverage:** primary Indonesian legislation, regulations and court decisions.
- **Rights:** Article 42 of Indonesia's Law No. 28 of 2014 states that there is no copyright in laws and regulations and court decisions/judicial determinations, among other enumerated works.
- **Initial role:** high-priority Indonesian-language primary-source collection, fetched from official government or court repositories. Preserve official identifier, issuing body, promulgation/decision date, effective status, jurisdiction and amendment/revocation relationships.
- **Boundary:** this statement applies to the legal instruments and decisions themselves. Editorial commentary, annotations, privately created translations, compilations, platform databases, headnotes and explanatory articles may have separate rights. Do not assume every PDF or all material on a government website is unrestricted.
- **Training status:** exact-source and content-scope audit still required. Legal material is time-sensitive and must be indexed with current status and jurisdiction.
- **Evidence:** [WIPO Lex — Indonesian Law No. 28 of 2014](https://www.wipo.int/wipolex/en/legislation/details/15600), especially Articles 41–42.

## 4. Initial full-text source priority

This prioritization considers usable text availability, discoverability, license transparency and coverage value. It does not grant permission.

| Priority | Candidate | Recommended initial operation | Training disposition |
|---|---|---|---|
| P1 | Open Textbook Library | Harvest catalog metadata; select exact titles and download links | Review CC0/CC BY titles individually; SA and NC remain held |
| P1 | DOAB + OAPEN | Harvest metadata; resolve individual openly licensed books | Check exact book license and training-use implications |
| P1 | Indonesian laws and court decisions | Acquire official primary legal texts with status metadata | Candidate only after source and corpus audit |
| P2 | Wikimedia Indonesia Education | Acquire selected Indonesian learning materials | Hold pending share-alike/model-output legal review |
| P2 | SPADA program-specific CC BY modules | Discover and verify program provenance and item license | CC BY items may enter use-specific training legal review |
| P2 | Saylor original CC BY materials | Select only content demonstrably authored and licensed by Saylor | Item-level review; no assembled-course assumption |
| P3 | Kemendikdasmen repository | Discover and retain only within confirmed non-commercial scope | Hold from default training |
| P3 | SUAKA-UT and OCW UI | Discover course and item metadata | Hold from default training due NC-SA |
| P3 | OpenStax and MIT OCW | Discover structured textbook/course material | Hold from default training due NC-SA |
| P4 | Project Gutenberg and Standard Ebooks | Catalog discovery and manual title review | No bulk training until jurisdiction-specific clearance |

P1/P2 indicate research priority, not activation state or legal clearance.

## 5. Indonesia-specific copyright handling

The acquisition engine needs an explicit Indonesia source profile because the initial project is developed and tested from Indonesia and must not equate U.S. public-domain determinations with Indonesian status.

For Indonesian legal source ingestion:
- Prefer the official published law/regulation or court decision.
- Preserve the official document identifier and issuing institution.
- Capture promulgation, amendment, effective, repeal and decision dates where applicable.
- Resolve consolidated texts against their amending instruments.
- Keep court decisions separate from annotations, summaries and case commentary.
- Keep official Indonesian text separate from third-party translations.
- Treat legal status as time-sensitive and jurisdiction-bound.

Article 41 of Law No. 28 of 2014 also distinguishes ideas, procedures, concepts, findings and data from protected expression. Article 42 lists specific categories with no copyright, including laws/regulations and court decisions. This must not be expanded into a blanket claim that all government publications, government websites, databases or explanatory documents are public domain.

## 6. Corpus construction rules for open text

Every selected item must be converted into a normalized, provenance-preserving record before any training decision.

The pipeline must retain:
- Work ID, edition/version, author/editor and publisher.
- Canonical title, source URL and stable source identifier.
- Original language, publication date and revision date.
- Item-level license, rights holder and required attribution.
- Exact license URL/version and any additional terms.
- Content checksum and retrieval timestamp.
- File format, parser version and extraction warnings.
- Included/excluded sections and third-party component notices.
- Domain/topic assignments and taxonomy version.
- Retrieval permission and training-eligibility state.
- Deletion/revocation linkage to all derived artifacts.

Extract text separately from images, diagrams, tables, datasets and embedded multimedia. Those components may have different rights. Do not silently include an image or third-party figure merely because the surrounding chapter has an open license.

For books with chapters or appendices under different licenses, split the rights records at the smallest practical unit. If the parser cannot reliably preserve boundaries, hold the whole item rather than silently mixing rights.

## 7. Training-corpus assembly policy

The first open-text training corpus must be a deliberately selected subset, not a bulk mirror of every candidate collection.

Required gates:
1. Exact item and edition are identified.
2. Item license and additional terms are captured.
3. Planned local storage and training operations are separately reviewed.
4. Commercial/non-commercial status is resolved for any NC material.
5. Attribution and share-alike implications are resolved for BY/SA material.
6. Embedded third-party content is separated or excluded.
7. The text is parsed and quality-checked.
8. Duplicate and near-duplicate content is detected.
9. The work is assigned to domain/topic and language coverage.
10. Train/validation/test split policy avoids leakage.
11. The corpus version is immutable and checksummed.
12. Model training and evaluation lineage references the exact corpus version.

No candidate collection in this document is automatically approved for neural training. CC0/public-domain or CC BY items should be evaluated first because their rights restrictions are generally less complex, but still require exact-item verification and a use-specific review of model training, attribution and distribution.

The training pipeline must not fetch directly from a live book URL or train from an unreviewed queue.

## 8. Indonesian-language coverage gaps

The current source shortlist does not yet provide sufficient broad, high-quality Indonesian-language educational text under clearly reusable terms.

Near-term research should prioritize:
- SPADA materials with verifiable item-level CC BY provenance.
- Indonesian educational resources with explicit CC0 or CC BY licenses.
- Official legislation and court decisions from primary sources.
- Indonesian public-domain works after local term calculation and source verification.
- Multilingual OER textbooks with explicit language and reuse metadata.
- University repositories where individual works carry an explicit open license.

Do not assume BSE availability equals an open license. The digital textbook portal's free-download function is not by itself a sufficient rights statement. Each BSE title must be checked for its own copyright/license and intended reuse before local full-text retention or training.

## 9. Recommended acquisition pilot

The first pilot should validate the entire document pipeline using a small, manually reviewed sample:

1. Catalog discovery through Open Textbook Library and DOAB.
2. Resolve the publisher-hosted full-text file for selected titles.
3. Compare item-level license metadata with the actual book and publisher notice.
4. Fetch only selected documents through bounded HTTPS connectors.
5. Parse text while preserving chapter, author and license boundaries.
6. Store local records with checksums and provenance.
7. Verify local retrieval and source attribution.
8. Keep neural training disabled until separate rights and numerical evaluation gates pass.

In parallel, test one Indonesian primary-law ingestion path and one SPADA/Indonesian OER item with an explicit license. These tests validate language handling and local source governance; they do not authorize bulk collection.

## 10. Open decisions

- Whether Argus's eventual commercial distribution model permits using NC materials.
- Legal interpretation of machine-learning training, model weights and output attribution for each license family.
- Which CC0/CC BY open textbooks should form the first subject-balanced text set.
- Whether SA content can be used for training under an acceptable model/output compliance design.
- Exact SPADA course/module identifiers and CC BY program provenance.
- Source-specific Indonesian OER items with explicit CC0 or CC BY licenses.
- Current BSE title-level copyright notices and reuse rights.
- Indonesia copyright term review for each candidate public-domain work.
- Parser support for EPUB, accessible HTML, PDF, DOCX and complex tables.
- How much offline storage can be reserved for text packages per device tier.

These decisions must be recorded before activation. The absence of a decision is not permission.
