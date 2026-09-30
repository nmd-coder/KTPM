package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Custom OAuth2 User Service to handle Google and Facebook login
 * Creates or updates user accounts based on OAuth2 provider data
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Value("${facebook.avatar.url.template}")
    private String facebookAvatarUrlTemplate;

    private final UserRepository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            ex.printStackTrace(); // Print full stack trace for debugging
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + errorMessage);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String email = extractEmail(attributes, registrationId);
        String name = extractName(attributes, registrationId);
        String avatar = extractAvatar(attributes, registrationId);
        
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found in OAuth2 response");
        }
        
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;
        
        if (existingUser.isPresent()) {
            user = existingUser.get();
            boolean needsUpdate = updateUserInfo(user, name, avatar);
            // Only save if user was updated
            if (needsUpdate) {
                try {
                    user = userRepository.save(user);
                    entityManager.flush(); // Force commit immediately
                } catch (Exception e) {
                    // Handle save errors for existing users
                    if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                        try {
                            user = userRepository.save(user);
                            entityManager.flush(); // Force commit immediately
                        } catch (Exception retryException) {
                            throw new OAuth2AuthenticationException("Failed to save user: " + retryException.getMessage());
                        }
                    } else {
                        throw new OAuth2AuthenticationException("Failed to save user: " + e.getMessage());
                    }
                }
            }
        } else {
            user = createNewUser(email, name, avatar, registrationId);
            
            // Save new user
            try {
                user = userRepository.save(user);
                entityManager.flush(); // Force commit immediately
            } catch (Exception e) {
                if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                    try {
                        user = userRepository.save(user);
                        entityManager.flush(); // Force commit immediately
                    } catch (Exception retryException) {
                        throw new OAuth2AuthenticationException("Failed to save user: " + retryException.getMessage());
                    }
                } else {
                    throw new OAuth2AuthenticationException("Failed to save user: " + e.getMessage());
                }
            }
        }
        
        // Create authorities
        var authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        
        return new DefaultOAuth2User(authorities, attributes, "email");
    }

    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("email");
        } else if ("facebook".equals(registrationId)) {
            return (String) attributes.get("email");
        }
        return null;
    }

    private String extractName(Map<String, Object> attributes, String registrationId) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("name");
        } else if ("facebook".equals(registrationId)) {
            return (String) attributes.get("name");
        }
        return null;
    }

    private String extractAvatar(Map<String, Object> attributes, String registrationId) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("picture");
        } else if ("facebook".equals(registrationId)) {
            String id = (String) attributes.get("id");
            if (id != null) {
                return facebookAvatarUrlTemplate.replace("{id}", id);
            }
        }
        return null;
    }

    private boolean updateUserInfo(User user, String name, String avatar) {
        boolean needsUpdate = false;
        
        if (name != null && !name.isEmpty()) {
            String[] nameParts = splitName(name);
            if (!nameParts[0].equals(user.getFirstname()) || !nameParts[1].equals(user.getLastname())) {
                user.setFirstname(nameParts[0]);
                user.setLastname(nameParts[1]);
                needsUpdate = true;
            }
        }
        
        if (avatar != null && !avatar.isEmpty() && !avatar.equals(user.getAvatar())) {
            user.setAvatar(avatar);
            needsUpdate = true;
        }
        
        return needsUpdate;
    }

    private User createNewUser(String email, String name, String avatar, String registrationId) {
        // Generate unique phone number to avoid constraint violation
        String defaultPhone = "OAuth2_" + System.currentTimeMillis();
        
        // Split name into first and last name
        String[] nameParts = splitName(name);
        
        User user = new User();
        user.setFirstname(nameParts[0]);
        user.setLastname(nameParts.length > 1 ? nameParts[1] : "");
        user.setEmail(email);
        user.setPhone(defaultPhone);
        user.setPassword("oauth2_user_no_password"); // OAuth2 users don't have passwords - dummy password to satisfy constraint
        user.setAvatar(avatar);
        user.setRole(UserRole.CUSTOMER);
        
        return user;
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[]{"User", "Name"};
        }
        
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        } else if (parts.length == 2) {
            return new String[]{parts[0], parts[1]};
        } else {
            StringBuilder lastName = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (i > 1) lastName.append(" ");
                lastName.append(parts[i]);
            }
            return new String[]{parts[0], lastName.toString()};
        }
    }
}
