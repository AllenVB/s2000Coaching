package com.s2000coaching.service;

import com.s2000coaching.domain.Lead;
import com.s2000coaching.domain.LeadStatus;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.LeadRepository;
import com.s2000coaching.web.dto.CreateLeadRequest;
import com.s2000coaching.web.dto.LeadResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-assessment requests. The public endpoint has no login, so it is guarded against bulk filling: a hidden
 * honeypot field (bots fill it, people never see it) and a small per-address limit. A request caught by the
 * honeypot is answered as a success and discarded, so a bot learns nothing from the reply.
 */
@Service
public class LeadService {

	static final int MAX_PER_HOUR = 5;

	private final LeadRepository leadRepository;
	private final Clock clock;
	private final HourlyLimiter limiter = new HourlyLimiter(MAX_PER_HOUR);

	public LeadService(LeadRepository leadRepository, Clock clock) {
		this.leadRepository = leadRepository;
		this.clock = clock;
	}

	@Transactional
	public void submit(CreateLeadRequest request, String clientAddress) {
		if (request.website() != null && !request.website().isBlank()) {
			return;
		}
		Instant now = Instant.now(clock);
		if (!limiter.tryAcquire(clientAddress, now)) {
			throw new BadRequestException("Çok fazla başvuru gönderdiniz. Lütfen biraz sonra tekrar deneyin.");
		}
		leadRepository.save(new Lead(UUID.randomUUID(), request.fullName().trim(), request.phone().trim(),
				blankToNull(request.email()), request.goal().trim(), blankToNull(request.level()), request.heightCm(),
				request.weightKg(), blankToNull(request.note()), now));
	}

	@Transactional(readOnly = true)
	public List<LeadResponse> list() {
		return leadRepository.findAllByOrderByCreatedAtDesc().stream().map(LeadResponse::from).toList();
	}

	@Transactional
	public LeadResponse setStatus(UUID id, LeadStatus status) {
		Lead lead = leadRepository.findById(id).orElseThrow(() -> new NotFoundException("Başvuru bulunamadı."));
		lead.setStatus(status);
		return LeadResponse.from(lead);
	}

	@Transactional
	public void delete(UUID id) {
		Lead lead = leadRepository.findById(id).orElseThrow(() -> new NotFoundException("Başvuru bulunamadı."));
		leadRepository.delete(lead);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
