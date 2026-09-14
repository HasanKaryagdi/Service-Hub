package io.supportops.service;
import io.supportops.ai.*;
import io.supportops.audit.AuditService;
import io.supportops.dto.ApiTypes.Investigation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.*;
import java.time.Instant;
import java.util.*;
@Service
public class InvestigationService {
    private static final Logger log=LoggerFactory.getLogger(InvestigationService.class);
    private final TransactionService transactions;private final EvidenceReasoner reasoner;private final AnalysisNarrator narrator;private final JdbcTemplate jdbc;private final ObjectMapper json;private final AuditService audit;
    public InvestigationService(TransactionService transactions,EvidenceReasoner reasoner,AnalysisNarrator narrator,JdbcTemplate jdbc,ObjectMapper json,AuditService audit){this.transactions=transactions;this.reasoner=reasoner;this.narrator=narrator;this.jdbc=jdbc;this.json=json;this.audit=audit;}
    public Investigation investigate(String id) throws Exception {
        var data=transactions.detail(id);
        if(Boolean.TRUE.equals(data.get("truncated")))throw new IllegalArgumentException("Evidence exceeds the investigation limit; narrow the source window before analysis");
        @SuppressWarnings("unchecked") var transaction=(Map<String,Object>)data.get("transaction");String tx=transaction.get("transaction_id").toString();
        var finding=reasoner.analyze(data);String summary=finding.rootCause(),provider="evidence-rules";
        if(narrator.enabled())try{summary=narrator.narrate(summary);provider="ollama + evidence-rules";}catch(Exception e){provider="evidence-rules (model unavailable)";log.warn("Model narration unavailable: {}",e.getClass().getSimpleName());}
        var analysis=new Investigation(UUID.randomUUID(),tx,summary,finding.confidence(),provider,List.of("Original transaction","Payment status","Rebate records","Wallet service logs","Kafka delivery and account ledger"),finding.evidence(),Instant.now());
        jdbc.update("insert into incident_analyses(id,transaction_id,root_cause,confidence,evidence,provider,created_at) values (?,?,?,?,?,?,now())",analysis.id(),tx,summary,analysis.confidence(),json.writeValueAsString(analysis.evidence()),provider);
        audit.record("TRANSACTION_INVESTIGATED","transaction",tx,"analysisId="+analysis.id());return analysis;
    }
}
