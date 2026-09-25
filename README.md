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
        +-- OrderService
        +-- CustomerOrderService
        +-- PaymentService
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
* Redis 7
* Docker

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

## MCP Tools

### OrderService

#### getOrderStatus

```text
getOrderStatus(orderId)
```

Returns the current order status from Redis.

Example:

```text
getOrderStatus("ORD-1001")
        |
        v
Redis: order:ORD-1001
        |
        v
FILLED
```

#### getOrders

```text
getOrders(status)
```

Returns orders filtered by status.

Example:

```text
getOrders("PENDING")
```

Result:

```text
ORD-1002: PENDING
```

#### cancelOrder

```text
cancelOrder(orderId)
```

Cancels an order only when the current status is `PENDING`.

```text
PENDING -> CANCELLED
```

Other order statuses cannot be cancelled.

### CustomerOrderService

```text
getCustomerOrders(customerId)
```

Returns all orders belonging to a customer.

Example:

```text
getCustomerOrders("CUST-001")
```

Result:

```text
ORD-1001
ORD-1002
```

### PaymentService

```text
getPaymentStatus(orderId)
```

Returns the payment status of an order.

Example:

```text
getPaymentStatus("ORD-1001")
```

Result:

```text
PAID
```

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
getCustomerOrders(customerId)
getPaymentStatus(orderId)
```
