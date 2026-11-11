package com.exportdocs.account;

import com.exportdocs.account.application.PasswordPolicy;
import com.exportdocs.project.application.ProjectProblem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @Test void acceptsSpacesWithoutTrimmingThem() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("  long password  ","password"));
    }
    @Test void rejectsTooShortAndTooLongPasswords() {
        for(String value:new String[]{"short","x".repeat(65)})
            assertThrows(ProjectProblem.class,()->PasswordPolicy.validate(value,"password"));
    }
    @Test void enforcesBcryptByteLimitForUnicodePasswords() {
        assertDoesNotThrow(()->PasswordPolicy.validate("é".repeat(36),"password"));
        assertThrows(ProjectProblem.class,()->PasswordPolicy.validate("é".repeat(37),"password"));
    }
}
