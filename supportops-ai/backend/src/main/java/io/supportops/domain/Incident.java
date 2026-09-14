package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="incidents")
public class Incident {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="incident_id") private String incidentId;
    public String getIncidentId() { return incidentId; }
    @Column(name="title") private String title;
    public String getTitle() { return title; }
    @Column(name="description") private String description;
    public String getDescription() { return description; }
    @Column(name="transaction_id") private String transactionId;
    public String getTransactionId() { return transactionId; }
    @Column(name="status") private String status;
    public String getStatus() { return status; }
    @Column(name="severity") private String severity;
    public String getSeverity() { return severity; }
    @Column(name="root_cause") private String rootCause;
    public String getRootCause() { return rootCause; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
    @Column(name="resolved_at") private Instant resolvedAt;
    public Instant getResolvedAt() { return resolvedAt; }
}
