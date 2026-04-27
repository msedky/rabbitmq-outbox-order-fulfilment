package com.rabbitmqoutbox.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.orderservice.model.dto.request.CreateOrderItemRequest;
import com.rabbitmqoutbox.orderservice.model.dto.request.CreateOrderRequest;
import com.rabbitmqoutbox.orderservice.model.entity.OrderEntity;
import com.rabbitmqoutbox.orderservice.model.entity.OutboxEventEntity;
import com.rabbitmqoutbox.orderservice.model.enums.OrderStatus;
import com.rabbitmqoutbox.orderservice.model.enums.OutboxStatus;
import com.rabbitmqoutbox.orderservice.repository.OrderRepository;
import com.rabbitmqoutbox.orderservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("OrderController Integration Tests")
class OrderControllerIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("order_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.show-sql", () -> "false");

        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;


    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    // =========================
    // CREATE ORDER
    // =========================
    @Nested
    @DisplayName("Create Order API Tests")
    class CreateOrderApiTests {

        @Test
        @DisplayName("Should create order and outbox event successfully")
        void testCreateOrderSuccess() throws Exception {

            CreateOrderRequest request = buildValidRequest();

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));

            List<OrderEntity> orders = orderRepository.findAll();
            assertThat(orders).hasSize(1);

            List<OutboxEventEntity> events = outboxEventRepository.findAll();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("ORDER_PLACED");
            assertThat(events.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
        }

        @Test
        @DisplayName("Should return validation error for invalid request")
        void testCreateOrderValidationFailure() throws Exception {

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    // =========================
    // CANCEL ORDER
    // =========================
    @Nested
    @DisplayName("Cancel Order API Tests")
    class CancelOrderApiTests {

        @Test
        @DisplayName("Should cancel order and create outbox event")
        void testCancelOrderSuccess() throws Exception {

            OrderEntity order = orderRepository.save(buildOrder(OrderStatus.PENDING));

            mockMvc.perform(patch("/api/v1/orders/{id}/cancel", order.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            OrderEntity updated = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            List<OutboxEventEntity> events = outboxEventRepository.findAll();
            assertThat(events).anyMatch(e -> e.getEventType().equals("ORDER_CANCELLED"));
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void testCancelOrderNotFound() throws Exception {

            mockMvc.perform(patch("/api/v1/orders/{id}/cancel", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 409 for invalid state")
        void testCancelOrderInvalidState() throws Exception {

            OrderEntity order = orderRepository.save(buildOrder(OrderStatus.DELIVERED));

            mockMvc.perform(patch("/api/v1/orders/{id}/cancel", order.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_STATE"));
        }
    }

    // =========================
    // GET BY ID
    // =========================
    @Nested
    @DisplayName("Get Order By Id API Tests")
    class GetOrderByIdApiTests {

        @Test
        @DisplayName("Should return order successfully")
        void testGetByIdSuccess() throws Exception {

            OrderEntity order = orderRepository.save(buildOrder(OrderStatus.PENDING));

            mockMvc.perform(get("/api/v1/orders/{id}", order.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderId").value(order.getId().toString()));
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void testGetByIdNotFound() throws Exception {

            mockMvc.perform(get("/api/v1/orders/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
        }
    }

    // =========================
    // GET ALL
    // =========================
    @Nested
    @DisplayName("Get All Orders API Tests")
    class GetAllOrdersApiTests {

        @Test
        @DisplayName("Should return all orders")
        void testGetAllSuccess() throws Exception {

            orderRepository.save(buildOrder(OrderStatus.PENDING));
            orderRepository.save(buildOrder(OrderStatus.CONFIRMED));

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("Should return empty list")
        void testGetAllEmpty() throws Exception {

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    // =========================
    // HELPERS
    // =========================
    private CreateOrderRequest buildValidRequest() {
        return CreateOrderRequest.builder()
                .customerId("CST-001")
                .customerEmail("customer@email.com")
                .deliveryAddress("123 Main St")
                .currency("USD")
                .items(List.of(
                        CreateOrderItemRequest.builder()
                                .productId("PROD-001")
                                .productName("Product 1")
                                .quantity(2)
                                .unitPrice(BigDecimal.valueOf(50.0))
                                .build()
                ))
                .build();
    }

    private OrderEntity buildOrder(OrderStatus status) {
        return OrderEntity.builder()
                .customerId("CST-001")
                .customerEmail("customer@email.com")
                .deliveryAddress("123 Main St")
                .totalAmount(BigDecimal.valueOf(100.0))
                .currency("USD")
                .status(status)
                .items(List.of())
                .build();
    }
}