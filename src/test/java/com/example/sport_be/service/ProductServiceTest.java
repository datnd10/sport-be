package com.example.sport_be.service;

import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.product.ProductCreateRequest;
import com.example.sport_be.dto.product.ProductResponse;
import com.example.sport_be.dto.product.ProductUpdateRequest;
import com.example.sport_be.dto.product.ProductVariantRequest;
import com.example.sport_be.dto.product.UpdateProductVariantRequest;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.exception.ConflictException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.CategoryRepository;
import com.example.sport_be.repository.ProductImageRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductService} (Requirement 1.2, 1.3, 1.4, 1.6, 1.7).
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private InventoryService inventoryService;

    private ProductService productService;

    private Category category;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, productVariantRepository, categoryRepository, productImageRepository, inventoryService);
        category = new Category("Vợt cầu lông", "vot-cau-long");
    }

    @Test
    void create_withValidDataAndUniqueSku_persistsProductAndInitializesStockThroughInventoryService() {
        when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.of(category));
        when(productVariantRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariant stockedVariant = new ProductVariant(new Product(category, "Vợt Yonex", BigDecimal.TEN), "SKU-1", "size:4U", BigDecimal.TEN, 10);
        when(inventoryService.adjustManually(any(), anyInt())).thenReturn(stockedVariant);

        ProductVariantRequest variantRequest = new ProductVariantRequest("SKU-1", "size:4U", BigDecimal.TEN, 10, 3);
        ProductCreateRequest request = new ProductCreateRequest(UUID.randomUUID(), "Vợt Yonex", "Mô tả", BigDecimal.TEN, List.of(variantRequest));

        ProductResponse response = productService.create(request);

        assertThat(response.name()).isEqualTo("Vợt Yonex");
        assertThat(response.basePrice()).isEqualTo(BigDecimal.TEN);
        assertThat(response.variants()).hasSize(1);
        assertThat(response.variants().get(0).sku()).isEqualTo("SKU-1");
        assertThat(response.variants().get(0).stockQuantity()).isEqualTo(10);
        verify(inventoryService).adjustManually(any(), anyInt());
    }

    @Test
    void create_withBlankName_throwsValidationException() {
        ProductVariantRequest variantRequest = new ProductVariantRequest("SKU-1", null, BigDecimal.TEN, 10, null);
        ProductCreateRequest request = new ProductCreateRequest(UUID.randomUUID(), "  ", "desc", BigDecimal.TEN, List.of(variantRequest));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_withMissingPrice_throwsValidationException() {
        ProductVariantRequest variantRequest = new ProductVariantRequest("SKU-1", null, BigDecimal.TEN, 10, null);
        ProductCreateRequest request = new ProductCreateRequest(UUID.randomUUID(), "Vợt Yonex", "desc", null, List.of(variantRequest));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_withNonPositivePrice_throwsValidationException() {
        ProductVariantRequest variantRequest = new ProductVariantRequest("SKU-1", null, BigDecimal.TEN, 10, null);
        ProductCreateRequest request = new ProductCreateRequest(UUID.randomUUID(), "Vợt Yonex", "desc", BigDecimal.ZERO, List.of(variantRequest));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void create_withUnknownCategory_throwsResourceNotFoundException() {
        when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        ProductVariantRequest variantRequest = new ProductVariantRequest("SKU-1", null, BigDecimal.TEN, 10, null);
        ProductCreateRequest request = new ProductCreateRequest(UUID.randomUUID(), "Vợt Yonex", "desc", BigDecimal.TEN, List.of(variantRequest));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_withDuplicateSku_throwsConflictExceptionAndDoesNotPersistProduct() {
        when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.of(category));
        when(productVariantRepository.existsBySku("SKU-1")).thenReturn(true);

        ProductVariantRequest variantRequest = new ProductVariantRequest("SKU-1", null, BigDecimal.TEN, 10, null);
        ProductCreateRequest request = new ProductCreateRequest(UUID.randomUUID(), "Vợt Yonex", "desc", BigDecimal.TEN, List.of(variantRequest));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ConflictException.class);

        verify(productRepository, org.mockito.Mockito.never()).save(any(Product.class));
    }

    @Test
    void update_withValidData_persistsUpdatedFields() {
        Product existingProduct = new Product(category, "Old Name", BigDecimal.ONE);
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.findByProduct_IdAndActiveTrue(any())).thenReturn(List.of());

        ProductUpdateRequest request = new ProductUpdateRequest(UUID.randomUUID(), "New Name", "New description", BigDecimal.valueOf(99));
        ProductResponse response = productService.update(productId, request);

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.basePrice()).isEqualTo(BigDecimal.valueOf(99));
    }

    @Test
    void update_withUnknownProductId_throwsResourceNotFoundException() {
        when(productRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        ProductUpdateRequest request = new ProductUpdateRequest(UUID.randomUUID(), "New Name", "desc", BigDecimal.TEN);

        assertThatThrownBy(() -> productService.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withBlankName_throwsValidationException() {
        ProductUpdateRequest request = new ProductUpdateRequest(UUID.randomUUID(), " ", "desc", BigDecimal.TEN);

        assertThatThrownBy(() -> productService.update(UUID.randomUUID(), request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void findAllForAdmin_withoutFilters_returnsPageResponseMatchingRepositoryPage() {
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        Pageable expectedPageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<Product> repositoryPage = new PageImpl<>(List.of(product), expectedPageable, 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);
        when(productVariantRepository.findByProduct_IdAndActiveTrue(any())).thenReturn(List.of());

        PageResponse<ProductResponse> response = productService.findAllForAdmin(0, 20, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("Vợt Yonex");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void findAllForAdmin_withKeyword_delegatesFilteringToRepositorySpecification() {
        Product matching = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        Pageable pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<Product> repositoryPage = new PageImpl<>(List.of(matching), pageable, 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);
        when(productVariantRepository.findByProduct_IdAndActiveTrue(any())).thenReturn(List.of());

        PageResponse<ProductResponse> response = productService.findAllForAdmin(0, 20, "yonex", null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findAllForAdmin_withSortByNameAsc_buildsPageableWithRequestedSort() {
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        Pageable expectedPageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name"));
        Page<Product> repositoryPage = new PageImpl<>(List.of(product), expectedPageable, 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);
        when(productVariantRepository.findByProduct_IdAndActiveTrue(any())).thenReturn(List.of());

        productService.findAllForAdmin(0, 20, null, null, "name", "asc");

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "name"));
    }

    @Test
    void findAllForAdmin_withSortByBasePriceDesc_buildsPageableWithRequestedSort() {
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        Pageable expectedPageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "basePrice"));
        Page<Product> repositoryPage = new PageImpl<>(List.of(product), expectedPageable, 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);
        when(productVariantRepository.findByProduct_IdAndActiveTrue(any())).thenReturn(List.of());

        productService.findAllForAdmin(0, 20, null, null, "basePrice", "desc");

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "basePrice"));
    }

    @Test
    void findAllForAdmin_withInvalidSortBy_fallsBackToDefaultSort() {
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        Pageable expectedPageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<Product> repositoryPage = new PageImpl<>(List.of(product), expectedPageable, 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);
        when(productVariantRepository.findByProduct_IdAndActiveTrue(any())).thenReturn(List.of());

        productService.findAllForAdmin(0, 20, null, null, "unknownField", "asc");

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void updateVariant_withValidData_updatesSkuAttributesPriceAndLowStockThreshold() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);

        UUID variantId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant(product, "SKU-OLD", "size:4U", BigDecimal.TEN, 10);
        ReflectionTestUtils.setField(variant, "id", variantId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(productVariantRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.findByProduct_IdAndActiveTrue(productId)).thenReturn(List.of(variant));

        UpdateProductVariantRequest request = new UpdateProductVariantRequest("SKU-NEW", "size:5U", BigDecimal.valueOf(50), 2);
        ProductResponse response = productService.updateVariant(productId, variantId, request);

        assertThat(variant.getSku()).isEqualTo("SKU-NEW");
        assertThat(variant.getAttributes()).isEqualTo("size:5U");
        assertThat(variant.getPrice()).isEqualTo(BigDecimal.valueOf(50));
        assertThat(variant.getLowStockThreshold()).isEqualTo(2);
        assertThat(variant.getStockQuantity()).isEqualTo(10);
        assertThat(response.variants()).hasSize(1);
        verify(productVariantRepository).save(variant);
    }

    @Test
    void updateVariant_withUnchangedSku_doesNotCheckForDuplicates() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);

        UUID variantId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant(product, "SKU-1", "size:4U", BigDecimal.TEN, 10);
        ReflectionTestUtils.setField(variant, "id", variantId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.findByProduct_IdAndActiveTrue(productId)).thenReturn(List.of(variant));

        UpdateProductVariantRequest request = new UpdateProductVariantRequest("SKU-1", "size:5U", BigDecimal.valueOf(50), null);
        productService.updateVariant(productId, variantId, request);

        verify(productVariantRepository, org.mockito.Mockito.never()).existsBySku(any());
        assertThat(variant.getLowStockThreshold()).isEqualTo(5);
    }

    @Test
    void updateVariant_withSkuUsedByAnotherVariant_throwsConflictException() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);

        UUID variantId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant(product, "SKU-OLD", null, BigDecimal.TEN, 10);
        ReflectionTestUtils.setField(variant, "id", variantId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(productVariantRepository.existsBySku("SKU-TAKEN")).thenReturn(true);

        UpdateProductVariantRequest request = new UpdateProductVariantRequest("SKU-TAKEN", null, BigDecimal.TEN, null);

        assertThatThrownBy(() -> productService.updateVariant(productId, variantId, request))
                .isInstanceOf(ConflictException.class);

        verify(productVariantRepository, org.mockito.Mockito.never()).save(any(ProductVariant.class));
    }

    @Test
    void updateVariant_withVariantBelongingToDifferentProduct_throwsResourceNotFoundException() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);

        UUID otherProductId = UUID.randomUUID();
        Product otherProduct = new Product(category, "Giày Mizuno", BigDecimal.TEN);
        ReflectionTestUtils.setField(otherProduct, "id", otherProductId);

        UUID variantId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant(otherProduct, "SKU-1", null, BigDecimal.TEN, 10);
        ReflectionTestUtils.setField(variant, "id", variantId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        UpdateProductVariantRequest request = new UpdateProductVariantRequest("SKU-1", null, BigDecimal.TEN, null);

        assertThatThrownBy(() -> productService.updateVariant(productId, variantId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productVariantRepository, org.mockito.Mockito.never()).save(any(ProductVariant.class));
    }

    @Test
    void updateVariant_withUnknownProductId_throwsResourceNotFoundException() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        UpdateProductVariantRequest request = new UpdateProductVariantRequest("SKU-1", null, BigDecimal.TEN, null);

        assertThatThrownBy(() -> productService.updateVariant(productId, UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateVariant_withUnknownVariantId_throwsResourceNotFoundException() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);

        UUID variantId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.empty());

        UpdateProductVariantRequest request = new UpdateProductVariantRequest("SKU-1", null, BigDecimal.TEN, null);

        assertThatThrownBy(() -> productService.updateVariant(productId, variantId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteVariant_withValidIds_setsActiveFalseAndKeepsStockQuantity() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);

        UUID variantId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant(product, "SKU-1", "size:4U", BigDecimal.TEN, 10);
        ReflectionTestUtils.setField(variant, "id", variantId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.findByProduct_IdAndActiveTrue(productId)).thenReturn(List.of());

        ProductResponse response = productService.deleteVariant(productId, variantId);

        assertThat(variant.isActive()).isFalse();
        assertThat(variant.getStockQuantity()).isEqualTo(10);
        assertThat(response.variants()).isEmpty();
        verify(productVariantRepository).save(variant);
    }

    @Test
    void deleteVariant_withVariantBelongingToDifferentProduct_throwsResourceNotFoundException() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);

        UUID otherProductId = UUID.randomUUID();
        Product otherProduct = new Product(category, "Giày Mizuno", BigDecimal.TEN);
        ReflectionTestUtils.setField(otherProduct, "id", otherProductId);

        UUID variantId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant(otherProduct, "SKU-1", null, BigDecimal.TEN, 10);
        ReflectionTestUtils.setField(variant, "id", variantId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> productService.deleteVariant(productId, variantId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productVariantRepository, org.mockito.Mockito.never()).save(any(ProductVariant.class));
    }

    @Test
    void deleteVariant_withUnknownProductId_throwsResourceNotFoundException() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteVariant(productId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteVariant_withUnknownVariantId_throwsResourceNotFoundException() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", productId);

        UUID variantId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteVariant(productId, variantId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
