package agent.exception;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String orderId, String reason) {
        super("Payment failed for order " + orderId + ": " + reason);
    }
}
