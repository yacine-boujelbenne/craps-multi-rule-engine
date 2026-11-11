package com.exportdocs.account.api;

import com.exportdocs.account.application.AccountRole;
import com.exportdocs.account.application.FormKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AccountDtos {
    private AccountDtos() {}
    private static String username(String value) { return value == null ? null : value.trim().toLowerCase(Locale.ROOT); }

    public record Login(@NotNull @Size(min=3,max=254) @Pattern(regexp="[a-z0-9][a-z0-9._@+-]{2,253}") String username,
            @NotNull @Size(min=1,max=64) String password) {
        public Login { username = AccountDtos.username(username); }
        @Override public String toString() { return "Login[credentials redacted]"; }
    }
    public record Create(@NotNull @Size(min=3,max=254) @Pattern(regexp="[a-z0-9][a-z0-9._@+-]{2,253}") String username,
            @NotBlank @Size(max=200) String displayName,
            @NotNull @Size(min=1,max=4) List<@NotNull AccountRole> roles,
            @NotNull @Size(min=12,max=64) String initialPassword) {
        public Create { username = AccountDtos.username(username); }
        @Override public String toString() { return "CreateAccount[credentials redacted]"; }
    }
    public record Update(@NotNull @PositiveOrZero Long expectedVersion,
            @Size(min=3,max=254) @Pattern(regexp="[a-z0-9][a-z0-9._@+-]{2,253}") String username,
            @Size(min=1,max=200) String displayName, Boolean active,
            @Size(min=1,max=4) List<@NotNull AccountRole> roles) {
        public Update { username = AccountDtos.username(username); }
    }
    public record Reset(@NotNull @PositiveOrZero Long expectedVersion,
            @NotNull @Size(min=12,max=64) String temporaryPassword) {
        @Override public String toString() { return "ResetPassword[credentials redacted]"; }
    }
    public record ChangePassword(@NotNull @Size(min=1,max=64) String currentPassword,
            @NotNull @Size(min=12,max=64) String newPassword) {
        @Override public String toString() { return "ChangePassword[credentials redacted]"; }
    }
    public record User(UUID id, String username, String displayName, boolean active, List<AccountRole> roles,
            boolean mustChangePassword, long version) {}
    public record UserPage(List<User> items, int page, int size, long totalElements, int totalPages) {}
    public record Assign(@NotNull @Size(max=10) List<@NotNull FormKey> formKeys) {}
    public record Assignment(UUID projectId, UUID userId, List<FormKey> formKeys) {}
    public record Permissions(UUID projectId, boolean canRead, boolean canManageAssignments, List<FormKey> editableForms) {}
}
