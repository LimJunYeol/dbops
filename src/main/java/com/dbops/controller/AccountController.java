package com.dbops.controller;

import com.dbops.dto.AccountDtos.*;
import com.dbops.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instances/{instanceId}")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** 대상 DB에 계정 생성 */
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createAccount(@PathVariable Long instanceId,
                                      @Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(instanceId, request);
    }

    /** 권한 부여 */
    @PostMapping("/grants")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse grant(@PathVariable Long instanceId,
                              @Valid @RequestBody GrantRequest request) {
        return accountService.grant(instanceId, request);
    }

    /** 작업 이력 조회 */
    @GetMapping("/tasks")
    public List<TaskResponse> tasks(@PathVariable Long instanceId) {
        return accountService.findTasks(instanceId);
    }
}