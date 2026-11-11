package com.exportdocs.account.application;

import java.util.List;
import java.util.UUID;

public record AccountRecord(UUID id, String username, String displayName, boolean active,
        List<AccountRole> roles, boolean mustChangePassword, long version,
        String passwordHash, long credentialVersion) {
    public boolean admin() { return roles.contains(AccountRole.ADMIN); }
    @Override public String toString() { return "Account[" + id + "]"; }
}
