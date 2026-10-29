package com.bank.account.web;

import com.bank.account.application.AccountService;
import com.bank.account.domain.Account;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse get(@PathVariable String accountNumber) {
        return AccountResponse.from(service.get(accountNumber));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody OpenAccountRequest request) {
        return AccountResponse.from(service.open(request.accountNumber(), request.ownerName(), request.currency()));
    }

    public record OpenAccountRequest(
            @NotBlank @Pattern(regexp = "\\d{10,16}", message = "Số tài khoản gồm 10-16 chữ số") String accountNumber,
            @NotBlank String ownerName,
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "Mã tiền tệ ISO 4217, ví dụ VND") String currency) {
    }

    public record AccountResponse(String accountNumber, String ownerName, BigDecimal balance, String currency) {
        static AccountResponse from(Account a) {
            return new AccountResponse(a.getAccountNumber(), a.getOwnerName(), a.getBalance(), a.getCurrency());
        }
    }
}
