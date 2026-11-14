package com.exportdocs.project.api;

import com.exportdocs.project.application.ShipmentFormService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/forms")
public class ShipmentFormController {
    public record Write(@NotNull @Min(0) Long expectedProjectVersion,@NotNull JsonNode data) {}
    private final ShipmentFormService service;
    public ShipmentFormController(ShipmentFormService service) { this.service=service; }
    @GetMapping public ShipmentFormService.Forms get(@PathVariable UUID projectId) { return service.get(projectId); }
    @PutMapping("/{key}") public ShipmentFormService.Forms save(@PathVariable UUID projectId,@PathVariable String key,@Valid @RequestBody Write body) {
        return service.save(projectId,key,body.expectedProjectVersion(),body.data());
    }
}
