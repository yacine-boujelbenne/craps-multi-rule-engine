package com.exportdocs.party.api;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.MultiValueMap;

public record SetupListQuery(int page, int size, String prefix, boolean includeInactive, String role) {
    public static SetupListQuery parse(MultiValueMap<String, String> parameters, boolean organization) {
        var allowed = new HashSet<>(Set.of("page", "size", "q",
            "includeInactive"));
        if (organization)allowed.add("role");
        rejectUnknown(parameters, allowed);
        int page = integer(parameters, "page", 0), size = integer(parameters, "size", 20);
        if (page<0 || (long)page*size>Integer.MAX_VALUE) throw SetupJson.invalid("page", "Page offset is invalid.");
        if (size<1 || size>100) throw SetupJson.invalid("size", "Size must be between 1 and 100.");
        String q = parameters.getFirst("q"), prefix = null;
        if (parameters.containsKey("q")) {
            if (q == null || q.trim().isEmpty() || q.trim().length()>250) throw SetupJson.invalid("q",
                "Prefix must contain 1 to 250 characters.");
            prefix = q.trim().toLowerCase(Locale.ROOT).replace("!",
                "!!").replace("%", "!%").replace("_",
                "!_") + "%";
        }
        String active = parameters.getFirst("includeInactive");
        if (parameters.containsKey("includeInactive") && !"true".equals(active) && !"false".equals(active)) throw SetupJson.invalid("includeInactive",
            "Expected true or false.");
        String role = parameters.getFirst("role");
        if (parameters.containsKey("role")) {
            try {
                com.exportdocs.party.domain.OrganizationRole.valueOf(role);
            } catch (IllegalArgumentException|NullPointerException exception) {
                throw SetupJson.invalid("role", "Unknown organization role.");
            }
        }
        return new SetupListQuery(page, size, prefix, "true".equals(active), role);
    }

    public static void rejectUnknown(MultiValueMap<String, String> parameters, Set<String> allowed) {
        for (var entry : parameters.entrySet()) if (!allowed.contains(entry.getKey()) || entry.getValue().size() != 1) throw SetupJson.invalid(entry.getKey(),
            "Unknown or repeated query parameter.");
    }

    private static int integer(MultiValueMap<String, String> parameters, String field, int fallback) {
        if (!parameters.containsKey(field)) return fallback;
        String value = parameters.getFirst(field);
        if (value == null || !value.matches("[0-9]+")) throw SetupJson.invalid(field, "Expected a nonnegative integer.");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw SetupJson.invalid(field, "Integer is out of range.");
        }
    }
}
