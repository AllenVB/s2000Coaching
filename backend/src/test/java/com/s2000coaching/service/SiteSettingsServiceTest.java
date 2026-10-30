package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.domain.SiteSetting;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.SiteSettingRepository;
import com.s2000coaching.web.dto.UpdateSiteSettingsRequests;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SiteSettingsServiceTest {

	@Mock
	private SiteSettingRepository repository;

	private SiteSettingsService service;
	private final Instant now = Instant.parse("2026-10-30T10:00:00Z");

	@BeforeEach
	void setUp() {
		service = new SiteSettingsService(repository, JsonMapper.builder().build(),
				Clock.fixed(now, ZoneId.of("Europe/Istanbul")));
	}

	@Test
	void shouldShowNothingMadeUpWhenNothingIsSet() {
		when(repository.findAll()).thenReturn(List.of());

		var site = service.publicSettings();

		assertThat(site.social().instagram()).isNull();
		assertThat(site.social().youtube()).isNull();
		assertThat(site.social().whatsapp()).isNull();
		assertThat(site.stats()).isNull();
		assertThat(site.legal()).hasSize(4).allSatisfy(document -> assertThat(document.published()).isFalse());
	}

	@Test
	void shouldNotPutLegalTextInThePublicOverview() {
		when(repository.findAll()).thenReturn(List.of(new SiteSetting("legal.kvkk", "Çok uzun bir metin", now)));

		var site = service.publicSettings();

		assertThat(site.legal()).filteredOn(document -> document.slug().equals("kvkk"))
				.singleElement().satisfies(document -> assertThat(document.published()).isTrue());
		assertThat(site.toString()).doesNotContain("Çok uzun bir metin");
	}

	@Test
	void shouldSaveNormalisedLinksAndRemoveABlankOne() {
		when(repository.findAll()).thenReturn(List.of());
		when(repository.findById("social.instagram")).thenReturn(Optional.empty());
		when(repository.findById("social.whatsapp")).thenReturn(Optional.empty());

		service.updateSocial(new UpdateSiteSettingsRequests.Social("@s2000coaching", "", "0555 111 22 33"));

		verify(repository).save(org.mockito.ArgumentMatchers.argThat(setting -> setting.getKey().equals("social.instagram")
				&& setting.getValue().equals("https://www.instagram.com/s2000coaching")));
		verify(repository).save(org.mockito.ArgumentMatchers.argThat(setting -> setting.getKey().equals("social.whatsapp")
				&& setting.getValue().equals("https://wa.me/905551112233")));
		verify(repository).deleteById("social.youtube");
	}

	@Test
	void shouldWriteNoneOfTheLinksWhenOneIsBad() {
		assertThatThrownBy(() -> service.updateSocial(
				new UpdateSiteSettingsRequests.Social("@s2000coaching", "https://evil.example/x", "")))
				.isInstanceOf(BadRequestException.class);

		verify(repository, never()).save(any());
		verify(repository, never()).deleteById(any());
	}

	@Test
	void shouldStoreStatsAndReadThemBack() {
		var stored = new java.util.concurrent.atomic.AtomicReference<String>();
		when(repository.findById("stats")).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(call -> {
			SiteSetting saved = call.getArgument(0);
			stored.set(saved.getValue());
			return saved;
		});
		when(repository.findAll()).thenAnswer(call -> List.of(new SiteSetting("stats", stored.get(), now)));

		var admin = service.updateStats(new UpdateSiteSettingsRequests.Stats(List.of(
				new UpdateSiteSettingsRequests.Stat(" Aktif ", " 12 ", " Öğrenci ", null, true))));

		assertThat(admin.stats()).singleElement().satisfies(stat -> {
			assertThat(stat.label()).isEqualTo("Aktif");
			assertThat(stat.value()).isEqualTo("12");
			assertThat(stat.footnote()).isEmpty();
			assertThat(stat.accent()).isTrue();
		});
	}

	@Test
	void shouldPutTheBuiltInStatsBackWhenAskedTo() {
		service.updateStats(new UpdateSiteSettingsRequests.Stats(null));

		verify(repository).deleteById("stats");
	}

	@Test
	void shouldTreatUnreadableStoredStatsAsNotSet() {
		when(repository.findAll()).thenReturn(List.of(new SiteSetting("stats", "{bozuk", now)));

		assertThat(service.publicSettings().stats()).isNull();
	}

	@Test
	void shouldPublishAndUnpublishALegalText() {
		when(repository.findById("legal.kvkk")).thenReturn(Optional.empty());

		service.updateLegal("kvkk", new UpdateSiteSettingsRequests.Legal("  Metin  "));
		verify(repository).save(org.mockito.ArgumentMatchers.argThat(setting -> setting.getKey().equals("legal.kvkk")
				&& setting.getValue().equals("Metin")));

		service.updateLegal("kvkk", new UpdateSiteSettingsRequests.Legal("   "));
		verify(repository).deleteById("legal.kvkk");
	}

	@Test
	void shouldOnlyKnowTheFourLegalPages() {
		assertThatThrownBy(() -> service.legalDocument("baska")).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> service.updateLegal("baska", new UpdateSiteSettingsRequests.Legal("x")))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldAnswerAnUnwrittenLegalPageWithoutContent() {
		when(repository.findById("legal.iade")).thenReturn(Optional.empty());

		var document = service.legalDocument("iade");

		assertThat(document.title()).isEqualTo("İptal ve İade Koşulları");
		assertThat(document.content()).isNull();
	}
}
