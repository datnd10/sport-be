-- PostgreSQL demo data for the existing product:
-- e4491ea6-8dff-47cc-a913-fddbe0cecc86
-- Run the WHOLE file in a SQL editor connected to your LOCAL/DEMO database.
-- This script is manual; Spring Boot does not run it automatically.
-- It changes this product's description and adds demo variants, disabled
-- customer accounts, delivered orders, order/status/inventory histories and reviews.
-- Existing category, price, variants, images and customer data are preserved.
-- The 3 existing uploaded images are reused without inserting duplicate images.
-- Demo orders contribute to dashboard revenue and order statistics.
-- Stable UUIDs and ON CONFLICT DO NOTHING make reruns non-duplicating:
-- stock, order status and review visibility are NOT reset on reruns.
-- No extensions or real account passwords are required.

BEGIN;

DO $seed$
DECLARE
    target_product constant uuid := 'e4491ea6-8dff-47cc-a913-fddbe0cecc86';
    product_row products%ROWTYPE;
    sample record;
    variant_id uuid;
    account_id uuid;
    order_id uuid;
    item_id uuid;
    review_id uuid;
    variant_price numeric;
    ordered_at timestamptz;
    was_inserted boolean;
BEGIN
    SELECT * INTO product_row FROM products WHERE id = target_product FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product % does not exist. No demo data was inserted.', target_product;
    END IF;
    IF NOT product_row.active THEN
        RAISE EXCEPTION 'Product % is inactive. Activate it before previewing.', target_product;
    END IF;

    UPDATE products SET description = E'Nike Air Force 1 – bộ dữ liệu minh họa trang chi tiết sản phẩm.\n\n'
        || E'Khám phá các lựa chọn kích cỡ và màu sắc bên dưới. Giá bán và số lượng còn lại được hiển thị theo từng biến thể.\n\n'
        || E'Bộ dữ liệu demo có các trạng thái còn hàng, sắp hết hàng và hết hàng để kiểm tra giao diện. '
        || E'Bạn có thể chọn ảnh thu nhỏ, chuyển biến thể và xem nhận xét của khách hàng mẫu.\n\n'
        || E'Lưu ý: mô tả, số lượng và đánh giá trong bộ seed này chỉ phục vụ kiểm thử, không phải thông tin thương mại của sản phẩm.'
    WHERE id = target_product;

    FOR sample IN
        SELECT * FROM (VALUES
            (1, '38', 'Trắng',     12, 1.00, 'Minh Anh (demo)', 5, false, 'Giao diện hiển thị ảnh rõ ràng, chọn size và thêm vào giỏ thuận tiện.'),
            (2, '39', 'Trắng',      8, 1.00, 'Hoàng Nam (demo)', 5, false, 'Mình đã nhận hàng. Thông tin size và màu trên đơn khớp với lựa chọn.'),
            (3, '40', 'Trắng',      3, 1.05, 'Thu Hà (demo)',    4, false, 'Dễ lựa chọn biến thể. Mong shop bổ sung thêm ảnh chi tiết.'),
            (4, '41', 'Đen',        0, 1.05, 'Quốc Bảo (demo)', 4, false, 'Đã mua size 41. Hiện thấy biến thể này hết hàng, chờ shop bổ sung.'),
            (5, '42', 'Trắng/Đen',  6, 1.10, 'Ngọc Linh (demo)',3, false, 'Trải nghiệm mua hàng ổn. Cần mô tả cách chọn kích cỡ cụ thể hơn.'),
            (6, '43', 'Đen',        2, 1.10, 'Ẩn đánh giá (demo)',1,true, 'Đánh giá demo bị ẩn: không được xuất hiện trên trang công khai.')
        ) AS samples(number, shoe_size, color, stock, multiplier, customer_name, rating, hidden, comment)
    LOOP
        variant_id := md5('seed-pdp-e449-variant-' || sample.number)::uuid;
        account_id := md5('seed-pdp-e449-account-' || sample.number)::uuid;
        order_id   := md5('seed-pdp-e449-order-' || sample.number)::uuid;
        item_id    := md5('seed-pdp-e449-item-' || sample.number)::uuid;
        review_id  := md5('seed-pdp-e449-review-' || sample.number)::uuid;
        variant_price := round(product_row.base_price * sample.multiplier, 0);
        ordered_at := now() - make_interval(days => 15 + sample.number);

        INSERT INTO product_variants
            (id, product_id, sku, attributes, price, stock_quantity, low_stock_threshold, active, version)
        VALUES (variant_id, target_product, 'DEMO-E449-SIZE-' || sample.shoe_size,
                'size=' || sample.shoe_size || ';color=' || sample.color,
                variant_price, sample.stock, 5, true, 0)
        ON CONFLICT (id) DO NOTHING;
        was_inserted := FOUND;

        -- Only record stock movements when this script first creates the variant.
        -- Opening stock = current remaining stock + one unit sold in demo order.
        IF was_inserted THEN
            INSERT INTO inventory_histories (id, variant_id, change_amount, reason, changed_at)
            VALUES
                (md5('seed-pdp-e449-stock-open-' || sample.number)::uuid,
                 variant_id, sample.stock + 1, 'MANUAL_ADJUSTMENT', ordered_at - interval '1 day'),
                (md5('seed-pdp-e449-stock-sale-' || sample.number)::uuid,
                 variant_id, -1, 'ORDER_PLACED', ordered_at);
        END IF;

        INSERT INTO accounts (id, email, password_hash, role, full_name, enabled, created_at)
        VALUES (account_id, 'pdp-e449-demo-' || sample.number || '@example.invalid',
                '!disabled-demo-account-no-login', 'CUSTOMER', sample.customer_name, false,
                ordered_at - interval '5 days')
        ON CONFLICT (id) DO NOTHING;

        INSERT INTO orders
            (id, order_code, account_id, status, payment_method, subtotal,
             discount_amount, total_amount, recipient_name, recipient_phone, shipping_address, created_at)
        VALUES (order_id, 'DEMO-PDP-E449-' || lpad(sample.number::text, 2, '0'), account_id,
                'DELIVERED', 'COD', variant_price, 0, variant_price, sample.customer_name,
                '0000000000', 'Địa chỉ dữ liệu demo – không giao hàng', ordered_at)
        ON CONFLICT (id) DO NOTHING;

        INSERT INTO order_items
            (id, order_id, variant_id, product_name_snapshot, unit_price_snapshot, quantity)
        VALUES (item_id, order_id, variant_id, product_row.name, variant_price, 1)
        ON CONFLICT (id) DO NOTHING;

        INSERT INTO order_status_histories (id, order_id, from_status, to_status, changed_at)
        VALUES
            (md5('seed-pdp-e449-status-created-' || sample.number)::uuid, order_id, NULL, 'PENDING', ordered_at),
            (md5('seed-pdp-e449-status-confirmed-' || sample.number)::uuid, order_id, 'PENDING', 'CONFIRMED', ordered_at + interval '1 hour'),
            (md5('seed-pdp-e449-status-shipping-' || sample.number)::uuid, order_id, 'CONFIRMED', 'SHIPPING', ordered_at + interval '1 day'),
            (md5('seed-pdp-e449-status-delivered-' || sample.number)::uuid, order_id, 'SHIPPING', 'DELIVERED', ordered_at + interval '3 days')
        ON CONFLICT (id) DO NOTHING;

        INSERT INTO product_reviews (id, product_id, account_id, order_item_id, rating, comment, hidden, created_at)
        VALUES (review_id, target_product, account_id, item_id, sample.rating, sample.comment,
                sample.hidden, ordered_at + interval '4 days')
        ON CONFLICT (id) DO NOTHING;
    END LOOP;
END
$seed$;

-- First run adds 6 variants, 6 disabled accounts, 6 delivered orders,
-- 6 items, 24 order-status histories, 12 inventory histories and 6 reviews.
-- Public reviews: 5; average: 4.2/5 (unless other reviews already exist).
-- One hidden review is excluded from both the public list and average.
SELECT p.id, p.name,
       (SELECT count(*) FROM product_variants v WHERE v.product_id = p.id AND v.active) AS active_variants,
       (SELECT count(*) FROM product_images i WHERE i.product_id = p.id) AS images,
       (SELECT count(*) FROM product_reviews r WHERE r.product_id = p.id AND NOT r.hidden) AS visible_reviews,
       (SELECT round(avg(r.rating), 2) FROM product_reviews r WHERE r.product_id = p.id AND NOT r.hidden) AS average_rating
FROM products p WHERE p.id = 'e4491ea6-8dff-47cc-a913-fddbe0cecc86';

COMMIT;
