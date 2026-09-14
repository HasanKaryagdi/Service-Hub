package io.supportops.config;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
@Component
@ConditionalOnProperty(name="supportops.seed",havingValue="true")
public class DemoSeed implements ApplicationRunner {
    private final JdbcTemplate jdbc;private final PasswordEncoder passwords;private final String password;
    public DemoSeed(JdbcTemplate jdbc,PasswordEncoder passwords,@Value("${supportops.demo-password}")String password){this.jdbc=jdbc;this.passwords=passwords;this.password=password;}
    @Override @Transactional public void run(ApplicationArguments args)throws Exception{
        jdbc.execute("select pg_advisory_xact_lock(714092)");
        if(jdbc.queryForObject("select count(*) from app_users",Long.class)>0)return;
        String hash=passwords.encode(password);
        for(int i=1;i<=50;i++){
            String role=i==1?"ADMIN":i==3?"VIEWER":"SUPPORT_ENGINEER";
            String email=i==1?"admin@supportops.local":i==2?"engineer@supportops.local":i==3?"viewer@supportops.local":"demo"+i+"@supportops.local";
            jdbc.update("insert into app_users values (?,?,?,?,?,?,?)",UUID.randomUUID(),String.format("usr-%03d",i),email,"Demo Engineer "+i,hash,role,"TR720001002136978165"+String.format("%06d",i));
        }
        jdbc.execute(new ClassPathResource("demo-seed.sql").getContentAsString(StandardCharsets.UTF_8));
    }
}
