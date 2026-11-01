package com.s2000coaching.service;

import com.s2000coaching.domain.LeadStatus;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.mail.NotificationMailSender;
import com.s2000coaching.repository.CheckInRepository;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.LeadRepository;
import com.s2000coaching.repository.MemberIntakeRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.OrderRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every Monday morning the coach gets one e-mail: how the last week went (records members entered, sales,
 * requests) and who needs attention. Like the other mail, nothing is sent, and nothing is pretended to be
 * sent, when no SMTP server is configured.
 */
@Service
public class WeeklySummaryService {

	private static final Logger log = LoggerFactory.getLogger(WeeklySummaryService.class);
	private static final int ATTENTION_LINES = 8;

	private final MemberRepository memberRepository;
	private final WorkoutLogRepository workoutLogRepository;
	private final MemberIntakeRepository intakeRepository;
	private final CheckInRepository checkInRepository;
	private final LeadRepository leadRepository;
	private final OrderRepository orderRepository;
	private final CoachAccountRepository coachAccountRepository;
	private final AttentionService attentionService;
	private final NotificationMailSender mailSender;
	private final Clock clock;
	private final String siteUrl;
	private final boolean enabled;

	public WeeklySummaryService(MemberRepository memberRepository, WorkoutLogRepository workoutLogRepository,
			MemberIntakeRepository intakeRepository, CheckInRepository checkInRepository, LeadRepository leadRepository,
			OrderRepository orderRepository, CoachAccountRepository coachAccountRepository,
			AttentionService attentionService, NotificationMailSender mailSender, Clock clock,
			@Value("${app.public-url}") String siteUrl, @Value("${app.reminders.enabled:true}") boolean enabled) {
		this.memberRepository = memberRepository;
		this.workoutLogRepository = workoutLogRepository;
		this.intakeRepository = intakeRepository;
		this.checkInRepository = checkInRepository;
		this.leadRepository = leadRepository;
		this.orderRepository = orderRepository;
		this.coachAccountRepository = coachAccountRepository;
		this.attentionService = attentionService;
		this.mailSender = mailSender;
		this.clock = clock;
		this.siteUrl = siteUrl.replaceAll("/+$", "");
		this.enabled = enabled;
	}

	@Scheduled(cron = "${app.weekly-summary.cron:0 30 8 * * MON}", zone = "${app.time-zone:Europe/Istanbul}")
	public void runScheduled() {
		send();
	}

	/** @return how many coaches the e-mail went to */
	@Transactional(readOnly = true)
	public int send() {
		if (!enabled) {
			return 0;
		}
		if (!mailSender.delivers()) {
			log.info("Haftalık özet atlandı: SMTP yapılandırılmadı (spring.mail.host).");
			return 0;
		}
		String body = WeeklySummary.build(numbers());
		int sent = 0;
		for (var coach : coachAccountRepository.findAll()) {
			if (mailSender.send(coach.getEmail(), "S2000 haftalık özet", body)) {
				sent++;
			}
		}
		log.info("Haftalık özet {} koça gönderildi.", sent);
		return sent;
	}

	WeeklySummary.Numbers numbers() {
		LocalDate today = LocalDate.now(clock);
		LocalDate from = today.minusDays(7);
		var fromInstant = from.atStartOfDay(clock.getZone()).toInstant();
		// The check-ins that belong to the week that just ended start on its Monday.
		LocalDate lastMonday = today.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
		Object[] sales = orderRepository.paidSince(fromInstant).get(0);
		var attention = attentionService.load().items();
		List<String> lines = attention.stream().limit(ATTENTION_LINES)
				.map(item -> item.fullName() + ": " + String.join(", ", item.reasons().stream().map(r -> r.label()).toList()))
				.toList();
		return new WeeklySummary.Numbers(
				memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.ACTIVE).size(),
				memberRepository.countByCreatedAtGreaterThanEqual(fromInstant),
				workoutLogRepository.countByPerformedOnGreaterThanEqual(from),
				intakeRepository.countByConsumedOnGreaterThanEqual(from),
				checkInRepository.countByWeekStartGreaterThanEqual(lastMonday),
				leadRepository.countByCreatedAtGreaterThanEqual(fromInstant),
				leadRepository.countByStatus(LeadStatus.NEW),
				((Number) sales[0]).longValue(), ((Number) sales[1]).longValue(),
				lines, attention.size(), siteUrl);
	}
}
