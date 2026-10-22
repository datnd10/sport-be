package com.example.sport_be.service;

import com.example.sport_be.dto.order.BankTransferInfoResponse;
import com.example.sport_be.dto.order.CreateOrderRequest;
import com.example.sport_be.dto.order.OrderItemRequest;
import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.DiscountType;
import com.example.sport_be.enums.PaymentMethod;
import com.example.sport_be.exception.FieldError;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.DiscountCodeRepository;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles placing new Đơn_Hàng (Order) records from a submitted set of
 * line items (Requirement 4).
 *
 * <p>{@link #createOrder} runs as a single transaction that: validates
 * shipping info (4.3), applies a Mã_Giảm_Giá if one was submitted via
 * {@link DiscountService#validate} (4.6, 4.7), computes the order total,
 * creates the {@link Order} with status {@code PENDING}
 * (Chờ_Xác_Nhận, 4.1), decreases stock for every line item exclusively
 * through {@link InventoryService#decreaseForOrder} (4.2, 7.5), and
 * writes the product name/price snapshot into each {@link OrderItem}
 * (Requirement 1.5 lineage).
 *
 * <p>Available to both authenticated Customers and guests: {@code
 * accountId} may be {@code null} for a guest checkout, in which case the
 * created {@link Order} has no associated {@link Account}.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AccountRepository accountRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final InventoryService inventoryService;
    private final DiscountService discountService;
    private final String shopBankName;
    private final String shopBankAccountNumber;
    private final String shopBankAccountHolder;
    private final String shopBankQrImageUrl;

    public OrderService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         AccountRepository accountRepository,
                         DiscountCodeRepository discountCodeRepository,
                         InventoryService inventoryService,
                         DiscountService discountService,
                         @Value("${shop.bank.bank-name}") String shopBankName,
                         @Value("${shop.bank.account-number}") String shopBankAccountNumber,
                         @Value("${shop.bank.account-holder}") String shopBankAccountHolder,
                         @Value("${shop.bank.qr-image-url}") String shopBankQrImageUrl) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.accountRepository = accountRepository;
        this.discountCodeRepository = discountCodeRepository;
        this.inventoryService = inventoryService;
        this.discountService = discountService;
        this.shopBankName = shopBankName;
        this.shopBankAccountNumber = shopBankAccountNumber;
        this.shopBankAccountHolder = shopBankAccountHolder;
        this.shopBankQrImageUrl = shopBankQrImageUrl;
    }

    /**
     * Places a new Đơn_Hàng from the submitted line items and shipping
     * info, within a single transaction.
     *
     * @param accountId the placing Account's id, or {@code null} for a guest checkout
     * @param request   the submitted items, shipping info, payment method, and optional discount code
     * @return the newly created Order, together with its persisted line items
     * @throws ValidationException       if shipping info is missing required fields (Requirement 4.3)
     * @throws ResourceNotFoundException if the account, a referenced variant, or the discount code does not exist
     * @throws com.example.sport_be.exception.BusinessRuleException
     *         if the discount code is invalid/expired/exhausted (Requirement 4.7), or if
     *         any item's requested quantity exceeds available stock
     */
    @Transactional
    public OrderCreationResult createOrder(UUID accountId, CreateOrderRequest request) {
        validateShippingInfo(request);

        Account account = accountId == null ? null : findAccountOrThrow(accountId);

        List<ProductVariant> variants = new ArrayList<>(request.items().size());
        for (OrderItemRequest itemRequest : request.items()) {
            variants.add(inventoryService.getVariant(itemRequest.variantId()));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < request.items().size(); i++) {
            BigDecimal price = variants.get(i).getPrice();
            int quantity = request.items().get(i).quantity();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(quantity)));
        }

        DiscountCode discountCode = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (StringUtils.hasText(request.discountCode())) {
            discountCode = discountService.validate(request.discountCode());
            discountAmount = computeDiscountAmount(discountCode, subtotal);
        }

        BigDecimal totalAmount = subtotal.subtract(discountAmount);

        Order order = new Order(account, request.paymentMethod(), subtotal, totalAmount,
                request.recipientName(), request.recipientPhone(), request.shippingAddress());
        order.setDiscountAmount(discountAmount);
        if (discountCode != null) {
            order.setDiscountCodeId(discountCode.getId());
        }
        order = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>(request.items().size());
        for (int i = 0; i < request.items().size(); i++) {
            ProductVariant variant = variants.get(i);
            int quantity = request.items().get(i).quantity();

            inventoryService.decreaseForOrder(variant.getId(), quantity);

            OrderItem orderItem = new OrderItem(order, variant, variant.getProduct().getName(),
                    variant.getPrice(), quantity);
            orderItems.add(orderItemRepository.save(orderItem));
        }

        if (discountCode != null) {
            discountCode.setUsageCount(discountCode.getUsageCount() + 1);
            discountCodeRepository.save(discountCode);
        }

        BankTransferInfoResponse bankTransferInfo = order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                ? buildBankTransferInfo()
                : null;

        return new OrderCreationResult(order, orderItems, bankTransferInfo);
    }

    /**
     * Builds the shop's bank account/QR info to return alongside the
     * order confirmation when Phương_Thức_Thanh_Toán is
     * Chuyển_Khoản_Ngân_Hàng, so the customer can complete the transfer
     * (Requirement 4.4). For Thanh_Toán_Khi_Nhận_Hàng (COD), the order
     * created above is already recorded with no further payment
     * confirmation step required (Requirement 4.5).
     */
    private BankTransferInfoResponse buildBankTransferInfo() {
        return new BankTransferInfoResponse(shopBankName, shopBankAccountNumber, shopBankAccountHolder,
                shopBankQrImageUrl);
    }

    private void validateShippingInfo(CreateOrderRequest request) {
        List<FieldError> fieldErrors = new ArrayList<>();
        if (!StringUtils.hasText(request.recipientName())) {
            fieldErrors.add(new FieldError("recipientName", "Tên người nhận không được để trống"));
        }
        if (!StringUtils.hasText(request.recipientPhone())) {
            fieldErrors.add(new FieldError("recipientPhone", "Số điện thoại người nhận không được để trống"));
        }
        if (!StringUtils.hasText(request.shippingAddress())) {
            fieldErrors.add(new FieldError("shippingAddress", "Địa chỉ giao hàng không được để trống"));
        }

        if (!fieldErrors.isEmpty()) {
            throw new ValidationException("Thông tin giao hàng còn thiếu", fieldErrors);
        }
    }

    private BigDecimal computeDiscountAmount(DiscountCode discountCode, BigDecimal subtotal) {
        BigDecimal rawAmount = discountCode.getDiscountType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(discountCode.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : discountCode.getDiscountValue();

        return rawAmount.min(subtotal);
    }

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }

    /**
     * Pairs a newly created {@link Order} with its persisted
     * {@link OrderItem} list and, when Phương_Thức_Thanh_Toán is
     * Chuyển_Khoản_Ngân_Hàng, the shop's bank account/QR info
     * (Requirement 4.4), since the controller needs all three to build
     * the response.
     *
     * @param bankTransferInfo the shop's bank transfer info, or {@code null} when the
     *                         order's payment method is not Chuyển_Khoản_Ngân_Hàng
     */
    public record OrderCreationResult(Order order, List<OrderItem> items, BankTransferInfoResponse bankTransferInfo) {
    }
}
