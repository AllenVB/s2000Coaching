package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.repository.CheckInRepository;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberIntakeRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.repository.MemberQuestionnaireRepository;
import com.s2000coaching.repository.MembershipRepository;
import com.s2000coaching.repository.NutritionPlanRepository;
import com.s2000coaching.repository.OrderRepository;
import com.s2000coaching.repository.TrainingPlanRepository;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.service.AttentionScorer.Item;
import com.s2000coaching.service.AttentionScorer.Signals;
import com.s2000coaching.web.dto.AttentionResponse;
import com.s2000coaching.web.dto.OrderSummaryResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who needs the coach today. Every signal is fetched once for the whole roster (a handful of
 * grouped queries), never per member: with a hundred members a per-member lookup would be
 * several hundred queries every time the panel opens.
 */
@Service
public class AttentionService {

	private final MemberRepository memberRepository;
	private final WorkoutLogRepository workoutLogRepository;
	private final MeasurementRepository measurementRepository;
	private final MemberIntakeRepository intakeRepository;
	private final TrainingPlanRepository trainingPlanRepository;
	private final NutritionPlanRepository nutritionPlanRepository;
	private final MembershipRepository membershipRepository;
	private final OrderRepository orderRepository;
	private final CheckInRepository checkInRepository;
	private final MemberQuestionnaireRepository questionnaireRepository;
	private final MessageService messageService;
	private final Clock clock;

	public AttentionService(MemberRepository memberRepository, WorkoutLogRepository workoutLogRepository,
			MeasurementRepository measurementRepository, MemberIntakeRepository intakeRepository,
			TrainingPlanRepository trainingPlanRepository, NutritionPlanRepository nutritionPlanRepository,
			MembershipRepository membershipRepository, OrderRepository orderRepository,
			CheckInRepository checkInRepository, MemberQuestionnaireRepository questionnaireRepository,
			MessageService messageService, Clock clock) {
		this.questionnaireRepository = questionnaireRepository;
		this.checkInRepository = checkInRepository;
		this.memberRepository = memberRepository;
		this.workoutLogRepository = workoutLogRepository;
		this.measurementRepository = measurementRepository;
		this.intakeRepository = intakeRepository;
		this.trainingPlanRepository = trainingPlanRepository;
		this.nutritionPlanRepository = nutritionPlanRepository;
		this.membershipRepository = membershipRepository;
		this.orderRepository = orderRepository;
		this.messageService = messageService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public AttentionResponse load() {
		LocalDate today = LocalDate.now(clock);
		ZoneId zone = clock.getZone();

		Map<UUID, LocalDate> lastWorkout = dates(workoutLogRepository.latestPerformedOnPerMember());
		Map<UUID, LocalDate> lastMeasurement = dates(measurementRepository.latestMeasuredOnPerMember());
		Map<UUID, LocalDate> lastIntake = dates(intakeRepository.latestConsumedOnPerMember());
		Map<UUID, LocalDate> membershipEnd = dates(membershipRepository.latestEndPerMember());
		Set<UUID> trainingPlans = new HashSet<>(trainingPlanRepository.findMemberIdsWithActivePlan());
		Set<UUID> nutritionPlans = new HashSet<>(nutritionPlanRepository.findMemberIdsWithActivePlan());
		Set<UUID> unread = new HashSet<>(messageService.memberIdsAwaitingReply());
		Set<UUID> withQuestionnaire = new HashSet<>(questionnaireRepository.findMemberIdsWithQuestionnaire());
		Set<UUID> needClearance = new HashSet<>(questionnaireRepository.findMemberIdsNeedingClearance());
		Set<UUID> unansweredCheckIns = new HashSet<>(checkInRepository.findMemberIdsWithUnansweredCheckIn());

		List<Item> items = memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.ACTIVE).stream()
				.map(member -> AttentionScorer.score(signals(member, zone, lastWorkout, lastMeasurement, lastIntake,
						membershipEnd, trainingPlans, nutritionPlans, unread, unansweredCheckIns, withQuestionnaire, needClearance), today))
				.filter(item -> item != null)
				.sorted(Comparator.comparingInt(Item::score).reversed().thenComparing(Item::fullName))
				.toList();

		List<OrderSummaryResponse> pendingTransfers = orderRepository
				.findAllByStatusOrderByCreatedAtAsc(OrderStatus.AWAITING_TRANSFER).stream()
				.map(OrderSummaryResponse::from)
				.toList();
		return new AttentionResponse(items, pendingTransfers);
	}

	private static Signals signals(Member member, ZoneId zone, Map<UUID, LocalDate> lastWorkout,
			Map<UUID, LocalDate> lastMeasurement, Map<UUID, LocalDate> lastIntake,
			Map<UUID, LocalDate> membershipEnd, Set<UUID> trainingPlans, Set<UUID> nutritionPlans,
			Set<UUID> unread, Set<UUID> unansweredCheckIns, Set<UUID> withQuestionnaire, Set<UUID> needClearance) {
		UUID id = member.getId();
		// Counted from the day they could first have done anything: confirming the account.
		var joined = member.getVerifiedAt() != null ? member.getVerifiedAt() : member.getCreatedAt();
		return new Signals(id, member.getFullName(), joined.atZone(zone).toLocalDate(), trainingPlans.contains(id),
				nutritionPlans.contains(id), lastWorkout.get(id), lastMeasurement.get(id), lastIntake.get(id),
				unread.contains(id), membershipEnd.get(id), unansweredCheckIns.contains(id),
				withQuestionnaire.contains(id), needClearance.contains(id));
	}

	/** Rows of (member id, date) from a grouped query. */
	private static Map<UUID, LocalDate> dates(List<Object[]> rows) {
		Map<UUID, LocalDate> map = new HashMap<>();
		for (Object[] row : rows) {
			map.put((UUID) row[0], (LocalDate) row[1]);
		}
		return map;
	}
}
