package com.example.demo.repository;

import com.example.demo.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m WHERE (m.sender.id = :userId1 AND m.receiver.id = :userId2) OR (m.sender.id = :userId2 AND m.receiver.id = :userId1) ORDER BY m.sentAt DESC")
    Page<Message> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver.id = :receiverId AND m.sender.id = :senderId")
    void markMessagesAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);
    
    @Query("SELECT DISTINCT CASE WHEN m.sender.id = :userId THEN m.receiver ELSE m.sender END FROM Message m WHERE m.sender.id = :userId OR m.receiver.id = :userId")
    List<Object> findConversationPartners(@Param("userId") Long userId);
    
    // New methods for conversation-based chat
    
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.sentAt ASC")
    List<Message> findByConversationIdOrderBySentAtAsc(@Param("conversationId") Long conversationId);
    
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.sentAt DESC")
    Page<Message> findByConversationIdOrderBySentAtDesc(@Param("conversationId") Long conversationId, Pageable pageable);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.receiver.id = :receiverId AND m.isRead = false")
    Long countUnreadByConversationIdAndReceiverId(@Param("conversationId") Long conversationId, @Param("receiverId") Long receiverId);
    
    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId ORDER BY m.sentAt DESC, m.id DESC")
    List<Message> findLastMessageByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.receiver.id = :receiverId")
    void markConversationMessagesAsRead(@Param("conversationId") Long conversationId, @Param("receiverId") Long receiverId);
}
