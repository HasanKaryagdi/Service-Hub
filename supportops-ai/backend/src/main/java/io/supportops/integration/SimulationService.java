package io.supportops.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;

@Service
@ConditionalOnProperty(name="supportops.simulator.enabled",havingValue="true")
public class SimulationService {
    private final JdbcTemplate jdbc;private final ObjectMapper json;private final TransactionTemplate transaction;private final RestClient client;
    private final String role,token,txUrl,walletUrl,rebateUrl;
    public SimulationService(JdbcTemplate jdbc,ObjectMapper json,PlatformTransactionManager manager,RestClient.Builder builder,@Value("${supportops.simulator.role}")String role,@Value("${supportops.simulator.token}")String token,@Value("${supportops.simulator.transaction-url}")String txUrl,@Value("${supportops.simulator.wallet-url}")String walletUrl,@Value("${supportops.simulator.rebate-url}")String rebateUrl){
        this.jdbc=jdbc;this.json=json;this.transaction=new TransactionTemplate(manager);this.role=role;this.token=token;this.txUrl=txUrl;this.walletUrl=walletUrl;this.rebateUrl=rebateUrl;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());factory.setReadTimeout(Duration.ofSeconds(5));
        this.client=builder.requestFactory(factory).defaultHeader("X-Simulator-Token",token).build();
    }
    public record Command(String transactionId,String scenario) {}
    public Map<String,Object> run(Command input){
        String scenario=input.scenario()==null?"success":input.scenario();
        if(!Set.of("success","rebate-zero","timeout","kafka-failed","duplicate","missing-account").contains(scenario))throw new IllegalArgumentException("Unknown scenario");
        if(role.equals("payment")){
            String tx="sim-"+UUID.randomUUID();var command=new Command(tx,scenario);
            client.post().uri(txUrl+"/internal/simulate").body(command).retrieve().toBodilessEntity();
            record(tx,"Payment request received",200);
            boolean success=!scenario.equals("timeout");
            var wallet=client.post().uri(walletUrl+"/internal/simulate").body(new Command(tx,scenario.equals("timeout")?"timeout":"success")).exchange((req,res)->res.getStatusCode().value());
            transaction.executeWithoutResult(status->{
                jdbc.update("update transactions set status=?,updated_at=now() where transaction_id=?",success?"SUCCESS":"FAILED",tx);
                jdbc.update("insert into payments values (?,?,100,'MOBILE','WALLET',?,now())",UUID.randomUUID(),tx,success?"SUCCESS":"FAILED");
                if(success && !scenario.equals("missing-account"))jdbc.update("insert into account_transactions values (?,?,100,'SUCCESS',now())",UUID.randomUUID(),tx);
                if(scenario.equals("kafka-failed"))jdbc.update("insert into kafka_events values (?, 'payment.completed',?,?,?,'FAILED',now())",UUID.randomUUID(),tx,tx,"{\"synthetic\":true,\"reason\":\"producer unavailable\"}");
                else outbox(tx);
            });
            if(scenario.equals("rebate-zero")||scenario.equals("duplicate"))client.post().uri(rebateUrl+"/internal/simulate").body(command).retrieve().toBodilessEntity();
            return Map.of("transactionId",tx,"walletStatus",wallet,"scenario",scenario);
        }
        if(input.transactionId()==null || !input.transactionId().matches("sim-[a-f0-9-]{36}"))throw new IllegalArgumentException("Simulator transaction ID required");
        String tx=input.transactionId();
        if(role.equals("transaction"))jdbc.update("insert into transactions values (?,?, 'usr-001',?, 'TR720001002136978165000001',100,'TRY','PAYMENT','CREATED',now(),now())",UUID.randomUUID(),tx,"rec-"+UUID.randomUUID());
        if(role.equals("wallet")){int code=scenario.equals("timeout")?504:scenario.equals("rebate-zero")||scenario.equals("duplicate")?400:200;record(tx,code==504?"Wallet Service timeout":code==400?scenario.equals("duplicate")?"Duplicate rebate request":"Refundable balance is zero":"Wallet service accepted payment",code);return Map.of("httpStatus",code);}
        if(role.equals("rebate")){
            client.post().uri(walletUrl+"/internal/simulate").body(input).exchange((req,res)->res.getStatusCode().value());
            transaction.executeWithoutResult(s->{String rebate="sim-"+UUID.randomUUID();String reason=scenario.equals("duplicate")?"Duplicate rebate request":"Refundable balance is zero";
                jdbc.update("insert into transactions values (?,?, 'usr-001',?, 'TR720001002136978165000001',10,'TRY','REBATE','FAILED',now(),now())",UUID.randomUUID(),rebate,"rec-"+UUID.randomUUID());
                jdbc.update("insert into rebates values (?,?,?,10,'FAILED',?,now())",UUID.randomUUID(),tx,rebate,reason);outbox(rebate);
            });record(tx,"Rebate failed",400);
        }
        return Map.of("transactionId",tx,"service",role);
    }
    private void record(String tx,String message,int status){jdbc.update("insert into service_logs values (?,?,?,?,?,?,?,?,?,?,now())",UUID.randomUUID(),role+"-service",status>=400?"ERROR":"INFO",message,"req-"+tx,"corr-"+tx,tx,"{\"synthetic\":true}","{\"status\":"+status+"}",status);}
    private void outbox(String tx){try{var id=UUID.randomUUID();String payload=json.writeValueAsString(Map.of("eventId",id,"transactionId",tx,"occurredAt",Instant.now().toString()));jdbc.update("insert into outbox_events(id,transaction_id,payload) values (?,?,?)",id,tx,payload);}catch(Exception e){throw new IllegalStateException("Cannot serialize event",e);}}
}
