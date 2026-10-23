package com.s2000coaching.repository;

import com.s2000coaching.domain.Message;
import com.s2000coaching.domain.MessageSender;
import com.s2000coaching.repository.projection.MemberUnreadCount;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

	List<Message> findAllByMemberIdOrderByCreatedAtAsc(UUID memberId);

	List<Message> findAllByMemberIdAndSenderAndReadAtIsNull(UUID memberId, MessageSender sender);

	long countByMemberIdAndSenderAndReadAtIsNull(UUID memberId, MessageSender sender);

	/** Everything the coach has not opened yet, across every thread — the panel's badge. */
	long countBySenderAndReadAtIsNull(MessageSender sender);

	/** Members with an unanswered message, newest waiting first — the coach's reply queue. */
	@Query("""
			select distinct m.member.id from Message m
			where m.sender = com.s2000coaching.domain.MessageSender.MEMBER
			  and m.readAt is null
			""")
	List<UUID> findMemberIdsWithUnreadMemberMessages();

	/**
	 * The newest message of every thread, for the inbox previews. A thread whose two newest
	 * messages share an instant yields both rows; the caller keeps one.
	 */
	@Query("""
			select m from Message m
			where m.createdAt = (select max(other.createdAt) from Message other
			                     where other.member.id = m.member.id)
			""")
	List<Message> findLatestPerMember();

	/** Unread counts per thread for one direction, so the inbox needs a single query. */
	@Query("""
			select new com.s2000coaching.repository.projection.MemberUnreadCount(m.member.id, count(m))
			from Message m
			where m.sender = :sender and m.readAt is null
			group by m.member.id
			""")
	List<MemberUnreadCount> countUnreadPerMember(@Param("sender") MessageSender sender);

	void deleteAllByMemberId(UUID memberId);
}
