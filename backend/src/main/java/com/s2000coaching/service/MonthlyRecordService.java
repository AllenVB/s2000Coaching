package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MonthlyRecord;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.MonthlyRecordRepository;
import com.s2000coaching.web.dto.MonthlyRecordResponse;
import com.s2000coaching.web.dto.UpsertMonthlyRecordRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The monthly tables ("Verilerim"): one set of body figures per member per month. The member
 * fills them in; the coach can read, add, correct and delete them on the member's behalf.
 *
 * <p>Body figures are health data, so — like measurements — nothing is written before the member
 * has consented to that. Deleting is always allowed: withdrawing data must not depend on consent.
 */
@Service
public class MonthlyRecordService implements MemberDataEraser {

	private final MonthlyRecordRepository recordRepository;
	private final MemberService memberService;
	private final Clock clock;

	public MonthlyRecordService(MonthlyRecordRepository recordRepository, MemberService memberService, Clock clock) {
		this.recordRepository = recordRepository;
		this.memberService = memberService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<MonthlyRecordResponse> list(UUID memberId) {
		return recordRepository.findAllByMemberIdOrderByPeriodDesc(memberId).stream()
				.map(MonthlyRecordResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MonthlyRecordResponse> listAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return list(memberId);
	}

	/** Opens a new month. A month that already exists is a conflict: it is edited, not re-created. */
	@Transactional
	public MonthlyRecordResponse create(UUID memberId, UpsertMonthlyRecordRequest request) {
		Member member = memberService.requireMember(memberId);
		requireConsent(member);
		requireFigures(request);
		if (request.period() == null) {
			throw new BadRequestException("Ay seçin.");
		}
		if (YearMonth.parse(request.period()).isAfter(YearMonth.now(clock))) {
			throw new BadRequestException("Gelecekteki bir ay için veri girilemez.");
		}
		if (recordRepository.findByMemberIdAndPeriod(memberId, request.period()).isPresent()) {
			throw new ConflictException(label(request.period())
					+ " için zaten bir kayıt var; kalem simgesiyle düzenleyebilirsiniz.");
		}

		Instant now = Instant.now();
		MonthlyRecord record = recordRepository.save(new MonthlyRecord(UUID.randomUUID(), member, request.period(), now));
		apply(record, request, now);
		return MonthlyRecordResponse.from(record);
	}

	/** Replaces the whole table of an existing month; the month itself never changes. */
	@Transactional
	public MonthlyRecordResponse update(UUID memberId, UUID recordId, UpsertMonthlyRecordRequest request) {
		MonthlyRecord record = ownedBy(memberId, recordId);
		requireConsent(record.getMember());
		requireFigures(request);
		apply(record, request, Instant.now());
		return MonthlyRecordResponse.from(record);
	}

	@Transactional
	public void delete(UUID memberId, UUID recordId) {
		recordRepository.delete(ownedBy(memberId, recordId));
	}

	/** The coach edits a row without knowing whose it is; the row says. */
	@Transactional
	public MonthlyRecordResponse updateAsCoach(UUID recordId, UpsertMonthlyRecordRequest request) {
		MonthlyRecord record = getOrThrow(recordId);
		return update(record.getMember().getId(), recordId, request);
	}

	@Transactional
	public void deleteAsCoach(UUID recordId) {
		recordRepository.delete(getOrThrow(recordId));
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		recordRepository.deleteAllByMemberId(memberId);
	}

	private void apply(MonthlyRecord record, UpsertMonthlyRecordRequest r, Instant now) {
		record.setFigures(r.heightCm(), r.weightKg(), r.shoulderCm(), r.chestCm(), r.armCm(), r.neckCm(),
				r.waistCm(), r.hipCm(), r.thighCm(), now);
	}

	private static void requireConsent(Member member) {
		if (!member.hasHealthDataConsent()) {
			throw new BadRequestException(
					"Veri girebilmek için sağlık verisi işleme onayının verilmiş olması gerekiyor.");
		}
	}

	private static void requireFigures(UpsertMonthlyRecordRequest request) {
		if (!request.hasAnyFigure()) {
			throw new BadRequestException("En az bir değer girin.");
		}
	}

	/** Someone else's row answers exactly like a missing one, so ids cannot be probed. */
	private MonthlyRecord ownedBy(UUID memberId, UUID recordId) {
		MonthlyRecord record = getOrThrow(recordId);
		if (!record.getMember().getId().equals(memberId)) {
			throw new NotFoundException("Kayıt bulunamadı: " + recordId);
		}
		return record;
	}

	private MonthlyRecord getOrThrow(UUID recordId) {
		return recordRepository.findById(recordId)
				.orElseThrow(() -> new NotFoundException("Kayıt bulunamadı: " + recordId));
	}

	/** "2026-10" as the "10/2026" the member sees. */
	static String label(String period) {
		return period.substring(5) + "/" + period.substring(0, 4);
	}
}
