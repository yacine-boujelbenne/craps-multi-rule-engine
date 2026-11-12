package com.exportdocs.party.api;

import com.exportdocs.party.domain.OrganizationRole;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class OrganizationDtos {
    private OrganizationDtos() {
    }

    public record Response(UUID id, long version, String legalName, String countryCode, String addressText,
        String registrationNumber, Set<OrganizationRole> roles, boolean active, Instant createdAt, Instant updatedAt) {
    }

    public record Page(List<Response> items, int page, int size, long totalElements, int totalPages) {
    }
}
