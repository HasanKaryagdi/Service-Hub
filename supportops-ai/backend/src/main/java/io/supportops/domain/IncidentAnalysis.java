package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="incident_analyses")
public class IncidentAnalysis {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="incident_id") private UUID incidentId;
    public UUID getIncidentId() { return incidentId; }
    @Column(name="transaction_id") private String transactionId;
    public String getTransactionId() { return transactionId; }
    @Column(name="root_cause") private String rootCause;
    public String getRootCause() { return rootCause; }
    @Column(name="confidence") private BigDecimal confidence;
    public BigDecimal getConfidence() { return confidence; }
    @Column(name="evidence") private String evidence;
    public String getEvidence() { return evidence; }
    @Column(name="provider") private String provider;
    public String getProvider() { return provider; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
}
