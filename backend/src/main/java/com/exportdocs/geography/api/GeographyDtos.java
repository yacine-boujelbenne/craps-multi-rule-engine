package com.exportdocs.geography.api;

import java.util.List;
import java.util.UUID;

public final class GeographyDtos {
    private GeographyDtos() {}

    public record PortWrite(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=200) String name, @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="[A-Z]{2}") String countryCode) {}
    public record PortResponse(UUID id, String name, String countryCode, String unLocode, boolean active) {}
    public record CountryResponse(String code, String name) {}
    public record CityResponse(UUID id, String countryCode, String name, boolean active) {}
    public record CityPage(List<CityResponse> items, int page, int size, long totalElements, int totalPages) {}
}
