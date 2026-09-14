package io.supportops.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class ApiTypes {
    private ApiTypes() {}
    public record Login(@Email @NotBlank String email, @NotBlank @Size(max=120) String password) {}
    public record Session(String token, String name, String role) {}
    public record QueryPrompt(@NotBlank @Size(max=1000) String prompt) {}
    public record RunQuery(@NotNull UUID previewId) {}
    public record Preview(UUID id, String sql, Instant expiresAt, String provider) {}
    public record QueryResult(List<Map<String,Object>> rows, int maxRows, boolean possiblyTruncated) {}
    public record NewIncident(@NotBlank @Size(max=180) String title, @NotBlank @Size(max=5000) String description, @NotBlank String transactionId, @Pattern(regexp="LOW|MEDIUM|HIGH|CRITICAL") @NotNull String severity) {}
    public record IncidentUpdate(@Pattern(regexp="OPEN|INVESTIGATING|RESOLVED|CLOSED") @NotNull String status, @Size(max=5000) String rootCause) {}
    public record TimelineEvent(String id, String type, String service, String status, Instant timestamp, String description, Map<String,Object> details) {}
    public record Investigation(UUID id, String transactionId, String rootCause, double confidence, String provider, List<String> steps, List<Map<String,Object>> evidence, Instant createdAt) {}
}
