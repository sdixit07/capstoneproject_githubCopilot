package org.ecom.productcatalog.service;

import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.dto.ProductPageResponse;
import org.ecom.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Captor
    private ArgumentCaptor<Specification<Product>> specificationCaptor;

    @InjectMocks
    private ProductService productService;

    @Test
    void getPagedProducts_usesOneSpecificationAndMapsPageMetadata() {
        Pageable requestedPage = PageRequest.of(2, 3);
        Product product = new Product();
        product.setId(7L);
        product.setName("Filtered product");
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    return new PageImpl<>(List.of(product), pageable, 21);
                });

        ProductPageResponse response = productService.getPagedProducts(
                "product", 4L, 10.0, 20.0, null, requestedPage);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());

        assertNotNull(specificationCaptor.getValue());
        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(3, pageableCaptor.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Order.asc("id")), pageableCaptor.getValue().getSort());
        assertEquals(List.of(product), response.getItems());
        assertEquals(2, response.getPage());
        assertEquals(3, response.getSize());
        assertEquals(21, response.getTotalItems());
        assertEquals(7, response.getTotalPages());
    }

    @Test
    void getPagedProducts_appliesExactSupportedSorts() {
        Map<String, Sort> supportedSorts = Map.of(
                "price", Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id")),
                "price,asc", Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id")),
                "price,desc", Sort.by(Sort.Order.desc("price"), Sort.Order.desc("id"))
        );
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(),
                        invocation.getArgument(1),
                        0
                ));

        supportedSorts.forEach((value, expectedSort) -> {
            productService.getPagedProducts(null, null, null, null, value, PageRequest.of(0, 10));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(productRepository, org.mockito.Mockito.atLeastOnce())
                    .findAll(ArgumentMatchers.<Specification<Product>>any(), pageableCaptor.capture());
            assertEquals(expectedSort, pageableCaptor.getValue().getSort());
            org.mockito.Mockito.clearInvocations(productRepository);
        });
    }

    @Test
    void getPagedProducts_rejectsUnsupportedSortValues() {
        for (String sort : List.of("", " ", "Price", "name", "price,ASC", "price,desc,extra", "price,descending")) {
            assertThrows(IllegalArgumentException.class,
                    () -> productService.getPagedProducts(null, null, null, null, sort, PageRequest.of(0, 10)),
                    sort);
        }
        org.mockito.Mockito.verifyNoInteractions(productRepository);
    }

    @Test
    void getPagedProducts_rejectsInvalidPriceBoundsBeforeQuerying() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getPagedProducts(null, null, 20.0, 10.0, null, PageRequest.of(0, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> productService.getPagedProducts(null, null, -1.0, null, null, PageRequest.of(0, 10)));
        org.mockito.Mockito.verifyNoInteractions(productRepository);
    }
}
