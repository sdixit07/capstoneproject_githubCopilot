# Implementation Plan — EPMCDMETST-69858

## Story scope and design basis

- **Story:** Backend: Add pagination support to `GET /api/products`.
- **Approved requirements:** FR-1–FR-8, NFR-1–NFR-3, and AC-1–AC-6 in `requirements.md`.
- **Architecture:** Final approved `architecture.md`, including design decisions D-1–D-5.
- **Design review:** Approved; D-1 through D-5 are agreed and applied.
- **In scope:** Backend database-backed pagination, the always-enveloped response, preservation of existing filters and sorting, input validation, and backend tests.
- **Out of scope:** React/UI changes, schema/entity changes, new filters, changes to other product endpoints, and an unapproved maximum page size.

## Task ordering and dependency map

### T-1 — Enable specification-based paged repository queries
- **Priority:** P1
- **Files:**
  - `ecom-project/src/main/java/org/ecom/productcatalog/repository/ProductRepository.java`
- **FR/AC:** FR-1, FR-5, FR-8; AC-1, AC-2, AC-6
- **Depends on:** None
- **Description:**
  - Add `JpaSpecificationExecutor<Product>` while retaining the existing `JpaRepository<Product, Long>` and category-specific lookup.
  - Provide the `Page<Product> findAll(Specification<Product>, Pageable)` query contract.
  - Preserve the existing entity model; no entity or schema change is required.

### T-2 — Implement filtered service pagination, sorting, and response mapping
- **Priority:** P1
- **Files:**
  - `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java`
  - `ecom-project/src/main/java/org/ecom/productcatalog/dto/ProductPageResponse.java`
- **FR/AC:** FR-1, FR-2, FR-4, FR-5, FR-6, FR-8; AC-1, AC-2, AC-5, AC-6
- **Depends on:** T-1
- **Description:**
  - Replace full-list retrieval and in-memory slicing/filtering with a single service-owned `Specification<Product>` combining search, category, minimum-price, and maximum-price predicates. Pass the same specification to the repository page query so content and count metadata use identical predicates.
  - Preserve case-insensitive name search, conjunctive filters, inclusive price bounds, and current price-bound validation.
  - Enforce the exact sort grammar: omitted sort means ID ascending; `price` and `price,asc` mean price ascending with ID ascending as tie-breaker; `price,desc` means both descending. Reject any other present value.
  - Map `Page<Product>` content and metadata into the existing five-field `ProductPageResponse`; always use this envelope and remove any legacy unpaged service path.
  - Do not add a service-level size fallback; effective pagination defaults are resolved only by the controller.

### T-3 — Resolve and validate pagination at the controller
- **Priority:** P1
- **Files:**
  - `ecom-project/src/main/java/org/ecom/productcatalog/controller/ProductController.java`
- **FR/AC:** FR-1, FR-2, FR-3, FR-4, FR-6, FR-7; AC-1–AC-6
- **Depends on:** T-2
- **Description:**
  - Keep `GET /api/products` and its existing optional query parameter names; always delegate to the paged service operation and return the envelope.
  - Resolve omitted page and size to `0` and `10`, respectively. Reject negative pages and non-positive sizes before repository access.
  - Safely calculate the requested offset and reject an unrepresentable or unsafe offset with HTTP 400. Do not clamp values or impose a maximum size.
  - Preserve `/api/products/{id}` and `/api/products/category/{categoryId}` behavior.

### T-4 — Preserve and verify the established HTTP 400 error contract
- **Priority:** P1
- **Files:**
  - `ecom-project/src/main/java/org/ecom/productcatalog/exception/GlobalExceptionHandler.java`
  - `ecom-project/src/main/java/org/ecom/productcatalog/exception/ApiError.java`
- **FR/AC:** FR-7; AC-3, AC-4; NFR-3
- **Depends on:** T-3
- **Description:**
  - Ensure invalid page/size values, malformed numeric parameters, invalid filter ranges, unsafe offsets, and unsupported/malformed sort values map to HTTP 400 using the existing `{timestamp,status,message,path}` payload.
  - Keep valid out-of-range pages as HTTP 200 with an empty `items` array and metadata. Do not introduce a new error schema or convert unexpected/database failures into successful empty pages.

### T-5 — Add service unit tests for query and response behavior
- **Priority:** P1
- **Files:**
  - `ecom-project/src/test/java/org/ecom/productcatalog/service/ProductServiceTest.java`
- **FR/AC:** FR-1, FR-2, FR-4–FR-6, FR-8; AC-1, AC-2, AC-5, AC-6
- **Depends on:** T-2
- **Description:**
  - Verify the service passes pagination and the combined filter specification to the repository and maps page content, effective page/size, total items, and total pages into the response DTO.
  - Cover exact supported sort forms and rejection of unsupported fields, directions, extra segments, casing/whitespace variants, and present blank values.
  - Cover conjunctive filters and inclusive price bounds without changing their existing meaning.

### T-6 — Migrate and extend product endpoint integration tests
- **Priority:** P1
- **Files:**
  - `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java`
- **FR/AC:** FR-1–FR-8; AC-1–AC-6; NFR-1–NFR-3
- **Depends on:** T-4, T-5
- **Description:**
  - Replace legacy array-response assertions with assertions for the envelope, including the no-parameter defaults.
  - Use MockMvc to cover first/later pages, item limits and metadata, out-of-range and empty-filter results, negative page, zero/negative size, malformed pagination, and unsafe offset behavior.
  - Verify combined filters produce consistent items and totals; cover default and supported price sorts, invalid sort values, and unchanged category-specific endpoint behavior.

## Ordered dependency graph

```text
T-1 -> T-2 -> T-3 -> T-4 --+
          \                 +-> T-6
           +-> T-5 ---------+
```

The graph is acyclic. With equal task weights, the critical path is **T-1 → T-2 → T-3 → T-4 → T-6**. T-5 is a parallel test branch after T-2 and must also finish before T-6.

## Blocked tasks

- **T-2 → blocked until T-1:** The service needs the specification-enabled paged repository contract.
- **T-3 → blocked until T-2:** The controller must delegate to the completed paged service operation.
- **T-4 → blocked until T-3:** Error handling must match the controller's validation and request-binding behavior.
- **T-5 → blocked until T-2:** Service tests need the completed query construction and response mapping behavior.
- **T-6 → blocked until T-4 and T-5:** The endpoint integration matrix depends on the final HTTP/error contract and the service behavior covered by unit tests.

**Blocked-task count:** 5 tasks.

## Unit-test tasks (Step 5)

- **T-5** covers service query construction, the shared filter specification, supported/invalid sort grammar, filter semantics, and response metadata.
- **T-6** updates the existing MockMvc integration suite for the complete architecture verification matrix, including defaults, invalid values, offsets, filters/sorts, out-of-range pages, and the unchanged category route.
- No frontend test task is included because the approved architecture explicitly requires no frontend behavior change.

## Verification outline (Step 7)

1. Run backend unit tests, including `ProductServiceTest`.
2. Run `ProductControllerIT` and verify the full HTTP/response matrix in the approved architecture.
3. Run the complete backend test suite and build; confirm no legacy list-response assertions remain for `GET /api/products`.
4. Review the diff against FR-1–FR-8, AC-1–AC-6, D-1–D-5, and the out-of-scope list. Confirm no React, entity/schema, or unrelated endpoint changes.

## Definition of Done

- `GET /api/products` always returns `items`, `page`, `size`, `totalItems`, and `totalPages`, with omitted page/size defaulting to `0`/`10`.
- Page retrieval and count metadata use Spring Data paging and the same combined filter specification.
- Existing filter semantics and the approved exact sort grammar/tie-break behavior are preserved.
- Invalid pagination, unsafe offsets, and invalid existing filter/sort inputs return HTTP 400 in the established `ApiError` shape; a valid out-of-range page returns HTTP 200 with empty items and metadata.
- The category-specific and product-detail endpoints remain unchanged.
- Unit and integration tests cover the approved behavior, and Step 7 verification passes.
- No unapproved size maximum, frontend change, entity/schema change, or unrelated scope is introduced.

## Planning note

This artifact is a plan only. Implementation and later SDLC stages are not started.
