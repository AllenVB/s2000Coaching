package com.s2000coaching.web.dto;

/**
 * What the single sign-in answers with: who signed in, and that side's own session. Exactly one
 * of {@code coach} and {@code member} is set, matching {@code role}.
 */
public record AccountSessionResponse(Role role, CoachSessionResponse coach, MemberSessionResponse member) {

	public enum Role {
		COACH, MEMBER
	}

	public static AccountSessionResponse ofCoach(CoachSessionResponse session) {
		return new AccountSessionResponse(Role.COACH, session, null);
	}

	public static AccountSessionResponse ofMember(MemberSessionResponse session) {
		return new AccountSessionResponse(Role.MEMBER, null, session);
	}
}
