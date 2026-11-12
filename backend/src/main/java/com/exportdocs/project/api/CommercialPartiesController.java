package com.exportdocs.project.api;

import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.project.application.CommercialPartiesService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{id}/commercial-parties")
public class CommercialPartiesController {
    private final CommercialPartiesService service;
    private final CommercialPartiesRequestParser parser;

    public CommercialPartiesController(CommercialPartiesService service, CommercialPartiesRequestParser parser) {
        this.service = service;
        this.parser = parser;
    }

    @GetMapping
    public CommercialPartiesResponse get(@PathVariable UUID id, @RequestParam MultiValueMap<String,
        String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        return service.get(id);
    }

    @PatchMapping
    public CommercialPartiesResponse patch(@PathVariable UUID id, @RequestBody JsonNode body,
        @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        return service.patch(id, parser.parse(body));
    }
}
