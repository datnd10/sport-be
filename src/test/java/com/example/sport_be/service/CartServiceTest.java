package com.example.sport_be.service;

import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.Cart;
import com.example.sport_be.entity.CartItem;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.Role;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.CartItemRepository;
import com.example.sport_be.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CartService} (Requirement 3.1, 3.2, 3.3, 3.4).
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private InventoryService inventoryService;

    private CartService cartService;

    private Account account;
    private Cart cart;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, accountRepository, inventoryService);

        account = new Account("user@example.com", "hashed-password", Role.CUSTOMER);
        cart = new Cart(account);

        Category category = new Category("Vợt cầu lông", "vot-cau-long");
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        variant = new ProductVariant(product, "SKU-1", "size:4U", BigDecimal.valueOf(100), 5);
    }

    @Test
    void updateItemQuantity_withQuantityWithinStock_updatesQuantityExactlyAsRequested() {
        UUID accountId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        CartItem item = new CartItem(cart, variant, 1);

        when(cartRepository.findByAccount_Id(accountId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.updateItemQuantity(accountId, itemId, 3);

        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void updateItemQuantity_withQuantityExceedingStock_clampsToCurrentStockInsteadOfRejecting() {
        UUID accountId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        CartItem item = new CartItem(cart, variant, 1);

        when(cartRepository.findByAccount_Id(accountId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // variant stock is 5, requesting 999 should clamp down to 5 rather than throw
        cartService.updateItemQuantity(accountId, itemId, 999);

        assertThat(item.getQuantity()).isEqualTo(variant.getStockQuantity());
    }

    @Test
    void updateItemQuantity_withUnknownCart_throwsResourceNotFoundException() {
        UUID accountId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(cartRepository.findByAccount_Id(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(accountId, itemId, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemQuantity_withItemNotBelongingToAccountsCart_throwsResourceNotFoundException() {
        UUID accountId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Cart otherCart = new Cart(new Account("other@example.com", "hashed-password", Role.CUSTOMER));
        CartItem itemInOtherCart = new CartItem(otherCart, variant, 1);

        when(cartRepository.findByAccount_Id(accountId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(itemInOtherCart));

        assertThatThrownBy(() -> cartService.updateItemQuantity(accountId, itemId, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemQuantity_withUnknownItemId_throwsResourceNotFoundException() {
        UUID accountId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(cartRepository.findByAccount_Id(accountId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(accountId, itemId, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
