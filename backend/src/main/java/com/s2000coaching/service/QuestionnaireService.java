package com.s2000coaching.service;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberQuestionnaire;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.repository.MemberQuestionnaireRepository;
import com.s2000coaching.web.dto.QuestionnaireResponse;
import com.s2000coaching.web.dto.UpsertQuestionnaireRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The starting health questionnaire. A "yes" to any PAR-Q question does not stop anyone
 * training — it tells the coach to ask for a doctor's clearance first, which is what the
 * standard instrument is for.
 */
@Service
public class QuestionnaireService implements MemberDataEraser {

	/** The standard seven PAR-Q readiness questions, in the order the answers are stored. */
	public static final List<String> QUESTIONS = List.of(
			"Doktorunuz kalp rahatsızlığınız olduğunu ve yalnızca doktor önerisiyle egzersiz yapmanız gerektiğini söyledi mi?",
			"Egzersiz yaparken göğsünüzde ağrı hissediyor musunuz?",
			"Son bir ay içinde, egzersiz yapmadığınız bir sırada göğüs ağrısı yaşadınız mı?",
			"Baş dönmesi yüzünden dengenizi kaybettiğiniz ya da bilincinizin kapandığı oldu mu?",
			"Egzersizle kötüleşebilecek bir kemik ya da eklem sorununuz (sırt, diz, kalça gibi) var mı?",
			"Doktorunuz size kan basıncı ya da kalp için ilaç (örneğin su atıcı) yazdı mı?",
			"Egzersiz yapmamanız için bilmeniz gereken başka bir neden var mı?");

	private final MemberQuestionnaireRepository questionnaireRepository;
	private final MemberService memberService;
	private final Clock clock;

	public QuestionnaireService(MemberQuestionnaireRepository questionnaireRepository, MemberService memberService,
			Clock clock) {
		this.questionnaireRepository = questionnaireRepository;
		this.memberService = memberService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public QuestionnaireResponse get(UUID memberId) {
		return QuestionnaireResponse.of(QUESTIONS, questionnaireRepository.findByMemberId(memberId).orElse(null));
	}

	@Transactional(readOnly = true)
	public QuestionnaireResponse getAsCoach(UUID memberId) {
		memberService.requireMember(memberId);
		return get(memberId);
	}

	/** Filling it in again replaces the earlier answers: the latest ones are the true ones. */
	@Transactional
	public QuestionnaireResponse save(Member member, UpsertQuestionnaireRequest request) {
		if (!member.hasHealthDataConsent()) {
			throw new BadRequestException(
					"Anketi doldurabilmek için sağlık verisi işleme onayını vermeniz gerekiyor.");
		}
		if (request.parq().size() != QUESTIONS.size()) {
			throw new BadRequestException("Tüm sağlık sorularını yanıtlayın.");
		}
		StringBuilder answers = new StringBuilder();
		request.parq().forEach(answer -> answers.append(Boolean.TRUE.equals(answer) ? 'Y' : 'N'));

		Instant now = Instant.now(clock);
		MemberQuestionnaire questionnaire = questionnaireRepository.findByMemberId(member.getId())
				.orElseGet(() -> questionnaireRepository.save(new MemberQuestionnaire(UUID.randomUUID(), member, now)));
		questionnaire.fill(answers.toString(), request.goal(), request.trainingDaysPerWeek(), request.equipment(),
				request.injuries() == null || request.injuries().isBlank() ? null : request.injuries().trim(), now);
		return QuestionnaireResponse.of(QUESTIONS, questionnaire);
	}

	@Override
	@Transactional
	public void eraseAllOf(UUID memberId) {
		questionnaireRepository.deleteAllByMemberId(memberId);
	}
}
