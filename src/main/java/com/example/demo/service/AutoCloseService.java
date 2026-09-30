package com.example.demo.service;

import com.example.demo.entity.Conversation;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.ConversationStatus;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service tự động đóng conversation sau 30 phút không hoạt động
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCloseService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    
    private static final int IDLE_MINUTES = 30;
    
    /**
     * Quét và tự động đóng conversation sau 30 phút không hoạt động
     * Chạy mỗi 5 phút
     */
    @Scheduled(fixedDelay = 300000) // 5 phút = 300,000 ms
    @Transactional
    public void autoCloseIdleConversations() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(IDLE_MINUTES);
            
            // Tìm conversations đang ASSIGNED và không có tin nhắn mới >= 30 phút
            List<Conversation> idleConversations = conversationRepository.findIdleConversations(
                ConversationStatus.ASSIGNED, 
                threshold
            );
            
            if (idleConversations.isEmpty()) {
                log.debug("No idle conversations to auto-close");
                return;
            }
            
            log.info("Found {} idle conversations to auto-close", idleConversations.size());
            
            User systemUser = getOrCreateSystemUser();
            
            for (Conversation conversation : idleConversations) {
                try {
                    // Đóng conversation
                    conversation.setStatus(ConversationStatus.CLOSED);
                    conversationRepository.save(conversation);
                    
                    // Gửi system message thông báo
                    Message systemMessage = new Message();
                    systemMessage.setConversationId(conversation.getId());
                    systemMessage.setSender(systemUser);
                    systemMessage.setContent("Cuộc hội thoại đã được tự động đóng do không có hoạt động trong " + IDLE_MINUTES + " phút. Bạn có thể nhắn lại bất cứ lúc nào, chúng tôi luôn sẵn sàng hỗ trợ! 🌸");
                    systemMessage.setIsAiGenerated(false);
                    systemMessage.setSentAt(LocalDateTime.now());
                    messageRepository.save(systemMessage);
                    
                    // Gửi WebSocket update
                    webSocketService.sendConversationUpdate(conversation.getId(), "status_change", null);
                    
                    log.info("✅ Auto-closed conversation {} (customer: {})", 
                        conversation.getId(), 
                        conversation.getCustomer().getFullName());
                        
                } catch (Exception e) {
                    log.error("Error auto-closing conversation {}: {}", conversation.getId(), e.getMessage(), e);
                }
            }
            
            log.info("Auto-close completed: {} conversations closed", idleConversations.size());
            
        } catch (Exception e) {
            log.error("Error in auto-close scheduled task", e);
        }
    }
    
    /**
     * Get or create system user for system messages
     */
    private User getOrCreateSystemUser() {
        return userRepository.findByEmail("system@local").orElseGet(() -> {
            log.info("Creating system user for auto-close messages");
            User systemUser = new User();
            systemUser.setEmail("system@local");
            systemUser.setFirstname("Hệ thống");
            systemUser.setLastname("StarShop");
            systemUser.setPassword("$2a$10$" + System.currentTimeMillis()); // BCrypt placeholder
            systemUser.setPhone("0000000001");
            systemUser.setRole(UserRole.STAFF);
            return userRepository.save(systemUser);
        });
    }
}

