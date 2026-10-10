# Agent MCP Server

Spring Boot 기반 MCP Server 프로젝트입니다.

## Overview

이 프로젝트는 주문 관련 기능을 MCP Tool로 제공하고, 실제 데이터는 Redis에서 관리합니다.

```text
AI Agent Client :8081
        |
        | MCP / Streamable HTTP
        v
MCP Server :8080
        |
        +-- OrderController (REST API)
        +-- OrderService (MCP Tool)
        +-- CustomerOrderService (MCP Tool)
        +-- PaymentService (MCP Tool)
        |
        +-- OrderRepository
        |
        v
Redis :6379
```

## Technology

* Java 17
* Spring Boot 4.1.1
* Spring AI 2.0.x
* Spring Web MVC
* Spring AI MCP Server
* Spring Data Redis
* Spring Validation
* Lombok
* SpringDoc OpenAPI (Swagger)
* Redis 7
* Docker
* JUnit 5 + Mockito

## Configuration

`application.properties`

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

## Running Redis

```powershell
docker run -d --name redis-agent -p 6379:6379 redis:7
```

Check Redis:

```powershell
docker exec redis-agent redis-cli ping
```

Expected:

```text
PONG
```

## Running the MCP Server

```powershell
mvn spring-boot:run
```

Server:

```text
http://localhost:8080
```

## Redis Data Model

Orders are stored as Redis Hashes.

```text
order:ORD-1001
  customerId=CUST-001
  status=FILLED
  paymentStatus=PAID

order:ORD-1002
  customerId=CUST-001
  status=PENDING
  paymentStatus=PAYMENT_PENDING

order:ORD-1003
  customerId=CUST-002
  status=FAILED
  paymentStatus=REFUNDED
```

Customer-to-order relationships are stored as Redis Sets.

```text
customer:CUST-001:orders
  ORD-1001
  ORD-1002

customer:CUST-002:orders
  ORD-1003
```

## Architecture

### Layered Architecture

- **Controller Layer**: REST API endpoints for direct HTTP access
- **Service Layer**: Business logic with MCP Tool annotations
- **Repository Layer**: Data access layer for Redis operations
- **Domain Layer**: Domain models (Order, OrderStatus, PaymentStatus)
- **DTO Layer**: Data Transfer Objects for API requests/responses
- **Exception Layer**: Custom exceptions and global error handling

### MCP Tools

#### OrderService

- `getOrderStatus(orderId)`: Get the current order status
- `getOrders(status)`: Get all orders, optionally filtered by status
- `cancelOrder(orderId)`: Cancel an order (only PENDING orders)
- `createOrder(customerId, totalAmount)`: Create a new order

#### CustomerOrderService

- `getCustomerOrders(customerId)`: Get all orders for a customer

#### PaymentService

- `getPaymentStatus(orderId)`: Get payment status of an order
- `processPayment(orderId, amount)`: Process payment for an order

## MCP Tool Registration

The tools are registered in `AgentApplication`:

```java
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
```

Methods annotated with `@Tool` are exposed as MCP tools.

## Architecture

The MCP Server does not communicate directly with the LLM.

```text
Ollama / Qwen3
      ^
      | LLM request / response
      |
AI Agent Client :8081
      |
      | MCP
      v
MCP Server :8080
      |
      v
Java Service / Business Logic
      |
      v
Redis :6379
```

The LLM receives the available tool definitions.

It does not receive or execute the Java implementation.

The MCP Server executes the Java method, and the Java service accesses Redis.

## Example: Cancel Order

User:

```text
Cancel order ORD-1002
```

Flow:

```text
Qwen3
  |
  | selects cancelOrder("ORD-1002")
  v
MCP Client
  |
  | MCP
  v
MCP Server
  |
  v
OrderService.cancelOrder()
  |
  v
Redis
  |
  +-- status = PENDING
  |
  +-- status = CANCELLED
```

The LLM does not directly modify Redis.

The business rule is implemented in Java:

```java
if (!"PENDING".equals(status)) {
    return "ORDER_CANNOT_BE_CANCELLED";
}
```

This keeps business rules and data access inside the backend.

## Production-Ready Features

### Error Handling
- Custom exceptions for different error scenarios
- Global exception handler with proper HTTP status codes
- Structured error responses with timestamps

### Validation
- Input validation using Jakarta Validation
- `@Valid` annotation on request bodies
- Custom validation error messages

### Logging
- SLF4J logging throughout the application
- Structured logs with contextual information
- Different log levels (INFO, WARN, ERROR)

### Testing
- Unit tests for service layer
- Mockito for mocking dependencies
- JUnit 5 for test framework

### Code Quality
- Lombok for reducing boilerplate
- Layered architecture for separation of concerns
- Repository pattern for data access
- DTO pattern for API contracts

## Example: Multi-Tool Workflow

A user can ask:

```text
What are the orders for CUST-001
and what is the payment status of each order?
```

The Agent can perform:

```text
getCustomerOrders("CUST-001")
        |
        +-- ORD-1001
        +-- ORD-1002
                |
                v
getPaymentStatus("ORD-1001")
getPaymentStatus("ORD-1002")
                |
                v
          Qwen3 summarizes
```

Example result:

```text
ORD-1001: PAID
ORD-1002: PAYMENT_PENDING
```

## Current MCP Tools

```text
getOrderStatus(orderId)
getOrders(status)
cancelOrder(orderId)
createOrder(customerId, totalAmount)
getCustomerOrders(customerId)
getPaymentStatus(orderId)
processPayment(orderId, amount)
```

## REST API Endpoints

### Order Management

- `GET /orders/{orderId}` - Get order details
- `POST /orders` - Create a new order
- `POST /orders/{orderId}/cancel` - Cancel an order
- `POST /orders/{orderId}/payment` - Process payment
- `GET /orders` - Get all orders
- `GET /orders/customer/{customerId}` - Get customer orders

### API Documentation

Swagger UI is available at:
```text
http://localhost:8080/swagger-ui.html
```

OpenAPI spec:
```text
http://localhost:8080/v3/api-docs
```
