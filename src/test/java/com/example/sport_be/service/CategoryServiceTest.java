package com.example.sport_be.service;

import com.example.sport_be.dto.category.CategoryResponse;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.entity.Category;
import com.example.sport_be.exception.ConflictException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CategoryService}'s Admin pagination/search
 * ({@link CategoryService#findAllForAdmin}) and rename
 * ({@link CategoryService#update}) behavior.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void findAllForAdmin_withoutKeyword_returnsPageResponseSortedByNameAsc() {
        Category category = new Category("Vợt cầu lông", "vot-cau-long");
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        Page<Category> repositoryPage = new PageImpl<>(List.of(category), expectedPageable, 1);

        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<CategoryResponse> response = categoryService.findAllForAdmin(0, 20, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("Vợt cầu lông");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(categoryRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Test
    void findAllForAdmin_withKeyword_delegatesFilteringToRepositorySpecification() {
        Category matching = new Category("Vợt Yonex", "vot-yonex");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        Page<Category> repositoryPage = new PageImpl<>(List.of(matching), pageable, 1);

        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<CategoryResponse> response = categoryService.findAllForAdmin(0, 20, "yonex");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(categoryRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findAllForAdmin_withSmallPageSize_buildsPageableWithRequestedPaging() {
        Category category = new Category("Vợt Yonex", "vot-yonex");
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "name"));
        Page<Category> repositoryPage = new PageImpl<>(List.of(category), pageable, 5);

        when(categoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<CategoryResponse> response = categoryService.findAllForAdmin(1, 2, null);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
    }

    @Test
    void update_withValidNewName_recomputesSlugAndPersists() {
        UUID id = UUID.randomUUID();
        Category category = new Category("Vợt cầu lông", "vot-cau-long");
        ReflectionTestUtils.setField(category, "id", id);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.findBySlug("vot-yonex")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.update(id, "Vợt Yonex");

        assertThat(response.name()).isEqualTo("Vợt Yonex");
        assertThat(response.slug()).isEqualTo("vot-yonex");
        verify(categoryRepository).save(category);
    }

    @Test
    void update_withNameProducingSameSlug_doesNotCheckForDuplicatesAndPersists() {
        UUID id = UUID.randomUUID();
        Category category = new Category("Vợt cầu lông", "vot-cau-long");
        ReflectionTestUtils.setField(category, "id", id);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.update(id, "vot cau long");

        assertThat(response.slug()).isEqualTo("vot-cau-long");
        verify(categoryRepository, never()).findBySlug(any());
        verify(categoryRepository).save(category);
    }

    @Test
    void update_withNameWhoseSlugIsUsedByAnotherCategory_throwsConflictException() {
        UUID id = UUID.randomUUID();
        Category category = new Category("Vợt cầu lông", "vot-cau-long");
        ReflectionTestUtils.setField(category, "id", id);

        UUID otherId = UUID.randomUUID();
        Category other = new Category("Vợt Yonex", "vot-yonex");
        ReflectionTestUtils.setField(other, "id", otherId);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.findBySlug("vot-yonex")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> categoryService.update(id, "Vợt Yonex"))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_withUnknownId_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(id, "Vợt Yonex"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }
}
