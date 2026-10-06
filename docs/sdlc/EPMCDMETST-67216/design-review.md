# Design Review: EPMCDMETST-67216

## Review scope
This review checks the story against the approved requirements and architecture for the product detail endpoint and UI compatibility. The review is limited to design validation and does not change the implementation.

## Findings

| ID | Severity | Area | Risk or gap | Recommendation |
| --- | --- | --- | --- | --- |
| F-1 | MINOR | API contract | The detail endpoint currently returns the JPA `Product` entity. That keeps the current UI working, but it leaves the response contract broader than the story’s minimum requirement and increases the risk of accidental field drift as the model evolves. | Keep the existing contract for this story, and if product detail payloads expand later, add an explicit `ProductDetailResponse` DTO or versioned response model instead of exposing the entire entity unintentionally. |
| F-2 | MINOR | Error handling | The frontend handles 404 and generic failure states correctly today, but the invalid-id path is only surfaced as a generic client-side error. This is acceptable for the current design, yet it makes the UX less precise for malformed user input. | Document the expected 400/404 client behavior and consider a dedicated invalid-id message in the detail page if UX differentiation is needed in a subsequent iteration. |

## Proposed design decisions

| Decision ID | Decision | Resolves finding(s) | Changes architecture.md? |
| --- | --- | --- | --- |
| D-1 | Keep the product-detail lookup in the existing Spring service/repository path and preserve the approved backend 400/404 semantics through `ProductController`, `ProductService`, and `GlobalExceptionHandler`. | F-1, F-2 | No |
| D-2 | If the product-detail response grows beyond the approved fields, introduce a dedicated detail DTO instead of exposing the full entity contract without explicit review. | F-1 | Yes |

## Traceability

| Requirement / AC | Design element | Evidence in code | Status |
| --- | --- | --- | --- |
| FR-1 / AC-1 | `ProductController.getProductById` and `GET /api/products/{id}` | `ecom-project/src/main/java/org/ecom/productcatalog/controller/ProductController.java` | Covered |
| FR-2 / AC-1 | `ProductService.getProductById` and standard HTTP 200 serialisation | `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java` | Covered |
| FR-3 / AC-1 | Product payload includes `id`, `name`, `description`, `price` and current UI fields | `Product` model + UI consumption in `ecom-front/ecom-catalog-react/src/pages/ProductDetail.jsx` | Covered |
| FR-4 / AC-2 | `ResourceNotFoundException` + `GlobalExceptionHandler.handleNotFound` | `ecom-project/src/main/java/org/ecom/productcatalog/exception/GlobalExceptionHandler.java` | Covered |
| FR-5 / AC-3 | `MethodArgumentTypeMismatchException` mapped to 400 | `GlobalExceptionHandler.handleTypeMismatch` | Covered |
| FR-6 / compatibility | `fetchProductById` and `ProductDetail.jsx` compatibility handling | `ecom-front/ecom-catalog-react/src/api/productsApi.js` and `ecom-front/ecom-catalog-react/src/pages/ProductDetail.jsx` | Covered |
| NFR-1 / NFR-2 / NFR-3 | Stable API contract, predictable error payload, no UI redesign | Approved architecture + current implementation alignment | Covered |

## Verdict
APPROVED WITH CHANGES

Rationale:
- The design satisfies the approved architecture and the story requirements for product detail retrieval, not-found handling, invalid-id handling, and frontend compatibility.
- The current implementation is consistent with the intended backend-centric contract and keeps the UI backward compatible.
- The only concerns are minor contract-drift and UX-precision risks, which do not block acceptance but are worth documenting for future iterations.

## Verification evidence
- Backend tests for the product catalog flow are passing in the current workspace.
- `ecom-project/target/surefire-reports/org.ecom.productcatalog.controller.ProductControllerIT.txt` reports: `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`.
- `ecom-project/target/surefire-reports/org.ecom.productcatalog.EcomProjectApplicationTests.txt` reports: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

## Errors
None.
