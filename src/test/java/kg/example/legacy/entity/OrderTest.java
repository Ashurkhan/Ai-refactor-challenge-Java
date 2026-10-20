package kg.example.legacy.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void recalculateTotalUsesHalfUpScale() {
        Order order = new Order("Alice", "Item", new BigDecimal("10.555"), 3);

        assertThat(order.getPrice()).isEqualByComparingTo("10.56");
        assertThat(order.getTotal()).isEqualByComparingTo("31.68");
    }

    @Test
    void updateQuantityRecalculatesTotalOnce() {
        Order order = new Order("Alice", "Item", new BigDecimal("10.00"), 1);
        order.setQuantity(4);
        order.recalculateTotal();

        assertThat(order.getTotal()).isEqualByComparingTo("40.00");
    }
}
