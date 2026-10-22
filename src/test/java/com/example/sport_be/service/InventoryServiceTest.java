package com.example.sport_be.service;

import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.inventory.InventoryItemResponse;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.repository.InventoryHistoryRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InventoryService#listAllPaged}, covering
 * pagination and keyword search for the Admin Tồn_Kho management list
 * (Requirement 7.3, 7.4).
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    private InventoryService inventoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(productVariantRepository, inventoryHistoryRepository);
        category = new Category("Vợt cầu lông", "vot-cau-long");
    }

    @Test
    void listAllPaged_withoutKeyword_returnsPageResponseSortedBySkuAsc() {
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ProductVariant variant = new ProductVariant(product, "SKU-1", "size:4U", BigDecimal.TEN, 10);
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "sku"));
        Page<ProductVariant> repositoryPage = new PageImpl<>(List.of(variant), expectedPageable, 1);

        when(productVariantRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<InventoryItemResponse> response = inventoryService.listAllPaged(0, 20, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).sku()).isEqualTo("SKU-1");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productVariantRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "sku"));
    }

    @Test
    void listAllPaged_withKeyword_delegatesFilteringToRepositorySpecification() {
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ProductVariant variant = new ProductVariant(product, "SKU-Y1", "size:4U", BigDecimal.TEN, 10);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "sku"));
        Page<ProductVariant> repositoryPage = new PageImpl<>(List.of(variant), pageable, 1);

        when(productVariantRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<InventoryItemResponse> response = inventoryService.listAllPaged(0, 20, "yonex");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(productVariantRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listAllPaged_withSmallPageSize_buildsPageableWithRequestedPaging() {
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ProductVariant variant = new ProductVariant(product, "SKU-1", null, BigDecimal.TEN, 10);
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "sku"));
        Page<ProductVariant> repositoryPage = new PageImpl<>(List.of(variant), pageable, 5);

        when(productVariantRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<InventoryItemResponse> response = inventoryService.listAllPaged(1, 2, null);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
    }
}
