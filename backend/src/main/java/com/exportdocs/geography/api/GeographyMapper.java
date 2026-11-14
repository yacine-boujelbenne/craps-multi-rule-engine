package com.exportdocs.geography.api;

import com.exportdocs.geography.domain.City;
import com.exportdocs.geography.domain.Country;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GeographyMapper {
    GeographyDtos.CountryResponse country(Country country);
    GeographyDtos.CityResponse city(City city);
}
