package com.s2000coaching.web;

import com.s2000coaching.service.MessageTemplateService;
import com.s2000coaching.web.dto.MessageTemplateResponse;
import com.s2000coaching.web.dto.SaveMessageTemplateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Coach-only: the saved replies behind the composer's quick-reply chips. */
@RestController
public class AdminMessageTemplateController {

	private final MessageTemplateService templateService;

	public AdminMessageTemplateController(MessageTemplateService templateService) {
		this.templateService = templateService;
	}

	@GetMapping("/api/admin/message-templates")
	public List<MessageTemplateResponse> list() {
		return templateService.listTemplates();
	}

	@PostMapping("/api/admin/message-templates")
	@ResponseStatus(HttpStatus.CREATED)
	public MessageTemplateResponse create(@Valid @RequestBody SaveMessageTemplateRequest request) {
		return templateService.createTemplate(request.title(), request.body());
	}

	@PutMapping("/api/admin/message-templates/{templateId}")
	public MessageTemplateResponse update(@PathVariable UUID templateId,
			@Valid @RequestBody SaveMessageTemplateRequest request) {
		return templateService.updateTemplate(templateId, request.title(), request.body());
	}

	@DeleteMapping("/api/admin/message-templates/{templateId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID templateId) {
		templateService.deleteTemplate(templateId);
	}
}
