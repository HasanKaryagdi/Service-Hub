package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="kafka_events")
public class KafkaEvent {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="topic") private String topic;
    public String getTopic() { return topic; }
    @Column(name="event_key") private String eventKey;
    public String getEventKey() { return eventKey; }
    @Column(name="transaction_id") private String transactionId;
    public String getTransactionId() { return transactionId; }
    @Column(name="payload") private String payload;
    public String getPayload() { return payload; }
    @Column(name="status") private String status;
    public String getStatus() { return status; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
}
