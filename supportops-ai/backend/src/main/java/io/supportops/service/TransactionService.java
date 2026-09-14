package io.supportops.service;

import io.supportops.repository.InvestigationRepository;
import io.supportops.integration.LogSource;
import io.supportops.dto.ApiTypes.TimelineEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class TransactionService {
    private final InvestigationRepository repo; private final LogSource logs;
    public TransactionService(InvestigationRepository repo,LogSource logs){this.repo=repo;this.logs=logs;}
    public Map<String,Object> list(int page){
        if(page<0 || page>10000)throw new IllegalArgumentException("Page is out of range");
        var records=repo.rows("select * from transactions order by created_at desc,id desc limit 51 offset ?",page*50);
        var total=repo.rows("select count(*) as total from transactions").getFirst().get("total");
        return Map.of("transactions",records.stream().limit(50).toList(),"total",total,"page",page,"pageSize",50,"hasNext",records.size()>50);
    }
    public Map<String,Object> search(String q,String type){
        if(q==null || q.isBlank() || q.length()>120) throw new IllegalArgumentException("Enter an identifier of 1–120 characters");
        if(!Set.of("auto","transactionId","userId","reconciliationId","iban","requestId","correlationId").contains(type)) throw new IllegalArgumentException("Unsupported search type");
        // IDs are opaque. Auto mode searches every exact-match namespace instead of guessing by prefix.
        var matches=repo.search(q.trim(),type);
        var results=matches.stream().limit(100).toList();
        return Map.of("transactions",results,"truncated",matches.size()>100,"searchType",type);
    }
    public Map<String,Object> detail(String id){
        var tx=repo.transaction(id); var ids=repo.family(tx.get("transaction_id").toString());
        Map<String,Object> result=new LinkedHashMap<>();result.put("transaction",tx);
        boolean truncated=false;
        for(String table:List.of("transactions","payments","rebates","account_transactions","kafka_events","incidents","incident_analyses")){
            var records=repo.related(table,ids);truncated|=records.size()>1000;result.put(table,records.stream().limit(1000).toList());
        }
        var records=logs.forTransactions(ids);truncated|=records.size()>1000;result.put("service_logs",records.stream().limit(1000).toList());
        result.put("truncated",truncated);result.put("timeline",timeline(result));return result;
    }
    @SuppressWarnings("unchecked")
    private List<TimelineEvent> timeline(Map<String,Object> data){
        List<TimelineEvent> events=new ArrayList<>();
        for(String table:List.of("transactions","payments","rebates","account_transactions","kafka_events","service_logs")){
            for(var row:(List<Map<String,Object>>)data.get(table)){
                String service=Objects.toString(row.get("service_name"),switch(table){case "payments"->"payment-service";case "rebates"->"rebate-service";case "kafka_events"->"kafka";case "account_transactions"->"wallet-service";default->"transaction-service";});
                String status=Objects.toString(row.get("status"),Objects.toString(row.get("level"),"INFO"));
                String description=Objects.toString(row.get("message"),table.replace('_',' ')+" · "+status);
                events.add(new TimelineEvent(row.get("id").toString(),table,service,status,Instant.parse(row.get("created_at").toString()),description,row));
            }
        }
        events.sort(Comparator.comparing(TimelineEvent::timestamp).thenComparing(TimelineEvent::id));return events;
    }
}
