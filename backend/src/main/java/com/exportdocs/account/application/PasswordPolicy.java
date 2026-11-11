package com.exportdocs.account.application;

import com.exportdocs.project.application.ProjectProblem;
import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() {}
    public static void validate(String password,String field) {
        if(password==null || password.length()<12 || password.length()>64 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw ProjectProblem.field(400,field,"invalid_password","Use 12–64 characters and at most 72 UTF-8 bytes.");
    }
}
