package com.exportdocs.account.api;

import com.exportdocs.account.application.AccountService;
import com.exportdocs.project.application.ProjectProblem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminAccountController {
    private final AccountService accounts;
    private final ObjectMapper json;
    private final Validator validator;
    public AdminAccountController(AccountService accounts,ObjectMapper json,Validator validator) {
        this.accounts=accounts;this.json=json;this.validator=validator;
    }
    @PostMapping public ResponseEntity<AccountDtos.User> create(@Valid @RequestBody AccountDtos.Create body) {
        var user=accounts.create(body);return ResponseEntity.created(URI.create("/api/admin/users/"+user.id())).body(user);
    }
    @GetMapping("/{id}") public AccountDtos.User get(@PathVariable UUID id) { return accounts.get(id); }
    @GetMapping public AccountDtos.UserPage list(@RequestParam MultiValueMap<String,String> parameters) {
        for(var name:parameters.keySet()) if(!Set.of("page","size").contains(name) || parameters.get(name).size()!=1)
            throw ProjectProblem.field(400,name,"invalid","Unknown or repeated pagination parameter.");
        return accounts.list(integer(parameters,"page",0),integer(parameters,"size",20));
    }
    @PatchMapping("/{id}") public AccountDtos.User update(@PathVariable UUID id,@RequestBody JsonNode body) {
        if(!(body instanceof ObjectNode object)) throw ProjectProblem.field(400,"body","invalid","Expected a JSON object.");
        Set<String> supplied=new HashSet<>();object.fieldNames().forEachRemaining(supplied::add);
        for(String field:supplied) if(!Set.of("expectedVersion","username","displayName","roles","active").contains(field) || object.get(field).isNull())
            throw ProjectProblem.field(400,field,"invalid","Unknown field or null account property.");
        var version=object.get("expectedVersion");
        if(version==null || !version.isIntegralNumber() || !version.canConvertToLong())
            throw ProjectProblem.field(400,"expectedVersion","invalid","An integer version is required.");
        AccountDtos.Update update;
        try { update=json.treeToValue(object,AccountDtos.Update.class); }
        catch(JsonProcessingException exception) { throw ProjectProblem.field(400,"body","invalid","Invalid account fields."); }
        var errors=validator.validate(update).stream().map(v->new ProjectProblem.FieldError(v.getPropertyPath().toString(),"invalid",v.getMessage())).toList();
        if(!errors.isEmpty()) throw new ProjectProblem(400,"Invalid account fields.",errors);
        return accounts.update(id,update,supplied);
    }
    @PostMapping("/{id}/password") public AccountDtos.User reset(@PathVariable UUID id,@Valid @RequestBody AccountDtos.Reset body) {
        return accounts.reset(id,body);
    }
    private int integer(MultiValueMap<String,String> values,String field,int fallback) {
        if(!values.containsKey(field)) return fallback;
        try { return Integer.parseInt(values.getFirst(field)); }
        catch(NumberFormatException exception) { throw ProjectProblem.field(400,field,"invalid","Expected an integer."); }
    }
}
