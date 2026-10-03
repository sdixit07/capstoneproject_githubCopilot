import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

import { fetchCategories, fetchProducts } from './productsApi';

describe('productsApi', () => {
  const origFetch = globalThis.fetch;

  beforeEach(() => {
    globalThis.fetch = vi.fn();
  });

  afterEach(() => {
    globalThis.fetch = origFetch;
  });

  it('fetchProducts sends query params and returns json', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      json: async () => ({ items: [], page: 0, size: 12, totalItems: 0, totalPages: 0 })
    });

    const data = await fetchProducts({ page: 1, size: 5, search: 'phone', categoryId: 2, sort: 'price,desc' });
    expect(globalThis.fetch).toHaveBeenCalledTimes(1);
    expect(globalThis.fetch.mock.calls[0][0]).toContain('/api/products?');
    expect(data).toHaveProperty('items');
  });

  it('fetchCategories sends request to /api/categories', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      json: async () => ([])
    });

    const data = await fetchCategories();
    expect(Array.isArray(data)).toBe(true);
    expect(globalThis.fetch.mock.calls[0][0]).toContain('/api/categories');
  });
});
