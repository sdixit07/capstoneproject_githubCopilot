package org.ecom.productcatalog.service;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    public ProductRepository productRepository;

    public List<Product> getAllProducts(){
        return productRepository.findAll();
    }

    public List<Product> getProductByCategory(Long categoryId){
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> getFilteredProducts(String search, Long categoryId, Double minPrice, Double maxPrice, String sort) {
        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("minPrice must not be negative");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("maxPrice must not be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice (" + minPrice + ") must be less than or equal to maxPrice (" + maxPrice + ")");
        }

        List<Product> products = productRepository.findAll();

        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase();
            products = products.stream()
                    .filter(product -> product.getName() != null && product.getName().toLowerCase().contains(term))
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

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String sortField = parts[0].trim();
            boolean descending = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());

            if ("price".equalsIgnoreCase(sortField)) {
                products.sort(descending ? Comparator.comparingDouble(Product::getPrice).reversed() : Comparator.comparingDouble(Product::getPrice));
            }
        }

        return products;
    }
}
