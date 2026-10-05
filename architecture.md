# Architecture and Design - EPMCDMETST-67217

## Design Goals

1. Move catalog filtering and sorting from the browser to the backend.
2. Add reusable frontend API access patterns.
3. Introduce product-detail routing and navigation.
4. Preserve current backend compatibility where tests already enforce legacy response shapes.

## High-Level Architecture

```text
React UI (App, ProductList, ProductDetail)
		|
		v
Frontend API helper (productsApi.js)
		|
		v
Spring Boot REST controllers
		|
		v
Service layer (filtering, validation, pagination orchestration)
		|
		v
Spring Data JPA repositories
		|
		v
Relational persistence (H2 in tests / configured DB in runtime)
```

## Architectural Decisions

### AD-1 Backend-First Query Execution
- Catalog filtering, sorting, and optional pagination will be executed on the server.
- Reason: the current frontend downloads the entire catalog and filters in-memory, which does not scale and does not match the intended test-driven API behavior.

### AD-2 Backward-Compatible Endpoint Strategy
- `GET /api/products` with no query parameters will continue returning a JSON array.
- Explicit paginated requests from the frontend will use query parameters such as `page` and `size`, allowing the backend to return a paginated envelope only when needed.
- Reason: existing backend tests validate array responses for the non-paginated contract.

### AD-3 Minimal-Diff Frontend Refactor
- The current `App.jsx` remains the composition root but will shift from raw `fetch` usage to a small `productsApi.js` abstraction.
- React Router will be added at the entry point and used for catalog/detail navigation.

### AD-4 Structured Error Responses
- A global exception handler will produce consistent API error payloads for validation and not-found scenarios.
- Reason: backend tests already expect JSON with `status`, `message`, and `path`.

## Component Breakdown

### Backend Components

#### `ProductController`
- Accepts query parameters: `search`, `categoryId`, `minPrice`, `maxPrice`, `sort`, `page`, `size`.
- Returns:
  - array responses for legacy non-paginated catalog queries
  - paginated envelope responses for explicit paginated requests
  - single product payload for `GET /api/products/{id}`

#### `ProductService`
- Validates query parameter combinations.
- Orchestrates filter and sort behavior.
- Handles optional pagination transformations.
- Retrieves individual products by ID and raises not-found exceptions.

#### `ProductRepository`
- Continues to support category lookups.
- Adds JPA specification support or equivalent dynamic-query support for combined filters.

#### `GlobalExceptionHandler` and `ApiError`
- Convert validation, parsing, and not-found failures into a stable JSON response format.

### Frontend Components

#### `productsApi.js`
- Builds query strings safely.
- Exposes:
  - `fetchProducts(params)`
  - `fetchProductById(id)`
  - `fetchCategories()`
- Normalizes error handling for UI consumers.

#### `App.jsx`
- Becomes the catalog page container.
- Tracks selected category, search term, sort, current page, page size, loading state, and API errors.
- Fetches catalog data through the API helper.

#### `ProductList.jsx`
- Renders product cards.
- Adds navigable links to the detail page.

#### `ProductDetail.jsx`
- Reuses the detail endpoint.
- Remains responsible for loading, not-found, and retry states.

## Data Contracts

### Non-Paginated Catalog Response
```json
[
  {
	"id": 1,
	"name": "iPhone 1",
	"description": "iPhone desc",
	"imageUrl": null,
	"price": 101.0,
	"category": {
	  "id": 1,
	  "name": "Electronics"
	}
  }
]
```

### Paginated Catalog Response
```json
{
  "items": [],
  "page": 0,
  "size": 12,
  "totalItems": 0,
  "totalPages": 0
}
```

### Error Response
```json
{
  "status": 400,
  "message": "minPrice must not be negative",
  "path": "/api/products"
}
```

## Data Flows

### Flow 1: Catalog Search / Filter / Sort
1. User updates search text, category, sort, or page.
2. `App.jsx` calls `fetchProducts(params)`.
3. `productsApi.js` builds the query string and sends the request.
4. `ProductController` delegates to `ProductService`.
5. `ProductService` validates inputs, queries the repository, sorts if needed, and optionally paginates.
6. The response is rendered into the catalog list and pagination controls.

### Flow 2: Product Detail
1. User selects a product card or detail link.
2. Router loads `/products/:id`.
3. `ProductDetail.jsx` calls `GET /api/products/{id}`.
4. Backend returns either the product or HTTP 404.
5. UI shows product details, not-found, or retryable error state.

### Flow 3: Validation Error
1. Frontend or test sends invalid price range.
2. Backend validation fails in service/controller handling.
3. `GlobalExceptionHandler` returns structured JSON.
4. Client receives a stable message format.

## Security and Quality Considerations

- Validate numeric inputs before processing.
- Restrict sort handling to supported properties to avoid unsafe field evaluation.
- Preserve `@JsonIgnore` on category-to-products back references to avoid recursive serialization.
- Keep frontend API helper focused on known endpoints to reduce request duplication and inconsistent error handling.

## Implementation Impacted Files

- `ecom-project/src/main/java/org/ecom/productcatalog/controller/ProductController.java`
- `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java`
- `ecom-project/src/main/java/org/ecom/productcatalog/repository/ProductRepository.java`
- `ecom-project/src/main/java/org/ecom/productcatalog/model/` (new DTO/error support as needed)
- `ecom-front/ecom-catalog-react/src/App.jsx`
- `ecom-front/ecom-catalog-react/src/ProductList.jsx`
- `ecom-front/ecom-catalog-react/src/pages/ProductDetail.jsx`
- `ecom-front/ecom-catalog-react/src/api/productsApi.js`
- `ecom-front/ecom-catalog-react/src/main.jsx`

