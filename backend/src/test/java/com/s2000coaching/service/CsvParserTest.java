package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.s2000coaching.exception.BadRequestException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CsvParserTest {

	private static byte[] bytes(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}

	@Nested
	class Structure {

		@Test
		void shouldReadAnEnglishStyleFile() {
			var table = CsvParser.parse(bytes("date,weight,waist\n2026-03-15,84.5,92\n2026-03-16,84.2,92"));

			assertThat(table.separator()).isEqualTo(',');
			assertThat(table.headers()).containsExactly("date", "weight", "waist");
			assertThat(table.rows()).hasSize(2);
			assertThat(table.rows().get(0)).containsExactly("2026-03-15", "84.5", "92");
		}

		@Test
		void shouldReadASemicolonFileAsTurkishExportsWrite() {
			var table = CsvParser.parse(bytes("Tarih;Kilo;Bel\n15.03.2026;84,5;92\n16.03.2026;84,2;92"));

			assertThat(table.separator()).isEqualTo(';');
			assertThat(table.headers()).containsExactly("Tarih", "Kilo", "Bel");
			assertThat(table.rows().get(0)).containsExactly("15.03.2026", "84,5", "92");
		}

		/** Excel writes one; left in place it becomes part of the first header name. */
		@Test
		void shouldStripTheByteOrderMark() {
			var table = CsvParser.parse(bytes("﻿date,weight\n2026-03-15,84.5"));

			assertThat(table.headers().get(0)).isEqualTo("date");
		}

		@Test
		void shouldKeepASeparatorThatSitsInsideQuotes() {
			var table = CsvParser.parse(bytes("date,note,weight\n2026-03-15,\"ağır, yorgun gün\",84.5"));

			assertThat(table.rows().get(0)).containsExactly("2026-03-15", "ağır, yorgun gün", "84.5");
		}

		/** Spreadsheets routinely drop trailing empty columns. */
		@Test
		void shouldPadAShortRowRatherThanRejectIt() {
			var table = CsvParser.parse(bytes("date,weight,waist\n2026-03-15,84.5"));

			assertThat(table.rows().get(0)).containsExactly("2026-03-15", "84.5", "");
		}

		@Test
		void shouldSkipBlankLines() {
			var table = CsvParser.parse(bytes("date,weight\n2026-03-15,84.5\n\n2026-03-16,84.2\n"));

			assertThat(table.rows()).hasSize(2);
		}

		@Test
		void shouldRefuseAnEmptyFile() {
			assertThatThrownBy(() -> CsvParser.parse(bytes("")))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("boş");
		}

		@Test
		void shouldRefuseAFileWithNoRecognisableHeader() {
			assertThatThrownBy(() -> CsvParser.parse(bytes("sadecebirsutun\n84.5")))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Başlık satırı");
		}
	}

	@Nested
	class Dates {

		@ParameterizedTest
		@ValueSource(strings = { "2026-03-15", "15.03.2026", "15/03/2026", "2026/03/15", "15.3.2026" })
		void shouldReadTheCommonDateShapes(String value) {
			assertThat(CsvParser.parseDate(value, 2)).isEqualTo(LocalDate.of(2026, 3, 15));
		}

		@Test
		void shouldIgnoreATimeAppendedToTheDate() {
			assertThat(CsvParser.parseDate("2026-03-15 08:30", 2)).isEqualTo(LocalDate.of(2026, 3, 15));
			assertThat(CsvParser.parseDate("2026-03-15T08:30:00", 2)).isEqualTo(LocalDate.of(2026, 3, 15));
		}

		@Test
		void shouldAnswerNullForAnEmptyCell() {
			assertThat(CsvParser.parseDate("", 2)).isNull();
			assertThat(CsvParser.parseDate(null, 2)).isNull();
		}

		/** The row number is in the message because that is what the coach has to go fix. */
		@Test
		void shouldNameTheRowWhenTheDateIsUnreadable() {
			assertThatThrownBy(() -> CsvParser.parseDate("dün", 7))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("7. satır");
		}
	}

	@Nested
	class Numbers {

		@ParameterizedTest
		@CsvSource({
				"'84.5', 84.5",
				"'84,5', 84.5",
				"'1.234,5', 1234.5",
				"'1,234.5', 1234.5",
				"'2500', 2500",
				"'2.500', 2.500",
				"'1,234', 1234",
		})
		void shouldReadBothDecimalMarks(String input, String expected) {
			assertThat(CsvParser.parseDecimal(input, 2, "kilo"))
					.isEqualByComparingTo(new BigDecimal(expected));
		}

		/**
		 * The dangerous case. Guessing wrong here turns 1234.5 into 1.2345, and the mistake
		 * would travel into a published figure without ever looking wrong on screen.
		 */
		@Test
		void shouldTreatTheLastMarkAsTheDecimalPoint() {
			assertThat(CsvParser.parseDecimal("1.234,56", 2, "kalori"))
					.isEqualByComparingTo(new BigDecimal("1234.56"));
			assertThat(CsvParser.parseDecimal("1,234.56", 2, "kalori"))
					.isEqualByComparingTo(new BigDecimal("1234.56"));
		}

		@Test
		void shouldStripSpacesUsedAsThousandsSeparators() {
			assertThat(CsvParser.parseDecimal("2 500", 2, "kalori"))
					.isEqualByComparingTo(new BigDecimal("2500"));
			assertThat(CsvParser.parseDecimal("2 500", 2, "kalori"))
					.isEqualByComparingTo(new BigDecimal("2500"));
		}

		@Test
		void shouldRoundWhenAnIntegerIsWanted() {
			assertThat(CsvParser.parseInteger("2500,6", 2, "kalori")).isEqualTo(2501);
			assertThat(CsvParser.parseInteger("2500,4", 2, "kalori")).isEqualTo(2500);
		}

		@Test
		void shouldAnswerNullForAnEmptyCell() {
			assertThat(CsvParser.parseDecimal("", 2, "kilo")).isNull();
			assertThat(CsvParser.parseInteger("  ", 2, "kalori")).isNull();
		}

		@Test
		void shouldNameTheRowAndColumnWhenANumberIsUnreadable() {
			assertThatThrownBy(() -> CsvParser.parseDecimal("yaklaşık 85", 9, "kilo"))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("9. satır")
					.hasMessageContaining("kilo");
		}
	}

	@Nested
	class HeaderNormalisation {

		@ParameterizedTest
		@CsvSource({ "Kalori, kalori", "KALORİ, kalori", "Ağırlık, agirlik", "Göğüs, gogus",
				"'Kilo (kg)', kilokg", "Protein_g, proteing" })
		void shouldFoldTurkishLettersAndPunctuation(String input, String expected) {
			assertThat(CsvParser.normalizeHeader(input)).isEqualTo(expected);
		}
	}
}
