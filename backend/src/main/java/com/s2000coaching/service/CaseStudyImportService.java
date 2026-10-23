package com.s2000coaching.service;

import com.s2000coaching.domain.CaseStudy;
import com.s2000coaching.domain.CaseStudyIntake;
import com.s2000coaching.domain.CaseStudyMeasurement;
import com.s2000coaching.exception.BadRequestException;
import com.s2000coaching.repository.CaseStudyIntakeRepository;
import com.s2000coaching.repository.CaseStudyMeasurementRepository;
import com.s2000coaching.web.dto.CsvPreviewResponse;
import com.s2000coaching.web.dto.ImportResultResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns an uploaded CSV into case study rows, in two steps: a preview the coach approves,
 * then the import itself using the mapping they confirmed.
 *
 * <p>Re-importing a date updates that row rather than adding a second one, so a corrected
 * export can simply be uploaded again.
 */
@Service
public class CaseStudyImportService {

	/** Enough rows for a wrong decimal mark or date order to be obvious on screen. */
	private static final int SAMPLE_ROWS = 5;

	/** A log of a human being: beyond this the file is something else, and worth refusing. */
	private static final int MAX_ROWS = 5000;

	private final CaseStudyMeasurementRepository measurementRepository;
	private final CaseStudyIntakeRepository intakeRepository;

	public CaseStudyImportService(CaseStudyMeasurementRepository measurementRepository,
			CaseStudyIntakeRepository intakeRepository) {
		this.measurementRepository = measurementRepository;
		this.intakeRepository = intakeRepository;
	}

	public CsvPreviewResponse previewMeasurements(byte[] content) {
		CsvParser.Table table = parseAndCheckSize(content);
		return preview(table, CsvColumnMatcher.guessMeasurementColumns(table.headers()),
				CsvColumnMatcher.measurementFields());
	}

	public CsvPreviewResponse previewIntake(byte[] content) {
		CsvParser.Table table = parseAndCheckSize(content);
		return preview(table, CsvColumnMatcher.guessIntakeColumns(table.headers()),
				CsvColumnMatcher.intakeFields());
	}

	@Transactional
	public ImportResultResponse importMeasurements(CaseStudy study, byte[] content,
			Map<String, Integer> mapping) {
		CsvParser.Table table = parseAndCheckSize(content);
		requireColumns(mapping, table, "measuredOn", "weightKg");

		List<String> warnings = new ArrayList<>();
		int imported = 0;
		int updated = 0;
		int skipped = 0;
		LocalDate first = null;
		LocalDate last = null;

		for (int i = 0; i < table.rows().size(); i++) {
			List<String> row = table.rows().get(i);
			// +2: the header is row 1, so the first data row is row 2 in the coach's spreadsheet.
			int rowNumber = i + 2;

			LocalDate date = CsvParser.parseDate(cell(row, mapping.get("measuredOn")), rowNumber);
			BigDecimal weight = CsvParser.parseDecimal(cell(row, mapping.get("weightKg")), rowNumber, "kilo");
			if (date == null || weight == null) {
				skipped++;
				warnings.add("%d. satır atlandı: tarih veya kilo boş.".formatted(rowNumber));
				continue;
			}

			var existing = measurementRepository.findByCaseStudyIdAndMeasuredOn(study.getId(), date);
			CaseStudyMeasurement measurement;
			if (existing.isPresent()) {
				measurement = existing.get();
				measurement.setWeightKg(weight);
				updated++;
			} else {
				measurement = measurementRepository.save(
						new CaseStudyMeasurement(UUID.randomUUID(), study, date, weight));
				imported++;
			}

			measurement.setNeckCm(decimal(row, mapping.get("neckCm"), rowNumber, "boyun"));
			measurement.setWaistCm(decimal(row, mapping.get("waistCm"), rowNumber, "bel"));
			measurement.setChestCm(decimal(row, mapping.get("chestCm"), rowNumber, "göğüs"));
			measurement.setHipCm(decimal(row, mapping.get("hipCm"), rowNumber, "kalça"));
			measurement.setArmCm(decimal(row, mapping.get("armCm"), rowNumber, "kol"));
			measurement.setThighCm(decimal(row, mapping.get("thighCm"), rowNumber, "bacak"));
			measurement.setShoulderCm(decimal(row, mapping.get("shoulderCm"), rowNumber, "omuz"));
			measurement.setBodyFatPercent(
					decimal(row, mapping.get("bodyFatPercent"), rowNumber, "yağ oranı"));

			first = first == null || date.isBefore(first) ? date : first;
			last = last == null || date.isAfter(last) ? date : last;
		}

		if (imported == 0 && updated == 0) {
			throw new BadRequestException("Hiçbir satır okunamadı. Sütun eşlemesini kontrol edin.");
		}
		return new ImportResultResponse(imported, updated, skipped, first, last, warnings);
	}

	@Transactional
	public ImportResultResponse importIntake(CaseStudy study, byte[] content, Map<String, Integer> mapping) {
		CsvParser.Table table = parseAndCheckSize(content);
		requireColumns(mapping, table, "consumedOn", "calories");

		List<String> warnings = new ArrayList<>();
		int imported = 0;
		int updated = 0;
		int skipped = 0;
		LocalDate first = null;
		LocalDate last = null;

		for (int i = 0; i < table.rows().size(); i++) {
			List<String> row = table.rows().get(i);
			int rowNumber = i + 2;

			LocalDate date = CsvParser.parseDate(cell(row, mapping.get("consumedOn")), rowNumber);
			Integer calories = CsvParser.parseInteger(cell(row, mapping.get("calories")), rowNumber, "kalori");
			if (date == null || calories == null) {
				skipped++;
				warnings.add("%d. satır atlandı: tarih veya kalori boş.".formatted(rowNumber));
				continue;
			}
			// A zero-calorie day is a day the log was not filled in, not a day of fasting. Kept
			// out because it would drag every average it touches towards a deficit that never
			// happened.
			if (calories <= 0) {
				skipped++;
				warnings.add("%d. satır atlandı: kalori sıfır veya negatif (%d)."
						.formatted(rowNumber, calories));
				continue;
			}

			var existing = intakeRepository.findByCaseStudyIdAndConsumedOn(study.getId(), date);
			CaseStudyIntake intake;
			if (existing.isPresent()) {
				intake = existing.get();
				intake.setCalories(calories);
				updated++;
			} else {
				intake = intakeRepository.save(new CaseStudyIntake(UUID.randomUUID(), study, date, calories));
				imported++;
			}

			intake.setProteinG(integer(row, mapping.get("proteinG"), rowNumber, "protein"));
			intake.setCarbsG(integer(row, mapping.get("carbsG"), rowNumber, "karbonhidrat"));
			intake.setFatG(integer(row, mapping.get("fatG"), rowNumber, "yağ"));
			intake.setSodiumMg(integer(row, mapping.get("sodiumMg"), rowNumber, "sodyum"));

			first = first == null || date.isBefore(first) ? date : first;
			last = last == null || date.isAfter(last) ? date : last;
		}

		if (imported == 0 && updated == 0) {
			throw new BadRequestException("Hiçbir satır okunamadı. Sütun eşlemesini kontrol edin.");
		}
		return new ImportResultResponse(imported, updated, skipped, first, last, warnings);
	}

	private CsvParser.Table parseAndCheckSize(byte[] content) {
		CsvParser.Table table = CsvParser.parse(content);
		if (table.rows().size() > MAX_ROWS) {
			throw new BadRequestException(
					"Dosyada %d satır var; en fazla %d satır işlenebilir."
							.formatted(table.rows().size(), MAX_ROWS));
		}
		return table;
	}

	private CsvPreviewResponse preview(CsvParser.Table table, Map<String, Integer> mapping,
			List<String> availableFields) {
		List<List<String>> sample = table.rows().stream().limit(SAMPLE_ROWS).toList();
		return new CsvPreviewResponse(table.headers(), mapping, availableFields, table.rows().size(), sample);
	}

	/** A mapping that points past the end of the row would read someone else's column. */
	private void requireColumns(Map<String, Integer> mapping, CsvParser.Table table, String... required) {
		for (String field : required) {
			if (!mapping.containsKey(field)) {
				throw new BadRequestException("Zorunlu sütun eşlenmedi: " + field);
			}
		}
		for (var entry : mapping.entrySet()) {
			int index = entry.getValue();
			if (index < 0 || index >= table.columnCount()) {
				throw new BadRequestException("\"%s\" için geçersiz sütun numarası: %d."
						.formatted(entry.getKey(), index));
			}
		}
	}

	private static String cell(List<String> row, Integer index) {
		return index == null || index >= row.size() ? null : row.get(index);
	}

	private static BigDecimal decimal(List<String> row, Integer index, int rowNumber, String label) {
		return CsvParser.parseDecimal(cell(row, index), rowNumber, label);
	}

	private static Integer integer(List<String> row, Integer index, int rowNumber, String label) {
		return CsvParser.parseInteger(cell(row, index), rowNumber, label);
	}
}
