# Argus Documentation Governance and Change Control

**Status:** Active documentation-governance baseline; implementation is not authorized  
**Last reviewed:** 2026-11-04  
**Scope:** Repository documentation structure, source-of-truth hierarchy, decision authority, evidence classification, document lifecycle, cross-document change control and phase boundaries  
**Authority:** Applies across the repository; accepted product decisions remain governed by document 01 and the project owner  
**Dependencies:** Documents 01, 02, 15–21, 23, 33–46, docs/README.md and CONTRIBUTING.md  
**Boundary:** This document defines project rules. It does not select a model architecture, tokenizer, optimizer, device profile, source provider or implementation technology. It does not authorize source-code changes, builds, workflow execution or device testing.

## 1. Purpose

Argus has accumulated research, architecture contracts, implementation audits and decision records across many documents. This governance layer defines how those documents relate, which file owns each kind of truth, how a proposal becomes an accepted decision, and how changes propagate without creating contradictory copies.

The goal is not to maximize the number of documents. The goal is to make every requirement, decision, research result, implementation claim and unresolved question traceable to one authoritative location.

## 2. Governing principles

1. **One canonical owner per decision or fact.** Other documents may summarize it, but must link to its authoritative record rather than silently creating a second version.
2. **Evidence is not approval.** A paper, benchmark, upstream API, source-code audit or analytical estimate may support a proposal. It does not independently select an Argus design.
3. **A proposal is not a decision.** Proposed contracts, candidate lists, comparison shortlists and DEFER dispositions remain non-final until the required decision is explicitly accepted and recorded.
4. **A design is not an implementation.** Planned architecture does not prove that code exists; code existence does not prove correctness; a passing test proves only the exact scope tested.
5. **Gate closure is scoped.** Closing one research gate or accepting one planning baseline does not close dependent gates or authorize the next delivery phase unless the record says so explicitly.
6. **No silent scope expansion.** New architecture families, data sources, platforms, dependencies or workflows require a documented reason and the appropriate review.
7. **Preserve traceability.** Stable document IDs, links, evidence references and decision history must survive edits and supersession.

## 3. Repository structure and file responsibilities

### 3.1 Physical structure

The current numbered documents remain directly under docs/. Their numbering and filenames are stable identifiers used by internal links, research records and decision dependencies.

**Do not move documents into new subdirectories, rename numbered files, or renumber the series as part of ordinary documentation work.** A future physical reorganization requires a separate approved migration plan containing a complete old-path/new-path map, inbound-link audit, compatibility redirects or equivalent references, and a single reviewed change.

This rule keeps the current repository navigable while allowing the index to group documents logically. A logical workstream is a navigation category, not a new directory or an independent source of authority.

### 3.2 Root-level files

| Path | Responsibility | Must not become |
|---|---|---|
| README.md | Concise project identity, approved high-level direction, current prototype caveat and navigation to key documents | A second detailed architecture specification or a competing ADR register |
| CONTRIBUTING.md | Contributor rules, change workflow, review expectations and testing requirements for authorized implementation work | The canonical home of technical research or product decisions |
| docs/README.md | Canonical document index, logical workstream map, shared status vocabulary and completion navigation | A replacement for the detailed documents it indexes |
| docs/01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md | Product requirements, accepted system constraints and the sole canonical ADR register | A dump of every research note or implementation finding |
| docs/16_ROADMAP_RESEARCH_REGISTER.md | Delivery phases, research IDs, gate state, dependencies, unresolved work and next documentary outputs | A second copy of full technical analysis or an unrecorded decision authority |
| docs/17_CURRENT_REPOSITORY_BASELINE.md | Dated facts observed in the repository at a named revision | A statement of intended future behavior |
| docs/18_IMPLEMENTATION_AUDIT.md | Traceable implementation findings, severity, evidence and gaps | A design proposal presented as existing behavior |
| docs/19_P0_REMEDIATION_DESIGN.md | Acceptance contracts for P0 remediation and the evidence needed to close findings | Authorization to implement those contracts |
| docs/20–43 and 45 | Topic-specific product, research, architecture, data, lifecycle, project-structure and decision-gate contracts within each document's declared scope | A competing global decision register |
| docs/44_DOCUMENTATION_GOVERNANCE_AND_CHANGE_CONTROL.md | Repository-wide documentation and change-control rules | A source of new product or model-architecture decisions |
| docs/46_ANDROID_SOURCE_TREE_OWNERSHIP_AND_DEPENDENCY_AUDIT.md | Audited file ownership, observed source dependencies, package graph and migration-priority findings | Permission to move files or modify application/build source |
| docs/47_PACKAGE_INTERFACE_AND_DEPENDENCY_CONTRACT_MATRIX.md | Proposed contract-owner, consumer, provider, state-authority and dependency mapping for target packages; explicit open interface gaps | Creation of Kotlin interfaces or implementation authorization |
| docs/48_PERSONAL_CAPTURE_AND_EXPERIENCE_INTAKE_CONTRACT.md | Proposed personal-capture ownership, surface/purpose consent, intake routing, review, retention, revocation, deletion and AccessibilityService continuity | Authorization to enable a capture surface, sever AccessibilityService or implement personal-data handling |
| docs/45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md | Proposed Android package organization, logical-subsystem ownership crosswalk, dependency direction, Gradle module extraction criteria, Kotlin/native boundaries and file-placement rules | Authorization to create modules, move source files or change build configuration |

The application source tree records the prototype implementation. It is not the authority for future product intent. Conversely, a design document cannot be cited as proof that a source-tree capability exists.

### 3.3 Logical workstreams

The following grouping is for navigation and ownership only. It does not change paths or document authority.

| Workstream | Documents | Primary responsibility |
|---|---|---|
| Product charter and system boundaries | 01–02 | Requirements, accepted decisions, architecture boundaries and system flows |
| Hardware, runtime and platform evidence | 03–08, 12 | Processor/GPU/NPU, SoC, memory, runtime and platform constraints |
| Model, learning, safety and verification foundations | 09–15 | Model, training, knowledge, governance, performance and verification requirements |
| Program control and implementation truth | 16–19 | Roadmap, research register, repository baseline, implementation audit and P0 remediation |
| Genesis and core learning direction | 20–23 | Initial model creation, device adaptation, general/personal learning and architecture research |
| Knowledge acquisition and corpus governance | 24–32 | Discovery, domains, item-level rights, source candidates and device-adaptive acquisition |
| Feasibility and pre-implementation foundations | 33–37 | From-scratch feasibility, tokenizer, Android evidence, module/lifecycle contracts and optimizer research |
| Architecture comparison and G4 preparation | 38–43 | Cross-family synthesis, component/init/state/tensor contracts and the G4 decision framework |
| Documentation governance | 44 | Document authority, evidence/status semantics and change control |
| Android package structure, source audit and interface contracts | 45–47 | Target package ownership, observed source dependencies, contract consumers/providers, Gradle seams and open interface gaps |
| Personal capture and personal-learning intake governance | 48 | Surface/purpose-specific consent, intake owner, memory/experience custody, review, retention, revocation and service continuity |

## 4. Source-of-truth hierarchy

When documents disagree, resolve the disagreement in this order:

1. **Explicit project-owner direction.** A clear newer direction from the project owner can change an earlier assumption. Record the change in the appropriate ADR before downstream documents are treated as updated.
2. **Accepted ADRs in document 01.** These define accepted product constraints and cross-system decisions. An ADR is authoritative only within its stated scope and status.
3. **Accepted domain contracts.** A topic document is authoritative for its detailed subject only to the constraints in document 01 and the decision scope it cites.
4. **Research and evidence documents.** Documents 03–15 and 20–43 may establish facts, comparisons, candidate designs and recommendations. Research conclusions do not silently become product decisions.
5. **Program status in document 16.** This is the canonical view of phase, research-register and gate status. It points to the detailed evidence but does not replace it.
6. **Implementation truth in documents 17–19 and exact verification records.** These describe inspected code and test results. They cannot redefine product intent merely because the prototype behaves differently.
7. **Summaries and contributor guidance.** README.md, docs/README.md and CONTRIBUTING.md summarize and route readers to the authorities above. If a summary conflicts with an authoritative record, correct the summary.

A domain document may define a necessary contract inside its scope, but it must identify its upstream authority and downstream dependencies. Where a cross-document conflict is discovered, do not choose whichever wording appears newest or most convenient. Log the conflict, identify the owning document, and resolve it through the decision process in Section 8.

## 5. Content classification and normative language

Every material statement must be recognizable as one of the following:

- **Observed fact:** directly inspected in a named repository revision, official specification, upstream source or dataset record.
- **Published evidence:** a result or claim from a cited paper, benchmark or technical publication, within that source's actual test conditions.
- **Analytical estimate:** a calculation derived from stated inputs, formulas and assumptions; it is not a measurement.
- **Assumption:** an unresolved premise used to continue analysis; it must have an owner or follow-up and must not be reported as verified.
- **Proposal:** a candidate design or recommendation awaiting decision.
- **Accepted decision:** a choice explicitly recorded with an accepted status in the appropriate ADR.
- **Verification result:** an observed pass/fail result tied to an exact source revision, method and environment.

Use normative terms consistently:

- **MUST / MUST NOT** — mandatory project requirement within the document's declared scope.
- **SHOULD / SHOULD NOT** — strong default; any deviation requires a recorded reason.
- **MAY** — permitted option, not a requirement.

A MUST in a narrow technical contract cannot override an accepted product constraint or an explicit project-wide rule.

## 6. Evidence and claim rules

### 6.1 Evidence classes

Use explicit evidence classes where claims could otherwise be confused:

| Class | Meaning | Minimum traceability |
|---|---|---|
| PUBLISHED_MEASUREMENT | Published experiment or benchmark result | Source, model/data/device/runtime, method, date and transfer limitations |
| UPSTREAM_SPEC_OR_SOURCE | Official API, SDK, standard or inspected upstream implementation | Exact version/release, relevant API or source path, access date and platform scope |
| REPOSITORY_OBSERVATION | Fact found in the Argus repository | Commit SHA, path and relevant symbol/section or line range |
| ANALYTICAL_ESTIMATE | Reproducible calculation rather than physical measurement | Formula, input values, units, assumptions and uncertainty |
| OWNER_ASSUMPTION | Temporary premise not yet accepted as a decision | Explicitly marked assumption and linked resolution item |
| DEVICE_VERIFICATION | Test or measurement on a physical device | Exact device/SoC, OS/build, ABI, runtime/driver, app commit, workload, method, result and limitations |

### 6.2 Evidence boundaries

1. A published inference or fine-tuning result MUST NOT be represented as proof of random-initialization training.
2. A server GPU result MUST NOT be represented as an Android result.
3. API availability, chipset branding, theoretical TOPS or successful library loading MUST NOT be represented as verified accelerator execution.
4. An analytical memory/compute estimate MUST NOT be represented as a device measurement.
5. A repository audit MUST identify the inspected commit. If code changes, the finding must be rechecked before it is called current.
6. Dataset, tokenizer and source-rights claims MUST identify the exact work/revision and the operation being considered. Discovery, fetch, transient processing, retention, tokenizer fitting, neural training and redistribution are distinct permissions.
7. Missing evidence MUST be labeled unknown or open. Do not fill it with a plausible value.

## 7. Status models — keep them separate

Document lifecycle, decision acceptance, implementation status and gate status are different axes. Never use one as a substitute for another.

### 7.1 Decision status

| Status | Meaning |
|---|---|
| PROPOSED | A recommendation or option is under review; not accepted |
| ACCEPTED_DIRECTION | A product/system direction has been accepted, but detailed implementation values may remain open |
| ACCEPTED_PLANNING_BASELINE | A bounded planning disposition or research scope is accepted; implementation is not implied |
| SELECTED_FOR_IMPLEMENTATION_PLANNING | A design has passed its stated pre-implementation decision gate and may enter a separately reviewed planning step |
| REJECTED | The option is not accepted for the stated scope; rationale and scope must be recorded |
| SUPERSEDED | A later accepted record replaces this decision; preserve a link to the successor |

### 7.2 Implementation and capability status

Use the shared vocabulary in docs/README.md:

- IMPLEMENTED means code exists; correctness is not implied.
- VERIFIED means a specified test passed on the exact named configuration.
- AVAILABLE means an external product/API/SDK exists; Argus integration is not implied.
- PLANNED means accepted design intent, not implementation.
- EXPERIMENTAL means prototype or limited integration.
- FUTURE RESEARCH means no delivery commitment.
- DEPRECATED and UNSUPPORTED describe lifecycle/support posture, not test success.

Add precision where needed: IMPLEMENTED_UNVERIFIED, REFERENCE_ONLY, AVAILABLE_UPSTREAM or VERIFIED_ON_[EXACT_CONFIGURATION]. A status must never imply more evidence than was produced.

### 7.3 Gate status

Use OPEN, BLOCKED, IN_PROGRESS and CLOSED only for a named gate or work item. A CLOSED gate entry MUST state exactly what scope was closed and link the acceptance evidence/decision. For example, closing a family shortlist does not mean a backbone was selected; closing a component-disposition baseline does not mean a component was implemented.

A document being written, reviewed or merged does not automatically close the gate it discusses.

## 8. Architecture Decision Record (ADR) rules

1. Document 01 is the sole canonical ADR register. Do not create a second ADR numbering series in topic documents.
2. Allocate ADR IDs sequentially and never reuse an ID, including after rejection or supersession.
3. Every material cross-system or product decision MUST record: title, date, status, context/problem, constraints, options considered, decision, rationale, consequences, unresolved dependencies, scope and supersession relationship where applicable.
4. A recommendation in a research document remains PROPOSED until the project owner accepts it and the decision is recorded in document 01.
5. ACCEPTED_DIRECTION and ACCEPTED_PLANNING_BASELINE are not equivalent to SELECTED_FOR_IMPLEMENTATION_PLANNING.
6. An accepted decision MUST NOT be silently edited into a different decision. Create a successor ADR, identify what it supersedes, and preserve the historical rationale.
7. A local implementation detail may be owned by a domain contract when it does not alter a cross-system decision. If it changes an accepted boundary or creates a new cross-system dependency, escalate it to document 01.
8. Every ADR must identify downstream documents that require reconciliation.

## 9. Document lifecycle and required metadata

A research/design document normally moves through:

1. **Question registered:** document 16 records the research ID, reason, dependencies and expected output.
2. **Evidence gathered:** the owning document records sources, methods, assumptions, contradictions and transfer limits.
3. **Synthesis prepared:** the document states candidate options, what is known, what remains open and the implications.
4. **Decision reviewed:** when a decision is needed, the owner accepts or rejects it through an ADR in document 01.
5. **Dependencies reconciled:** affected domain contracts and document 16 are updated; docs/README.md remains navigable.
6. **Readiness audited:** unresolved requirements and implementation assumptions are listed. Passing this review authorizes only the next explicitly stated phase.

Every substantial numbered document SHOULD declare:

- Status and last-reviewed date.
- Scope and explicit boundary.
- Upstream dependencies and authoritative inputs.
- Evidence/source method where applicable.
- Accepted conclusions separated from proposals and open questions.
- Downstream documents or decisions affected.
- Verification obligations that cannot be closed by documentary evidence alone.

A document SHOULD answer one coherent question. Split it only when the new document has a distinct owner, lifecycle or downstream use; avoid creating near-duplicate documents that merely restate another file.

## 10. Cross-document change propagation

Update the canonical owner first, then reconcile only the documents affected by the change.

| Change type | Canonical update | Required propagation |
|---|---|---|
| Product boundary or cross-system decision | Document 01 ADR | Reconcile affected contracts, document 16 status/dependencies and README.md only when the high-level project description changes |
| Research evidence or comparison | Owning research document | Update the relevant research-register row in document 16; update an ADR only after owner acceptance |
| Detailed technical contract | Owning domain document | Update direct dependents and document 16 if gate state, dependency or phase status changes |
| Repository implementation fact | Documents 17/18 | Cite the exact commit and reconcile document 15 or 19 where verification/remediation criteria are affected |
| P0 acceptance/remediation contract | Document 19 | Link the source finding in document 18 and the required verification in document 15; do not claim closure before evidence exists |
| Source, corpus or tokenizer rights | Exact item/source record in the relevant data document | Reconcile documents 22 and/or 26–34 as applicable, plus the research register; never infer permission from a catalog-level label |
| Documentation structure or navigation | Document 44 and/or docs/README.md | Update all affected internal links and root README navigation where relevant |
| Android package/module/file placement | Document 45 | Reconcile document 36 logical ownership, document 17 observed baseline and document 46 audit before any separately authorized migration |
| Android package/interface ownership and dependency mapping | Document 47 | Reconcile documents 36, 45 and 46; keep unresolved API gaps explicit and do not infer source/module authorization |
| Personal capture and experience-intake contract | Document 48 | Reconcile documents 13, 22, 36 and 47; update package mapping only where ownership changes; do not infer capture enablement, source severance or implementation authorization |
| Android source ownership/dependency audit | Document 46 | Update document 45 target ownership and document 17 implementation baseline only when new source facts materially change |
| Contributor workflow | CONTRIBUTING.md, under this document | Keep the rule consistent with the current phase and authorization boundary |

Do not update every document merely to synchronize dates or repeat a new paragraph. Prefer links to the canonical owner. When a summary must repeat a decision, label it as a summary and keep its wording consistent with the ADR.

## 11. Gate, phase and verification separation

The project uses different labels for different purposes:

- **Phase 0–6** in document 16 describes the product delivery/research sequence.
- **G0–G4** describes architecture/research decision gates and their bounded outputs.
- **G1-D and G1-V** distinguish documentary tokenizer closure from later runtime/conformance verification.
- **E1–E7** describes later physical-device feasibility and resource verification.

These labels are not interchangeable and must not be collapsed into one progress percentage. A gate can close only for its declared scope. Physical verification cannot be claimed from published evidence or analytical estimates. Conversely, a documentary research gate does not need physical device testing when its criteria are explicitly documentary.

A gate closure record must state:
- the exact gate and scope;
- acceptance criteria and evidence links;
- decisions made and decisions explicitly left open;
- remaining dependencies;
- what the closure permits next;
- what it does not authorize.

G4 closure can support an implementation-plan review only where document 43 and the accepted ADR say so. It does not independently authorize source changes, builds, workflow runs, artifact publication or device testing.

## 12. Current authorization boundary

As of 2026-11-04, the active work scope is documentation governance, evidence completion, research synthesis and cross-document consistency.

The current baseline recorded by documents 16 and 43 is:

- G2 is closed only for the four-family comparison shortlist.
- G3 is closed only for the initial component-disposition planning baseline; DEFER remains unresolved.
- G1-D remains OPEN.
- G4 remains BLOCKED pending its declared inputs.
- No backbone, final tokenizer profile, optimizer or model profile has been selected.
- Android execution support and physical-device feasibility remain unverified.
- Implementation has not been authorized.

Within this documentation-only scope, work MAY:
- inspect repository structure and current documentation;
- research, cite and classify evidence;
- draft and reconcile requirements, decision records, research contracts and indexes;
- identify unresolved decisions and define later verification obligations;
- update documentation through the agreed repository workflow.

Work MUST NOT:
- modify application source, Gradle/build configuration, native code, runtime behavior or feature logic;
- implement a design merely because a document describes it;
- run builds, tests, GitHub Actions/workflows or physical-device experiments as a substitute for documentary completion;
- upload build artifacts, model weights or datasets;
- activate an unapproved data source or treat a candidate corpus as approved training material;
- claim implementation, device support, performance or quality without the specified evidence.

Any transition beyond documentation-only work requires a separate, explicit project-owner authorization and a reviewed, dependency-ordered implementation plan. No gate closure or assistant recommendation grants that authorization by implication.

## 13. Documentation completion checklist

The documentation baseline is ready for an implementation-plan review only when all applicable items are true:

- [ ] Every requirement and accepted cross-system decision has one canonical owner.
- [ ] Accepted decisions are recorded in document 01; proposals are visibly distinguished.
- [ ] Every open question and gate dependency is reflected in document 16.
- [ ] Detailed contracts identify their upstream authority and downstream dependencies.
- [ ] Research claims identify evidence class, source/version/date and transfer limits.
- [ ] Analytical estimates expose formulas, inputs, units and assumptions.
- [ ] Repository facts cite the inspected commit; verification results cite the exact tested configuration.
- [ ] Data/source permissions are operation-specific and item-level where required.
- [ ] README.md, docs/README.md and CONTRIBUTING.md do not contradict the canonical documents.
- [ ] Internal links, document IDs and cross-references are valid.
- [ ] Remaining implementation assumptions and physical verification obligations are explicit.
- [ ] The next phase is authorized separately; documentation completion alone does not start implementation.

## 14. Change history

- 2026-11-04 — Initial governance baseline added to establish document hierarchy, authority, evidence/status rules and documentation-only change control.
