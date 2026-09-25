package agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CustomerOrderService {

    private final StringRedisTemplate redisTemplate;

    public CustomerOrderService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Tool(description = "Get all orders for a customer by customer ID")
    public String getCustomerOrders(String customerId) {

        Set<String> orders = redisTemplate
                .opsForSet()
                .members("customer:" + customerId + ":orders");

        if (orders == null || orders.isEmpty()) {
            return "NO_ORDERS_FOUND";
        }

        return String.join(", ", orders);
    }
}