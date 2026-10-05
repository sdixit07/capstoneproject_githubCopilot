# Implementation Plan - EPMCDMETST-67217

- **Task ID**: EPMCDMETST-67217
- **Description**: Complete the product catalog API and frontend integration for server-side filtering, product detail navigation, reusable API access, and structured verification artifacts.

## Delivery Strategy

Implement the smallest robust backend and frontend changes that satisfy the current automated tests while preserving backward compatibility for existing endpoint consumers.

## Dependency-Ordered Task Breakdown

### Task 1 - Establish delivery branch
- Create `feature/capstone-EPMCDMETST-67217` from `main`.
- Keep all commits isolated to the feature branch.
- **Dependencies**: none
- **Estimated Effort**: Low

### Task 2 - Implement backend query and detail support
- Extend `ProductController` to support query parameters and product-by-id retrieval.
- Extend `ProductService` to support:
  - search
  - category filtering
  - price bounds
  - sorting
  - optional pagination
  - product lookup by ID
- Extend `ProductRepository` with dynamic query support.
- **Dependencies**: Task 1
- **Estimated Effort**: Medium

### Task 3 - Implement backend validation and error contracts
- Add API error DTO.
- Add not-found exception.
- Add global exception handler.
- Validate price filters and invalid ranges.
- Normalize malformed parameter handling into structured JSON responses.
- **Dependencies**: Task 2
- **Estimated Effort**: Medium

### Task 4 - Add backend pagination response DTO
- Introduce a response wrapper for paginated catalog responses.
- Ensure explicit paginated requests return metadata without breaking legacy array responses.
- **Dependencies**: Task 2
- **Estimated Effort**: Low

### Task 5 - Add frontend API helper
- Create `src/api/productsApi.js`.
- Implement `fetchProducts`, `fetchProductById`, and `fetchCategories`.
- Align with existing unit test expectations for paginated product retrieval.
- **Dependencies**: Task 4
- **Estimated Effort**: Low

### Task 6 - Refactor frontend catalog page
- Update `App.jsx` to:
  - load categories via API helper
  - request paginated catalog data from the backend
  - manage search, sort, category, paging, loading, and error states
- Add pagination controls and empty-state messaging.
- **Dependencies**: Task 5
- **Estimated Effort**: Medium

### Task 7 - Wire routing and detail navigation
- Add router setup in `main.jsx`.
- Update `ProductList.jsx` to navigate to detail pages.
- Update `ProductDetail.jsx` to use the reusable API helper.
- Add any needed dependency for routing.
- **Dependencies**: Task 5
- **Estimated Effort**: Low

### Task 8 - Run automated verification and fix regressions
- Run backend integration tests.
- Run frontend unit tests.
- Run frontend build/lint as practical.
- Fix issues until the feature is stable.
- **Dependencies**: Tasks 2 through 7
- **Estimated Effort**: Medium

### Task 9 - Commit and push implementation
- Stage documentation and code changes.
- Commit with a traceable message referencing `EPMCDMETST-67217`.
- Push `feature/capstone-EPMCDMETST-67217` to `origin` immediately after implementation stabilizes.
- **Dependencies**: Task 8
- **Estimated Effort**: Low

### Task 10 - Produce final review and completion package
- Create `code-review.md` with the 7-point review.
- Generate `COMPLETION_SUMMARY.md` with test evidence and coverage observations.
- Prepare the final PR body/changelog package.
- **Dependencies**: Task 9
- **Estimated Effort**: Low

## Verification Plan

1. Backend:
   - run `ProductControllerIT`
   - run the broader Maven test suite if practical
2. Frontend:
   - run `vitest`
   - run `vite build`
3. Delivery:
   - inspect git status
   - verify feature branch is pushed to remote

## Risks to Monitor During Implementation

1. Response-shape mismatch between old backend tests and new frontend pagination needs.
2. Routing dependency addition may require install/update before frontend tests can run.
3. Existing frontend test file contains a matcher typo and may need correction to become executable.

