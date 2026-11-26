package org.ecom.productcatalog.controller;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.dto.ProductPageResponse;
import org.ecom.productcatalog.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    @Autowired
    public ProductService productService;

    @GetMapping
    public ProductPageResponse getAllProducts(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) Double minPrice,
                                              @RequestParam(required = false) Double maxPrice,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 10 : size;

        if (resolvedPage < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (resolvedSize <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }

        long offset = (long) resolvedPage * resolvedSize;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page and size produce an unsupported offset");
        }

        return productService.getPagedProducts(
                search,
                categoryId,
                minPrice,
                maxPrice,
                sort,
                PageRequest.of(resolvedPage, resolvedSize)
        );
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @GetMapping("category/{categoryId}")
    public List<Product> getProductByCategory(@PathVariable Long categoryId){
        return productService.getProductByCategory(categoryId);
    }
}
