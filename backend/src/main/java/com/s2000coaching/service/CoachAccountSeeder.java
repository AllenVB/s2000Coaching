package com.s2000coaching.service;

import com.s2000coaching.domain.CoachAccount;
import com.s2000coaching.repository.CoachAccountRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first coach account from {@code ADMIN_EMAIL} / {@code ADMIN_PASSWORD} so a
 * fresh deployment is reachable without a manual database insert.
 *
 * <p>Runs once: an existing account is never overwritten, so changing the variables later
 * does not reset a password someone has since changed.
 */
@Component
public class CoachAccountSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CoachAccountSeeder.class);

	/** What a decoder leaves behind when it cannot read the bytes it was handed. */
	private static final char REPLACEMENT_CHAR = '\uFFFD';
	private static final String FALLBACK_NAME = "Koc";

	private final CoachAccountRepository coachRepository;
	private final PasswordEncoder passwordEncoder;
	private final String seedEmail;
	private final String seedPassword;
	private final String seedName;

	public CoachAccountSeeder(CoachAccountRepository coachRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.seed-email:}") String seedEmail,
			@Value("${app.admin.seed-password:}") String seedPassword,
			@Value("${app.admin.seed-name:Koç}") String seedName) {
		this.coachRepository = coachRepository;
		this.passwordEncoder = passwordEncoder;
		this.seedEmail = seedEmail;
		this.seedPassword = seedPassword;
		this.seedName = seedName;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (coachRepository.count() > 0) {
			return;
		}

		if (seedEmail.isBlank() || seedPassword.isBlank()) {
			log.warn("Yönetim hesabı yok ve ADMIN_EMAIL/ADMIN_PASSWORD tanımlı değil — "
					+ "yönetim paneline giriş yapılamaz. Bu iki değişkeni tanımlayıp yeniden başlatın.");
			return;
		}

		String name = seedName.trim();
		if (name.indexOf(REPLACEMENT_CHAR) >= 0) {
			// The JVM decodes environment variables with sun.jnu.encoding, so on a host with a
			// non-UTF-8 locale "Koç" arrives as "Ko??" and would be stored that way for good.
			log.warn("ADMIN_NAME bozuk geldi (\"{}\") — sunucunun dil ayarı UTF-8 değil. "
					+ "LANG=C.UTF-8 tanımlayıp yeniden başlatın; ad düzeltilene kadar \"{}\" kullanılıyor.",
					name, FALLBACK_NAME);
			name = FALLBACK_NAME;
		}

		String email = CoachAuthService.normalizeEmail(seedEmail);
		coachRepository.save(new CoachAccount(UUID.randomUUID(), email,
				passwordEncoder.encode(seedPassword), name, Instant.now()));
		log.info("İlk yönetim hesabı oluşturuldu: {}", email);
	}
}
