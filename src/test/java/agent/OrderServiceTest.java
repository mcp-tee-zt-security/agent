package agent;

import agent.domain.Order;
import agent.domain.OrderStatus;
import agent.domain.PaymentStatus;
import agent.exception.OrderCannotBeCancelledException;
import agent.exception.OrderNotFoundException;
import agent.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .orderId("ORD-TEST001")
                .customerId("CUST-001")
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PAYMENT_PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getOrderStatus_WhenOrderExists_ReturnsStatus() {
        when(orderRepository.findById("ORD-TEST001")).thenReturn(testOrder);

        String status = orderService.getOrderStatus("ORD-TEST001");

        assertEquals("PENDING", status);
        verify(orderRepository).findById("ORD-TEST001");
    }

    @Test
    void getOrderStatus_WhenOrderNotFound_ReturnsNotFound() {
        when(orderRepository.findById("ORD-NOTFOUND")).thenReturn(null);

        String status = orderService.getOrderStatus("ORD-NOTFOUND");

        assertEquals("ORDER_NOT_FOUND", status);
    }

    @Test
    void cancelOrder_WhenOrderIsPending_CancelsOrder() {
        when(orderRepository.findById("ORD-TEST001")).thenReturn(testOrder);

        String result = orderService.cancelOrder("ORD-TEST001");

        assertEquals("ORDER_CANCELLED", result);
        verify(orderRepository).updateStatus("ORD-TEST001", OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_WhenOrderNotFound_ThrowsException() {
        when(orderRepository.findById("ORD-NOTFOUND")).thenReturn(null);

        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder("ORD-NOTFOUND"));
    }

    @Test
    void cancelOrder_WhenOrderNotPending_ThrowsException() {
        testOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById("ORD-TEST001")).thenReturn(testOrder);

        assertThrows(OrderCannotBeCancelledException.class, () -> orderService.cancelOrder("ORD-TEST001"));
        verify(orderRepository, never()).updateStatus(any(), any());
    }

    @Test
    void getOrders_WithoutFilter_ReturnsAllOrders() {
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        String result = orderService.getOrders(null);

        assertTrue(result.contains("ORD-TEST001"));
        assertTrue(result.contains("PENDING"));
    }

    @Test
    void getOrders_WithStatusFilter_ReturnsFilteredOrders() {
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(orders);

        String result = orderService.getOrders("PENDING");

        assertTrue(result.contains("ORD-TEST001"));
        verify(orderRepository).findByStatus(OrderStatus.PENDING);
    }

    @Test
    void getOrders_WhenNoOrdersFound_ReturnsNotFound() {
        when(orderRepository.findAll()).thenReturn(List.of());

        String result = orderService.getOrders(null);

        assertEquals("NO_ORDERS_FOUND", result);
    }

    @Test
    void createOrder_CreatesOrderSuccessfully() {
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        String orderId = orderService.createOrder("CUST-001", new BigDecimal("100.00"));

        assertNotNull(orderId);
        assertTrue(orderId.startsWith("ORD-"));
        verify(orderRepository).save(any(Order.class));
    }
}
