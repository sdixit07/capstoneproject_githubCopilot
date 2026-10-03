const API_BASE_URL = 'http://localhost:8080';

async function requestJson(url, options = {}) {
  const response = await fetch(`${API_BASE_URL}${url}`, {
    headers: { Accept: 'application/json', ...(options.headers || {}) },
    ...options,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Request failed: ${response.status}`);
  }

  return response.json();
}

export async function fetchProducts({ page = 0, size = 12, search = '', categoryId = null, minPrice = null, maxPrice = null, sort = 'price,asc' } = {}) {
  const params = new URLSearchParams();
  if (page != null) params.set('page', String(page));
  if (size != null) params.set('size', String(size));
  if (search) params.set('search', search);
  if (categoryId != null) params.set('categoryId', String(categoryId));
  if (minPrice != null) params.set('minPrice', String(minPrice));
  if (maxPrice != null) params.set('maxPrice', String(maxPrice));
  if (sort) params.set('sort', String(sort));

  return requestJson(`/api/products?${params.toString()}`);
}

export async function fetchCategories() {
  return requestJson('/api/categories');
}

