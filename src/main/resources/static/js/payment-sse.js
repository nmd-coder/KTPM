/**
 * Payment SSE (Server-Sent Events) for real-time payment status updates
 */

class PaymentSSE {
    constructor() {
        this.eventSource = null;
        this.orderId = null;
        this.isConnected = false;
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    /**
     * Subscribe to payment updates for an order
     */
    subscribe(orderId) {
        this.orderId = orderId;
        
        // Close existing connection
        this.disconnect();
        
        try {
            // Create EventSource connection
            this.eventSource = new EventSource(`/sse/orders/${orderId}`);
            
            // Handle connection opened
            this.eventSource.onopen = (event) => {
                this.isConnected = true;
                this.retryCount = 0;
                this.showStatus('Đang chờ xác nhận thanh toán...', 'info');
            };
            
            // Handle connection events
            this.eventSource.addEventListener('connected', (event) => {
                const data = JSON.parse(event.data);
                this.showStatus(`Đã kết nối. Đang theo dõi đơn hàng ${data.orderId}...`, 'info');
            });
            
            // Handle payment events
            this.eventSource.addEventListener('payment', (event) => {
                const data = JSON.parse(event.data);
                this.handlePaymentUpdate(data);
            });
            
            // Handle errors
            this.eventSource.onerror = (event) => {
                this.isConnected = false;
                
                if (this.retryCount < this.maxRetries) {
                    this.retryCount++;
                    setTimeout(() => this.subscribe(orderId), 2000 * this.retryCount);
                } else {
                    this.showStatus('Không thể kết nối real-time. Vui lòng refresh trang.', 'warning');
                }
            };
            
        } catch (error) {
            this.showStatus('Lỗi kết nối real-time. Vui lòng refresh trang.', 'error');
        }
    }
    
    /**
     * Handle payment status update
     */
    handlePaymentUpdate(data) {
        
        const { status, message, transactionId, orderId } = data;
        
        if (status === 'SUCCESS') {
            this.showPaymentSuccess(message, transactionId, orderId);
        } else if (status === 'FAILED') {
            this.showPaymentFailed(message, orderId);
        }
        
        // Disconnect after receiving payment result
        setTimeout(() => this.disconnect(), 1000);
    }
    
    /**
     * Show payment success
     */
    showPaymentSuccess(message, transactionId, orderId) {
        this.showStatus(message, 'success');
        
        // Update page content
        const statusElement = document.getElementById('payment-status');
        if (statusElement) {
            statusElement.innerHTML = `
                <div class="alert alert-success">
                    <i class="bi bi-check-circle-fill me-2"></i>
                    <strong>Thanh toán thành công!</strong>
                    <p class="mb-1">${message}</p>
                    ${transactionId ? `<small>Mã giao dịch: ${transactionId}</small>` : ''}
                </div>
            `;
        }
        
        // Show success toast
        this.showToast('Thanh toán thành công!', message, 'success');
        
        // Redirect to order detail after 3 seconds
        setTimeout(() => {
            window.location.href = `/account/orders/${orderId.replace('ORDER-', '')}?payment=success&transId=${transactionId}`;
        }, 3000);
    }
    
    /**
     * Show payment failed
     */
    showPaymentFailed(message, orderId) {
        this.showStatus(message, 'error');
        
        // Update page content
        const statusElement = document.getElementById('payment-status');
        if (statusElement) {
            statusElement.innerHTML = `
                <div class="alert alert-danger">
                    <i class="bi bi-x-circle-fill me-2"></i>
                    <strong>Thanh toán thất bại!</strong>
                    <p class="mb-0">${message}</p>
                </div>
            `;
        }
        
        // Show error toast
        this.showToast('Thanh toán thất bại!', message, 'error');
        
        // Redirect to orders page after 5 seconds
        setTimeout(() => {
            window.location.href = `/account/orders?payment=failed&message=${encodeURIComponent(message)}`;
        }, 5000);
    }
    
    /**
     * Show status message
     */
    showStatus(message, type = 'info') {
        const statusElement = document.getElementById('sse-status');
        if (statusElement) {
            const iconMap = {
                info: 'bi-info-circle',
                success: 'bi-check-circle-fill',
                warning: 'bi-exclamation-triangle-fill',
                error: 'bi-x-circle-fill'
            };
            
            const colorMap = {
                info: 'text-info',
                success: 'text-success',
                warning: 'text-warning',
                error: 'text-danger'
            };
            
            statusElement.innerHTML = `
                <i class="bi ${iconMap[type]} ${colorMap[type]} me-2"></i>
                ${message}
            `;
        }
    }
    
    /**
     * Show toast notification
     */
    showToast(title, message, type = 'info') {
        // Create toast if not exists
        let toastContainer = document.getElementById('toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }
        
        const toastId = `toast-${Date.now()}`;
        const bgClass = type === 'success' ? 'bg-success' : type === 'error' ? 'bg-danger' : 'bg-info';
        
        const toastHtml = `
            <div id="${toastId}" class="toast ${bgClass} text-white" role="alert">
                <div class="toast-header ${bgClass} text-white border-0">
                    <strong class="me-auto">${title}</strong>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
                </div>
                <div class="toast-body">
                    ${message}
                </div>
            </div>
        `;
        
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        
        // Show toast
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { delay: 5000 });
        toast.show();
        
        // Remove toast after hide
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
    
    /**
     * Disconnect SSE
     */
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
            this.isConnected = false;
        }
    }
    
    /**
     * Get connection status
     */
    getStatus() {
        return {
            isConnected: this.isConnected,
            orderId: this.orderId,
            retryCount: this.retryCount
        };
    }
}

// Global instance
window.paymentSSE = new PaymentSSE();

// Auto-connect if orderId is in URL
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const orderId = urlParams.get('orderId');
    
    if (orderId) {
        window.paymentSSE.subscribe(orderId);
    }
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (window.paymentSSE) {
        window.paymentSSE.disconnect();
    }
});
