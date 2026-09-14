package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="rebates")
public class Rebate {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="original_transaction_id") private String originalTransactionId;
    public String getOriginalTransactionId() { return originalTransactionId; }
    @Column(name="rebate_transaction_id") private String rebateTransactionId;
    public String getRebateTransactionId() { return rebateTransactionId; }
    @Column(name="amount") private BigDecimal amount;
    public BigDecimal getAmount() { return amount; }
    @Column(name="status") private String status;
    public String getStatus() { return status; }
    @Column(name="failure_reason") private String failureReason;
    public String getFailureReason() { return failureReason; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
}
