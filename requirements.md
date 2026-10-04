# Requirements EPMCDMETST-67217
## Functional Requirements
1. The application shall allow users to search products by keyword across product names and descriptions.
2. The application shall allow filtering by product category.
3. The application shall allow filtering by minimum and maximum price thresholds.
4. The application shall allow sorting results by price in ascending or descending order.
5. The application shall paginate product results for improved browsing.
6. The API shall return a 400 Bad Request with a clear error message for invalid query parameters.
## Non-Functional Requirements
1. Search shall be case-insensitive and string-based.
2. Validation shall reject negative or reversed price ranges.
3. The UI and backend shall remain responsive during filtering and paging operations.
4. The implementation shall be maintainable and follow a layered architecture.
## Acceptance Criteria
- Users can search for products from the catalog.
- Users can filter by category.
- Users can set min/max price values.
- Users can sort by price.
- Users can navigate paginated results.
- Invalid inputs return a clear error response.
