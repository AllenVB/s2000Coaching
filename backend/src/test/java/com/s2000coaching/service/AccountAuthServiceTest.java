package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.repository.CoachAccountRepository;
import com.s2000coaching.repository.MemberRepository;
import com.s2000coaching.web.dto.AccountSessionResponse;
import com.s2000coaching.web.dto.AccountSessionResponse.Role;
import com.s2000coaching.web.dto.CoachSessionResponse;
import com.s2000coaching.web.dto.MemberSessionResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountAuthServiceTest {

	private static final String EMAIL = "kisi@example.com";
	private static final String PASSWORD = "dogru-sifre";

	@Mock
	private CoachAccountRepository coachRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private CoachAuthService coachAuthService;
	@Mock
	private MemberAuthService memberAuthService;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private AccountAuthService service;
	private CoachAccount coach;
	private final CoachSessionResponse coachSession = new CoachSessionResponse("coach-token", Instant.now(), null);
	private final MemberSessionResponse memberSession = new MemberSessionResponse("member-token", Instant.now(), null);

	@BeforeEach
	void setUp() {
		service = new AccountAuthService(coachRepository, memberRepository, coachAuthService, memberAuthService,
				passwordEncoder);
		coach = new CoachAccount(UUID.randomUUID(), EMAIL, passwordEncoder.encode(PASSWORD), "Koç", Instant.now());
	}

	@Test
	void shouldSignTheCoachInWhenTheAddressAndPasswordAreTheirs() {
		when(coachRepository.findByEmail(EMAIL)).thenReturn(Optional.of(coach));
		when(coachAuthService.login(EMAIL, PASSWORD)).thenReturn(coachSession);

		AccountSessionResponse response = service.login("  Kisi@Example.com ", PASSWORD);

		assertThat(response.role()).isEqualTo(Role.COACH);
		assertThat(response.coach()).isSameAs(coachSession);
		assertThat(response.member()).isNull();
		verify(memberAuthService, never()).login(any(), any());
	}

	@Test
	void shouldSignAMemberInWhenNoCoachHasTheAddress() {
		when(coachRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
		when(memberAuthService.login(EMAIL, PASSWORD)).thenReturn(memberSession);

		AccountSessionResponse response = service.login(EMAIL, PASSWORD);

		assertThat(response.role()).isEqualTo(Role.MEMBER);
		assertThat(response.member()).isSameAs(memberSession);
		verify(coachAuthService, never()).login(any(), any());
	}

	@Test
	void shouldLetTheCoachSignInCountTheFailureWhenOnlyACoachHasTheAddress() {
		when(coachRepository.findByEmail(EMAIL)).thenReturn(Optional.of(coach));
		when(memberRepository.existsByEmail(EMAIL)).thenReturn(false);
		when(coachAuthService.login(EMAIL, "yanlis"))
				.thenThrow(new UnauthorizedException("E-posta veya şifre hatalı."));

		assertThatThrownBy(() -> service.login(EMAIL, "yanlis")).isInstanceOf(UnauthorizedException.class);
		verify(memberAuthService, never()).login(any(), any());
	}

	@Test
	void shouldFallBackToTheMemberWhenTheSameAddressIsAlsoAMemberWithAnotherPassword() {
		when(coachRepository.findByEmail(EMAIL)).thenReturn(Optional.of(coach));
		when(memberRepository.existsByEmail(EMAIL)).thenReturn(true);
		when(memberAuthService.login(EMAIL, "uye-sifresi")).thenReturn(memberSession);

		AccountSessionResponse response = service.login(EMAIL, "uye-sifresi");

		assertThat(response.role()).isEqualTo(Role.MEMBER);
		verify(coachAuthService, never()).login(any(), any());
	}

	@Test
	void shouldNotTryTheCoachPasswordOnALockedCoachWhoIsAlsoAMember() {
		coach.registerFailedAttempt(Instant.now(), java.time.Duration.ofMinutes(15));
		for (int i = 0; i < 6; i++) {
			coach.registerFailedAttempt(Instant.now(), java.time.Duration.ofMinutes(15));
		}
		when(coachRepository.findByEmail(EMAIL)).thenReturn(Optional.of(coach));
		when(memberRepository.existsByEmail(EMAIL)).thenReturn(true);
		when(memberAuthService.login(EMAIL, PASSWORD)).thenReturn(memberSession);

		assertThat(service.login(EMAIL, PASSWORD).role()).isEqualTo(Role.MEMBER);
		verify(coachAuthService, never()).login(any(), any());
	}
}
