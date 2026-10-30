package com.s2000coaching.service;

import com.s2000coaching.domain.LegalDocument;
import com.s2000coaching.domain.SiteSetting;
import com.s2000coaching.exception.NotFoundException;
import com.s2000coaching.repository.SiteSettingRepository;
import com.s2000coaching.web.dto.LegalDocumentResponse;
import com.s2000coaching.web.dto.SiteSettingsResponse;
import com.s2000coaching.web.dto.SiteSettingsResponse.LegalSummary;
import com.s2000coaching.web.dto.SiteSettingsResponse.Social;
import com.s2000coaching.web.dto.SiteSettingsResponse.Stat;
import com.s2000coaching.web.dto.UpdateSiteSettingsRequests;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * What the coach edits about the public site without a deployment: social links, the figures at the
 * top of the home page and the legal pages. A setting with no row is "not set" — the site shows its
 * built-in default, leaves the link out, or says the text is not published — and nothing is ever
 * made up to fill the gap.
 */
@Service
public class SiteSettingsService {

	private static final Logger log = LoggerFactory.getLogger(SiteSettingsService.class);

	static final String INSTAGRAM = "social.instagram";
	static final String YOUTUBE = "social.youtube";
	static final String WHATSAPP = "social.whatsapp";
	static final String STATS = "stats";
	private static final String LEGAL_PREFIX = "legal.";

	private final SiteSettingRepository repository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public SiteSettingsService(SiteSettingRepository repository, ObjectMapper objectMapper, Clock clock) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	/** Everyone's view: links, figures, and which legal pages have text (not the text itself). */
	@Transactional(readOnly = true)
	public SiteSettingsResponse publicSettings() {
		Map<String, SiteSetting> settings = all();
		List<LegalSummary> legal = Arrays.stream(LegalDocument.values())
				.map(document -> new LegalSummary(document.slug(), document.title(),
						settings.containsKey(legalKey(document))))
				.toList();
		return new SiteSettingsResponse(social(settings), stats(settings), legal);
	}

	@Transactional(readOnly = true)
	public LegalDocumentResponse legalDocument(String slug) {
		LegalDocument document = LegalDocument.fromSlug(slug)
				.orElseThrow(() -> new NotFoundException("Sayfa bulunamadı."));
		return toResponse(document, repository.findById(legalKey(document)).orElse(null));
	}

	@Transactional(readOnly = true)
	public SiteSettingsResponse.Admin adminSettings() {
		Map<String, SiteSetting> settings = all();
		List<LegalDocumentResponse> legal = Arrays.stream(LegalDocument.values())
				.map(document -> toResponse(document, settings.get(legalKey(document))))
				.toList();
		return new SiteSettingsResponse.Admin(social(settings), stats(settings), legal);
	}

	@Transactional
	public SiteSettingsResponse.Admin updateSocial(UpdateSiteSettingsRequests.Social request) {
		// All three are checked before any is written, so one bad link leaves the others as they were.
		String instagram = SiteLinks.instagram(request.instagram());
		String youtube = SiteLinks.youtube(request.youtube());
		String whatsapp = SiteLinks.whatsapp(request.whatsapp());
		put(INSTAGRAM, instagram);
		put(YOUTUBE, youtube);
		put(WHATSAPP, whatsapp);
		return adminSettings();
	}

	@Transactional
	public SiteSettingsResponse.Admin updateStats(UpdateSiteSettingsRequests.Stats request) {
		if (request.stats() == null) {
			put(STATS, null);
		} else {
			List<Stat> stats = request.stats().stream()
					.map(stat -> new Stat(stat.label().trim(), stat.value().trim(), stat.caption().trim(),
							blankToEmpty(stat.footnote()), stat.accent()))
					.toList();
			put(STATS, objectMapper.writeValueAsString(stats));
		}
		return adminSettings();
	}

	@Transactional
	public SiteSettingsResponse.Admin updateLegal(String slug, UpdateSiteSettingsRequests.Legal request) {
		LegalDocument document = LegalDocument.fromSlug(slug)
				.orElseThrow(() -> new NotFoundException("Sayfa bulunamadı."));
		String content = request.content() == null || request.content().isBlank() ? null : request.content().strip();
		put(legalKey(document), content);
		return adminSettings();
	}

	/** Writes the value, or removes the row when there is none. */
	private void put(String key, String value) {
		if (value == null) {
			repository.deleteById(key);
			return;
		}
		Instant now = Instant.now(clock);
		repository.findById(key).ifPresentOrElse(
				existing -> existing.change(value, now),
				() -> repository.save(new SiteSetting(key, value, now)));
	}

	private Map<String, SiteSetting> all() {
		return repository.findAll().stream().collect(Collectors.toMap(SiteSetting::getKey, Function.identity()));
	}

	private static Social social(Map<String, SiteSetting> settings) {
		return new Social(valueOf(settings, INSTAGRAM), valueOf(settings, YOUTUBE), valueOf(settings, WHATSAPP));
	}

	/** Null when never set; a stored list that no longer parses counts as never set rather than breaking the home page. */
	private List<Stat> stats(Map<String, SiteSetting> settings) {
		String stored = valueOf(settings, STATS);
		if (stored == null) {
			return null;
		}
		try {
			return objectMapper.readValue(stored, new TypeReference<List<Stat>>() {
			});
		} catch (JacksonException e) {
			log.warn("Kayıtlı site rakamları okunamadı, varsayılanlar gösterilecek", e);
			return null;
		}
	}

	private static LegalDocumentResponse toResponse(LegalDocument document, SiteSetting setting) {
		return new LegalDocumentResponse(document.slug(), document.title(),
				setting == null ? null : setting.getValue(), setting == null ? null : setting.getUpdatedAt());
	}

	private static String legalKey(LegalDocument document) {
		return LEGAL_PREFIX + document.slug();
	}

	private static String valueOf(Map<String, SiteSetting> settings, String key) {
		SiteSetting setting = settings.get(key);
		return setting == null ? null : setting.getValue();
	}

	private static String blankToEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}
