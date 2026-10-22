package com.example.sport_be.constants;

/**
 * Centralized declaration of all REST API endpoint paths used by
 * {@code @RequestMapping}/{@code @GetMapping}/... annotations across
 * controllers. Keeping every path here avoids duplicated literal
 * strings scattered across controllers, so a path only needs to
 * change in one place.
 *
 * <p>Organized as nested classes, one per module, mirroring the
 * controller/service module split (Account, Auth, Category, Product,
 * Inventory, ...).
 */
public final class ApiPaths {

    private ApiPaths() {
    }

    /**
     * Public health-check endpoint, used by uptime monitors (e.g.
     * UptimeRobot) to keep a free-tier Render deployment from spinning
     * down due to inactivity, and for basic liveness checks.
     */
    public static final class Health {

        private Health() {
        }

        public static final String CHECK = "/api/health";
    }

    /** Endpoints for the currently authenticated account's own profile. */
    public static final class Account {

        private Account() {
        }

        public static final String BASE = "/api/account";
        public static final String ME = "/me";
    }

    /** Public endpoints for registration and authentication. */
    public static final class Auth {

        private Auth() {
        }

        public static final String BASE = "/api/auth";
        public static final String REGISTER = "/register";
        public static final String LOGIN = "/login";
    }

    /** Endpoints for Danh_Mục (Category) records. */
    public static final class Category {

        private Category() {
        }

        public static final String PUBLIC_LIST = "/api/categories";
        public static final String ADMIN_CREATE = "/api/admin/categories";
        public static final String ADMIN_LIST = "/api/admin/categories";
        public static final String ADMIN_BY_ID = "/api/admin/categories/{id}";
    }

    /** Endpoints for Sản_Phẩm (Product) records and their Biến_Thể/Hình_Ảnh. */
    public static final class Product {

        private Product() {
        }

        public static final String ADMIN_LIST = "/api/admin/products";
        public static final String ADMIN_CREATE = "/api/admin/products";
        public static final String ADMIN_BY_ID = "/api/admin/products/{id}";
        public static final String ADMIN_VARIANTS = "/api/admin/products/{id}/variants";
        public static final String ADMIN_VARIANT_BY_ID = "/api/admin/products/{id}/variants/{variantId}";
        public static final String ADMIN_IMAGES = "/api/admin/products/{id}/images";
        public static final String ADMIN_IMAGE_BY_ID = "/api/admin/products/{id}/images/{imageId}";
        public static final String PUBLIC_LIST = "/api/products";
        public static final String PUBLIC_BY_ID = "/api/products/{id}";
    }

    /** Admin-only endpoints for viewing and adjusting Tồn_Kho. */
    public static final class Inventory {

        private Inventory() {
        }

        public static final String BASE = "/api/admin/inventory";
        public static final String BY_VARIANT_ID = "/{variantId}";
        public static final String HISTORY_BY_VARIANT_ID = "/{variantId}/history";
    }

    /** Endpoints for the authenticated account's Giỏ_Hàng (Cart). */
    public static final class Cart {

        private Cart() {
        }

        public static final String BASE = "/api/cart";
        public static final String ITEMS = "/api/cart/items";
        public static final String ITEM_BY_ID = "/api/cart/items/{itemId}";
        public static final String MERGE = "/api/cart/merge";
    }

    /** Endpoints for Mã_Giảm_Giá (Discount_Code) records. */
    public static final class Discount {

        private Discount() {
        }

        public static final String ADMIN_LIST = "/api/admin/discount-codes";
        public static final String ADMIN_CREATE = "/api/admin/discount-codes";
        public static final String ADMIN_DEACTIVATE = "/api/admin/discount-codes/{id}/deactivate";
        public static final String PUBLIC_VALIDATE = "/api/discount-codes/validate";
    }

    /** Endpoints for Đơn_Hàng (Order) records. */
    public static final class Order {

        private Order() {
        }

        public static final String BASE = "/api/orders";
        public static final String BY_ID = "/api/orders/{id}";
        public static final String ADMIN_LIST = "/api/admin/orders";
        public static final String ADMIN_STATUS_BY_ID = "/api/admin/orders/{id}/status";
    }

    /** Endpoints for Đánh_Giá_Sản_Phẩm (Product_Review) records. */
    public static final class Review {

        private Review() {
        }

        public static final String BY_PRODUCT_ID = "/api/products/{id}/reviews";
        public static final String ADMIN_LIST = "/api/admin/reviews";
        public static final String ADMIN_BY_ID = "/api/admin/reviews/{id}";
    }

    /** Admin-only endpoints for the Dashboard overview (Requirement 10). */
    public static final class Dashboard {

        private Dashboard() {
        }

        public static final String SUMMARY = "/api/admin/dashboard/summary";
        public static final String TOP_PRODUCTS = "/api/admin/dashboard/top-products";
        public static final String REVENUE_BY_DAY = "/api/admin/dashboard/revenue-by-day";
        public static final String REVENUE_BY_CATEGORY = "/api/admin/dashboard/revenue-by-category";
    }

    /**
     * Admin-only endpoints for the Tài_Khoản (Account) management page:
     * viewing/searching every account and khóa/mở khóa (locking/
     * unlocking) one. Kept separate from {@link Account}, which only
     * covers the caller's own self-profile endpoints.
     */
    public static final class AdminAccount {

        private AdminAccount() {
        }

        public static final String ADMIN_LIST = "/api/admin/accounts";
        public static final String ADMIN_SET_ENABLED = "/api/admin/accounts/{id}/enabled";
    }
}