package com.s2000coaching.web.dto;

import jakarta.validation.constraints.AssertTrue;

/**
 * Publishing photos is its own, explicit step. The flag has to be sent as true — a request
 * that merely hits the URL is refused — so it cannot happen by a stray click or a script
 * that was only meant to publish the numbers.
 */
public record PublishPhotosRequest(@AssertTrue(message = "Fotoğrafları yayınlamak için açık onay gerekiyor.") boolean confirmed) {
}
