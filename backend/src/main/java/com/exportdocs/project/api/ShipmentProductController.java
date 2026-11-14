package com.exportdocs.project.api;

import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.project.application.ShipmentProductService;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/products")
public class ShipmentProductController {
    private final ShipmentProductService service;
    private final ShipmentProductRequestParser parser;

    public ShipmentProductController(ShipmentProductService service, ShipmentProductRequestParser parser) {
        this.service = service;
        this.parser = parser;
    }

    @GetMapping
    public List<ShipmentProductResponse> list(@PathVariable UUID projectId, @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        return service.list(projectId);
    }

    @PostMapping
    public ResponseEntity<ShipmentProductResponse> create(@PathVariable UUID projectId, @RequestBody JsonNode body,
        @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        var response = service.create(projectId, parser.parse(body, false));
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/products/" + response.id())).body(response);
    }

    @PatchMapping("/{projectProductId}")
    public ShipmentProductResponse patch(@PathVariable UUID projectId, @PathVariable UUID projectProductId,
        @RequestBody JsonNode body, @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        return service.patch(projectId, projectProductId, parser.parse(body, true));
    }
}
