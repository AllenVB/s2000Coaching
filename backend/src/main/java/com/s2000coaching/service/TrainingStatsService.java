package com.s2000coaching.service;

import com.s2000coaching.domain.SetLog;
import com.s2000coaching.domain.WorkoutLog;
import com.s2000coaching.repository.WorkoutLogRepository;
import com.s2000coaching.service.TrainingStatsCalculator.SetFact;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Personal records, weekly volume and next-session suggestions from a member's logged sets. */
@Service
public class TrainingStatsService {

	private final WorkoutLogRepository workoutLogRepository;
	private final MemberService memberService;
	private final Clock clock;

	public TrainingStatsService(WorkoutLogRepository workoutLogRepository, MemberService memberService, Clock clock) {
		this.workoutLogRepository = workoutLogRepository;
		this.memberService = memberService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public TrainingStatsCalculator.Result forMember(UUID memberId) {
		List<SetFact> facts = new ArrayList<>();
		for (WorkoutLog log : workoutLogRepository.findAllByMemberIdOrderByPerformedOnDesc(memberId)) {
			for (SetLog set : log.getSets()) {
				var exercise = set.getPlanExercise().getExercise();
				facts.add(new SetFact(exercise.getId(), exercise.getName(), set.getPlanExercise().getReps(),
						log.getPerformedOn(), set.getReps(), set.getWeightKg(), set.getRpe()));
			}
		}
		return TrainingStatsCalculator.calculate(facts, LocalDate.now(clock));
	}

	@Transactional(readOnly = true)
	public TrainingStatsCalculator.Result forMemberAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return forMember(memberId);
	}
}
