-- =====================================================================
-- Sport Shop (lananhsport) - Production schema init script
-- =====================================================================
-- Mục đích: tạo toàn bộ bảng khớp với các @Entity hiện tại trong
-- com.example.sport_be.entity, để chạy TAY trên database production
-- (không dựa vào spring.jpa.hibernate.ddl-auto=update của Hibernate).
--
-- Áp dụng cho PostgreSQL (dialect: PostgreSQLDialect).
--
-- LƯU Ý QUAN TRỌNG TRƯỚC KHI CHẠY:
--   1. Chỉ chạy trên database RỖNG (chưa có các bảng này). Script dùng
--      "CREATE TABLE IF NOT EXISTS" để an toàn hơn, nhưng không ALTER
--      bảng đã tồn tại — nếu bảng đã tồn tại với schema khác, script
--      này sẽ không đồng bộ lại.
--   2. Sau khi chạy script này trên production, hãy đặt
--      JPA_DDL_AUTO=validate (KHÔNG để "update") để Hibernate chỉ xác
--      thực schema khớp, không tự động sửa đổi DDL production.
--   3. Thứ tự tạo bảng tuân theo phụ thuộc khóa ngoại (bảng cha trước,
--      bảng con sau).
--   4. BigDecimal trong entity không khai báo precision/scale cụ thể;
--      script này chọn numeric(12,2) cho các cột tiền tệ (VNĐ) — đổi
--      lại nếu nghiệp vụ cần khác.
--   5. Các cột có default ở tầng Java (vd. enabled=true, active=true,
--      status='PENDING'...) KHÔNG có @PrePersist/@PreUpdate, nên script
--      này thêm DEFAULT tương ứng ở DB để an toàn khi insert ngoài JPA
--      (migration dữ liệu cũ, script thủ công...). Khi insert qua ứng
--      dụng Spring Boot, Hibernate vẫn tự gửi giá trị như thường.
-- =====================================================================

BEGIN;

-- pgcrypto cho gen_random_uuid() (hoặc dùng "uuid-ossp" + uuid_generate_v4()
-- nếu pgcrypto không khả dụng trên instance của bạn).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------
-- accounts
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email         varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,
    role          varchar(20)  NOT NULL CHECK (role IN ('CUSTOMER', 'ADMIN')),
    full_name     varchar(255),
    phone         varchar(255),
    address       varchar(255),
    enabled       boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_accounts_email UNIQUE (email)
);

-- ---------------------------------------------------------------------
-- categories
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(255) NOT NULL,
    slug varchar(255) NOT NULL,
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

-- ---------------------------------------------------------------------
-- products
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES categories (id),
    name        varchar(255) NOT NULL,
    description TEXT,
    base_price  numeric(12, 2) NOT NULL,
    active      boolean NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);

-- ---------------------------------------------------------------------
-- product_variants
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_variants (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          uuid NOT NULL REFERENCES products (id),
    sku                 varchar(255) NOT NULL,
    attributes          TEXT,
    price               numeric(12, 2) NOT NULL,
    stock_quantity      integer NOT NULL CHECK (stock_quantity >= 0),
    low_stock_threshold integer NOT NULL DEFAULT 5,
    active              boolean NOT NULL DEFAULT true,
    version             bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_product_variants_sku UNIQUE (sku)
);

CREATE INDEX IF NOT EXISTS idx_product_variants_product_id ON product_variants (product_id);

-- ---------------------------------------------------------------------
-- product_images
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_images (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id    uuid NOT NULL REFERENCES products (id),
    file_path     varchar(255) NOT NULL,
    display_order integer NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images (product_id);

-- ---------------------------------------------------------------------
-- carts (1-1 với accounts)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carts (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES accounts (id),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_carts_account_id UNIQUE (account_id)
);

-- ---------------------------------------------------------------------
-- cart_items
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id    uuid NOT NULL REFERENCES carts (id),
    variant_id uuid NOT NULL REFERENCES product_variants (id),
    quantity   integer NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_variant_id ON cart_items (variant_id);

-- ---------------------------------------------------------------------
-- discount_codes
-- (orders.discount_code_id tham chiếu tới đây CHỈ bằng UUID, KHÔNG có
--  FK constraint ở tầng entity hiện tại — xem ghi chú ở bảng orders).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discount_codes (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code           varchar(255) NOT NULL,
    discount_type  varchar(20)  NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value numeric(12, 2) NOT NULL,
    valid_from     timestamptz NOT NULL,
    valid_to       timestamptz NOT NULL,
    max_usage      integer NOT NULL,
    usage_count    integer NOT NULL DEFAULT 0,
    active         boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_discount_codes_code UNIQUE (code)
);

-- ---------------------------------------------------------------------
-- orders
-- Lưu ý: discount_code_id KHÔNG có FK constraint trong entity hiện tại
-- (Order.java dùng UUID trần, không @ManyToOne). Giữ đúng hành vi này
-- để không lệch với code; nếu muốn ràng buộc chặt hơn ở DB, có thể bật
-- dòng FK đã comment sẵn dưới đây, nhưng cần đảm bảo logic xóa/deactivate
-- discount code không vi phạm nó.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_code        varchar(255) NOT NULL,
    account_id        uuid NULL REFERENCES accounts (id),
    discount_code_id  uuid NULL, -- REFERENCES discount_codes (id), -- xem ghi chú trên
    status            varchar(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPING', 'DELIVERED', 'CANCELLED')),
    payment_method    varchar(20) NOT NULL CHECK (payment_method IN ('COD', 'BANK_TRANSFER')),
    subtotal          numeric(12, 2) NOT NULL,
    discount_amount   numeric(12, 2) NOT NULL DEFAULT 0,
    total_amount      numeric(12, 2) NOT NULL,
    recipient_name    varchar(255) NOT NULL,
    recipient_phone   varchar(255) NOT NULL,
    shipping_address  varchar(255) NOT NULL,
    created_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_orders_order_code UNIQUE (order_code)
);

CREATE INDEX IF NOT EXISTS idx_orders_account_id ON orders (account_id);
CREATE INDEX IF NOT EXISTS idx_orders_discount_code_id ON orders (discount_code_id);

-- ---------------------------------------------------------------------
-- order_items
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id               uuid NOT NULL REFERENCES orders (id),
    variant_id             uuid NOT NULL REFERENCES product_variants (id),
    product_name_snapshot varchar(255) NOT NULL,
    unit_price_snapshot    numeric(12, 2) NOT NULL,
    quantity               integer NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_variant_id ON order_items (variant_id);

-- ---------------------------------------------------------------------
-- order_status_histories
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_histories (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    uuid NOT NULL REFERENCES orders (id),
    from_status varchar(20) NULL,
    to_status   varchar(20) NOT NULL,
    changed_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_order_status_histories_order_id ON order_status_histories (order_id);

-- ---------------------------------------------------------------------
-- inventory_histories
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_histories (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id    uuid NOT NULL REFERENCES product_variants (id),
    change_amount integer NOT NULL,
    reason        varchar(30) NOT NULL
        CHECK (reason IN ('ORDER_PLACED', 'ORDER_CANCELLED', 'MANUAL_ADJUSTMENT')),
    changed_at    timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_inventory_histories_variant_id ON inventory_histories (variant_id);

-- ---------------------------------------------------------------------
-- product_reviews
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_reviews (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id    uuid NOT NULL REFERENCES products (id),
    account_id    uuid NOT NULL REFERENCES accounts (id),
    order_item_id uuid NOT NULL REFERENCES order_items (id),
    rating        integer NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment       TEXT,
    hidden        boolean NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_reviews_order_item_id UNIQUE (order_item_id)
);

CREATE INDEX IF NOT EXISTS idx_product_reviews_product_id ON product_reviews (product_id);
CREATE INDEX IF NOT EXISTS idx_product_reviews_account_id ON product_reviews (account_id);

COMMIT;
