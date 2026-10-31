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

	/**
	 * Opens a new month. What a member has saved cannot be changed by the member afterwards — they are
	 * asked to be sure before saving — so a month that already exists is a conflict; only the coach
	 * corrects a row.
	 */
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
					+ " için zaten bir kayıt var. Girilen veriler sonradan değiştirilemez; düzeltme gerekiyorsa koçunuza yazın.");
		}

		Instant now = Instant.now();
		MonthlyRecord record = recordRepository.save(new MonthlyRecord(UUID.randomUUID(), member, request.period(), now));
		apply(record, request, now);
		return MonthlyRecordResponse.from(record);
	}

	/** The coach edits a row without knowing whose it is; the row says. */
	@Transactional
	public MonthlyRecordResponse updateAsCoach(UUID recordId, UpsertMonthlyRecordRequest request) {
		MonthlyRecord record = getOrThrow(recordId);
		requireFigures(request);
		apply(record, request, Instant.now());
		return MonthlyRecordResponse.from(record);
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

	private MonthlyRecord getOrThrow(UUID recordId) {
		return recordRepository.findById(recordId)
				.orElseThrow(() -> new NotFoundException("Kayıt bulunamadı: " + recordId));
	}

	/** "2026-10" as the "10/2026" the member sees. */
	static String label(String period) {
		return period.substring(5) + "/" + period.substring(0, 4);
	}
}
