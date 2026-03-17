package com.farvic.fintech.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.farvic.fintech.dto.account.AccountResponse;
import com.farvic.fintech.dto.account.DepositRequest;
import com.farvic.fintech.service.AccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(Authentication authentication) {
        return accountService.createAccount(authentication);
    }

    @GetMapping
    public List<AccountResponse> listMyAccounts(Authentication authentication) {
        return accountService.listMyAccounts(authentication);
    }

    @GetMapping("/{id}")
    public AccountResponse getMyAccountById(@PathVariable UUID id, Authentication authentication) {
        return accountService.getMyAccountById(id, authentication);
    }

    @PostMapping("/{id}/deposit")
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse deposit(@PathVariable UUID id,
                                @Valid @RequestBody DepositRequest request,
                                Authentication authentication) {
        return accountService.deposit(id, request, authentication);
    }
}