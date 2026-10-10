package agent;

import agent.domain.Order;
import agent.domain.OrderStatus;
import agent.domain.PaymentStatus;
import agent.exception.OrderCannotBeCancelledException;
import agent.exception.OrderNotFoundException;
import agent.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Tool(description = "Get the current status of an order by order ID")
    public String getOrderStatus(String orderId) {
        log.info("Getting order status for orderId: {}", orderId);
        
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            log.warn("Order not found: {}", orderId);
            return "ORDER_NOT_FOUND";
        }
        
        log.info("Order status retrieved: {} - {}", orderId, order.getStatus());
        return order.getStatus().name();
    }

    @Tool(description = "Cancel an order by order ID. Only PENDING orders can be cancelled.")
    public String cancelOrder(String orderId) {
        log.info("Attempting to cancel order: {}", orderId);
        
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            log.warn("Order not found for cancellation: {}", orderId);
            throw new OrderNotFoundException(orderId);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} cannot be cancelled. Current status: {}", orderId, order.getStatus());
            throw new OrderCannotBeCancelledException(orderId, order.getStatus().name());
        }

        orderRepository.updateStatus(orderId, OrderStatus.CANCELLED);
        log.info("Order cancelled successfully: {}", orderId);
        return "ORDER_CANCELLED";
    }

    @Tool(description = "Get all orders, optionally filtered by order status")
    public String getOrders(String status) {
        log.info("Getting orders with status filter: {}", status);
        
        List<Order> orders;
        if (status == null || status.isEmpty()) {
            orders = orderRepository.findAll();
        } else {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatus(orderStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", status);
                return "INVALID_ORDER_STATUS";
            }
        }

        if (orders.isEmpty()) {
            log.info("No orders found");
            return "NO_ORDERS_FOUND";
        }

        String result = orders.stream()
                .map(order -> order.getOrderId() + ": " + order.getStatus().name())
                .collect(Collectors.joining(", "));
        
        log.info("Retrieved {} orders", orders.size());
        return result;
    }

    @Tool(description = "Create a new order with customer ID and total amount")
    public String createOrder(String customerId, BigDecimal totalAmount) {
        log.info("Creating new order for customer: {} with amount: {}", customerId, totalAmount);
        
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Order order = Order.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PAYMENT_PENDING)
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        orderRepository.save(order);
        log.info("Order created successfully: {}", orderId);
        return orderId;
    }
}