package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.s2000coaching.TestcontainersConfiguration;
import com.s2000coaching.web.dto.UpdateSiteSettingsRequests;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Round trip through the real table: links, the stats JSON and a legal text. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SiteSettingsIntegrationTest {

	@Autowired
	private SiteSettingsService service;

	@Test
	void shouldRoundTripLinksStatsAndALegalText() {
		service.updateSocial(new UpdateSiteSettingsRequests.Social("@s2000test", "https://www.youtube.com/@s2000test", "+90 555 111 22 33"));
		service.updateStats(new UpdateSiteSettingsRequests.Stats(List.of(
				new UpdateSiteSettingsRequests.Stat("Aktif", "12", "Öğrenci", "Not", false),
				new UpdateSiteSettingsRequests.Stat("Skor", "4,8", "Memnuniyet", "", true))));
		service.updateLegal("iade", new UpdateSiteSettingsRequests.Legal("14 gün içinde iade."));

		var site = service.publicSettings();
		assertThat(site.social().instagram()).isEqualTo("https://www.instagram.com/s2000test");
		assertThat(site.social().whatsapp()).isEqualTo("https://wa.me/905551112233");
		assertThat(site.stats()).extracting(stat -> stat.value()).containsExactly("12", "4,8");
		assertThat(site.legal()).filteredOn(document -> document.slug().equals("iade")).singleElement()
				.satisfies(document -> assertThat(document.published()).isTrue());
		assertThat(service.legalDocument("iade").content()).isEqualTo("14 gün içinde iade.");

		service.updateStats(new UpdateSiteSettingsRequests.Stats(null));
		service.updateLegal("iade", new UpdateSiteSettingsRequests.Legal(""));
		assertThat(service.publicSettings().stats()).isNull();
		assertThat(service.legalDocument("iade").content()).isNull();
	}
}
