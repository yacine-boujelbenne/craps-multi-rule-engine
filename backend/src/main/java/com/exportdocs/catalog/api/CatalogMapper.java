package com.exportdocs.catalog.api;

import com.exportdocs.catalog.domain.Product;
import com.exportdocs.catalog.domain.Grade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CatalogMapper {
    CatalogDtos.ProductResponse product(Product product);
    @Mapping(target = "defaultCartonNetKg", expression = "java(grade.defaultCartonNetKg()==null?null:grade.defaultCartonNetKg().setScale(3).toPlainString())")
    @Mapping(target = "defaultPricePerKg", expression = "java(grade.defaultPricePerKg()==null?null:grade.defaultPricePerKg().setScale(4).toPlainString())")
    CatalogDtos.GradeResponse grade(Grade grade);
}
