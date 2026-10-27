package com.s2000coaching.web.dto;

import jakarta.validation.constraints.AssertTrue;

/** Consent has to be given as an explicit true; hitting the URL alone is refused. */
public record StoryConsentRequest(@AssertTrue(message = "Onay için açıkça kabul etmeniz gerekiyor.") boolean agreed) {
}
