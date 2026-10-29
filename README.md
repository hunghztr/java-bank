# Bank Microservices

Khung hệ thống ngân hàng microservice: Java 21, Spring Boot 3.3, Spring Cloud Gateway, Kafka, PostgreSQL, Flyway.
Luồng mẫu: **chuyển tiền giữa 2 tài khoản** bằng saga + transactional outbox.

## Cấu trúc

```
bank-microservices/
├── pom.xml                       # parent POM (Maven đa module)
├── libs/
│   ├── common-core/              # BusinessException, ErrorResponse, GlobalExceptionHandler
│   ├── common-events/            # hợp đồng message: command / reply / event, Topics, MessageCodec
│   └── common-messaging/         # outbox (writer + relay), DLT error handler, khai báo topic
├── platform/
│   └── api-gateway/              # :8080, route /api/accounts → 8081, /api/transfers → 8082
├── services/
│   ├── account-service/          # :8081 tài khoản, số dư; xử lý lệnh trừ / cộng / hoàn tiền
│   ├── transaction-service/      # :8082 hexagonal: domain / application (port, saga) / adapter
│   └── notification-service/     # :8083 nghe sự kiện kết thúc giao dịch
└── infrastructure/docker/        # docker-compose: Postgres, Kafka (KRaft), Kafka UI
```

## Luồng chuyển tiền (saga điều phối)

```
Client ──POST /api/transfers (Idempotency-Key)──▶ transaction-service
   transfers(PENDING) + outbox[DebitAccount]          (cùng 1 transaction DB)
        │ OutboxRelay ──▶ bank.account.commands
        ▼
   account-service: khoá dòng, trừ tiền nguồn, outbox[AccountDebited | AccountDebitFailed]
        │ ──▶ bank.account.replies
        ▼
   TransferSaga: DEBITED → gửi CreditAccount
        ├─ AccountCredited      → COMPLETED    → bank.transfer.events: TransferCompleted
        └─ AccountCreditFailed  → COMPENSATING → RefundAccount → AccountRefunded
                                → FAILED       → bank.transfer.events: TransferFailed
```

Các điểm đảm bảo an toàn tiền:
- **Transactional outbox**: dữ liệu nghiệp vụ và message được ghi trong cùng transaction, relay đẩy lên Kafka sau.
- **Idempotency**:
  - API: header `Idempotency-Key` (unique trong DB).
  - account-service: bảng `processed_messages`.
  - Saga: bỏ qua reply không đúng trạng thái đang chờ.
- **Khoá**: pessimistic lock khi đổi số dư; `@Version` cho giao dịch.
- **Lỗi consumer**: thử lại 3 lần, sau đó chuyển sang topic `*.DLT`.
- **Tiền**: dùng `BigDecimal` / `NUMERIC(19,2)`, có ràng buộc `balance >= 0`.

## Chạy local

Các service không gọi HTTP trực tiếp cho nhau mà trao đổi qua Kafka: message được giữ lại trong topic, service
nào bật sau vẫn nhận đủ. Vì vậy chỉ cần **PostgreSQL và Kafka chạy trước**, còn các service bật theo thứ tự nào
cũng được (thứ tự khuyến nghị ở bước 3).

### Bước 0: Chuẩn bị (một lần)

Cần JDK 21, Maven 3.9 và Docker (kèm Docker Compose v2).

```bash
# Ubuntu
sudo apt install docker.io docker-compose-v2
sudo usermod -aG docker $USER   # sau đó đăng xuất / đăng nhập lại

java -version   # 21.x
mvn -v          # 3.9.x
```

### Bước 1: Khởi động hạ tầng

```bash
docker compose -f infrastructure/docker/docker-compose.yml up -d
docker compose -f infrastructure/docker/docker-compose.yml ps   # đợi postgres, kafka ở trạng thái running
```

- Lần đầu PostgreSQL tự tạo 2 database `accountdb` và `transactiondb` (`infrastructure/docker/postgres/init.sql`).
- Kafka UI: http://localhost:8090.

### Bước 2: Build

```bash
mvn clean install -DskipTests
```

Bước này bắt buộc: nó cài các thư viện `libs/*` vào Maven local. Bỏ qua thì `spring-boot:run` từng service
sẽ báo không tìm thấy `common-core` / `common-messaging`.

### Bước 3: Chạy các service (mỗi service một terminal)

| # | Service | Lệnh | Cổng | Lý do thứ tự |
|---|---|---|---|---|
| 1 | account-service | `mvn -pl services/account-service spring-boot:run` | 8081 | Flyway tạo bảng, seed tài khoản demo, tạo topic Kafka |
| 2 | transaction-service | `mvn -pl services/transaction-service spring-boot:run` | 8082 | Nhận lệnh chuyển tiền, chạy saga |
| 3 | notification-service | `mvn -pl services/notification-service spring-boot:run` | 8083 | Chỉ nghe sự kiện, bật lúc nào cũng được |
| 4 | api-gateway | `mvn -pl platform/api-gateway spring-boot:run` | 8080 | Cửa vào cho client (gọi thẳng 8081/8082 thì không cần) |

Service đã sẵn sàng khi log có dòng `Started ...Application in ... seconds`. Kiểm tra nhanh:

```bash
curl localhost:8081/actuator/health   # {"status":"UP"}
curl localhost:8082/actuator/health
curl localhost:8083/actuator/health
curl localhost:8080/actuator/health
```

IntelliJ: mở thư mục `bank-microservices` dưới dạng dự án Maven, chạy các class `*Application` theo thứ tự trên
(có thể gom vào tab **Services** để chạy cùng lúc).

### Biến môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | `bank` / `bank` | Tài khoản DB (mặc định chỉ dùng cho local) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Địa chỉ Kafka |
| `ACCOUNT_SERVICE_URL` | `http://localhost:8081` | Gateway route tới account-service |
| `TRANSACTION_SERVICE_URL` | `http://localhost:8082` | Gateway route tới transaction-service |

### Dừng

```bash
# Service: Ctrl + C ở từng terminal
docker compose -f infrastructure/docker/docker-compose.yml down      # dừng hạ tầng, giữ dữ liệu
docker compose -f infrastructure/docker/docker-compose.yml down -v   # dừng và xoá dữ liệu DB
```

### Lỗi thường gặp

| Lỗi | Cách xử lý |
|---|---|
| `Could not resolve dependencies ... common-core` | Chưa chạy `mvn clean install` ở thư mục gốc (bước 2) |
| `Connection to localhost:5432 refused` | PostgreSQL chưa chạy hoặc chưa sẵn sàng; xem `docker compose ... ps` |
| `database "accountdb" does not exist` | Volume DB tạo trước khi có `init.sql`: chạy `down -v` rồi `up -d` lại |
| Log lặp `Connection to node -1 could not be established` | Kafka chưa chạy, hoặc sai `KAFKA_BOOTSTRAP_SERVERS` |
| `Port 808x already in use` | Một service cũ vẫn đang chạy; tắt nó hoặc đổi `server.port` |
| Giao dịch đứng mãi ở `PENDING` | account-service chưa chạy; bật lên là message tồn trong Kafka được xử lý tiếp |

## Thử nhanh

Flyway seed 2 tài khoản: `1000000001` (10.000.000 VND) và `1000000002` (500.000 VND).

```bash
# Chuyển tiền
curl -s -X POST localhost:8080/api/transfers \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-0001' \
  -d '{"fromAccount":"1000000001","toAccount":"1000000002","amount":150000}'
# → 202 {"id":"...","status":"PENDING",...}

curl -s localhost:8080/api/transfers/<id>          # sau ~1s: COMPLETED
curl -s localhost:8080/api/accounts/1000000002     # số dư 650000.00

# Số dư không đủ → FAILED, reason "Số dư không đủ"
curl -s -X POST localhost:8080/api/transfers -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-0002' \
  -d '{"fromAccount":"1000000002","toAccount":"1000000001","amount":99999999}'

# Tài khoản đích không tồn tại → trừ nguồn, rồi hoàn tiền (compensation) → FAILED
curl -s -X POST localhost:8080/api/transfers -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-0003' \
  -d '{"fromAccount":"1000000001","toAccount":"9999999999","amount":1000}'
```

## Công nghệ

### Đã có

| Mảng | Công nghệ |
|---|---|
| Framework | Java 21, Spring Boot 3.3, Maven đa module |
| API Gateway | Spring Cloud Gateway |
| Messaging | Kafka (KRaft), spring-kafka, retry + dead letter topic (`*.DLT`) |
| Pattern | Transactional outbox, saga điều phối, idempotent consumer, hexagonal (transaction-service) |
| Database | PostgreSQL (database-per-service), Spring Data JPA, Flyway, optimistic + pessimistic lock |
| Metrics | Actuator + Micrometer Prometheus (mới có endpoint `/actuator/prometheus`) |
| Hạ tầng local | Docker Compose: PostgreSQL, Kafka, Kafka UI |
| Test | JUnit 5, AssertJ (unit test domain) |

### Còn thiếu

| Mảng | Công nghệ | Ưu tiên |
|---|---|---|
| Bảo mật | Keycloak + OAuth2/JWT (Spring Security Resource Server) ở gateway và service | Cao |
| Cache, rate limit, khoá phân tán | Redis: cache, `RequestRateLimiter` ở gateway, ShedLock cho outbox relay | Cao |
| Chịu lỗi khi gọi đồng bộ | OpenFeign + Resilience4j (circuit breaker, retry, timeout) | Cao |
| Tracing phân tán | Micrometer Tracing + OpenTelemetry → Jaeger/Tempo, correlation id trong log | Cao |
| Test tích hợp | Testcontainers (PostgreSQL, Kafka), Spring Cloud Contract | Cao |
| Giám sát | Prometheus + Grafana dashboard, cảnh báo độ trễ consumer Kafka | Trung bình |
| Logging tập trung | Log JSON → Loki (hoặc ELK) | Trung bình |
| CQRS read model | reporting-service (sao kê, thống kê), có thể dùng Elasticsearch | Trung bình |
| Schema message | Avro + Confluent Schema Registry thay cho JSON | Trung bình |
| Đóng gói, triển khai | Dockerfile/Jib, Kubernetes + Helm, GitHub Actions/Jenkins | Trung bình |
| CDC | Debezium đọc bảng outbox thay cho relay polling | Thấp |
| Tài liệu API | springdoc-openapi (Swagger UI) | Thấp |
| Config / discovery | Spring Cloud Config, Eureka (hoặc dùng của Kubernetes) | Thấp |
| RPC hiệu năng cao | gRPC giữa các service | Thấp |

### Lộ trình đề xuất

1. **Swagger + Testcontainers**: dễ làm, kiểm chứng ngay bằng `mvn verify`.
2. **Keycloak + Redis**: xác thực JWT, rate limit, ShedLock cho outbox relay.
3. **OpenFeign + Resilience4j**: ví dụ transaction-service kiểm tra tài khoản đích tồn tại trước khi tạo lệnh.
4. **Observability**: OpenTelemetry tracing, Prometheus, Grafana, Loki (thêm vào docker-compose).
5. **reporting-service (CQRS + Elasticsearch)**, **Avro + Schema Registry**, **Debezium**.
6. **Docker image + Kubernetes/Helm + CI/CD**.
