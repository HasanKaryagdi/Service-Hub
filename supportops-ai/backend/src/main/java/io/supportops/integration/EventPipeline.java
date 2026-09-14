package io.supportops.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name="supportops.simulator.enabled",havingValue="false",matchIfMissing=true)
public class EventPipeline {
    private static final Logger log=LoggerFactory.getLogger(EventPipeline.class);
    private final JdbcTemplate jdbc;private final KafkaTemplate<String,String> kafka;private final ObjectMapper json;
    public EventPipeline(JdbcTemplate jdbc,KafkaTemplate<String,String> kafka,ObjectMapper json){this.jdbc=jdbc;this.kafka=kafka;this.json=json;}
    @Bean NewTopic eventsTopic(){return TopicBuilder.name("supportops.events").partitions(3).replicas(1).build();}
    @Scheduled(fixedDelay=3000) @Transactional public void publish(){
        for(var row:jdbc.queryForList("select * from outbox_events where published_at is null order by created_at limit 20 for update skip locked")){
            try{kafka.send("supportops.events",row.get("transaction_id").toString(),row.get("payload").toString()).get(12,TimeUnit.SECONDS);
                jdbc.update("update outbox_events set published_at=now() where id=?",row.get("id"));
            }catch(Exception e){log.warn("Outbox delivery deferred for {}: {}",row.get("id"),e.getClass().getSimpleName());if(e instanceof InterruptedException)Thread.currentThread().interrupt();break;}
        }
    }
    @KafkaListener(topics="supportops.events") public void consume(String payload)throws Exception{
        var event=json.readTree(payload);
        jdbc.update("insert into kafka_events values (?,?,?,?,?,'SUCCESS',?::timestamptz) on conflict(id) do nothing",UUID.fromString(event.path("eventId").asText()),"supportops.events",event.path("transactionId").asText(),event.path("transactionId").asText(),payload,event.path("occurredAt").asText());
    }
}
