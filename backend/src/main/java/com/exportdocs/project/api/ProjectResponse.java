package com.exportdocs.project.api;

import com.exportdocs.project.domain.ProjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(UUID id, long version, ProjectStatus status, String reference,
        UUID exporterId, List<UUID> producerIds, UUID consigneeId, UUID notifyPartyId,
        UUID freightForwarderId, UUID shippingLineId, String destinationCountryCode,
        UUID portOfLoadingId, UUID portOfDischargeId, LocalDate expectedDepartureDate,
        String notes, Instant createdAt, Instant updatedAt) {}
