package com.s2000coaching.web.dto;

import java.util.List;
import java.util.Map;

/**
 * What the coach sees before anything is written: the headers found, the guessed mapping,
 * and the first few rows as they were actually read.
 *
 * <p>The sample rows matter more than they look. They are the only place a wrong decimal
 * mark or a misread date shows itself while it is still cheap to fix — once imported, a
 * number that is off by a factor of a thousand looks like an ordinary number.
 */
public record CsvPreviewResponse(
		List<String> headers,
		/** Canonical field name to column index; a field missing here matched nothing. */
		Map<String, Integer> guessedMapping,
		/** Fields this import understands, so the UI can offer the full list. */
		List<String> availableFields,
		int totalRows,
		List<List<String>> sampleRows) {
}
