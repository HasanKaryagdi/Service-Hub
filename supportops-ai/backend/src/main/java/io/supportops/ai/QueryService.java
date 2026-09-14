package io.supportops.ai;

import io.supportops.audit.AuditService;
import io.supportops.dto.ApiTypes.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@Service
public class QueryService {
    private final JdbcTemplate jdbc;private final NaturalLanguageSql generator;private final SqlValidator validator;private final AuditService audit;private final String url,user,password;
    public QueryService(JdbcTemplate jdbc,NaturalLanguageSql generator,SqlValidator validator,AuditService audit,@Value("${spring.datasource.url}")String url,@Value("${supportops.readonly.username}")String user,@Value("${supportops.readonly.password}")String password){this.jdbc=jdbc;this.generator=generator;this.validator=validator;this.audit=audit;this.url=url;this.user=user;this.password=password;}
    public Preview preview(String prompt){
        String sql=generator.generate(prompt);var id=UUID.randomUUID();var expires=Instant.now().plusSeconds(600);
        jdbc.update("insert into query_previews values (?,?,?,now(),?)",id,AuditService.actor(),sql,Timestamp.from(expires));
        audit.record("AI_QUERY_CREATED","query",id.toString(),"provider="+generator.provider()+";sql="+sql);
        return new Preview(id,sql,expires,generator.provider());
    }
    public QueryResult run(UUID id){
        var preview=jdbc.queryForList("select sql_text from query_previews where id=? and user_id=? and expires_at>now()",id,AuditService.actor());
        if(preview.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Query preview is missing or expired");
        String sql=validator.validate(preview.getFirst().get("sql_text").toString());
        audit.record("SQL_QUERY_EXECUTION_STARTED","query",id.toString(),sql);
        Properties props=new Properties();props.setProperty("user",user);props.setProperty("password",password);props.setProperty("connectTimeout","3");props.setProperty("socketTimeout","6");props.setProperty("options","-c statement_timeout=3000 -c lock_timeout=1000 -c default_transaction_read_only=on");
        try(Connection connection=DriverManager.getConnection(url,props)){
            connection.setReadOnly(true);connection.setAutoCommit(false);
            try(Statement statement=connection.createStatement()){
                statement.setQueryTimeout(3);statement.setMaxRows(200);
                List<Map<String,Object>> rows=new ArrayList<>();
                try(ResultSet rs=statement.executeQuery(sql)){var meta=rs.getMetaData();while(rs.next()){Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=meta.getColumnCount();i++){Object value=rs.getObject(i);row.put(meta.getColumnLabel(i),value instanceof Timestamp t?t.toInstant().toString():value);}rows.add(row);}}
                connection.rollback();audit.record("SQL_QUERY_EXECUTED","query",id.toString(),"rows="+rows.size());
                return new QueryResult(rows,200,rows.size()==200);
            }
        }catch(SQLException e){audit.record("SQL_QUERY_FAILED","query",id.toString(),"sqlState="+e.getSQLState());throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Read-only query failed or exceeded its time limit");}
    }
}
