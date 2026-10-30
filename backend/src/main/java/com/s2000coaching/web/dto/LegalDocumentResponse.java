package com.s2000coaching.web.dto;

import java.time.Instant;

/** One legal page; {@code content} is null until the coach has written it. */
public record LegalDocumentResponse(String slug, String title, String content, Instant updatedAt) {
}
