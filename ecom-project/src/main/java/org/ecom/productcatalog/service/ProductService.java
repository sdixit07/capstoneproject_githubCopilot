package org.ecom.productcatalog.service;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.dto.ProductPageResponse;
import org.ecom.productcatalog.exception.ResourceNotFoundException;
import org.ecom.productcatalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 12;

    @Autowired
    public ProductRepository productRepository;

    public List<Product> getAllProducts(String search, Long categoryId, Double minPrice, Double maxPrice, String sort) {
        validatePriceBounds(minPrice, maxPrice);
        return applyFilters(search, categoryId, minPrice, maxPrice, sort);
    }

    public ProductPageResponse getPagedProducts(String search,
                                                Long categoryId,
                                                Double minPrice,
                                                Double maxPrice,
                                                String sort,
                                                Integer page,
                                                Integer size) {
        validatePriceBounds(minPrice, maxPrice);
        List<Product> filteredProducts = applyFilters(search, categoryId, minPrice, maxPrice, sort);

        int resolvedPage = page == null ? DEFAULT_PAGE : page;
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : size;

        if (resolvedPage < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (resolvedSize <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }

        int totalItems = filteredProducts.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / resolvedSize);
        int fromIndex = Math.min(resolvedPage * resolvedSize, totalItems);
        int toIndex = Math.min(fromIndex + resolvedSize, totalItems);

        return new ProductPageResponse(
                filteredProducts.subList(fromIndex, toIndex),
                resolvedPage,
                resolvedSize,
                totalItems,
                totalPages
        );
    }

    public List<Product> getProductByCategory(Long categoryId){
        return productRepository.findByCategoryId(categoryId);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " was not found"));
    }

    private List<Product> applyFilters(String search, Long categoryId, Double minPrice, Double maxPrice, String sort) {
        return productRepository.findAll().stream()
                .filter(product -> matchesSearch(product, search))
                .filter(product -> matchesCategory(product, categoryId))
                .filter(product -> matchesMinPrice(product, minPrice))
                .filter(product -> matchesMaxPrice(product, maxPrice))
                .sorted(resolveSort(sort))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void validatePriceBounds(Double minPrice, Double maxPrice) {
        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("minPrice must not be negative");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("maxPrice must not be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice (" + minPrice + ") must be less than or equal to maxPrice (" + maxPrice + ")");
        }
    }

    private boolean matchesSearch(Product product, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        return product.getName() != null
                && product.getName().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private boolean matchesCategory(Product product, Long categoryId) {
        if (categoryId == null) {
            return true;
        }
        return product.getCategory() != null && Objects.equals(product.getCategory().getId(), categoryId);
    }

    private boolean matchesMinPrice(Product product, Double minPrice) {
        return minPrice == null || product.getPrice() >= minPrice;
    }

    private boolean matchesMaxPrice(Product product, Double maxPrice) {
        return maxPrice == null || product.getPrice() <= maxPrice;
    }

    private Comparator<Product> resolveSort(String sort) {
        Comparator<Product> baseComparator = Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo));
        if (sort == null || sort.isBlank()) {
            return baseComparator;
        }

        String[] sortParts = sort.split(",", 2);
        String property = sortParts[0].trim();
        String direction = sortParts.length > 1 ? sortParts[1].trim().toLowerCase(Locale.ROOT) : "asc";

        if (!"price".equals(property)) {
            throw new IllegalArgumentException("Unsupported sort field: " + property);
        }

        Comparator<Product> comparator = Comparator.comparing(Product::getPrice).thenComparing(baseComparator);
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }
}
