package com.exportdocs.account.api;

import com.exportdocs.account.application.AssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AssignmentController {
    private final AssignmentService assignments;
    public AssignmentController(AssignmentService assignments) { this.assignments=assignments; }
    @GetMapping("/api/admin/projects/{projectId}/operators") public List<AccountDtos.Assignment> list(@PathVariable UUID projectId) {
        return assignments.list(projectId);
    }
    @PutMapping("/api/admin/projects/{projectId}/operators/{userId}") public AccountDtos.Assignment assign(
            @PathVariable UUID projectId,@PathVariable UUID userId,@Valid @RequestBody AccountDtos.Assign body) {
        return assignments.assign(projectId,userId,body);
    }
    @DeleteMapping("/api/admin/projects/{projectId}/operators/{userId}") public ResponseEntity<Void> remove(
            @PathVariable UUID projectId,@PathVariable UUID userId) {
        assignments.remove(projectId,userId);return ResponseEntity.noContent().build();
    }
    @GetMapping("/api/projects/{id}/permissions") public AccountDtos.Permissions permissions(@PathVariable UUID id) {
        return assignments.permissions(id);
    }
}
