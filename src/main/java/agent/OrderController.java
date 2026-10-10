package agent;

import agent.dto.OrderRequest;
import agent.dto.OrderResponse;
import agent.dto.PaymentRequest;
import agent.domain.Order;
import agent.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        log.info("REST API: Getting order: {}", orderId);
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("REST API: Creating order for customer: {}", request.getCustomerId());
        String orderId = orderService.createOrder(request.getCustomerId(), request.getTotalAmount());
        Order order = orderRepository.findById(orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId) {
        log.info("REST API: Cancelling order: {}", orderId);
        String result = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{orderId}/payment")
    public ResponseEntity<String> processPayment(@PathVariable String orderId, 
                                                   @Valid @RequestBody PaymentRequest request) {
        log.info("REST API: Processing payment for order: {}", orderId);
        String result = paymentService.processPayment(orderId, request.getAmount());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("REST API: Getting all orders");
        List<Order> orders = orderRepository.findAll();
        List<OrderResponse> responses = orders.stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getCustomerOrders(@PathVariable String customerId) {
        log.info("REST API: Getting orders for customer: {}", customerId);
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        List<OrderResponse> responses = orders.stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}