package com.s2000coaching.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * What an import actually did. Skipped rows are returned rather than silently dropped: a
 * file that imports "successfully" while quietly discarding a third of its rows is worse
 * than one that fails, because the gap only shows up later as a wrong average.
 */
public record ImportResultResponse(
		int importedRows,
		int updatedRows,
		int skippedRows,
		LocalDate firstDate,
		LocalDate lastDate,
		/** One line per skipped row, naming the row number and the reason. */
		List<String> warnings) {
}
