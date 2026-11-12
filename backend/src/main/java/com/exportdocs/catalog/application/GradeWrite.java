package com.exportdocs.catalog.application;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.Set;

public record GradeWrite(Fields fields, Set<String> supplied, Long expectedVersion) {
    public record Fields(@Size(min = 1, max = 80) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/+-]{0,79}") String code, @Size(min = 1,
        max = 200) String name, Integer displayOrder, @DecimalMin(value = "0",
        inclusive = false) @Digits(integer = 15, fraction = 3) BigDecimal defaultCartonNetKg,
        @DecimalMin("0") @Digits(integer = 14, fraction = 4) BigDecimal defaultPricePerKg,
        @Pattern(regexp = "[A-Z]{3}") String defaultCurrency, Boolean active) {
    }
}
