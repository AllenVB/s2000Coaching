package com.s2000coaching.web;

import com.s2000coaching.service.AccountAuthService;
import com.s2000coaching.web.dto.AccountSessionResponse;
import com.s2000coaching.web.dto.MemberLoginRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The site's single "Giriş Yap": coach and members sign in through the same form. */
@RestController
public class AccountAuthController {

	private final AccountAuthService accountAuthService;

	public AccountAuthController(AccountAuthService accountAuthService) {
		this.accountAuthService = accountAuthService;
	}

	@PostMapping("/api/auth/login")
	public AccountSessionResponse login(@Valid @RequestBody MemberLoginRequest request) {
		return accountAuthService.login(request.email(), request.password());
	}
}
