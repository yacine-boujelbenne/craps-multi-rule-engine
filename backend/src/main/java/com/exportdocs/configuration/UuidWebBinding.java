package com.exportdocs.configuration;

import java.beans.PropertyEditorSupport;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class UuidWebBinding {
    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @InitBinder
    public void bindUuids(WebDataBinder binder) {
        // Override Spring's lenient UUID editor as well as UUID.fromString's shortened-group parsing.
        binder.registerCustomEditor(UUID.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String value) {
                if (value == null || !UUID_TEXT.matcher(value).matches()) {
                    throw new IllegalArgumentException("Expected a full hyphenated UUID.");
                }
                setValue(UUID.fromString(value));
            }
        });
    }
}
