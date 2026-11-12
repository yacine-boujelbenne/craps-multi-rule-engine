package com.exportdocs.party.api;

import com.exportdocs.party.application.OrganizationService;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService service;
    private final OrganizationRequestParser parser;

    public OrganizationController(OrganizationService service, OrganizationRequestParser parser) {
        this.service = service;
        this.parser = parser;
    }

    @PostMapping
    public ResponseEntity<OrganizationDtos.Response> create(@RequestBody JsonNode body,
        @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        var response = service.create(parser.parse(body, false));
        return ResponseEntity.created(URI.create("/api/organizations/" + response.id())).body(response);
    }

    @GetMapping
    public OrganizationDtos.Page list(@RequestParam MultiValueMap<String, String> query) {
        return service.list(SetupListQuery.parse(query, true));
    }

    @GetMapping("/{id}")
    public OrganizationDtos.Response get(@PathVariable UUID id,
        @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public OrganizationDtos.Response patch(@PathVariable UUID id,
        @RequestBody JsonNode body, @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        return service.patch(id, parser.parse(body, true));
    }
}
