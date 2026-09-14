package io.supportops.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.UUID;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public static String actor() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void record(String action, String type, String id, String metadata) {
        insert(action,type,id,metadata);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void recordAtomic(String action,String type,String id,String metadata) {
        insert(action,type,id,metadata);
    }
    private void insert(String action,String type,String id,String metadata) {
        jdbc.update("insert into audit_logs values (?,?,?,?,?,?,now())",UUID.randomUUID(),actor(),action,type,id,metadata);
    }
}
