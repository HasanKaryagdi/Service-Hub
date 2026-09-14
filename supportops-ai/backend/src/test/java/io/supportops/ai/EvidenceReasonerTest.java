package io.supportops.ai;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class EvidenceReasonerTest {
    Map<String,Object> empty(){Map<String,Object> data=new HashMap<>();for(String key:List.of("service_logs","rebates","kafka_events","payments","account_transactions"))data.put(key,List.of());return data;}
    @Test void doesNotInventAConclusionWithoutEvidence(){var finding=new EvidenceReasoner().analyze(empty());assertThat(finding.confidence()).isLessThan(.5);assertThat(finding.rootCause()).contains("Further investigation");}
    @Test void tiesRebateConclusionToTheActualLog(){var data=empty();var log=Map.<String,Object>of("id","log-1","http_status",400,"message","Refundable balance is zero");data.put("service_logs",List.of(log));var finding=new EvidenceReasoner().analyze(data);assertThat(finding.rootCause()).contains("refundable balance was zero");assertThat(finding.evidence()).containsExactly(log);}
    @Test void matchesAccountEntriesByTransactionInsteadOfJustCheckingAnyEntryExists(){var data=empty();data.put("payments",List.of(Map.of("transaction_id","tx-005","status","SUCCESS")));data.put("account_transactions",List.of(Map.of("transaction_id","other","status","SUCCESS")));assertThat(new EvidenceReasoner().analyze(data).rootCause()).contains("no corresponding account transaction");}
    @Test void similarityIsBoundedAndDoesNotFabricatePercentages(){var provider=new TextSimilarityProvider();assertThat(provider.score("wallet balance zero","wallet balance zero")).isEqualTo(1);assertThat(provider.score("wallet balance zero","kafka timeout")).isZero();assertThat(provider.score("","")).isZero();}
}
