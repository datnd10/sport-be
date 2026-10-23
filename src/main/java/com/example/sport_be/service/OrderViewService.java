package com.example.sport_be.service;

import com.example.sport_be.dto.order.*;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.enums.PaymentMethod;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.ProductImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/** Builds complete order views inside the transaction, including batched product thumbnails. */
@Service
@Transactional(readOnly = true)
public class OrderViewService {
    private final OrderQueryService queries;
    private final OrderItemRepository items;
    private final ProductImageRepository images;
    private final BankTransferInfoResponse bank;

    public OrderViewService(OrderQueryService queries, OrderItemRepository items, ProductImageRepository images,
            @Value("${shop.bank.bank-name}") String name,
            @Value("${shop.bank.account-number}") String number,
            @Value("${shop.bank.account-holder}") String holder,
            @Value("${shop.bank.qr-image-url}") String qr) {
        this.queries = queries;
        this.items = items;
        this.images = images;
        this.bank = new BankTransferInfoResponse(name, number, holder, qr);
    }

    public List<OrderSummaryResponse> history(UUID accountId) {
        var orders = queries.getOrdersForAccount(accountId);
        if (orders.isEmpty()) return List.of();
        var allItems = items.findByOrder_IdIn(orders.stream().map(o -> o.getId()).toList());
        var thumbnails = thumbnails(allItems);
        var grouped = allItems.stream().collect(Collectors.groupingBy(i -> i.getOrder().getId()));
        return orders.stream().map(o -> OrderSummaryResponse.from(o).withItems(
                mapItems(grouped.getOrDefault(o.getId(), List.of()), thumbnails))).toList();
    }

    public OrderResponse detail(UUID id, UUID accountId, boolean admin) {
        var detail = queries.getOrderDetail(id, accountId, admin);
        var o = detail.order();
        return new OrderResponse(o.getId(), o.getOrderCode(), o.getStatus(), o.getPaymentMethod(),
                o.getSubtotal(), o.getDiscountAmount(), o.getTotalAmount(), o.getRecipientName(),
                o.getRecipientPhone(), o.getShippingAddress(), mapItems(detail.items(), thumbnails(detail.items())),
                o.getPaymentMethod() == PaymentMethod.BANK_TRANSFER ? bank : null, o.getCreatedAt());
    }

    private Map<UUID, String> thumbnails(List<OrderItem> orderItems) {
        var ids = orderItems.stream().map(i -> i.getVariant().getProduct().getId()).distinct().toList();
        Map<UUID, String> result = new HashMap<>();
        if (!ids.isEmpty()) images.findByProduct_IdInOrderByDisplayOrderAscIdAsc(ids)
                .forEach(image -> result.putIfAbsent(image.getProduct().getId(), image.getFilePath()));
        return result;
    }

    private List<OrderItemResponse> mapItems(List<OrderItem> orderItems, Map<UUID, String> thumbnails) {
        return orderItems.stream().sorted(Comparator.comparing(OrderItem::getId))
                .map(i -> OrderItemResponse.from(i, thumbnails.get(i.getVariant().getProduct().getId()))).toList();
    }
}
