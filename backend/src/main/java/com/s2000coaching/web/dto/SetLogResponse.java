package com.s2000coaching.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SetLogResponse(UUID planExerciseId, int setNumber, int reps, BigDecimal weightKg, BigDecimal rpe) {
}
