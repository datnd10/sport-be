package com.example.sport_be.dto.order;

/**
 * Shop bank account/QR info returned alongside an {@link OrderResponse}
 * when the chosen Phương_Thức_Thanh_Toán is Chuyển_Khoản_Ngân_Hàng
 * (bank transfer), so the customer can complete the transfer
 * (Requirement 4.4).
 *
 * @param bankName      the receiving bank's name
 * @param accountNumber the shop's bank account number
 * @param accountHolder the shop's bank account holder name
 * @param qrImageUrl    a URL/path to a QR code image encoding the transfer details
 */
public record BankTransferInfoResponse(
        String bankName,
        String accountNumber,
        String accountHolder,
        String qrImageUrl
) {
}
