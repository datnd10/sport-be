package com.example.sport_be.dto.order;

import com.example.sport_be.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for {@code POST /api/orders}: places a new Đơn_Hàng
 * (Order) built directly from the submitted line items (Requirement 4.1).
 *
 * <p>Items are submitted explicitly rather than read from a server-side
 * Giỏ_Hàng because this endpoint is also used by Khách_Hàng vãng lai
 * (guests), who have no server-side Cart — their cart lives in the
 * frontend's {@code localStorage} (see design.md, Module Cart).
 *
 * <p>{@code recipientName}, {@code recipientPhone}, and
 * {@code shippingAddress} are validated as required here for the common
 * "blank" case; {@code OrderService} performs the authoritative check
 * (Requirement 4.3) so the same field-level error shape is produced
 * regardless of how the request arrives.
 *
 * @param items            the line items to purchase; must contain at least one item
 * @param recipientName    the shipping recipient's name; required
 * @param recipientPhone   the shipping recipient's phone number; required
 * @param shippingAddress  the shipping destination address; required
 * @param paymentMethod    the chosen Phương_Thức_Thanh_Toán; required
 * @param discountCode     an optional Mã_Giảm_Giá to apply to the order
 */
public record CreateOrderRequest(
        @NotEmpty(message = "Đơn_Hàng phải có ít nhất một mục")
        @Valid
        List<OrderItemRequest> items,

        @NotBlank(message = "Tên người nhận không được để trống")
        String recipientName,

        @NotBlank(message = "Số điện thoại người nhận không được để trống")
        String recipientPhone,

        @NotBlank(message = "Địa chỉ giao hàng không được để trống")
        String shippingAddress,

        @NotNull(message = "Phương_Thức_Thanh_Toán không được để trống")
        PaymentMethod paymentMethod,

        String discountCode
) {
}
