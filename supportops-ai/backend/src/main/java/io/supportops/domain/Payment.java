package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="payments")
public class Payment {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="transaction_id") private String transactionId;
    public String getTransactionId() { return transactionId; }
    @Column(name="amount") private BigDecimal amount;
    public BigDecimal getAmount() { return amount; }
    @Column(name="channel_type") private String channelType;
    public String getChannelType() { return channelType; }
    @Column(name="account_type") private String accountType;
    public String getAccountType() { return accountType; }
    @Column(name="status") private String status;
    public String getStatus() { return status; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
}
