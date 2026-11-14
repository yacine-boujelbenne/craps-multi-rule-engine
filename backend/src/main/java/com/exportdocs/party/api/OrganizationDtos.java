package com.exportdocs.party.api;

import com.exportdocs.party.domain.OrganizationRole;
import com.exportdocs.party.domain.OrganizationAddress;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class OrganizationDtos {
    private OrganizationDtos() {
    }

    public record Response(UUID id, long version, String legalName, String countryCode, String addressText,
        String registrationNumber, String tradeName, String taxIdentifier, String email, String phone,
        Set<OrganizationRole> roles, boolean active, Instant createdAt, Instant updatedAt,
        List<AddressResponse> addresses) {
    }

    public record Page(List<Response> items, int page, int size, long totalElements, int totalPages) {
    }

    public record AddressResponse(UUID id, UUID organizationId, OrganizationAddress.Kind kind, String countryCode,
        UUID cityId, String manualCity, String addressLine1, String addressLine2, String postalCode,
        boolean active, long version) {
    }
}
