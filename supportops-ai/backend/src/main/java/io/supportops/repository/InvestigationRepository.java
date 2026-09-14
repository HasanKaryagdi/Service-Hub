package io.supportops.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Timestamp;
import java.util.*;

@Repository
public class InvestigationRepository {
    private final JdbcTemplate jdbc;
    public InvestigationRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<Map<String,Object>> rows(String sql,Object... args){
        var rows=jdbc.queryForList(sql,args);
        rows.forEach(row->row.replaceAll((key,value)->value instanceof Timestamp t?t.toInstant().toString():value));
        return rows;
    }
    public Map<String,Object> transaction(String id){
        return rows("select * from transactions where transaction_id=? or id::text=?",id,id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Transaction not found"));
    }
    public List<String> family(String id){
        return rows("with recursive family(tid) as (select transaction_id from transactions where transaction_id=? UNION select case when r.original_transaction_id=f.tid then r.rebate_transaction_id else r.original_transaction_id end from rebates r join family f on r.original_transaction_id=f.tid or r.rebate_transaction_id=f.tid) select tid from family",id).stream().map(r->r.get("tid").toString()).toList();
    }
    public List<Map<String,Object>> related(String table,List<String> ids){
        if(!Set.of("transactions","payments","rebates","account_transactions","service_logs","kafka_events","incidents","incident_analyses").contains(table)) throw new IllegalArgumentException("Unknown resource");
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        String column=table.equals("rebates")?"original_transaction_id":"transaction_id";
        return rows("select * from "+table+" where "+column+" in ("+placeholders+") order by created_at asc,id asc limit 1001",ids.toArray());
    }
    public List<Map<String,Object>> search(String q,String type){
        String column=switch(type){case "transactionId"->"t.transaction_id";case "userId"->"t.user_id";case "reconciliationId"->"t.reconciliation_id";case "iban"->"t.iban";case "requestId"->"l.request_id";case "correlationId"->"l.correlation_id";default->null;};
        String condition=column==null?"(t.transaction_id=? or t.user_id=? or t.reconciliation_id=? or t.iban=? or l.request_id=? or l.correlation_id=?)":column+"=?";
        Object[] params=column==null?new Object[]{q,q,q,q.replace(" ",""),q,q}:new Object[]{type.equals("iban")?q.replace(" ",""):q};
        return rows("select distinct t.* from transactions t left join service_logs l on l.transaction_id=t.transaction_id where "+condition+" order by t.created_at desc limit 101",params);
    }
}
