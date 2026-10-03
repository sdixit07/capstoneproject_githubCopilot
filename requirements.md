# Requirements Specification - EPMCDMETST-67216

## User Story
As a customer browsing the catalog, I want to search, filter, and sort product listings so that I can quickly identify the right items.

## Functional Requirements
1. The backend shall expose `/api/products` and return a list of products.
2. The client shall support searching by keyword against product names/descriptions.
3. The client shall support filtering products by category.
4. The client shall support filtering products by minimum and maximum price values.
5. The API shall support sorting products by price in ascending or descending order.
6. The catalog shall support pagination across multiple pages of results.
7. The system shall validate invalid filter values and return HTTP 400 with a useful error payload.
8. The `GET /api/categories` endpoint shall return all category data for the catalog filter.

## Non-Functional Requirements
- Response time for product queries should remain acceptable for a small catalog dataset.
- API validation must avoid unsafe or ambiguous parameter parsing.
- CORS must allow the local React development server to access the Spring Boot API.
- The code must remain maintainable and follow DRY principles.

## Acceptance Criteria
- A user can search for a product and see matching results.
- A user can select a category and see only products from that category.
- A user can sort high-to-low or low-to-high by price.
- A user can page through results when more products exist than fit on a page.
- Invalid price queries such as negative/empty/non-numeric values return a 400 response.
- The system supports both backend-driven and front-end-driven catalog filtering without regression.

