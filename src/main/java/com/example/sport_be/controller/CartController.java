package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.cart.AddCartItemRequest;
import com.example.sport_be.dto.cart.CartResponse;
import com.example.sport_be.dto.cart.MergeCartItemRequest;
import com.example.sport_be.dto.cart.UpdateCartItemRequest;
import com.example.sport_be.security.JwtPrincipal;
import com.example.sport_be.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for the authenticated Customer's Giỏ_Hàng (Cart)
 * (Requirement 3). Routes require authentication, per {@code
 * SecurityConfig}'s default {@code anyRequest().authenticated()} rule.
 */
@RestController
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Adds a Biến_Thể_Sản_Phẩm to the current Account's Giỏ_Hàng,
     * rejecting the request if the variant is out of stock or the
     * requested quantity exceeds available stock (Requirement 3.1, 3.2).
     */
    @PostMapping(ApiPaths.Cart.ITEMS)
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        UUID accountId = currentAccountId(authentication);
        return ResponseEntity.ok(cartService.addItemResponse(accountId, request.variantId(), request.quantity()));
    }

    /**
     * Updates the quantity of an existing item in the current Account's
     * Giỏ_Hàng. If the requested quantity exceeds the variant's current
     * stock, it is clamped down to the current stock quantity instead of
     * being rejected (Requirement 3.3, 3.4).
     */
    @PutMapping(ApiPaths.Cart.ITEM_BY_ID)
    public ResponseEntity<CartResponse> updateItemQuantity(
            Authentication authentication,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        UUID accountId = currentAccountId(authentication);
        return ResponseEntity.ok(cartService.updateItemResponse(accountId, itemId, request.quantity()));
    }

    /**
     * Removes an item from the current Account's Giỏ_Hàng, recalculating
     * the cart's total from the remaining items (Requirement 3.5).
     */
    @DeleteMapping(ApiPaths.Cart.ITEM_BY_ID)
    public ResponseEntity<CartResponse> removeItem(
            Authentication authentication,
            @PathVariable UUID itemId
    ) {
        UUID accountId = currentAccountId(authentication);
        return ResponseEntity.ok(cartService.removeItemResponse(accountId, itemId));
    }

    /**
     * Merges the Khách_Hàng's temporary (local/guest) Giỏ_Hàng — sent from
     * the frontend's localStorage on successful login — into the current
     * Account's server-side Giỏ_Hàng. Quantities for variants present on
     * both sides are summed and clamped to current stock; quantities for
     * variants only present in the local cart are added as new items,
     * likewise clamped to current stock (Requirement 3.6).
     */
    @PostMapping(ApiPaths.Cart.MERGE)
    public ResponseEntity<CartResponse> merge(
            Authentication authentication,
            @Valid @RequestBody List<MergeCartItemRequest> localItems
    ) {
        UUID accountId = currentAccountId(authentication);
        return ResponseEntity.ok(cartService.mergeResponse(accountId, localItems));
    }

    /**
     * Returns the current Account's Giỏ_Hàng together with its items.
     */
    @GetMapping(ApiPaths.Cart.BASE)
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        UUID accountId = currentAccountId(authentication);
        return ResponseEntity.ok(cartService.getCartResponse(accountId));
    }

    private UUID currentAccountId(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return principal.accountId();
    }
}
