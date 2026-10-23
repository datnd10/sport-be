package com.example.sport_be.service;

import com.example.sport_be.dto.order.CreateOrderRequest;
import com.example.sport_be.dto.order.OrderItemRequest;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.DiscountType;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.PaymentMethod;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.CartItemRepository;
import com.example.sport_be.repository.DiscountCodeRepository;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderService#createOrder} (Requirement 4.1, 4.2,
 * 4.3, 4.6, 4.7).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private DiscountCodeRepository discountCodeRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private DiscountService discountService;

    @Mock
    private CartItemRepository cartItemRepository;

    private OrderService orderService;

    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, accountRepository,
                discountCodeRepository, inventoryService, discountService, cartItemRepository,
                "Vietcombank", "0123456789", "CONG TY LANANHSPORT", "/static/bank-qr.png");

        Category category = new Category("Vợt cầu lông", "vot-cau-long");
        Product product = new Product(category, "Vợt Yonex", BigDecimal.TEN);
        variant = new ProductVariant(product, "SKU-1", "size:4U", BigDecimal.valueOf(100), 10);
    }

    @Test
    void createOrder_withValidRequest_createsOrderWithPendingStatus() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = validRequest(2, null);

        OrderService.OrderCreationResult result = orderService.createOrder(null, request);

        assertThat(result.order().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_withValidRequest_computesSubtotalAndDecreasesStockForEachItem() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = validRequest(3, null);

        OrderService.OrderCreationResult result = orderService.createOrder(null, request);

        assertThat(result.order().getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(result.order().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
        verify(inventoryService).decreaseForOrder(variant.getId(), 3);
    }

    @Test
    void createOrder_withValidRequest_writesProductNameAndPriceSnapshotIntoOrderItem() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = validRequest(1, null);

        OrderService.OrderCreationResult result = orderService.createOrder(null, request);

        OrderItem item = result.items().get(0);
        assertThat(item.getProductNameSnapshot()).isEqualTo("Vợt Yonex");
        assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(item.getQuantity()).isEqualTo(1);
    }

    @Test
    void createOrder_withMissingRecipientName_throwsValidationException() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(variant.getId(), 1)),
                "  ",
                "0900000000",
                "123 Đường ABC",
                PaymentMethod.COD,
                null
        );

        assertThatThrownBy(() -> orderService.createOrder(null, request))
                .isInstanceOf(ValidationException.class);

        verify(inventoryService, never()).decreaseForOrder(any(), any(Integer.class));
    }

    @Test
    void createOrder_withMissingShippingFields_throwsValidationExceptionBeforeTouchingStock() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(variant.getId(), 1)),
                "",
                "",
                "",
                PaymentMethod.COD,
                null
        );

        assertThatThrownBy(() -> orderService.createOrder(null, request))
                .isInstanceOf(ValidationException.class);

        verify(inventoryService, never()).getVariant(any());
    }

    @Test
    void createOrder_withValidDiscountCode_reducesTotalByDiscountAmount() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiscountCode discountCode = new DiscountCode("SALE10", DiscountType.PERCENTAGE, BigDecimal.TEN,
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS), 100);
        when(discountService.validate("SALE10")).thenReturn(discountCode);
        when(discountCodeRepository.save(any(DiscountCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = validRequest(2, "SALE10");

        OrderService.OrderCreationResult result = orderService.createOrder(null, request);

        // subtotal = 200, 10% discount = 20
        assertThat(result.order().getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(result.order().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(180));
        assertThat(discountCode.getUsageCount()).isEqualTo(1);
    }

    @Test
    void createOrder_withInvalidDiscountCode_rejectsAndDoesNotCreateOrder() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(discountService.validate("EXPIRED"))
                .thenThrow(new BusinessRuleException("DISCOUNT_CODE_EXPIRED", "Mã_Giảm_Giá đã hết hạn sử dụng."));

        CreateOrderRequest request = validRequest(1, "EXPIRED");

        assertThatThrownBy(() -> orderService.createOrder(null, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(orderRepository, never()).save(any());
        verify(inventoryService, never()).decreaseForOrder(any(), any(Integer.class));
    }

    @Test
    void createOrder_withInsufficientStock_propagatesBusinessRuleExceptionFromInventoryService() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.decreaseForOrder(any(), any(Integer.class)))
                .thenThrow(new BusinessRuleException("INSUFFICIENT_STOCK", "Số lượng Tồn_Kho không đủ để đặt hàng."));

        CreateOrderRequest request = validRequest(999, null);

        assertThatThrownBy(() -> orderService.createOrder(null, request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createOrder_withBankTransferPaymentMethod_returnsBankTransferInfo() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(variant.getId(), 1)),
                "Nguyễn Văn A",
                "0900000000",
                "123 Đường ABC, Quận 1",
                PaymentMethod.BANK_TRANSFER,
                null
        );

        OrderService.OrderCreationResult result = orderService.createOrder(null, request);

        assertThat(result.bankTransferInfo()).isNotNull();
        assertThat(result.bankTransferInfo().bankName()).isEqualTo("Vietcombank");
        assertThat(result.bankTransferInfo().accountNumber()).isEqualTo("0123456789");
    }

    @Test
    void createOrder_withCodPaymentMethod_returnsNoBankTransferInfoAndOrderIsRecordedImmediately() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = validRequest(1, null);

        OrderService.OrderCreationResult result = orderService.createOrder(null, request);

        assertThat(result.bankTransferInfo()).isNull();
        assertThat(result.order().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_assignsHumanReadableOrderCode() {
        when(inventoryService.getVariant(any())).thenReturn(variant);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = validRequest(1, null);

        OrderService.OrderCreationResult result = orderService.createOrder(null, request);

        assertThat(result.order().getOrderCode()).matches("^ORD-\\d{8}-[A-Z0-9]{6}$");
    }

    @Test
    void createOrder_withUnknownAccountId_throwsResourceNotFoundException() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        CreateOrderRequest request = validRequest(1, null);

        assertThatThrownBy(() -> orderService.createOrder(accountId, request))
                .isInstanceOf(com.example.sport_be.exception.ResourceNotFoundException.class);
    }

    private CreateOrderRequest validRequest(int quantity, String discountCode) {
        return new CreateOrderRequest(
                List.of(new OrderItemRequest(variant.getId(), quantity)),
                "Nguyễn Văn A",
                "0900000000",
                "123 Đường ABC, Quận 1",
                PaymentMethod.COD,
                discountCode
        );
    }
}
