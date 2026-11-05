# PC-OPEN-01 — Personal Capture and Experience Intake Contract

**Status:** Proposed design contract — documentation only  
**Review date:** 2026-11-05  
**Repository baseline:** `main` at `fab7da5a0ce86d7265a7be152f550a4b524741cd`  
**Scope:** Ownership, surface registration, routing, consent, review, retention, revocation, deletion and the memory–personal-training boundary for personal-capture inputs  
**Upstream authority:** [01 — System Requirements and Decisions](01_SYSTEM_REQUIREMENTS_AND_DECISIONS.md), [13 — Security and Data Governance](13_SECURITY_PRIVACY_DATA_GOVERNANCE.md), [22 — General Corpus and Personal Learning](22_GENERAL_CORPUS_AND_PERSONAL_LEARNING.md), [36 — Pre-Implementation Module and Lifecycle Contracts](36_PRE_IMPLEMENTATION_MODULE_AND_LIFECYCLE_CONTRACTS.md), [45 — Android Project Structure and File-Placement Rules](45_ANDROID_PROJECT_STRUCTURE_AND_FILE_PLACEMENT_RULES.md), and [47 — Package Interface and Dependency Contract Matrix](47_PACKAGE_INTERFACE_AND_DEPENDENCY_CONTRACT_MATRIX.md)  
**Boundary:** This contract defines obligations for every current or future capture surface, including a replacement for the current AccessibilityService path. It creates no Kotlin interface/class, chooses no DI framework or storage engine, creates no Gradle module, changes no source/build configuration, and authorizes no implementation, build, test, workflow or device experiment.

---

## 1. Purpose and scope

Argus may receive personal experience through screen content, accessibility events, user-selected files, camera input, sensors or other device-level sources. This contract defines the controlled intake boundary that every such input must cross before it can become transient inference context, inspectable memory or a candidate for personal parameter training.

The contract applies to:

- Every current or future capture surface, including the prototype AccessibilityService path.
- Every personal input that has not explicitly and validly been designated as general-corpus material.
- The handoff from a platform capture adapter to the personal-experience intake workflow.
- The handoffs from intake to inspectable memory and from an approved personal-training candidate to the learning workflow.
- Existing personal records and pending candidates that must be reconciled when a capture adapter is replaced, disabled or removed.

It does not govern bundled general-corpus data or public-source acquisition. Those paths retain their own source provenance, rights and eligibility contracts. Personal content must never be reclassified as general-corpus data merely to bypass this boundary.

The governing principle is: **access to a device surface is not consent to retain its content, and retention is not consent to train on it.**

## 2. Ownership and responsibility

### 2.1 Accountable contract owner

The proposed accountable workflow owner is **`application/knowledge`**, which owns personal-experience intake orchestration and the memory-facing path. It is responsible for:

- Registering and validating the declared capture surface and its requested purpose.
- Coordinating purpose-specific consent and policy checks before accepting an input.
- Owning the intake-session state, classification, retention assignment and provenance handoff.
- Owning the review workflow that determines whether a retained personal experience record may be proposed for personal-training use.
- Maintaining the permitted-surface registry in coordination with `core/policy` and the durable policy records.
- Ensuring that any new capture surface is registered with a reviewed consent boundary before it can write to the personal-experience store.

This is an accountable logical owner under the existing package hierarchy, not a claim that an `application/knowledge` package or API already exists. No new `application/experience` package is created or authorized by this document. If later implementation evidence shows that intake needs a distinct workflow package, that change must be reviewed against document 45 rather than introduced speculatively.

### 2.2 Current provisional capture path

Until the current AccessibilityService dependency is severed, the prototype AccessibilityService is treated as the **provisional capture ingress and capture-session producer**. It is the de-facto source of current accessibility observations, but it is not the owner of consent policy, retention policy, training eligibility, memory state or durable review decisions.

Its provisional status means:

- It has no standing permission to expand the surfaces, apps, events, content or purposes it captures.
- The existence or availability of an accessibility event is not evidence of user consent.
- It may not choose retention, approve training candidacy or send raw observations directly to a training queue.
- It must provide enough bounded metadata for the intake owner to identify the declared surface, source instance, capture request/session, observation time and applicable consent reference.
- Any existing content or candidate created by the prototype path remains subject to reconciliation under Sections 8 and 9.

A replacement capture path inherits these obligations. It does not become an authority merely because it is the only available implementation.

### 2.3 Separation of owners

| Responsibility | Owner | Boundary |
|---|---|---|
| Personal-experience intake and memory-routing contract | `application/knowledge` | Owns intake workflow, classification, retention assignment and review orchestration; cannot grant consent to itself |
| Purpose/consent rules and fail-closed decisions | `core/policy` | Owns policy semantics and decision contracts, not Android capture or data persistence |
| Durable consent grants, revocations, review evidence and exclusions | `data/policy` | Persists versioned records; does not independently authorize an operation |
| Android capture surfaces and OS integration | `platform/android/capture` | Produces bounded observations and platform evidence; does not decide retention or training permission |
| Personal experience and inspectable-memory records | `core/knowledge` contracts and `data/knowledge` | Owns record identity, provenance, retention state and durable content where permitted |
| Personal-training admission and session lifecycle | `application/learning` | Revalidates approved candidate records and owns training-session admission; cannot bypass intake review |
| Immutable personal dataset/candidate manifest | `data/corpus` under `core/training` / `core/policy` contracts | Contains only explicitly approved record revisions and permission lineage |
| User-facing consent/review controls | Owning feature presentation packages under `feature/*/presentation` | Renders state and submits user intent; does not own policy or authoritative review state |
| Composition and dependency wiring | `app/bootstrap` | Wires implementations to contracts; does not own intake decisions or mutable workflow state |

The application/knowledge workflow owns the intake review gate. The application/learning workflow owns the subsequent training-session admission and must independently validate the approval and authorization records before it accepts a candidate. A capture adapter cannot review or approve its own input.

## 3. Surface declaration and eligibility

### 3.1 Surface declaration

Before a capture session begins, the adapter must declare a versioned surface identity and bounded request context. The logical declaration includes:

- Stable capture-surface ID and surface type.
- Adapter/provider identity and version.
- Requested operation/purpose.
- Applicable application, package, file, sensor or other source scope where relevant.
- User/instance identity and capture-session/request identity.
- Start time and the required visible active-state indication.
- Applicable OS permission/authorization observation.
- Requested resource and content bounds.

These are logical fields, not a finalized serialization schema or Kotlin data class. Android `Context`, `Intent`, `Uri`, `Bitmap`, CameraX objects, accessibility node objects and other framework objects do not cross into core/application domain contracts.

### 3.2 Registration and allowlisting

Every surface must be registered and reviewed before it is enabled. Registration defines its permitted source scope, permitted purposes, prohibited contexts, active-state behavior, retention options, resource bounds and evidence owner.

A new capture surface is **not approved by appearing in this document**. The initial permitted-surface allowlist and the exact prohibited-source list remain implementation/product review inputs. An unregistered, unknown, suspended or out-of-scope surface is denied.

Screen capture, camera, AccessibilityService and user-selected files are distinct surface types. Consent for one must not be reused for another. Sensors and other device-level inputs require the same registration review; their inclusion in this general contract is not blanket approval to activate them.

### 3.3 Prohibited or fail-closed inputs

The intake path must deny or hold, as applicable:

- Secure surfaces or protected content.
- Sensitive applications or sources excluded by default, unless a separately reviewed and explicit product policy permits a narrowly scoped operation.
- A surface whose identity, purpose, permission or scope cannot be established.
- Content outside the user-declared application/file/source scope.
- An input for which the required consent, provenance or retention decision is missing or indeterminate.
- General-corpus or public-source material presented as personal experience to bypass its own rights and provenance review.

If secure-surface or sensitive-source status cannot be determined reliably, the protected input is not accepted. Do not claim perfect exclusion when the platform cannot provide that guarantee.

## 4. Mandatory intake and routing sequence

### 4.1 Ordered stages

Every input intended to become inspectable memory or a personal-training candidate must pass these stages in order:

1. **Surface declaration:** identify the registered capture surface, adapter version, source scope and requested purpose.
2. **Platform gate:** verify the applicable Android permission/authorization and active-state requirements. This is separate from Argus consent.
3. **Consent gate:** check a current grant for the exact surface, purpose and declared scope. If absent, revoked, expired or unknown, deny the operation and do not persist the content.
4. **Bounded capture:** collect only the declared content and metadata within the approved resource and source bounds.
5. **Classification and safety review:** classify sensitivity, scope, provenance and the intended path. Redact or reject disallowed material as required.
6. **Retention assignment:** before durable persistence, bind an explicit retention policy and its purpose scope. No durable personal record is written without one.
7. **Personal-experience record:** where retention is authorized, create a versioned record in the controlled personal-experience/memory store. Its initial eligibility is memory-only unless a later review and separate training authorization pass.
8. **Review gate:** a request to use the record for personal training is reviewed by the application/knowledge intake owner. The review confirms consent, source classification, retention, sensitivity, redaction, provenance and revocation state.
9. **Training authorization and handoff:** only after an explicit authorization for a specific batch/session may the approved record revisions be assembled into an immutable personal-training candidate manifest and handed to `application/learning`.
10. **Learning admission:** `application/learning` rechecks candidate identity, approval, consent/revocation state, dataset identity and session policy before a session can consume the manifest.

The exact runtime state names and serialization are implementation bindings. The ordering and fail-closed guarantees are design requirements.

### 4.2 Memory-layer intermediate boundary

A personal-training candidate must never travel directly from a capture surface to the learning/training pipeline. It must first pass through the controlled personal-experience record boundary owned by the knowledge path.

For clarity, **the personal-experience/memory store is the custody and provenance boundary; inspectable memory retrieval is a separate permission and capability**. A record held in that store is not automatically exposed to model context, retrieval results, chat history or other features. This distinction allows the memory layer to be an obligatory intermediate record boundary without silently granting retrieval access or extending retention.

The default persisted state is **MEMORY_ONLY**. If a user intends personal training, that intention may request review, but it does not skip the record boundary, review gate or second authorization. Only an approved and separately authorized record can be promoted to personal-training candidacy.

A purely transient-context operation may be processed in memory for its declared request and then discarded. It is not a retained personal-experience record, cannot be retrieved later and cannot be converted into a training candidate. If later retention or training is requested, the input must be captured/accepted again under the relevant consent and retention scopes.

### 4.3 Routing prohibitions

- No capture adapter may call a training manager, optimizer, engine or candidate repository directly.
- No raw observation may be enqueued into a live training queue.
- A personal-experience record is not a training dataset; training consumes an immutable, reviewed candidate manifest.
- Capture, transient context, durable retention, retrieval, training review and personal parameter updates are distinct operations and permissions.
- An input is not both inspectable memory and training-eligible by default. Any dual-purpose use requires separate, explicit authorization for each purpose.
- No personal capture is placed into the general corpus or general-training path.
- An event, notification refresh, model confidence score or frequent retrieval is not a permission decision.

### 4.4 Provenance envelope

Every retained personal-experience record must preserve enough metadata to reconstruct the origin and authorization path:

- Record ID and immutable source/capture revision identity.
- Capture surface ID/type, adapter version and scoped source identity.
- Capture request/session ID and capture timestamp.
- Purpose requested and the classification decision.
- Consent grant ID/version and consent/policy state used at intake.
- Platform permission/authorization observation where applicable.
- Sensitivity classification and redaction/processing version.
- Retention-policy ID/version and expiry/deletion trigger.
- Review decision ID, reviewer/actor, outcome and timestamp when review occurs.
- Training authorization ID and exact batch/session identity if training is later approved.
- Content digest and links to derived records where permitted.

Generic events and audit logs carry only sanitized metadata and identifiers. They must not include raw screen text, private files, prompts, credentials or full captured content. Content is held only in the personal-experience store and only within the approved retention scope.

## 5. Consent boundary

### 5.1 Surface-and-purpose-specific consent

Consent is granted separately for each capture surface and purpose. A grant for screen capture does not authorize accessibility events; a grant for camera does not authorize file import; and a grant for one purpose does not authorize another purpose.

The policy model must distinguish at least these purpose scopes:

| Purpose scope | What it permits | What it does not permit |
|---|---|---|
| Capture for a declared request | Collect the declared input from the selected surface | Durable retention or retrieval |
| Transient inference context | Use the content for the declared request/session only | Retention after the request or training |
| Retain personal experience | Persist a personal-experience record under an explicit retention policy | Retrieval or training unless separately authorized |
| Inspectable memory/retrieval | Make the approved record available to the user's memory/retrieval features | Personal parameter training |
| Training review | Submit a retained record for explicit candidate review | Adding it to a training batch or changing weights |
| Personal-training batch/session | Include the specifically approved record revisions in the identified candidate/batch/session | Reuse for other sessions, general training, export or external transfer |
| External transfer/export | No default grant; requires a separate future product/security decision | Any implicit upload based on a local capture grant |

The exact user-facing consent wording, grant UI and scope schema remain open. The purpose separation above is mandatory even if a later storage representation groups compatible fields.

OS permission, accessibility enablement, camera permission, file-picker selection or a visible screen is not Argus consent. Both the applicable platform authorization and the Argus purpose-specific grant must pass.

### 5.2 What consent does not authorize

Unless a separate, explicit, current grant and policy decision exist, intake consent does not authorize:

- Retention beyond a transient request or the assigned retention period.
- Inspectable memory or retrieval.
- Inclusion in a personal-training candidate.
- Inclusion in a specific training batch/session.
- Changing model parameters.
- Sharing with an external service, central training server or another Argus instance.
- Reuse for a purpose other than the one declared at intake.
- Addition to general-corpus storage or general training.

### 5.3 Consent check and revocation

Consent is checked before capture, before durable retention, at review, when building the immutable candidate manifest, at learning admission and at the defined training safe boundary. A cached approval must not survive a known revocation, expiry, scope change or policy version change.

Revocation must:

- Prevent future capture for the revoked surface/purpose.
- Stop or close the affected capture session.
- Prevent new retention/retrieval/training operations under that grant.
- Mark existing records for the applicable deletion/restriction action.
- Remove revoked records from any uncommitted candidate or pending dataset manifest.
- Prevent a not-yet-committed optimizer update from consuming a revoked record.
- Stop future use at the next safe optimizer/update boundary if revocation occurs during a running session, consistent with document 36.

Revocation of one grant must not delete unrelated records or disable unrelated surfaces. The user must be able to review which records, candidates and sessions are affected.

For data already consumed by a committed optimizer update, apply the separately approved model-lineage, rollback or retraining policy. Do not promise exact unlearning or imply that deleting the source record reverses parameter influence.

## 6. Review and training eligibility

### 6.1 Default state

A retained personal-experience record begins as **MEMORY_ONLY** (or held/rejected where classification requires). It is not training-eligible because it exists, is frequently retrieved, has aged, or appears useful to the model.

### 6.2 Review before training candidacy

The application/knowledge intake owner coordinates the review gate. The capture surface cannot review, approve or promote its own input.

The review must confirm:

- Consent was valid for the declared surface and purpose at intake.
- Current consent remains valid for training review and the proposed training use.
- The retention policy explicitly covers the proposed training-candidate period and purpose.
- The record has not expired, been revoked, deleted or superseded.
- Provenance and source identity are sufficient to establish that this is personal experience, not general-corpus material misclassified as personal.
- Sensitivity classification and required redaction are complete.
- The exact record revision and content digest are stable.
- The user has been shown the relevant content/metadata and has explicitly approved the personal-training use.

A missing or ambiguous review fact results in HOLD/REJECT, not a best-effort promotion.

### 6.3 Separate training authorization

Approval for review or memory retention is not authorization to update model parameters. After review, the user must separately authorize personal training for an identified batch/session.

That authorization is bound to:

- Exact personal-experience record IDs/revisions and content digests.
- Exact candidate/dataset manifest digest.
- Target instance and training session/batch identity.
- Purpose, policy and consent versions.
- Authorization time and actor.
- Applicable retention and revocation references.

It must not become a permanent blanket permission for later batches. A subsequent batch requires a new explicit authorization or a clearly scoped grant that names the batch boundary; no authorization may be inferred from a prior training run.

### 6.4 Immutable handoff

The learning workflow receives only an immutable candidate manifest containing approved record revisions and their provenance/consent lineage. It must not read directly from a mutable live memory queue or capture session.

At admission and before the first optimizer update, learning revalidates:

- The candidate manifest digest and instance identity.
- Every approval and required consent grant.
- Current revocation/expiry/deletion state.
- The session's declared objective, data partition and policy.
- That no general-corpus and personal-training partition has been mixed.

Document 36 controls optimizer update sequencing, checkpoint/recovery and revocation races. This contract adds no alternative training lifecycle.

## 7. Retention and deletion

### 7.1 Retention assignment at intake

No personal-experience record may be durably written without an explicit retention policy assigned before persistence. The policy must state:

- Maximum retention duration or a bounded expiry condition.
- Permitted purpose(s): memory-only, training-review eligible, or a separately authorized combination.
- Whether retrieval is permitted.
- Deletion/expiry trigger: time-based, revocation-based, user-requested, session-based or another explicitly defined event.
- Applicable source, consent and policy versions.

The concrete durations and exact deletion-trigger taxonomy remain open. Until a duration/policy is selected, the system must not silently retain the content indefinitely. A transient-only operation is discarded at the end of its declared request/session and is not eligible for later promotion.

A training review or training authorization does not silently extend the underlying retention period. Any extension requires a separately disclosed, valid retention decision before the existing expiry.

### 7.2 Deletion controls

The design must support user-initiated deletion of an individual record, a capture session or all records within a selected consent/surface scope; revocation-driven deletion; and expiry-driven deletion.

Deletion/restriction must cover relevant derived data and pending handoffs, including:

- The personal-experience record and retained content.
- Inspectable memory entries and retrieval indexes.
- Derived excerpts, summaries or embeddings where they are personal derivatives.
- Pending review items and uncommitted personal-training candidate manifests.
- Any queued job or session input that has not crossed its committed update boundary.
- Provenance/consent/audit metadata, subject to the minimum sanitized evidence required to record that deletion/revocation occurred and the applicable legal/retention policy.

Deletion must be idempotent and traceable without copying the deleted content into audit logs. Expired or revoked records cannot be reintroduced from stale indexes, queues or cached candidate manifests.

### 7.3 No retention by inaction

A record cannot be retained beyond its assigned expiry merely because it has not been reviewed, retrieved or explicitly deleted by the user. Expiry is enforced by the retention policy and its owning workflow/repository, not by the absence of a deletion request.

If a review is not completed before expiry, the record expires or is deleted/held according to its declared policy. Review backlog is not grounds to extend retention.

### 7.4 Post-training lineage

If a personal record has already contributed to a committed model update, record the relevant candidate/session/generation lineage. The effect of revocation/deletion on already-trained weights remains governed by the model-lineage and deletion policy in document 36 and the applicable accepted decision. Exact machine unlearning is not claimed by this contract.

## 8. Required continuity of the AccessibilityService path

While the current AccessibilityService path remains in use, it is provisional and must stay within the declared source/purpose scope. It must not infer consent from event availability, broaden app/content access, self-assign retention or enqueue data into training directly.

Before its dependency is severed, one of the following controlled outcomes must be ready:

**A. Replacement capture path is enabled**
1. The replacement adapter is registered and reviewed under this contract.
2. Its surface/purpose-specific consent and platform authorization paths are explicit.
3. Its metadata/provenance envelope is compatible with the intake workflow.
4. The application/knowledge intake owner confirms that its routing, review, retention and revocation behavior satisfies Sections 3–7.
5. The application/learning workflow confirms that only separately approved immutable candidate manifests can reach training.
6. The replacement is verified not to reintroduce a direct capture-to-training shortcut or the source dependency cycle recorded in document 46.

**B. Capture capability is retired rather than replaced**
1. The feature is explicitly disabled/marked unavailable; no unowned background capture remains.
2. Existing personal records and pending candidates from the old path are inventoried and reconciled.
3. Unknown, unreviewable or unauthorized records are held or discarded, not silently migrated as eligible.
4. Pending training candidates are either fully reviewed/authorized under this contract or discarded.
5. The user-visible status and deletion/revocation behavior remain defined for retained records.

In both cases, the owner must record the transition and reconciliation outcome. Severing a class import is not by itself proof that capture authority, personal data or pending work has been safely transferred.

## 9. Logical lifecycle

The following is a proposed state model. Exact enum names and durable representation remain implementation bindings.

### 9.1 Capture-session lifecycle

~~~text
SURFACE_DECLARED
  -> PLATFORM_GATE_PASSED
  -> CONSENT_CHECKED
  -> CAPTURE_ACTIVE
  -> STOP_REQUESTED / REVOKED / EXPIRED
  -> CAPTURE_CLOSED
~~~

A denied or unknown platform/consent state transitions to DENIED without accepting or persisting content. Pause/stop and visible active-state behavior are mandatory for applicable capture surfaces.

### 9.2 Personal-experience record lifecycle

~~~text
CAPTURE_ACCEPTED
  -> CLASSIFIED
  -> RETENTION_ASSIGNED
  -> EXPERIENCE_RECORD_STAGED
  -> VALIDATED
  -> MEMORY_ONLY
       -> RETRIEVAL_AUTHORIZED (separate purpose grant)
       -> TRAINING_REVIEW_PENDING (explicit request only)
              -> TRAINING_REVIEW_APPROVED
              -> TRAINING_AUTHORIZED (batch/session-specific)
              -> PERSONAL_CANDIDATE_MANIFEST_COMMITTED
              -> LEARNING_ADMISSION_CHECKED
              -> CONSUMED_BY_DECLARED_SESSION
~~~

Any applicable state may transition to HELD, REJECTED, REVOKED, EXPIRED or DELETED. A transient-only context does not enter the durable record lifecycle; it ends at request/session closure and cannot later be promoted.

The lifecycle does not mean a record must be both retrievable and training-eligible. Retrieval and training remain separately authorized paths.

## 10. Verification obligations

These are later implementation tests, not tests claimed to have run.

| Test family | Required assertion |
|---|---|
| Surface registration | Unknown/unregistered/suspended surfaces and out-of-scope sources are denied |
| Purpose-specific consent | Grant for one surface/purpose cannot authorize another; missing/unknown/expired scope fails closed |
| Platform permission | OS permission and Argus consent are both required where applicable; neither substitutes for the other |
| Secure/sensitive surfaces | Unknown secure/sensitivity status fails closed according to the reviewed policy |
| No direct training route | No capture adapter can enqueue raw data or call engine/optimizer/candidate repository |
| Memory intermediate | A training-intended record first crosses the personal-experience record boundary; no direct capture-to-learning path exists |
| Default memory-only | Retention/retrieval does not make a record training-eligible; no automatic promotion occurs |
| Review isolation | Capture adapter cannot approve its own data; incomplete review is held/rejected |
| Second authorization | A reviewed candidate cannot enter an unapproved batch/session; authorization is bound to exact record revisions and manifest digest |
| Revocation race | Revocation blocks pending candidate use and prevents the next uncommitted update from consuming the record |
| Retention/expiry | No record is persisted without a policy; expiry is enforced even when review is pending |
| Deletion lineage | Deletion invalidates memory indexes, pending candidates and uncommitted work without logging deleted content |
| Training partition | Personal data cannot enter the general corpus or general-training path |
| Accessibility continuity | Replacement or retirement reconciles old records/candidates and preserves the same policy boundary |
| Package dependency | Platform capture depends on narrow application intake contracts; application workflows do not depend on Android service implementations |

Passing a static dependency rule does not prove runtime consent behavior, deletion durability, Android capture coverage or model unlearning. Each requires its own test method and evidence tied to an exact revision and environment.

## 11. Open implementation bindings

The following remain open and must be resolved before the corresponding capability is enabled:

| ID | Open item | Required resolution |
|---|---|---|
| PC-IMPL-01 | Exact permitted capture-surface allowlist and prohibited source registry | Product/privacy review, surface-specific platform feasibility and scope definitions |
| PC-IMPL-02 | Concrete retention durations and deletion-trigger taxonomy | Storage/privacy decision covering each purpose, expiry enforcement and backup/deletion behavior |
| PC-IMPL-03 | Exact review UI and user-facing consent wording | Review states, visibility, reject/defer actions, re-consent and audit evidence |
| PC-IMPL-04 | Exact capture envelope/API signatures and request lifecycle | Typed identities, payload bounds, cancellation, redaction and adapter versioning |
| PC-IMPL-05 | Post-training revocation/model-lineage disposition | Accepted policy for affected generations, rollback/retraining and user disclosure; no unlearning promise |
| PC-IMPL-06 | Post-severance reconciliation/audit format | Inventory schema, record migration/hold/discard outcomes and proof of no pending direct training route |
| PC-IMPL-07 | Exact storage/schema/encryption ownership | Repository schema, key lifecycle, transaction, deletion cascade and backup behavior under document 36 |

These open bindings do not prevent defining ownership and ordering. They do prevent claiming the intake path is implemented, enabling unreviewed capture surfaces, selecting retention durations by guess, or severing the existing service without a controlled replacement/retirement transition.

## 12. Relationship to existing documents

| Document | Authority retained | This contract adds |
|---|---|---|
| 01 — System Requirements and Decisions | Accepted product constraints, especially separate general/personal learning and local-instance boundaries | No new product decision or ADR |
| 13 — Security and Data Governance | Capture purpose, platform consent, secure-surface safeguards, privacy and deletion principles | An ordered personal-capture intake and continuity contract |
| 21 — Device-Adaptive Model and Training | Training admission/resource control and session policy | Confirms capture intake does not bypass training admission |
| 22 — General Corpus and Personal Learning | Personal-learning consent, provenance, redaction, review, revocation and memory/training separation | Concrete capture-to-memory-to-training routing and record lifecycle |
| 36 — Pre-Implementation Module and Lifecycle Contracts | Generic policy, training, transaction, revocation-race and event contracts | Personal-capture-specific ownership, intake stages and verification obligations |
| 45 — Android Project Structure | Target package and dependency direction | Assigns the intake owner and adapter/record boundaries within the existing package map |
| 46 — Android Source-Tree Ownership Audit | Observed AccessibilityService and global-service dependencies | Defines conditions for controlled severance/retirement |
| 47 — Package Interface and Dependency Contract Matrix | Interface/consumer/provider mapping and dependency rules | Resolves PC-OPEN-01 at the design-contract level; concrete API bindings remain open |

If this document conflicts with accepted product constraints in document 01 or the general personal-learning rules in documents 13/22/36, those upstream authorities remain controlling. This document specializes their application to capture intake; it does not weaken them.

## 13. Completion and disposition

- Personal capture contract and ordered intake stages: **documented as a proposal**.
- Accountable intake workflow owner: **proposed as `application/knowledge`**.
- Training review orchestration: **owned by the intake workflow; training admission remains owned by `application/learning`**.
- Current AccessibilityService: **provisional capture ingress only; not consent, retention or training authority**.
- Memory/existing-record boundary: **mandatory intermediate provenance/custody boundary for retained training-intended records; retrieval remains a separate permission**.
- Consent, review, retention, revocation and deletion obligations: **defined; concrete wording, durations and storage bindings remain open**.
- Replacement capture surface: **not selected**.
- AccessibilityService dependency severance: **not authorized or performed**.
- Full personal-intake workflow/API implementation and Gradle changes: **not implemented or authorized by this contract**. A narrow UI safeguard against known raw-string auto-enqueue paths is recorded below.
- Legacy UI auto-enqueue safeguard: the identified chat prompt, file/image selection and Settings web-result paths no longer call the raw-string training queue. File/image selection explicitly reports that inference/intake is not connected. This prevents those observed paths from silently becoming training input; it does not create consent, retention, review or training authorization.
- Legacy training path: `TrainingManager.kt`, its raw-string queue/worker and `ArgusNeuralEngine.trainStep(String)` have been removed from the current source tree. No replacement `application/learning` coordinator, candidate-manifest reader or `application/knowledge` intake exists. This retirement prevents the old automatic path from running; it does not implement consent, retention, review or training authorization.
- Build, tests, GitHub Actions, artifact publication and device execution: **not performed**.

**Next work:** implement the selected first-surface intake only after its consent, retention, provenance and review bindings are concrete. The raw-string queue safeguard is not a substitute for PC-IMPL-01 through PC-IMPL-07, the immutable approved-candidate handoff or the independent learning-admission recheck. Keep capture implementation and AccessibilityService severance separately bounded.

## 14. Change history

- 2026-11-05 — Initial PC-OPEN-01 personal capture and experience intake contract prepared against `main` at `fab7da5a0ce86d7265a7be152f550a4b524741cd`. Reconciled with documents 01, 13, 21, 22, 36, 45 and 47. Documentation only; no source, build configuration, test, workflow or device experiment was changed.


- 2026-11-05 — Recorded the final containment state: identified chat, file/image and Settings web-result paths no longer enqueue raw strings; the legacy `TrainingManager` worker and `ArgusNeuralEngine.trainStep(String)` were then removed. No consent, review, retention or learning workflow is claimed. No build/test result is claimed.


- 2026-11-05 — Updated the implementation disposition after retirement of the raw-string training worker and engine entry point. Learning remains unavailable until a reviewed immutable-candidate intake and independent learning-admission workflow is implemented. This does not close PC-IMPL-01 through PC-IMPL-07.
