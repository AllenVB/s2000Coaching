package com.s2000coaching.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Understands the YouTube links a coach actually pastes — watch, short, share, embed, live,
 * with or without a start time — and reduces them to the 11-character video id.
 *
 * <p>Only the id is ever used to build the player address, and only on the YouTube hosts below.
 * A free-form URL put into an {@code <iframe src>} would let anything be embedded in a page
 * members are signed in on; extracting the id and building the address ourselves means the
 * only thing a link can choose is which YouTube video plays.
 */
public final class YouTubeLink {

	private static final Set<String> HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com",
			"music.youtube.com", "youtube-nocookie.com", "www.youtube-nocookie.com", "youtu.be");

	private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
	private static final Pattern PATH_ID = Pattern.compile("^/(?:shorts|embed|live|v)/([^/?#]+)");
	private static final Pattern CLOCK = Pattern.compile("^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$");

	/** A start offset beyond this is a typo, not a timestamp. */
	private static final long MAX_START_SECONDS = 12L * 3600;

	private YouTubeLink() {
	}

	public record Parsed(String videoId, int startSeconds) {

		/** The one address form stored, so the same video always looks the same in the catalogue. */
		public String canonicalUrl() {
			return "https://www.youtube.com/watch?v=" + videoId + (startSeconds > 0 ? "&t=" + startSeconds : "");
		}
	}

	public static Optional<Parsed> parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		String text = raw.trim();
		if (!text.contains("://")) {
			text = "https://" + text;
		}
		URI uri;
		try {
			uri = new URI(text);
		} catch (URISyntaxException e) {
			return Optional.empty();
		}
		String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
		String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
		if ((!scheme.equals("https") && !scheme.equals("http")) || !HOSTS.contains(host)) {
			return Optional.empty();
		}

		String path = uri.getPath() == null ? "" : uri.getPath();
		String query = uri.getRawQuery();
		String id = null;
		if (host.equals("youtu.be")) {
			id = path.length() > 1 ? path.substring(1).split("/")[0] : null;
		} else if (path.equals("/watch")) {
			id = queryParam(query, "v");
		} else {
			Matcher matcher = PATH_ID.matcher(path);
			if (matcher.find()) {
				id = matcher.group(1);
			}
		}
		if (id == null || !ID.matcher(id).matches()) {
			return Optional.empty();
		}

		String start = firstNonNull(queryParam(query, "t"), queryParam(query, "start"));
		if (start == null && uri.getRawFragment() != null && uri.getRawFragment().startsWith("t=")) {
			start = uri.getRawFragment().substring(2);
		}
		return Optional.of(new Parsed(id, startSeconds(start)));
	}

	/** "90", "90s" and "1m30s" are all in use; anything unreadable just means "from the start". */
	static int startSeconds(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		Matcher clock = CLOCK.matcher(value.trim().toLowerCase(Locale.ROOT));
		long seconds;
		if (value.trim().matches("\\d+")) {
			seconds = parseLong(value.trim());
		} else if (clock.matches() && !value.isBlank()) {
			seconds = parseLong(clock.group(1)) * 3600 + parseLong(clock.group(2)) * 60 + parseLong(clock.group(3));
		} else {
			return 0;
		}
		return seconds > 0 && seconds <= MAX_START_SECONDS ? (int) seconds : 0;
	}

	private static long parseLong(String digits) {
		if (digits == null || digits.length() > 7) {
			return digits == null ? 0 : Long.MAX_VALUE / 4;
		}
		return Long.parseLong(digits);
	}

	private static String queryParam(String rawQuery, String name) {
		if (rawQuery == null) {
			return null;
		}
		for (String pair : rawQuery.split("&")) {
			int eq = pair.indexOf('=');
			if (eq > 0 && pair.substring(0, eq).equals(name)) {
				return pair.substring(eq + 1);
			}
		}
		return null;
	}

	private static String firstNonNull(String a, String b) {
		return a != null ? a : b;
	}
}
