package com.rabbitmqoutbox.warehouseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqoutbox.warehouseservice.model.dto.request.CreateStockRequest;
import com.rabbitmqoutbox.warehouseservice.model.dto.response.StockResponse;
import com.rabbitmqoutbox.warehouseservice.model.entity.StockEntity;
import com.rabbitmqoutbox.warehouseservice.repository.StockRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("StockController Integration Tests")
class StockControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("warehouse_test")
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
    private StockRepository stockRepository;

    private CreateStockRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
        validCreateRequest = CreateStockRequest.builder()
                .productId("PROD-001")
                .productName("Test Product")
                .availableQuantity(100)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/stocks - Create Stock Tests")
    class CreateStockEndpointTests {

        @Test
        @DisplayName("Should create stock with valid request")
        void testCreateStockSuccess() throws Exception {
            MvcResult result = assertCreateStockSuccess(validCreateRequest);

            assertStockPersistedCorrectly(result);
        }

        @Test
        @DisplayName("Should fail when productId already exists")
        void testCreateStockDuplicateProductId() throws Exception {
            mockMvc.perform(post("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("DUPLICATE_ENTRY"));
        }

        @Test
        @DisplayName("Should create stock with zero quantity")
        void testCreateStockWithZeroQuantity() throws Exception {
            MvcResult result = assertCreateStockSuccess(CreateStockRequest.builder()
                    .productId("PROD-003")
                    .productName("Zero Stock Product")
                    .availableQuantity(0)
                    .build());

            assertStockPersistedCorrectly(result);
        }

        @Test
        @DisplayName("Should create stock with large quantity")
        void testCreateStockWithLargeQuantity() throws Exception {
            MvcResult result = assertCreateStockSuccess(CreateStockRequest.builder()
                    .productId("PROD-004")
                    .productName("Large Stock Product")
                    .availableQuantity(999999)
                    .build());

            assertStockPersistedCorrectly(result);
        }

        @Test
        @DisplayName("Should fail when productId is blank")
        void testCreateStockMissingProductId() throws Exception {
            assertBadRequest(CreateStockRequest.builder()
                    .productId("")
                    .productName("Test Product")
                    .availableQuantity(100)
                    .build());
        }

        @Test
        @DisplayName("Should fail when productName is blank")
        void testCreateStockMissingProductName() throws Exception {
            assertBadRequest(CreateStockRequest.builder()
                    .productId("PROD-005")
                    .productName("")
                    .availableQuantity(100)
                    .build());
        }

        @Test
        @DisplayName("Should fail when availableQuantity is null")
        void testCreateStockMissingQuantity() throws Exception {
            assertBadRequestRaw("""
                    {
                        "productId": "PROD-006",
                        "productName": "Test Product"
                    }
                    """);
        }

        @Test
        @DisplayName("Should fail when availableQuantity is negative")
        void testCreateStockNegativeQuantity() throws Exception {
            assertBadRequest(CreateStockRequest.builder()
                    .productId("PROD-007")
                    .productName("Test Product")
                    .availableQuantity(-10)
                    .build());
        }

        @Test
        @DisplayName("Should fail with invalid JSON")
        void testCreateStockInvalidJson() throws Exception {
            mockMvc.perform(post("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail with missing Content-Type header")
        void testCreateStockMissingContentType() throws Exception {
            mockMvc.perform(post("/api/v1/stocks")
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isUnsupportedMediaType());
        }

        private MvcResult assertCreateStockSuccess(CreateStockRequest request) throws Exception {
            return mockMvc.perform(post("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.stockId").exists())
                    .andExpect(jsonPath("$.data.productId").value(request.getProductId()))
                    .andExpect(jsonPath("$.data.productName").value(request.getProductName()))
                    .andExpect(jsonPath("$.data.availableQuantity").value(request.getAvailableQuantity()))
                    .andExpect(jsonPath("$.data.reservedQuantity").value(0))
                    .andReturn();
        }

        private void assertStockPersistedCorrectly(MvcResult result) throws Exception {
            String responseBody = result.getResponse().getContentAsString();
            StockResponse stockResponse = objectMapper.readValue(
                    objectMapper.readTree(responseBody).get("data").toString(),
                    StockResponse.class
            );

            StockEntity savedStock = stockRepository
                    .findByProductId(stockResponse.getProductId())
                    .orElseThrow();

            assertThat(savedStock.getId()).isEqualTo(stockResponse.getStockId());
        }

        private void assertBadRequest(Object request) throws Exception {
            mockMvc.perform(post("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }

        private void assertBadRequestRaw(String body) throws Exception {
            mockMvc.perform(post("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stocks - Get All Stocks Tests")
    class GetAllStocksEndpointTests {

        @Test
        @DisplayName("Should return empty list when no stocks exist")
        void testGetAllStocksEmpty() throws Exception {
            mockMvc.perform(get("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return all stocks successfully")
        void testGetAllStocksSuccess() throws Exception {
            CreateStockRequest request1 = CreateStockRequest.builder()
                    .productId("PROD-001")
                    .productName("Product 1")
                    .availableQuantity(100)
                    .build();

            CreateStockRequest request2 = CreateStockRequest.builder()
                    .productId("PROD-002")
                    .productName("Product 2")
                    .availableQuantity(50)
                    .build();

            mockMvc.perform(post("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            MvcResult result = mockMvc.perform(get("/api/v1/stocks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].stockId").exists())
                    .andExpect(jsonPath("$.data[0].productId").exists())
                    .andExpect(jsonPath("$.data[0].productName").exists())
                    .andExpect(jsonPath("$.data[0].availableQuantity").exists())
                    .andExpect(jsonPath("$.data[0].reservedQuantity").exists())
                    .andExpect(jsonPath("$.data[0].createdAt").exists())
                    .andExpect(jsonPath("$.data[0].updatedAt").exists())
                    .andExpect(jsonPath("$.data[1].stockId").exists())
                    .andExpect(jsonPath("$.data[1].productId").exists())
                    .andExpect(jsonPath("$.data[1].productName").exists())
                    .andExpect(jsonPath("$.data[1].availableQuantity").exists())
                    .andExpect(jsonPath("$.data[1].reservedQuantity").exists())
                    .andExpect(jsonPath("$.data[1].createdAt").exists())
                    .andExpect(jsonPath("$.data[1].updatedAt").exists())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            List<StockResponse> stockResponses = objectMapper.readValue(
                    objectMapper.readTree(responseBody).get("data").toString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StockResponse.class)
            );

            for (StockResponse stockResponse : stockResponses) {
                StockEntity savedStock = stockRepository
                        .findByProductId(stockResponse.getProductId())
                        .orElseThrow();
                assertThat(savedStock.getId()).isEqualTo(stockResponse.getStockId());
            }
        }
    }
}