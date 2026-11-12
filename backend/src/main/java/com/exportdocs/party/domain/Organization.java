package com.exportdocs.party.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record Organization(UUID id, long version, String legalName, String countryCode, String addressText,
    String registrationNumber, Set<OrganizationRole> roles, boolean active, Instant createdAt, Instant updatedAt) {
}
