package agent;

import agent.domain.Order;
import agent.domain.OrderStatus;
import agent.domain.PaymentStatus;
import agent.exception.InvalidOrderStatusException;
import agent.exception.OrderNotFoundException;
import agent.exception.PaymentFailedException;
import agent.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;

    @Tool(description = "Get the payment status of an order by order ID")
    public String getPaymentStatus(String orderId) {
        log.info("Getting payment status for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            log.warn("Order not found: {}", orderId);
            throw new OrderNotFoundException(orderId);
        }

        log.info("Payment status retrieved: {} - {}", orderId, order.getPaymentStatus());
        return order.getPaymentStatus().name();
    }

    @Tool(description = "Process payment for an order by order ID and payment amount")
    public String processPayment(String orderId, BigDecimal amount) {
        log.info("Processing payment for order: {} with amount: {}", orderId, amount);
        
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            log.warn("Order not found for payment: {}", orderId);
            throw new OrderNotFoundException(orderId);
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.warn("Order {} already paid", orderId);
            throw new InvalidOrderStatusException("Order " + orderId + " is already paid");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Cannot process payment for cancelled order: {}", orderId);
            throw new InvalidOrderStatusException("Cannot process payment for cancelled order: " + orderId);
        }

        if (!amount.equals(order.getTotalAmount())) {
            log.warn("Payment amount mismatch. Expected: {}, Provided: {}", order.getTotalAmount(), amount);
            throw new PaymentFailedException(orderId, "Amount mismatch");
        }

        // Simulate payment processing
        orderRepository.updatePaymentStatus(orderId, PaymentStatus.PAID);
        orderRepository.updateStatus(orderId, OrderStatus.CONFIRMED);
        
        log.info("Payment processed successfully for order: {}", orderId);
        return "PAYMENT_SUCCESSFUL";
    }
}