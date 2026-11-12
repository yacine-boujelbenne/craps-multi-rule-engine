package com.exportdocs.project.api;

import com.exportdocs.project.domain.Project;
import com.exportdocs.project.infrastructure.ProjectDetailsStore.Details;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CommercialPartiesMapper {
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "supplierId", expression = "java(details.one(\"SUPPLIER\"))")
    @Mapping(target = "clientId", expression = "java(details.one(\"CLIENT\"))")
    @Mapping(target = "importerId", expression = "java(details.one(\"IMPORTER\"))")
    @Mapping(target = "consigneeId", expression = "java(details.one(\"CONSIGNEE\"))")
    CommercialPartiesResponse response(Project project, Details details);
}
