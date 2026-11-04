# Curated Initial Text Seed Set

**Status:** Retrieval-pilot candidates and rights holds are distinguished per item; no item is approved for tokenizer fitting, artifact distribution or neural training  
**Research date:** 2026-11-05  
**Purpose:** Turn broad OER/source candidates into concrete, auditable text records for the first acquisition and parsing pilot

## 1. Selection principles

The first text pilot should use a small, manually reviewed selection rather than mirror entire catalogs. The selected titles demonstrate:

- Full-text or complete-book availability.
- Explicit item-level licensing metadata.
- More than one subject domain.
- English and Indonesian content.
- Different file and publishing paths.
- Provenance, attribution and quality handling.

A license statement on a catalog or landing page must be stored with the exact content URL and file checksum. A PDF that does not contain an embedded license notice must not lose the license evidence from its source record.

All entries below remain candidates, not activated tokenizer or neural-training sources. A declared CC BY license is recorded as **license evidence**, not as an Argus operation approval. Before fitting tokenizer frequencies or distributing a derived tokenizer artifact, the exact acquired edition/file, third-party exceptions, attribution payload, SHA-256 and the applicable operation-specific rights decision must be recorded. Neural training requires its own separate approval and remains disabled.

## 2. Candidate status

| Status | Meaning |
|---|---|
| CANDIDATE_FOR_REVIEW | Exact item and a license notice have been identified; rights and content audit remains |
| RETRIEVAL_PILOT_CANDIDATE | Suitable for a bounded local parsing/indexing test after source terms and fetch policy are approved |
| TRAINING_REVIEW_PENDING | May be evaluated for a future corpus only after legal and data-quality review |
| HOLD_METADATA_MISMATCH | Source metadata or license evidence conflicts and must be resolved |
| HOLD_SHAREALIKE | Share-alike obligations require separate model-training analysis |
| REJECTED | Not to be acquired for the proposed operation |

No status here activates a runtime connector or grants an automatic training entitlement.

## 3. Open Textbook Library candidates

### SEED-OTL-001 — Programming for Computations: Python

- **Record:** https://open.umn.edu/opentextbooks/uki/textbooks/programming-for-computations-python-a-gentle-introduction-to-numerical-simulations-with-python-3-6
- **Authors:** Svein Linge and Hans Petter Langtangen.
- **Publisher / year:** Springer, 2020.
- **Language:** English.
- **Declared license:** CC BY on the Open Textbook Library item record. The publisher's Springer Nature open-access chapter pages for this book explicitly state CC BY 4.0; the inspected chapter-rights pages also state that separately credited third-party material may require its own permission.
- **Available formats shown:** eBook, PDF and online.
- **Rights evidence reviewed 2026-11-04:** [Springer Nature chapter rights example](https://link.springer.com/chapter/10.1007/978-3-030-16877-3_4) confirms CC BY 4.0 at chapter level. This is stronger than relying only on the OTL catalogue label, but is not a complete exception-by-exception audit of every page/asset in the downloadable book PDF.
- **Subject mapping:** KD-02 Mathematics and Statistics; KD-05 Computing and Digital Technology; KD-06 Engineering and Applied Technology.
- **Content value:** introduces programming as a method for solving mathematical problems, numerical simulation, functions, testing, integration and differential equations.
- **Limitations:** the title specifies Python 3.6. Treat version-specific code and library guidance as historical, not current implementation advice.
- **Status:** RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library record](https://open.umn.edu/opentextbooks/uki/textbooks/programming-for-computations-python-a-gentle-introduction-to-numerical-simulations-with-python-3-6).

### SEED-OTL-002 — Introduction to the Social Sciences

- **Record:** https://open.umn.edu/opentextbooks/uki/textbooks/introduction-to-the-social-sciences
- **Authors:** Zoe Staines, Gerhard Hoffstaedter and Ned Binnie.
- **Publisher / year:** University of Queensland, 2023.
- **Language:** English.
- **Declared license:** the Open Textbook Library record says CC BY; the publisher's [University of Queensland Pressbooks title page](https://uq.pressbooks.pub/introduction-social-sciences/) identifies CC BY 4.0, **except where otherwise noted**.
- **Available formats shown:** eBook, online, PDF and XML.
- **Rights evidence reviewed 2026-11-04:** the publisher page confirms the CC BY 4.0 title-level declaration and identifies text, video and online materials in the book. Every explicit exception and embedded media/third-party asset must still be inventoried before tokenizer fitting or artifact distribution.
- **Subject mapping:** KD-11 Society, Governance and Law; KD-14 Arts, Literature and Culture; KD-15 Education and Research Methods.
- **Content value:** multidisciplinary introduction covering social-science methods, sociology, anthropology, political science, public policy, psychology, human geography, institutions, environment, health, development and work.
- **Limitations:** many examples and policy contexts are Australian or global-comparative. Preserve regional context; do not generalize country-specific findings.
- **Status:** RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library record](https://open.umn.edu/opentextbooks/uki/textbooks/introduction-to-the-social-sciences).

### SEED-OTL-003 — Sociology in Everyday Life: An Equity Lens

- **Record:** https://open.umn.edu/opentextbooks/textbooks/1924
- **Declared license:** original content CC BY 4.0, except where otherwise noted.
- **Language:** English.
- **Subject mapping:** KD-11 Society, Governance and Law; KD-13 Philosophy, Religion and Ethics.
- **Content value:** concepts and methods for understanding social life, groups, networks, interaction, institutions and social structures.
- **Limitations:** the book states that it uses an intersectional feminist approach and focuses on contemporary sociology in the United States. This is one perspective and regional frame, not a universal social taxonomy. Inspect each exception and embedded asset.
- **Status:** CANDIDATE_FOR_REVIEW; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library record](https://open.umn.edu/opentextbooks/textbooks/1924).

### SEED-OTL-004 — Applied Combinatorics

- **Record:** https://open.umn.edu/opentextbooks/textbooks/339
- **Authors:** Mitchel T. Keller and William T. Trotter.
- **Declared license:** CC BY-SA 4.0.
- **Subject mapping:** KD-02 Mathematics and Statistics; KD-05 Computing and Digital Technology.
- **Content value:** combinatorics, discrete probability, graph-related applications and mathematical reasoning.
- **Limitations:** share-alike obligations apply to adaptations; the impact on any model-training corpus, model weights and distributed artifacts requires separate legal analysis.
- **Status:** HOLD_SHAREALIKE for neural training; retrieval use requires attribution and compliance review.
- **Evidence:** [Open Textbook Library record](https://open.umn.edu/opentextbooks/textbooks/339).

### SEED-OTL-005 — Biology 2E, Part I (2nd Edition)

- **Record:** https://open.umn.edu/opentextbooks/textbooks/1340
- **Declared license in description:** CC BY 4.0, unless otherwise noted in the text.
- **Subject mapping:** KD-04 Life Sciences.
- **Content value:** introductory biology material.
- **Metadata issue:** the catalog record title says "Part I", while the descriptive text refers to "Biology 2e Part II, 2nd edition". The mismatch must be resolved against the publisher's actual file and edition before acquisition.
- **Status:** HOLD_METADATA_MISMATCH.
- **Evidence:** [Open Textbook Library record](https://open.umn.edu/opentextbooks/textbooks/1340).

## 4. Indonesian ITATS OER candidates

The Program Studi Sistem Informasi ITATS OER collection lists individual records with CC BY 4.0 labels and language metadata. The individual landing page is the license-evidence record; preserve it alongside the PDF. The downloadable PDFs inspected do not show a machine-readable CC BY notice in their extracted text, so the collector must retain the landing-page rights evidence and verify any third-party components before using the content.

### SEED-ID-001 — Pemrograman Internet of Things

- **Landing page:** https://itats.ac.id/sisteminformasi/oer/pemrograman-internet-of-things/
- **PDF:** https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Ajar_Pemrograman_Internet_of_Things.pdf
- **Author:** Isa Albanna, S.Si., M.Si.
- **Publisher / program:** Program Studi Sistem Informasi, Institut Teknologi Adhi Tama Surabaya.
- **Edition:** first edition, academic year 2026/2027.
- **Language / format:** Indonesian PDF; 66 pages; approximately 1 MB according to the item page.
- **Declared item license:** CC BY 4.0 on the individual landing page; the current ITATS OER catalogue also labels the item CC BY 4.0.
- **Rights evidence reviewed 2026-11-04:** [individual item page](https://itats.ac.id/sisteminformasi/oer/pemrograman-internet-of-things/) and [ITATS OER catalogue](https://itats.ac.id/sisteminformasi/oer/).
- **Audit state (reviewed 2026-11-05):** the individual item page declares CC BY 4.0 and exposes a 66-page substantive preview PDF. The package's separate material/handout entries are labelled `[DUMMY]` / file not selected; do not treat those package entries as available corpus content. The preview's inspected title/identity page contains no restrictive reproduction notice, but this is not a complete third-party-content or legal audit.
- **Operation status:** preview PDF is a bounded retrieval-pilot candidate only. The exact downloaded-byte SHA-256, page/figure/code/quotation inventory, complete attribution, and separate permission record for tokenizer fitting and derived-artifact distribution remain pending. Keep `TRAINING_REVIEW_PENDING`; do not activate tokenizer training.
- **Content value:** IoT architecture, ESP32 and RP2040 Pico, sensors, MQTT/HTTP/CoAP, Node-RED, InfluxDB, cloud middleware, TLS and TinyML.
- **Content audit note:** the references include commercial Packt books and other cited material. A citation does not itself establish that third-party text or figures were reproduced, but the PDF's images, diagrams, code samples and quoted material must be checked before training. Preserve the landing-page license statement, author attribution and exact PDF checksum.
- **Status:** RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [ITATS item metadata and license](https://itats.ac.id/sisteminformasi/oer/pemrograman-internet-of-things/); [PDF file](https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Ajar_Pemrograman_Internet_of_Things.pdf).

### SEED-ID-002 — Transformasi Digital

- **Landing page:** https://itats.ac.id/sisteminformasi/oer/transformasi-digital/
- **PDF:** https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Buku_Modul_Transformasi_Digital_ITATS.pdf
- **Author:** Adib Pakarbudi, S.Kom., M.Kom.
- **Publisher / program:** Program Studi Sistem Informasi, Institut Teknologi Adhi Tama Surabaya.
- **Edition:** first edition, academic year 2026/2027.
- **Language / format:** Indonesian PDF; 32 pages; approximately 854 KB according to the item page.
- **Declared item license:** CC BY 4.0 on the individual landing page; the current ITATS OER catalogue also labels the item CC BY 4.0.
- **Rights evidence reviewed 2026-11-04:** [individual item page](https://itats.ac.id/sisteminformasi/oer/transformasi-digital/) and [ITATS OER catalogue](https://itats.ac.id/sisteminformasi/oer/).
- **Audit state (reviewed 2026-11-05):** the item landing page and catalogue declare CC BY 4.0, but the separate 32-page preview PDF contains a copyright notice prohibiting reproduction of part or all of the module without written permission from the author and ITATS Information Systems Study Program. The package's material entries are also labelled `[DUMMY]` / file not selected.
- **Rights conflict:** the landing-page CC BY 4.0 declaration and the PDF's written-permission restriction are materially inconsistent. This register does not decide which statement controls. Obtain written clarification from the author/rightsholder and institution before copying, storing, indexing, retrieving or using the PDF.
- **Subject mapping:** KD-05 Computing and Digital Technology; KD-11 Society, Governance and Law; KD-12 Economics, Business and Finance; KD-15 Education and Research Methods.
- **Content value:** digital transformation, strategic frameworks, organizational change, digital maturity and enterprise architecture; content value does not override the rights hold.
- **Content audit note:** named frameworks and external standards must be treated as concepts or citations, not a blanket license for third-party source texts.
- **Status:** HOLD_RIGHTS_CONFLICT; NOT_ACTIVATED. Excluded from retrieval-pilot ingestion and all tokenizer/training manifests pending written resolution.
- **Evidence:** [ITATS item metadata and license](https://itats.ac.id/sisteminformasi/oer/transformasi-digital/); [PDF file](https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Buku_Modul_Transformasi_Digital_ITATS.pdf).

### SEED-ID-003 — Pemrograman Berorientasi Objek

- **Landing page:** https://itats.ac.id/sisteminformasi/oer/pemrograman-berorientasi-objek/
- **PDF:** https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Ajar_Pemrograman_Berorientasi_Objek.pdf
- **Authors:** Budanis Dwi Meilani, S.T., M.Kom. and Ressa Uttungga, S.Kom., M.Kom.
- **Publisher / program:** Program Studi Sistem Informasi, Institut Teknologi Adhi Tama Surabaya.
- **Edition:** first edition, November 2025.
- **Language / format:** Indonesian PDF; 64 pages.
- **Declared item license:** CC BY 4.0 is shown on the individual ITATS item page and in the OER catalogue.
- **File availability (reviewed 2026-11-05):** the item page identifies a substantive `01. Materi Utama PDF` rather than a dummy package entry. Its 64-page preview PDF is publicly readable; the separate download action returned HTTP 403 in this audit, so an independently acquired original file and SHA-256 have not been established.
- **Rights-text check:** the reviewed PDF text/cover-title pages did not reveal the same blanket reproduction restriction seen in SEED-ID-002. This is not a complete page-by-page or third-party asset clearance.
- **Subject mapping:** KD-05 Computing and Digital Technology; KD-15 Education and Research Methods.
- **Content value:** Java SE practice, class modeling, object-oriented programming principles and GUI application development.
- **Content audit note:** preserve the individual item page and license evidence. Review diagrams, examples, code and third-party content; do not treat all referenced Java documentation as part of the same license.
- **Status:** RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING. Tokenizer fitting and derived-artifact distribution remain unauthorized pending exact-file, attribution and operation-specific review.
- **Evidence:** [ITATS OER catalog](https://itats.ac.id/sisteminformasi/oer/); [ITATS item page](https://itats.ac.id/sisteminformasi/oer/pemrograman-berorientasi-objek/); [PDF file](https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Ajar_Pemrograman_Berorientasi_Objek.pdf).

## 5. Initial retrieval-pilot recommendation

Use a compact cross-domain sample to validate acquisition, parsing, attribution, storage and retrieval:

1. SEED-OTL-001 — English programming and numerical-methods textbook; source/file/exception and use-specific rights review still pending.
2. SEED-OTL-002 — English social-science textbook; item-level CC BY 4.0 is qualified by exceptions and still needs full asset/use review.
3. SEED-ID-001 — Indonesian IoT module preview PDF; limited retrieval-pilot candidate, with dummy package add-ons excluded.
4. SEED-ID-003 — Indonesian object-oriented programming module; substantive preview available, with exact download/checksum and full rights/asset review pending.

SEED-ID-002 — Transformasi Digital is excluded and held for a written rights clarification because its PDF reproduction restriction conflicts with the CC BY 4.0 item-page declaration.

SEED-OTL-003 may be added after its "except otherwise noted" sections and third-party assets are mapped. SEED-OTL-004 remains on share-alike legal hold. SEED-OTL-005 remains on metadata hold until its part/edition mismatch is resolved.

This is a retrieval and document-ingestion pilot, not a foundation-training corpus. It should test whether Argus can parse full text, preserve source citations, classify language/domain, and search useful passages. Training remains disabled for these items pending the separate legal-use, corpus quality, numerical training and evaluation gates.

## 6. Item-level metadata to preserve

For each selected work, record:

- Stable work ID and immutable edition identifier.
- Title, author/contributors, publisher/institution and publication date.
- Language and subject/domain assignments.
- Landing-page URL and exact content URL.
- License name, version and license URL.
- Exact page section where the item-level license is declared.
- Retrieval timestamp, file size and SHA-256 checksum.
- Parser version, page count and extraction warnings.
- Third-party asset/excerpt exceptions and their disposition.
- Attribution string and source citation.
- Retrieval status and training-eligibility state.
- Taxonomy version and acquisition-run ID.

For ITATS items, the landing page is part of the rights record. If the file changes while the page metadata does not, Argus must treat the new checksum as a new content version and re-run the rights/content checks.

## 7. Quality review before any training

The selected titles are teaching materials, not a balanced general-language corpus. They are concentrated in academic and technical subject matter. They cannot alone provide broad everyday language, conversational competence, cultural coverage or multilingual coverage.

Before training:
- Inspect the complete text for extraction quality, duplicated passages, missing pages, malformed tables and OCR errors.
- Identify figures, tables, code and embedded third-party material.
- Deduplicate against the bundled bootstrap corpus and other acquired records.
- Record subject and language coverage to avoid over-weighting technical content.
- Create separate held-out evaluation material; never train on validation/test records.
- Confirm exact-license obligations and use-specific AI-training treatment.
- Measure actual token count with the selected Argus tokenizer rather than estimating from PDF page count.
- Keep training-corpus versions immutable and checksummed.

## 8. Open gaps after this seed set

These sample titles improve technical, social-science and Indonesian educational coverage. The broader Indonesian OER candidate set is recorded in [31 — Indonesian-Language OER Source Expansion](31_INDONESIAN_LANGUAGE_OER_SOURCE_EXPANSION.md). Substantial gaps remain:

- Indonesian language, grammar, literature and linguistics.
- Core mathematics outside programming-oriented numerical methods.
- Physics, chemistry and broad life-science foundations.
- Geography, history and cultural knowledge across regions.
- Philosophy, ethics and comparative religion.
- Arts, literature and music.
- Health sciences from high-quality, appropriately licensed sources.
- Agriculture, environment and earth sciences.
- Everyday practical knowledge and conversational language diversity.

Discovery must continue to prioritize broad foundational coverage rather than repeatedly collecting more programming and information-systems books. Additional mathematics, science, geography, philosophy and history candidates are recorded in [30 — Domain-Balanced Text Source Expansion](30_DOMAIN_BALANCED_TEXT_SOURCE_EXPANSION.md).

## 9. Evidence checked

The following specific item pages and files were examined on 2026-11-03:

- [Open Textbook Library — Programming for Computations: Python](https://open.umn.edu/opentextbooks/uki/textbooks/programming-for-computations-python-a-gentle-introduction-to-numerical-simulations-with-python-3-6)
- [Open Textbook Library — Introduction to the Social Sciences](https://open.umn.edu/opentextbooks/uki/textbooks/introduction-to-the-social-sciences)
- [Open Textbook Library — Sociology in Everyday Life: An Equity Lens](https://open.umn.edu/opentextbooks/textbooks/1924)
- [Open Textbook Library — Applied Combinatorics](https://open.umn.edu/opentextbooks/textbooks/339)
- [Open Textbook Library — Biology 2E, Part I](https://open.umn.edu/opentextbooks/textbooks/1340)
- [ITATS OER — Pemrograman Internet of Things](https://itats.ac.id/sisteminformasi/oer/pemrograman-internet-of-things/)
- [ITATS OER — Transformasi Digital](https://itats.ac.id/sisteminformasi/oer/transformasi-digital/)
- [ITATS OER — Pemrograman Berorientasi Objek](https://itats.ac.id/sisteminformasi/oer/pemrograman-berorientasi-objek/)

Source pages and files can change. Re-check item metadata, rights and checksums immediately before a connector fetch or training-corpus build.
