-- PostgreSQL migration: one review can have multiple images.
-- Run on an existing Sport Shop database. No product/customer data is changed.
-- Re-running this file is supported; CREATE IF NOT EXISTS does not repair
-- an independently created table with a different structure.
BEGIN;

CREATE TABLE IF NOT EXISTS product_review_images (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id     uuid NOT NULL,
    file_path     varchar(255) NOT NULL,
    display_order integer NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_product_review_images_review
        FOREIGN KEY (review_id) REFERENCES product_reviews(id) ON DELETE CASCADE,
    CONSTRAINT ck_product_review_images_path
        CHECK (length(btrim(file_path)) > 0),
    CONSTRAINT ck_product_review_images_display_order
        CHECK (display_order >= 0),
    CONSTRAINT uq_product_review_images_review_path
        UNIQUE (review_id, file_path)
);

CREATE INDEX IF NOT EXISTS idx_product_review_images_review_order
    ON product_review_images (review_id, display_order, id);

-- CASCADE removes database rows only, not the physical image files.
-- Hiding a review keeps its image rows; public API must filter by
-- product_reviews.hidden = false before returning these images.
COMMIT;
