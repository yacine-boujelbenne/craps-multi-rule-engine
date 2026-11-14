package com.exportdocs.party.domain;

import java.time.Instant;
import java.util.Set;
import java.util.List;
import java.util.UUID;

public record Organization(UUID id, long version, String legalName, String countryCode, String addressText,
    String registrationNumber, String tradeName, String taxIdentifier, String email, String phone,
    Set<OrganizationRole> roles, boolean active, Instant createdAt, Instant updatedAt,
    List<OrganizationAddress> addresses) {
}
