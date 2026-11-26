package org.ecom.productcatalog.service;

import jakarta.persistence.criteria.Predicate;
import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.dto.ProductPageResponse;
import org.ecom.productcatalog.exception.ResourceNotFoundException;
import org.ecom.productcatalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    @Autowired
    public ProductRepository productRepository;

    public ProductPageResponse getPagedProducts(String search,
                                                Long categoryId,
                                                Double minPrice,
                                                Double maxPrice,
                                                String sort,
                                                Pageable pageable) {
        validatePriceBounds(minPrice, maxPrice);

        Specification<Product> specification = buildSpecification(search, categoryId, minPrice, maxPrice);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                resolveSort(sort)
        );
        Page<Product> productPage = productRepository.findAll(specification, sortedPageable);

        return new ProductPageResponse(
                productPage.getContent(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    public List<Product> getProductByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " was not found"));
    }

    private Specification<Product> buildSpecification(String search,
                                                       Long categoryId,
                                                       Double minPrice,
                                                       Double maxPrice) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String escapedSearch = search.toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                String searchPattern = "%" + escapedSearch + "%";
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("name")),
                        searchPattern,
                        '\\'
                ));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<Double>get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<Double>get("price"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validatePriceBounds(Double minPrice, Double maxPrice) {
        if (minPrice != null && !Double.isFinite(minPrice)) {
            throw new IllegalArgumentException("minPrice must be finite");
        }
        if (maxPrice != null && !Double.isFinite(maxPrice)) {
            throw new IllegalArgumentException("maxPrice must be finite");
        }
        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("minPrice must not be negative");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("maxPrice must not be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice (" + minPrice
                    + ") must be less than or equal to maxPrice (" + maxPrice + ")");
        }
    }

    private Sort resolveSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Order.asc("id"));
        }

        return switch (sort) {
            case "price", "price,asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "price,desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"));
            default -> throw new IllegalArgumentException("Unsupported sort value: " + sort);
        };
    }
}
