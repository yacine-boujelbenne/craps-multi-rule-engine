package com.exportdocs.account.application;

import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

public record AccountPrincipal(UUID userId, long credentialVersion) implements Principal, Serializable {
    @Override public String getName() { return userId.toString(); }
}
