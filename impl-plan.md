# Implementation Plan - EPMCDMETST-67216

- **Task ID**: EPMCDMETST-67216
- **Description**: Complete the product catalog API and frontend integration for server-side filtering, product detail navigation, reusable API access, and structured verification artifacts.

## Dependency-Ordered Task Breakdown

### Task 1 - Establish delivery branch
- Create `feature/capstone-EPMCDMETST-67216` from `main`.
- Keep all commits isolated to the feature branch.

### Task 2 - Implement backend query and detail support
- Extend `ProductController` to support query parameters and product-by-id retrieval.
- Extend `ProductService` to support search, category filtering, price filters, sorting, pagination, and product lookup.
- Extend `ProductRepository` with dynamic query support.

### Task 3 - Implement backend validation and error contracts
- Add an API error DTO.
- Add a not-found exception.
- Add a global exception handler.
- Validate price filters and malformed inputs.

### Task 4 - Add frontend API helper
- Create `ecom-front/ecom-catalog-react/src/api/productsApi.js`.
- Implement `fetchProducts`, `fetchProductById`, and `fetchCategories`.

### Task 5 - Refactor frontend catalog page
- Update `App.jsx` to manage filters, paging, loading, and error states.
- Add pagination controls and empty-state messaging.

### Task 6 - Wire routing and detail navigation
- Add router setup in `main.jsx`.
- Update `ProductList.jsx` to navigate to detail pages.
- Update `ProductDetail.jsx` to use the reusable API helper.

### Task 7 - Run automated verification and fix regressions
- Run backend controller tests.
- Run frontend unit tests and build.
- Fix regressions until stable.

### Task 8 - Commit, push, and prepare final PR
- Commit with a message referencing `EPMCDMETST-67216`.
- Push `feature/capstone-EPMCDMETST-67216` to `origin`.
- Prepare final PR summary and completion package.

## Verification Plan
1. Backend: run controller integration tests.
2. Frontend: run `vitest` and `vite build`.
3. Delivery: confirm the feature branch is pushed and PR-ready.
