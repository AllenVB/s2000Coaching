package com.s2000coaching.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** One of the member's progress photos to copy into the story, with the label shown under it. */
public record StoryPhotoSelection(@NotNull UUID progressPhotoId, @NotBlank @Size(max = 60) String caption) {
}
