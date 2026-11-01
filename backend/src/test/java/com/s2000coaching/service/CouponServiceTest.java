package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.Coupon;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.repository.CouponRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

	@Mock
	private CouponRepository couponRepository;

	@Mock
	private PackageService packageService;

	private CouponService service;

	@BeforeEach
	void setUp() {
		service = new CouponService(couponRepository, packageService, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private Coupon coupon(Integer maxUses, LocalDate expires) {
		return new Coupon(UUID.randomUUID(), "YAZ20", 20, maxUses, expires, NOW);
	}

	@Test
	void shouldRoundTheDiscountedPriceToTheNearestKurus() {
		assertThat(CouponService.discounted(749000, 20)).isEqualTo(599200);
		assertThat(CouponService.discounted(299000, 15)).isEqualTo(254150);
		assertThat(CouponService.discounted(100001, 33)).isEqualTo(67001);
	}

	@Test
	void shouldIgnoreCaseAndSpacesInTheCode() {
		var c = coupon(null, null);
		when(couponRepository.findByCode("YAZ20")).thenReturn(Optional.of(c));
		when(couponRepository.takeOneUse(c.getId())).thenReturn(1);

		assertThat(service.redeem("  yaz20 ")).isSameAs(c);
	}

	@Test
	void shouldGiveTheSameAnswerForWrongExpiredAndUsedUpCodes() {
		var expired = coupon(null, LocalDate.of(2026, 10, 31));
		when(couponRepository.findByCode("YAZ20")).thenReturn(Optional.of(expired));
		when(couponRepository.findByCode("YOK")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.redeem("yaz20")).isInstanceOf(BadRequestException.class)
				.hasMessage(CouponService.INVALID);
		assertThatThrownBy(() -> service.redeem("yok")).isInstanceOf(BadRequestException.class)
				.hasMessage(CouponService.INVALID);
	}

	@Test
	void shouldRefuseWhenTheLastUseWasTakenInTheMeantime() {
		var c = coupon(1, null);
		when(couponRepository.findByCode("YAZ20")).thenReturn(Optional.of(c));
		when(couponRepository.takeOneUse(c.getId())).thenReturn(0);

		assertThatThrownBy(() -> service.redeem("YAZ20")).isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldAcceptACodeOnItsLastDay() {
		assertThat(coupon(null, LocalDate.of(2026, 11, 1)).isUsableOn(LocalDate.of(2026, 11, 1))).isTrue();
		assertThat(coupon(null, LocalDate.of(2026, 11, 1)).isUsableOn(LocalDate.of(2026, 11, 2))).isFalse();
	}
}
