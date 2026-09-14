package io.supportops.ai;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
class SqlValidatorTest {
    final SqlValidator validator=new SqlValidator();
    @Test void acceptsOnlyBoundedViewQueries(){String sql="SELECT * FROM analytics.transactions WHERE iban = 'TR720001002136978165000001' AND created_at >= CURRENT_TIMESTAMP - INTERVAL '3 months' ORDER BY created_at DESC LIMIT 200";assertThat(validator.validate(sql)).isEqualTo(sql);}
    @ParameterizedTest @ValueSource(strings={
        "DELETE FROM transactions", "DROP TABLE transactions", "INSERT INTO transactions SELECT * FROM transactions", "UPDATE transactions SET status='SUCCESS'", "ALTER TABLE transactions ADD x text", "TRUNCATE transactions", "CREATE TABLE x(id int)", "GRANT ALL ON transactions TO public", "REVOKE ALL ON transactions FROM public",
        "SELECT pg_sleep(30)","SELECT * FROM app_users", "SELECT * FROM analytics.transactions; DROP TABLE transactions",
        "WITH x AS (DELETE FROM transactions RETURNING *) SELECT * FROM x", "SELECT * INTO x FROM analytics.transactions",
        "SELECT * FROM analytics.transactions ORDER BY created_at DESC LIMIT 201", "SELECT * FROM analytics.transactions ORDER BY created_at DESC LIMIT 0",
        "SELECT * FROM analytics.transactions WHERE status = 'FAILED' OR '1'='1' ORDER BY created_at DESC LIMIT 200",
        "SELECT * FROM analytics.transactions WHERE status = 'FAILED' UNION SELECT * FROM app_users ORDER BY created_at DESC LIMIT 200",
        "SELECT * FROM analytics.transactions WHERE status = 'FAILED' /*comment*/ ORDER BY created_at DESC LIMIT 200",
        "SELECT * FROM analytics.transactions WHERE status = $$FAILED$$ ORDER BY created_at DESC LIMIT 200",
        "SELECT * FROM analytics.transactions WHERE amount = '10' ORDER BY created_at DESC LIMIT 200",
        "SELECT * FROM analytics.transactions WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '99 months' ORDER BY created_at DESC LIMIT 200",
        "SELECT * FROM analytics.transactions ORDER BY created_at DESC LIMIT 200 FOR UPDATE",
        "SELECT * FROM analytics.transactions ORDER BY created_at DESC LIMIT 200\n",
        "SELECT * FROM analytics.transactions WHERE status = E'FAILED' ORDER BY created_at DESC LIMIT 200"
    }) void rejectsSqlOutsideTheGrammar(String sql){assertThatThrownBy(()->validator.validate(sql)).isInstanceOf(IllegalArgumentException.class);}
    @Test void unknownQuestionsFailInsteadOfRunningBroadQuery(){assertThatThrownBy(()->new NaturalLanguageSql(validator).generate("Show me everything")).isInstanceOf(IllegalArgumentException.class);}
    @Test void turkishIbanAndPeriodArePreserved(){String sql=new NaturalLanguageSql(validator).generate("Son 3 ay içerisinde TR720001002136978165000001 IBANına yapılan işlemleri getir");assertThat(sql).contains("iban = 'TR720001002136978165000001'","INTERVAL '3 months'");}
    @Test void injectionInQuestionCannotReachSql(){String sql=new NaturalLanguageSql(validator).generate("failed transactions; DROP TABLE transactions");assertThat(sql).isEqualTo("SELECT * FROM analytics.transactions WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT 200");}
}
