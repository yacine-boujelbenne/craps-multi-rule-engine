package com.exportdocs.project.api;

import java.util.List;

public record ProjectPage(List<ProjectResponse> items, int page, int size, long totalElements, int totalPages) {}
