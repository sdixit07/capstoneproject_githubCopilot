package org.ecom.productcatalog.controller;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.model.Category;
import org.ecom.productcatalog.repository.CategoryRepository;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long electronicsId;
    private Long clothingId;
    private Long firstEqualPriceProductId;
    private Long secondEqualPriceProductId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category electronics = new Category();
        electronics.setName("Electronics");
        electronics = categoryRepository.save(electronics);
        electronicsId = electronics.getId();

        Category clothing = new Category();
        clothing.setName("Clothing");
        clothing = categoryRepository.save(clothing);
        clothingId = clothing.getId();

        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            products.add(product("iPhone " + i, 100.0 + i, electronics));
        }
        products.add(product("Socks", 19.99, clothing));
        products.add(product("Winter jacket", 39.99, clothing));
        products.add(product("Equal price A", 50.0, electronics));
        products.add(product("Equal price B", 50.0, electronics));
        List<Product> savedProducts = productRepository.saveAll(products);
        firstEqualPriceProductId = savedProducts.get(27).getId();
        secondEqualPriceProductId = savedProducts.get(28).getId();
    }

    @Test
    void getProducts_withoutPagination_returnsDefaultEnvelope() throws Exception {
        mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(29))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getProducts_laterPageReturnsBoundedItemsAndMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(9)))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(29))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getProducts_pageBeyondLast_returnsEmptyItemsAndMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "5")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(29))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getProducts_filtersAreConjunctiveAndUseMatchingTotals() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("search", "IPHONE 2")
                        .param("categoryId", String.valueOf(electronicsId))
                        .param("minPrice", "120")
                        .param("maxPrice", "125")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].name",
                        containsInAnyOrder("iPhone 20", "iPhone 21", "iPhone 22")))
                .andExpect(jsonPath("$.items[*].price", everyItem(allOf(
                        greaterThanOrEqualTo(120.0),
                        lessThanOrEqualTo(125.0)))))
                .andExpect(jsonPath("$.totalItems").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getProducts_noMatchingFilters_returnsEmptyEnvelope() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("search", "not-a-product")
                        .param("size", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(4))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void getProducts_defaultSortUsesProductIdAscending() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("iPhone 1"))
                .andExpect(jsonPath("$.items[1].name").value("iPhone 2"));
    }

    @Test
    void getProducts_priceSortUsesPriceAndIdTieBreakers() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("sort", "price,asc")
                        .param("size", "29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[2].id").value(firstEqualPriceProductId))
                .andExpect(jsonPath("$.items[3].id").value(secondEqualPriceProductId));

        mockMvc.perform(get("/api/products")
                        .param("sort", "price,desc")
                        .param("size", "29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[25].id").value(secondEqualPriceProductId))
                .andExpect(jsonPath("$.items[26].id").value(firstEqualPriceProductId));

        mockMvc.perform(get("/api/products")
                        .param("sort", "price")
                        .param("size", "29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[2].id").value(firstEqualPriceProductId))
                .andExpect(jsonPath("$.items[3].id").value(secondEqualPriceProductId));
    }

    @Test
    void getProducts_invalidPagination_returnsApiError() throws Exception {
        for (String[] parameters : List.of(
                new String[]{"page", "-1"},
                new String[]{"size", "0"},
                new String[]{"size", "-1"},
                new String[]{"page", "not-a-number"},
                new String[]{"size", "not-a-number"})) {
            mockMvc.perform(get("/api/products").param(parameters[0], parameters[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.path").value("/api/products"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Test
    void getProducts_unrepresentableOffset_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", String.valueOf(Integer.MAX_VALUE))
                        .param("size", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("page and size produce an unsupported offset"));
    }

    @Test
    void getProducts_invalidSortValues_returnBadRequest() throws Exception {
        for (String sort : List.of("", " ", "Price", "name", "price,ASC", "price,desc,extra", "price,descending")) {
            mockMvc.perform(get("/api/products").param("sort", sort))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.path").value("/api/products"));
        }
    }

    @Test
    void getProducts_preservesPriceValidationAndInclusiveBounds() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("minPrice", "125")
                        .param("maxPrice", "125"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].price").value(125.0))
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(get("/api/products")
                        .param("minPrice", "500")
                        .param("maxPrice", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getProductsByCategoryPath_keepsArrayResponse() throws Exception {
        mockMvc.perform(get("/api/products/category/{categoryId}", clothingId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Socks")));
    }

    private Product product(String name, double price, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(price);
        product.setCategory(category);
        return product;
    }
}
