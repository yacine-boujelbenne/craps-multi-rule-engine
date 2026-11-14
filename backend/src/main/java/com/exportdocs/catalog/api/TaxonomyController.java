package com.exportdocs.catalog.api;

import com.exportdocs.catalog.application.CatalogService;
import com.exportdocs.party.api.SetupJson;
import com.exportdocs.party.api.SetupListQuery;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-taxonomy-categories")
public class TaxonomyController {
    private final CatalogService service;
    private final CatalogRequestParser parser;

    public TaxonomyController(CatalogService service, CatalogRequestParser parser) {
        this.service = service;
        this.parser = parser;
    }

    @GetMapping
    public CatalogDtos.TaxonomyPage list(@RequestParam MultiValueMap<String, String> query) {
        String q = query.getFirst("q");
        if (q != null && q.length() > 200) throw SetupJson.invalid("q", "Prefix must contain 1 to 200 characters.");
        return service.taxonomy(SetupListQuery.parse(query, false));
    }

    @PostMapping
    public ResponseEntity<CatalogDtos.TaxonomyResponse> create(@RequestBody JsonNode body,
        @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        var response = service.createTaxonomy(parser.taxonomy(body, false));
        return ResponseEntity.created(URI.create("/api/product-taxonomy-categories/" + response.id())).body(response);
    }

    @PatchMapping("/{categoryId}")
    public CatalogDtos.TaxonomyResponse patch(@PathVariable UUID categoryId, @RequestBody JsonNode body,
        @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
        return service.patchTaxonomy(categoryId, parser.taxonomy(body, true));
    }
}
