package com.s2000coaching.repository;

import com.s2000coaching.domain.Message;
import com.s2000coaching.domain.MessageSender;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, UUID> {

	List<Message> findAllByMemberIdOrderByCreatedAtAsc(UUID memberId);

	List<Message> findAllByMemberIdAndSenderAndReadAtIsNull(UUID memberId, MessageSender sender);

	long countByMemberIdAndSenderAndReadAtIsNull(UUID memberId, MessageSender sender);

	/** Members with an unanswered message, newest waiting first — the coach's reply queue. */
	@Query("""
			select distinct m.member.id from Message m
			where m.sender = com.s2000coaching.domain.MessageSender.MEMBER
			  and m.readAt is null
			""")
	List<UUID> findMemberIdsWithUnreadMemberMessages();

	void deleteAllByMemberId(UUID memberId);
}
