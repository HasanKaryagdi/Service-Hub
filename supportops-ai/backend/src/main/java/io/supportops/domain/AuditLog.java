package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="audit_logs")
public class AuditLog {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="user_id") private String userId;
    public String getUserId() { return userId; }
    @Column(name="action") private String action;
    public String getAction() { return action; }
    @Column(name="resource_type") private String resourceType;
    public String getResourceType() { return resourceType; }
    @Column(name="resource_id") private String resourceId;
    public String getResourceId() { return resourceId; }
    @Column(name="metadata") private String metadata;
    public String getMetadata() { return metadata; }
    @Column(name="created_at") private Instant createdAt;
    public Instant getCreatedAt() { return createdAt; }
}
