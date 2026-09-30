// Staff Dashboard JavaScript
// Handles check-in/check-out, real-time updates, and WebSocket connections

let stompClient = null;
let currentShift = null;

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    initializeCheckInOutButton();
    connectWebSocket();
    startAutoRefresh();
    
    // If on dashboard page, refresh status immediately
    if (window.location.pathname.includes('/staff/dashboard')) {
        setTimeout(() => {
            if (window.updateDashboardStatus) {
                window.updateDashboardStatus();
            }
        }, 500);
    }
});

/**
 * Initialize check-in/check-out button
 */
async function initializeCheckInOutButton() {
    try {
        const response = await fetch('/api/staff/shift/current', {
            credentials: 'same-origin'
        });
        const data = await response.json();
        
        if (response.ok && data.data) {
            currentShift = data.data;
            renderCheckInOutButton();
        } else {
            renderCheckInOutButton();
        }
    } catch (error) {
        renderCheckInOutButton();
    }
}

/**
 * Render check-in/check-out button based on current state
 */
function renderCheckInOutButton() {
    const container = document.getElementById('checkInOutButton');
    if (!container) return;
    
    // Check if currently checked in (has check-in but no check-out)
    // Note: Lombok @Data with boolean fields starting with 'is' will serialize without the 'is' prefix
    const isCheckedIn = currentShift && (currentShift.checkedIn === true || currentShift.isCheckedIn === true);
    const isCheckedOut = currentShift && (currentShift.checkedOut === true || currentShift.isCheckedOut === true);
    
    if (isCheckedIn && !isCheckedOut) {
        // Currently checked in - show check-out button
        container.innerHTML = `
            <button onclick="performCheckOut()" 
                    class="check-out-button text-white px-4 py-2 rounded-lg font-medium flex items-center space-x-2 transition-all">
                <i class="fas fa-sign-out-alt"></i>
                <span>Check-out</span>
            </button>
        `;
    } else {
        // Not checked in - show check-in button
        container.innerHTML = `
            <button onclick="performCheckIn()" 
                    class="check-in-button text-white px-4 py-2 rounded-lg font-medium flex items-center space-x-2 transition-all">
                <i class="fas fa-sign-in-alt"></i>
                <span>Check-in</span>
            </button>
        `;
    }
}

/**
 * Perform check-in
 */
async function performCheckIn() {
    try {
        const button = event.target.closest('button');
        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xử lý...';
        
        const response = await fetch('/api/staff/check-in', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin'
        });
        
        const data = await response.json();
        
        if (response.ok && data.data) {
            currentShift = data.data;
            renderCheckInOutButton();
            
            // Show success with check-in time
            const checkInTime = new Date(currentShift.checkIn).toLocaleTimeString('vi-VN');
            showToast(`Check-in thành công lúc ${checkInTime}!`, 'success');
            
            // Update dashboard status immediately without reload
            if (window.location.pathname.includes('/staff/dashboard')) {
                // Small delay to ensure DOM is ready
                setTimeout(() => {
                    updateDashboardStatus();
                }, 100);
            }
        } else {
            // Handle error response
            const errorMessage = data.message || 'Check-in thất bại';
            showToast(errorMessage, 'error');
            button.disabled = false;
            renderCheckInOutButton();
        }
    } catch (error) {
        showToast('Đã xảy ra lỗi khi check-in', 'error');
        const button = event.target.closest('button');
        if (button) {
            button.disabled = false;
        }
        renderCheckInOutButton();
    }
}

/**
 * Perform check-out
 */
async function performCheckOut() {
    // THAY THẾ confirm bằng SweetAlert2
    Swal.fire({
        title: 'Xác nhận Check-out?',
        text: 'Bạn sẽ kết thúc ca làm việc hôm nay.',
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Hủy'
    }).then(async (result) => {
        if(result.isConfirmed){
            try {
                const button = event.target.closest('button');
                button.disabled = true;
                button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xử lý...';
                
                const response = await fetch('/api/staff/check-out', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    credentials: 'same-origin'
                });
                
                const data = await response.json();
                
                if (response.ok && data.data) {
                    currentShift = data.data;
                    
                    // After check-out, button should show "Check-in" for next day
                    // But prevent check-in again today
                    renderCheckInOutButton();
                    
                    // Show success with hours worked
                    const hours = data.data.hoursWorked || 0;
                    const checkOutTime = new Date(data.data.checkOut).toLocaleTimeString('vi-VN');
                    showToast(
                        `Check-out thành công lúc ${checkOutTime}! Bạn đã làm việc ${hours} giờ hôm nay.`, 
                        'success'
                    );
                    
                    // Update dashboard status immediately without reload
                    if (window.location.pathname.includes('/staff/dashboard')) {
                        // Small delay to ensure DOM is ready
                        setTimeout(() => {
                            updateDashboardStatus();
                        }, 100);
                    }
                } else {
                    showToast(data.message || 'Check-out thất bại', 'error');
                    button.disabled = false;
                    renderCheckInOutButton();
                }
            } catch (error) {
                showToast('Đã xảy ra lỗi khi check-out', 'error');
                const button = event.target.closest('button');
                if (button) {
                    button.disabled = false;
                }
                renderCheckInOutButton();
            }
        }
    });
}

/**
 * Connect to WebSocket for real-time updates
 */
function connectWebSocket() {
    if (!STAFF_ID) {
        return;
    }
    
    try {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        
        // Disable debug logging in production
        stompClient.debug = null;
        
        stompClient.connect({}, function(frame) {
            
            // Make stompClient globally available AFTER connected
            window.stompClient = stompClient;
            
            // Subscribe to personal notifications
            stompClient.subscribe('/topic/messages/' + STAFF_ID, function(message) {
                const notification = JSON.parse(message.body);
                handleWebSocketNotification(notification);
            });
            
            // Note: /topic/staff/notifications is now handled by staff-notifications.js Alpine component
            
            // Subscribe to new conversations
            stompClient.subscribe('/topic/staff/conversations', function(message) {
                const data = JSON.parse(message.body);
                handleNewConversation(data);
            });
        }, function(error) {
            // Attempt to reconnect after 5 seconds
            setTimeout(connectWebSocket, 5000);
        });
    } catch (error) {
        // Error setting up WebSocket
    }
}

/**
 * Handle WebSocket notifications
 */
function handleWebSocketNotification(notification) {
    
    // Show notification dot
    const notificationDot = document.getElementById('notificationDot');
    if (notificationDot) {
        notificationDot.style.display = 'block';
    }
    
    // Update unread badge if it's a new message
    if (notification.type === 'new_message') {
        updateUnreadBadge();
    }
    
    // Show browser notification if supported
    if (Notification.permission === 'granted') {
        new Notification('StarShop Staff', {
            body: notification.message,
            icon: '/images/logo.png',
            badge: '/images/badge.png'
        });
    }
    
    // Play sound notification
    playNotificationSound();
}

/**
 * Handle new conversation notification
 */
function handleNewConversation(data) {
    
    // Show notification
    showToast('Có cuộc hội thoại mới đang chờ xử lý!', 'info');
    
    // Refresh unassigned queue if on dashboard
    if (window.location.pathname.includes('/staff/dashboard')) {
        setTimeout(() => location.reload(), 2000);
    }
}

/**
 * Update unread message badge
 */
async function updateUnreadBadge() {
    try {
        const response = await fetch('/api/staff/dashboard', {
            credentials: 'same-origin'
        });
        const data = await response.json();
        
        if (data.success && data.data) {
            const badge = document.getElementById('unreadBadge');
            if (badge) {
                const unreadCount = data.data.unreadMessages || 0;
                if (unreadCount > 0) {
                    badge.textContent = unreadCount;
                    badge.style.display = 'inline-flex';
                    badge.classList.add('pulse-badge');
                } else {
                    badge.style.display = 'none';
                }
            }
        }
    } catch (error) {
        // Error updating unread badge
    }
}

/**
 * Update dashboard status after check-in/check-out
 */
async function updateDashboardStatus() {
    try {
        // Get fresh dashboard data
        const response = await fetch('/api/staff/dashboard', {
            credentials: 'same-origin'
        });
        const data = await response.json();
        
        if (data.success && data.data) {
            // Update shift status display directly
            const shiftStatusElement = document.querySelector('[x-text="shiftStatus"]');
            if (shiftStatusElement) {
                shiftStatusElement.textContent = data.data.shiftStatus || 'Chưa check-in';
            }
            
            // Update hours worked
            const todayHoursElement = document.querySelector('[x-text="stats.todayHours"]');
            if (todayHoursElement) {
                todayHoursElement.textContent = data.data.todayHoursWorked || 0;
            }
            
            const weekHoursElement = document.querySelector('[x-text="stats.weekHours"]');
            if (weekHoursElement) {
                weekHoursElement.textContent = data.data.weekHoursWorked || 0;
            }
            
            // Update Alpine.js component data if available
            const dashboardComponent = document.querySelector('[x-data*="staffDashboard"]');
            if (dashboardComponent && dashboardComponent._x_dataStack && dashboardComponent._x_dataStack[0]) {
                const alpineData = dashboardComponent._x_dataStack[0];
                if (alpineData.shiftStatus !== undefined) {
                    alpineData.shiftStatus = data.data.shiftStatus || 'Chưa check-in';
                }
                if (alpineData.stats) {
                    alpineData.stats.todayHours = data.data.todayHoursWorked || 0;
                    alpineData.stats.weekHours = data.data.weekHoursWorked || 0;
                }
            }
        }
    } catch (error) {
        console.error('Error updating dashboard status:', error);
    }
}

/**
 * Start auto-refresh for dashboard stats
 */
function startAutoRefresh() {
    // Refresh stats every 30 seconds
    setInterval(async () => {
        if (window.location.pathname.includes('/staff/dashboard')) {
            updateUnreadBadge();
        }
    }, 30000);
}

// Bỏ HÀM showNotification() riêng vì đã có hàm showToast toàn cục trong main.js

/**
 * Play notification sound
 */
function playNotificationSound() {
    // Create and play a simple beep sound
    try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        
        oscillator.frequency.value = 800;
        oscillator.type = 'sine';
        
        gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);
        
        oscillator.start(audioContext.currentTime);
        oscillator.stop(audioContext.currentTime + 0.5);
    } catch (error) {
        // Could not play notification sound
    }
}

/**
 * Request notification permission
 */
function requestNotificationPermission() {
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }
}

// Request notification permission on load
requestNotificationPermission();

// Make updateDashboardStatus globally available for external calls
window.updateDashboardStatus = updateDashboardStatus;

