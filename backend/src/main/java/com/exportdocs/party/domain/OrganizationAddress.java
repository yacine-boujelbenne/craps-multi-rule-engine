package com.exportdocs.party.domain;

import java.util.UUID;

public record OrganizationAddress(UUID id, UUID organizationId, Kind kind, String countryCode, UUID cityId,
    String manualCity, String addressLine1, String addressLine2, String postalCode, boolean active, long version) {
    public enum Kind { BUSINESS, FACTORY, BILLING, OTHER }
}
