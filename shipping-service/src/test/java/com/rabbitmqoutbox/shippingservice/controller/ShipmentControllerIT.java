package com.rabbitmqoutbox.shippingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.shippingservice.model.dto.request.FailShipmentRequest;
import com.rabbitmqoutbox.shippingservice.model.entity.ShipmentEntity;
import com.rabbitmqoutbox.shippingservice.model.enums.ShipmentStatus;
import com.rabbitmqoutbox.shippingservice.repository.OutboxEventRepository;
import com.rabbitmqoutbox.shippingservice.repository.ShipmentRepository;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("ShipmentController Integration Tests")
class ShipmentControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shipping_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        shipmentRepository.deleteAll();
    }

    private ShipmentEntity buildShipment(ShipmentStatus status) {
        return ShipmentEntity.builder()
                .orderId(UUID.randomUUID())
                .customerId("CST-001")
                .customerEmail("customer@email.com")
                .deliveryAddress("123 Main St")
                .status(status)
                .scheduledAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("PATCH /api/v1/shipments/{id}/dispatch — Dispatch Tests")
    class DispatchShipmentEndpointTests {

        @Test
        @DisplayName("Should dispatch shipment successfully")
        void testDispatchSuccess() throws Exception {
            ShipmentEntity shipment = shipmentRepository.save(
                    buildShipment(ShipmentStatus.SCHEDULED));

            mockMvc.perform(patch("/api/v1/shipments/{id}/dispatch", shipment.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.shipmentId")
                            .value(shipment.getId().toString()))
                    .andExpect(jsonPath("$.data.status").value("OUT_FOR_DELIVERY"))
                    .andExpect(jsonPath("$.data.dispatchedAt").exists());

            ShipmentEntity updated = shipmentRepository.findById(shipment.getId())
                    .orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.OUT_FOR_DELIVERY);
            assertThat(updated.getDispatchedAt()).isNotNull();

            assertThat(outboxEventRepository.findAll())
                    .anyMatch(e -> e.getEventType().equals("SHIPMENT_OUT_FOR_DELIVERY"));
        }

        @Test
        @DisplayName("Should return 404 when shipment not found")
        void testDispatchNotFound() throws Exception {
            mockMvc.perform(patch("/api/v1/shipments/{id}/dispatch", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("SHIPMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 409 when shipment is not SCHEDULED")
        void testDispatchInvalidStatus() throws Exception {
            ShipmentEntity shipment = shipmentRepository.save(
                    buildShipment(ShipmentStatus.OUT_FOR_DELIVERY));

            mockMvc.perform(patch("/api/v1/shipments/{id}/dispatch", shipment.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("INVALID_SHIPMENT_STATE"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/shipments/{id}/deliver — Deliver Tests")
    class DeliverShipmentEndpointTests {

        @Test
        @DisplayName("Should deliver shipment successfully")
        void testDeliverSuccess() throws Exception {
            ShipmentEntity shipment = shipmentRepository.save(
                    buildShipment(ShipmentStatus.OUT_FOR_DELIVERY));

            mockMvc.perform(patch("/api/v1/shipments/{id}/deliver", shipment.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                    .andExpect(jsonPath("$.data.deliveredAt").exists());

            ShipmentEntity updated = shipmentRepository.findById(shipment.getId())
                    .orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
            assertThat(updated.getDeliveredAt()).isNotNull();

            assertThat(outboxEventRepository.findAll())
                    .anyMatch(e -> e.getEventType().equals("SHIPMENT_DELIVERED"));
        }

        @Test
        @DisplayName("Should return 404 when shipment not found")
        void testDeliverNotFound() throws Exception {
            mockMvc.perform(patch("/api/v1/shipments/{id}/deliver", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("SHIPMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 409 when shipment is not OUT_FOR_DELIVERY")
        void testDeliverInvalidStatus() throws Exception {
            ShipmentEntity shipment = shipmentRepository.save(
                    buildShipment(ShipmentStatus.SCHEDULED));

            mockMvc.perform(patch("/api/v1/shipments/{id}/deliver", shipment.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("INVALID_SHIPMENT_STATE"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/shipments/{id}/fail — Fail Tests")
    class FailShipmentEndpointTests {

        @Test
        @DisplayName("Should fail shipment successfully")
        void testFailSuccess() throws Exception {
            ShipmentEntity shipment = shipmentRepository.save(
                    buildShipment(ShipmentStatus.SCHEDULED));

            FailShipmentRequest request = FailShipmentRequest.builder()
                    .failureReason("Delivery address not found")
                    .build();

            mockMvc.perform(patch("/api/v1/shipments/{id}/fail", shipment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.failedAt").exists());

            ShipmentEntity updated = shipmentRepository.findById(shipment.getId())
                    .orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ShipmentStatus.FAILED);
            assertThat(updated.getFailedAt()).isNotNull();

            assertThat(outboxEventRepository.findAll())
                    .anyMatch(e -> e.getEventType().equals("SHIPMENT_FAILED"));
        }

        @Test
        @DisplayName("Should return 404 when shipment not found")
        void testFailNotFound() throws Exception {
            FailShipmentRequest request = FailShipmentRequest.builder()
                    .failureReason("reason")
                    .build();

            mockMvc.perform(patch("/api/v1/shipments/{id}/fail", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("SHIPMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 409 when shipment is already delivered")
        void testFailDeliveredShipment() throws Exception {
            ShipmentEntity shipment = shipmentRepository.save(
                    buildShipment(ShipmentStatus.DELIVERED));

            FailShipmentRequest request = FailShipmentRequest.builder()
                    .failureReason("reason")
                    .build();

            mockMvc.perform(patch("/api/v1/shipments/{id}/fail", shipment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("INVALID_SHIPMENT_STATE"));
        }

        @Test
        @DisplayName("Should return 400 when failureReason is missing")
        void testFailMissingReason() throws Exception {
            ShipmentEntity shipment = shipmentRepository.save(
                    buildShipment(ShipmentStatus.SCHEDULED));

            mockMvc.perform(patch("/api/v1/shipments/{id}/fail", shipment.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/shipments/{id} — Get By Id Tests")
    class GetShipmentByIdEndpointTests {

        @Test
        @DisplayName("Should return shipment by id successfully")
        void testGetByIdSuccess() throws Exception {
            ShipmentEntity shipment = shipmentRepository.save(
                    buildShipment(ShipmentStatus.SCHEDULED));

            mockMvc.perform(get("/api/v1/shipments/{id}", shipment.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.shipmentId")
                            .value(shipment.getId().toString()))
                    .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.data.orderId").exists())
                    .andExpect(jsonPath("$.data.customerId").exists())
                    .andExpect(jsonPath("$.data.customerEmail").exists())
                    .andExpect(jsonPath("$.data.deliveryAddress").exists())
                    .andExpect(jsonPath("$.data.scheduledAt").exists())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.updatedAt").exists());
        }

        @Test
        @DisplayName("Should return 404 when shipment not found")
        void testGetByIdNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/shipments/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("SHIPMENT_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/shipments — Get All Tests")
    class GetAllShipmentsEndpointTests {

        @Test
        @DisplayName("Should return empty list when no shipments exist")
        void testGetAllEmpty() throws Exception {
            mockMvc.perform(get("/api/v1/shipments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return all shipments successfully")
        void testGetAllSuccess() throws Exception {
            shipmentRepository.save(buildShipment(ShipmentStatus.SCHEDULED));
            shipmentRepository.save(buildShipment(ShipmentStatus.DELIVERED));

            mockMvc.perform(get("/api/v1/shipments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2));
        }
    }
}
