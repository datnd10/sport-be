package com.example.sport_be.service;

import com.example.sport_be.dto.review.CreateReviewRequest;
import com.example.sport_be.dto.review.ReviewResponse;
import com.example.sport_be.entity.ProductReviewImage;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.ProductReviewImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Creates the review and all attachments atomically; removes files on rollback. */
@Service
public class ReviewSubmissionService {
    private static final int MAX_IMAGES = 5;
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final ReviewService reviews;
    private final ProductReviewImageRepository images;
    private final Path uploadRoot;

    public ReviewSubmissionService(ReviewService reviews, ProductReviewImageRepository images,
                                   @Value("${app.upload.root-dir:uploads}") String root) {
        this.reviews = reviews;
        this.images = images;
        this.uploadRoot = Path.of(root).toAbsolutePath().normalize();
    }

    @Transactional
    public ReviewResponse submit(UUID accountId, UUID productId, CreateReviewRequest request, List<MultipartFile> files) {
        var attachments = files == null ? List.<MultipartFile>of() : files;
        if (attachments.size() > MAX_IMAGES) throw invalid("Mỗi đánh giá được đính kèm tối đa 5 ảnh.");
        List<byte[]> contents = new ArrayList<>();
        List<String> extensions = new ArrayList<>();
        for (var file : attachments) {
            if (file == null || file.isEmpty()) throw invalid("Ảnh không được để trống.");
            if (file.getSize() > MAX_BYTES) throw invalid("Mỗi ảnh không được vượt quá 5 MB.");
            String name = file.getOriginalFilename();
            String extension = name == null || !name.contains(".") ? "" : name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!Set.of("jpg", "jpeg", "png", "webp").contains(extension)) throw invalid("Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP.");
            try {
                byte[] bytes = file.getBytes();
                if (!validContent(bytes, extension)) throw invalid("Nội dung tệp không phải ảnh hợp lệ hoặc không khớp định dạng.");
                contents.add(bytes);
                extensions.add(extension);
            } catch (IOException ex) {
                throw invalid("Không thể đọc tệp ảnh.");
            }
        }
        // Eligibility and ownership are checked before anything is written to disk.
        var review = reviews.submit(accountId, productId, request.orderItemId(), request.rating(), request.comment());
        List<Path> createdFiles = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    for (Path file : createdFiles) {
                        try { Files.deleteIfExists(file); }
                        catch (IOException ex) { org.slf4j.LoggerFactory.getLogger(ReviewSubmissionService.class).warn("Cannot remove rolled-back review image {}", file, ex); }
                    }
                }
            }
        });
        for (int index = 0; index < contents.size(); index++) {
            String name = UUID.randomUUID() + "." + extensions.get(index);
            Path directory = uploadRoot.resolve("reviews").resolve(review.getId().toString());
            Path target = directory.resolve(name);
            createdFiles.add(target);
            try {
                Files.createDirectories(directory);
                Files.write(target, contents.get(index));
            } catch (IOException ex) {
                throw new IllegalStateException("Không thể lưu ảnh đánh giá. Vui lòng thử lại.", ex);
            }
            var image = images.save(new ProductReviewImage(review, "uploads/reviews/" + review.getId() + "/" + name, index));
            review.getImages().add(image);
        }
        images.flush();
        return ReviewResponse.from(review);
    }

    private boolean validContent(byte[] bytes, String extension) throws IOException {
        if (extension.equals("webp")) {
            if (bytes.length < 20) return false;
            var buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            String riff = new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
            String webp = new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            String chunk = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
            return riff.equals("RIFF") && webp.equals("WEBP") && Integer.toUnsignedLong(buffer.getInt(4)) == bytes.length - 8L
                    && Set.of("VP8 ", "VP8L", "VP8X").contains(chunk);
        }
        boolean signature = extension.equals("png")
                ? bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                : bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff;
        if (!signature) return false;
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return false;
            var reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 40_000_000) return false;
                return reader.read(0) != null;
            } finally { reader.dispose(); }
        }
    }

    private ValidationException invalid(String message) {
        return new ValidationException(message, List.of());
    }
}
