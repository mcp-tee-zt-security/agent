package agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final StringRedisTemplate redisTemplate;

    public PaymentService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Tool(description = "Get the payment status of an order by order ID")
    public String getPaymentStatus(String orderId) {

        String paymentStatus = (String) redisTemplate
                .opsForHash()
                .get("order:" + orderId, "paymentStatus");

        return paymentStatus != null
                ? paymentStatus
                : "PAYMENT_NOT_FOUND";
    }
}