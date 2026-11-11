package com.exportdocs.project.infrastructure;

import com.exportdocs.project.application.ProjectActor;
import com.exportdocs.account.application.AccountIdentity;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProjectActorProvider {
    private final AccountIdentity identity;

    public ProjectActorProvider(AccountIdentity identity) { this.identity = identity; }

    public ProjectActor current() {
        var actor=identity.ready();
        return new ProjectActor(actor.id(),actor.roles().stream().map(Enum::name).collect(Collectors.toSet()));
    }
}
