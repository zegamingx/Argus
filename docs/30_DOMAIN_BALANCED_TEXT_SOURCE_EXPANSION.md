# Domain-Balanced Text Source Expansion

**Status:** Item-level source candidates researched; retrieval and neural-training approval remain separate  
**Research date:** 2026-11-03  
**Purpose:** Expand the initial text seed set beyond programming and information systems

## 1. Why this expansion is needed

The first curated text seed set is useful for testing acquisition and document parsing, but its subject distribution is too concentrated in computing, engineering and information systems. Argus's general-knowledge design requires coverage across all major domains, including mathematics, physical sciences, life sciences, geography, history and philosophy.

This document adds exact Open Textbook Library records with full-text formats and item-level CC BY declarations. They are candidates for a later retrieval pilot. The CC BY label does not by itself approve a record for Argus neural training: item-level rights, third-party assets, use-specific AI-training treatment, extraction quality and corpus policy still require review.

## 2. Candidate status

All candidates in this document are **NOT ACTIVATED**. The suggested order is for research and retrieval-pilot preparation, not an operational allowlist.

| Status | Meaning |
|---|---|
| DOMAIN_COVERAGE_CANDIDATE | Exact title and domain role identified |
| RETRIEVAL_PILOT_CANDIDATE | Suitable for bounded parsing and local-index tests after connector policy approval |
| TRAINING_REVIEW_PENDING | May be evaluated for a later training corpus after rights, content and quality review |
| HOLD | Known license, edition, scope or content issue prevents the proposed operation |

Do not ingest the Open Textbook Library's instructor reviews as book content. Some review text has a separate CC BY-ND notice even when the textbook itself is CC BY. The item parser must follow the book's actual content/download link and exclude catalog reviews, comments and unrelated ancillary material unless separately licensed.

## 3. Additional CC BY textbook candidates

### SEED-EXP-001 — Biology 2e

- **Record:** https://open.umn.edu/opentextbooks/textbooks/biology-2e
- **Authors:** Mary Ann Clark, Jung Choi and Matthew Douglas.
- **Publisher / copyright year:** OpenStax, 2018.
- **Language / formats:** English; online, PDF, hardcopy and audiobook.
- **Declared textbook license:** CC BY on the item record.
- **Domain mapping:** KD-04 Life Sciences; supporting links to KD-03 Physical Sciences and KD-07 Health and Medicine.
- **Coverage:** chemistry of life, cells, genetics, evolution, biological diversity, plant and animal structure/function, ecology.
- **Limitations:** the record is for the 2018 second edition. Reviewer feedback notes that some content/terminology may require current verification. Use it for foundational explanations, not as the sole source for current discoveries or clinical guidance. The book's figures and third-party assets must be audited separately.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — Biology 2e](https://open.umn.edu/opentextbooks/textbooks/biology-2e).

### SEED-EXP-002 — Chemistry 2e

- **Record:** https://open.umn.edu/opentextbooks/textbooks/219
- **Authors:** Paul Flowers, Klaus Theopold and Richard Langley.
- **Publisher / copyright year:** OpenStax, 2019.
- **Language / formats:** English; PDF, online and audiobook.
- **Declared textbook license:** CC BY on the item record.
- **Domain mapping:** KD-03 Physical Sciences; supporting links to KD-04 Life Sciences and KD-06 Engineering and Applied Technology.
- **Coverage:** introductory general chemistry and core two-semester chemistry topics.
- **Limitations:** establish the exact edition/file and inspect figures, tables, examples and any third-party material. Do not assume the license shown for the textbook covers catalog reviews or external ancillary content.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — Chemistry 2e](https://open.umn.edu/opentextbooks/textbooks/219).

### SEED-EXP-003 — College Physics 2e

- **Record:** https://open.umn.edu/opentextbooks/textbooks/college-physics
- **Authors:** Paul Peter Urone, Roger Hinrichs and Kim Dirks.
- **Publisher / copyright year:** OpenStax, 2022.
- **ISBN:** 9781951693602.
- **Language / formats:** English; online, PDF, hardcopy and eBook.
- **Declared textbook license:** CC BY on the item record.
- **Domain mapping:** KD-03 Physical Sciences; supporting links to KD-06 Engineering and Applied Technology and KD-07 Health and Medicine.
- **Coverage:** mechanics, energy, fluids, thermodynamics, waves, electricity, magnetism, optics, relativity, quantum physics and nuclear physics.
- **Limitations:** the catalog contains instructor reviews with their own license notices, including CC BY-ND. Those review texts are not part of the textbook and must not be ingested with it. Preserve textbook attribution separately from reviews. Source examples and standards may be U.S.-centric.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — College Physics 2e](https://open.umn.edu/opentextbooks/textbooks/college-physics).

### SEED-EXP-004 — Algebra and Trigonometry 2e

- **Record:** https://open.umn.edu/opentextbooks/textbooks/algebra-and-trigonometry
- **Author:** Jay Abramson.
- **Publisher / copyright year:** OpenStax, 2021.
- **ISBN:** 9781951693404.
- **Language / formats:** English; PDF, online and eBook.
- **Declared textbook license:** CC BY on the item record.
- **Domain mapping:** KD-02 Mathematics and Statistics; supporting links to KD-05 Computing and Digital Technology.
- **Coverage:** algebra, functions, equations, modeling, trigonometry and precalculus foundations.
- **Limitations:** it complements but does not replace a complete proof-based or advanced mathematics curriculum. Preserve examples and mathematical notation accurately during PDF/HTML extraction.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — Algebra and Trigonometry 2e](https://open.umn.edu/opentextbooks/textbooks/algebra-and-trigonometry).

### SEED-EXP-005 — The Essence of Mathematics Through Elementary Problems

- **Record:** https://open.umn.edu/opentextbooks/textbooks/the-essence-of-mathematics-through-elementary-problems
- **Authors:** Alexandre Borovik and Tony Gardiner.
- **Publisher / copyright year:** Open Book Publishers, 2019.
- **ISBN:** 9781783747016.
- **Language / formats:** English; PDF, hardcopy and online.
- **Declared license:** CC BY on the item record.
- **Domain mapping:** KD-02 Mathematics and Statistics; supporting links to KD-15 Education and Research Methods.
- **Coverage:** mathematical thinking, mental arithmetic, algebra, geometry, trigonometry, polygons, polyhedra and problem-solving.
- **Limitations:** the title emphasizes elementary problems and mathematical thinking. It is not a complete substitute for a full school-to-university mathematics sequence.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — The Essence of Mathematics Through Elementary Problems](https://open.umn.edu/opentextbooks/textbooks/the-essence-of-mathematics-through-elementary-problems).

### SEED-EXP-006 — Fundamentals of Human Geography

- **Record:** https://open.umn.edu/opentextbooks/textbooks/fundamentals-of-human-geography
- **Author:** Wing Cheung.
- **Publisher / copyright year:** Wing Cheung, 2023.
- **Language / formats:** English; PDF, Google Doc and online interactive text.
- **Declared license:** CC BY on the item record.
- **Domain mapping:** KD-09 Geography and Geospatial Knowledge; supporting links to KD-08 Earth, Climate and Environment; KD-10 History and Archaeology; KD-11 Society, Governance and Law; KD-13 Philosophy, Religion and Ethics.
- **Coverage:** population, health, migration, folk/popular culture, language, religion, ethnicity, political geography, development, agriculture, industry, settlements, environment and resources.
- **Limitations:** the PDF omits videos, audio, interactive maps/graphs and some image alt text. The web version carries additional learning material. If Argus uses the PDF alone, record that incomplete coverage; do not silently claim it contains all media content.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — Fundamentals of Human Geography](https://open.umn.edu/opentextbooks/textbooks/fundamentals-of-human-geography).

### SEED-EXP-007 — Introduction to Philosophy

- **Record:** https://open.umn.edu/opentextbooks/textbooks/1270
- **Authors:** Nathan Smith, Gregory Browne and Parish Conkling.
- **Publisher / copyright year:** OpenStax, 2022.
- **ISBN:** 9781951693596.
- **Language / formats:** English; PDF, online, hardcopy and audiobook.
- **Declared license:** CC BY on the item record.
- **Domain mapping:** KD-13 Philosophy, Religion and Ethics; supporting links to KD-11 Society, Governance and Law and KD-15 Education and Research Methods.
- **Coverage:** critical thinking, world philosophical history, logic, metaphysics, epistemology, value theory, ethics, political philosophy and contemporary thought.
- **Limitations:** the Open Textbook Library review notes that the book makes an effort to include Indigenous, Indian and Chinese philosophical perspectives. This remains one introductory selection and cannot represent all philosophical and religious traditions. Pair with regional and primary-source material.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — Introduction to Philosophy](https://open.umn.edu/opentextbooks/textbooks/1270).

### SEED-EXP-008 — World History, Volume 1: to 1500

- **Record:** https://open.umn.edu/opentextbooks/textbooks/world-history-volume-1-to-1500
- **Authors:** Ann Kordas, Ryan J. Lynch and Brooke Nelson.
- **Publisher:** OpenStax.
- **Language / formats:** English; PDF, online, hardcopy and audiobook.
- **Declared license:** CC BY on the item record.
- **Domain mapping:** KD-10 History and Archaeology; supporting links to KD-09 Geography and Geospatial Knowledge and KD-13 Philosophy, Religion and Ethics.
- **Coverage:** early human societies, ancient civilizations, the Mediterranean, Africa, Asia, the Americas, religious history, trade and societies up to 1500.
- **Limitations:** world-history surveys necessarily select and compress regions and events. The catalog review notes weaker coverage of Oceania compared with other regions. Argus must retain regional coverage metadata and supplement missing histories rather than treating one survey as comprehensive.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — World History Volume 1: to 1500](https://open.umn.edu/opentextbooks/textbooks/world-history-volume-1-to-1500).

### SEED-EXP-009 — World History, Volume 2: From 1400

- **Record:** https://open.umn.edu/opentextbooks/textbooks/world-history-volume-2-from-1400-volume-2
- **Authors:** Ann Kordas, Ryan J. Lynch, Brooke Nelson and Julie Tatlock.
- **Publisher / copyright year:** OpenStax, 2022.
- **ISBN:** 9781711471433.
- **Language / formats:** English; online, PDF and hardcopy.
- **Declared license:** CC BY on the item record.
- **Domain mapping:** KD-10 History and Archaeology; supporting links to KD-09 Geography and Geospatial Knowledge and KD-11 Society, Governance and Law.
- **Coverage:** global history from 1400, including early modern and modern political, economic and cultural developments.
- **Limitations:** use it as a second chronological volume, not as an independent source for every historical claim. Corroborate important dates, interpretations and regional accounts against primary sources and specialist research.
- **Status:** DOMAIN_COVERAGE_CANDIDATE; RETRIEVAL_PILOT_CANDIDATE; TRAINING_REVIEW_PENDING.
- **Evidence:** [Open Textbook Library — World History Volume 2: From 1400](https://open.umn.edu/opentextbooks/textbooks/world-history-volume-2-from-1400-volume-2).

## 4. Relevant candidates deliberately held from default neural training

These records can help Argus understand the source landscape, but their declared licenses create additional conditions:

| Title | Declared license | Disposition |
|---|---|---|
| Essentials of Linguistics, 2nd Edition | CC BY-NC-SA | HOLD from default general training; useful candidate for separately authorized non-commercial retrieval |
| Introduction to Earth Science, Second Edition | CC BY-NC-SA | HOLD from default general training; verify NC and SA implications |
| Modern World History | CC BY-NC-SA | HOLD from default general training |
| World History: Cultures, States, and Societies to 1500 | CC BY-SA | HOLD pending share-alike review |

Sources:
- [Essentials of Linguistics 2e](https://open.umn.edu/opentextbooks/textbooks/599)
- [Introduction to Earth Science 2e](https://open.umn.edu/opentextbooks/textbooks/1264)
- [Modern World History](https://open.umn.edu/opentextbooks/textbooks/949)
- [World History: Cultures, States, and Societies to 1500](https://open.umn.edu/opentextbooks/textbooks/375)

Do not treat HOLD as "forbidden to read" in every context. It means the current default neural-training pipeline must not ingest the work. Any retrieval or retention must still satisfy its license and source terms.

## 5. Expanded domain coverage map

| Domain | New text candidates | Remaining limitation |
|---|---|---|
| KD-02 Mathematics and Statistics | SEED-EXP-004, SEED-EXP-005 | Need probability, statistics, calculus, proofs and applied mathematics coverage |
| KD-03 Physical Sciences | SEED-EXP-002, SEED-EXP-003 | Need astronomy, optics, materials and specialized physics |
| KD-04 Life Sciences | SEED-EXP-001 | Need ecology, evolution, microbiology, genetics depth and current specialist sources |
| KD-08 Earth, Climate and Environment | Partial coverage through SEED-EXP-006 | Need a standalone earth/climate text with acceptable rights |
| KD-09 Geography and Geospatial Knowledge | SEED-EXP-006, history volumes | Need physical geography, cartography and regional geospatial sources |
| KD-10 History and Archaeology | SEED-EXP-008, SEED-EXP-009 | Need archaeology and region-specific primary sources |
| KD-11 Society, Governance and Law | Supporting chapters in SEED-EXP-006 and SEED-EXP-007 | Need jurisdiction-specific Indonesian primary texts and broader social science |
| KD-13 Philosophy, Religion and Ethics | SEED-EXP-007 | Need religious traditions, regional philosophy and primary texts |
| KD-15 Education and Research Methods | Methods and critical-thinking content in selected books | Need dedicated research-methods and education texts |

The expansion does not yet solve KD-01 language/linguistics, KD-14 arts/literature, KD-07 health/medicine, KD-12 economics/business or KD-16 agriculture/practical knowledge with balanced full-text sources. Those domains remain active discovery gaps.

## 6. Recommended next retrieval pilot batch

The first seed set (document 29) should remain a small cross-language parser test. This expansion is a second, domain-balancing candidate batch.

Recommended first selections for this next batch:
1. SEED-EXP-001 — Biology 2e.
2. SEED-EXP-002 — Chemistry 2e.
3. SEED-EXP-004 — Algebra and Trigonometry 2e.
4. SEED-EXP-006 — Fundamentals of Human Geography.
5. SEED-EXP-007 — Introduction to Philosophy.
6. SEED-EXP-008 — World History Volume 1.

Add College Physics and the two-volume mathematics/history companions based on storage budget and coverage needs. Downloading all long textbooks at once is not required. Each title should be fetched in bounded work and checked for PDF/HTML extraction quality and file size.

This batch remains a retrieval pilot. No model training is authorized by the selection list.

## 7. Quality, balance and source independence

A subject-balanced title list is not automatically a balanced training corpus. The corpus builder must measure:

- Actual token counts per title, chapter, domain, language and source.
- Duplicate and near-duplicate overlap with Small C4 and existing seeds.
- Chapter and section coverage, not only book-level record counts.
- Source and author concentration.
- Reading level and educational purpose.
- Regional and cultural framing.
- Age/version and subject-specific freshness.
- Third-party figures, tables, code, quotations and ancillary content.
- Held-out evaluation leakage.

Do not give one 1,200-page textbook the same training weight as a short 30-page module merely because both are one title. Conversely, raw token weighting alone may let a very large text dominate; the eventual sampler should combine domain-level balance, source diversity and a validated token budget.

## 8. Evidence checked

The source records were checked on 2026-11-03:
- [Biology 2e](https://open.umn.edu/opentextbooks/textbooks/biology-2e)
- [Chemistry 2e](https://open.umn.edu/opentextbooks/textbooks/219)
- [College Physics 2e](https://open.umn.edu/opentextbooks/textbooks/college-physics)
- [Algebra and Trigonometry 2e](https://open.umn.edu/opentextbooks/textbooks/algebra-and-trigonometry)
- [The Essence of Mathematics Through Elementary Problems](https://open.umn.edu/opentextbooks/textbooks/the-essence-of-mathematics-through-elementary-problems)
- [Fundamentals of Human Geography](https://open.umn.edu/opentextbooks/textbooks/fundamentals-of-human-geography)
- [Introduction to Philosophy](https://open.umn.edu/opentextbooks/textbooks/1270)
- [World History Volume 1: to 1500](https://open.umn.edu/opentextbooks/textbooks/world-history-volume-1-to-1500)
- [World History Volume 2: From 1400](https://open.umn.edu/opentextbooks/textbooks/world-history-volume-2-from-1400-volume-2)

The Open Textbook Library item pages display individual CC BY conditions for the nine named candidates. This is evidence of the declared textbook license, not a substitute for inspection of the source file, excluded assets, third-party components or any separate publisher notice. Re-check the current title page and file checksum before acquisition.
