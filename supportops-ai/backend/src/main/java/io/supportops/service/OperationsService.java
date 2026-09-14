package io.supportops.service;

import io.supportops.repository.InvestigationRepository;
import io.supportops.ai.SimilarityProvider;
import io.supportops.audit.AuditService;
import io.supportops.dto.ApiTypes.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class OperationsService {
    private final InvestigationRepository repo;private final JdbcTemplate jdbc;private final TransactionService transactions;private final SimilarityProvider similarity;private final AuditService audit;
    public OperationsService(InvestigationRepository repo,JdbcTemplate jdbc,TransactionService transactions,SimilarityProvider similarity,AuditService audit){this.repo=repo;this.jdbc=jdbc;this.transactions=transactions;this.similarity=similarity;this.audit=audit;}
    public Map<String,Object> dashboard(){
        var stats=repo.rows("select count(*) as total_transactions,count(*) filter(where status='SUCCESS') as successful_transactions,count(*) filter(where status='FAILED') as failed_transactions,(select count(*) from incidents where status in ('OPEN','INVESTIGATING')) as open_incidents,(select count(*) from incidents where severity='CRITICAL' and status in ('OPEN','INVESTIGATING')) as critical_incidents,(select round(100.0*count(*) filter(where status='FAILED')/nullif(count(*),0),1) from rebates) as rebate_failure_rate,(select round(100.0*count(*) filter(where status='FAILED')/nullif(count(*),0),1) from payments) as payment_failure_rate from transactions").getFirst();
        var errors=repo.rows("select to_char(h,'HH24:00') as hour,count(l.id) as count from generate_series(date_trunc('hour',now())-interval '23 hours',date_trunc('hour',now()),interval '1 hour') h left join service_logs l on l.created_at>=h and l.created_at<h+interval '1 hour' and l.level='ERROR' group by h order by h");
        var services=repo.rows("select service_name,count(*) as errors from service_logs where level='ERROR' and created_at>=now()-interval '24 hours' group by service_name order by errors desc");
        return Map.of("stats",stats,"errors",errors,"services",services,"incidents",list("incidents",0,""));
    }
    public List<Map<String,Object>> list(String resource,int page,String q){
        if(page<0 || page>10000)throw new IllegalArgumentException("Page is out of range");
        String table=switch(resource){case "incidents"->"incidents";case "logs"->"service_logs";case "audit"->"audit_logs";default->throw new IllegalArgumentException("Unknown resource");};
        String column=switch(resource){case "incidents"->"title";case "logs"->"message";default->"action";};
        return repo.rows("select * from "+table+" where "+column+" ilike ? order by created_at desc,id desc limit 50 offset ?","%"+q+"%",page*50);
    }
    public Map<String,Object> incident(String id){
        var incident=repo.rows("select * from incidents where incident_id=? or id::text=?",id,id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found"));
        var related=new LinkedHashMap<>(transactions.detail(incident.get("transaction_id").toString()));related.put("incident",incident);
        String text=incident.get("title")+" "+Objects.toString(incident.get("root_cause"),"");
        var candidates=repo.rows("select incident_id,title,root_cause from incidents where id<>? order by created_at desc limit 500",incident.get("id"));
        candidates.forEach(c->c.put("similarity",Math.round(100*similarity.score(text,c.get("title")+" "+Objects.toString(c.get("root_cause"),"")))));
        candidates.sort(Comparator.comparingLong((Map<String,Object> r)->((Number)r.get("similarity")).longValue()).reversed());
        related.put("similar",candidates.stream().filter(c->((Number)c.get("similarity")).intValue()>0).limit(5).toList());return related;
    }
    @Transactional
    public Map<String,Object> create(NewIncident input){
        String tx=repo.transaction(input.transactionId()).get("transaction_id").toString();var id=UUID.randomUUID();String code="INC-"+id.toString().substring(0,8).toUpperCase();
        jdbc.update("insert into incidents(id,incident_id,title,description,transaction_id,status,severity,created_at) values (?,?,?,?,?,'OPEN',?,now())",id,code,input.title(),input.description(),tx,input.severity());
        audit.recordAtomic("INCIDENT_CREATED","incident",code,"transactionId="+tx);return Map.of("incidentId",code);
    }
    @Transactional
    public void update(String id,IncidentUpdate input){
        var found=repo.rows("select * from incidents where incident_id=? for update",id);
        if(found.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found");
        String current=found.getFirst().get("status").toString();
        Map<String,Set<String>> transitions=Map.of("OPEN",Set.of("INVESTIGATING","RESOLVED"),"INVESTIGATING",Set.of("OPEN","RESOLVED"),"RESOLVED",Set.of("OPEN","CLOSED"),"CLOSED",Set.of("OPEN"));
        if(!transitions.get(current).contains(input.status()))throw new IllegalArgumentException("Invalid incident status transition");
        String root=input.rootCause()==null?Objects.toString(found.getFirst().get("root_cause"),""):input.rootCause();
        if(input.status().equals("RESOLVED") && root.isBlank())throw new IllegalArgumentException("A root cause is required to resolve an incident");
        jdbc.update("update incidents set status=?,root_cause=?,resolved_at=case when ? in ('RESOLVED','CLOSED') then coalesce(resolved_at,now()) else null end where incident_id=?",input.status(),root,input.status(),id);
        audit.recordAtomic(input.status().equals("RESOLVED")?"INCIDENT_RESOLVED":"INCIDENT_STATUS_CHANGED","incident",id,"from="+current+";to="+input.status());
    }
}
