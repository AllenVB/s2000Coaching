package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.OrderStatus;
import com.s2000coaching.repository.CheckInRepository;
import com.s2000coaching.repository.FormCheckRepository;
import com.s2000coaching.repository.MeasurementRepository;
import com.s2000coaching.repository.MemberActivityRepository;
import com.s2000coaching.repository.MemberIntakeRepository;
import com.s2000coaching.repository.WeeklyNutritionRepository;
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
	private final MemberActivityRepository activityRepository;
	private final WeeklyNutritionRepository weeklyNutritionRepository;
	private final TrainingPlanRepository trainingPlanRepository;
	private final NutritionPlanRepository nutritionPlanRepository;
	private final MembershipRepository membershipRepository;
	private final OrderRepository orderRepository;
	private final CheckInRepository checkInRepository;
	private final FormCheckRepository formCheckRepository;
	private final MemberQuestionnaireRepository questionnaireRepository;
	private final MessageService messageService;
	private final Clock clock;

	public AttentionService(MemberRepository memberRepository, WorkoutLogRepository workoutLogRepository,
			MeasurementRepository measurementRepository, MemberIntakeRepository intakeRepository,
			WeeklyNutritionRepository weeklyNutritionRepository,
			TrainingPlanRepository trainingPlanRepository, NutritionPlanRepository nutritionPlanRepository,
			MembershipRepository membershipRepository, OrderRepository orderRepository,
			CheckInRepository checkInRepository, FormCheckRepository formCheckRepository, MemberQuestionnaireRepository questionnaireRepository,
			MessageService messageService, MemberActivityRepository activityRepository, Clock clock) {
		this.activityRepository = activityRepository;
		this.questionnaireRepository = questionnaireRepository;
		this.formCheckRepository = formCheckRepository;
		this.checkInRepository = checkInRepository;
		this.memberRepository = memberRepository;
		this.workoutLogRepository = workoutLogRepository;
		this.measurementRepository = measurementRepository;
		this.intakeRepository = intakeRepository;
		this.weeklyNutritionRepository = weeklyNutritionRepository;
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
		Map<UUID, LocalDate> lastIntake = lastFoodDay(intakeRepository.latestConsumedOnPerMember(),
				weeklyNutritionRepository.latestWeekStartPerMember());
		Map<UUID, LocalDate> membershipEnd = dates(membershipRepository.latestEndPerMember());
		Set<UUID> trainingPlans = new HashSet<>(trainingPlanRepository.findMemberIdsWithActivePlan());
		Set<UUID> nutritionPlans = new HashSet<>(nutritionPlanRepository.findMemberIdsWithActivePlan());
		Set<UUID> unread = new HashSet<>(messageService.memberIdsAwaitingReply());
		Set<UUID> withQuestionnaire = new HashSet<>(questionnaireRepository.findMemberIdsWithQuestionnaire());
		Set<UUID> needClearance = new HashSet<>(questionnaireRepository.findMemberIdsNeedingClearance());
		Set<UUID> unansweredFormChecks = new HashSet<>(formCheckRepository.findMemberIdsWithUnansweredFormCheck());
		Set<UUID> unansweredCheckIns = new HashSet<>(checkInRepository.findMemberIdsWithUnansweredCheckIn());

		Map<UUID, LocalDate> lastDailyIntake = dates(intakeRepository.latestConsumedOnPerMember());
		Map<UUID, Integer> overTarget = overTargetDays(today);
		Set<UUID> stepsEver = new HashSet<>(activityRepository.findMemberIdsWithSteps());
		Map<UUID, Integer> averageSteps = averageSteps(today);

		List<Item> items = memberRepository.findAllByStatusOrderByCreatedAtDesc(MemberStatus.ACTIVE).stream()
				.map(member -> AttentionScorer.score(signals(member, zone, lastWorkout, lastMeasurement, lastIntake,
						membershipEnd, trainingPlans, nutritionPlans, unread, unansweredCheckIns, withQuestionnaire, needClearance, unansweredFormChecks,
						lastDailyIntake, overTarget, stepsEver, averageSteps), today))
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
			Set<UUID> unread, Set<UUID> unansweredCheckIns, Set<UUID> withQuestionnaire, Set<UUID> needClearance,
			Set<UUID> unansweredFormChecks, Map<UUID, LocalDate> lastDailyIntake, Map<UUID, Integer> overTarget,
			Set<UUID> stepsEver, Map<UUID, Integer> averageSteps) {
		UUID id = member.getId();
		// Counted from the day they could first have done anything: confirming the account.
		var joined = member.getVerifiedAt() != null ? member.getVerifiedAt() : member.getCreatedAt();
		return new Signals(id, member.getFullName(), joined.atZone(zone).toLocalDate(), trainingPlans.contains(id),
				nutritionPlans.contains(id), lastWorkout.get(id), lastMeasurement.get(id), lastIntake.get(id),
				unread.contains(id), membershipEnd.get(id), unansweredCheckIns.contains(id),
				withQuestionnaire.contains(id), needClearance.contains(id), unansweredFormChecks.contains(id),
				lastDailyIntake.get(id), overTarget.getOrDefault(id, 0), stepsEver.contains(id), averageSteps.get(id));
	}

	/**
	 * Per member, how many of the last seven days were over the calorie target of the day type the member chose.
	 * Days with no day type, or members with no target for that type, simply do not count.
	 */
	private Map<UUID, Integer> overTargetDays(LocalDate today) {
		Map<UUID, Integer[]> targets = new HashMap<>();
		for (Object[] row : nutritionPlanRepository.activeCalorieTargets()) {
			targets.put((UUID) row[0], new Integer[] { (Integer) row[1], (Integer) row[2] });
		}
		Map<UUID, Integer> counts = new HashMap<>();
		for (Object[] row : intakeRepository.recentDays(today.minusDays(6))) {
			Integer[] goal = targets.get((UUID) row[0]);
			if (goal == null || row[3] == null) {
				continue;
			}
			Integer target = row[3].toString().equals("DINLENME") ? goal[1] : goal[0];
			int calories = (Integer) row[2];
			if (target != null && target > 0 && calories > target * (1 + AttentionScorer.OVER_TARGET_MARGIN)) {
				counts.merge((UUID) row[0], 1, Integer::sum);
			}
		}
		return counts;
	}

	/** Average steps per logged day over the last seven days; a member with no step entry in the window is absent. */
	private Map<UUID, Integer> averageSteps(LocalDate today) {
		Map<UUID, long[]> sums = new HashMap<>();
		for (Object[] row : activityRepository.recentSteps(today.minusDays(6))) {
			long[] sum = sums.computeIfAbsent((UUID) row[0], k -> new long[2]);
			sum[0] += (Integer) row[2];
			sum[1]++;
		}
		Map<UUID, Integer> averages = new HashMap<>();
		sums.forEach((id, sum) -> averages.put(id, (int) (sum[0] / sum[1])));
		return averages;
	}

	/**
	 * The latest day any food was recorded for: the end of the newest weekly entry (the Sunday that
	 * closes the week) or the newest day of the older daily log, whichever is later.
	 */
	private static Map<UUID, LocalDate> lastFoodDay(List<Object[]> dailyLog, List<Object[]> weeks) {
		Map<UUID, LocalDate> latest = dates(dailyLog);
		dates(weeks).forEach((memberId, weekStart) -> latest.merge(memberId, weekStart.plusDays(6),
				(a, b) -> a.isAfter(b) ? a : b));
		return latest;
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
