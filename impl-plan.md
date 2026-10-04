# Implementation Plan EPMCDMETST-67217

## Objective
Implement the product catalog enhancement that supports search, category filtering, price filtering, sorting, and pagination with proper validation and error handling.

## Phase 1: Backend query and validation
- Add or update the product controller endpoint to accept query params for:
  - `search`
  - `categoryId`
  - `minPrice`
  - `maxPrice`
  - `sort`
- Validate:
  - no negative price values
  - minPrice <= maxPrice
  - invalid numeric values produce a clear 400 response
- Ensure controller error payload is consistent and readable.

## Phase 2: Service-layer filtering and sorting
- Reuse a single service method for filtered catalog retrieval.
- Apply filters in order:
  1. Search by keyword
  2. Category filter
  3. Minimum price filter
  4. Maximum price filter
  5. Sort by price
- Keep logic deterministic and centralised for maintainability.

## Phase 3: Frontend API wrapper and UI flow
- Create a reusable `productsApi` abstraction for `fetchProducts` and `fetchCategories`.
- Use query params consistent with backend expectations.
- Implement search input, category selection, and price filter interactions.
- Integrate sorting and paging controls into the main catalog screen.

## Phase 4: Frontend state handling and UX
- Add loading state for product fetches.
- Add empty state for products with no matches.
- Add error alert state for failed requests.
- Reset page index to 1 when search, category, or sort changes.

## Phase 5: Testing and verification
- Run backend integration tests for controller and validation behavior.
- Run frontend unit tests for the API wrapper.
- Verify no regressions in catalog behavior.
- Confirm the total test suite meets or exceeds 85% coverage.

## Dependencies
1. Requirements and architecture docs must be in place.
2. Backend query contract must be implemented before frontend call wiring.
3. Frontend UI depends on the reusable API abstraction.
4. Validation and UX updates are completed before final review.

## Success Criteria
- Users can search, filter, sort, and page through products.
- Invalid queries return clear 400 responses.
- The feature behaves consistently across frontend and backend.
- All tests pass and coverage remains above the target threshold.

