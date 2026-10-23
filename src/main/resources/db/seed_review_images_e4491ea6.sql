-- Demo review images for product e4491ea6-8dff-47cc-a913-fddbe0cecc86.
-- Run AFTER add_product_review_images.sql and seed_product_e4491ea6.sql.
-- Uses the 3 already uploaded product images as DEMO review attachments.
-- Does not download, copy, rename or delete physical files.
-- Seed references these existing files; keep them available for the preview.
-- Stable UUIDs + conflict handling make this script safe to rerun.
BEGIN;

DO $seed$
DECLARE
    target_product constant uuid := 'e4491ea6-8dff-47cc-a913-fddbe0cecc86';
    sample record;
    review_id uuid;
    image_path text;
    image_created_at timestamptz;
BEGIN
    IF to_regclass('product_review_images') IS NULL THEN
        RAISE EXCEPTION 'Run add_product_review_images.sql first.';
    END IF;
    IF (SELECT count(*) FROM product_images WHERE product_id = target_product) < 3 THEN
        RAISE EXCEPTION 'Product % needs at least 3 uploaded images for this demo.', target_product;
    END IF;
    IF (SELECT count(*) FROM product_reviews
        WHERE product_id = target_product AND id IN (
            md5('seed-pdp-e449-review-1')::uuid,
            md5('seed-pdp-e449-review-2')::uuid,
            md5('seed-pdp-e449-review-4')::uuid,
            md5('seed-pdp-e449-review-6')::uuid)) <> 4 THEN
        RAISE EXCEPTION 'Demo reviews are missing. Run seed_product_e4491ea6.sql first.';
    END IF;

    -- Review 1: 2 images; review 2: 1; review 4: 3; hidden review 6: 1.
    -- Reviews 3 and 5 have no images to exercise that UI state as well.
    FOR sample IN SELECT * FROM (VALUES
        (1, 1, 0), (1, 2, 1),
        (2, 3, 0),
        (4, 1, 0), (4, 2, 1), (4, 3, 2),
        (6, 2, 0)
    ) AS samples(review_number, source_image_number, image_order)
    LOOP
        review_id := md5('seed-pdp-e449-review-' || sample.review_number)::uuid;
        SELECT file_path INTO image_path FROM product_images
        WHERE product_id = target_product
        ORDER BY display_order, id
        OFFSET (sample.source_image_number - 1) LIMIT 1;
        SELECT created_at INTO image_created_at FROM product_reviews WHERE id = review_id;

        INSERT INTO product_review_images (id, review_id, file_path, display_order, created_at)
        VALUES (md5('seed-pdp-e449-review-image-' || sample.review_number || '-' || sample.image_order)::uuid,
                review_id, image_path, sample.image_order, image_created_at)
        ON CONFLICT DO NOTHING;
    END LOOP;
END
$seed$;

-- Expected on first run: 7 image rows total; 6 on public reviews,
-- 1 on a hidden review. The hidden review image must NOT be returned
-- by the public API. Existing unrelated attachments are not modified.
SELECT r.id AS review_id, a.full_name AS reviewer_name, r.rating, r.hidden,
       count(i.id) AS image_count
FROM product_reviews r
JOIN accounts a ON a.id = r.account_id
LEFT JOIN product_review_images i ON i.review_id = r.id
WHERE r.product_id = 'e4491ea6-8dff-47cc-a913-fddbe0cecc86'
GROUP BY r.id, a.full_name, r.rating, r.hidden
ORDER BY r.created_at DESC, r.id;

COMMIT;
