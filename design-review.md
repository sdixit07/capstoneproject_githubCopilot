# Design Review - EPMCDMETST-67217

## Review Scope

This review evaluates the architecture proposed in `architecture.md` against the current implementation and the acceptance expectations documented in `requirements.md`.

## Summary Assessment

The architecture is sound for an incremental update to the existing catalog application. The main strengths are backward compatibility, low implementation risk, and clear separation between UI concerns and backend query behavior. The main risks are contract drift between frontend pagination expectations and backend legacy response shapes, plus the need for consistent validation and structured errors.

## Strengths

1. **Compatibility-aware design**
   - The design preserves the existing no-parameter array response for `GET /api/products`, which matches current integration tests.

2. **Separation of concerns**
   - The API helper isolates frontend request logic.
   - The service layer becomes the home for validation, filtering, and sort orchestration.

3. **Incremental adoption path**
   - The design reuses current controllers, models, and pages rather than replacing them wholesale.

4. **Explicit error contract**
   - A standardized error object supports both automated testing and predictable UI behavior.

## Gaps and Risks

### Gap 1 - Pagination contract mismatch
- **Observation**: Frontend tests expect a paginated envelope, while backend integration tests currently assert array responses for `/api/products`.
- **Risk**: A single response shape for all requests could break either the frontend or backend tests.
- **Mitigation**: Return paginated envelopes only when `page` or `size` is supplied; otherwise return arrays for legacy compatibility.

### Gap 2 - Missing product detail endpoint
- **Observation**: `ProductDetail.jsx` calls `GET /api/products/{id}`, but no such endpoint exists.
- **Risk**: Product detail navigation cannot function and will surface runtime errors.
- **Mitigation**: Add a controller endpoint and service method that throws a not-found exception when the ID is missing.

### Gap 3 - Missing frontend API helper
- **Observation**: `productsApi.test.js` imports `productsApi.js`, but the implementation file does not exist.
- **Risk**: Frontend tests fail immediately and request logic remains duplicated.
- **Mitigation**: Create the API helper with query-string generation and error handling.

### Gap 4 - Unrestricted sort parsing
- **Observation**: Accepting arbitrary sort fields can lead to fragile or unsafe behavior.
- **Risk**: Unsupported properties may fail unpredictably or leak implementation details.
- **Mitigation**: Constrain sorting to an allow-list of supported fields, initially `price` only.

### Gap 5 - Validation consistency
- **Observation**: Error handling is currently ad hoc or absent.
- **Risk**: Similar invalid requests could return inconsistent payloads and statuses.
- **Mitigation**: Centralize validation and exception translation in a global exception handler.

## Security Review

### Input Validation
- Validate numeric filters before querying.
- Reject negative values for price bounds.
- Reject invalid range combinations where `minPrice > maxPrice`.

### Query Safety
- Use typed repository/service filtering rather than string-built SQL.
- Limit sort parsing to known supported values.

### Serialization Safety
- Preserve the existing category-to-products `@JsonIgnore` to avoid recursive object graphs.

### Cross-Origin Access
- Existing controllers are restricted to `http://localhost:5173`, which is acceptable for local development.
- No change required for this stage, but environment-based configuration would be preferable in future hardening.

## Operational Review

### Testability
- Backend integration tests already describe the expected filter behavior.
- Frontend unit tests already define the API helper contract.
- Additional verification will be needed for routing and detail navigation behavior.

### Maintainability
- The proposed API helper and error classes reduce duplication.
- A paginated response DTO keeps API response structure explicit.

## Recommended Adjustments Before Implementation

1. Introduce a dedicated DTO for paginated product responses.
2. Add a dedicated API error DTO and centralized exception handler.
3. Use repository specifications or composable in-memory filtering with deterministic sorting, depending on the smallest robust change that satisfies tests.
4. Add React Router dependency and route wiring at the app entry point.
5. Ensure the catalog UI gracefully handles both loading and error states.

## Go/No-Go Decision

**Decision: GO**

The design is approved for implementation provided the compatibility strategy is followed:
- legacy array responses remain intact for no-parameter catalog requests
- paginated envelopes are used only for explicit paginated frontend requests
- structured error handling is implemented centrally

