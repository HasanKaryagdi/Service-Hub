package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="service_logs")
public class ServiceLog {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="service_name") private String serviceName;
    public String getServiceName() { return serviceName; }
    @Column(name="level") private String level;
    public String getLevel() { return level; }
    @Column(name="message") private String message;
    public String getMessage() { return message; }
    @Column(name="request_id") private String requestId;
    public String getRequestId() { return requestId; }
    @Column(name="correlation_id") private String correlationId;
    public String getCorrelationId() { return correlationId; }
    @Column(name="transaction_id") private String transactionId;
    public String getTransactionId() { return transactionId; }
    @Column(name="request_body") private String requestBody;
    public String getRequestBody() { return requestBody; }
    @Column(name="response_body") private String responseBody;
    public String getResponseBody() { return responseBody; }
    @Column(name="http_status") private Integer httpStatus;
    public Integer getHttpStatus() { return httpStatus; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
}
