package io.supportops.security;

import io.supportops.dto.ApiTypes.*;
import io.supportops.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.*;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder passwords; private final JwtEncoder encoder; private final StringRedisTemplate redis;
    public AuthService(UserRepository users,PasswordEncoder passwords,JwtEncoder encoder,StringRedisTemplate redis){this.users=users;this.passwords=passwords;this.encoder=encoder;this.redis=redis;}
    public Session login(Login input, String clientAddress){
        String key="login:"+clientAddress;
        Long attempts=redis.opsForValue().increment(key);
        if(attempts!=null && attempts==1) redis.expire(key,Duration.ofMinutes(5));
        if(attempts!=null && attempts>30) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many login attempts; retry in five minutes");
        var user=users.findByEmail(input.email().toLowerCase()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"));
        if(!passwords.matches(input.password(),user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
        var now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("supportops").subject(user.getUserId()).issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("role",user.getRole()).build();
        var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Session(token,user.getName(),user.getRole());
    }
}
