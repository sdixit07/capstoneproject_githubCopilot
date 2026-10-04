## Summary
This PR implements the product catalog browsing flow for EPMCDMETST-67216, including search, category filtering, price bounds, sorting, and pagination.
## Changes
- Added catalog validation and filtering to the Spring Boot product API.
- Added a reusable frontend API helper for product/category requests.
- Updated the catalog UI to support search, sort, and pagination controls.
- Added architecture, requirements, and implementation-plan documents.
## Test Evidence
- Backend: ./ecom-project/mvnw -q -Dtest=ProductControllerIT test
- Frontend: cd ecom-front/ecom-catalog-react && npm test
## Limitations
- The repository currently contains an existing Playwright E2E suite that is outside the unit-test scope and intentionally excluded from the Vitest verification run.
## Checklist
- [x] Requirements captured
- [x] Architecture reviewed
- [x] Design review completed
- [x] Implementation plan created
- [x] Backend validation and filtering implemented
- [x] Frontend catalog flow updated
- [x] Automated tests run and passing
