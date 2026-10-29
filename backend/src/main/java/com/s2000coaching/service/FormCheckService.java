package com.s2000coaching.service;

import com.s2000coaching.domain.FormCheck;
import com.s2000coaching.domain.Member;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.FormCheckRepository;
import com.s2000coaching.web.dto.CreateFormCheckRequest;
import com.s2000coaching.web.dto.FormCheckResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Form checks: the member sends a link to a clip of themselves lifting, the coach watches it
 * and answers in writing. The clip stays on YouTube (the member uploads it as unlisted) — this
 * app stores only the link, never video.
 */
@Service
public class FormCheckService implements MemberDataEraser {

	private final FormCheckRepository formCheckRepository;
	private final MemberService memberService;
	private final Clock clock;

	public FormCheckService(FormCheckRepository formCheckRepository, MemberService memberService, Clock clock) {
		this.formCheckRepository = formCheckRepository;
		this.memberService = memberService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<FormCheckResponse> listForMember(UUID memberId) {
		return formCheckRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
				.map(FormCheckResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<FormCheckResponse> listForMemberAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return listForMember(memberId);
	}

	@Transactional
	public FormCheckResponse create(Member member, CreateFormCheckRequest request) {
		String canonical = YouTubeLink.parse(request.videoUrl())
				.map(YouTubeLink.Parsed::canonicalUrl)
				.orElseThrow(() -> new BadRequestException(
						"Video bağlantısı bir YouTube bağlantısı olmalı. Videoyu YouTube'a \"Listelenmemiş\" olarak yükleyip bağlantısını yapıştırın."));
		FormCheck check = formCheckRepository.save(new FormCheck(UUID.randomUUID(), member,
				request.exerciseName().trim(), canonical,
				request.note() == null || request.note().isBlank() ? null : request.note().trim(),
				Instant.now(clock)));
		return FormCheckResponse.from(check);
	}

	@Transactional
	public void deleteOwn(Member member, UUID id) {
		FormCheck check = formCheckRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Form kontrolü bulunamadı."));
		if (!check.getMember().getId().equals(member.getId())) {
			// Same answer as a missing row, so one member cannot probe for another's ids.
			throw new NotFoundException("Form kontrolü bulunamadı.");
		}
		formCheckRepository.delete(check);
	}

	@Transactional
	public FormCheckResponse reply(UUID id, String reply) {
		FormCheck check = formCheckRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Form kontrolü bulunamadı."));
		check.reply(reply.trim(), Instant.now(clock));
		return FormCheckResponse.from(check);
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		formCheckRepository.deleteAllByMemberId(memberId);
	}
}
