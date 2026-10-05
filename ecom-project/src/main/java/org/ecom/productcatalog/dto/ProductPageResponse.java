package org.ecom.productcatalog.dto;

import org.ecom.productcatalog.Product;

import java.util.List;

public class ProductPageResponse {
    private final List<Product> items;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;

    public ProductPageResponse(List<Product> items, int page, int size, long totalItems, int totalPages) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public List<Product> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }
}

