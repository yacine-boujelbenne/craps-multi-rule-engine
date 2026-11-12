package com.exportdocs.party.api;

import com.exportdocs.party.domain.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrganizationMapper {
    OrganizationDtos.Response response(Organization organization);
}
