package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.repository.CoachAccountRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CoachAccountSeederTest {

	@Mock
	private CoachAccountRepository coachRepository;

	@Mock
	private com.s2000coaching.repository.MemberRepository memberRepository;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private CoachAccountSeeder seeder(String email, String password, String name) {
		return new CoachAccountSeeder(coachRepository, memberRepository, passwordEncoder, email, password, name);
	}

	private CoachAccount seedAndCapture(CoachAccountSeeder seeder) {
		seeder.run(null);
		var saved = ArgumentCaptor.forClass(CoachAccount.class);
		verify(coachRepository).save(saved.capture());
		return saved.getValue();
	}

	@Test
	void shouldCreateTheFirstAccountWithANormalizedAddressAndHashedPassword() {
		when(coachRepository.findByEmail("koc@s2000.com")).thenReturn(Optional.empty());

		CoachAccount saved = seedAndCapture(seeder("  Koc@S2000.COM ", "gizli-sifre", "Koç"));

		assertThat(saved.getEmail()).isEqualTo("koc@s2000.com");
		assertThat(saved.getFullName()).isEqualTo("Koç");
		assertThat(saved.getPasswordHash()).isNotEqualTo("gizli-sifre");
		assertThat(passwordEncoder.matches("gizli-sifre", saved.getPasswordHash())).isTrue();
	}

	@Test
	void shouldNotCreateACoachWhoseAddressBelongsToAMember() {
		when(coachRepository.findByEmail("uye@example.com")).thenReturn(Optional.empty());
		when(memberRepository.existsByEmail("uye@example.com")).thenReturn(true);

		seeder("uye@example.com", "Test-sifre1", "Koç").run(null);

		verify(coachRepository, never()).save(any());
	}

	@Test
	void shouldLeaveAnExistingAccountAlone() {
		// Re-seeding must never reset a password the coach has since changed.
		when(coachRepository.findByEmail("koc@s2000.com")).thenReturn(Optional.of(mock(CoachAccount.class)));

		seeder("koc@s2000.com", "yeni-sifre", "Koç").run(null);

		verify(coachRepository, never()).save(any());
	}

	/** A coach can be added by starting once with a new address; the first one is not special. */
	@Test
	void shouldAddAnotherCoachWhenTheAddressIsNew() {
		when(coachRepository.findByEmail("ikinci@s2000.com")).thenReturn(Optional.empty());

		CoachAccount saved = seedAndCapture(seeder("ikinci@s2000.com", "gizli-sifre-2", "İkinci Koç"));

		assertThat(saved.getEmail()).isEqualTo("ikinci@s2000.com");
		verify(coachRepository, never()).count();
	}

	@Test
	void shouldDoNothingWhenTheVariablesAreMissing() {
		when(coachRepository.count()).thenReturn(0L);

		seeder("", "", "Koç").run(null);

		verify(coachRepository, never()).save(any());
	}

	/**
	 * The JVM decodes environment variables with {@code sun.jnu.encoding}, so on a host whose
	 * locale is not UTF-8 the name arrives already broken. Storing it would make the damage
	 * permanent, since the seeder never runs again.
	 */
	@Test
	void shouldNotStoreANameTheJvmFailedToDecode() {
		when(coachRepository.findByEmail("koc@s2000.com")).thenReturn(Optional.empty());

		CoachAccount saved = seedAndCapture(seeder("koc@s2000.com", "gizli-sifre", "S�leyman Ko�"));

		assertThat(saved.getFullName()).isEqualTo("Koc");
	}
}
