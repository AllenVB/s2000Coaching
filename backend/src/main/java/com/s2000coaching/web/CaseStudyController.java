package com.s2000coaching.web;

import com.s2000coaching.service.CaseStudyService;
import com.s2000coaching.web.dto.PublicCaseStudyResponse;
import com.s2000coaching.web.dto.PublicCaseStudySummaryResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public read of a case study, for the Tecrübelerim section.
 *
 * <p>Both routes go through the published-only queries. An unpublished draft is not reachable
 * here even with its exact slug, which is the only reason it is safe for the coach to import
 * a messy file and look at the result before deciding to show it.
 */
@RestController
public class CaseStudyController {

	private final CaseStudyService caseStudyService;

	public CaseStudyController(CaseStudyService caseStudyService) {
		this.caseStudyService = caseStudyService;
	}

	@GetMapping("/api/case-studies")
	public List<PublicCaseStudySummaryResponse> list() {
		return caseStudyService.listPublished();
	}

	@GetMapping("/api/case-studies/{slug}")
	public PublicCaseStudyResponse read(@PathVariable String slug) {
		return caseStudyService.readPublished(slug);
	}
}
