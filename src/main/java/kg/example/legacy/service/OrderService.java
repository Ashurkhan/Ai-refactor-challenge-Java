package kg.example.legacy.service;

import kg.example.legacy.dto.OrderRequest;
import kg.example.legacy.dto.OrderResponse;
import kg.example.legacy.dto.OrderTotalResponse;
import kg.example.legacy.entity.Order;
import kg.example.legacy.exception.OrderNotFoundException;
import kg.example.legacy.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse create(OrderRequest request) {
        Order order = new Order(
                request.customerName(),
                request.productName(),
                request.price(),
                request.quantity()
        );
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrder(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> search(String query, Pageable pageable) {
        return orderRepository.searchByCustomerOrProduct(query, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findExpensive(BigDecimal minPrice, Pageable pageable) {
        return orderRepository.findByPriceGreaterThan(minPrice, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderTotalResponse getTotal(Long id) {
        Order order = getOrder(id);
        return new OrderTotalResponse(order.getId(), order.getTotal());
    }

    public OrderResponse update(Long id, OrderRequest request) {
        Order order = getOrder(id);
        order.setCustomerName(request.customerName());
        order.setProductName(request.productName());
        order.setPrice(request.price());
        order.setQuantity(request.quantity());
        order.recalculateTotal();
        return OrderResponse.from(orderRepository.save(order));
    }

    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteById(id);
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
