package kg.example.legacy.dto;

import kg.example.legacy.entity.Order;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        String customerName,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal total
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getProductName(),
                order.getPrice(),
                order.getQuantity(),
                order.getTotal()
        );
    }
}
