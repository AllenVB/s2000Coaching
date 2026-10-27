package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class YouTubeLinkTest {

	private static final String ID = "dQw4w9WgXcQ";

	@ParameterizedTest
	@ValueSource(strings = {
			"https://www.youtube.com/watch?v=dQw4w9WgXcQ",
			"https://youtube.com/watch?v=dQw4w9WgXcQ&feature=share",
			"https://m.youtube.com/watch?v=dQw4w9WgXcQ",
			"https://www.youtube.com/watch?list=PL123&v=dQw4w9WgXcQ",
			"https://youtu.be/dQw4w9WgXcQ",
			"https://youtu.be/dQw4w9WgXcQ?si=abcdef",
			"https://www.youtube.com/shorts/dQw4w9WgXcQ",
			"https://www.youtube.com/embed/dQw4w9WgXcQ",
			"https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ",
			"https://www.youtube.com/live/dQw4w9WgXcQ",
			"http://www.youtube.com/watch?v=dQw4w9WgXcQ",
			"youtu.be/dQw4w9WgXcQ",
			"  https://youtu.be/dQw4w9WgXcQ  "})
	void shouldExtractTheIdFromEveryCommonForm(String link) {
		assertThat(YouTubeLink.parse(link)).hasValueSatisfying(p -> assertThat(p.videoId()).isEqualTo(ID));
	}

	@ParameterizedTest
	@CsvSource({
			"https://youtu.be/dQw4w9WgXcQ?t=90,90",
			"https://youtu.be/dQw4w9WgXcQ?t=90s,90",
			"https://youtu.be/dQw4w9WgXcQ?t=1m30s,90",
			"https://youtu.be/dQw4w9WgXcQ?t=1h2m3s,3723",
			"https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=45,45",
			"https://www.youtube.com/watch?v=dQw4w9WgXcQ&start=12,12",
			"https://youtu.be/dQw4w9WgXcQ?t=abc,0",
			"https://youtu.be/dQw4w9WgXcQ?t=99999999,0",
			"https://youtu.be/dQw4w9WgXcQ,0"})
	void shouldReadTheStartTimeAndIgnoreOneThatMakesNoSense(String link, int expected) {
		assertThat(YouTubeLink.parse(link)).hasValueSatisfying(p -> assertThat(p.startSeconds()).isEqualTo(expected));
	}

	/**
	 * Anything that is not a YouTube video is refused — the iframe address is built from the id,
	 * so a link may only ever choose which YouTube video plays.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
			"https://vimeo.com/123456789",
			"https://evil.example/watch?v=dQw4w9WgXcQ",
			"https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ",
			"https://evil.example/?u=https://youtu.be/dQw4w9WgXcQ",
			"javascript:alert(1)",
			"ftp://youtu.be/dQw4w9WgXcQ",
			"https://www.youtube.com/watch?v=short",
			"https://www.youtube.com/watch?v=dQw4w9WgXcQ%22%3E%3Cscript%3E",
			"https://www.youtube.com/playlist?list=PL1234567890A",
			"https://www.youtube.com/",
			"not a link at all",
			""})
	void shouldRefuseWhatIsNotAYouTubeVideo(String link) {
		assertThat(YouTubeLink.parse(link)).isEmpty();
	}

	@Test
	void shouldReturnEmptyForNull() {
		assertThat(YouTubeLink.parse(null)).isEmpty();
	}

	@Test
	void shouldBuildOneCanonicalAddress() {
		assertThat(YouTubeLink.parse("youtu.be/dQw4w9WgXcQ?t=1m30s").orElseThrow().canonicalUrl())
				.isEqualTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=90");
		assertThat(YouTubeLink.parse("https://m.youtube.com/watch?v=dQw4w9WgXcQ").orElseThrow().canonicalUrl())
				.isEqualTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
	}
}
