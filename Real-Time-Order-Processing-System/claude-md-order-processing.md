# Claude.md - Real-time Order Processing System (IntelliJ Integrated)

## Project Overview
Building a distributed order processing system using IntelliJ IDEA Ultimate's integrated tools for Docker, Kafka, databases, and microservices management. This leverages IntelliJ's Services window, Docker integration, Database tools, and Kafka plugin for streamlined development.

## Architecture Summary
- **Order Service**: Main orchestrator, REST + gRPC endpoints
- **Inventory Service**: Stock management, event-driven updates  
- **Payment Service**: Payment processing with circuit breakers
- **Notification Service**: Event consumer for customer notifications

## Tech Stack with IntelliJ Integration
- Java 17 + Spring Boot 3.2.x (IntelliJ Spring Initializr)
- Apache Kafka (IntelliJ Kafka Plugin)
- gRPC (IntelliJ Protocol Buffer Plugin)
- PostgreSQL (IntelliJ Database Tools)
- Docker (IntelliJ Docker Integration)
- Redis (IntelliJ Redis Plugin)
- HTTP Client (IntelliJ .http files for testing)

## IntelliJ Setup Prerequisites
Ensure these IntelliJ plugins are installed:
- Docker
- Kafka
- Database Tools and SQL
- Protocol Buffers
- Spring Boot Assistant
- Lombok
- EnvFile
- HTTP Client

## Project Structure
```
order-processing-system/
├── .idea/
│   ├── runConfigurations/    # IntelliJ run configs
│   └── httpRequests/          # HTTP client test files
├── .run/                      # Shared run configurations
├── docker/
│   └── docker-compose.yml     # Managed via IntelliJ Services
├── services/
│   ├── order-service/
│   ├── inventory-service/
│   ├── payment-service/
│   └── notification-service/
├── shared/
│   ├── proto/                 # IntelliJ Proto support
│   └── common/
├── http/                      # IntelliJ HTTP Client files
│   ├── order-api.http
│   └── test-scenarios.http
└── README.md
```

## Development Timeline (56 hours)
- Setup & Infrastructure: 8 hours
- Core Services: 12 hours  
- Kafka Integration: 10 hours
- gRPC Implementation: 8 hours
- Patterns (Outbox/Saga): 6 hours
- Testing & Documentation: 8 hours
- Buffer: 4 hours

## Phase 1: IntelliJ Environment Setup (Hours 1-8)

### 1.1 Create Project Using IntelliJ Spring Initializr
1. File → New → Project → Spring Initializr
2. Project Settings:
   - Type: Maven
   - Language: Java
   - Group: `com.orderprocessing`
   - Artifact: `order-processing-system`
   - Java: 17
   - Packaging: Jar

3. Dependencies to add in Spring Initializr:
   - Spring Web
   - Spring Data JPA
   - PostgreSQL Driver
   - Spring for Apache Kafka
   - Spring Boot Actuator
   - Lombok
   - Spring Boot DevTools

### 1.2 Docker Compose with IntelliJ Services
Create `docker/docker-compose.yml` and manage via IntelliJ Services window:

```yaml
version: '3.8'

services:
  # PostgreSQL - Connect via IntelliJ Database Tool
  postgres:
    image: postgres:15-alpine
    container_name: order-postgres
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: password
      POSTGRES_DB: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Kafka with KRaft mode (no Zookeeper needed)
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: order-kafka
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092'
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://kafka:29092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:9092'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
      CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'

  # Redis - Connect via IntelliJ Redis Plugin
  redis:
    image: redis:7-alpine
    container_name: order-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

  # Jaeger for tracing - View in browser
  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: order-jaeger
    ports:
      - "16686:16686"  # UI
      - "14268:14268"  # HTTP collector
      - "4317:4317"    # OTLP gRPC
      - "4318:4318"    # OTLP HTTP

  # Prometheus - Configure in IntelliJ Services
  prometheus:
    image: prom/prometheus:latest
    container_name: order-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  # Grafana - Access via IntelliJ Services browser
  grafana:
    image: grafana/grafana:latest
    container_name: order-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_USERS_ALLOW_SIGN_UP: false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards

volumes:
  postgres_data:
  redis_data:
  prometheus_data:
  grafana_data:
```

### 1.3 IntelliJ Services Configuration

#### Configure Docker in Services Window:
1. View → Tool Windows → Services (Alt+8)
2. Add New Service → Docker → Docker Compose
3. Select `docker/docker-compose.yml`
4. Right-click → Run to start all containers

#### Configure Database Connection:
1. View → Tool Windows → Database
2. New → Data Source → PostgreSQL
3. Settings:
   - Host: localhost
   - Port: 5432
   - User: admin
   - Password: password
   - Database: postgres
4. Test Connection → OK

#### Configure Kafka in IntelliJ:
1. Install Kafka plugin from Marketplace
2. View → Tool Windows → Kafka
3. Add Connection:
   - Bootstrap Servers: localhost:9092
   - Name: Local Kafka
4. Create topics directly from UI

### 1.4 Create IntelliJ Run Configurations

Create `.run/Order Service.run.xml`:
```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Order Service" type="SpringBootApplicationConfigurationType" factoryName="Spring Boot">
    <module name="order-service" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="com.orderprocessing.order.OrderServiceApplication" />
    <option name="ACTIVE_PROFILES" value="local" />
    <option name="ENVIRONMENT_VARIABLES">
      <map>
        <entry key="SERVER_PORT" value="8081" />
        <entry key="SPRING_PROFILES_ACTIVE" value="local" />
      </map>
    </option>
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
```

Create Compound Configuration for all services:
1. Run → Edit Configurations
2. Add → Compound
3. Name: "All Services"
4. Add all service configurations

## Phase 2: Order Service Implementation (Hours 9-20)

### 2.1 Create Order Service Module
Using IntelliJ's module creation:
1. Right-click project → New → Module
2. Spring Initializr → Fill details
3. Add dependencies via IntelliJ's Maven tool window

### 2.2 Application Properties with IntelliJ EnvFile
Create `services/order-service/src/main/resources/application-local.yml`:

```yaml
server:
  port: ${SERVER_PORT:8081}
  
spring:
  application:
    name: order-service
    
  # DataSource - Viewable in IntelliJ Database tool
  datasource:
    url: jdbc:postgresql://localhost:5432/orderdb
    username: admin
    password: password
    hikari:
      maximum-pool-size: 10
      
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true  # See SQL in IntelliJ Run window
    
  # Kafka config - Monitor in Kafka tool window
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: order-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.orderprocessing.*"
        
  # Redis cache - View in Redis plugin
  data:
    redis:
      host: localhost
      port: 6379
      
# Actuator endpoints - Test in IntelliJ HTTP Client
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# OpenTelemetry
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4317
  resource:
    attributes:
      service.name: order-service

logging:
  level:
    com.orderprocessing: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### 2.3 Domain Model with IntelliJ Live Templates
Create custom Live Templates for common patterns:
1. Settings → Editor → Live Templates
2. Add template for Entity:

```java
@Entity
@Table(name = "$TABLE$")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class $CLASS$ {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Version
    private Long version;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    $END$
}
```

Actual Order entity:
```java
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String orderId;
    
    private String customerId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private BigDecimal totalAmount;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();
    
    @Version
    private Long version; // Optimistic locking
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Outbox pattern support
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OutboxEvent> outboxEvents = new ArrayList<>();
    
    public void addOutboxEvent(OutboxEvent event) {
        this.outboxEvents.add(event);
        event.setAggregateId(this.orderId);
    }
}
```

### 2.4 IntelliJ HTTP Client for Testing
Create `http/order-api.http`:

```http
### Create Order
POST http://localhost:8081/api/orders
Content-Type: application/json

{
  "customerId": "CUST-123",
  "items": [
    {
      "productId": "PROD-1",
      "quantity": 2,
      "price": 29.99
    },
    {
      "productId": "PROD-2", 
      "quantity": 1,
      "price": 49.99
    }
  ]
}

> {%
    client.test("Order created", function() {
        client.assert(response.status === 201);
        client.global.set("orderId", response.body.orderId);
    });
%}

### Get Order Status
GET http://localhost:8081/api/orders/{{orderId}}
Accept: application/json

### Get Order Health
GET http://localhost:8081/actuator/health
Accept: application/json

### Get Metrics
GET http://localhost:8081/actuator/metrics/http.server.requests
Accept: application/json
```

## Phase 3: Kafka Integration with IntelliJ Tools (Hours 21-28)

### 3.1 Create Topics Using IntelliJ Kafka Plugin
1. Open Kafka tool window
2. Right-click connection → Create Topic
3. Create topics:
   - `order-events` (3 partitions)
   - `inventory-events` (3 partitions)
   - `payment-events` (3 partitions)
   - `notification-events` (1 partition)

### 3.2 Kafka Configuration with IntelliJ Debugging
```java
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {
    
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configs.put(ProducerConfig.ACKS_CONFIG, "all");
        configs.put(ProducerConfig.RETRIES_CONFIG, 3);
        configs.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        // Enable debugging in IntelliJ console
        configs.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, 
                   "com.orderprocessing.order.config.LoggingProducerInterceptor");
        
        return new DefaultKafkaProducerFactory<>(configs);
    }
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service");
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        configs.put(JsonDeserializer.TRUSTED_PACKAGES, "com.orderprocessing.*");
        
        return new DefaultKafkaConsumerFactory<>(configs);
    }
}
```

### 3.3 Monitor Kafka Messages in IntelliJ
1. Kafka tool window → Topics → Select topic
2. Right-click → Consume Messages
3. Set breakpoints in @KafkaListener methods
4. Debug to see message flow

## Phase 4: gRPC with IntelliJ Protocol Buffer Support (Hours 29-36)

### 4.1 Configure Protocol Buffer Plugin
1. Install Protocol Buffer plugin
2. Settings → Build → Compiler → Protocol Buffers
3. Configure proto paths

### 4.2 Proto Files with IntelliJ Syntax Support
Create `shared/proto/services.proto`:

```protobuf
syntax = "proto3";

package com.orderprocessing.grpc;

option java_multiple_files = true;
option java_outer_classname = "ServicesProto";

// Order Service
service OrderService {
  rpc GetOrderStatus(OrderRequest) returns (OrderResponse);
  rpc StreamOrderUpdates(OrderRequest) returns (stream OrderUpdate);
}

// Inventory Service  
service InventoryService {
  rpc CheckInventory(InventoryCheckRequest) returns (InventoryCheckResponse);
  rpc ReserveInventory(ReserveRequest) returns (ReserveResponse);
}

// Payment Service
service PaymentService {
  rpc ProcessPayment(PaymentRequest) returns (PaymentResponse);
}

message OrderRequest {
  string order_id = 1;
}

message OrderResponse {
  string order_id = 1;
  string status = 2;
  int64 timestamp = 3;
}

message OrderUpdate {
  string order_id = 1;
  string status = 2;
  string message = 3;
  int64 timestamp = 4;
}
```

### 4.3 Maven Configuration for Proto Compilation
```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
    <configuration>
        <protocArtifact>com.google.protobuf:protoc:3.24.0:exe:${os.detected.classifier}</protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.59.0:exe:${os.detected.classifier}</pluginArtifact>
        <protoSourceRoot>${project.basedir}/src/main/proto</protoSourceRoot>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
                <goal>compile-custom</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Phase 5: Event Patterns Implementation (Hours 37-44)

### 5.1 Outbox Pattern with Database Viewer
Monitor outbox table in IntelliJ Database tool:

```java
@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String eventId;
    
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    private boolean published;
    private int retryCount;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime publishedAt;
}

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxProcessor {
    
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> unpublished = repository.findTop100ByPublishedFalseOrderByCreatedAt();
        
        for (OutboxEvent event : unpublished) {
            try {
                // Send to Kafka - monitor in Kafka tool window
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                    .addCallback(
                        success -> markAsPublished(event),
                        failure -> handleFailure(event, failure)
                    );
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getEventId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
            }
        }
    }
    
    private void markAsPublished(OutboxEvent event) {
        event.setPublished(true);
        event.setPublishedAt(LocalDateTime.now());
        repository.save(event);
        log.info("Published event: {} for aggregate: {}", event.getEventType(), event.getAggregateId());
    }
    
    private void handleFailure(OutboxEvent event, Throwable ex) {
        log.error("Failed to publish event: {}", event.getEventId(), ex);
        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() > 3) {
            // Move to DLQ or alert
            event.setPublished(true); // Mark as published to skip
        }
        repository.save(event);
    }
}
```

### 5.2 Saga Orchestrator with IntelliJ Debugger
Set breakpoints to trace saga flow:

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSagaOrchestrator {
    
    private final OrderService orderService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = "order-events", 
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Starting saga for order: {}", event.getOrderId());
        // Set breakpoint here to debug saga flow
        
        // Step 1: Reserve inventory
        InventoryReservationCommand command = InventoryReservationCommand.builder()
            .orderId(event.getOrderId())
            .items(event.getItems())
            .build();
            
        kafkaTemplate.send("inventory-commands", event.getOrderId(), command);
    }
    
    @KafkaListener(topics = "inventory-events")
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Inventory reserved for order: {}", event.getOrderId());
        
        // Step 2: Process payment
        PaymentCommand command = PaymentCommand.builder()
            .orderId(event.getOrderId())
            .amount(event.getTotalAmount())
            .customerId(event.getCustomerId())
            .build();
            
        kafkaTemplate.send("payment-commands", event.getOrderId(), command);
    }
    
    @KafkaListener(topics = "payment-events")
    public void handlePaymentResult(PaymentResultEvent event) {
        if (event.isSuccess()) {
            handlePaymentSuccess(event);
        } else {
            handlePaymentFailure(event);
        }
    }
    
    private void handlePaymentSuccess(PaymentResultEvent event) {
        log.info("Payment successful for order: {}", event.getOrderId());
        
        // Complete order
        orderService.completeOrder(event.getOrderId());
        
        // Notify customer
        NotificationCommand notification = NotificationCommand.builder()
            .orderId(event.getOrderId())
            .type("ORDER_COMPLETED")
            .message("Your order has been confirmed!")
            .build();
            
        kafkaTemplate.send("notification-commands", notification);
    }
    
    private void handlePaymentFailure(PaymentResultEvent event) {
        log.warn("Payment failed for order: {}, starting compensation", event.getOrderId());
        
        // Compensate: Release inventory
        ReleaseInventoryCommand command = ReleaseInventoryCommand.builder()
            .orderId(event.getOrderId())
            .reason("Payment failed")
            .build();
            
        kafkaTemplate.send("inventory-commands", event.getOrderId(), command);
        
        // Cancel order
        orderService.cancelOrder(event.getOrderId(), "Payment failed");
    }
}
```

## Phase 6: Testing & Monitoring (Hours 45-52)

### 6.1 Integration Tests with IntelliJ Test Runner
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
class OrderServiceIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Test
    @DisplayName("Should complete order saga successfully")
    void testCompleteOrderSaga() throws Exception {
        // Create order via REST
        String orderJson = """
            {
                "customerId": "CUST-123",
                "items": [
                    {"productId": "PROD-1", "quantity": 2, "price": 29.99}
                ]
            }
            """;
            
        MvcResult result = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isCreated())
                .andReturn();
                
        // Extract order ID
        String orderId = JsonPath.read(result.getResponse().getContentAsString(), "$.orderId");
        
        // Wait for saga to complete
        Thread.sleep(5000);
        
        // Verify final status
        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
```

### 6.2 IntelliJ Profiler Integration
1. Run → Run with Profiler
2. Monitor:
   - CPU usage per service
   - Memory allocation
   - Thread activity
   - Kafka consumer lag

### 6.3 Database Query Analysis
1. Database tool → Console
2. Enable query execution plan
3. Analyze slow queries
4. Add indexes as needed

## Phase 7: Documentation & Polish (Hours 53-56)

### 7.1 Generate API Documentation
Use IntelliJ's built-in tools:
1. Tools → Generate JavaDoc
2. Include in README

### 7.2 Create Architecture Diagram
Use IntelliJ's Diagrams:
1. Right-click package → Diagrams → Show Diagram
2. Export as image for README

### 7.3 Run Configuration Export
1. Run → Edit Configurations
2. Export all configurations
3. Share via `.run/` directory

## IntelliJ-Specific Tips & Shortcuts

### Essential Shortcuts for This Project:
- `Shift+Shift`: Search everywhere (find Kafka topics, Docker containers)
- `Ctrl+Shift+F10`: Run current file/test
- `Alt+8`: Open Services window (Docker, Spring Boot apps)
- `Alt+F12`: Open Terminal in project root
- `Ctrl+E`: Recent files
- `Ctrl+Shift+A`: Find Action (e.g., "Kafka", "Docker")

### Debugging Microservices:
1. Set up Compound configuration with all services
2. Use conditional breakpoints for specific order IDs
3. Enable "Suspend All Threads" for Kafka listeners
4. Use Evaluate Expression for runtime inspection

### Monitoring in IntelliJ:
1. Services window shows all running containers
2. Spring Boot dashboard shows service health
3. Kafka tool window shows topic lag
4. Database console shows query performance

### Performance Optimization:
1. Use IntelliJ Profiler to find bottlenecks
2. Analyze GC logs in Run window
3. Monitor thread dumps for deadlocks
4. Check Kafka consumer lag in tool window

## Quick Reference Commands

### IntelliJ Terminal Commands:
```bash
# Build all modules
./mvnw clean package

# Run specific service
./mvnw spring-boot:run -pl services/order-service

# Run tests with coverage
./mvnw test jacoco:report

# Check for dependency updates
./mvnw versions:display-dependency-updates
```

### Database Queries (Run in Database Console):
```sql
-- Check outbox events
SELECT * FROM outbox_events WHERE published = false;

-- Monitor order status
SELECT order_id, status, created_at FROM orders ORDER BY created_at DESC LIMIT 10;

-- Check saga state
SELECT * FROM saga_state WHERE status = 'IN_PROGRESS';
```

## Time Management with IntelliJ

### Use IntelliJ's Time Tracking:
1. Install Time Tracker plugin
2. Track time per module
3. Set reminders for phase transitions

### Feature Freeze Checklist (Hour 16):
- [ ] Core order flow working
- [ ] Kafka events publishing
- [ ] Basic saga implementation
- [ ] Health checks active
- [ ] Docker Compose runs cleanly

### Documentation Sprint (Hour 48):
- [ ] README with architecture diagram
- [ ] HTTP client test files
- [ ] JavaDoc for public APIs
- [ ] Run configurations exported

## Common IntelliJ Solutions

### Kafka Connection Issues:
1. Services → Docker → Check container health
2. Kafka tool → Refresh connection
3. Check bootstrap.servers in application.yml

### Database Connection Problems:
1. Database tool → Test Connection
2. Verify Docker container running
3. Check credentials in Data Source settings

### gRPC Compilation Errors:
1. Build → Rebuild Project
2. Maven → Reimport
3. Check proto file syntax highlighting

### Memory Issues with Multiple Services:
1. Help → Change Memory Settings
2. Set Xmx to 4GB minimum
3. Use Services window to stop unused services

---

## 📋 Order Service Implementation Checklist (Phase 2)

### **1. Project Setup & Configuration**
- [ ] Create Maven Spring Boot project (Java 17, Spring Boot 3.2.x)
- [ ] Add dependencies:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - Spring Kafka
  - Spring Boot Actuator
  - Lombok
  - Spring Boot Validation
  - Spring Boot DevTools
  - Micrometer (Prometheus)
- [ ] Configure `application.yml` (or `application-local.yml`):
  - Server port (8081)
  - PostgreSQL connection (orderdb)
  - Kafka bootstrap servers (localhost:9092)
  - Redis connection (optional for Phase 2)
  - Actuator endpoints
  - Logging configuration

### **2. Domain Model & Entities**
- [ ] **Order Entity** (orders table)
  - orderId (UUID, primary key)
  - customerId
  - status (enum: PENDING, CONFIRMED, PROCESSING, COMPLETED, FAILED, CANCELLED)
  - totalAmount
  - createdAt, updatedAt (audit fields)
  - version (for optimistic locking)
- [ ] **OrderItem Entity** (order_items table)
  - itemId (UUID)
  - orderId (foreign key)
  - productId
  - quantity
  - price
- [ ] **OutboxEvent Entity** (outbox_events table) - for transactional outbox pattern
  - eventId (UUID)
  - aggregateId (orderId)
  - aggregateType ("ORDER")
  - eventType (e.g., "order-created", "order-completed")
  - payload (JSON string)
  - published (boolean)
  - retryCount
  - createdAt, publishedAt

### **3. Repository Layer**
- [ ] OrderRepository (extends JpaRepository)
- [ ] OrderItemRepository
- [ ] OutboxEventRepository
  - Custom query: `findTop100ByPublishedFalseOrderByCreatedAt()`

### **4. Service Layer**
- [ ] **OrderService** - Core business logic
  - `createOrder(CreateOrderRequest)` → Order
  - `getOrder(String orderId)` → Order
  - `updateOrderStatus(String orderId, OrderStatus status)`
  - `cancelOrder(String orderId, String reason)`
  - `completeOrder(String orderId)`
- [ ] **OutboxProcessor** - Scheduled task to publish events
  - `@Scheduled(fixedDelay = 5000)` to poll unpublished events
  - Publish to Kafka
  - Mark as published on success

### **5. REST API (Controller Layer)**
- [ ] **OrderController** (`/api/orders`)
  - `POST /api/orders` - Create new order
  - `GET /api/orders/{orderId}` - Get order by ID
  - `GET /api/orders` - List orders (with pagination)
  - `GET /api/orders/customer/{customerId}` - Get customer orders
  - `PATCH /api/orders/{orderId}/cancel` - Cancel order
- [ ] Request/Response DTOs:
  - CreateOrderRequest
  - OrderResponse
  - OrderItemDTO
- [ ] Proper HTTP status codes (201, 200, 404, 400, etc.)
- [ ] Exception handling (@ControllerAdvice)

### **6. Kafka Integration**
- [ ] **Kafka Configuration**
  - ProducerFactory bean
  - ConsumerFactory bean
  - KafkaTemplate bean
  - Topic creation (order-events, order-commands)
- [ ] **Event Publishing** (via OutboxProcessor)
  - OrderCreatedEvent
  - OrderCompletedEvent
  - OrderCancelledEvent
  - OrderFailedEvent
- [ ] **Event Models** (POJOs for events)
  - Include: orderId, customerId, items, totalAmount, timestamp, status

### **7. Database Configuration**
- [ ] JPA configuration in application.yml
- [ ] Enable auditing (@EnableJpaAuditing)
- [ ] Database initialization (schema auto-creation for dev)
- [ ] Connection pooling (HikariCP - comes with Spring Boot)

### **8. Error Handling & Validation**
- [ ] Global exception handler (@RestControllerAdvice)
  - OrderNotFoundException
  - InvalidOrderStateException
  - ValidationException
- [ ] Request validation (@Valid, @NotNull, @Min, etc.)
- [ ] Standard error response format

### **9. Monitoring & Observability**
- [ ] Actuator endpoints configured:
  - `/actuator/health`
  - `/actuator/metrics`
  - `/actuator/prometheus`
- [ ] Custom metrics (optional):
  - Order creation counter
  - Order completion rate
  - Average order processing time
- [ ] Logging with correlation IDs

### **10. Testing**
- [ ] **Unit Tests**
  - OrderService test
  - Controller tests (with MockMvc)
- [ ] **Integration Tests**
  - Full order creation flow
  - Database interaction tests
  - Kafka event publishing tests (with @EmbeddedKafka or TestContainers)
- [ ] Test coverage > 70%

### **11. Documentation**
- [ ] README for order-service
  - API endpoints
  - How to run locally
  - Environment variables
- [ ] Swagger/OpenAPI documentation (optional but recommended)
  - Add `springdoc-openapi-starter-webmvc-ui` dependency
  - Access at `/swagger-ui.html`

### **12. Docker Support (Optional for Phase 2)**
- [ ] Dockerfile for order-service
- [ ] Add to main docker-compose.yml as a service

---

## 🎯 **Minimum Viable Product (MVP) for Phase 2:**
Focus on getting these working first:
1. ✅ Create Order API endpoint
2. ✅ Store order in PostgreSQL (orderdb)
3. ✅ Publish OrderCreatedEvent to Kafka (via Outbox pattern)
4. ✅ Get Order API endpoint
5. ✅ Basic health check working

---

## 📚 **Key Patterns to Implement (Resume Highlights):**
- **Transactional Outbox Pattern** - Guarantees event delivery
- **Optimistic Locking** - Prevent race conditions (@Version)
- **Audit Fields** - Track created/updated timestamps
- **RESTful API Design** - Proper HTTP methods and status codes
- **Exception Handling** - Centralized error responses

---

## 🚀 **Phase 2 Success Criteria:**
- [ ] Order Service runs on port 8081
- [ ] Can create orders via REST API
- [ ] Orders are persisted to PostgreSQL
- [ ] Events are published to Kafka topics
- [ ] Health endpoint returns UP status
- [ ] All unit tests pass
- [ ] Integration test demonstrates full flow