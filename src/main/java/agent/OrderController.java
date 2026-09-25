package agent;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public String getOrderStatus(@PathVariable String orderId) {
        return orderService.getOrderStatus(orderId);
    }
}