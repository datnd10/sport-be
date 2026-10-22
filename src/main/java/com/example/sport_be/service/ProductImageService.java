package com.example.sport_be.service;

import com.example.sport_be.dto.product.ProductImageResponse;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductImage;
import com.example.sport_be.exception.FieldError;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.ProductImageRepository;
import com.example.sport_be.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Handles storing Hình_Ảnh_Sản_Phẩm (ProductImage) files on local disk
 * under {@code uploads/products/{productId}/}, and removing them again
 * when an Admin deletes an image (Requirement 1.8, 1.9, 1.10).
 *
 * <p>Every uploaded file is validated for format (jpg/jpeg/png/webp) and
 * maximum size before anything is written to disk or persisted, and is
 * stored under a randomly generated file name so stored file names can
 * neither collide nor be guessed from the original upload.
 */
@Service
public class ProductImageService {

    public static final String INVALID_IMAGE_FORMAT_ERROR_CODE = "INVALID_IMAGE_FORMAT";
    public static final String IMAGE_TOO_LARGE_ERROR_CODE = "IMAGE_TOO_LARGE";
    public static final String EMPTY_IMAGE_FILE_ERROR_CODE = "EMPTY_IMAGE_FILE";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Không tìm thấy Sản_Phẩm.";
    private static final String IMAGE_NOT_FOUND_MESSAGE = "Không tìm thấy Hình_Ảnh_Sản_Phẩm.";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final Path uploadRoot;
    private final long maxFileSizeBytes;

    public ProductImageService(ProductRepository productRepository,
                                ProductImageRepository productImageRepository,
                                @Value("${app.upload.products-dir:uploads/products}") String uploadDir,
                                @Value("${app.upload.max-image-size-bytes:5242880}") long maxFileSizeBytes) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.uploadRoot = Path.of(uploadDir);
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * Validates and stores one or more uploaded image files for a
     * Sản_Phẩm, persisting a {@link ProductImage} record for each
     * (Requirement 1.8).
     *
     * <p>Every file is validated (format, size, non-empty) before any
     * file is written to disk, so a single invalid file in the batch
     * rejects the whole request without leaving partial uploads behind
     * (Requirement 1.10).
     *
     * @throws ResourceNotFoundException if the product does not exist
     * @throws ValidationException       if any file has an unsupported format, is empty, or exceeds the max size
     */
    @Transactional
    public List<ProductImageResponse> upload(UUID productId, List<MultipartFile> files) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        if (files == null || files.isEmpty()) {
            throw new ValidationException(EMPTY_IMAGE_FILE_ERROR_CODE,
                    "Phải tải lên ít nhất một tệp hình ảnh.",
                    List.of(new FieldError("files", "Không được để trống")));
        }

        for (MultipartFile file : files) {
            validate(file);
        }

        int nextDisplayOrder = productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(productId).size();

        List<ProductImage> savedImages = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            String relativePath = storeFile(productId, file);
            ProductImage image = new ProductImage(product, relativePath, nextDisplayOrder++);
            savedImages.add(productImageRepository.save(image));
        }

        return savedImages.stream().map(ProductImageResponse::from).toList();
    }

    /**
     * Deletes a Hình_Ảnh_Sản_Phẩm: removes the database record and the
     * corresponding physical file from disk (Requirement 1.9).
     *
     * @throws ResourceNotFoundException if the product or image does not exist, or the image does not belong to the product
     */
    @Transactional
    public void delete(UUID productId, UUID imageId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE);
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException(IMAGE_NOT_FOUND_MESSAGE));

        if (!image.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException(IMAGE_NOT_FOUND_MESSAGE);
        }

        productImageRepository.delete(image);
        deletePhysicalFile(image.getFilePath());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(EMPTY_IMAGE_FILE_ERROR_CODE,
                    "Tệp hình ảnh không được để trống.",
                    List.of(new FieldError("files", "Tệp trống")));
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException(INVALID_IMAGE_FORMAT_ERROR_CODE,
                    "Định dạng hình ảnh không được hỗ trợ. Chỉ chấp nhận: jpg, jpeg, png, webp.",
                    List.of(new FieldError("files", "Định dạng không hợp lệ")));
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new ValidationException(IMAGE_TOO_LARGE_ERROR_CODE,
                    "Kích thước hình ảnh vượt quá giới hạn cho phép.",
                    List.of(new FieldError("files", "Vượt quá kích thước tối đa")));
        }
    }

    private String storeFile(UUID productId, MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String generatedFileName = UUID.randomUUID() + "." + extension;

        Path productDir = uploadRoot.resolve(productId.toString());
        Path targetFile = productDir.resolve(generatedFileName);

        try {
            Files.createDirectories(productDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu tệp hình ảnh vào disk.", e);
        }

        return productDir.resolve(generatedFileName).toString().replace('\\', '/');
    }

    private void deletePhysicalFile(String relativePath) {
        try {
            Path path = Path.of(relativePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Không thể xóa tệp hình ảnh vật lý tại '{}': {}", relativePath, e.getMessage());
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
