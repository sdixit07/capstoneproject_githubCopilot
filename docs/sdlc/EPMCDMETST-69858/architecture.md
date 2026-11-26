# EPMCDMETST-69858 Architecture

## Architecture recommendation

Implement pagination at the Spring Boot API and persistence-query layers, not by slicing the full product collection in the browser or loading all products into application memory. `GET /api/products` should always return the paginated envelope, including when `page` and `size` are omitted. Use Spring Data's `Pageable`/`Page` model with an effective default of page `0` and size `10`.

The current endpoint conditionally returns a page only when a pagination argument is supplied; otherwise it returns a raw list. The service currently calls `findAll()` and applies search, category, price, and sort in Java, then slices the list manually. Both behaviors need to change: the no-parameter request must adopt the new envelope contract, and the repository must select/count the same filtered result set page-wise. Keep the existing React catalog behavior out of scope: it already requests products with explicit page and size values and consumes `items`/`totalPages`.

Preserve the existing optional filter and sort meanings by translating them into database predicates and `Sort` on the same product query that supplies the page. Return a `ProductPageResponse` assembled from the repository `Page<Product>`, rather than serializing Spring Data's full `Page` implementation as the public API.

## Component diagram

```text
Browser / React
  |
  | GET /api/products?page=...&size=...&search=...&categoryId=...
  v
ProductController
  | validate/default page and size; create Pageable
  v
ProductService
  | build filter specification and pageable sort
  v
ProductRepository (JpaSpecificationExecutor)
  | paged select + matching count query
  v
Product / Category entities and database
  |
  +--> Spring Data Page<Product>
          |
          v
    ProductPageResponse
    {items, page, size, totalItems, totalPages}

Invalid page/size or filter/sort values
  --> GlobalExceptionHandler --> HTTP 400 ApiError
```

## EPMCDMETST-69858 components and responsibilities

### Spring Boot backend

- `ProductController` (`controller/ProductController.java`)
  - Retains `GET /api/products` and all existing optional search/category/price/sort query parameters.
  - Accepts page and size with defaults `0` and `10`; build a Spring Data `Pageable` (for example with `PageRequest`) after validating the values.
  - Always delegates to the paged service operation and returns the response envelope. Remove the current no-pagination branch that returns `getAllProducts(...)` as an array.
  - Leave `/api/products/{id}` and `/api/products/category/{categoryId}` behavior unchanged.

- `ProductService` (`service/ProductService.java`)
  - Owns the translation of existing catalog filters and sort choices into repository query criteria.
  - Creates the `Pageable` sort while preserving the current default ordering (`id` ascending), exact supported price sort grammar (`price`, `price,asc`, `price,desc`), and tie-break direction. `price` defaults to ascending. Unsupported properties, directions, extra sort segments, or malformed sort values remain HTTP 400. A present blank sort value is invalid; an omitted sort parameter uses ID ascending.
  - Uses page `0` and size `10` as server defaults only; it does not retain a size fallback of `12`.
  - Delegates to the repository's pageable filtered query and maps its content and metadata into `ProductPageResponse`.
  - Keeps existing price-bound validation and inclusive min/max behavior. Search remains a case-insensitive product-name substring match; category and price constraints remain conjunctive.

- `ProductRepository` (`repository/ProductRepository.java`)
  - Continues to extend `JpaRepository<Product, Long>` and adds `JpaSpecificationExecutor<Product>` (or an equivalent Spring Data query abstraction) so the product selection and matching count are performed in the database with a `Pageable`.
  - Exposes `Page<Product> findAll(Specification<Product> specification, Pageable pageable)`, using one `Specification<Product>` instance built for search/category/min-price/max-price predicates.
  - The framework calls that same specification for item selection and count metadata, so `totalItems`/`totalPages` are derived from the identical predicate set as `items`. The existing category-specific lookup stays intact.

- `Product` and `Category` entities
  - Continue to represent the existing product and category model. Product is `org.ecom.productcatalog.Product`; its `category` relationship supplies category filtering.
  - No schema or entity-field change is required.

- `ProductPageResponse` (`dto/ProductPageResponse.java`)
  - Remains the public response DTO with `items`, `page`, `size`, `totalItems`, and `totalPages`.
  - Receives page content and metadata from Spring Data's `Page<Product>`; response metadata reflects the effective requested page/size and the filtered total.

- `GlobalExceptionHandler` and `ApiError`
  - Continue mapping `IllegalArgumentException` and request-parameter type mismatch to HTTP 400 using the existing `{timestamp,status,message,path}` payload.
  - Ensure negative page and zero/negative size are rejected as HTTP 400 before repository access. Do not silently clamp or normalize invalid values.

### React frontend

No frontend behavior change is in scope. These current components remain the existing consumer of the API contract:

- `App.jsx`
  - Holds the current page, page size (`12` today), product list, and total-page state.
  - Calls `fetchProducts` when pagination, search, category, or sort state changes; consumes `data.items` and `data.totalPages`.
  - Continues to send its explicit size, so the server's default size of `10` applies to clients that omit size, not this current UI request.
- `productsApi.js`
  - Continues constructing the query string and returning the JSON response from `GET /api/products`.
  - Requires no API wrapper change for the envelope since it already returns the parsed payload.
- `ProductList.jsx` and `CategoryFilter.jsx`
  - Continue rendering the current product items and filter controls. Pagination controls remain in `App.jsx`.
- `data-testid` hooks
  - No new UI or hooks are required by this backend-only story. For stable existing/future UI tests, recommended hooks are `catalog-page`, `product-list`, `pagination-previous`, `pagination-next`, and `pagination-status`; their addition is explicitly not part of this implementation.

## Technology choices

### Selected

- Spring Data `Pageable`, `PageRequest`, and `Page<Product>`
  - Required by the story and already available through Spring Data JPA.
  - Provides page offset/limit and result metadata without a new dependency.
- Spring Data JPA repository query with `JpaSpecificationExecutor`/JPA criteria
  - Allows existing optional filters to be applied consistently to both page content and count queries, avoiding retrieval of the entire catalog in Java.
  - Uses the current persistence technology and requires no new framework.
  - `JpaSpecificationExecutor.findAll(specification, pageable)` makes one specification responsible for both content and framework count queries.
- Existing `ProductPageResponse`
  - Already defines the required stable JSON fields and avoids coupling clients to Spring Data's implementation-specific `Page` serialization.
- Existing `GlobalExceptionHandler`
  - Reuses the established 400 error contract for invalid query parameters.

### Rejected

- Browser-side pagination or in-memory backend slicing of `findAll()`
  - Rejected because it still retrieves the full catalog, does not satisfy the requested Spring Data `Pageable` data-access flow, and defeats the page-wise retrieval objective.
- Conditional legacy list response when `page` and `size` are absent
  - Rejected because the approved clarification explicitly requires the envelope by default.
- Returning Spring Data `Page<Product>` directly
  - Rejected because the public API should retain the explicitly required five-field response contract, rather than exposing framework serialization details.
- New pagination framework or additional dependency
  - Rejected because Spring Data JPA already supplies the required abstractions.

## Data flow

1. A client requests `GET /api/products`, optionally supplying `page`, `size`, `search`, `categoryId`, `minPrice`, `maxPrice`, and `sort`.
2. `ProductController` resolves omitted pagination inputs to page `0` and size `10`. It rejects negative page and non-positive size with HTTP 400 and creates a `Pageable` for valid input.
3. `ProductService` validates price bounds and translates the current filter and sort semantics into a pageable repository query.
4. `ProductRepository` executes a bounded product query plus the count for the same filtered result set.
5. Spring Data returns `Page<Product>`. A page index past the final page is valid: its content is empty while its requested page/size and matching total metadata remain available.
6. `ProductService` maps the page into `ProductPageResponse`.
7. Spring serializes the envelope as JSON. Invalid request values flow through `GlobalExceptionHandler` as HTTP 400 `ApiError`.
8. The existing React API module parses the JSON; `App.jsx` renders the returned items and uses `totalPages` for its controls.

## LLD

### Backend low-level design

- Entity
  - Use the existing `org.ecom.productcatalog.Product` entity and its `Category` association. No new entity or schema migration is expected.

- Repository
  - Extend `ProductRepository` with `JpaSpecificationExecutor<Product>` while retaining `JpaRepository<Product, Long>` and `findByCategoryId`.
  - Query contract: `Page<Product> findAll(Specification<Product> specification, Pageable pageable)`.
  - One service-owned specification builder combines optional predicates: case-insensitive name contains `search`; matching category ID; `price >= minPrice`; and `price <= maxPrice`.
  - Pass that same immutable specification instance to `findAll(specification, pageable)` for content and framework count. Do not separately query and count with a different predicate path.

- Service
  - The list operation always returns `ProductPageResponse`; there is no separate unpaged code path for this endpoint.
  - Preserve current validation: negative price bounds and `minPrice > maxPrice` remain HTTP 400.
  - Resolve ordering with exact grammar: omitted `sort` means ID ascending; `price` means price ascending; `price,asc` means price ascending then ID ascending; `price,desc` means price descending then ID descending. In both price directions, ID is the tie-breaker in the same direction. Reject any present value other than the exact forms above, including unsupported properties, unsupported directions, extra segments, casing changes, or surrounding whitespace.
  - Server pagination resolution belongs only in the controller: page `null` -> `0`, size `null` -> `10`. The service has no size fallback and must not retain or reintroduce the current fallback of `12`.
  - Map `Page<Product>.getContent()`, effective page index and size, total elements, and total pages into the DTO.

- Controller and validation
  - Keep query parameter names unchanged. Define effective defaults as `page=0` and `size=10`.
  - Validate page `>= 0` and size `>= 1` before constructing `PageRequest`. Non-numeric page/size remains a Spring request binding error and maps to HTTP 400.
  - Do not impose an application maximum size. Before constructing or executing a pageable, calculate the requested `page * size` offset safely (using a representable arithmetic type) and reject with HTTP 400 if the offset cannot be represented or safely processed by the database. This is numeric rejection, not clamping or an unapproved maximum-size policy.
  - Avoid `@PageableDefault` behavior that could obscure the required strict invalid-input behavior; the controller/service may accept a `Pageable` internally while explicitly validating the request values used to construct it.

- Error mapping
  - Negative page, zero/negative size, malformed numeric values, invalid filter ranges, and unsupported sort input return HTTP 400 in the existing `ApiError` shape.
  - A valid out-of-range page is not an error and returns HTTP 200 with an empty `items` list.
  - No new error codes or error schema are introduced.

### Frontend low-level design

- State
  - `App.jsx` continues to manage `products`, `page`, `size`, and `totalPages`, plus current search/category/sort and loading/error state.
  - It does not need `totalItems` for current behavior, but the server includes it in each response.
  - The current app sets size to `12` and sends it explicitly; do not silently change it as part of this backend story.
- API module
  - `productsApi.js` continues sending the caller's query parameters and returning the parsed JSON object.
  - The API contract is envelope-based for all calls, including callers that omit pagination values.
- Components
  - `App.jsx` remains responsible for pagination controls and passes page contents to `ProductList`.
  - No new React state, component, route, or UI behavior is necessary in this story.
- Test hooks
  - No test-hook change required. Recommended selectors for catalog pagination tests are `catalog-page`, `product-list`, `pagination-previous`, `pagination-next`, and `pagination-status`.

## API contract

### Endpoint and query parameters

`GET /api/products`

| Parameter | Optional | Default / behavior |
| --- | --- | --- |
| `page` | Yes | `0`; zero-based, must be non-negative |
| `size` | Yes | `10`; must be greater than zero |
| `search` | Yes | Existing case-insensitive name substring filter |
| `categoryId` | Yes | Existing category filter |
| `minPrice` | Yes | Existing inclusive lower bound |
| `maxPrice` | Yes | Existing inclusive upper bound |
| `sort` | Yes | Existing price sort (`price[,asc|desc]`); default product ID ascending |

Supported sort values are exactly `price`, `price,asc`, or `price,desc`. An omitted sort value uses ID ascending. `price`/`price,asc` use ascending price with ID ascending as the tie-breaker; `price,desc` uses descending price with ID descending as the tie-breaker. Any other present sort value is HTTP 400.

### Success response

- HTTP 200, `Content-Type: application/json`.
- The response is always the following envelope, whether pagination parameters are present or omitted:

```json
{
  "items": [
    {
      "id": 101,
      "name": "Example product",
      "description": "Product description",
      "imageUrl": "/images/example.png",
      "price": 19.99,
      "category": {
        "id": 4,
        "name": "Example category"
      }
    }
  ],
  "page": 0,
  "size": 10,
  "totalItems": 21,
  "totalPages": 3
}
```

- `items` contains at most `size` products. `totalItems` is the count after the active filters; `totalPages` is the corresponding page count (zero when there are no matching products).
- For an out-of-range but valid page, return HTTP 200, `items: []`, the requested page and size, and the same filtered `totalItems`/`totalPages`.
- The product JSON fields remain the current entity representation; this story changes the list response wrapper, not individual product fields.

### Invalid request response

- HTTP 400 for negative page, size `0` or below, malformed pagination values, or invalid existing filter/sort values.
- Reuse the current `ApiError` JSON schema, for example:

```json
{
  "timestamp": "2026-11-26T00:00:00Z",
  "status": 400,
  "message": "size must be greater than 0",
  "path": "/api/products"
}
```

### Compatibility

- The no-parameter response intentionally changes from an array to the envelope; this is explicitly approved in the requirements.
- Existing filter and sort query names and meanings remain unchanged.
- The current React catalog already sends page and size and reads `items` and `totalPages`. Its explicit current size of `12` remains effective; default size `10` applies only when size is omitted.
- Other product endpoints are unaffected.

## ASCII wireframes

No frontend wireframe change is required by this backend-only story. The existing page shape remains:

```text
+--------------------------------------------------------------+
| Product Catalog                                               |
| [Category] [Search products................] [Sort by price]  |
+--------------------------------------------------------------+
| [Product card]          [Product card]          [Product]    |
|                                                              |
| [Previous]                 Page 1 of 3             [Next]    |
+--------------------------------------------------------------+
```

Example API representation (not a UI change):

```text
GET /api/products?page=0&size=10
  -> { items: [... up to 10 ...], page: 0, size: 10,
       totalItems: 21, totalPages: 3 }
```

## Error handling

- `page < 0`: HTTP 400 with `ApiError`.
- `size <= 0`: HTTP 400 with `ApiError`.
- Non-numeric page/size and invalid existing filter/sort inputs: HTTP 400 with `ApiError`.
- Valid page beyond the filtered last page: HTTP 200 with empty `items` and pagination metadata.
- Empty filtered result: HTTP 200, empty `items`, `totalItems: 0`, `totalPages: 0`.
- Database/unexpected failures remain server errors under existing Spring handling; do not convert them to a successful empty page.
- `App.jsx` retains its existing loading/error handling; no frontend error-state change is required.

## Backend HTTP/response verification matrix

The existing legacy integration-test assertions must be replaced; the backend test suite will verify the envelope and the following matrix with MockMvc:

| Case | Request shape | Expected result |
|---|---|---|
| Default pagination | `GET /api/products` | HTTP 200; envelope; `page=0`, `size=10`, items at most 10, filtered metadata |
| Normal first page | `page=0&size=10` | HTTP 200; at most 10 items; `page=0`, `size=10` |
| Normal later page | `page=2&size=10` | HTTP 200; at most 10 items; `page=2`, `size=10` |
| Out-of-range page | valid `page` beyond final page | HTTP 200; `items=[]`; requested page/size and matching total metadata |
| No matching filters | valid filter with no matches | HTTP 200; `items=[]`, `totalItems=0`, `totalPages=0` |
| Negative page | `page=-1&size=10` | HTTP 400 `ApiError` |
| Zero size | `page=0&size=0` | HTTP 400 `ApiError` |
| Negative size | `page=0&size=-1` | HTTP 400 `ApiError` |
| Malformed pagination | non-numeric page or size | HTTP 400 `ApiError` |
| Unrepresentable offset | page/size combination whose safe offset cannot be represented or processed | HTTP 400 `ApiError`; never clamp |
| Combined filters | search/category/minPrice/maxPrice with pagination | HTTP 200; items and metadata use the same conjunctive predicates |
| Default sort | omitted sort | HTTP 200; ID ascending |
| Sort ascending | `sort=price,asc` | HTTP 200; price ascending, ID ascending for ties |
| Bare sort price | `sort=price` | HTTP 200; price ascending, ID ascending for ties |
| Sort descending | `sort=price,desc` | HTTP 200; price descending, ID descending for ties |
| Invalid sort | unsupported field, direction, extra segment, or malformed/present blank value | HTTP 400 `ApiError` |
| Category legacy path | `/api/products/category/{categoryId}` | HTTP 200; existing array behavior unchanged |

## Risks

- **Response contract change:** clients expecting the old no-parameter array will break. This is intentional and approved; identify any consumers outside this repository before release.
- **Database filter equivalence:** moving matching from Java to database criteria must preserve case-insensitive name matching and inclusive price/category behavior. Database collation/case-folding can differ; retain/add integration coverage for the existing behavior.
- **Metadata/content consistency:** selection and count predicates must be identical, or `totalItems` and `totalPages` can disagree with the returned page.
- **Sort stability:** preserve the existing default ordering and price ordering/tie-break behavior to avoid products moving unexpectedly between pages.
- **Unbounded requested size:** the requirements specify no maximum size. Extremely large positive `size` values can still produce expensive requests; any maximum would require a product decision because silently imposing one would change the accepted contract.
- **Numeric offset safety:** no maximum size is introduced. Only offsets that cannot be represented or safely processed are rejected; a very large but representable, processable offset is not clamped.
- **Existing `KNOWN-ISSUES` file unavailable:** `docs/KNOWN-ISSUES.md` is absent from the checked-out repository, so no additional documented known issues could be incorporated.

## Changes after design review

| Decision ID | What changed | Updated architecture location |
|---|---|---|
| D-1 | Replaced the legacy verification gap with an explicit backend HTTP/response test matrix covering server defaults, normal and out-of-range pages, invalid pagination, combined filters/sorts, exact sort forms, and unchanged category-path behavior. | **Backend HTTP/response verification matrix** |
| D-2 | Made the sort contract exact: omitted sort -> ID ascending; `price` -> price ascending; `price,asc` -> price ascending/ID ascending; `price,desc` -> price descending/ID descending. Present unsupported, malformed, or extra-segment values are rejected as HTTP 400. | `ProductService`, `sort` API parameter, LLD service/controller, verification matrix |
| D-3 | Restricted server pagination defaults to page `0` and size `10`; removed the service fallback of `12`. The existing React size `12` remains an explicit caller value and is not the server default. | Component responsibilities, service, controller/validation, API contract, frontend state |
| D-4 | Defined one `Specification<Product>` builder for search/category/min-price/max-price and required it to be passed unchanged to `findAll(specification, pageable)`, which supplies both page content and framework count metadata. | Repository, service/repository data flow, LLD repository/service |
| D-5 | Added a no-maximum-size policy and a safe-offset rule: calculate the requested offset without overflow, reject it with HTTP 400 when it cannot be represented or safely processed, and never clamp valid size values or add an unapproved bound. | Controller/validation LLD, error handling, numeric-safety risk |

## FR → design traceability

| Functional requirement | Design response |
| --- | --- |
| FR1: expose pagination on `GET /api/products` using Spring Data `Pageable` | Controller resolves request values into `Pageable`; service passes it to the Spring Data repository query. |
| FR2: always return envelope with `items`, `page`, `size`, `totalItems`, `totalPages` | `ProductPageResponse` is the sole list response, including when both pagination parameters are omitted. |
| FR3: omitted page defaults to 0; omitted size defaults to 10 | Controller applies those exact effective defaults before building the pageable. |
| FR4: valid page returns 200, no more than size items, effective requested page/size | Repository supplies bounded content; service maps pageable and page metadata into the response DTO. |
| FR5: matching count and corresponding page count | Repository page count uses the same filter specification; map total elements/pages from `Page<Product>`. |
| FR6: beyond-final page returns 200 with empty items and metadata | Spring Data page content is empty while requested page/size and filtered totals are returned normally. |
| FR7: negative page or size zero returns 400 | Validate request values before repository access and use the existing global 400 `ApiError` mapping. |
| FR8: preserve search/category/price/sort parameter meaning | Translate existing optional filters and price ordering into repository predicates/sort; retain names, validation, inclusive bounds, and defaults. |
