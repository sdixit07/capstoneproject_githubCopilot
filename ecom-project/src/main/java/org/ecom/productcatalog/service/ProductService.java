package org.ecom.productcatalog.service;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    public ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> getFilteredProducts(String search, Long categoryId, Double minPrice, Double maxPrice, String sort) {
        List<Product> products = getAllProducts();

        if (search != null && !search.trim().isEmpty()) {
            String normalized = search.trim().toLowerCase(Locale.ROOT);
            products = products.stream()
                    .filter(product -> product.getName() != null && product.getName().toLowerCase(Locale.ROOT).contains(normalized)
                            || product.getDescription() != null && product.getDescription().toLowerCase(Locale.ROOT).contains(normalized))
                    .collect(Collectors.toList());
        }

        if (categoryId != null) {
            products = products.stream()
                    .filter(product -> product.getCategory() != null && categoryId.equals(product.getCategory().getId()))
                    .collect(Collectors.toList());
        }

        if (minPrice != null) {
            products = products.stream()
                    .filter(product -> product.getPrice() >= minPrice)
                    .collect(Collectors.toList());
        }

        if (maxPrice != null) {
            products = products.stream()
                    .filter(product -> product.getPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        if (sort != null && !sort.trim().isEmpty()) {
            String normalizedSort = sort.trim();
            boolean descending = normalizedSort.equalsIgnoreCase("desc") || normalizedSort.endsWith(",desc");
            products.sort(Comparator.comparing(Product::getPrice));
            if (descending) {
                products.sort(Comparator.comparing(Product::getPrice).reversed());
            }
        }

        return products;
    }
}
