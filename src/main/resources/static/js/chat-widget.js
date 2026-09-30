/**
 * Customer Chat Widget - WebSocket Integration
 * Handles real-time chat between customers and staff
 */

let chatWidgetStompClient = null;
let chatWidgetConversationId = null;
let chatWidgetUserId = null;
let chatWidgetUserName = null;
let chatWidgetIsConnected = false;
// Keep only one active subscription per conversation
let chatWidgetConversationSubscription = null;
let chatWidgetSubscribedConversationId = null;
// Track last outbound message to trigger resync if response misses
let chatWidgetLastSendAt = 0;
// Prevent multiple simultaneous message loads
let chatWidgetLoadingMessages = false;
// Track conversation status to determine if AI should respond
let chatWidgetConversationStatus = 'OPEN'; // OPEN, ASSIGNED, or CLOSED
// Keep a global reference to SSE connection to avoid GC closing it prematurely
let chatWidgetEventSource = null;

/**
 * Get CSRF token from meta tag
 */
function getCsrfToken() {
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    return {
        token: csrfToken ? csrfToken.getAttribute('content') : null,
        header: csrfHeader ? csrfHeader.getAttribute('content') : 'X-XSRF-TOKEN'
    };
}

/**
 * Initialize chat widget on page load
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Chat widget DOMContentLoaded - initializing...');
    // Check if user is logged in
    fetch('/api/auth/me')
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            throw new Error('Not authenticated');
        })
        .then(data => {
            if (data.data) {
                chatWidgetUserId = data.data.id;
                chatWidgetUserName = data.data.firstname + ' ' + data.data.lastname;
                console.log('✅ User authenticated, userId:', chatWidgetUserId);
                
                // Initialize in try-catch to prevent errors from hiding widget
                try {
                    initializeChatWidget();
                } catch (e) {
                    console.error('Error initializing chat widget:', e);
                }
            } else {
                throw new Error('No user data');
            }
        })
        .catch(error => {
            console.log('❌ User not authenticated, hiding chat widget:', error.message);
            // Hide chat widget if not logged in
            const chatWidget = document.getElementById('chatWidget');
            if (chatWidget) {
                chatWidget.style.display = 'none';
            }
        });
});

/**
 * Initialize chat widget connection
 */
function initializeChatWidget() {
    // Connect to WebSocket
    connectChatWidget();
    
    // Load existing conversation or create new one
    loadOrCreateConversation();
}

/**
 * Connect to WebSocket
 */
function connectChatWidget() {
    // Check if SockJS and Stomp are loaded
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        console.log('⏳ WebSocket libraries not loaded yet, retrying in 100ms...');
        setTimeout(connectChatWidget, 100);
        return;
    }
    
    console.log('✅ SockJS and Stomp loaded, connecting...');
    const socket = new SockJS('/ws');
    chatWidgetStompClient = Stomp.over(socket);
    
    // Disable debug logging
    chatWidgetStompClient.debug = null;
    
    chatWidgetStompClient.connect({}, function(frame) {
        chatWidgetIsConnected = true;
        updateChatWidgetStatus('online');
        console.log('🔌 WebSocket connected for chat widget');
        console.log('📋 Current conversationId:', chatWidgetConversationId);
        
        // Subscribe to personal queue for messages (backup channel)
        chatWidgetStompClient.subscribe('/user/queue/chat', function(message) {
            const chatMessage = JSON.parse(message.body);
            console.log('📨 Customer received notification via personal queue:', chatMessage);
            // Note: This is backup/notification channel, main messages come via topic
            displayChatWidgetMessage(chatMessage);
        });
        
        // CRITICAL: Subscribe to personal chat updates for conversation_assigned events
        chatWidgetStompClient.subscribe('/user/queue/chat-updates', function(message) {
            try {
                const update = JSON.parse(message.body);
                console.log('📨 Customer received personal update:', update);
                
                // Handle conversation_assigned event
                if (update && update.type === 'conversation_assigned' && update.data) {
                    if (chatWidgetConversationId && update.data.conversationId == chatWidgetConversationId) {
                        chatWidgetConversationStatus = 'ASSIGNED';
                        console.log('📋 Conversation assigned to staff, status updated to ASSIGNED');
                        
                        // CRITICAL: Ensure we're subscribed to conversation topic
                        if (chatWidgetSubscribedConversationId !== chatWidgetConversationId) {
                            console.log('🔄 Not subscribed yet, subscribing now to conversation:', chatWidgetConversationId);
                            subscribeToChatWidgetConversation(chatWidgetConversationId);
                        }
                    }
                }
            } catch (e) {
                console.error('Error processing personal update:', e);
            }
        });
        
        // PRIMARY: Subscribe to conversation topic when conversation is loaded
        // This is the main channel for receiving all messages in the conversation
        if (chatWidgetConversationId) {
            console.log('🔄 Auto-subscribing to conversation on connect:', chatWidgetConversationId);
            subscribeToChatWidgetConversation(chatWidgetConversationId);
        } else {
            console.log('⚠️ No conversationId yet, will subscribe when conversation loads');
        }
    }, function(error) {
        chatWidgetIsConnected = false;
        updateChatWidgetStatus('offline');
        
        // Retry connection after 5 seconds
        setTimeout(connectChatWidget, 5000);
    });
}

/**
 * Subscribe to conversation topic - PRIMARY message channel
 * All messages (customer, staff, AI) are sent to /topic/chat/{conversationId}
 */
function subscribeToChatWidgetConversation(conversationId) {
    if (chatWidgetStompClient && chatWidgetIsConnected) {
        // Prevent duplicate subscriptions
        if (chatWidgetSubscribedConversationId === conversationId && chatWidgetConversationSubscription) {
            console.log('✅ Already subscribed to conversation:', conversationId);
            return;
        }
        if (chatWidgetConversationSubscription) {
            try { chatWidgetConversationSubscription.unsubscribe(); } catch (e) { /* noop */ }
            chatWidgetConversationSubscription = null;
        }

        // PRIMARY SUBSCRIPTION: This is where ALL messages arrive
        chatWidgetConversationSubscription = chatWidgetStompClient.subscribe('/topic/chat/' + conversationId, function(message) {
            const chatMessage = JSON.parse(message.body);
            console.log('📨 Customer received message via conversation topic:', {
                id: chatMessage.id,
                senderId: chatMessage.senderId,
                senderName: chatMessage.senderName,
                isAiGenerated: chatMessage.isAiGenerated,
                content: chatMessage.content?.substring(0, 50),
                timestamp: new Date().toISOString()
            });
            
            // CRITICAL FIX: If message is from staff (not AI), update status to ASSIGNED
            // This prevents AI typing indicator from showing after staff takes over
            if (chatMessage.senderId !== chatWidgetUserId && !chatMessage.isAiGenerated) {
                chatWidgetConversationStatus = 'ASSIGNED';
                console.log('📋 Staff message detected, status updated to ASSIGNED');
            }
            
            displayChatWidgetMessage(chatMessage);
        });
        
        chatWidgetSubscribedConversationId = conversationId;
        console.log('✅ Subscribed to conversation topic:', '/topic/chat/' + conversationId);
        
        // Sau khi subscribe, đảm bảo đang ở cuối danh sách (trường hợp conversation vừa tạo)
        setTimeout(scrollChatWidgetToBottom, 50);
        // REMOVED: Auto-reload after subscribe causes race condition
        // Staff messages arrive via WebSocket but get cleared when reload happens
        // Messages will be loaded when chat widget opens or when explicitly needed
    }
}

// Subscribe to general chat updates to handle hide_typing broadcast
if (!window.chatWidgetSubscribedChatUpdates) {
    window.chatWidgetSubscribedChatUpdates = true;
    try {
        const ensureConnection = () => {
            if (chatWidgetStompClient && chatWidgetIsConnected) {
                // Subscribe to broadcast updates
                chatWidgetStompClient.subscribe('/topic/chat-updates', function(message) {
                    try {
                        const update = JSON.parse(message.body);
                        if (update && update.type === 'message_sent' && update.data && update.data.message) {
                            const incomingMessage = update.data.message;
                            const incomingConversationId = incomingMessage.conversationId;

                            // CRITICAL: If widget has not yet tracked this conversation, subscribe now
                            // This handles the race condition for first message
                            if (!chatWidgetConversationId && incomingConversationId) {
                                console.log('🆕 First message detected, setting up conversation:', incomingConversationId);
                                chatWidgetConversationId = incomingConversationId;
                                subscribeToChatWidgetConversation(chatWidgetConversationId);
                                
                                // Display the message immediately (don't wait for reload)
                                console.log('📬 Displaying first message immediately:', incomingMessage);
                                displayChatWidgetMessage(incomingMessage);
                                
                                // Then load full history to ensure nothing is missed
                                setTimeout(() => {
                                    if (!chatWidgetLoadingMessages) {
                                        loadChatWidgetMessages(chatWidgetConversationId);
                                    }
                                }, 300);
                                return; // Exit early to avoid duplicate display
                            }

                            // If we already have conversationId, the message will come via /topic/chat/{id}
                            // So we DON'T need to display it here (avoid duplicate)
                            // This channel is only for catching the FIRST message before subscription
                            console.log('⏭️ Skipping message_sent display (will come via topic subscription)');
                        }
                        if (update && update.type === 'hide_typing') {
                            if (!update.data || !chatWidgetConversationId || update.data.conversationId == chatWidgetConversationId) {
                                hideTypingIndicator();
                            }
                        }
                        // Track conversation status changes (e.g., when staff assigns)
                        if (update && update.type === 'conversation_assigned' && update.data) {
                            if (chatWidgetConversationId && update.data.conversationId == chatWidgetConversationId) {
                                chatWidgetConversationStatus = 'ASSIGNED';
                                console.log('📋 Conversation assigned to staff, status updated to ASSIGNED');
                                
                                // CRITICAL: Ensure we're subscribed to conversation topic
                                // This handles the case where AI creates conversation and hands off to staff
                                if (chatWidgetSubscribedConversationId !== chatWidgetConversationId) {
                                    console.log('🔄 Not subscribed yet, subscribing now to conversation:', chatWidgetConversationId);
                                    subscribeToChatWidgetConversation(chatWidgetConversationId);
                                }
                            }
                        }
                    } catch (e) { /* noop */ }
                });
            } else {
                setTimeout(ensureConnection, 500);
            }
        };
        ensureConnection();
    } catch (e) { /* noop */ }
}

/**
 * Load existing conversation or create new one
 */
function loadOrCreateConversation() {
    fetch('/api/chat/conversations/my')
        .then(response => response.json())
        .then(data => {
            if (data.data && data.data.length > 0) {
                // Use the first active conversation (OPEN or ASSIGNED)
                const activeConversation = data.data.find(c => c.status === 'OPEN' || c.status === 'ASSIGNED');
                if (activeConversation) {
                    chatWidgetConversationId = activeConversation.id;
                    chatWidgetConversationStatus = activeConversation.status; // Track status
                    loadChatWidgetMessages(chatWidgetConversationId);
                    subscribeToChatWidgetConversation(chatWidgetConversationId);
                } else {
                    // Create new conversation
                    createNewConversation();
                }
            } else {
                // Create new conversation
                createNewConversation();
            }
        })
        .catch(error => {
            // Silent fail for conversation loading
        });
}

/**
 * Create new conversation
 */
function createNewConversation() {
    // Don't create conversation immediately, wait for first message
}

/**
 * Load messages for conversation
 */
function loadChatWidgetMessages(conversationId) {
    // Prevent multiple simultaneous loads
    if (chatWidgetLoadingMessages) {
        return;
    }
    
    chatWidgetLoadingMessages = true;
    
    // Load with pagination to get ALL messages (increase page size)
    fetch('/api/chat/conversations/' + conversationId + '/messages?page=0&size=100')
        .then(response => response.json())
        .then(data => {
            if (data.data) {
                const messagesContainer = document.getElementById('chatWidgetMessages');
                
                // Sort messages by sentAt ascending (oldest first)
                // Tie-breaker by id ascending to keep stable order when sentAt has second-level precision
                const sortedMessages = data.data.sort((a, b) => {
                    const ta = a.sentAt ? new Date(a.sentAt).getTime() : 0;
                    const tb = b.sentAt ? new Date(b.sentAt).getTime() : 0;
                    if (ta !== tb) return ta - tb;
                    const ia = Number(a.id) || 0;
                    const ib = Number(b.id) || 0;
                    return ia - ib;
                });
                
                // Check if we need to reload or just add new messages
                const existingMessageIds = new Set();
                const existingMessages = messagesContainer.querySelectorAll('[data-message-id]');
                existingMessages.forEach(el => {
                    const id = el.getAttribute('data-message-id');
                    if (id && !id.startsWith('temp-')) {
                        existingMessageIds.add(id);
                    }
                });
                
                // Only clear and reload if we have significantly different messages
                const newMessageIds = new Set(sortedMessages.map(m => m.id.toString()));
                const hasNewMessages = sortedMessages.some(m => !existingMessageIds.has(m.id.toString()));
                
                if (hasNewMessages && sortedMessages.length > existingMessageIds.size) {
                    
                    // Preserve temp user messages before clearing
                    const tempUserMessages = [];
                    const tempElements = messagesContainer.querySelectorAll('[data-message-id^="temp-user-"]');
                    tempElements.forEach(el => {
                        const id = el.getAttribute('data-message-id');
                        const content = el.querySelector('.message-content-holder, .text-sm.leading-relaxed')?.innerHTML;
                        if (id && content) {
                            tempUserMessages.push({ id, content });
                        }
                    });
                    
                    // Clear ALL existing messages
                    messagesContainer.innerHTML = '';
                    
                    // Display messages in order
                    sortedMessages.forEach(message => {
                        // CRITICAL FIX: Validate message has content before displaying
                        if (!message.content) {
                            console.error('❌ Message from API has no content:', message);
                        } else {
                            console.log('📩 Loading message:', {
                                id: message.id,
                                sender: message.senderName,
                                contentLength: message.content?.length,
                                contentPreview: message.content?.substring(0, 50)
                            });
                        }
                        
                        // CRITICAL FIX: Update status if we see staff message
                        if (message.senderId !== chatWidgetUserId && !message.isAiGenerated) {
                            chatWidgetConversationStatus = 'ASSIGNED';
                        }
                        
                        displayChatWidgetMessage(message, true); // true = skip duplicate check for initial load
                    });
                    
                    // Restore temp user messages if they weren't replaced by persisted messages
                    tempUserMessages.forEach(tempMsg => {
                        const persistedExists = sortedMessages.some(m => 
                            m.senderId === chatWidgetUserId && 
                            parseMarkdown(m.content).trim() === tempMsg.content.trim()
                        );
                        if (!persistedExists) {
                            // Recreate temp message element
                            const tempDiv = document.createElement('div');
                            tempDiv.className = 'flex mb-4 justify-end';
                            tempDiv.setAttribute('data-message-id', tempMsg.id);
                            tempDiv.innerHTML = `
                                <div class="flex items-end space-x-2 max-w-xs">
                                    <div class="flex flex-col items-end">
                                        <div class="bg-gradient-to-r from-pink-500 to-purple-600 text-white px-4 py-3 rounded-2xl rounded-br-md shadow-lg">
                                            <div class="text-sm leading-relaxed">${tempMsg.content}</div>
                                        </div>
                                        <span class="text-xs text-gray-500 mt-1 px-2">✓</span>
                                    </div>
                                    <div class="w-8 h-8 rounded-full bg-gradient-to-r from-pink-500 to-purple-600 flex items-center justify-center text-white text-sm font-semibold">
                                        ${chatWidgetUserName ? chatWidgetUserName.charAt(0).toUpperCase() : 'C'}
                                    </div>
                                </div>
                            `;
                            messagesContainer.appendChild(tempDiv);
                        }
                    });
                } else {
                    // No new messages, keeping existing display
                }
                
                // Scroll to bottom after loading
                setTimeout(() => {
                    scrollChatWidgetToBottom();
                }, 100);
            }
        })
        .catch(error => {
            // Silent fail for message loading
        })
        .finally(() => {
            chatWidgetLoadingMessages = false;
        });
}

/**
 * Toggle chat widget visibility
 */
function toggleChatWidget() {
    const chatWindow = document.getElementById('chatWidgetWindow');
    const chatButton = document.getElementById('chatBubbleButton');
    const badge = document.getElementById('chatWidgetBadge');
    
    if (chatWindow.style.display === 'none') {
        chatWindow.style.display = 'flex';
        chatButton.classList.remove('pulsate');
        badge.style.display = 'none';
        badge.textContent = '0';
        
        // Mark messages as read
        if (chatWidgetConversationId) {
            markChatWidgetMessagesAsRead(chatWidgetConversationId);
        }
        
        // Force reload messages when opening chat to ensure we have latest
        if (chatWidgetConversationId && !chatWidgetLoadingMessages) {
            loadChatWidgetMessages(chatWidgetConversationId);
        } else {
            // Scroll to bottom
            scrollChatWidgetToBottom();
        }
        
        // Focus input
        document.getElementById('chatWidgetInput').focus();
    } else {
        chatWindow.style.display = 'none';
    }
}

/**
 * Send message from widget with streaming support
 */
function sendWidgetMessage() {
    const input = document.getElementById('chatWidgetInput');
    const content = input.value.trim();
    
    if (!content) {
        return;
    }
    
    // Clear input immediately for better UX
    input.value = '';
    
    // Create message object
    const message = {
        conversationId: chatWidgetConversationId,
        content: content,
        messageType: 'TEXT'
    };
    
    // Display user message immediately with a more unique ID
    const tempMessageId = 'temp-user-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
    displayChatWidgetMessage({
        id: tempMessageId,
        senderId: chatWidgetUserId,
        content: content,
        sentAt: new Date().toISOString(),
        isRead: false
    });
    
    // Show typing indicator for AI response if conversation not assigned to staff
    if (chatWidgetConversationStatus === 'ASSIGNED') {
        // Do not show AI typing when staff is handling
        // Staff will respond via WebSocket
    } else {
        // Show AI typing indicator for OPEN conversations
        showTypingIndicator();
    }
    
    // Get CSRF token
    const csrf = getCsrfToken();
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }
    
    // Determine which API to use
    const apiEndpoint = chatWidgetConversationId ? '/api/chat/messages' : '/api/chat/messages/first';
    
    // Check if streaming is supported and enabled
    const supportsStreaming = window.EventSource && chatWidgetConversationId;
    
    if (supportsStreaming) {
        // Try streaming approach
        sendMessageWithStreaming(message, headers, apiEndpoint);
    } else {
        // Fallback to regular approach
        sendMessageRegular(message, headers, apiEndpoint);
    }

    // Failsafe: if no new message arrives within 2.5s, reload messages
    chatWidgetLastSendAt = Date.now();
    setTimeout(() => {
        const elapsed = Date.now() - chatWidgetLastSendAt;
        if (elapsed >= 2400 && chatWidgetConversationId && !chatWidgetLoadingMessages) {
            try { loadChatWidgetMessages(chatWidgetConversationId); } catch (e) { /* noop */ }
        }
    }, 2500);
}

/**
 * Send message with streaming response
 */
function sendMessageWithStreaming(message, headers, apiEndpoint) {
    // Send message first
    fetch(apiEndpoint, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(message)
    })
    .then(response => response.json())
    .then(data => {
        if (data.data) {
            // Update conversation ID if needed
            if (!chatWidgetConversationId && data.data.conversationId) {
                chatWidgetConversationId = data.data.conversationId;
                subscribeToChatWidgetConversation(chatWidgetConversationId);
                // Khi bắt đầu stream ngay lần đầu, đảm bảo đã có placeholder trước
                // và đồng thời fallback fetch lại nếu stream không tới
                setTimeout(() => {
                    if (!chatWidgetLoadingMessages) {
                        try { loadChatWidgetMessages(chatWidgetConversationId); } catch (e) { /* noop */ }
                    }
                }, 150);
            }
            
            // Start streaming response
            startStreamingResponse(chatWidgetConversationId);
        } else {
            hideTypingIndicator();
            displayChatWidgetMessage({
                id: 'error-' + Date.now(),
                senderId: 'system',
                content: 'Không thể gửi tin nhắn. Vui lòng thử lại.',
                sentAt: new Date().toISOString(),
                senderName: 'System'
            });
        }
    })
    .catch(error => {
        hideTypingIndicator();
        // Fallback to regular approach
        sendMessageRegular(message, headers, apiEndpoint);
    });
}

/**
 * Send message with regular (non-streaming) response
 */
function sendMessageRegular(message, headers, apiEndpoint) {
    fetch(apiEndpoint, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(message)
    })
    .then(response => response.json())
    .then(data => {
        hideTypingIndicator();
        
        if (data.data) {
            // Update conversation ID if needed
            if (!chatWidgetConversationId && data.data.conversationId) {
                chatWidgetConversationId = data.data.conversationId;
                subscribeToChatWidgetConversation(chatWidgetConversationId);
                // Safety: ngay sau khi có conversation đầu tiên, tải lại toàn bộ tin nhắn
                // để tránh miss phản hồi AI do subscribe trễ trong lần đầu
                setTimeout(() => {
                    if (!chatWidgetLoadingMessages) {
                        try { loadChatWidgetMessages(chatWidgetConversationId); } catch (e) { /* noop */ }
                    }
                }, 150);
            }
            
            // AI response will come via WebSocket
        } else {
            displayChatWidgetMessage({
                id: 'error-' + Date.now(),
                senderId: 'system',
                content: 'Không thể gửi tin nhắn. Vui lòng thử lại.',
                sentAt: new Date().toISOString(),
                senderName: 'System'
            });
        }
    })
    .catch(error => {
        hideTypingIndicator();
        displayChatWidgetMessage({
            id: 'error-' + Date.now(),
            senderId: 'system',
            content: 'Có lỗi xảy ra. Vui lòng thử lại.',
            sentAt: new Date().toISOString(),
            senderName: 'System'
        });
    });
}

/**
 * Start streaming response using EventSource
 */
function startStreamingResponse(conversationId) {
    // Close previous stream if any
    try { if (chatWidgetEventSource) { chatWidgetEventSource.close(); } } catch (e) { /* noop */ }
    const eventSource = new EventSource(`/api/chat/stream/${conversationId}`);
    chatWidgetEventSource = eventSource;
    let streamingMessageId = 'streaming-' + Date.now();
    let accumulatedContent = '';
    
    // Create placeholder message for streaming content
    const placeholderMessage = {
        id: streamingMessageId,
        senderId: 'ai',
        content: '',
        sentAt: new Date().toISOString(),
        senderName: 'Hoa AI'
    };
    
    eventSource.onopen = function() {
        // connection established
    };

    eventSource.onmessage = function(event) {
        try {
            const data = JSON.parse(event.data);
            
            if (data.type === 'chunk') {
                // Hide typing indicator on first chunk
                if (accumulatedContent === '') {
                    hideTypingIndicator();
                    displayChatWidgetMessage(placeholderMessage);
                }
                
                // Accumulate content
                accumulatedContent += data.content;
                
                // Update the streaming message
                updateStreamingMessage(streamingMessageId, accumulatedContent);
                
            } else if (data.type === 'complete') {
                // Streaming complete
                eventSource.close();
                chatWidgetEventSource = null;
                hideTypingIndicator();
                
                // Replace with final message
                if (data.finalMessage) {
                    replaceStreamingMessage(streamingMessageId, data.finalMessage);
                }
                
            } else if (data.type === 'error') {
                // Streaming error
                eventSource.close();
                chatWidgetEventSource = null;
                hideTypingIndicator();
                // Streaming error
                
                // Show error message
                if (accumulatedContent === '') {
                    displayChatWidgetMessage({
                        id: 'error-' + Date.now(),
                        senderId: 'system',
                        content: 'Có lỗi xảy ra khi tạo phản hồi. Vui lòng thử lại.',
                        sentAt: new Date().toISOString(),
                        senderName: 'System'
                    });
                }
            }
        } catch (error) {
            // Error parsing streaming data
        }
    };
    
    eventSource.onerror = function(error) {
        eventSource.close();
        chatWidgetEventSource = null;
        hideTypingIndicator();
        
        // Fallback: the response will come via WebSocket
        setTimeout(() => {
            if (accumulatedContent === '') {
                // Streaming failed, waiting for WebSocket response
                // Extra safety: reload messages to avoid missed AI reply
                if (!chatWidgetLoadingMessages) {
                    try { loadChatWidgetMessages(conversationId); } catch (e) { /* noop */ }
                }
            }
        }, 1000);
    };
    
    // Close EventSource after 30 seconds (timeout)
    setTimeout(() => {
        if (eventSource.readyState !== EventSource.CLOSED) {
            eventSource.close();
            chatWidgetEventSource = null;
            hideTypingIndicator();
        }
    }, 30000);
}

/**
 * Display message in chat widget
 */
function displayChatWidgetMessage(message, skipDuplicateCheck = false) {
    const messagesContainer = document.getElementById('chatWidgetMessages');
    if (!messagesContainer) {
        return false;
    }
    const isOwn = message.senderId === chatWidgetUserId;
    // When replacing a temp message, we want to keep its position
    let insertBeforeEl = null;
    
    console.log('🎨 displayChatWidgetMessage called:', {
        id: message.id,
        senderId: message.senderId,
        isOwn: isOwn,
        content: message.content?.substring(0, 50),
        skipDuplicateCheck: skipDuplicateCheck
    });
    
    // Enhanced duplicate check to prevent duplicate messages
    if (!skipDuplicateCheck) {
        // Check by message ID first (most reliable)
        const existingMessage = messagesContainer.querySelector(`[data-message-id="${message.id}"]`);
        if (existingMessage) {
            console.log('⚠️ Message already exists, skipping:', message.id);
            return false;
        }
        
        // Only check for temp message replacement (not general content duplicates)
        if (isOwn && message.id && !String(message.id).startsWith('temp')) {
            // Find and remove temp message with same content
            const tempDup = Array.from(messagesContainer.querySelectorAll('[data-message-id^="temp-"]'))
                .reverse()
                .find(el => {
                    const div = el.querySelector('.message-content-holder, .text-sm.leading-relaxed');
                    return div && div.innerHTML.trim() === parseMarkdown(message.content).trim();
                });
            if (tempDup) {
                console.log('🔄 Replacing temp message with persisted message');
                // Preserve position: remember the next sibling, then remove temp
                try { insertBeforeEl = tempDup.nextSibling; tempDup.remove(); } catch (e) { /* noop */ }
            }
        }
        
        // Special handling: if this is a temp user message, make sure it's not replaced by accident
        if (isOwn && String(message.id).startsWith('temp-user-')) {
            // This is a temp user message, ensure it stays visible
        }
        
        // REMOVED: Content-based duplicate check was too strict and blocked legitimate staff messages
        // Staff can send multiple messages with similar content, and they should all be displayed
        // We only check duplicate by message ID (above), which is the correct approach
    }
    
    console.log('✅ Displaying message:', message.id);
    console.log('📍 Message details:', {
        id: message.id,
        senderId: message.senderId,
        senderName: message.senderName,
        isOwn: isOwn,
        isAiGenerated: message.isAiGenerated,
        content: message.content?.substring(0, 30)
    });
    
    // If AI/system message arrives, ensure typing indicator is hidden
    if (!isOwn) {
        try { hideTypingIndicator(); } catch (e) { /* noop */ }
    }

    // Create message element with MODERN BEAUTIFUL design
    const messageDiv = document.createElement('div');
    messageDiv.className = 'flex mb-4 animate-fade-in ' + (isOwn ? 'justify-end' : 'justify-start');
    messageDiv.setAttribute('data-message-id', message.id);
    
    if (isOwn) {
        // Customer message (right side)
        messageDiv.innerHTML = `
            <div class="flex items-end space-x-2 max-w-xs">
                <div class="flex flex-col items-end">
                    <div class="bg-gradient-to-r from-pink-500 to-purple-600 text-white px-4 py-3 rounded-2xl rounded-br-md shadow-lg">
                        <div class="text-sm leading-relaxed">${parseMarkdown(message.content)}</div>
                    </div>
                    <span class="text-xs text-gray-500 mt-1 px-2">
                        ${formatMessageTime(message.sentAt)}
                        ${message.isRead ? '✓✓' : '✓'}
                    </span>
                </div>
                <div class="w-8 h-8 rounded-full bg-gradient-to-r from-pink-500 to-purple-600 flex items-center justify-center text-white text-sm font-semibold">
                    ${chatWidgetUserName ? chatWidgetUserName.charAt(0).toUpperCase() : 'C'}
                </div>
            </div>
        `;
    } else {
        // Staff/AI message (left side) - parse markdown for images and links
        const senderName = message.senderName || 'StarShop Support';
        messageDiv.innerHTML = `
            <div class="flex items-start space-x-2 max-w-lg">
                <div class="w-8 h-8 rounded-full bg-gradient-to-r from-blue-500 to-cyan-500 flex items-center justify-center text-white text-sm font-semibold flex-shrink-0">
                    🌸
                </div>
                <div class="flex flex-col items-start flex-1">
                    <div class="bg-white text-gray-800 px-4 py-3 rounded-2xl rounded-bl-md shadow-lg border border-gray-100 w-full">
                        <div class="message-content-holder text-sm leading-relaxed break-words"></div>
                    </div>
                    <span class="text-xs text-gray-500 mt-1 px-2">${senderName}</span>
                </div>
            </div>
        `;
    }
    
    // Insert message with correct position rules
    if (insertBeforeEl) {
        // Keep the exact position where the temp message was
        messagesContainer.insertBefore(messageDiv, insertBeforeEl);
    } else {
        // Otherwise, insert before typing indicator if exists, else append
        const typingIndicator = messagesContainer.querySelector('.typing-indicator');
        if (typingIndicator) {
            messagesContainer.insertBefore(messageDiv, typingIndicator);
        } else {
            messagesContainer.appendChild(messageDiv);
        }
    }
    
    // Set innerHTML AFTER appending to DOM to ensure proper rendering
    if (!isOwn) {
        const contentHolder = messageDiv.querySelector('.message-content-holder');
        if (contentHolder) {
            // CRITICAL FIX: Check if content exists before parsing
            if (!message.content) {
                console.error('❌ Message content is null/undefined:', message);
                contentHolder.innerHTML = '<span class="text-red-500 italic">Lỗi: Tin nhắn trống</span>';
            } else {
                console.log('✅ Setting AI message content:', message.content.substring(0, 100));
                contentHolder.innerHTML = parseMarkdown(message.content);
                
                // Update wishlist icons after rendering product cards
                setTimeout(() => updateAllWishlistIcons(), 100);
            }
        } else {
            console.error('❌ Content holder not found for message:', message.id);
        }
    }
    
    // Scroll to bottom
    scrollChatWidgetToBottom();
    
    // If chat window is closed and message is not from current user, show notification
    const chatWindow = document.getElementById('chatWidgetWindow');
    if (!isOwn && chatWindow.style.display === 'none') {
        showChatWidgetNotification();
    }

    return true;
}

/**
 * Format message time
 */
function formatMessageTime(dateTime) {
    if (!dateTime) return '';
    const date = new Date(dateTime);
    return date.toLocaleTimeString('vi-VN', {hour: '2-digit', minute: '2-digit'});
}

/**
 * Parse basic markdown to HTML
 * Supports: images, links, bold text, lists, line breaks
 * OPTIMIZED: Handles escaped newlines and ensures proper formatting
 */
function parseMarkdown(text) {
    if (!text) return '';
    
    let html = text;
    
    // Fix escaped newlines from JSON (\\n -> actual newline)
    html = html.replace(/\\n/g, '\n');
    
    // Parse images with product metadata: ![alt](url)<!--product:id,price-->
    html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)<!--product:(\d+),([^-]+)-->/g, function(match, alt, url, productId, price) {
        const safeAlt = alt.replace(/"/g, '&quot;');
        const formattedPrice = formatPrice(price);
        
        // Build HTML without template literal to avoid escaping issues
        return '<div class="product-card-chat my-3 bg-white rounded-xl shadow-md overflow-hidden border border-gray-200 hover:shadow-lg transition-shadow">' +
            '<img src="' + url + '" alt="' + safeAlt + '" class="w-full h-40 object-cover cursor-pointer" onclick="window.open(\'' + url + '\', \'_blank\')" />' +
            '<div class="p-3">' +
            '<h4 class="font-semibold text-gray-900 text-sm mb-1">' + safeAlt + '</h4>' +
            '<p class="text-pink-600 font-bold text-base mb-3">' + formattedPrice + 'đ</p>' +
            '<div class="flex gap-2">' +
            '<button onclick="addToCartFromChat(' + productId + ')" class="flex-1 bg-gradient-to-r from-pink-500 to-purple-600 text-white text-xs py-2 px-3 rounded-lg hover:shadow-md transition-all flex items-center justify-center gap-1">' +
            '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>' +
            'Thêm vào giỏ' +
            '</button>' +
            '<button onclick="addToWishlistFromChat(' + productId + ')" data-product-id="' + productId + '" class="wishlist-btn bg-gray-100 hover:bg-pink-50 text-gray-400 text-xs py-2 px-3 rounded-lg transition-all flex items-center justify-center">' +
            '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>' +
            '</button>' +
            '</div>' +
            '</div>' +
            '</div>';
    });
    
    // Parse regular images without metadata: ![alt](url)
    html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, function(match, alt, url) {
        const safeAlt = alt.replace(/"/g, '&quot;');
        return `<img src="${url}" alt="${safeAlt}" class="max-w-full h-auto rounded-lg my-2 cursor-pointer message-image-hover" style="max-width: 200px;" onclick="window.open('${url}', '_blank')" />`;
    });
    
    // Parse links: [text](url)
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, function(match, text, url) {
        return `<a href="${url}" target="_blank" class="text-blue-600 hover:underline">${text}</a>`;
    });
    
    // Parse bold: **text**
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    
    // Parse bullet lists: * item or - item
    html = html.replace(/^[\*\-]\s+(.+)$/gm, '<li class="ml-4 mb-1">• $1</li>');
    
    // Wrap consecutive list items in ul
    html = html.replace(/(<li[^>]*>.*?<\/li>\s*)+/g, function(match) {
        return '<ul class="my-2 space-y-1">' + match + '</ul>';
    });
    
    // Parse numbered list items: 1️⃣, 2️⃣, etc.
    html = html.replace(/(\d)️⃣/g, '<span class="font-bold text-pink-600">$1️⃣</span>');
    
    // Parse line breaks (but not inside lists)
    html = html.replace(/\n(?![<ul>|<li>])/g, '<br>');
    
    // Clean up extra br tags around lists and images
    html = html.replace(/<br>\s*<ul>/g, '<ul>');
    html = html.replace(/<\/ul>\s*<br>/g, '</ul>');
    html = html.replace(/<br>\s*<img/g, '<img');
    html = html.replace(/\/>\s*<br>/g, '/>');
    
    return html;
}

/**
 * Show notification badge
 */
function showChatWidgetNotification() {
    const badge = document.getElementById('chatWidgetBadge');
    const button = document.getElementById('chatBubbleButton');
    
    const currentCount = parseInt(badge.textContent) || 0;
    badge.textContent = currentCount + 1;
    badge.style.display = 'flex';
    button.classList.add('pulsate');
}

/**
 * Mark messages as read
 */
function markChatWidgetMessagesAsRead(conversationId) {
    const csrf = getCsrfToken();
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }
    
    fetch('/api/chat/conversations/' + conversationId + '/read', {
        method: 'PUT',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        // Messages marked as read
    })
    .catch(error => {
        // Error marking messages as read
    });
}

/**
 * Scroll chat to bottom
 */
function scrollChatWidgetToBottom() {
    const messagesContainer = document.getElementById('chatWidgetMessages');
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

/**
 * Update connection status
 */
function updateChatWidgetStatus(status) {
    const statusIndicator = document.getElementById('chatWidgetStatus');
    if (statusIndicator) {
        if (status === 'online') {
            statusIndicator.className = 'inline-block w-2 h-2 rounded-full bg-green-400 mr-1';
        } else {
            statusIndicator.className = 'inline-block w-2 h-2 rounded-full bg-gray-400 mr-1';
        }
    }
}

/**
 * Show typing indicator
 */
function showTypingIndicator() {
    const messagesContainer = document.getElementById('chatWidgetMessages');
    
    // Remove existing typing indicator
    const existingIndicator = messagesContainer.querySelector('.typing-indicator');
    if (existingIndicator) {
        existingIndicator.remove();
    }
    
    // Create typing indicator - ALWAYS append to the END of messages
    const typingDiv = document.createElement('div');
    typingDiv.className = 'flex justify-start mb-4 typing-indicator';
    typingDiv.setAttribute('role', 'status');
    typingDiv.setAttribute('aria-live', 'polite');
    typingDiv.innerHTML = `
        <div class="flex items-start gap-3 max-w-md">
            <div class="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500 to-cyan-500 flex items-center justify-center text-white text-sm font-semibold shadow-md flex-shrink-0">
                🌸
            </div>
            <div class="flex flex-col items-start">
                <div class="px-4 py-2 rounded-2xl rounded-bl-md shadow-lg border border-gray-100 bg-white/90 backdrop-blur-sm">
                    <div class="flex items-center gap-1">
                        <span class="typing-dot"></span>
                        <span class="typing-dot"></span>
                        <span class="typing-dot"></span>
                    </div>
                </div>
                <span class="text-[11px] text-gray-500 mt-1 px-1 leading-none typing-status">Hoa AI đang trả lời…</span>
            </div>
        </div>
    `;
    
    // ALWAYS append to the very end, after all existing messages
    messagesContainer.appendChild(typingDiv);
    
    // Force scroll to bottom to show typing indicator
    setTimeout(() => {
        scrollChatWidgetToBottom();
    }, 50);
}

/**
 * Hide typing indicator
 */
function hideTypingIndicator() {
    const messagesContainer = document.getElementById('chatWidgetMessages');
    const typingIndicator = messagesContainer.querySelector('.typing-indicator');
    if (typingIndicator) {
        typingIndicator.remove();
    }
}

/**
 * Update streaming message content
 */
function updateStreamingMessage(messageId, content) {
    const messagesContainer = document.getElementById('chatWidgetMessages');
    const messageElement = messagesContainer.querySelector(`[data-message-id="${messageId}"]`);
    
    if (messageElement) {
        const contentDiv = messageElement.querySelector('.message-content-holder, .text-sm.leading-relaxed');
        if (contentDiv) {
            contentDiv.innerHTML = parseMarkdown(content);
            scrollChatWidgetToBottom();
        }
    }
}

/**
 * Replace streaming message with final message
 */
function replaceStreamingMessage(streamingMessageId, finalMessage) {
    const messagesContainer = document.getElementById('chatWidgetMessages');
    const streamingElement = messagesContainer.querySelector(`[data-message-id="${streamingMessageId}"]`);
    
    if (streamingElement) {
        // Remove the streaming message
        streamingElement.remove();
        
        // Display the final message
        displayChatWidgetMessage(finalMessage);
    }
}

/**
 * Format price with thousands separator
 */
function formatPrice(price) {
    if (!price) return '0';
    // Remove any existing formatting
    const numPrice = parseFloat(price.toString().replace(/,/g, ''));
    return numPrice.toLocaleString('vi-VN');
}

/**
 * Add product to cart from chat widget
 */
function addToCartFromChat(productId) {
    const csrf = getCsrfToken();
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }
    
    // Add to cart with quantity 1
    fetch('/api/cart/add', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            productId: productId,
            quantity: 1
        })
    })
    .then(response => {
        // Check HTTP status first
        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.error || data.message || 'Không thể thêm vào giỏ hàng');
            });
        }
        return response.json();
    })
    .then(data => {
        // Response OK - show success
        if (typeof showToast === 'function') {
            showToast('Đã thêm vào giỏ hàng!', 'success');
        }
        
        // Update cart badge if exists
        updateCartBadge();
    })
    .catch(error => {
        console.error('Error adding to cart:', error);
        if (typeof showToast === 'function') {
            showToast(error.message || 'Có lỗi xảy ra', 'error');
        }
    });
}

/**
 * Add product to wishlist from chat widget
 */
function addToWishlistFromChat(productId) {
    const csrf = getCsrfToken();
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }
    
    fetch('/api/wishlist/add', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify({
            productId: productId
        })
    })
    .then(response => {
        // Check HTTP status first
        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.error || data.message || 'Không thể thêm vào yêu thích');
            });
        }
        return response.json();
    })
    .then(data => {
        // Response OK - show success
        if (typeof showToast === 'function') {
            showToast('Đã thêm vào danh sách yêu thích!', 'success');
        }
        
        // Update wishlist count if function exists
        if (typeof window.updateWishlistCount === 'function') {
            // Fetch current wishlist count
            fetch('/api/wishlist/list', { credentials: 'same-origin' })
                .then(response => response.json())
                .then(countData => {
                    if (countData && countData.success && countData.data) {
                        const count = Array.isArray(countData.data) ? countData.data.length : 0;
                        window.updateWishlistCount(count);
                    }
                })
                .catch(error => console.error('Error updating wishlist count:', error));
        }
        
        // Update heart icon color for this product
        updateProductWishlistIcon(productId, true);
    })
    .catch(error => {
        console.error('Error adding to wishlist:', error);
        if (typeof showToast === 'function') {
            showToast(error.message || 'Có lỗi xảy ra', 'error');
        }
    });
}

/**
 * Update product wishlist icon color in chat
 */
function updateProductWishlistIcon(productId, isInWishlist) {
    // Find all wishlist buttons for this product in chat messages
    const wishlistButtons = document.querySelectorAll(`button.wishlist-btn[data-product-id="${productId}"]`);
    
    wishlistButtons.forEach(button => {
        const svg = button.querySelector('svg');
        if (svg) {
            if (isInWishlist) {
                // Fill with pink color
                svg.setAttribute('fill', 'currentColor');
                button.classList.add('text-pink-600');
                button.classList.remove('text-gray-400');
            } else {
                // Outline only
                svg.setAttribute('fill', 'none');
                button.classList.add('text-gray-400');
                button.classList.remove('text-pink-600');
            }
        }
    });
}

/**
 * Check and update all wishlist icons in chat
 */
function updateAllWishlistIcons() {
    // Get all wishlist buttons in chat
    const wishlistButtons = document.querySelectorAll('button.wishlist-btn[data-product-id]');
    if (wishlistButtons.length === 0) return;
    
    // Fetch user's wishlist
    fetch('/api/wishlist/list', { credentials: 'same-origin' })
        .then(response => response.json())
        .then(data => {
            if (data && data.success && data.data) {
                // Extract product IDs - handle different response structures
                const wishlistProductIds = data.data.map(item => {
                    // Try different possible structures
                    if (item.product && item.product.id) {
                        return item.product.id;
                    } else if (item.productId) {
                        return item.productId;
                    } else if (item.id) {
                        return item.id;
                    }
                    return null;
                }).filter(id => id !== null);
                
                console.log('Wishlist product IDs:', wishlistProductIds);
                
                // Update each button
                wishlistButtons.forEach(button => {
                    const productId = parseInt(button.getAttribute('data-product-id'));
                    const isInWishlist = wishlistProductIds.includes(productId);
                    updateProductWishlistIcon(productId, isInWishlist);
                });
            }
        })
        .catch(error => console.error('Error fetching wishlist:', error));
}

/**
 * Update cart badge count
 */
function updateCartBadge() {
    // Try to update cart badge in header if exists
    fetch('/api/cart/count')
        .then(response => response.json())
        .then(data => {
            const count = data.data !== undefined ? data.data : 0;
            
            // Update using global function if available
            if (typeof window.updateCartCount === 'function') {
                window.updateCartCount(count);
            } else {
                // Fallback: update badge directly
                const cartBadge = document.querySelector('.cart-badge, #cartBadge');
                if (cartBadge) {
                    cartBadge.textContent = count;
                    cartBadge.style.display = count > 0 ? 'flex' : 'none';
                }
            }
        })
        .catch(error => {
            console.error('Error updating cart badge:', error);
        });
}
