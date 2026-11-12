package com.exportdocs.party.application;

import com.exportdocs.party.domain.OrganizationRole;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

public record OrganizationWrite(Fields fields, Set<String> supplied, Long expectedVersion) {
    public record Fields(@Size(min = 1, max = 250) String legalName,
        @Pattern(regexp = "[A-Z]{2}") String countryCode, @Size(max = 4000) String addressText,
        @Size(max = 100) String registrationNumber, Set<OrganizationRole> roles, Boolean active) {
    }
}
