# Architecture EPMCDMETST-67217

## Overview
The product catalog feature is implemented as a layered full-stack design with a React frontend and a Spring Boot backend. The frontend is responsible for rendering product data, user interactions, filters, sort options, and pagination controls. The backend exposes filtered catalog endpoints and validates invalid query parameters before returning product data.

## High-Level Components

### 1. Frontend Layer (React + Vite)
- `ecom-front/ecom-catalog-react`
- UI features:
  - Product list rendering
  - Category selector
  - Keyword search input
  - Price range inputs (optional if required by UX)
  - Sort dropdown
  - Pagination controls
- Responsibilities:
  - Capture user selections and filters
  - Call the REST API with query params
  - Display loading and error states
  - Render product cards in a responsive layout

### 2. API Layer (Spring Boot REST)
- `ecom-project/src/main/java/org/ecom/productcatalog/controller`
- Endpoint exposed:
  - `GET /api/products`
- Query parameters supported:
  - `search`
  - `categoryId`
  - `minPrice`
  - `maxPrice`
  - `sort`
- Responsibilities:
  - Accept HTTP requests from the frontend
  - Validate numeric and range constraints
  - Invoke service layer business logic
  - Return structured 400 responses for invalid values

### 3. Service Layer
- `ecom-project/src/main/java/org/ecom/productcatalog/service`
- Main service:
  - `ProductService`
- Responsibilities:
  - Apply search across product name and description
  - Filter by category ID
  - Filter by min/max price range
  - Sort by price ascending/descending
  - Return consistent product lists for UI rendering

### 4. Persistence Layer
- `ecom-project/src/main/java/org/ecom/productcatalog/repository`
- Repositories:
  - `ProductRepository`
  - `CategoryRepository`
- Responsibilities:
  - Access persisted product and category data
  - Support listing by category ID
  - Provide the data required by service logic

## Request Flow

1. User enters a term in the catalog search box or changes a filter.
2. The React app builds a query string and calls the backend endpoint.
3. Spring Boot controller receives the request and validates values.
4. If validation fails, a `400 Bad Request` response is returned.
5. If validation succeeds, the service applies filters and sorting.
6. The filtered list is returned to the frontend.
7. The React app updates the product list and pagination controls.

## Data Model

### Product
- `id`
- `name`
- `description`
- `price`
- `category`

### Category
- `id`
- `name`

## Validation Rules
- `minPrice` must not be negative
- `maxPrice` must not be negative
- `minPrice` must be less than or equal to `maxPrice`
- Invalid numeric request values must produce a clear error payload

## Error Handling Strategy
- Validation failures are handled at the controller layer
- Structured JSON error payload includes:
  - timestamp
  - status
  - error
  - message
  - path
- Invalid number parsing is mapped to a descriptive bad request response

## Technology Stack
- Frontend: React + Vite + Bootstrap
- Backend: Spring Boot + Java
- Persistence: Spring Data JPA
- Database: H2 for tests / MySQL-compatible persistence layer
- API Contract: REST/JSON

## Design Principles
- Separation of concerns between UI, API, service, and persistence
- Reusable API abstraction on the frontend
- Deterministic filtering and sorting on the backend
- Maintainability through clear responsibilities
- Defensive validation to prevent invalid search states

## Risks and Mitigations
- Risk: Invalid filter values produce inconsistent results
  - Mitigation: centralized validation in controller
- Risk: Ambiguous sort behavior
  - Mitigation: explicit sort values `price,asc` and `price,desc`
- Risk: UI difficult to maintain
  - Mitigation: componentized React layout and reusable API helper
- Risk: Pagination mismatch between frontend and backend
  - Mitigation: explicit `page` and `size` query params and consistent page indexing

## Final Decision
Proceed with implementation using the layered architecture described above. The separation between UI, API, service, and repository layers supports maintainability and future extension.

