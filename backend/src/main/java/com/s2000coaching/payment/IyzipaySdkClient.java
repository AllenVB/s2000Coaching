package com.s2000coaching.payment;

import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.CheckoutForm;
import com.iyzipay.model.CheckoutFormInitialize;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.model.Status;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import com.s2000coaching.domain.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * iyzico's hosted checkout form, through the official SDK.
 *
 * <p>NOT exercised against iyzico from this codebase's own tests (they need merchant keys); the
 * code that decides what a result means is {@code IyzicoCallbackService}, which is tested with
 * a fake of {@link IyzicoClient}. Before going live, pay once in iyzico's sandbox
 * ({@code PAYMENT_IYZICO_BASE_URL=https://sandbox-api.iyzipay.com}) and watch the order turn PAID.
 */
@Service
public class IyzipaySdkClient implements IyzicoClient {

	private static final DateTimeFormatter IYZICO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneOffset.UTC);

	private final String apiKey;
	private final String secretKey;
	private final String baseUrl;
	private final String callbackUrl;
	private final String identityNumber;
	private final String address;
	private final String city;

	public IyzipaySdkClient(@Value("${app.payment.iyzico.api-key:}") String apiKey,
			@Value("${app.payment.iyzico.secret-key:}") String secretKey,
			@Value("${app.payment.iyzico.base-url:https://sandbox-api.iyzipay.com}") String baseUrl,
			@Value("${app.payment.iyzico.callback-url:}") String callbackUrl,
			@Value("${app.payment.iyzico.buyer-identity-number:11111111111}") String identityNumber,
			@Value("${app.payment.iyzico.buyer-address:Dijital hizmet - adres belirtilmedi}") String address,
			@Value("${app.payment.iyzico.buyer-city:Istanbul}") String city) {
		this.apiKey = apiKey.trim();
		this.secretKey = secretKey.trim();
		this.baseUrl = baseUrl.trim();
		this.callbackUrl = callbackUrl.trim();
		this.identityNumber = identityNumber.trim();
		this.address = address.trim();
		this.city = city.trim();
	}

	@Override
	public boolean configured() {
		return !apiKey.isBlank() && !secretKey.isBlank() && !callbackUrl.isBlank();
	}

	@Override
	public Initialized initialize(Order order, String buyerIp) {
		BigDecimal price = tl(order.getPriceMinor());

		CreateCheckoutFormInitializeRequest request = new CreateCheckoutFormInitializeRequest();
		request.setLocale(Locale.TR.getValue());
		request.setConversationId(order.getOrderNumber());
		request.setPrice(price);
		request.setPaidPrice(price);
		request.setCurrency(Currency.TRY.name());
		request.setBasketId(order.getOrderNumber());
		request.setPaymentGroup(PaymentGroup.PRODUCT.name());
		request.setCallbackUrl(callbackUrl);

		String[] name = splitName(order.getCustomerName());
		Buyer buyer = new Buyer();
		buyer.setId(order.getId().toString());
		buyer.setName(name[0]);
		buyer.setSurname(name[1]);
		buyer.setGsmNumber(order.getPhone());
		buyer.setEmail(order.getEmail());
		buyer.setIdentityNumber(identityNumber);
		buyer.setRegistrationDate(IYZICO_DATE.format(order.getCreatedAt()));
		buyer.setLastLoginDate(IYZICO_DATE.format(order.getCreatedAt()));
		buyer.setRegistrationAddress(address);
		buyer.setIp(buyerIp == null || buyerIp.isBlank() ? "127.0.0.1" : buyerIp);
		buyer.setCity(city);
		buyer.setCountry("Turkey");
		request.setBuyer(buyer);

		Address billing = new Address();
		billing.setContactName(order.getCustomerName());
		billing.setCity(city);
		billing.setCountry("Turkey");
		billing.setAddress(address);
		request.setShippingAddress(billing);
		request.setBillingAddress(billing);

		BasketItem item = new BasketItem();
		item.setId(order.getCoachingPackage().getSlug());
		item.setName(order.getCoachingPackage().getName());
		item.setCategory1("Online Koçluk");
		item.setItemType(BasketItemType.VIRTUAL.name());
		item.setPrice(price);
		request.setBasketItems(List.of(item));

		CheckoutFormInitialize result = CheckoutFormInitialize.create(request, options());
		if (!Status.SUCCESS.getValue().equals(result.getStatus()) || result.getPaymentPageUrl() == null) {
			throw new PaymentFailedException("iyzico ödeme sayfası açılamadı: " + result.getErrorMessage());
		}
		return new Initialized(result.getToken(), result.getPaymentPageUrl());
	}

	@Override
	public Result retrieve(String token) {
		RetrieveCheckoutFormRequest request = new RetrieveCheckoutFormRequest();
		request.setLocale(Locale.TR.getValue());
		request.setToken(token);
		CheckoutForm form = CheckoutForm.retrieve(request, options());

		boolean authentic = form.verifySignature(secretKey);
		boolean paid = Status.SUCCESS.getValue().equals(form.getStatus()) && "SUCCESS".equals(form.getPaymentStatus());
		return new Result(authentic && paid, form.getBasketId(), form.getPaidPrice(), form.getErrorMessage());
	}

	private Options options() {
		Options options = new Options();
		options.setApiKey(apiKey);
		options.setSecretKey(secretKey);
		options.setBaseUrl(baseUrl);
		return options;
	}

	/** Prices are kept in kuruş; the provider wants lira with two decimals. */
	static BigDecimal tl(long priceMinor) {
		return BigDecimal.valueOf(priceMinor, 2).setScale(2, RoundingMode.UNNECESSARY);
	}

	/** iyzico wants a first and a last name; everything before the last word is the first name. */
	static String[] splitName(String fullName) {
		String trimmed = fullName == null ? "" : fullName.trim();
		int space = trimmed.lastIndexOf(' ');
		return space > 0
				? new String[] {trimmed.substring(0, space).trim(), trimmed.substring(space + 1).trim()}
				: new String[] {trimmed, trimmed};
	}
}
