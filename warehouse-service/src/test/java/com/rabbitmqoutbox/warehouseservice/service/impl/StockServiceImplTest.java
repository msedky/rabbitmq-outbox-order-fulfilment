package com.rabbitmqoutbox.warehouseservice.service.impl;

import com.rabbitmqoutbox.warehouseservice.exception.InsufficientStockException;
import com.rabbitmqoutbox.warehouseservice.exception.StockNotFoundException;
import com.rabbitmqoutbox.warehouseservice.mapper.StockMapper;
import com.rabbitmqoutbox.warehouseservice.mapper.StockMapperImpl;
import com.rabbitmqoutbox.warehouseservice.messaging.event.*;
import com.rabbitmqoutbox.warehouseservice.model.dto.request.CreateStockRequest;
import com.rabbitmqoutbox.warehouseservice.model.dto.response.StockResponse;
import com.rabbitmqoutbox.warehouseservice.model.entity.StockEntity;
import com.rabbitmqoutbox.warehouseservice.model.entity.StockReservationEntity;
import com.rabbitmqoutbox.warehouseservice.model.enums.ReservationStatus;
import com.rabbitmqoutbox.warehouseservice.repository.StockRepository;
import com.rabbitmqoutbox.warehouseservice.repository.StockReservationRepository;
import com.rabbitmqoutbox.warehouseservice.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockServiceImpl Unit Tests")
class StockServiceImplTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private OutboxEventService outboxEventService;

    // Use actual StockMapper implementation instead of mocking
    private StockMapper stockMapper;

    private StockServiceImpl stockService;

    private UUID stockId;
    private String productId;
    private String productName;
    private Integer availableQuantity;

    @BeforeEach
    void setUp() {
        stockId = UUID.randomUUID();
        productId = "PROD-001";
        productName = "Product Name";
        availableQuantity = 100;
        
        // Initialize actual StockMapper (MapStruct generates implementation at compile time)
        stockMapper = new StockMapperImpl();
        
        // Initialize service with actual mapper and mocked repositories
        stockService = new StockServiceImpl(
                stockRepository,
                stockReservationRepository,
                outboxEventService,
                stockMapper
        );
    }

    @Nested
    @DisplayName("Create Stock Tests")
    class CreateStockTests {

        @Test
        @DisplayName("Should create stock successfully with valid request")
        void testCreateStockSuccess() {
            // Arrange
            CreateStockRequest request = CreateStockRequest.builder()
                    .productId(productId)
                    .productName(productName)
                    .availableQuantity(availableQuantity)
                    .build();

            StockEntity stockEntity = StockEntity.builder()
                    .id(stockId)
                    .productId(productId)
                    .productName(productName)
                    .availableQuantity(availableQuantity)
                    .reservedQuantity(0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(stockRepository.save(any(StockEntity.class))).thenReturn(stockEntity);

            // Act
            StockResponse result = stockService.create(request);

            // Assert - Verify mapping was applied correctly by actual StockMapper
            assertThat(result).isNotNull();
            assertThat(result.getStockId()).isEqualTo(stockId);
            assertThat(result.getProductId()).isEqualTo(productId);
            assertThat(result.getProductName()).isEqualTo(productName);
            assertThat(result.getAvailableQuantity()).isEqualTo(availableQuantity);
            assertThat(result.getReservedQuantity()).isEqualTo(0);

            verify(stockRepository, times(1)).save(any(StockEntity.class));
        }

        @Test
        @DisplayName("Should create stock with zero quantity")
        void testCreateStockWithZeroQuantity() {
            // Arrange
            CreateStockRequest request = CreateStockRequest.builder()
                    .productId(productId)
                    .productName(productName)
                    .availableQuantity(0)
                    .build();

            StockEntity stockEntity = StockEntity.builder()
                    .id(stockId)
                    .productId(productId)
                    .productName(productName)
                    .availableQuantity(0)
                    .reservedQuantity(0)
                    .build();

            when(stockRepository.save(any(StockEntity.class))).thenReturn(stockEntity);

            // Act
            StockResponse result = stockService.create(request);

            // Assert - Actual mapper validates the mapping works
            assertThat(result.getAvailableQuantity()).isEqualTo(0);
            assertThat(result.getReservedQuantity()).isEqualTo(0);
            verify(stockRepository, times(1)).save(any(StockEntity.class));
        }
    }

    @Nested
    @DisplayName("Get All Stocks Tests")
    class GetAllStocksTests {

        @Test
        @DisplayName("Should get all stocks successfully")
        void testGetAllStocksSuccess() {
            // Arrange
            StockEntity stock1 = StockEntity.builder()
                    .id(UUID.randomUUID())
                    .productId("PROD-001")
                    .productName("Product 1")
                    .availableQuantity(100)
                    .reservedQuantity(10)
                    .build();

            StockEntity stock2 = StockEntity.builder()
                    .id(UUID.randomUUID())
                    .productId("PROD-002")
                    .productName("Product 2")
                    .availableQuantity(50)
                    .reservedQuantity(5)
                    .build();

            when(stockRepository.findAll()).thenReturn(List.of(stock1, stock2));

            // Act - Using actual StockMapper to verify mapping works
            List<StockResponse> result = stockService.getAll();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting("productId")
                    .containsExactly("PROD-001", "PROD-002");
            assertThat(result.get(0).getAvailableQuantity()).isEqualTo(100);
            assertThat(result.get(1).getAvailableQuantity()).isEqualTo(50);

            verify(stockRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no stocks exist")
        void testGetAllStocksEmpty() {
            // Arrange
            when(stockRepository.findAll()).thenReturn(List.of());

            // Act
            List<StockResponse> result = stockService.getAll();

            // Assert
            assertThat(result).isEmpty();
            verify(stockRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Reserve Stock Tests")
    class ReserveStockTests {

        @Test
        @DisplayName("Should reserve stock successfully with single item")
        void testReserveStockSuccessWithSingleItem() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            String customerId = "CUST-001";
            String customerEmail = "customer@example.com";
            String deliveryAddress = "123 Main St";

            OrderPlacedEventItem item = OrderPlacedEventItem.builder()
                    .productId(productId)
                    .productName(productName)
                    .quantity(10)
                    .unitPrice(BigDecimal.valueOf(100))
                    .build();

            OrderPlacedEvent event = OrderPlacedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId(customerId)
                    .customerEmail(customerEmail)
                    .deliveryAddress(deliveryAddress)
                    .items(List.of(item))
                    .build();

            StockEntity stock = StockEntity.builder()
                    .id(stockId)
                    .productId(productId)
                    .productName(productName)
                    .availableQuantity(100)
                    .reservedQuantity(0)
                    .build();

            when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
            when(stockRepository.save(argThat(s -> s.getProductId().equals(productId)))).thenReturn(stock);
            when(stockReservationRepository.save(argThat(r -> 
                    r.getOrderId().equals(orderId) && 
                    r.getProductId().equals(productId) && 
                    r.getQuantity() == 10 &&
                    r.getStatus() == ReservationStatus.RESERVED
            ))).thenReturn(StockReservationEntity.builder().build());

            // Act
            stockService.reserveStock(event);

            // Assert
            verify(stockRepository, times(1)).findByProductId(productId);
            verify(stockRepository, times(1)).save(argThat(s ->
                    s.getAvailableQuantity() == 90 && s.getReservedQuantity() == 10
            ));
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r.getOrderId().equals(orderId) &&
                    r.getProductId().equals(productId) &&
                    r.getQuantity() == 10 &&
                    r.getStatus() == ReservationStatus.RESERVED
            ));
            verify(outboxEventService, times(1)).saveEvent(
                    eq(orderId.toString()),
                    eq("STOCK"),
                    eq("STOCK_RESERVED"),
                    argThat(event_ -> event_ instanceof StockReservedEvent)
            );
        }

        @Test
        @DisplayName("Should reserve stock successfully with multiple items (3 items)")
        void testReserveStockSuccessWithMultipleItems() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderPlacedEventItem item1 = OrderPlacedEventItem.builder()
                    .productId("PROD-001")
                    .productName("Product 1")
                    .quantity(10)
                    .unitPrice(BigDecimal.valueOf(100))
                    .build();

            OrderPlacedEventItem item2 = OrderPlacedEventItem.builder()
                    .productId("PROD-002")
                    .productName("Product 2")
                    .quantity(20)
                    .unitPrice(BigDecimal.valueOf(50))
                    .build();

            OrderPlacedEventItem item3 = OrderPlacedEventItem.builder()
                    .productId("PROD-003")
                    .productName("Product 3")
                    .quantity(15)
                    .unitPrice(BigDecimal.valueOf(75))
                    .build();

            OrderPlacedEvent event = OrderPlacedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId("CUST-001")
                    .customerEmail("customer@example.com")
                    .deliveryAddress("123 Main St")
                    .items(List.of(item1, item2, item3))
                    .build();

            StockEntity stock1 = StockEntity.builder()
                    .id(UUID.randomUUID())
                    .productId("PROD-001")
                    .productName("Product 1")
                    .availableQuantity(100)
                    .reservedQuantity(0)
                    .build();

            StockEntity stock2 = StockEntity.builder()
                    .id(UUID.randomUUID())
                    .productId("PROD-002")
                    .productName("Product 2")
                    .availableQuantity(100)
                    .reservedQuantity(0)
                    .build();

            StockEntity stock3 = StockEntity.builder()
                    .id(UUID.randomUUID())
                    .productId("PROD-003")
                    .productName("Product 3")
                    .availableQuantity(100)
                    .reservedQuantity(0)
                    .build();

            // Setup mocks for finding stocks
            when(stockRepository.findByProductId("PROD-001")).thenReturn(Optional.of(stock1));
            when(stockRepository.findByProductId("PROD-002")).thenReturn(Optional.of(stock2));
            when(stockRepository.findByProductId("PROD-003")).thenReturn(Optional.of(stock3));
            
            // Setup mocks for saving stocks - use any() for setup, specific checks in verify
            when(stockRepository.save(any(StockEntity.class))).thenReturn(stock1).thenReturn(stock2).thenReturn(stock3);
            
            // Setup mocks for saving reservations
            when(stockReservationRepository.save(any(StockReservationEntity.class))).thenReturn(StockReservationEntity.builder().build());

            // Act
            stockService.reserveStock(event);

            // Assert - Verify all 3 products were processed
            verify(stockRepository, times(1)).findByProductId("PROD-001");
            verify(stockRepository, times(1)).findByProductId("PROD-002");
            verify(stockRepository, times(1)).findByProductId("PROD-003");
            
            // Verify stock saves with correct quantities
            verify(stockRepository, times(1)).save(argThat(s ->
                    s != null && s.getProductId().equals("PROD-001") && s.getAvailableQuantity() == 90 && s.getReservedQuantity() == 10
            ));
            verify(stockRepository, times(1)).save(argThat(s ->
                    s != null && s.getProductId().equals("PROD-002") && s.getAvailableQuantity() == 80 && s.getReservedQuantity() == 20
            ));
            verify(stockRepository, times(1)).save(argThat(s ->
                    s != null && s.getProductId().equals("PROD-003") && s.getAvailableQuantity() == 85 && s.getReservedQuantity() == 15
            ));
            
            // Verify reservations saved with correct details
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r != null && r.getOrderId().equals(orderId) &&
                    r.getProductId().equals("PROD-001") &&
                    r.getQuantity() == 10 &&
                    r.getStatus() == ReservationStatus.RESERVED
            ));
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r != null && r.getOrderId().equals(orderId) &&
                    r.getProductId().equals("PROD-002") &&
                    r.getQuantity() == 20 &&
                    r.getStatus() == ReservationStatus.RESERVED
            ));
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r != null && r.getOrderId().equals(orderId) &&
                    r.getProductId().equals("PROD-003") &&
                    r.getQuantity() == 15 &&
                    r.getStatus() == ReservationStatus.RESERVED
            ));
            
            verify(outboxEventService, times(1)).saveEvent(
                    eq(orderId.toString()),
                    eq("STOCK"),
                    eq("STOCK_RESERVED"),
                    argThat(event_ -> event_ instanceof StockReservedEvent)
            );
        }

        @Test
        @DisplayName("Should throw InsufficientStockException when stock is insufficient")
        void testReserveStockInsufficientQuantity() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderPlacedEventItem item = OrderPlacedEventItem.builder()
                    .productId(productId)
                    .productName(productName)
                    .quantity(150)  // More than available
                    .unitPrice(BigDecimal.valueOf(100))
                    .build();

            OrderPlacedEvent event = OrderPlacedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId("CUST-001")
                    .customerEmail("customer@example.com")
                    .deliveryAddress("123 Main St")
                    .items(List.of(item))
                    .build();

            StockEntity stock = StockEntity.builder()
                    .id(stockId)
                    .productId(productId)
                    .productName(productName)
                    .availableQuantity(100)
                    .reservedQuantity(0)
                    .build();

            when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));

            // Act & Assert
            assertThatThrownBy(() -> stockService.reserveStock(event))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock");

            verify(stockRepository, never()).save(any(StockEntity.class));
            verify(stockReservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw StockNotFoundException when stock not found")
        void testReserveStockNotFound() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderPlacedEventItem item = OrderPlacedEventItem.builder()
                    .productId(productId)
                    .productName(productName)
                    .quantity(10)
                    .unitPrice(BigDecimal.valueOf(100))
                    .build();

            OrderPlacedEvent event = OrderPlacedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId("CUST-001")
                    .customerEmail("customer@example.com")
                    .deliveryAddress("123 Main St")
                    .items(List.of(item))
                    .build();

            when(stockRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> stockService.reserveStock(event))
                    .isInstanceOf(StockNotFoundException.class)
                    .hasMessageContaining("Stock not found");

            verify(stockRepository, never()).save(any());
            verify(stockReservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Release Stock Tests")
    class ReleaseStockTests {

        @Test
        @DisplayName("Should release stock successfully")
        void testReleaseStockSuccess() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId("CUST-001")
                    .customerEmail("customer@example.com")
                    .build();

            StockReservationEntity reservation = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(10)
                    .status(ReservationStatus.RESERVED)
                    .build();

            StockEntity stock = StockEntity.builder()
                    .id(stockId)
                    .productId(productId)
                    .productName(productName)
                    .availableQuantity(90)
                    .reservedQuantity(10)
                    .build();

            when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                    .thenReturn(List.of(reservation));
            when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
            when(stockRepository.save(any(StockEntity.class))).thenReturn(stock);
            when(stockReservationRepository.save(any(StockReservationEntity.class)))
                    .thenReturn(reservation);

            // Act
            stockService.releaseStock(event);

            // Assert
            verify(stockRepository, times(1)).findByProductId(productId);
            verify(stockRepository, times(1)).save(argThat(s ->
                    s.getAvailableQuantity() == 100 && s.getReservedQuantity() == 0
            ));
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r.getStatus() == ReservationStatus.RELEASED
            ));
        }

        @Test
        @DisplayName("Should handle no reservations found gracefully")
        void testReleaseStockNoReservationsFound() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId("CUST-001")
                    .customerEmail("customer@example.com")
                    .build();

            when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                    .thenReturn(List.of());

            // Act
            stockService.releaseStock(event);

            // Assert
            verify(stockRepository, never()).save(any());
            verify(stockReservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should release multiple reservations")
        void testReleaseStockMultipleItems() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId("CUST-001")
                    .customerEmail("customer@example.com")
                    .build();

            StockReservationEntity reservation1 = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId("PROD-001")
                    .quantity(10)
                    .status(ReservationStatus.RESERVED)
                    .build();

            StockReservationEntity reservation2 = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId("PROD-002")
                    .quantity(20)
                    .status(ReservationStatus.RESERVED)
                    .build();

            StockEntity stock1 = StockEntity.builder()
                    .productId("PROD-001")
                    .availableQuantity(90)
                    .reservedQuantity(10)
                    .build();

            StockEntity stock2 = StockEntity.builder()
                    .productId("PROD-002")
                    .availableQuantity(80)
                    .reservedQuantity(20)
                    .build();

            when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                    .thenReturn(List.of(reservation1, reservation2));
            when(stockRepository.findByProductId("PROD-001")).thenReturn(Optional.of(stock1));
            when(stockRepository.findByProductId("PROD-002")).thenReturn(Optional.of(stock2));
            when(stockRepository.save(any(StockEntity.class))).thenReturn(stock1);
            when(stockReservationRepository.save(any(StockReservationEntity.class)))
                    .thenReturn(reservation1);

            // Act
            stockService.releaseStock(event);

            // Assert
            verify(stockRepository, times(2)).save(any(StockEntity.class));
            verify(stockReservationRepository, times(2)).save(any(StockReservationEntity.class));
        }

        @Test
        @DisplayName("Should release stock successfully with multiple items (3 items)")
        void testReleaseStockSuccessWithMultipleItems() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId("CUST-001")
                    .customerEmail("customer@example.com")
                    .build();

            StockReservationEntity reservation1 = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId("PROD-001")
                    .quantity(10)
                    .status(ReservationStatus.RESERVED)
                    .build();

            StockReservationEntity reservation2 = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId("PROD-002")
                    .quantity(20)
                    .status(ReservationStatus.RESERVED)
                    .build();

            StockReservationEntity reservation3 = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId("PROD-003")
                    .quantity(15)
                    .status(ReservationStatus.RESERVED)
                    .build();

            StockEntity stock1 = StockEntity.builder()
                    .id(UUID.randomUUID())
                    .productId("PROD-001")
                    .productName("Product 1")
                    .availableQuantity(90)
                    .reservedQuantity(10)
                    .build();

            StockEntity stock2 = StockEntity.builder()
                    .id(UUID.randomUUID())
                    .productId("PROD-002")
                    .productName("Product 2")
                    .availableQuantity(80)
                    .reservedQuantity(20)
                    .build();

            StockEntity stock3 = StockEntity.builder()
                    .id(UUID.randomUUID())
                    .productId("PROD-003")
                    .productName("Product 3")
                    .availableQuantity(85)
                    .reservedQuantity(15)
                    .build();

            when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                    .thenReturn(List.of(reservation1, reservation2, reservation3));
            when(stockRepository.findByProductId("PROD-001")).thenReturn(Optional.of(stock1));
            when(stockRepository.findByProductId("PROD-002")).thenReturn(Optional.of(stock2));
            when(stockRepository.findByProductId("PROD-003")).thenReturn(Optional.of(stock3));
            when(stockRepository.save(any(StockEntity.class))).thenReturn(stock1);
            when(stockReservationRepository.save(any(StockReservationEntity.class)))
                    .thenReturn(reservation1);

            // Act
            stockService.releaseStock(event);

            // Assert - Verify all 3 products were processed
            verify(stockRepository, times(1)).findByProductId("PROD-001");
            verify(stockRepository, times(1)).findByProductId("PROD-002");
            verify(stockRepository, times(1)).findByProductId("PROD-003");

            // Verify stock saves with correct quantities restored
            verify(stockRepository, times(1)).save(argThat(s ->
                    s != null && s.getProductId().equals("PROD-001") && s.getAvailableQuantity() == 100 && s.getReservedQuantity() == 0
            ));
            verify(stockRepository, times(1)).save(argThat(s ->
                    s != null && s.getProductId().equals("PROD-002") && s.getAvailableQuantity() == 100 && s.getReservedQuantity() == 0
            ));
            verify(stockRepository, times(1)).save(argThat(s ->
                    s != null && s.getProductId().equals("PROD-003") && s.getAvailableQuantity() == 100 && s.getReservedQuantity() == 0
            ));

            // Verify reservations saved with RELEASED status
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r != null && r.getOrderId().equals(orderId) &&
                    r.getProductId().equals("PROD-001") &&
                    r.getQuantity() == 10 &&
                    r.getStatus() == ReservationStatus.RELEASED
            ));
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r != null && r.getOrderId().equals(orderId) &&
                    r.getProductId().equals("PROD-002") &&
                    r.getQuantity() == 20 &&
                    r.getStatus() == ReservationStatus.RELEASED
            ));
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r != null && r.getOrderId().equals(orderId) &&
                    r.getProductId().equals("PROD-003") &&
                    r.getQuantity() == 15 &&
                    r.getStatus() == ReservationStatus.RELEASED
            ));
        }

        @Test
        @DisplayName("Should throw StockNotFoundException when stock not found during release")
        void testReleaseStockStockNotFound() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .customerId("CUST-001")
                    .customerEmail("customer@example.com")
                    .build();

            StockReservationEntity reservation = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(10)
                    .status(ReservationStatus.RESERVED)
                    .build();

            when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                    .thenReturn(List.of(reservation));
            when(stockRepository.findByProductId(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> stockService.releaseStock(event))
                    .isInstanceOf(StockNotFoundException.class)
                    .hasMessageContaining("Stock not found");

            verify(stockRepository, never()).save(any());
            verify(stockReservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Fulfill Stock Tests")
    class FulfillStockTests {

        @Test
        @DisplayName("Should fulfill stock successfully")
        void testFulfillStockSuccess() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .build();

            StockReservationEntity reservation = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(10)
                    .status(ReservationStatus.RESERVED)
                    .build();

            when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                    .thenReturn(List.of(reservation));
            when(stockReservationRepository.save(any(StockReservationEntity.class)))
                    .thenReturn(reservation);

            // Act
            stockService.fulfillStock(event);

            // Assert
            verify(stockReservationRepository, times(1)).findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
            verify(stockReservationRepository, times(1)).save(argThat(r ->
                    r.getStatus() == ReservationStatus.FULFILLED
            ));
        }

        @Test
        @DisplayName("Should handle no reservations found gracefully")
        void testFulfillStockNoReservationsFound() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .build();

            when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                    .thenReturn(List.of());

            // Act
            stockService.fulfillStock(event);

            // Assert
            verify(stockReservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fulfill multiple reservations")
        void testFulfillStockMultipleItems() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(orderId)
                    .shipmentId(UUID.randomUUID())
                    .build();

            StockReservationEntity reservation1 = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId("PROD-001")
                    .quantity(10)
                    .status(ReservationStatus.RESERVED)
                    .build();

            StockReservationEntity reservation2 = StockReservationEntity.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .productId("PROD-002")
                    .quantity(20)
                    .status(ReservationStatus.RESERVED)
                    .build();

            when(stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                    .thenReturn(List.of(reservation1, reservation2));
            when(stockReservationRepository.save(any(StockReservationEntity.class)))
                    .thenReturn(reservation1);

            // Act
            stockService.fulfillStock(event);

            // Assert
            verify(stockReservationRepository, times(2)).save(any(StockReservationEntity.class));
        }
    }
}

