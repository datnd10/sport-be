package com.example.sport_be.service;

import com.example.sport_be.dto.cart.MergeCartItemRequest;
import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.Cart;
import com.example.sport_be.entity.CartItem;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.CartItemRepository;
import com.example.sport_be.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages the authenticated Account's Giỏ_Hàng (Cart) (Requirement 3).
 *
 * <p>Only the authenticated, Account-owned Cart flow is handled here; the
 * guest/local-storage cart is frontend-only and is merged into the
 * Account's Cart on login via {@link #merge} (Requirement 3.6).
 *
 * <p>Stock is only ever read here via {@link InventoryService#getVariant}
 * — adding an item to the cart does not change {@code stockQuantity}, so
 * {@code InventoryService} is never asked to mutate stock from this
 * service.
 */
@Service
public class CartService {

    public static final String OUT_OF_STOCK_ERROR_CODE = "VARIANT_OUT_OF_STOCK";
    public static final String INSUFFICIENT_STOCK_ERROR_CODE = "INSUFFICIENT_STOCK";
    private static final String ACCOUNT_NOT_FOUND_ERROR_CODE = "ACCOUNT_NOT_FOUND";
    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "Không tìm thấy tài khoản.";
    private static final String CART_ITEM_NOT_FOUND_ERROR_CODE = "CART_ITEM_NOT_FOUND";
    private static final String CART_ITEM_NOT_FOUND_MESSAGE = "Không tìm thấy mục trong Giỏ_Hàng.";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AccountRepository accountRepository;
    private final InventoryService inventoryService;

    public CartService(CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        AccountRepository accountRepository,
                        InventoryService inventoryService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.accountRepository = accountRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Adds a quantity of a Biến_Thể_Sản_Phẩm to the Account's Giỏ_Hàng,
     * creating the Cart lazily if this is the Account's first addition.
     *
     * <p>Rejects the request entirely if the variant is out of stock
     * (Requirement 3.2) or if the requested quantity (combined with any
     * quantity already in the cart) exceeds the current stock
     * (Requirement 3.1). On success, increments the existing
     * {@link CartItem} for that variant, or creates a new one.
     *
     * @throws ResourceNotFoundException if the account or variant does not exist
     * @throws BusinessRuleException     if the variant is out of stock or the
     *                                    requested quantity exceeds available stock
     */
    @Transactional
    public Cart addItem(UUID accountId, UUID variantId, int quantity) {
        Cart cart = getOrCreateCart(accountId);
        ProductVariant variant = inventoryService.getVariant(variantId);

        if (variant.getStockQuantity() == 0) {
            throw new BusinessRuleException(OUT_OF_STOCK_ERROR_CODE,
                    "Biến_Thể_Sản_Phẩm đã hết hàng.");
        }

        CartItem existingItem = cartItemRepository.findByCart_IdAndVariant_Id(cart.getId(), variantId)
                .orElse(null);
        int currentQuantity = existingItem == null ? 0 : existingItem.getQuantity();
        int requestedTotal = currentQuantity + quantity;

        if (requestedTotal > variant.getStockQuantity()) {
            throw new BusinessRuleException(INSUFFICIENT_STOCK_ERROR_CODE,
                    "Số lượng yêu cầu vượt quá số lượng Tồn_Kho hiện có.");
        }

        if (existingItem == null) {
            cartItemRepository.save(new CartItem(cart, variant, quantity));
        } else {
            existingItem.setQuantity(requestedTotal);
            cartItemRepository.save(existingItem);
        }

        return cart;
    }

    /**
     * Updates the quantity of an existing {@link CartItem} belonging to the
     * Account's Giỏ_Hàng.
     *
     * <p>If {@code requestedQuantity} does not exceed the variant's current
     * stock, the quantity is updated exactly as requested
     * (Requirement 3.3). If {@code requestedQuantity} exceeds the current
     * stock, the quantity is clamped down to the current stock quantity
     * instead of being rejected (Requirement 3.4). The cart's total is
     * always recalculated from the persisted item quantities when the
     * cart is next read (see {@link com.example.sport_be.dto.cart.CartResponse#from}).
     *
     * @param accountId         the owning Account's id, used to make sure the
     *                          item actually belongs to that Account's cart
     * @param itemId            the {@link CartItem} to update
     * @param requestedQuantity the quantity requested by the customer (must be at least 1)
     * @return the updated Cart
     * @throws ResourceNotFoundException if the account or cart item does not exist,
     *                                    or the item does not belong to the account's cart
     */
    @Transactional
    public Cart updateItemQuantity(UUID accountId, UUID itemId, int requestedQuantity) {
        Cart cart = cartRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        CART_ITEM_NOT_FOUND_ERROR_CODE, CART_ITEM_NOT_FOUND_MESSAGE));

        CartItem item = cartItemRepository.findById(itemId)
                .filter(candidate -> candidate.getCart().equals(cart))
                .orElseThrow(() -> new ResourceNotFoundException(
                        CART_ITEM_NOT_FOUND_ERROR_CODE, CART_ITEM_NOT_FOUND_MESSAGE));

        ProductVariant variant = inventoryService.getVariant(item.getVariant().getId());
        int clampedQuantity = Math.min(requestedQuantity, variant.getStockQuantity());

        item.setQuantity(clampedQuantity);
        cartItemRepository.save(item);

        return cart;
    }

    /**
     * Removes an existing {@link CartItem} from the Account's Giỏ_Hàng.
     *
     * <p>The item is deleted outright (Requirement 3.5); the cart's total
     * is always recalculated from the remaining persisted item quantities
     * when the cart is next read (see
     * {@link com.example.sport_be.dto.cart.CartResponse#from}).
     *
     * @param accountId the owning Account's id, used to make sure the
     *                  item actually belongs to that Account's cart
     * @param itemId    the {@link CartItem} to remove
     * @return the updated Cart
     * @throws ResourceNotFoundException if the account or cart item does not exist,
     *                                    or the item does not belong to the account's cart
     */
    @Transactional
    public Cart removeItem(UUID accountId, UUID itemId) {
        Cart cart = cartRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        CART_ITEM_NOT_FOUND_ERROR_CODE, CART_ITEM_NOT_FOUND_MESSAGE));

        CartItem item = cartItemRepository.findById(itemId)
                .filter(candidate -> candidate.getCart().equals(cart))
                .orElseThrow(() -> new ResourceNotFoundException(
                        CART_ITEM_NOT_FOUND_ERROR_CODE, CART_ITEM_NOT_FOUND_MESSAGE));

        cartItemRepository.delete(item);

        return cart;
    }

    /**
     * Merges a Khách_Hàng's temporary (local/guest) Giỏ_Hàng into the
     * Account's server-side Giỏ_Hàng on login (Requirement 3.6), creating
     * the Cart lazily if this is the Account's first Cart activity.
     *
     * <p>For each incoming local item: if a {@link CartItem} for the same
     * {@link ProductVariant} already exists in the Account's Cart, its
     * quantity becomes the sum of both quantities, clamped down to the
     * variant's current stock quantity instead of being rejected,
     * consistent with {@link #updateItemQuantity} (Requirement 3.4). If no
     * existing item for that variant exists yet, a new {@link CartItem} is
     * created with the local quantity, likewise clamped to current stock.
     * Items that exist only in the Account's Cart (not present in the
     * incoming local list) are left unchanged.
     *
     * @param accountId  the owning Account's id
     * @param localItems the Khách_Hàng's temporary/local cart items to merge in
     * @return the updated Cart
     * @throws ResourceNotFoundException if the account or any referenced variant does not exist
     */
    @Transactional
    public Cart merge(UUID accountId, List<MergeCartItemRequest> localItems) {
        Cart cart = getOrCreateCart(accountId);

        for (MergeCartItemRequest localItem : localItems) {
            ProductVariant variant = inventoryService.getVariant(localItem.variantId());

            CartItem existingItem = cartItemRepository
                    .findByCart_IdAndVariant_Id(cart.getId(), variant.getId())
                    .orElse(null);
            int currentQuantity = existingItem == null ? 0 : existingItem.getQuantity();
            int mergedQuantity = currentQuantity + localItem.quantity();
            int clampedQuantity = Math.min(mergedQuantity, variant.getStockQuantity());

            if (existingItem == null) {
                if (clampedQuantity > 0) {
                    cartItemRepository.save(new CartItem(cart, variant, clampedQuantity));
                }
            } else {
                existingItem.setQuantity(clampedQuantity);
                cartItemRepository.save(existingItem);
            }
        }

        return cart;
    }

    /**
     * Retrieves the Account's current Giỏ_Hàng together with its items.
     * If the Account has no Cart yet, returns an empty, unsaved Cart
     * (nothing is persisted until an item is actually added).
     *
     * @throws ResourceNotFoundException if the account does not exist
     */
    @Transactional(readOnly = true)
    public Cart getCart(UUID accountId) {
        Account account = findAccountOrThrow(accountId);
        return cartRepository.findByAccount_Id(accountId)
                .orElseGet(() -> new Cart(account));
    }

    /**
     * Returns the line items belonging to {@code cart}, or an empty list
     * if the cart has not been persisted yet (no id assigned).
     */
    @Transactional(readOnly = true)
    public List<CartItem> getItems(Cart cart) {
        if (cart.getId() == null) {
            return List.of();
        }
        return cartItemRepository.findByCart_Id(cart.getId());
    }

    private Cart getOrCreateCart(UUID accountId) {
        return cartRepository.findByAccount_Id(accountId)
                .orElseGet(() -> {
                    Account account = findAccountOrThrow(accountId);
                    return cartRepository.save(new Cart(account));
                });
    }

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ACCOUNT_NOT_FOUND_ERROR_CODE, ACCOUNT_NOT_FOUND_MESSAGE));
    }
}
