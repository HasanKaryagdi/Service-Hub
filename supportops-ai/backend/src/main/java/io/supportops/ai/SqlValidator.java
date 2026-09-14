package io.supportops.ai;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;

/** Closed grammar, not a SQL blacklist. No joins, expressions, functions, aliases, comments or subqueries. */
@Component
public class SqlValidator {
    private static final Map<String,Set<String>> COLUMNS=Map.of(
        "transactions",Set.of("transaction_id","user_id","reconciliation_id","iban","status","currency","transaction_type"),
        "payments",Set.of("transaction_id","status","channel_type","account_type"),
        "rebates",Set.of("original_transaction_id","rebate_transaction_id","status"),
        "service_logs",Set.of("transaction_id","request_id","correlation_id","service_name","level"),
        "kafka_events",Set.of("transaction_id","topic","status"),
        "incidents",Set.of("transaction_id","incident_id","status","severity"));
    private static final Pattern SELECT=Pattern.compile("\\ASELECT \\* FROM analytics\\.([a-z_]+)(?: WHERE (.+?))? ORDER BY created_at DESC LIMIT ([1-9][0-9]{0,2})\\z");
    private static final Pattern EQUALITY=Pattern.compile("([a-z_]+) = '([a-zA-Z0-9@._:/ -]{1,120})'");
    private static final Pattern RECENT=Pattern.compile("created_at >= CURRENT_TIMESTAMP - INTERVAL '([1-9]|[12][0-9]|3[0-6]) months'");
    public String validate(String sql){
        if(sql==null || sql.length()>2000)throw new IllegalArgumentException("SQL exceeds policy");
        var match=SELECT.matcher(sql);
        if(!match.matches() || !COLUMNS.containsKey(match.group(1)))throw new IllegalArgumentException("SQL must use the approved single SELECT grammar and analytics views");
        if(Integer.parseInt(match.group(3))>200)throw new IllegalArgumentException("Maximum 200 rows");
        if(match.group(2)!=null){
            String[] predicates=match.group(2).split(" AND ",-1);
            if(predicates.length>6)throw new IllegalArgumentException("Too many predicates");
            for(String predicate:predicates){var eq=EQUALITY.matcher(predicate);
                if(RECENT.matcher(predicate).matches())continue;
                if(!eq.matches() || !COLUMNS.get(match.group(1)).contains(eq.group(1)))throw new IllegalArgumentException("Unsupported SQL predicate");
            }
        }
        return sql;
    }
}
