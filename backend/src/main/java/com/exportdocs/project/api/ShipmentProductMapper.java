package com.exportdocs.project.api;

import com.exportdocs.project.domain.ShipmentProduct;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ShipmentProductMapper {
    @Mapping(target = "projectVersion", source = "projectVersion")
    @Mapping(target = "quantity", expression = "java(decimal(product.quantity(), 3))")
    @Mapping(target = "netWeightKg", expression = "java(decimal(product.netWeightKg(), 3))")
    @Mapping(target = "grossWeightKg", expression = "java(decimal(product.grossWeightKg(), 3))")
    @Mapping(target = "unitPrice", expression = "java(decimal(product.unitPrice(), 4))")
    @Mapping(target = "appliedCartonNetKg", expression = "java(decimal(product.appliedCartonNetKg(), 3))")
    ShipmentProductResponse response(ShipmentProduct product, long projectVersion);

    default String decimal(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale).toPlainString();
    }
}
