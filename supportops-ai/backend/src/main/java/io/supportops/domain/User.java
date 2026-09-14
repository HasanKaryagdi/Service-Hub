package io.supportops.domain;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
@Entity
@Table(name="app_users")
public class User {
    @Id private UUID id;
    public UUID getId() { return id; }
    @Column(name="user_id") private String userId;
    public String getUserId() { return userId; }
    @Column(name="email") private String email;
    public String getEmail() { return email; }
    @Column(name="name") private String name;
    public String getName() { return name; }
    @Column(name="password_hash") private String passwordHash;
    public String getPasswordHash() { return passwordHash; }
    @Column(name="role") private String role;
    public String getRole() { return role; }
    @Column(name="iban") private String iban;
    public String getIban() { return iban; }
}
