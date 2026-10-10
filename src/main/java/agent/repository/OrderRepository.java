package agent.repository;

import agent.domain.Order;
import agent.domain.OrderStatus;
import agent.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private static final String ORDER_KEY_PREFIX = "order:";
    private static final String CUSTOMER_ORDERS_KEY_PREFIX = "customer:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StringRedisTemplate redisTemplate;

    public Order save(Order order) {
        String key = ORDER_KEY_PREFIX + order.getOrderId();
        
        Map<String, String> orderData = Map.of(
            "customerId", order.getCustomerId(),
            "status", order.getStatus().name(),
            "paymentStatus", order.getPaymentStatus().name(),
            "totalAmount", order.getTotalAmount().toString(),
            "createdAt", order.getCreatedAt().format(FORMATTER),
            "updatedAt", order.getUpdatedAt().format(FORMATTER)
        );

        redisTemplate.opsForHash().putAll(key, orderData);
        
        // Add to customer's order set
        String customerOrdersKey = CUSTOMER_ORDERS_KEY_PREFIX + order.getCustomerId() + ":orders";
        redisTemplate.opsForSet().add(customerOrdersKey, order.getOrderId());
        
        log.info("Order saved: {}", order.getOrderId());
        return order;
    }

    public Order findById(String orderId) {
        String key = ORDER_KEY_PREFIX + orderId;
        Map<Object, Object> orderData = redisTemplate.opsForHash().entries(key);

        if (orderData.isEmpty()) {
            return null;
        }

        try {
            String customerId = (String) orderData.get("customerId");
            String statusStr = (String) orderData.get("status");
            String paymentStatusStr = (String) orderData.get("paymentStatus");
            String totalAmountStr = (String) orderData.get("totalAmount");
            String createdAtStr = (String) orderData.get("createdAt");
            String updatedAtStr = (String) orderData.get("updatedAt");

            // Handle null values with defaults
            if (statusStr == null) statusStr = "PENDING";
            if (paymentStatusStr == null) paymentStatusStr = "PAYMENT_PENDING";
            if (totalAmountStr == null) totalAmountStr = "0.00";
            if (createdAtStr == null) createdAtStr = LocalDateTime.now().format(FORMATTER);
            if (updatedAtStr == null) updatedAtStr = LocalDateTime.now().format(FORMATTER);

            return Order.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .status(OrderStatus.valueOf(statusStr))
                    .paymentStatus(PaymentStatus.valueOf(paymentStatusStr))
                    .totalAmount(new BigDecimal(totalAmountStr))
                    .createdAt(LocalDateTime.parse(createdAtStr, FORMATTER))
                    .updatedAt(LocalDateTime.parse(updatedAtStr, FORMATTER))
                    .build();
        } catch (Exception e) {
            log.error("Error parsing order data for orderId: {}", orderId, e);
            return null;
        }
    }

    public List<Order> findByStatus(OrderStatus status) {
        Set<String> keys = redisTemplate.keys(ORDER_KEY_PREFIX + "*");
        List<Order> orders = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {
                String orderStatus = (String) redisTemplate.opsForHash().get(key, "status");
                if (status.name().equals(orderStatus)) {
                    String orderId = key.substring(ORDER_KEY_PREFIX.length());
                    Order order = findById(orderId);
                    if (order != null) {
                        orders.add(order);
                    }
                }
            }
        }

        return orders;
    }

    public List<Order> findAll() {
        Set<String> keys = redisTemplate.keys(ORDER_KEY_PREFIX + "*");
        List<Order> orders = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {
                String orderId = key.substring(ORDER_KEY_PREFIX.length());
                Order order = findById(orderId);
                if (order != null) {
                    orders.add(order);
                }
            }
        }

        return orders;
    }

    public List<Order> findByCustomerId(String customerId) {
        String customerOrdersKey = CUSTOMER_ORDERS_KEY_PREFIX + customerId + ":orders";
        Set<String> orderIds = redisTemplate.opsForSet().members(customerOrdersKey);
        List<Order> orders = new ArrayList<>();

        if (orderIds != null) {
            for (String orderId : orderIds) {
                Order order = findById(orderId);
                if (order != null) {
                    orders.add(order);
                }
            }
        }

        return orders;
    }

    public void updateStatus(String orderId, OrderStatus status) {
        String key = ORDER_KEY_PREFIX + orderId;
        redisTemplate.opsForHash().put(key, "status", status.name());
        redisTemplate.opsForHash().put(key, "updatedAt", LocalDateTime.now().format(FORMATTER));
        log.info("Order status updated: {} -> {}", orderId, status);
    }

    public void updatePaymentStatus(String orderId, PaymentStatus paymentStatus) {
        String key = ORDER_KEY_PREFIX + orderId;
        redisTemplate.opsForHash().put(key, "paymentStatus", paymentStatus.name());
        redisTemplate.opsForHash().put(key, "updatedAt", LocalDateTime.now().format(FORMATTER));
        log.info("Payment status updated: {} -> {}", orderId, paymentStatus);
    }

    public boolean existsById(String orderId) {
        String key = ORDER_KEY_PREFIX + orderId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void deleteById(String orderId) {
        String key = ORDER_KEY_PREFIX + orderId;
        Order order = findById(orderId);
        
        if (order != null) {
            // Remove from customer's order set
            String customerOrdersKey = CUSTOMER_ORDERS_KEY_PREFIX + order.getCustomerId() + ":orders";
            redisTemplate.opsForSet().remove(customerOrdersKey, orderId);
        }
        
        redisTemplate.delete(key);
        log.info("Order deleted: {}", orderId);
    }
}
