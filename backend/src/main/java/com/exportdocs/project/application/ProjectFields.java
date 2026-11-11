package com.exportdocs.project.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record ProjectFields(
        @Size(min = 1, max = 80)
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9._/-]{0,79}$") String reference,
        UUID exporterId,
        @Size(max = 100) List<@NotNull UUID> producerIds,
        UUID consigneeId,
        UUID notifyPartyId,
        UUID freightForwarderId,
        UUID shippingLineId,
        @Pattern(regexp = "^[A-Z]{2}$") String destinationCountryCode,
        UUID portOfLoadingId,
        UUID portOfDischargeId,
        LocalDate expectedDepartureDate,
        @Size(max = 2000) String notes) {
    public ProjectFields {
        if (reference != null) reference = reference.trim().toUpperCase(Locale.ROOT);
    }
}
