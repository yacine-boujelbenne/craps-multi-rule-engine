package com.exportdocs.account.api;

import com.exportdocs.account.application.AccountRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel="spring", unmappedTargetPolicy=ReportingPolicy.ERROR)
public interface AccountMapper { AccountDtos.User user(AccountRecord account); }
