package io.supportops.controller;
import io.supportops.integration.SimulationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
@RestController
@ConditionalOnProperty(name="supportops.simulator.enabled",havingValue="true")
public class SimulationController {
    private final SimulationService service;private final String token;
    public SimulationController(SimulationService service,@Value("${supportops.simulator.token}")String token){this.service=service;this.token=token;}
    @PostMapping("/internal/simulate") public ResponseEntity<Map<String,Object>> simulate(@RequestHeader(value="X-Simulator-Token",defaultValue="")String supplied,@RequestBody SimulationService.Command command){
        if(!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var result=service.run(command);return ResponseEntity.status(result.get("httpStatus") instanceof Integer status?status:200).body(result);
    }
}
