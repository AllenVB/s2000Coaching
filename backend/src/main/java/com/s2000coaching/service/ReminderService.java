package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.ReminderLog;
import com.s2000coaching.mail.NotificationMailSender;
import com.s2000coaching.repository.CheckInRepository;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MembershipRepository;
import com.s2000coaching.repository.ReminderLogRepository;
import com.s2000coaching.service.ReminderPlanner.Reminder;
import com.s2000coaching.service.ReminderPlanner.Signals;
import com.s2000coaching.web.dto.AttentionResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily reminder e-mails to members, and a summary to the coach.
 *
 * <p>Does nothing at all while SMTP is not configured, rather than running and logging reminders
 * as sent: a reminder is only recorded once a mail server has taken it, so turning SMTP on later
 * sends everything that is then due instead of finding it already "done".
 */
@Service
public class ReminderService implements MemberDataEraser {

	private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

	private final MemberRepository memberRepository;
	private final MeasurementRepository measurementRepository;
	private final CheckInRepository checkInRepository;
	private final MembershipRepository membershipRepository;
	private final ReminderLogRepository reminderLogRepository;
	private final CoachAccountRepository coachAccountRepository;
	private final AttentionService attentionService;
	private final NotificationMailSender mailSender;
	private final Clock clock;
	private final String siteUrl;
	private final boolean enabled;

	public ReminderService(MemberRepository memberRepository, MeasurementRepository measurementRepository,
			CheckInRepository checkInRepository, MembershipRepository membershipRepository,
			ReminderLogRepository reminderLogRepository, CoachAccountRepository coachAccountRepository,
			AttentionService attentionService, NotificationMailSender mailSender, Clock clock,
			@Value("${app.public-url}") String siteUrl, @Value("${app.reminders.enabled:true}") boolean enabled) {
		this.memberRepository = memberRepository;
		this.measurementRepository = measurementRepository;
		this.checkInRepository = checkInRepository;
		this.membershipRepository = membershipRepository;
		this.reminderLogRepository = reminderLogRepository;
		this.coachAccountRepository = coachAccountRepository;
		this.attentionService = attentionService;
		this.mailSender = mailSender;
		this.clock = clock;
		this.siteUrl = siteUrl.replaceAll("/+$", "");
		this.enabled = enabled;
	}

	@Scheduled(cron = "${app.reminders.cron:0 0 9 * * *}", zone = "Europe/Istanbul")
	public void runScheduled() {
		run();
	}

	/** @return how many e-mails were sent */
	@Transactional
	public int run() {
		if (!enabled) {
			return 0;
		}
		if (!mailSender.delivers()) {
			log.info("Hatırlatmalar atlandı: SMTP yapılandırılmadı (spring.mail.host).");
			return 0;
		}
		LocalDate today = LocalDate.now(clock);
		ZoneId zone = clock.getZone();

		Map<UUID, LocalDate> lastMeasurement = dates(measurementRepository.latestMeasuredOnPerMember());
		Map<UUID, LocalDate> lastCheckIn = dates(checkInRepository.latestWeekPerMember());
		Map<UUID, LocalDate> membershipEnd = dates(membershipRepository.latestEndPerMember());
		Set<String> alreadySent = new HashSet<>();
		for (Object[] row : reminderLogRepository.allSent()) {
			alreadySent.add(row[0] + "|" + row[1] + "|" + row[2]);
		}

		int sent = 0;
		for (Member member : memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.ACTIVE)) {
			var signals = new Signals(member.getFullName(), member.isRemindersEnabled(),
					member.hasHealthDataConsent(), (member.getVerifiedAt() != null ? member.getVerifiedAt() : member.getCreatedAt())
							.atZone(zone).toLocalDate(),
					lastMeasurement.get(member.getId()), lastCheckIn.get(member.getId()), membershipEnd.get(member.getId()));
			for (Reminder reminder : ReminderPlanner.plan(signals, today, siteUrl)) {
				String key = member.getId() + "|" + reminder.kind() + "|" + reminder.occasion();
				if (alreadySent.contains(key)) {
					continue;
				}
				if (mailSender.send(member.getEmail(), reminder.subject(), reminder.body())) {
					reminderLogRepository.save(new ReminderLog(UUID.randomUUID(), member, reminder.kind(),
							reminder.occasion(), Instant.now(clock)));
					sent++;
				}
			}
		}
		sent += sendCoachDigest();
		log.info("Hatırlatma çalışması bitti: {} e-posta gönderildi.", sent);
		return sent;
	}

	private int sendCoachDigest() {
		AttentionResponse attention = attentionService.load();
		if (attention.items().isEmpty() && attention.pendingTransfers().isEmpty()) {
			return 0;
		}
		StringBuilder body = new StringBuilder("Günaydın,\n\n");
		if (!attention.pendingTransfers().isEmpty()) {
			body.append(attention.pendingTransfers().size()).append(" havale onayınızı bekliyor:\n");
			attention.pendingTransfers().forEach(order -> body.append("  - ").append(order.customerName())
					.append(" (").append(order.orderNumber()).append(")\n"));
			body.append('\n');
		}
		if (!attention.items().isEmpty()) {
			body.append("İlgi bekleyen üyeler (en acilden):\n");
			attention.items().stream().limit(10).forEach(item -> {
				body.append("  - ").append(item.fullName()).append(": ");
				body.append(String.join(", ", item.reasons().stream().map(r -> r.label()).toList())).append('\n');
			});
			if (attention.items().size() > 10) {
				body.append("  ... ve ").append(attention.items().size() - 10).append(" üye daha\n");
			}
		}
		body.append("\nPanel: ").append(siteUrl).append("/yonetim\n");
		int sent = 0;
		for (var coach : coachAccountRepository.findAll()) {
			if (mailSender.send(coach.getEmail(), "S2000 günlük özet", body.toString())) {
				sent++;
			}
		}
		return sent;
	}

	private static Map<UUID, LocalDate> dates(List<Object[]> rows) {
		Map<UUID, LocalDate> map = new HashMap<>();
		for (Object[] row : rows) {
			map.put((UUID) row[0], (LocalDate) row[1]);
		}
		return map;
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		reminderLogRepository.deleteAllByMemberId(memberId);
	}
}
