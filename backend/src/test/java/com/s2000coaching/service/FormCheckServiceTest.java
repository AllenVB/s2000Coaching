package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.FormCheck;
import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.FormCheckRepository;
import com.s2000coaching.web.dto.CreateFormCheckRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FormCheckServiceTest {

	@Mock
	private FormCheckRepository repository;

	@Mock
	private MemberService memberService;

	@Mock
	private MemberNotifier notifier;

	private FormCheckService service;
	private Member member;

	@BeforeEach
	void setUp() {
		service = new FormCheckService(repository, memberService,
				Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneOffset.UTC), notifier);
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet", "+905551112233", 180,
				new BigDecimal("80"), "x", MemberStatus.ACTIVE, Instant.now());
	}

	@Test
	void shouldStoreOnlyTheCanonicalYouTubeAddress() {
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

		var response = service.create(member,
				new CreateFormCheckRequest("  Squat  ", "youtu.be/dQw4w9WgXcQ?t=30&si=zzz", "  dizim içe kaçıyor mu?  "));

		assertThat(response.exerciseName()).isEqualTo("Squat");
		assertThat(response.video().id()).isEqualTo("dQw4w9WgXcQ");
		assertThat(response.video().startSeconds()).isEqualTo(30);
		assertThat(response.note()).isEqualTo("dizim içe kaçıyor mu?");
	}

	/** The link ends up in an iframe on the coach's screen, so anything but YouTube is refused. */
	@Test
	void shouldRefuseALinkThatIsNotYouTube() {
		assertThatThrownBy(() -> service.create(member,
				new CreateFormCheckRequest("Squat", "https://evil.example/clip.mp4", null)))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("YouTube");

		verify(repository, never()).save(any());
	}

	@Test
	void shouldNotLetAMemberDeleteSomeoneElsesRequest() {
		var other = new Member(UUID.randomUUID(), "b@example.com", "Başka", "+905551112244", 170,
				new BigDecimal("70"), "x", MemberStatus.ACTIVE, Instant.now());
		var theirs = new FormCheck(UUID.randomUUID(), other, "Squat", "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
				null, Instant.now());
		when(repository.findById(theirs.getId())).thenReturn(Optional.of(theirs));

		assertThatThrownBy(() -> service.deleteOwn(member, theirs.getId())).isInstanceOf(NotFoundException.class);
		verify(repository, never()).delete(any());
	}

	@Test
	void shouldStoreTheCoachReply() {
		var check = new FormCheck(UUID.randomUUID(), member, "Squat", "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
				null, Instant.now());
		when(repository.findById(check.getId())).thenReturn(Optional.of(check));

		var response = service.reply(check.getId(), "  Dizlerini dışarı it  ");

		assertThat(response.coachReply()).isEqualTo("Dizlerini dışarı it");
		assertThat(response.coachRepliedAt()).isEqualTo(Instant.parse("2026-10-27T09:00:00Z"));
	}

	@Test
	void shouldEraseEverythingOfAMember() {
		service.eraseAllOf(member.getId());

		verify(repository).deleteAllByMemberId(member.getId());
	}
}
