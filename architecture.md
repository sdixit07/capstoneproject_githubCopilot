# Architecture and Design - EPMCDMETST-67216

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
- Explicit paginated requests from the frontend will use query parameters such as `page` and `size`.

### AD-3 Minimal-Diff Frontend Refactor
- The current `App.jsx` remains the composition root but will shift from raw `fetch` usage to a small `productsApi.js` abstraction.
- React Router will be added for catalog/detail navigation.

### AD-4 Structured Error Responses
- A global exception handler will produce consistent API error payloads for validation and not-found scenarios.

## Component Breakdown

### Backend Components
- `ProductController`: accepts query parameters and returns arrays or paginated metadata.
- `ProductService`: validates query parameters, computes filters and sorting, and resolves product detail lookups.
- `ProductRepository`: provides dynamic querying support for category and price filtering.
- `GlobalExceptionHandler` and `ApiError`: convert validation failures and missing products into structured responses.

### Frontend Components
- `productsApi.js`: builds query strings safely and exposes `fetchProducts`, `fetchProductById`, and `fetchCategories`.
- `App.jsx`: manages category, search, sorting, pagination, loading, and error state.
- `ProductList.jsx`: renders cards and routes to detail pages.
- `ProductDetail.jsx`: loads product data and handles not-found and retry states.

## Data Flows

### Flow 1: Catalog Search / Filter / Sort
1. User updates search text, category, sort, or page.
2. `App.jsx` calls `fetchProducts(params)`.
3. `productsApi.js` builds the query string and sends the request.
4. `ProductController` delegates to `ProductService`.
5. `ProductService` validates inputs, queries the repository, sorts if needed, and optionally paginates.
6. The response is rendered into the catalog list and pagination controls.

### Flow 2: Product Detail
1. User selects a product card.
2. Router loads `/products/:id`.
3. `ProductDetail.jsx` calls `GET /api/products/{id}`.
4. Backend returns either the product or HTTP 404.
5. UI shows details or a not-found state.

## Implementation Impacted Files
- `ecom-project/src/main/java/org/ecom/productcatalog/controller/ProductController.java`
- `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java`
- `ecom-project/src/main/java/org/ecom/productcatalog/repository/ProductRepository.java`
- `ecom-front/ecom-catalog-react/src/App.jsx`
- `ecom-front/ecom-catalog-react/src/ProductList.jsx`
- `ecom-front/ecom-catalog-react/src/pages/ProductDetail.jsx`
- `ecom-front/ecom-catalog-react/src/api/productsApi.js`
- `ecom-front/ecom-catalog-react/src/main.jsx`
