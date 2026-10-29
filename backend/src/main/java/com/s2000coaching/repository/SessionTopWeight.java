package com.s2000coaching.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The heaviest weight a member recorded for one exercise in one session. A row exists for every
 * session in which at least one set of the exercise was recorded; {@code topWeightKg} is null
 * when none of those sets carried a weight.
 */
public record SessionTopWeight(UUID planExerciseId, LocalDate performedOn, BigDecimal topWeightKg) {
}
