package com.exportdocs.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "project")
public class Project {
    @Id private UUID id;
    @Version private Long version;
    @Column(nullable = false, length = 80) private String reference;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40) private ProjectStatus status;
    @Column(name = "destination_country_code", length = 2) private String destinationCountryCode;
    @Column(name = "expected_departure_date") private LocalDate expectedDepartureDate;
    @Column(length = 2000) private String notes;
    @Column(name = "created_by", updatable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Project() {}

    public static Project draft(String reference, String country, LocalDate departure, String notes, UUID creator) {
        Project project = new Project();
        project.id = UUID.randomUUID();
        project.reference = reference;
        project.status = ProjectStatus.DRAFT_DATA;
        project.destinationCountryCode = country;
        project.expectedDepartureDate = departure;
        project.notes = notes;
        project.createdBy = creator;
        project.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        project.updatedAt = project.createdAt;
        return project;
    }

    public boolean replaceSetup(String reference, String country, LocalDate departure, String notes) {
        boolean changed = false;
        if (!Objects.equals(this.reference, reference)) {
            this.reference = reference; changed = true;
        }
        if (!Objects.equals(destinationCountryCode, country)) {
            destinationCountryCode = country; changed = true;
        }
        if (!Objects.equals(expectedDepartureDate, departure)) {
            expectedDepartureDate = departure; changed = true;
        }
        if (!Objects.equals(this.notes, notes)) {
            this.notes = notes; changed = true;
        }
        return changed;
    }

    // Touch the aggregate when only a party or transport row changes, too.
    public void touch() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = now.isAfter(updatedAt) ? now : updatedAt.plus(1, ChronoUnit.MICROS);
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getReference() { return reference; }
    public ProjectStatus getStatus() { return status; }
    public String getDestinationCountryCode() { return destinationCountryCode; }
    public LocalDate getExpectedDepartureDate() { return expectedDepartureDate; }
    public String getNotes() { return notes; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
