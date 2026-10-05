# Requirements Specification - EPMCDMETST-67216

- **User Story**: As a shopper, I want the product catalog to support server-side search, category filtering, price filtering, sorting, pagination, and product detail retrieval so that I can browse large catalogs efficiently and inspect an individual product without downloading the entire catalog.
- **Story Title**: Product Catalog Browsing Flow
- **Application Under Change**: `sdixit07/capstoneproject_githubCopilot`
- **Source of Truth in Fallback Mode**: Existing backend integration tests, frontend API tests, and local implementation gaps in the workspace.

## Business Goal
Complete the product catalog experience by moving query behavior to the backend, formalizing validation/error handling, and wiring the frontend to the richer API contract.

## Functional Requirements

### FR-1 Catalog Retrieval
- The system shall return the full catalog from `GET /api/products` when no filter parameters are supplied.
- The system shall preserve compatibility with `GET /api/products/category/{categoryId}`.

### FR-2 Search and Filtering
- The system shall support case-insensitive `search` by product name.
- The system shall support filtering by `categoryId`.
- The system shall support filtering by `minPrice`.
- The system shall support filtering by `maxPrice`.
- The system shall support combining `search`, `categoryId`, `minPrice`, and `maxPrice` in a single request.

### FR-3 Sorting
- The system shall support a `sort` query parameter such as `price,asc` and `price,desc`.
- If sorting is omitted, the system should return a stable deterministic order suitable for repeatable verification.

### FR-4 Validation and Error Handling
- The system shall reject negative `minPrice` or `maxPrice` values with HTTP 400.
- The system shall reject requests where `minPrice > maxPrice` with HTTP 400.
- The system shall return structured JSON error payloads containing at least `status`, `message`, and `path`.
- The system shall return HTTP 400 for malformed numeric query parameters.

### FR-5 Product Detail
- The system shall expose `GET /api/products/{id}`.
- The system shall return HTTP 404 when a requested product does not exist.

### FR-6 Frontend API Integration
- The frontend shall use a reusable API helper module for catalog and category requests.
- The frontend shall request filtered/sorted catalog data from the backend instead of filtering the full dataset in memory.
- The frontend shall support page navigation using pagination metadata.

### FR-7 Product Detail Navigation
- Each catalog card shall provide a path to the product detail page.
- The application shall support route handling for both catalog and product detail views.

## Non-Functional Requirements

### NFR-1 Maintainability
- Backend query logic shall remain modular and testable.
- Frontend request construction shall be centralized in one API helper.

### NFR-2 Compatibility
- Existing tests for category retrieval and filter behavior shall remain supported.
- The implementation shall preserve the current Spring Boot and React/Vite project structure.

### NFR-3 Usability
- The catalog UI shall present clear empty-state and request-failure messages.
- Pagination controls shall be simple and require no manual URL manipulation.

### NFR-4 Quality Gates
- Backend and frontend automated tests shall pass.
- Verification should demonstrate alignment with the repository's documented 85% coverage threshold where existing tooling provides measurable evidence.

## Acceptance Criteria
1. `GET /api/products` returns all products in JSON array form when called without query parameters.
2. `GET /api/products?search=iPhone%202&categoryId=<electronicsId>` returns at least one matching Electronics product.
3. `GET /api/products?sort=price,desc` returns results ordered by descending price.
4. `GET /api/products?minPrice=120` excludes products priced below 120.
5. `GET /api/products?maxPrice=39.99` returns only the low-price matches in the seeded dataset.
6. `GET /api/products?minPrice=110&maxPrice=115` returns only products inside the inclusive range.
7. `GET /api/products?minPrice=500&maxPrice=100` returns HTTP 400 with structured error JSON.
8. `GET /api/products?minPrice=-10` returns HTTP 400 with a descriptive validation message.
9. `GET /api/products/{id}` returns the product when present and HTTP 404 when absent.
10. Frontend catalog pages use a dedicated API helper and support pagination plus detail navigation.

## Traceability to Workspace Evidence
- Backend controller expectations: `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java`
- Frontend API helper contract: `ecom-front/ecom-catalog-react/src/api/productsApi.test.js`
- Product detail expectations: `ecom-front/ecom-catalog-react/src/pages/ProductDetail.jsx`
- Current UI composition: `ecom-front/ecom-catalog-react/src/App.jsx`
