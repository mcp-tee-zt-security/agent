package agent;

import agent.domain.Order;
import agent.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final OrderRepository orderRepository;

    @Tool(description = "Get all orders for a customer by customer ID")
    public String getCustomerOrders(String customerId) {
        log.info("Getting orders for customer: {}", customerId);
        
        List<Order> orders = orderRepository.findByCustomerId(customerId);

        if (orders.isEmpty()) {
            log.info("No orders found for customer: {}", customerId);
            return "NO_ORDERS_FOUND";
        }

        String result = orders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.joining(", "));
        
        log.info("Retrieved {} orders for customer: {}", orders.size(), customerId);
        return result;
    }
}