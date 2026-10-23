# Ảnh đánh giá – script PostgreSQL

Chạy toàn bộ từng file bằng DBeaver/pgAdmin trên database local/demo theo thứ tự:

1. `add_product_review_images.sql`: tạo bảng ảnh đánh giá.
2. `seed_product_e4491ea6.sql`: nếu chưa seed đánh giá của sản phẩm này.
3. `seed_review_images_e4491ea6.sql`: thêm ảnh cho các đánh giá mẫu.

## Bảng `product_review_images`

| Cột | Ý nghĩa |
| --- | --- |
| `id` | UUID của ảnh |
| `review_id` | Khóa ngoại đến `product_reviews.id` |
| `file_path` | Đường dẫn tương đối đến file ảnh, giống cách lưu `product_images` |
| `display_order` | Thứ tự hiển thị, bắt đầu từ 0 |
| `created_at` | Thời điểm thêm ảnh |

Một review có nhiều ảnh; không lưu ảnh dạng binary trong DB. Có index cho truy vấn ảnh theo review/thứ tự và ràng buộc không trùng đường dẫn trong cùng review. Xóa review xóa các dòng ảnh liên quan; backend vẫn phải xử lý xóa file. Ẩn review giữ nguyên ảnh trong DB.

## Dữ liệu demo

Dùng sản phẩm `e4491ea6-8dff-47cc-a913-fddbe0cecc86` và 3 đường dẫn ảnh đã upload của sản phẩm này.

| Đánh giá mẫu | Ảnh |
| --- | ---: |
| Minh Anh | 2 |
| Hoàng Nam | 1 |
| Thu Hà | 0 |
| Quốc Bảo | 3 |
| Ngọc Linh | 0 |
| Đánh giá bị ẩn | 1 |

Tổng cộng 7 ảnh: 6 ảnh gắn với review công khai và 1 ảnh gắn với review bị ẩn. Chạy lại không tạo trùng. Ảnh demo dùng chung file với sản phẩm, không tạo file mới; không xóa các file nguồn khi đang xem thử.

## Tính năng đã tích hợp

Backend đã có entity ảnh, trả mảng `images` trong DTO review và nhận multipart tại `POST /api/products/{id}/reviews`: phần `review` là JSON, phần `images` là danh sách file. JSON review cũ vẫn được hỗ trợ. Endpoint đăng nhập `GET /api/account/review-eligibility?productId=...` trả các lần mua đã giao chưa được đánh giá.

UI cho khách hàng đủ điều kiện gửi đánh giá, chọn/xóa ảnh trước khi gửi; ảnh được hiển thị ở trang sản phẩm và quản trị. Bấm ảnh để xem lớn, chuyển ảnh bằng nút hoặc phím trái/phải, Escape để đóng.

Tối đa 5 ảnh/review, 5 MB/ảnh, JPG/PNG/WebP. Backend kiểm tra nội dung file, quyền mua hàng, trùng đánh giá; lưu ảnh mới tại `uploads/reviews/{reviewId}/` và dọn file khi transaction rollback. Ẩn review không trả review/ảnh trong API công khai.

Với DB đang tồn tại, chạy migration rồi restart backend. Script seed là tùy chọn và cần chạy thủ công; ứng dụng không tự seed ảnh. Các tài khoản demo bị khóa đăng nhập nên muốn thử gửi đánh giá cần tài khoản khách thật có đơn đã giao chưa đánh giá.
