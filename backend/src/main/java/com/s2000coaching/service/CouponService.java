package com.s2000coaching.service;

import com.s2000coaching.domain.CoachingPackage;
import com.s2000coaching.domain.Coupon;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.ConflictException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.CouponRepository;
import com.s2000coaching.web.dto.CouponDtos;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Discount codes. The buyer can only ask "is this code good for this package"; the answer for a wrong,
 * expired, switched-off or used-up code is the same message, so the endpoint does not confirm which codes
 * exist. It is also limited per address, since it is public and a code is a short guessable string.
 */
@Service
public class CouponService {

	static final String INVALID = "Geçersiz ya da süresi dolmuş indirim kodu.";
	static final int MAX_CHECKS_PER_HOUR = 30;

	private final CouponRepository couponRepository;
	private final PackageService packageService;
	private final Clock clock;
	private final HourlyLimiter limiter = new HourlyLimiter(MAX_CHECKS_PER_HOUR);

	public CouponService(CouponRepository couponRepository, PackageService packageService, Clock clock) {
		this.couponRepository = couponRepository;
		this.packageService = packageService;
		this.clock = clock;
	}

	public static String normalize(String code) {
		return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
	}

	/** The price in minor units after {@code percentOff}, rounded to the nearest kuruş. */
	public static long discounted(long priceMinor, int percentOff) {
		return Math.round(priceMinor * (100 - percentOff) / 100.0);
	}

	@Transactional(readOnly = true)
	public CouponDtos.Validation validate(CouponDtos.ValidateRequest request, String clientAddress) {
		if (!limiter.tryAcquire(clientAddress, Instant.now(clock))) {
			throw new BadRequestException("Çok fazla deneme yaptınız. Lütfen biraz sonra tekrar deneyin.");
		}
		CoachingPackage pack = packageService.getActiveOrThrow(request.packageId());
		Coupon coupon = usable(request.code());
		return new CouponDtos.Validation(coupon.getCode(), coupon.getPercentOff(), pack.getPriceMinor(),
				discounted(pack.getPriceMinor(), coupon.getPercentOff()));
	}

	/** Used by the order flow: checks the code and takes one use, atomically. */
	@Transactional
	public Coupon redeem(String code) {
		Coupon coupon = usable(code);
		if (couponRepository.takeOneUse(coupon.getId()) == 0) {
			throw new BadRequestException(INVALID);
		}
		return coupon;
	}

	private Coupon usable(String code) {
		Coupon coupon = couponRepository.findByCode(normalize(code)).orElseThrow(() -> new BadRequestException(INVALID));
		if (!coupon.isUsableOn(LocalDate.now(clock))) {
			throw new BadRequestException(INVALID);
		}
		return coupon;
	}

	// --- coach ---

	@Transactional(readOnly = true)
	public List<CouponDtos.Response> list() {
		return couponRepository.findAllByOrderByCreatedAtDesc().stream().map(CouponDtos.Response::from).toList();
	}

	@Transactional
	public CouponDtos.Response create(CouponDtos.CreateRequest request) {
		String code = normalize(request.code());
		if (couponRepository.findByCode(code).isPresent()) {
			throw new ConflictException("Bu kod zaten var.");
		}
		Coupon coupon = couponRepository.save(new Coupon(UUID.randomUUID(), code, request.percentOff(),
				request.maxUses(), request.expiresOn(), Instant.now(clock)));
		return CouponDtos.Response.from(coupon);
	}

	@Transactional
	public CouponDtos.Response setActive(UUID id, boolean active) {
		Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new NotFoundException("Kod bulunamadı."));
		coupon.setActive(active);
		return CouponDtos.Response.from(coupon);
	}
}
