package io.supportops.ai;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
@Component
public class NaturalLanguageSql {
    private final SqlValidator validator;
    private final SqlModelAdapter model;
    public NaturalLanguageSql(SqlValidator validator){this.validator=validator;this.model=null;}
    @org.springframework.beans.factory.annotation.Autowired
    public NaturalLanguageSql(SqlValidator validator,SqlModelAdapter model){this.validator=validator;this.model=model;}
    public String provider(){return model!=null&&model.enabled()?"ollama-validated":"constrained-template";}
    public String generate(String prompt){
        if(model!=null&&model.enabled())return validator.validate(model.generate(prompt));
        String p=prompt.toLowerCase(Locale.ROOT);
        String table=p.contains("log")?"service_logs":p.contains("kafka")?"kafka_events":p.contains("incident")?"incidents":p.contains("rebate") || p.contains("iade")?"rebates":p.contains("payment") || p.contains("ödeme")?"payments":"transactions";
        List<String> conditions=new ArrayList<>();
        var iban=Pattern.compile("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}\\b").matcher(prompt.toUpperCase(Locale.ROOT));
        if(iban.find()){table="transactions";conditions.add("iban = '"+iban.group()+"'");}
        var tx=Pattern.compile("\\b(?:tx|abc|sim)-[a-zA-Z0-9-]+\\b").matcher(prompt);
        if(tx.find())conditions.add((table.equals("rebates")?"original_transaction_id":"transaction_id")+" = '"+tx.group()+"'");
        if(p.contains("fail") || p.contains("başarısız"))conditions.add((table.equals("service_logs")?"level = 'ERROR'":"status = 'FAILED'"));
        var months=Pattern.compile("([0-9]{1,2})\\s*(?:ay|month)").matcher(p);
        if(months.find())conditions.add("created_at >= CURRENT_TIMESTAMP - INTERVAL '"+months.group(1)+" months'");
        if(conditions.isEmpty())throw new IllegalArgumentException("Specify an IBAN, transaction ID, failed status, or a 1–36 month period. MVP SQL generation supports these explicit filters only.");
        if(table.equals("incidents") && conditions.contains("status = 'FAILED'"))throw new IllegalArgumentException("Incidents use OPEN, INVESTIGATING, RESOLVED or CLOSED; FAILED is not an incident status");
        return validator.validate("SELECT * FROM analytics."+table+" WHERE "+String.join(" AND ",conditions)+" ORDER BY created_at DESC LIMIT 200");
    }
}
