package com.exportdocs.geography.domain;

import java.util.UUID;

public record City(UUID id, String countryCode, String name, boolean active) {}
