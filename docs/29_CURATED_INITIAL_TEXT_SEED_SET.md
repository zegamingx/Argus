# Curated Initial Text Seed Set

**Status:** Item-level research completed for selected records; no item is approved for neural training  
**Research date:** 2026-11-03  
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

All entries below are **CANDIDATE_FOR_RETRIEVAL_AND_TRAINING_REVIEW**, not activated training sources. Neural training must remain disabled until the legal-use review, third-party-content inspection, parser validation and corpus acceptance gates pass.

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
- **Declared license:** CC BY.
- **Available formats shown:** eBook, PDF and online.
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
- **Declared license:** CC BY.
- **Available formats shown:** eBook, online, PDF and XML.
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
- **Declared item license:** CC BY 4.0 on the individual landing page.
- **Subject mapping:** KD-05 Computing and Digital Technology; KD-06 Engineering and Applied Technology.
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
- **Declared item license:** CC BY 4.0 on the individual landing page.
- **Subject mapping:** KD-05 Computing and Digital Technology; KD-11 Society, Governance and Law; KD-12 Economics, Business and Finance; KD-15 Education and Research Methods.
- **Content value:** digital transformation, strategic frameworks, organizational change, digital maturity and enterprise architecture.
- **Content audit note:** named frameworks and external standards must be treated as concepts or citations, not a blanket license for third-party source texts. Inspect all diagrams, tables and embedded material. Retain title, author, institution, landing-page license evidence and file checksum.
- **Status:** RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [ITATS item metadata and license](https://itats.ac.id/sisteminformasi/oer/transformasi-digital/); [PDF file](https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Buku_Modul_Transformasi_Digital_ITATS.pdf).

### SEED-ID-003 — Pemrograman Berorientasi Objek

- **Landing page:** https://itats.ac.id/sisteminformasi/oer/pemrograman-berorientasi-objek/
- **PDF:** https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Ajar_Pemrograman_Berorientasi_Objek.pdf
- **Authors:** Budanis Dwi Meilani, S.T., M.Kom. and Ressa Uttungga, S.Kom., M.Kom.
- **Publisher / program:** Program Studi Sistem Informasi, Institut Teknologi Adhi Tama Surabaya.
- **Edition:** first edition, November 2025.
- **Language / format:** Indonesian PDF; 64 pages.
- **Declared item license:** CC BY 4.0 shown in the ITATS OER catalog entry.
- **Subject mapping:** KD-05 Computing and Digital Technology; KD-15 Education and Research Methods.
- **Content value:** Java SE practice, class modeling, object-oriented programming principles and GUI application development.
- **Content audit note:** preserve the specific catalog item and license evidence. Review diagrams, examples, code and third-party content; do not treat all referenced Java documentation as part of the same license.
- **Status:** RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [ITATS OER catalog](https://itats.ac.id/sisteminformasi/oer/); [ITATS item page](https://itats.ac.id/sisteminformasi/oer/pemrograman-berorientasi-objek/); [PDF file](https://itats.ac.id/sisteminformasi/wp-content/uploads/sites/6/2026/08/Modul_Ajar_Pemrograman_Berorientasi_Objek.pdf).

## 5. Initial retrieval-pilot recommendation

Use a compact cross-domain sample to validate acquisition, parsing, attribution, storage and retrieval:

1. SEED-OTL-001 — English programming and numerical-methods textbook.
2. SEED-OTL-002 — English social-science textbook.
3. SEED-ID-001 — Indonesian IoT textbook/module.
4. SEED-ID-002 — Indonesian digital-transformation book.
5. SEED-ID-003 — Indonesian object-oriented programming module.

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

These sample titles improve technical, social-science and Indonesian educational coverage, but substantial gaps remain:

- Indonesian language, grammar, literature and linguistics.
- Core mathematics outside programming-oriented numerical methods.
- Physics, chemistry and broad life-science foundations.
- Geography, history and cultural knowledge across regions.
- Philosophy, ethics and comparative religion.
- Arts, literature and music.
- Health sciences from high-quality, appropriately licensed sources.
- Agriculture, environment and earth sciences.
- Everyday practical knowledge and conversational language diversity.

Discovery must continue to prioritize broad foundational coverage rather than repeatedly collecting more programming and information-systems books.

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
