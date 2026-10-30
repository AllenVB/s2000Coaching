package com.s2000coaching.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.exception.UnauthorizedException;
import com.s2000coaching.service.CoachAuthService;
import com.s2000coaching.service.SiteSettingsService;
import com.s2000coaching.web.dto.LegalDocumentResponse;
import com.s2000coaching.web.dto.SiteSettingsResponse;
import com.s2000coaching.web.dto.SiteSettingsResponse.Social;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest({ SiteController.class, AdminSiteSettingsController.class })
class SiteControllersTest {

	private static final String COACH = "Bearer coach-token";

	@Autowired
	private MockMvcTester mvc;
	@MockitoBean
	private SiteSettingsService service;
	@MockitoBean
	private CoachAuthService coachAuthService;

	@BeforeEach
	void coachSession() {
		doThrow(new UnauthorizedException("Oturum yok.")).when(coachAuthService).requireCoach(any());
		doReturn(null).when(coachAuthService).requireCoach(COACH);
	}

	@Test
	void shouldServeThePublicSettingsWithoutASession() {
		when(service.publicSettings()).thenReturn(new SiteSettingsResponse(
				new Social("https://www.instagram.com/s2000", null, null), null, List.of()));

		var result = mvc.get().uri("/api/site");

		assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("$.social.instagram")
				.isEqualTo("https://www.instagram.com/s2000");
	}

	@Test
	void shouldServeALegalPageWithoutASessionAnd404ForAnUnknownOne() {
		when(service.legalDocument("kvkk")).thenReturn(new LegalDocumentResponse("kvkk", "KVKK", "Metin", null));
		when(service.legalDocument("baska")).thenThrow(new NotFoundException("Sayfa bulunamadı."));

		assertThat(mvc.get().uri("/api/site/legal/kvkk")).hasStatus(HttpStatus.OK)
				.bodyJson().extractingPath("$.content").isEqualTo("Metin");
		assertThat(mvc.get().uri("/api/site/legal/baska")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldKeepTheEditorBehindTheCoachSession() {
		assertThat(mvc.get().uri("/api/admin/site-settings")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.put().uri("/api/admin/site-settings/social").contentType(MediaType.APPLICATION_JSON)
				.content("{\"instagram\":\"x\"}")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.put().uri("/api/admin/site-settings/legal/kvkk").contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"x\"}")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldLetTheCoachSaveLinksAndAnswerABadOneWith400() {
		when(service.updateSocial(any())).thenThrow(new BadRequestException("Instagram bağlantısı geçersiz."));

		assertThat(mvc.put().uri("/api/admin/site-settings/social").header(HttpHeaders.AUTHORIZATION, COACH)
				.contentType(MediaType.APPLICATION_JSON).content("{\"instagram\":\"https://evil.example\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verify(service).updateSocial(any());
	}

	@Test
	void shouldRefuseMoreThanFourStats() {
		String stat = "{\"label\":\"a\",\"value\":\"1\",\"caption\":\"b\",\"footnote\":\"\",\"accent\":false}";
		String body = "{\"stats\":[" + String.join(",", stat, stat, stat, stat, stat) + "]}";

		assertThat(mvc.put().uri("/api/admin/site-settings/stats").header(HttpHeaders.AUTHORIZATION, COACH)
				.contentType(MediaType.APPLICATION_JSON).content(body)).hasStatus(HttpStatus.BAD_REQUEST);
	}
}
