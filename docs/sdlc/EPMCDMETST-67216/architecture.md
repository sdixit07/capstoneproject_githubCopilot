# EPMCDMETST-67216 Architecture

## Architecture recommendation

The logic for retrieving a single product belongs in the Spring Boot backend, not in the browser.

Reasoning:
- The story defines a canonical server-side resource lookup at GET /api/products/{id}.
- The backend owns validation and persistence rules for product identity, including resource existence and 404 semantics.
- The React frontend already uses a fetch abstraction and a route-driven detail page; keeping the retrieval in the API keeps the behavior consistent with the existing catalog architecture.
- This matches the repo’s established pattern: list/filter/sort behavior is driven by the backend contract and React state, while the detail-page lookup must be server-authoritative to handle missing products and invalid identifiers correctly.

This is also backward compatible with the existing `ProductDetail` page, which already calls `fetchProductById(id)` and expects a `404` state when the product is missing.

## Component diagram

```text
Browser / React UI
  |
  |  Route: /products/:id
  v
ProductDetail.jsx
  |
  |  fetchProductById(id)
  v
productsApi.js (fetch /api/products/{id})
  |
  |  HTTP GET /api/products/{id}
  v
ProductController
  |
  |  productService.getProductById(id)
  v
ProductService
  |
  |  productRepository.findById(id)
  v
ProductRepository (Spring Data JPA)
  |
  |  Product entity
  v
Database / in-memory persistence
  |
  +--> ResourceNotFoundException / GlobalExceptionHandler
                 |
                 +--> 404 ApiError JSON
                         (or 400 for invalid id format)
```

## EPMCDMETST-67216 components and responsibilities

### Spring Boot backend

- `ProductController`
  - Owns the HTTP API mapping for product detail retrieval.
  - Exposes GET /api/products/{id}.
  - Delegates business logic to `ProductService`.
  - Keeps `@CrossOrigin(origins = "http://localhost:5173")` in place so the Vite frontend can call it locally.

- `ProductService`
  - Validates the retrieval request through the service layer.
  - Calls `productRepository.findById(id)`.
  - Uses `ResourceNotFoundException` for missing products.
  - Keeps the lookup logic independent from HTTP concerns.

- `ProductRepository`
  - Supplies the database lookup by primary key.
  - Reuses the repository abstraction already used by the catalog list endpoints.

- `Product` entity
  - Represents the persisted product record.
  - Includes product identity and the fields already consumed by the detail page (`id`, `name`, `description`, `price`, `imageUrl`, `category`).

- `GlobalExceptionHandler`
  - Centralizes API error handling for missing resources and invalid request inputs.
  - Converts exceptions into predictable JSON payloads and HTTP status codes.

- `ResourceNotFoundException` and `ApiError`
  - Provide a consistent contract for not found response payloads.
  - Ensure the UI can show a clean “product not found” state without guessing.

### React frontend

- `ProductDetail.jsx`
  - Reads the route parameter `:id` via `useParams()`.
  - Tracks loading, product data, not-found state, and generic error state.
  - Calls `fetchProductById(id)` on mount and when route param changes.
  - Renders different views for loading, not found, error, and success.

- `productsApi.js`
  - Contains the client API wrapper for the product detail request.
  - Wraps raw fetch responses, extracts JSON, and preserves HTTP status metadata via `error.status`.
  - Keeps the same API-call style used by the rest of the catalog.

- Routing
  - `main.jsx` defines `/products/:id` and mounts `ProductDetail` for the single-product screen.

## Technology choices

### Selected

- Spring Boot + Spring MVC + RestController
  - Reason: the project already uses this stack and it is the standard place to expose backend endpoints and error handling.
  - Benefit: minimal code and consistent API semantics with the rest of the catalog.

- Spring Data JPA repository lookup by ID
  - Reason: existing code already uses repository-based access patterns.
  - Benefit: no new DAL or persistence framework is needed.

- React `fetch` with route-based page state
  - Reason: the app already uses this pattern for catalog pages and the detail page.
  - Benefit: minimal impact on the current UI behavior and no new client framework.

- `@RestControllerAdvice` for exception mapping
  - Reason: it standardizes 400 and 404 responses across the app.
  - Benefit: the UI gets consistent JSON error bodies with helpful messages.

### Rejected

- Browser-only product lookup in React state
  - Rejected because the app cannot correctly enforce resource existence or a canonical 404 response without a backend authority.

- New framework or custom data layer
  - Rejected because the issue requires an API behavior change, not a platform redesign, and the repo already has the required Spring and React stack.

- UI-only route logic without an API call
  - Rejected because it would break the story requirement for GET /api/products/{id} and the contract expected by the existing `ProductDetail` integration.

## Data flow

1. The user navigates to a product detail URL such as `/products/42`.
2. `ProductDetail.jsx` receives the route param `id` and invokes `fetchProductById(42)`.
3. `productsApi.js` performs a fetch to `http://localhost:8080/api/products/42`.
4. `ProductController.getProductById(Long id)` receives the request.
5. The controller delegates to `ProductService.getProductById(id)`.
6. The service calls `productRepository.findById(id)`.
7. If the row exists, the entity is returned and serialized as JSON.
8. If the row does not exist, the repository returns empty and the service throws `ResourceNotFoundException`.
9. `GlobalExceptionHandler` converts that to HTTP 404 and a JSON `ApiError` response.
10. The frontend catches the error in `ProductDetail.jsx`, sets `notFound` or `error`, and renders the corresponding screen.

## LLD

### Backend low-level design

- Entity
  - `Product` retains the persisted fields already used by the front end.
  - `id` is the primary key used for detail retrieval.

- Repository
  - `ProductRepository` exposes the standard Spring Data identity lookup method.
  - Example contract: `findById(Long id)`.

- Service
  - `getProductById(Long id)`
  - Pseudocode:
    - `return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " was not found"));`

- Controller
  - `@GetMapping("/{id}")`
  - Method signature: `public Product getProductById(@PathVariable Long id)`
  - Returns the entity directly, leaving serialization and error handling to Spring and the `@RestControllerAdvice`.

- Validation and error handling
  - `MethodArgumentTypeMismatchException` leads to 400 for invalid path values.
  - `IllegalArgumentException` leads to 400 for invalid business input.
  - `ResourceNotFoundException` leads to 404 when the product does not exist.
  - `ApiError` includes `timestamp`, `status`, `message`, and `path`.

### Frontend low-level design

- State
  - `loading`: request in flight
  - `product`: resolved product payload
  - `notFound`: 404 detected by the client
  - `error`: fallback error state for other failures

- API module
  - `fetchProductById(id)` runs `fetch(`${API_BASE_URL}/products/${id}`)`.
  - `ensureOk` is responsible for converting non-OK HTTP responses into an Error object with `status` metadata.

- Components
  - `ProductDetail.jsx` has the route-driven page behavior.
  - Success view renders product name, description, price, image, and category details.
  - Error view exposes a retry action and a back-to-catalog link.

- `data-testid` recommendations
  - `data-testid="product-detail-page"`
  - `data-testid="product-detail-loading"`
  - `data-testid="product-detail-not-found"`
  - `data-testid="product-detail-error"`
  - `data-testid="product-detail-name"`

These test hooks are optional but align with the repo’s testability expectations and make the page easier to verify without brittle DOM selection.

## API contract

### Endpoint

- GET /api/products/{id}

### Success response

- HTTP 200 OK
- Content-Type: application/json
- The response body is the product entity as represented by the current Spring model.
- The contract stays backward compatible with `ProductDetail.jsx`, which reads fields such as `id`, `name`, `description`, `price`, `imageUrl`, and `category.name`.
- The story’s acceptance criteria require the response to include at least `id`, `name`, `description`, and `price`; the actual UI also consumes `imageUrl` and `category` fields already present in the domain model.

### Not found response

- HTTP 404 Not Found
- Example payload:

```json
{
  "timestamp": "2026-10-06T12:00:00Z",
  "status": 404,
  "message": "Product with id 999999 was not found",
  "path": "/api/products/999999"
}
```

### Invalid identifier response

- HTTP 400 Bad Request
- Triggered by a non-numeric path value or a bad request format.
- Example:

```json
{
  "timestamp": "2026-10-06T12:00:00Z",
  "status": 400,
  "message": "Invalid value provided for id",
  "path": "/api/products/abc"
}
```

### Compatibility

- The API remains backward compatible with the existing front-end call flow.
- No new UI screen or route redesign is introduced beyond the already existing detail page.

## ASCII wireframes

```text
+--------------------------------------------------------------+
| Product Details                                              |
+--------------------------------------------------------------+
| [ product image ]                                              |
| Product Name                                                  |
| Description text                                             |
| Price: $29.99                                                 |
| Category: Electronics                                         |
| [Back to catalog]                                            |
+--------------------------------------------------------------+
```

```text
+--------------------------------------------------------------+
| Product not found                                             |
+--------------------------------------------------------------+
| The product you are looking for does not exist.               |
| [Back to catalog]                                             |
+--------------------------------------------------------------+
```

```text
+--------------------------------------------------------------+
| Unable to load product                                        |
+--------------------------------------------------------------+
| Error message                                                 |
| [Retry] [Back to catalog]                                     |
+--------------------------------------------------------------+
```

## Error handling

- Missing product ID: 404 Not Found with a clear message.
- Invalid id format: 400 Bad Request via Spring type mismatch handling.
- Server-side error or network outage: generic client error state, allowing the user to retry.
- Frontend state handling:
  - `notFound` for 404
  - `error` for any non-404 failure
  - loading indicator while the request is in flight

This keeps the user experience predictable while preserving the backend responsibility for definitive validation.

## Risks

- Response drift: if the JSON contract changes, the existing `ProductDetail` rendering logic may break.
- Client assumption drift: frontend renderers must continue to tolerate the current fields (`imageUrl`, `category`) even when the story only mentions a reduced field set.
- CORS configuration: the local frontend runs on port 5173 and the backend on port 8080; the existing CORS configuration must remain in place.

## FR → design traceability

| Functional requirement | Design response |
| --- | --- |
| FR1: expose GET /api/products/{id} | `ProductController` exposes `@GetMapping("/{id}")` and keeps the route under `/api/products` |
| FR2: 200 for valid product ID | `ProductService.getProductById` returns the entity from the repository |
| FR3: JSON contains at least id, name, description, price | `Product` entity provides these fields and the response remains serializable as JSON |
| FR4: 404 for missing product | `ResourceNotFoundException` + `GlobalExceptionHandler` map to 404 |
| FR5: 400 for invalid id format | Spring type mismatch handling and `GlobalExceptionHandler` return 400 |
| FR6: remain compatible with existing frontend usage | `ProductDetail.jsx` continues to call `fetchProductById(id)` and render the same success / not-found / error states |

## Summary

This design keeps the single-product retrieval in the Spring Boot API layer for authoritative identity lookup, error semantics, and persistence rules, while preserving the existing React detail page contract and keeping the stack fully aligned with the current repo.
