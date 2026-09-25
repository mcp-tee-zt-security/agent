package agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final StringRedisTemplate redisTemplate;

    public OrderService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Tool(description = "Get the current status of an order by order ID")
    public String getOrderStatus(String orderId) {

        String status = (String) redisTemplate
                .opsForHash()
                .get("order:" + orderId, "status");

        return status != null ? status : "ORDER_NOT_FOUND";
    }

    @Tool(description = "Cancel an order by order ID. Only PENDING orders can be cancelled.")
    public String cancelOrder(String orderId) {

        String key = "order:" + orderId;

        String status = (String) redisTemplate
                .opsForHash()
                .get(key, "status");

        if (status == null) {
            return "ORDER_NOT_FOUND";
        }

        if (!"PENDING".equals(status)) {
            return "ORDER_CANNOT_BE_CANCELLED";
        }

        redisTemplate.opsForHash()
                .put(key, "status", "CANCELLED");

        return "ORDER_CANCELLED";
    }

    @Tool(description = "Get all orders, optionally filtered by order status")
    public String getOrders(String status) {

        Set<String> keys = redisTemplate.keys("order:*");

        if (keys == null || keys.isEmpty()) {
            return "NO_ORDERS_FOUND";
        }

        List<String> orders = new ArrayList<>();

        for (String key : keys) {

            String orderStatus = (String) redisTemplate
                    .opsForHash()
                    .get(key, "status");

            if (status == null || status.equalsIgnoreCase(orderStatus)) {
                String orderId = key.substring("order:".length());

                orders.add(orderId + ": " + orderStatus);
            }
        }

        if (orders.isEmpty()) {
            return "NO_ORDERS_FOUND";
        }

        return String.join(", ", orders);
    }
}