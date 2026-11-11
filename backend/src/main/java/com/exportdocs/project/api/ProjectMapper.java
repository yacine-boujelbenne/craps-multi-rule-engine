package com.exportdocs.project.api;

import com.exportdocs.project.domain.Project;
import com.exportdocs.project.infrastructure.ProjectDetailsStore.Details;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProjectMapper {
    @Mapping(target = "exporterId", expression = "java(details.one(\"EXPORTER\"))")
    @Mapping(target = "producerIds", expression = "java(details.producers())")
    @Mapping(target = "consigneeId", expression = "java(details.one(\"CONSIGNEE\"))")
    @Mapping(target = "notifyPartyId", expression = "java(details.one(\"NOTIFY_PARTY\"))")
    @Mapping(target = "freightForwarderId", expression = "java(details.one(\"FREIGHT_FORWARDER\"))")
    @Mapping(target = "shippingLineId", expression = "java(details.one(\"SHIPPING_LINE\"))")
    @Mapping(target = "portOfLoadingId", source = "details.loading")
    @Mapping(target = "portOfDischargeId", source = "details.discharge")
    ProjectResponse response(Project project, Details details);
}
