package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Member;
import com.s2000coaching.domain.MemberStatus;
import com.s2000coaching.domain.PushSubscription;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.push.PushGateway;
import com.s2000coaching.repository.PushSubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

	@Mock
	private PushSubscriptionRepository repository;

	@Mock
	private PushGateway gateway;

	private PushService service;
	private Member member;

	@BeforeEach
	void setUp() {
		service = new PushService(repository, gateway);
		member = new Member(UUID.randomUUID(), "a@example.com", "Ahmet", "+905551112233", 180, new BigDecimal("80"),
				"x", MemberStatus.ACTIVE, Instant.now());
	}

	private PushSubscription device(String endpoint) {
		return new PushSubscription(UUID.randomUUID(), member, endpoint, "p256", "auth", Instant.now());
	}

	@Test
	void shouldRefuseToSubscribeWhileNotificationsAreOff() {
		when(gateway.enabled()).thenReturn(false);

		assertThatThrownBy(() -> service.subscribe(member, "https://push.example/1", "k", "a"))
				.isInstanceOf(BadRequestException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void shouldOnlyAcceptHttpsEndpoints() {
		when(gateway.enabled()).thenReturn(true);

		assertThatThrownBy(() -> service.subscribe(member, "http://internal.example/hook", "k", "a"))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldNotDuplicateADeviceThatSubscribesAgain() {
		when(gateway.enabled()).thenReturn(true);
		var existing = device("https://push.example/1");
		when(repository.findByEndpoint("https://push.example/1")).thenReturn(Optional.of(existing));

		service.subscribe(member, "https://push.example/1", "newkey", "newauth");

		assertThat(existing.getP256dh()).isEqualTo("newkey");
		verify(repository, never()).save(any());
	}

	@Test
	void shouldForgetADeviceThePushServiceSaysIsGoneAndStillReachTheOthers() {
		when(gateway.enabled()).thenReturn(true);
		var gone = device("https://push.example/gone");
		var alive = device("https://push.example/alive");
		when(repository.findAllByMemberId(member.getId())).thenReturn(List.of(gone, alive));
		when(gateway.send(org.mockito.ArgumentMatchers.eq("https://push.example/gone"), anyString(), anyString(),
				anyString(), anyString(), anyString())).thenReturn(PushGateway.Result.GONE);
		when(gateway.send(org.mockito.ArgumentMatchers.eq("https://push.example/alive"), anyString(), anyString(),
				anyString(), anyString(), anyString())).thenReturn(PushGateway.Result.SENT);

		assertThat(service.notify(member, "Başlık", "Gövde", "/uye")).isTrue();

		verify(repository).delete(gone);
		verify(repository, never()).delete(alive);
	}

	@Test
	void shouldReportNotDeliveredWhenTheGatewayIsOff() {
		when(gateway.enabled()).thenReturn(false);

		assertThat(service.notify(member, "t", "b", "/uye")).isFalse();
		verify(repository, never()).findAllByMemberId(any());
	}
}
