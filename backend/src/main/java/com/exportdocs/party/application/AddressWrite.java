package com.exportdocs.party.application;

import com.exportdocs.party.domain.OrganizationAddress;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record AddressWrite(Fields fields, Set<String> supplied, Long expectedVersion, Long expectedOrganizationVersion) {
    public record Fields(OrganizationAddress.Kind kind, @Pattern(regexp = "[A-Z]{2}") String countryCode,
        UUID cityId, @Size(max = 200) String manualCity, @Size(min = 1, max = 250) String addressLine1,
        @Size(max = 250) String addressLine2, @Size(max = 40) String postalCode, Boolean active) {
    }
}
