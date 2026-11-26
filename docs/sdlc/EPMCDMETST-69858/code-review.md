# Code Review — EPMCDMETST-69858

## Review Scope
- Reviewed `feature/EPMCDMETST-69858` against `main`, scoped to ticket commits and ticket documentation.
- Read requirements, architecture, design review, implementation plan, implementation notes, changed backend code, and tests.
- Evaluated FR1–FR8, NFR1–NFR3, AC1–AC6, and D1–D5.

## Summary
The pagination implementation is substantially aligned with the approved design, but unresolved correctness gaps remain in search semantics, numeric price validation, and stale legacy integration tests.

## Overall Recommendation
**Request changes**

## Findings by Severity

### Warning — Stale legacy integration suite asserts the old array contract
**File:** `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java:69`

**Problem:** `ProductControllerIT` still asserts that `/api/products` returns a raw array and that filtered results are array-based. This conflicts with FR2/FR3/AC5 and D-1, which require the paginated envelope and migration of legacy assertions.

**Evidence:** The no-parameter test asserts `"$"` is an array and has size 27 at line 69; additional tests in the same file use array assertions for this endpoint.

**Recommendation:** Migrate or remove the legacy suite so it does not contradict the new contract. The new `ProductControllerIntegrationTest` covers the envelope, but the stale `*IT.java` suite is not discovered by default Surefire patterns. Configure Maven test discovery consistently or replace the legacy assertions and naming.

### Warning — Search wildcard characters do not implement substring semantics
**File:** `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java:54`

**Problem:** User-provided `%` and `_` are passed directly into a SQL `LIKE` pattern. They act as SQL wildcards rather than literal substring characters, violating documented case-insensitive substring search semantics.

**Evidence:** The implementation creates `"%" + search.toLowerCase(Locale.ROOT) + "%"` and calls `criteriaBuilder.like(...)`. Search for `_` or `%` therefore matches by wildcard behavior rather than literal contents.

**Recommendation:** Escape `%`, `_`, and the escape character in the search text, and pass an explicit escape character to the JPA `LIKE` predicate.

### Warning — Non-finite price parameters are accepted
**File:** `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java:81`

**Problem:** Java binds values such as `NaN` and `Infinity` as `Double` values. Existing validation checks negative and reversed bounds but does not reject non-finite values; these can reach database predicates, contrary to invalid-filter-input behavior in the architecture and NFR3.

**Evidence:** `minPrice < 0` and `minPrice > maxPrice` do not reject `Double.NaN`; positive infinity is neither negative nor reversed.

**Recommendation:** Reject `Double.isNaN(...)` and `Double.isInfinite(...)` for both price bounds before building the specification.

## Test Coverage and Verification Assessment
- Implementation notes report 16 focused tests passing and 17 full-suite tests passing.
- The full-suite count is consistent with Surefire discovering `*Test.java`; `ProductControllerIT.java` is not discovered by default Maven test patterns.
- `pom.xml` has no Failsafe or additional Surefire inclusion configuration.
- Package build and live startup are reported successful, but not independently rerun during review.

## Missing Context
- Jira was not independently verified during review; requirements.md was the source of truth.
- `docs/KNOWN-ISSUES.md` is absent, as noted in implementation artifacts.
- CI configuration and hosted Jira state were unavailable.
- Independent command execution was unavailable; validation is based on repository evidence and implementation notes.

## Actionable Recommendations
1. Fix stale `*IT.java` assertions or remove/archive that legacy suite.
2. Escape SQL `LIKE` wildcard characters for search.
3. Reject NaN and infinite price parameters.
4. After fixes, run both the Surefire suite and the legacy integration suite explicitly.

## GitHub PR Comment Status
No PR exists. No PR comments or PR actions were performed.

## Re-review after remediation (d599b543...)

### Review Scope
Reviewed the checked-out feature snapshot at `d599b543b15ea1797ca131f3b89ea8063ed1ef6f`, including backend code, tests, Maven configuration, requirements, and implementation notes.

The `main...feature` diff could not be obtained: GitHub access failed authentication, and available tools did not expose a Git diff. Branch and `main` ref hashes were readable, but the ticket-only change scope could not be independently confirmed.

### Result
The three previous findings appear remediated in the checked-out snapshot. No additional code issue worth reporting was identified.

**Conditional recommendation:** No further code changes appear necessary, conditional on verifying the full branch diff against `main` and running the full backend suite after remediation.

No PR exists; no PR comment actions were performed.

### Prior Finding Verification
1. **Paginated response assertions and test discovery:** `ProductControllerIT` envelope assertions are at `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java:79-88`. Surefire includes `*IT.java` at `ecom-project/pom.xml:82`. The targeted report records 15 `ProductControllerIT` tests, zero failures, and the target selector.
2. **Literal search wildcard handling:** `ProductService.java:68-78` escapes backslash, `%`, and `_`, and supplies an explicit backslash escape to `CriteriaBuilder.like`. Regression cases are at `ProductControllerIT.java:207-219`.
3. **Non-finite price bounds:** `ProductService.java:93-98` rejects non-finite bounds. Endpoint tests cover NaN and positive/negative infinity for both parameters at `ProductControllerIT.java:222-238`; service tests are at `ProductServiceTest.java:116-125`.

### Test Evidence and Remaining Validation
- Surefire XML reports 15 `ProductControllerIT` tests and 5 `ProductServiceTest` tests, all passing.
- The report selector `ProductServiceTest,ProductControllerIT` confirms Maven test discovery.
- Implementation notes report package and startup checks, but these were not independently rerun.
- There is no evidence of a post-remediation full `mvnw test` run. The separate `ProductControllerIntegrationTest` report was not part of the targeted run.

The current implementation appears consistent with FR1-FR8, NFR1-NFR3, AC1-AC6, and D1-D5: defaults are page 0/size 10; pagination and offsets are validated; filtered pagination uses a shared query; envelope, filtering, and sorting behavior are preserved; and invalid price bounds are rejected.
