package com.exportdocs.project.api;

import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.application.ProjectService;
import com.exportdocs.project.domain.ProjectStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
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
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;
    private final ProjectRequestParser parser;

    public ProjectController(ProjectService service, ProjectRequestParser parser) {
        this.service = service;
        this.parser = parser;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@RequestBody JsonNode body) {
        var project = service.create(parser.create(body));
        return ResponseEntity.created(URI.create("/api/projects/" + project.id())).body(project);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable UUID id) { return service.get(id); }

    @PatchMapping("/{id}")
    public ProjectResponse patch(@PathVariable UUID id, @RequestBody JsonNode body) {
        return service.patch(id, parser.patch(body));
    }

    @GetMapping
    public ProjectPage list(@RequestParam MultiValueMap<String, String> parameters) {
        for (String name : parameters.keySet()) {
            if (!Set.of("page", "size", "status", "q").contains(name) || parameters.get(name).size() != 1) {
                throw ProjectProblem.field(400, name, "invalid", "Unknown or repeated listing parameter.");
            }
        }
        ProjectStatus status = null;
        if (parameters.containsKey("status")) {
            String value = parameters.getFirst("status");
            if (value == null) throw ProjectProblem.field(400, "status", "invalid", "Unknown project status.");
            try { status = ProjectStatus.valueOf(value); }
            catch (IllegalArgumentException exception) {
                throw ProjectProblem.field(400, "status", "invalid", "Unknown project status.");
            }
        }
        return service.list(integer(parameters, "page", 0), integer(parameters, "size", 20), status, parameters.getFirst("q"));
    }

    private int integer(MultiValueMap<String, String> parameters, String name, int fallback) {
        String value = parameters.getFirst(name);
        if (!parameters.containsKey(name)) return fallback;
        if (value == null) throw ProjectProblem.field(400, name, "invalid", "Expected an integer.");
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw ProjectProblem.field(400, name, "invalid", "Expected an integer."); }
    }
}
