package org.ecom.productcatalog.controller;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    @Autowired
    public ProductService productService;

    @GetMapping
    public Object getAllProducts(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) Double minPrice,
                                 @RequestParam(required = false) Double maxPrice,
                                 @RequestParam(required = false) String sort,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            return productService.getPagedProducts(search, categoryId, minPrice, maxPrice, sort, page, size);
        }
        return productService.getAllProducts(search, categoryId, minPrice, maxPrice, sort);
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
