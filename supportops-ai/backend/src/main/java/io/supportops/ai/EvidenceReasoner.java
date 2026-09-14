package io.supportops.ai;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class EvidenceReasoner {
    public record Finding(String rootCause,double confidence,List<Map<String,Object>> evidence) {}
    @SuppressWarnings("unchecked")
    public Finding analyze(Map<String,Object> data){
        var logs=(List<Map<String,Object>>)data.get("service_logs");
        var rebates=(List<Map<String,Object>>)data.get("rebates");
        var kafka=(List<Map<String,Object>>)data.get("kafka_events");
        var payments=(List<Map<String,Object>>)data.get("payments");
        var accounts=(List<Map<String,Object>>)data.get("account_transactions");
        List<Map<String,Object>> evidence=new ArrayList<>();
        logs.stream().filter(r->r.get("http_status") instanceof Number n && n.intValue()>=400).forEach(evidence::add);
        rebates.stream().filter(r->"FAILED".equals(r.get("status"))).forEach(evidence::add);
        kafka.stream().filter(r->"FAILED".equals(r.get("status"))).forEach(evidence::add);
        String joined=evidence.toString().toLowerCase(Locale.ROOT);
        if(joined.contains("refundable balance"))return new Finding("Wallet Service rejected the rebate because the refundable balance was zero. Verify the original payment's remaining refundable amount before retrying.",.92,evidence);
        if(joined.contains("duplicate"))return new Finding("The rebate was rejected as a duplicate request. Compare the idempotency key and previous rebate before issuing another operation.",.90,evidence);
        if(joined.contains("timeout"))return new Finding("Wallet Service timed out during payment processing. The evidence does not establish whether the downstream operation committed; reconcile before retrying.",.85,evidence);
        if(kafka.stream().anyMatch(r->"FAILED".equals(r.get("status"))))return new Finding("Payment evidence exists, but a Kafka delivery failed. Inspect the producer/outbox and consumer lag before replaying the event.",.86,evidence);
        for(var payment:payments) if("SUCCESS".equals(payment.get("status")) && accounts.stream().noneMatch(a->Objects.equals(a.get("transaction_id"),payment.get("transaction_id")))){
            evidence.add(payment);return new Finding("A successful payment has no corresponding account transaction in the retrieved evidence. Check ledger consumption and reconciliation; missing evidence alone does not prove loss.",.78,evidence);
        }
        return new Finding("No supported failure pattern was found in the available evidence. Further investigation is required.",.25,evidence);
    }
}
