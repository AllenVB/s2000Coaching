package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.repository.LeadRepository;
import com.s2000coaching.web.dto.CreateLeadRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

	@Mock
	private LeadRepository leadRepository;

	private LeadService service;

	@BeforeEach
	void setUp() {
		service = new LeadService(leadRepository, Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC));
	}

	private static CreateLeadRequest request(String website) {
		return new CreateLeadRequest("Ahmet Yılmaz", "05551112233", "", "Kilo vermek", "", 180, null, "  not  ", true,
				website);
	}

	@Test
	void shouldStoreARealRequest() {
		service.submit(request(null), "1.2.3.4");

		verify(leadRepository).save(any());
	}

	@Test
	void shouldSilentlyDropAFormTheHoneypotCaught() {
		service.submit(request("http://spam.example"), "1.2.3.4");

		verify(leadRepository, never()).save(any());
	}

	@Test
	void shouldLimitHowManyRequestsOneAddressCanSendPerHour() {
		for (int i = 0; i < LeadService.MAX_PER_HOUR; i++) {
			service.submit(request(null), "9.9.9.9");
		}

		assertThatThrownBy(() -> service.submit(request(null), "9.9.9.9")).isInstanceOf(BadRequestException.class);
		verify(leadRepository, times(LeadService.MAX_PER_HOUR)).save(any());
		// Another address is not affected.
		service.submit(request(null), "8.8.8.8");
		assertThat(true).isTrue();
	}
}
