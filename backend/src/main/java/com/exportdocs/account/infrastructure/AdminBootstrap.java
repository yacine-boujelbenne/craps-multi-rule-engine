package com.exportdocs.account.infrastructure;

import com.exportdocs.account.api.AccountDtos;
import com.exportdocs.account.application.AccountRole;
import com.exportdocs.account.application.AccountService;
import jakarta.validation.Validator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("bootstrap-admin")
public class AdminBootstrap implements ApplicationRunner {
    private final AccountService accounts;
    private final Validator validator;
    private final ConfigurableApplicationContext context;
    private final AccountDtos.Create request;
    public AdminBootstrap(AccountService accounts,Validator validator,ConfigurableApplicationContext context,
            @Value("${BOOTSTRAP_ADMIN_USERNAME}") String username,@Value("${BOOTSTRAP_ADMIN_DISPLAY_NAME}") String displayName,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD}") String password) {
        this.accounts=accounts;this.validator=validator;this.context=context;
        request=new AccountDtos.Create(username,displayName,List.of(AccountRole.ADMIN),password);
    }
    @Override public void run(ApplicationArguments arguments) {
        if(!validator.validate(request).isEmpty()) throw new IllegalArgumentException("Invalid bootstrap account configuration.");
        accounts.bootstrap(request);
        SpringApplication.exit(context,()->0);
    }
}
