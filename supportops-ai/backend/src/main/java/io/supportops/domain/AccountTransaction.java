package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="account_transactions")
public class AccountTransaction {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="transaction_id") private String transactionId;
    public String getTransactionId() { return transactionId; }
    @Column(name="amount") private BigDecimal amount;
    public BigDecimal getAmount() { return amount; }
    @Column(name="status") private String status;
    public String getStatus() { return status; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
}
