package io.supportops.controller;

import io.supportops.dto.ApiTypes.*;
import io.supportops.security.AuthService;
import io.supportops.service.*;
import io.supportops.ai.QueryService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final AuthService auth;private final TransactionService transactions;private final InvestigationService investigation;private final OperationsService operations;private final QueryService queries;
    public ApiController(AuthService auth,TransactionService transactions,InvestigationService investigation,OperationsService operations,QueryService queries){this.auth=auth;this.transactions=transactions;this.investigation=investigation;this.operations=operations;this.queries=queries;}
    @PostMapping("/auth/login") public Session login(@Valid @RequestBody Login input,HttpServletRequest request){return auth.login(input,request.getRemoteAddr());}
    @GetMapping("/dashboard") public Map<String,Object> dashboard(){return operations.dashboard();}
    @GetMapping("/transactions") public Map<String,Object> listTransactions(@RequestParam(defaultValue="0")int page){return transactions.list(page);}
    @GetMapping("/search") public Map<String,Object> search(@RequestParam String q,@RequestParam(defaultValue="auto")String type){return transactions.search(q,type);}
    @GetMapping("/transactions/{id}") public Map<String,Object> transaction(@PathVariable String id){return transactions.detail(id);}
    @PostMapping("/transactions/{id}/investigate") public Investigation investigate(@PathVariable String id)throws Exception{return investigation.investigate(id);}
    @GetMapping("/incidents") public List<Map<String,Object>> incidents(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="")String q){return operations.list("incidents",page,q);}
    @GetMapping("/incidents/{id}") public Map<String,Object> incident(@PathVariable String id){return operations.incident(id);}
    @PostMapping("/incidents") public Map<String,Object> create(@Valid @RequestBody NewIncident input){return operations.create(input);}
    @PatchMapping("/incidents/{id}") public void update(@PathVariable String id,@Valid @RequestBody IncidentUpdate input){operations.update(id,input);}
    @GetMapping("/logs") public List<Map<String,Object>> logs(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="")String q){return operations.list("logs",page,q);}
    @GetMapping("/audit") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> audit(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="")String q){return operations.list("audit",page,q);}
    @PostMapping("/ai-query/preview") public Preview preview(@Valid @RequestBody QueryPrompt input){return queries.preview(input.prompt());}
    @PostMapping("/ai-query/run") public QueryResult run(@Valid @RequestBody RunQuery input){return queries.run(input.previewId());}
}
