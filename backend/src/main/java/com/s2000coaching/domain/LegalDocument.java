package com.s2000coaching.domain;

import java.util.Arrays;
import java.util.Optional;

/** The legal pages the site links to; the coach writes their text, the system never invents it. */
public enum LegalDocument {
	TERMS("kullanim-kosullari", "Kullanım Koşulları"),
	PRIVACY("kvkk", "KVKK & Aydınlatma Metni"),
	DISTANCE_SALES("mesafeli-satis", "Mesafeli Satış Sözleşmesi"),
	REFUND("iade", "İptal ve İade Koşulları");

	private final String slug;
	private final String title;

	LegalDocument(String slug, String title) {
		this.slug = slug;
		this.title = title;
	}

	public String slug() {
		return slug;
	}

	public String title() {
		return title;
	}

	public static Optional<LegalDocument> fromSlug(String slug) {
		return Arrays.stream(values()).filter(document -> document.slug.equals(slug)).findFirst();
	}
}
