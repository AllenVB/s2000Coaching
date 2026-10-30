package com.s2000coaching.service;

import com.s2000coaching.exception.BadRequestException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns what the coach typed into the address the footer links to, refusing anything that is not
 * on the network it claims to be. A link on the public site is a link every visitor will click, so
 * the host is checked rather than trusted, and only {@code https} is ever stored.
 */
final class SiteLinks {

	private static final Set<String> INSTAGRAM_HOSTS = Set.of("instagram.com", "www.instagram.com");
	private static final Set<String> YOUTUBE_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be");
	private static final Set<String> WHATSAPP_HOSTS = Set.of("wa.me", "api.whatsapp.com");
	private static final Pattern INSTAGRAM_USER = Pattern.compile("[A-Za-z0-9._]{1,30}");

	private SiteLinks() {
	}

	/** A user name ("kullanici", "@kullanici") or an instagram.com address; blank means "no link". */
	static String instagram(String raw) {
		String value = trimmed(raw);
		if (value == null) {
			return null;
		}
		if (looksLikeAddress(value)) {
			return onHost(value, INSTAGRAM_HOSTS, "Instagram");
		}
		String user = value.startsWith("@") ? value.substring(1) : value;
		if (!INSTAGRAM_USER.matcher(user).matches()) {
			throw new BadRequestException("Instagram için bir kullanıcı adı ya da instagram.com adresi yazın.");
		}
		return "https://www.instagram.com/" + user;
	}

	/** A youtube.com or youtu.be address; blank means "no link". */
	static String youtube(String raw) {
		String value = trimmed(raw);
		return value == null ? null : onHost(value, YOUTUBE_HOSTS, "YouTube");
	}

	/** A phone number (any way of writing it) or a wa.me address; blank means "no link". */
	static String whatsapp(String raw) {
		String value = trimmed(raw);
		if (value == null) {
			return null;
		}
		if (looksLikeAddress(value)) {
			return onHost(value, WHATSAPP_HOSTS, "WhatsApp");
		}
		String digits = value.replaceAll("[^0-9]", "");
		// 05551112233 is how a Turkish number is written at home; the link wants it with the country code.
		if (digits.length() == 11 && digits.startsWith("0")) {
			digits = "9" + digits;
		} else if (digits.length() == 10) {
			digits = "90" + digits;
		}
		if (digits.length() < 11 || digits.length() > 15) {
			throw new BadRequestException("WhatsApp için ülke koduyla bir telefon numarası yazın (örn. +90 555 111 22 33).");
		}
		return "https://wa.me/" + digits;
	}

	private static String trimmed(String raw) {
		return raw == null || raw.isBlank() ? null : raw.trim();
	}

	private static boolean looksLikeAddress(String value) {
		return value.contains("/") || value.contains(".com") || value.startsWith("http");
	}

	private static String onHost(String value, Set<String> hosts, String network) {
		String address = value.startsWith("http") ? value : "https://" + value;
		try {
			URI uri = new URI(address);
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || !hosts.contains(host)) {
				throw new BadRequestException("%s bağlantısı https ile başlayan bir %s adresi olmalı.".formatted(network, network));
			}
			return uri.toString();
		} catch (URISyntaxException e) {
			throw new BadRequestException("%s bağlantısı geçerli bir adres değil.".formatted(network));
		}
	}
}
