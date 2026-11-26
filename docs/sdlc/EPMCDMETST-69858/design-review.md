# EPMCDMETST-69858 Design Review

## Review scope

Reviewed `docs/sdlc/EPMCDMETST-69858/requirements.md`, `docs/sdlc/EPMCDMETST-69858/architecture.md`, the Spring controller/service/repository/DTO/error implementation, the React consumer, and the existing product controller integration tests.

Mode: **review**. `architecture.md` was not modified.

## Findings

| ID | Severity | Area | Risk or gap | Recommendation |
|---|---|---|---|---|
| F-1 | MAJOR | Verification / backward compatibility | Existing integration tests assert the old array response and array-based filter assertions. The architecture lists acceptance behavior but does not add an explicit test-update and test-coverage plan, so implementation could leave the suite failing or preserve legacy assumptions in code. | Update the integration tests to assert the envelope for no parameters and paged calls, and add cases for page/size defaults, negative page, zero/negative size, out-of-range page, and filtered metadata. |
| F-2 | MAJOR | Sort validation | The architecture requires unsupported sort properties/directions to remain bad requests, but the current `resolveSort` implementation treats any direction other than `desc` (including malformed text) as ascending. It could therefore accept invalid input contrary to the design. | Make the sort grammar explicit: exactly `price`, `price,asc`, or `price,desc`; reject all other properties/directions with HTTP 400. Preserve ID ascending as the default and use ID as the price-sort tie-breaker in the direction specified by the architecture. |
| F-3 | MAJOR | Backend default size | The requirements require omitted `size=10`, and the architecture does so, but the current service constant is `DEFAULT_PAGE_SIZE = 12`. The two current frontend callers can already send 12 explicitly, but omitted server requests would be wrong until the baseline mismatch is removed. | Resolve pagination defaults only once in the controller (page 0, size 10) and remove or align the service fallback; do not let the service silently retain 12. |
| F-4 | MAJOR | Repository specification detail | The architecture says to use `JpaSpecificationExecutor` and one shared specification, but it does not specify that count and content queries must be built from the same immutable predicate set or define an implementation/API method signature. This leaves metadata/content consistency dependent on implementation discipline. | Propose a single specification-building path that is passed to `findAll(specification, pageable)`, so the framework applies identical filters to content and count and returns `Page<Product>` metadata. |
| F-5 | MINOR | Numeric bounds | `page` and `size` are Java `Integer` values and an implementation may perform page-by-size calculations. Extremely large valid values or near-`Integer.MAX_VALUE` products can overflow arithmetic or produce resource-heavy queries; the architecture permits no maximum and does not describe overflow handling. | Keep the values as Spring `PageRequest` integers, reject values that cannot form a representable/processable offset with HTTP 400 if needed, and document any chosen bound rather than silently clamping; no maximum is required unless explicitly approved. |

No finding is a security or authorization gap: this story has no authentication or authorization requirement in the supplied sources.

## Proposed design decisions

| Decision ID | Decision | Resolves findings | Changes `architecture.md`? |
|---|---|---|---|
| D-1 | Replace legacy array assertions and add the complete HTTP/response test matrix for defaults, pagination, out-of-range pages, invalid values, and combined filters/sorts. | F-1 | Yes; add a backend verification section. |
| D-2 | Define and enforce sort grammar as `price`, `price,asc`, or `price,desc`; default ordering remains ID ascending; price ordering uses ID as the tie-breaker. | F-2 | Yes; clarify sort grammar and tie-break behavior. |
| D-3 | Set the server pagination fallback to page `0` and size `10`, and remove the current service fallback of size `12`; the React `size=12` remains explicit and unrelated to the omitted-size contract. | F-3 | Yes; add a note that the frontend size is explicit and must not become the server default. |
| D-4 | Use one `Specification<Product>` builder for search/category/min-price/max-price and pass it to `findAll(specification, pageable)` for both item selection and framework count metadata. | F-4 | Yes; add the repository method shape and shared-specification rule. |
| D-5 | Do not add an unapproved maximum size. Use Spring `PageRequest`/bounded database paging and reject any numeric offset that cannot be represented safely rather than clamping. | F-5 | Yes; add a concise numeric-safety rule. |

## Traceability

| Requirement / acceptance criterion | Architecture coverage | Review result |
|---|---|---|
| FR1 / Spring Data `Pageable` | Controller, service, repository, and data flow specify `Pageable`/`Page`. | Covered, pending repository implementation. |
| FR2 / always return envelope | `ProductPageResponse` and no-parameter envelope are explicit. | Covered. |
| FR3 / defaults 0 and 10 | Controller and API contract specify 0/10. | Covered in architecture; D-3 resolves baseline size mismatch. |
| FR4 / valid page and item limit | Bounded repository query and DTO mapping are specified. | Covered. |
| FR5 / filtered total count/pages | `Page<Product>` metadata and same specification are specified at a high level. | Covered; D-4 makes consistency explicit. |
| FR6 / out-of-range page 200 and empty items | Explicit data-flow and error-handling behavior. | Covered. |
| FR7 / negative page, zero/negative size 400 | Controller/service validation and global handler are explicit. | Covered. |
| FR8 / preserve filters/sort | Repository predicates and existing sort semantics are described. | Covered substantively; D-2 clarifies malformed/unsupported sort direction. |
| AC1 / page 0 size 10 more than 10 | API contract and bounded page flow cover this. | Not yet verified; D-1 test matrix is required. |
| AC2 / page 2 size 10 | Out-of-range and normal-page behavior covered. | Not yet verified; D-1 test matrix is required. |
| AC3 / negative page 400 | Explicit. | Covered. |
| AC4 / size zero 400 | Explicit. | Covered. |
| AC5 / no parameters envelope | Explicitly replaces legacy array behavior. | Covered; existing tests require migration under D-1. |
| AC6 / beyond final page 200 empty items | Explicit. | Covered. |

## Verdict

**APPROVED WITH CHANGES**

There is no architecture blocker that prevents implementation planning, but the implementation must adopt D-1 through D-4 and may adopt D-5 where numeric-safety behavior is implemented. The MAJOR findings are design/verifiability gaps rather than missing core requirements.

## Agreed design decisions

The user approved all proposed decisions, D-1 through D-5. The architecture updates applying them were committed in `f7bd507e07f3ff635661ecc7316e43de7c2d1fc0`.

| Decision ID | Agreement | Applied in commit |
|---|---|---|
| D-1 | Agreed as proposed | `f7bd507e07f3ff635661ecc7316e43de7c2d1fc0` |
| D-2 | Agreed as proposed | `f7bd507e07f3ff635661ecc7316e43de7c2d1fc0` |
| D-3 | Agreed as proposed | `f7bd507e07f3ff635661ecc7316e43de7c2d1fc0` |
| D-4 | Agreed as proposed | `f7bd507e07f3ff635661ecc7316e43de7c2d1fc0` |
| D-5 | Agreed as proposed | `f7bd507e07f3ff635661ecc7316e43de7c2d1fc0` |

## Final verdict

**APPROVED**
