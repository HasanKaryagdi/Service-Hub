package io.supportops;

import io.supportops.ai.QueryService;
import io.supportops.ai.EvidenceReasoner;
import io.supportops.dto.ApiTypes.*;
import io.supportops.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.kafka.listener.auto-startup=false","spring.kafka.admin.auto-create=false","management.tracing.enabled=false","supportops.seed=true"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker=true)
class PlatformIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17.6-alpine").withDatabaseName("supportops").withUsername("supportops").withPassword("test-password").withInitScript("test-bootstrap.sql");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
    @Autowired JdbcTemplate jdbc;@Autowired TransactionService transactions;@Autowired EvidenceReasoner reasoner;@Autowired QueryService queries;@Autowired OperationsService operations;@Autowired MockMvc mvc;
    @Test void seedMeetsVolumeAndReferentialRequirements(){
        for(var entry:Map.of("app_users",50,"transactions",500,"rebates",50,"incidents",20,"service_logs",1000,"kafka_events",100).entrySet())assertThat(jdbc.queryForObject("select count(*) from "+entry.getKey(),Integer.class)).isGreaterThanOrEqualTo(entry.getValue());
        assertThat(jdbc.queryForObject("select count(*) from rebates where status='FAILED'",Integer.class)).isEqualTo(30);
        assertThat(jdbc.queryForObject("select count(*) from rebates r join transactions t on t.transaction_id=r.rebate_transaction_id where t.amount<>r.amount",Integer.class)).isZero();
    }
    @Test void scenariosProduceDistinctSupportedFindings(){
        assertThat(reasoner.analyze(transactions.detail("abc-123")).rootCause()).contains("refundable balance was zero");
        assertThat(reasoner.analyze(transactions.detail("tx-002")).rootCause()).contains("Kafka delivery failed");
        assertThat(reasoner.analyze(transactions.detail("tx-003")).rootCause()).contains("timed out");
        assertThat(reasoner.analyze(transactions.detail("tx-004")).rootCause()).contains("duplicate");
        assertThat(reasoner.analyze(transactions.detail("tx-005")).rootCause()).contains("no corresponding account transaction");
    }
    @Test void searchFindsOpaqueIdentifiersAndTimelineIncludesRebateFamily(){
        for(String q:List.of("abc-123","usr-001","rec-0001","TR720001002136978165000001","req-001","corr-001"))assertThat((List<?>)transactions.search(q,"auto").get("transactions")).isNotEmpty();
        @SuppressWarnings("unchecked") var timeline=(List<TimelineEvent>)transactions.detail("abc-123").get("timeline");
        assertThat(timeline).extracting(TimelineEvent::timestamp).isSorted();
        assertThat(timeline).anyMatch(e->"tx-451".equals(e.details().get("transaction_id")));
    }
    @Test void readerCannotReadSecretsOrModifyEvenIfValidationIsBypassed()throws Exception{
        try(Connection c=DriverManager.getConnection(postgres.getJdbcUrl(),"supportops_reader","local-reader-password");Statement s=c.createStatement()){
            assertThat(s.executeQuery("select count(*) from analytics.transactions").next()).isTrue();
            assertThatThrownBy(()->s.executeQuery("select * from app_users")).isInstanceOf(SQLException.class);
            assertThatThrownBy(()->s.execute("delete from transactions")).isInstanceOf(SQLException.class);
            assertThatThrownBy(()->s.execute("create table public.unwanted(id integer)")).isInstanceOf(SQLException.class);
        }
    }
    @Test @WithMockUser(username="usr-002",roles="SUPPORT_ENGINEER") void queryRequiresOwnedUnexpiredPreviewAndIsAudited(){
        var preview=queries.preview("failed transactions");assertThat(queries.run(preview.id()).rows()).isNotEmpty();
        jdbc.update("update query_previews set user_id='other-user' where id=?",preview.id());assertThatThrownBy(()->queries.run(preview.id())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        jdbc.update("update query_previews set user_id='usr-002',expires_at=now()-interval '1 minute' where id=?",preview.id());assertThatThrownBy(()->queries.run(preview.id())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='SQL_QUERY_EXECUTED' and resource_id=?",Integer.class,preview.id().toString())).isEqualTo(1);
    }
    @Test @WithMockUser(roles="VIEWER") void viewerCannotExecuteInvestigationsOrQueries()throws Exception{
        mvc.perform(post("/api/transactions/abc-123/investigate")).andExpect(status().isForbidden());
        mvc.perform(post("/api/ai-query/preview").contentType("application/json").content("{\"prompt\":\"failed transactions\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit")).andExpect(status().isForbidden());
        mvc.perform(get("/api/transactions/abc-123")).andExpect(status().isOk());
    }
    @Test void unauthenticatedRequestsFail()throws Exception{mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());}
    @Test @WithMockUser(roles="VIEWER") void transactionBrowserIsPagedAndReadOnly()throws Exception{
        mvc.perform(get("/api/transactions?page=0")).andExpect(status().isOk()).andExpect(jsonPath("$.transactions.length()").value(50)).andExpect(jsonPath("$.total").value(500)).andExpect(jsonPath("$.hasNext").value(true));
        @SuppressWarnings("unchecked") var first=(List<Map<String,Object>>)transactions.list(0).get("transactions");
        @SuppressWarnings("unchecked") var second=(List<Map<String,Object>>)transactions.list(1).get("transactions");
        assertThat(first).extracting(r->r.get("id")).doesNotContainAnyElementsOf(second.stream().map(r->r.get("id")).toList());
        assertThat(transactions.list(9).get("hasNext")).isEqualTo(false);
        mvc.perform(get("/api/transactions?page=-1")).andExpect(status().isBadRequest());
    }
    @Test @WithMockUser(username="usr-002",roles="SUPPORT_ENGINEER") void incidentResolutionRequiresCauseAndValidTransition(){
        String id=operations.create(new NewIncident("Test incident","Resolution workflow","abc-123","HIGH")).get("incidentId").toString();
        assertThatThrownBy(()->operations.update(id,new IncidentUpdate("RESOLVED",""))).isInstanceOf(IllegalArgumentException.class);
        operations.update(id,new IncidentUpdate("RESOLVED","Verified balance mismatch"));operations.update(id,new IncidentUpdate("CLOSED",null));
        assertThatThrownBy(()->operations.update(id,new IncidentUpdate("INVESTIGATING",null))).isInstanceOf(IllegalArgumentException.class);
    }
}
