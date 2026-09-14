package io.supportops.integration;
import io.supportops.repository.InvestigationRepository;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class PostgresLogSource implements LogSource {
    private final InvestigationRepository repo;
    public PostgresLogSource(InvestigationRepository repo){this.repo=repo;}
    public List<Map<String,Object>> forTransactions(List<String> ids){return repo.related("service_logs",ids);}
}
