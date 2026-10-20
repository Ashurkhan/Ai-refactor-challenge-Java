package kg.example.legacy.service;

import kg.example.legacy.dto.OrderRequest;
import kg.example.legacy.dto.OrderResponse;
import kg.example.legacy.dto.OrderTotalResponse;
import kg.example.legacy.entity.Order;
import kg.example.legacy.exception.OrderNotFoundException;
import kg.example.legacy.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order existingOrder;

    @BeforeEach
    void setUp() {
        existingOrder = new Order("Alice", "Keyboard", new BigDecimal("25.50"), 2);
        setId(existingOrder, 1L);
    }

    @Test
    void createCalculatesTotalWithBigDecimal() {
        OrderRequest request = new OrderRequest("Alice", "Keyboard", new BigDecimal("25.50"), 2);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            setId(order, 10L);
            return order;
        });

        OrderResponse response = orderService.create(request);

        assertThat(response.total()).isEqualByComparingTo("51.00");
        assertThat(response.price()).isEqualByComparingTo("25.50");
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getTotal()).isEqualByComparingTo("51.00");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found: 99");
    }

    @Test
    void getTotalUsesPersistedAmount() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));

        OrderTotalResponse total = orderService.getTotal(1L);

        assertThat(total.orderId()).isEqualTo(1L);
        assertThat(total.total()).isEqualByComparingTo("51.00");
    }

    @Test
    void updateRecalculatesTotalAndThrowsWhenMissing() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderRequest request = new OrderRequest("Bob", "Mouse", new BigDecimal("10.00"), 3);
        OrderResponse updated = orderService.update(1L, request);

        assertThat(updated.customerName()).isEqualTo("Bob");
        assertThat(updated.total()).isEqualByComparingTo("30.00");

        when(orderRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.update(5L, request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenOrderDoesNotExist() {
        when(orderRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.delete(7L))
                .isInstanceOf(OrderNotFoundException.class);
        verify(orderRepository, never()).deleteById(7L);
    }

    @Test
    void searchDelegatesToRepositoryQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.searchByCustomerOrProduct("key", pageable))
                .thenReturn(new PageImpl<>(List.of(existingOrder), pageable, 1));

        Page<OrderResponse> result = orderService.search("key", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productName()).isEqualTo("Keyboard");
        verify(orderRepository).searchByCustomerOrProduct("key", pageable);
        verify(orderRepository, never()).findAll();
    }

    @Test
    void findExpensiveDelegatesToPriceQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        BigDecimal minPrice = new BigDecimal("20.00");
        when(orderRepository.findByPriceGreaterThan(minPrice, pageable))
                .thenReturn(new PageImpl<>(List.of(existingOrder), pageable, 1));

        Page<OrderResponse> result = orderService.findExpensive(minPrice, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderRepository).findByPriceGreaterThan(minPrice, pageable);
    }

    private void setId(Order order, Long id) {
        try {
            var field = Order.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(order, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
