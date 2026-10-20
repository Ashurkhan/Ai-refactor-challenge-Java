package kg.example.legacy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.example.legacy.dto.OrderRequest;
import kg.example.legacy.entity.Order;
import kg.example.legacy.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    private Long existingId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        Order saved = orderRepository.save(new Order("Alice", "Keyboard", new BigDecimal("120.00"), 1));
        orderRepository.save(new Order("Bob", "Mouse", new BigDecimal("15.50"), 2));
        existingId = saved.getId();
    }

    @Test
    void createOrderReturnsCalculatedTotal() throws Exception {
        OrderRequest request = new OrderRequest("Carol", "Monitor", new BigDecimal("199.99"), 2);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Carol"))
                .andExpect(jsonPath("$.total").value(399.98));
    }

    @Test
    void createOrderRejectsInvalidPayload() throws Exception {
        String payload = """
                {"customerName":"","productName":"X","price":-1,"quantity":0}
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void listOrdersIsPaginated() throws Exception {
        mockMvc.perform(get("/api/orders").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void searchUsesDatabaseQueryAndIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/orders/search").param("q", "key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].productName", hasItem("Keyboard")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void expensiveFiltersByPriceInSql() throws Exception {
        mockMvc.perform(get("/api/orders/expensive").param("price", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Keyboard"));
    }

    @Test
    void getMissingOrderReturns404() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Order not found")));
    }

    @Test
    void totalEndpointReturnsStoredTotal() throws Exception {
        mockMvc.perform(get("/api/orders/{id}/total", existingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(existingId))
                .andExpect(jsonPath("$.total").value(120.00));
    }

    @Test
    void updateMissingOrderReturns404() throws Exception {
        OrderRequest request = new OrderRequest("Dana", "Cable", new BigDecimal("5.00"), 1);

        mockMvc.perform(put("/api/orders/{id}", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingOrderReturns404() throws Exception {
        mockMvc.perform(delete("/api/orders/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistingOrderReturnsMessage() throws Exception {
        mockMvc.perform(delete("/api/orders/{id}", existingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("deleted"));
    }
}
