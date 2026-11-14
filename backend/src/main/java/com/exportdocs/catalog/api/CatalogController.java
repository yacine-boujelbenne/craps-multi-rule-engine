package com.exportdocs.catalog.api;

import com.exportdocs.catalog.application.CatalogService;
import com.exportdocs.party.api.SetupListQuery;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import com.exportdocs.party.api.SetupJson;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class CatalogController {
    private final CatalogService service;
    private final CatalogRequestParser parser;

    public CatalogController(CatalogService service, CatalogRequestParser parser) {
        this.service = service;
        this.parser = parser;
    }

    @PostMapping
    public ResponseEntity<CatalogDtos.ProductResponse> createProduct(@RequestBody JsonNode body,
        @RequestParam MultiValueMap<String, String> query) {
        none(query);
        var response = service.createProduct(parser.product(body, false));
        return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
    }

    @GetMapping
    public CatalogDtos.ProductPage products(@RequestParam(required = false) UUID categoryId,
        @RequestParam MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of("q", "page", "size", "includeInactive", "categoryId"));
        if (query.containsKey("categoryId") && categoryId == null) throw SetupJson.invalid("categoryId", "A canonical UUID is required.");
        var paging = new LinkedMultiValueMap<>(query);
        paging.remove("categoryId");
        return service.products(categoryId, SetupListQuery.parse(paging, false));
    }

    @GetMapping("/{id}")
    public CatalogDtos.ProductResponse product(@PathVariable UUID id,
        @RequestParam MultiValueMap<String, String> query) {
        none(query);
        return service.getProduct(id);
    }

    @PatchMapping("/{id}")
    public CatalogDtos.ProductResponse patchProduct(@PathVariable UUID id,
        @RequestBody JsonNode body, @RequestParam MultiValueMap<String, String> query) {
        none(query);
        return service.patchProduct(id, parser.product(body, true));
    }

    @PostMapping("/{productId}/grades")
    public ResponseEntity<CatalogDtos.GradeResponse> createGrade(@PathVariable UUID productId,
        @RequestBody JsonNode body, @RequestParam MultiValueMap<String, String> query) {
        none(query);
        var response = service.createGrade(productId, parser.grade(body, false));
        return ResponseEntity.created(URI.create("/api/products/" + productId + "/grades/" + response.id())).body(response);
    }

    @GetMapping("/{productId}/grades")
    public CatalogDtos.GradePage grades(@PathVariable UUID productId,
        @RequestParam MultiValueMap<String, String> query) {
        return service.grades(productId, SetupListQuery.parse(query, false));
    }

    @GetMapping("/{productId}/grades/{id}")
    public CatalogDtos.GradeResponse grade(@PathVariable UUID productId,
        @PathVariable UUID id, @RequestParam MultiValueMap<String, String> query) {
        none(query);
        return service.getGrade(productId, id);
    }

    @PatchMapping("/{productId}/grades/{id}")
    public CatalogDtos.GradeResponse patchGrade(@PathVariable UUID productId,
        @PathVariable UUID id, @RequestBody JsonNode body, @RequestParam MultiValueMap<String, String> query) {
        none(query);
        return service.patchGrade(productId, id, parser.grade(body, true));
    }

    @GetMapping("/{productId}/taxonomy-categories")
    public List<CatalogDtos.TaxonomyResponse> assignedCategories(@PathVariable UUID productId,
        @RequestParam MultiValueMap<String, String> query) {
        none(query);
        return service.assignedCategories(productId);
    }

    @PutMapping("/{productId}/taxonomy-categories")
    public List<CatalogDtos.TaxonomyResponse> replaceCategories(@PathVariable UUID productId, @RequestBody JsonNode body,
        @RequestParam MultiValueMap<String, String> query) {
        none(query);
        return service.replaceCategories(productId, parser.assignment(body));
    }

    private void none(MultiValueMap<String, String> query) {
        SetupListQuery.rejectUnknown(query, Set.of());
    }
}
