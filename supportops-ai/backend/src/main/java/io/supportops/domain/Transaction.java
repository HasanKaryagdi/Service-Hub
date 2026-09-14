package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="transactions")
public class Transaction {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="transaction_id") private String transactionId;
    public String getTransactionId() { return transactionId; }
    @Column(name="user_id") private String userId;
    public String getUserId() { return userId; }
    @Column(name="reconciliation_id") private String reconciliationId;
    public String getReconciliationId() { return reconciliationId; }
    @Column(name="iban") private String iban;
    public String getIban() { return iban; }
    @Column(name="amount") private BigDecimal amount;
    public BigDecimal getAmount() { return amount; }
    @Column(name="currency") private String currency;
    public String getCurrency() { return currency; }
    @Column(name="transaction_type") private String transactionType;
    public String getTransactionType() { return transactionType; }
    @Column(name="status") private String status;
    public String getStatus() { return status; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
    @Column(name="updated_at") private Instant updatedAt;
    public Instant getUpdatedAt() { return updatedAt; }
}
