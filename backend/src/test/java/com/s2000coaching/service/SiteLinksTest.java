package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.s2000coaching.exception.BadRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class SiteLinksTest {

	@Test
	void shouldTurnAUserNameIntoAnInstagramAddress() {
		assertThat(SiteLinks.instagram("s2000coaching")).isEqualTo("https://www.instagram.com/s2000coaching");
		assertThat(SiteLinks.instagram(" @s2000.coaching ")).isEqualTo("https://www.instagram.com/s2000.coaching");
	}

	@Test
	void shouldKeepAnInstagramAddressOnInstagram() {
		assertThat(SiteLinks.instagram("https://www.instagram.com/s2000coaching/"))
				.isEqualTo("https://www.instagram.com/s2000coaching/");
		assertThat(SiteLinks.instagram("instagram.com/s2000coaching")).isEqualTo("https://instagram.com/s2000coaching");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://www.instagram.com/x",              // not https
			"https://evil.example/instagram.com",       // another host
			"https://instagram.com@evil.example/x",     // the host is what follows the @
			"javascript:alert(1)",
			"kullanici adı",                            // a space is not in a user name
	})
	void shouldRefuseAnInstagramValueThatIsNotInstagram(String value) {
		assertThatThrownBy(() -> SiteLinks.instagram(value)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldAcceptYoutubeAddressesOnYoutubeOnly() {
		assertThat(SiteLinks.youtube("https://www.youtube.com/@s2000")).isEqualTo("https://www.youtube.com/@s2000");
		assertThat(SiteLinks.youtube("youtu.be/abc")).isEqualTo("https://youtu.be/abc");
		assertThatThrownBy(() -> SiteLinks.youtube("https://vimeo.com/1")).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> SiteLinks.youtube("https://youtube.com.evil.example/x")).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> SiteLinks.youtube("javascript:alert(1)")).isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldBuildAWhatsappAddressFromAnyWayOfWritingTheNumber() {
		assertThat(SiteLinks.whatsapp("+90 555 111 22 33")).isEqualTo("https://wa.me/905551112233");
		assertThat(SiteLinks.whatsapp("0555 111 22 33")).isEqualTo("https://wa.me/905551112233");
		assertThat(SiteLinks.whatsapp("555-111-22-33")).isEqualTo("https://wa.me/905551112233");
		assertThat(SiteLinks.whatsapp("https://wa.me/905551112233")).isEqualTo("https://wa.me/905551112233");
	}

	@Test
	void shouldRefuseAWhatsappValueThatIsNotANumberOrWhatsapp() {
		assertThatThrownBy(() -> SiteLinks.whatsapp("12345")).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> SiteLinks.whatsapp("https://evil.example/905551112233")).isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldTreatBlankAsNoLink() {
		assertThat(SiteLinks.instagram("  ")).isNull();
		assertThat(SiteLinks.youtube(null)).isNull();
		assertThat(SiteLinks.whatsapp("")).isNull();
	}
}
