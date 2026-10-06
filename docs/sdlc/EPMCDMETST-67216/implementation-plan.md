# Implementation Plan — EPMCDMETST-67216

## Story scope and design basis
- Story: As a shopper, I can view a product details page so that I can see full information before purchasing.
- Approved requirements: FR-1 through FR-6 and NFR-1 through NFR-4 from the story definition.
- Architecture decision: the detail lookup remains backend-owned at GET /api/products/{id}; the React page remains compatible with the existing `ProductDetail` flow.
- Design review outcome: approved with minor contract-drift and UX-precision concerns; no blocking gaps remain.
- In scope: product detail retrieval, 200/404/400 semantics, and compatibility with the existing frontend detail page.
- Out of scope: product creation/update, UI redesign, new metadata fields beyond the existing contract.

## Task ordering and dependency map

### T-1 — Repository and data contract readiness
- Priority: P1
- Files:
  - `ecom-project/src/main/java/org/ecom/productcatalog/repository/ProductRepository.java`
  - `ecom-project/src/main/java/org/ecom/productcatalog/model/Product.java`
- FR/AC:
  - FR-1, FR-2, FR-3, AC-1
- Depends on: None
- Description:
  - Confirm the entity and repository support a lookup by product identifier and expose the fields already consumed by the front end (`id`, `name`, `description`, `price`, and existing detail-page fields).
  - Validate the persisted model remains compatible with the existing detail-page contract.

### T-2 — Service-layer retrieval and not-found logic
- Priority: P1
- Files:
  - `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java`
  - `ecom-project/src/main/java/org/ecom/productcatalog/exception/ResourceNotFoundException.java`
- FR/AC:
  - FR-2, FR-3, FR-4, AC-1, AC-2
- Depends on: T-1
- Description:
  - Add product lookup by id in the service layer.
  - Throw `ResourceNotFoundException` when the entity is absent.
  - Keep the business logic independent from HTTP and controller concerns.

### T-3 — Controller and global error mapping
- Priority: P1
- Files:
  - `ecom-project/src/main/java/org/ecom/productcatalog/controller/ProductController.java`
  - `ecom-project/src/main/java/org/ecom/productcatalog/exception/GlobalExceptionHandler.java`
  - `ecom-project/src/main/java/org/ecom/productcatalog/exception/ApiError.java`
- FR/AC:
  - FR-1, FR-4, FR-5, AC-1, AC-2, AC-3
- Depends on: T-2
- Description:
  - Expose GET `/api/products/{id}`.
  - Return a 200 JSON payload for valid products.
  - Route missing products to HTTP 404 and invalid identifiers to HTTP 400.
  - Preserve the project’s existing error payload structure and semantics.

### T-4 — Frontend API wrapper compatibility check
- Priority: P1
- Files:
  - `ecom-front/ecom-catalog-react/src/api/productsApi.js`
- FR/AC:
  - FR-6, NFR-3, AC-1, AC-2, AC-3
- Depends on: T-3
- Description:
  - Confirm the fetch contract remains compatible with the current `fetchProductById` call pattern.
  - Preserve status propagation and error handling for 404 and 400 responses.
  - Do not introduce a new API abstraction or route contract at the client layer.

### T-5 — Product detail page state handling and rendering
- Priority: P1
- Files:
  - `ecom-front/ecom-catalog-react/src/pages/ProductDetail.jsx`
- FR/AC:
  - FR-6, NFR-3, AC-1, AC-2
- Depends on: T-4
- Description:
  - Keep the page state model for loading, success, not-found, and generic error consistent with the approved UI behavior.
  - Render product fields already expected by the existing page without redesign work.
  - Maintain route-based fetch on mount and on parameter change.

### T-6 — Regression tests and validation coverage
- Priority: P1
- Files:
  - `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java`
  - `ecom-front/ecom-catalog-react/src/api/productsApi.test.js`
  - `ecom-front/ecom-catalog-react/tests/catalog-search-pagination.spec.js` (if extended for detail-page regression coverage)
- FR/AC:
  - All FR and AC artifacts
- Depends on: T-5
- Description:
  - Validate 200/404/400 behavior end-to-end and within the frontend API wrapper.
  - Protect the compatibility contract against regression.

### T-7 — Final release readiness and defect triage check
- Priority: P2
- Files:
  - Project-level review notes and release validation artifacts
- FR/AC:
  - NFR-1, NFR-2, NFR-3
- Depends on: T-6
- Description:
  - Review the final diff for contract drift and ensure no scope expansion beyond the approved design.
  - Confirm risk controls are in place and release notes are aligned with the story wording.

## Ordered dependency graph

T-1 -> T-2 -> T-3 -> T-4 -> T-5 -> T-6 -> T-7

This is a linear dependency path for the approved story. There are no parallel branches requiring additional coordination beyond the dependency chain above.

## Blocked tasks

- T-2 is blocked until T-1 is complete because the service layer cannot implement product lookup without a confirmed repository contract and entity shape.
- T-3 is blocked until T-2 is complete because the controller must delegate to a service that already enforces 404 semantics and returns the correct record.
- T-4 is blocked until T-3 is complete because the frontend wrapper depends on the backend’s final HTTP contract and error semantics.
- T-5 is blocked until T-4 is complete because the detail page must call the final API wrapper contract to render the success and failure states correctly.
- T-6 is blocked until T-5 is complete because regression tests should validate the finished behavior, not partial or hand-wired stubs.
- T-7 is blocked until T-6 is complete because release readiness is only meaningful after the verification evidence is complete.

## Unit-test strategy (Step 5)

### Backend tests
1. Product lookup success case
   - Given an existing product id, GET /api/products/{id} returns HTTP 200.
   - Assert the JSON body contains at least `id`, `name`, `description`, and `price`.

2. Product lookup not found
   - Given an id that does not exist, GET /api/products/{id} returns HTTP 404.
   - Assert the `ApiError` payload contains a meaningful not-found message and the expected status.

3. Invalid identifier handling
   - Given a non-numeric path segment, GET /api/products/abc returns HTTP 400.
   - Assert the error response is mapped consistently through the global exception handler.

### Frontend tests
1. API wrapper success response handling
   - Ensure `fetchProductById(id)` resolves the correct JSON payload and preserves the product data shape.

2. 404 path handling
   - Confirm the wrapper surfaces the HTTP status correctly so the detail page can render the not-found state.

3. Client-side state rendering
   - Validate loading, success, and error states within `ProductDetail.jsx` using the existing route-driven flow.

## Risk controls

### R-1: Response contract drift beyond the approved fields
- Risk: the JPA entity contract grows over time and breaks the compatibility assumption.
- Control: keep the scope strictly to the approved fields and defer additional fields to a dedicated DTO in a later explicit design change.
- Design basis: approved design review explicitly calls out the need to gate any future expansion via a dedicated detail DTO.

### R-2: Error payload semantics diverge from the UI contract
- Risk: inconsistent 400/404 payloads make the UI handle errors incorrectly.
- Control: enforce centralized exception mapping through `GlobalExceptionHandler` and preserve the current UI contract for `error.status` and 404 detection.

### R-3: Invalid input handling is too generic
- Risk: malformed ids are caught as generic client errors, reducing observability and clarity.
- Control: ensure invalid path values are mapped to 400 with a predictable message; treat this as a requirement, not an incidental outcome.

### R-4: Frontend compatibility breaks silently
- Risk: the page and API contract drift apart after the implementation.
- Control: maintain the existing `fetchProductById` pattern and execute regression tests that cover both success and not-found flows.

## Verification outline (Step 7)

1. Backend verification
   - Run the product controller integration tests.
   - Confirm all success, missing-resource, and invalid-id cases pass.

2. Frontend verification
   - Run the product detail API tests and any relevant UI test coverage.
   - Confirm the detail page continues to show loading, success, error, and not-found states.

3. Manual smoke check
   - Navigate to the product details route for an existing product.
   - Verify the page renders product details and that a missing id returns the not-found state without server-side errors.

4. Independent review
   - Confirm the changes do not expand scope beyond the approved story requirements or the design review directions.

## Definition of Done

- GET /api/products/{id} is implemented and available in the backend.
- 200, 404, and 400 responses follow the approved semantics.
- The product payload remains compatible with the existing frontend detail page contract.
- The service and controller layers are aligned with the architecture and not bypassing the approved design.
- Unit and/or integration tests cover the success, not-found, and invalid-id cases.
- The implementation does not introduce redesign work beyond the existing product detail flow.
- The final change set is reviewed against the approved requirements and design-review decisions before sign-off.

## Implementation notes
- This plan intentionally does not include code changes; it is a planning artifact only.
- Execution should follow the numbered dependency order above to minimize rework and maintain a clear validation trail.
