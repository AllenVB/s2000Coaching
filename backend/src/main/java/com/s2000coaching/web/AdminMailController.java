package com.s2000coaching.web;

import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.service.MailTestService;
import com.s2000coaching.web.dto.MailTestResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminMailController {

	private final MailTestService mailTestService;
	private final CoachAuthService coachAuthService;

	public AdminMailController(MailTestService mailTestService, CoachAuthService coachAuthService) {
		this.mailTestService = mailTestService;
		this.coachAuthService = coachAuthService;
	}

	/** Sends a test e-mail to the signed-in coach and reports what the mail server said. */
	@PostMapping("/api/admin/mail/test")
	public MailTestResponse sendTest(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return mailTestService.sendTo(coachAuthService.requireCoach(authorization));
	}
}
