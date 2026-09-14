package io.supportops.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean JwtEncoder encoder(@Value("${supportops.jwt-secret}") String secret) {
        if(secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT secret must contain at least 32 bytes");
        return new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
    }
    @Bean JwtDecoder decoder(@Value("${supportops.jwt-secret}") String secret) {
        var decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("supportops"));
        return decoder;
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var granted=new JwtGrantedAuthoritiesConverter(); granted.setAuthoritiesClaimName("role"); granted.setAuthorityPrefix("ROLE_");
        var converter=new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(granted);
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers("/api/auth/login","/actuator/health","/actuator/prometheus","/internal/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/**").hasAnyRole("ADMIN","SUPPORT_ENGINEER","VIEWER")
                .requestMatchers("/api/**").hasAnyRole("ADMIN","SUPPORT_ENGINEER").anyRequest().denyAll())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
    }
}
