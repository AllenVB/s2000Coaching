package com.s2000coaching.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One session of one exercise. {@code sessionNumber} counts the member's sessions of that
 * exercise from 1, so "Antrenman 3" keeps its number as the window slides past ten sessions.
 * {@code topWeightKg} is null when the member recorded the sets without a weight.
 */
public record WeightEntry(int sessionNumber, LocalDate performedOn, BigDecimal topWeightKg) {
}
