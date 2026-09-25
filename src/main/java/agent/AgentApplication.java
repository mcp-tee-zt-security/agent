package agent;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class  AgentApplication {

	public static void main(String[] args) {
		SpringApplication.run( AgentApplication.class, args);
	}

	@Bean
	public MethodToolCallbackProvider orderTools(
			OrderService orderService,
			CustomerOrderService customerOrderService,
			PaymentService paymentService) {

		return MethodToolCallbackProvider.builder()
				.toolObjects(
						orderService,
						customerOrderService,
						paymentService)
				.build();
	}
}