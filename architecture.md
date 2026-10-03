# Architecture Document - EPMCDMETST-67216

## Overview
The solution implements a simple e-commerce catalog with a React frontend and Spring Boot backend. The frontend renders catalog data and sends requests to the backend API. The backend reads product and category data from the database and applies filtering, sorting, and validation rules before returning JSON.

## Component Architecture
- Frontend: React + Vite app that renders products, category selector, search input, sort dropdown, and pagination controls.
- API Layer: Spring Boot REST controllers for products and categories.
- Service Layer: Business logic for filtering, validation, and sorting.
- Data Layer: JPA repositories backed by H2 in test and MySQL-compatible persistence in deployment.
- Data Model: Product and Category entities with a many-to-one relationship.

## Request Flow
1. Browser loads the catalog UI.
2. Frontend requests categories and initial product data.
3. Spring Boot controller receives query parameters.
4. Service layer validates and filters the product collection.
5. Results are serialized as JSON and returned to the UI.
6. UI updates the rendered catalog and pagination state.

## Data Model
- Product: id, name, description, imageUrl, price, category
- Category: id, name

## Design Decisions
- Keep the catalog logic close to the controller/service layer to simplify validation and reuse.
- Use JPA repositories for persistence and lightweight in-memory filtering for small catalog sizes.
- Keep CORS enabled for localhost development.
- Maintain a thin frontend API wrapper to centralize fetch logic and DRY usage across screens.

