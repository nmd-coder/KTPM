/**
 * ACCOUNT ORDERS PAGE - JAVASCRIPT
 * Load và hiển thị orders động từ backend API
 */

// Global variables
let currentPage = 0;
let currentStatus = 'all';
const pageSize = 10;

// Helper to get translation (falls back when missing)
function t(key, fallback) {
    if (typeof window.languageSwitcher !== 'undefined' && window.languageSwitcher && typeof window.languageSwitcher.translate === 'function') {
        const translated = window.languageSwitcher.translate(key);
        // If translation system returns the key (missing), use fallback
        if (translated && translated !== key) {
            return translated;
        }
    }
    return fallback || key;
}


// Format currency VND
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

// Format date
function formatDate(dateString) {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}

// Get status badge HTML with Tailwind styling
function getStatusBadge(status) {
    const statusMap = {
        'PENDING': { 
            class: 'bg-yellow-100 text-yellow-700 border-yellow-200', 
            icon: 'clock-history', 
            textKey: 'pending'
        },
        'PROCESSING': { 
            class: 'bg-blue-100 text-blue-700 border-blue-200', 
            icon: 'arrow-repeat', 
            textKey: 'processing'
        },
        'SHIPPED': { 
            class: 'bg-purple-100 text-purple-700 border-purple-200', 
            icon: 'truck', 
            textKey: 'shipping'
        },
        'COMPLETED': { 
            class: 'bg-green-100 text-green-700 border-green-200', 
            icon: 'check-circle-fill', 
            textKey: 'completed'
        },
        'RECEIVED': { 
            class: 'bg-emerald-100 text-emerald-700 border-emerald-200', 
            icon: 'check-circle-fill', 
            textKey: 'received'
        },
        'CANCELLED': { 
            class: 'bg-red-100 text-red-700 border-red-200', 
            icon: 'x-circle-fill', 
            textKey: 'cancelled'
        }
    };
    
    const statusInfo = statusMap[status] || statusMap['PENDING'];
    const text = t(statusInfo.textKey, statusInfo.textKey === 'received' ? 'Đã nhận hàng' : statusInfo.textKey);
    return `
        <span class="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-semibold border ${statusInfo.class}">
            <svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 2a8 8 0 1 0 0 16 8 8 0 0 0 0-16zm0 14a6 6 0 1 1 0-12 6 6 0 0 1 0 12z"/>
            </svg>
            ${text}
        </span>
    `;
}

// Get action buttons based on order status with Tailwind styling
function getActionButtons(order) {
    const buttons = [];
    
    // Cancel button - for PENDING or PROCESSING status
    if (order.status === 'PENDING' || order.status === 'PROCESSING') {
        const cancelText = t('cancel-order', 'Hủy đơn');
        buttons.push(`
            <button onclick="cancelOrder('${order.id}')" 
                    class="group relative px-6 py-3 bg-white border-2 border-gray-200 text-gray-700 rounded-2xl font-semibold hover:border-red-300 hover:bg-red-50 hover:text-red-700 transition-all duration-300 transform hover:scale-105 hover:shadow-lg focus:outline-none focus:ring-4 focus:ring-red-100">
                <div class="flex items-center justify-center gap-2">
                    <div class="relative">
                        <svg class="w-5 h-5 transition-transform duration-300 group-hover:scale-110" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd"/>
                        </svg>
                        <div class="absolute inset-0 bg-red-400 rounded-full opacity-0 group-hover:opacity-20 transition-opacity duration-300 blur-sm"></div>
                    </div>
                    <span class="relative z-10">${cancelText}</span>
                </div>
            </button>
        `);
    }
    
    // Track button - for SHIPPED status
    if (order.status === 'SHIPPED') {
        buttons.push(`
            <button onclick="trackOrder(${order.id})" 
                    class="group relative px-6 py-3 bg-white border-2 border-gray-200 text-gray-700 rounded-2xl font-semibold hover:border-purple-300 hover:bg-purple-50 hover:text-purple-700 transition-all duration-300 transform hover:scale-105 hover:shadow-lg focus:outline-none focus:ring-4 focus:ring-purple-100">
                <div class="flex items-center justify-center gap-2">
                    <div class="relative">
                        <svg class="w-5 h-5 transition-transform duration-300 group-hover:scale-110" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"/>
                        </svg>
                        <div class="absolute inset-0 bg-purple-400 rounded-full opacity-0 group-hover:opacity-20 transition-opacity duration-300 blur-sm"></div>
                    </div>
                    <span class="relative z-10">Theo dõi</span>
                </div>
            </button>
        `);
    }
    
    // Confirm received button - for COMPLETED status (Shopee-like flow)
    if (order.status === 'COMPLETED') {
        buttons.push(`
            <button onclick="confirmOrderReceived('${order.id}')" 
                    class="group relative px-8 py-4 bg-gradient-to-r from-emerald-500 via-green-500 to-teal-600 text-white rounded-3xl font-bold text-sm shadow-lg hover:shadow-2xl transition-all duration-500 transform hover:scale-110 hover:-translate-y-1 focus:outline-none focus:ring-4 focus:ring-emerald-200 focus:ring-opacity-50 overflow-hidden">
                <!-- Animated background -->
                <div class="absolute inset-0 bg-gradient-to-r from-emerald-400 via-green-400 to-teal-500 opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
                
                <!-- Shimmer effect -->
                <div class="absolute inset-0 -top-2 -left-2 w-full h-full bg-gradient-to-r from-transparent via-white to-transparent opacity-0 group-hover:opacity-20 group-hover:animate-pulse"></div>
                
                <!-- Content -->
                <div class="relative z-10 flex items-center justify-center gap-3">
                    <!-- Icon with animation -->
                    <div class="relative">
                        <div class="absolute inset-0 bg-white rounded-full opacity-0 group-hover:opacity-30 transition-opacity duration-300 blur-md scale-150"></div>
                        <svg class="w-6 h-6 transition-all duration-500 group-hover:scale-125 group-hover:rotate-12" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                    </div>
                    
                    <!-- Text with subtle animation -->
                    <span class="relative font-bold tracking-wide">
                        <span class="inline-block transition-transform duration-300 group-hover:translate-x-1">Đã nhận</span>
                        <span class="inline-block transition-transform duration-300 group-hover:-translate-x-1">hàng</span>
                    </span>
                    
                    <!-- Arrow icon -->
                    <svg class="w-4 h-4 transition-all duration-300 group-hover:translate-x-1 group-hover:scale-110" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z" clip-rule="evenodd"/>
                    </svg>
                </div>
                
                <!-- Ripple effect on click -->
                <div class="absolute inset-0 rounded-3xl overflow-hidden">
                    <div class="absolute inset-0 bg-white opacity-0 group-active:opacity-20 group-active:animate-ping"></div>
                </div>
            </button>
        `);
    }
    
    // Review button - for RECEIVED status (after user confirms)
    if (order.status === 'RECEIVED') {
        // Check if order has been reviewed
        const hasReview = order.hasReview || (order.orderItems && order.orderItems.some(item => item.hasReview));
        
        if (hasReview) {
            // Show "View Review" button
            buttons.push(`
                <button onclick="viewOrderReview('${order.id}')" 
                        class="group relative px-6 py-3 bg-white border-2 border-green-200 text-green-700 rounded-2xl font-semibold hover:border-green-300 hover:bg-green-50 hover:text-green-800 transition-all duration-300 transform hover:scale-105 hover:shadow-lg focus:outline-none focus:ring-4 focus:ring-green-100">
                    <div class="flex items-center justify-center gap-2">
                        <div class="relative">
                            <svg class="w-5 h-5 transition-transform duration-300 group-hover:scale-110" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                                <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
                            </svg>
                            <div class="absolute inset-0 bg-green-400 rounded-full opacity-0 group-hover:opacity-20 transition-opacity duration-300 blur-sm"></div>
                        </div>
                        <span class="relative z-10">Xem đánh giá</span>
                    </div>
                </button>
            `);
        }
    }
    
    // Reorder button - ONLY for RECEIVED
    if (order.status === 'RECEIVED') {
        const reorderText = t('reorder', 'Mua lại');
        buttons.push(`
            <button onclick="reorder(${order.id})" 
                    class="group relative px-6 py-3 bg-white border-2 border-gray-200 text-gray-700 rounded-2xl font-semibold hover:border-emerald-300 hover:bg-emerald-50 hover:text-emerald-700 transition-all duration-300 transform hover:scale-105 hover:shadow-lg focus:outline-none focus:ring-4 focus:ring-emerald-100">
                <div class="flex items-center justify-center gap-2">
                    <div class="relative">
                        <svg class="w-5 h-5 transition-transform duration-300 group-hover:scale-110" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M15.312 11.424a5.5 5.5 0 01-9.201 2.466l-.312-.311h2.433a.75.75 0 000-1.5H3.989a.75.75 0 00-.75.75v4.242a.75.75 0 001.5 0v-2.43l.31.31a7 7 0 0011.712-3.138.75.75 0 00-1.449-.39zm1.23-3.723a.75.75 0 00.219-.53V2.929a.75.75 0 00-1.5 0V5.36l-.31-.31A7 7 0 003.239 8.188a.75.75 0 101.448.389A5.5 5.5 0 0113.89 6.11l.311.31h-2.432a.75.75 0 000 1.5h4.243a.75.75 0 00.53-.219z" clip-rule="evenodd"/>
                        </svg>
                        <div class="absolute inset-0 bg-emerald-400 rounded-full opacity-0 group-hover:opacity-20 transition-opacity duration-300 blur-sm"></div>
                    </div>
                    <span class="relative z-10">${reorderText}</span>
                </div>
            </button>
        `);
    }
    
    return buttons.join('');
}

// Render single order card with Tailwind styling
function renderOrderCard(order) {
    const statusBadge = getStatusBadge(order.status);
    const actionButtons = getActionButtons(order);
    
    let itemsHTML = '';
    if (order.items && order.items.length > 0) {
        // Show max 3 items, then "+ X more"
        const displayItems = order.items.slice(0, 3);
        const remainingCount = order.items.length - 3;
        
        itemsHTML = displayItems.map(item => `
            <div class="flex items-center gap-4 p-4 bg-gray-50 rounded-xl">
                <div class="w-20 h-20 flex-shrink-0">
                    <img src="${item.productImage || '/images/placeholder.jpg'}" 
                         alt="${item.productName}"
                         class="w-full h-full object-cover rounded-lg"
                         onerror="this.src='/images/placeholder.jpg'">
                </div>
                <div class="flex-1 min-w-0">
                    <h4 class="font-semibold text-gray-900 truncate">${item.productName}</h4>
                    <p class="text-sm text-gray-600 mt-1">${t('quantity', 'Số lượng')}: ${item.quantity} × ${formatCurrency(item.price)}</p>
                    ${order.status === 'COMPLETED' ? `
                        ${item.hasReview ? `
                            <button onclick="viewOrderReview('${order.id}')" 
                                    class="mt-2 px-3 py-1.5 bg-gradient-to-r from-green-500 to-emerald-600 text-white text-xs font-medium rounded-lg hover:shadow-md transition-all">
                                <svg class="w-4 h-4 mr-1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                    <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
                                    <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
                                </svg> Xem đánh giá
                            </button>
                        ` : `
                            <span class="mt-2 px-3 py-1.5 bg-gray-100 text-gray-500 text-xs font-medium rounded-lg">
                                <svg class="w-4 h-4 mr-1 inline" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                    <path d="M10.868 2.884c-.321-.772-1.415-.772-1.736 0l-1.83 4.401-4.753.381c-.833.067-1.171 1.107-.536 1.651l3.62 3.102-1.106 4.637c-.194.813.691 1.456 1.405 1.02L10 15.591l4.069 2.485c.713.436 1.598-.207 1.404-1.02l-1.106-4.637 3.62-3.102c.635-.544.297-1.584-.536-1.65l-4.752-.382-1.831-4.401Z"/>
                                </svg> Đánh giá từng sản phẩm
                            </span>
                        `}
                    ` : ''}
                </div>
                <div class="text-right">
                    <p class="font-bold text-gray-900">${formatCurrency(item.price * item.quantity)}</p>
                </div>
            </div>
        `).join('');
        
        if (remainingCount > 0) {
            itemsHTML += `
                <div class="text-center py-2 text-sm text-gray-600">
                    <svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M10 3a1.5 1.5 0 110 3 1.5 1.5 0 010-3zM10 8.5a1.5 1.5 0 110 3 1.5 1.5 0 010-3zM11.5 15.5a1.5 1.5 0 10-3 0 1.5 1.5 0 003 0z"/>
                    </svg> Và ${remainingCount} sản phẩm khác
                </div>
            `;
        }
    }
    
    return `
        <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow">
            <!-- Header -->
            <div class="flex items-center justify-between p-6 border-b border-gray-100">
                <div>
                    <div class="flex items-center gap-3 mb-2">
                        <span class="text-lg font-bold text-gray-900">#${order.id}</span>
                        ${statusBadge}
                    </div>
                    <p class="text-sm text-gray-600">
                        <svg class="w-4 h-4 mr-1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M5.75 2a.75.75 0 01.75.75V4h7V2.75a.75.75 0 011.5 0V4h.25A2.75 2.75 0 0118 6.75v8.5A2.75 2.75 0 0115.25 18H4.75A2.75 2.75 0 012 15.25v-8.5A2.75 2.75 0 014.75 4H5V2.75A.75.75 0 015.75 2zm-1 5.5c-.69 0-1.25.56-1.25 1.25v6.5c0 .69.56 1.25 1.25 1.25h10.5c.69 0 1.25-.56 1.25-1.25v-6.5c0-.69-.56-1.25-1.25-1.25H4.75z" clip-rule="evenodd"/>
                        </svg>
                        ${formatDate(order.orderDate)}
                    </p>
                </div>
                <button onclick="viewOrderDetail(${order.id})" class="text-pink-600 hover:text-pink-700 font-medium text-sm">
                    ${t('details', 'Chi tiết')} <svg class="w-4 h-4 ml-1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd"/>
                    </svg>
                </button>
            </div>
            
            <!-- Items -->
            <div class="p-6 space-y-3">
                ${itemsHTML}
            </div>
            
            <!-- Footer -->
            <div class="flex items-center justify-between p-6 bg-gray-50 border-t border-gray-100">
                <div>
                    <p class="text-sm text-gray-600 mb-1">${t('total-payment', 'Tổng thanh toán')}</p>
                    <p class="text-2xl font-bold bg-gradient-to-r from-pink-500 to-purple-600 bg-clip-text text-transparent">
                        ${formatCurrency(order.totalAmount)}
                    </p>
                </div>
                <div class="flex items-center gap-2">
                    ${actionButtons}
                </div>
            </div>
        </div>
    `;
}

// Load orders from API
async function loadOrders(status = 'all', page = 0) {
    console.log('Loading orders with status:', status, 'page:', page);
    const ordersList = document.getElementById('ordersList');
    
    if (!ordersList) {
        console.error('ordersList element not found');
        return;
    }
    
    // Show loading state
    ordersList.innerHTML = `
        <div class="flex items-center justify-center py-20">
            <div class="text-center">
                <div class="inline-block animate-spin rounded-full h-12 w-12 border-4 border-pink-200 border-t-pink-600 mb-4"></div>
                <p class="text-gray-600">Đang tải đơn hàng...</p>
            </div>
        </div>
    `;
    
    try {
        // Build API URL
        let apiUrl = `/api/orders/list?page=${page}&size=${pageSize}`;
        if (status !== 'all') {
            apiUrl += `&status=${status}`;
        }
        
        console.log('Fetching orders from:', apiUrl);
        
        const response = await fetch(apiUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        console.log('Response status:', response.status);
        
        if (!response.ok) {
            throw new Error(`Failed to load orders: ${response.status} ${response.statusText}`);
        }
        
        const data = await response.json();
        console.log('Orders data received:', data);
        
        // Check if we have orders
        if (!data.data || !data.data.content || data.data.content.length === 0) {
            ordersList.innerHTML = `
                <div class="text-center py-20">
                    <div class="inline-flex items-center justify-center w-24 h-24 bg-gradient-to-br from-pink-100 to-purple-100 rounded-full mb-6">
                        <svg class="w-12 h-12 text-pink-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M1 11.27c0-.246.033-.492.099-.73l1.523-5.521A2.75 2.75 0 015.273 3h9.454a2.75 2.75 0 012.651 2.019l1.523 5.52c.066.239.099.485.099.732V15a2 2 0 01-2 2H3a2 2 0 01-2-2v-3.73zM15 16a1 1 0 100-2 1 1 0 000 2zm-8-1a1 1 0 11-2 0 1 1 0 012 0z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                    <h3 class="text-2xl font-bold text-gray-900 mb-3">Không có đơn hàng nào</h3>
                    <p class="text-gray-600">
                        ${status === 'all' 
                            ? 'Bạn chưa có đơn hàng nào trong hệ thống.' 
                            : 'Không tìm thấy đơn hàng với trạng thái này.'}
                    </p>
                </div>
            `;
            return;
        }
        
        // Render orders
        const ordersHtml = data.data.content.map(order => renderOrderCard(order)).join('');
        ordersList.innerHTML = ordersHtml;
        
        // Update status counts in filter tabs
        updateStatusCounts();
        
    } catch (error) {
        console.error('Error loading orders:', error);
        ordersList.innerHTML = `
            <div class="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
                <svg class="w-8 h-8 text-red-600 mb-3" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/>
                </svg>
                <p class="text-red-700 font-medium">Có lỗi xảy ra khi tải đơn hàng.</p>
                <p class="text-red-600 text-sm mt-1">Vui lòng thử lại sau.</p>
                <p class="text-red-500 text-xs mt-2">Lỗi: ${error.message}</p>
            </div>
        `;
    }
}

// Update status counts in filter tabs and quick stats
async function updateStatusCounts() {
    try {
        const response = await fetch('/api/orders/list?page=0&size=1000');
        if (!response.ok) return;
        
        const data = await response.json();
        const orders = data.data.content || [];
        
        // Count by status
        const counts = {
            'all': orders.length,
            'PENDING': 0,
            'PROCESSING': 0,
            'SHIPPED': 0,
            'COMPLETED': 0,
            'CANCELLED': 0
        };
        
        orders.forEach(order => {
            if (counts[order.status] !== undefined) {
                counts[order.status]++;
            }
        });
        
        // Update quick stats cards
        const pendingCount = document.getElementById('pendingCount');
        const shippedCount = document.getElementById('shippedCount');
        const completedCount = document.getElementById('completedCount');
        
        if (pendingCount) pendingCount.textContent = counts['PENDING'];
        if (shippedCount) shippedCount.textContent = counts['SHIPPED'];
        if (completedCount) completedCount.textContent = counts['COMPLETED'];
        
    } catch (error) {
        // Error updating status counts
    }
}

// Filter tabs click handler with Tailwind styling
function setupFilterTabs() {
    document.querySelectorAll('.filter-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            // Remove active styles from all tabs
            document.querySelectorAll('.filter-tab').forEach(t => {
                t.classList.remove('bg-gradient-to-r', 'from-pink-500', 'to-purple-600', 'text-white', 'shadow-md');
                t.classList.add('text-gray-700', 'hover:bg-gray-100');
            });
            
            // Add active styles to clicked tab
            this.classList.remove('text-gray-700', 'hover:bg-gray-100');
            this.classList.add('bg-gradient-to-r', 'from-pink-500', 'to-purple-600', 'text-white', 'shadow-md');
            
            // Get status and load orders
            const status = this.getAttribute('data-status');
            currentStatus = status;
            currentPage = 0;
            loadOrders(status, currentPage);
        });
    });
}

// View order detail
function viewOrderDetail(orderId) {
    // Support both button click and direct ID
    const id = typeof orderId === 'number' ? orderId : orderId;
    window.location.href = `/orders/${id}`;
}

// Cancel order
async function cancelOrder(orderId) {
    // THAY THẾ confirm bằng SweetAlert2
    Swal.fire({
        title: 'Bạn chắc chắn muốn hủy?',
        text: "Hành động này không thể hoàn tác!",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Đồng ý hủy',
        cancelButtonText: 'Không'
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                const response = await fetch(`/api/orders/${orderId}/cancel`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                if (!response.ok) {
                    throw new Error('Failed to cancel order');
                }
                
                showToast('Đơn hàng đã được hủy thành công', 'success');
                loadOrders(currentStatus, currentPage);
                
            } catch (error) {
                showToast('Có lỗi xảy ra khi hủy đơn hàng', 'error');
            }
        }
    });
}

// Confirm order received (Shopee-like flow)
async function confirmOrderReceived(orderId) {
    Swal.fire({
        title: 'Xác nhận đã nhận hàng?',
        text: "Sau khi xác nhận, bạn có thể đánh giá đơn hàng này.",
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#10b981',
        cancelButtonColor: '#6b7280',
        confirmButtonText: 'Đã nhận hàng',
        cancelButtonText: 'Hủy'
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                console.log('Confirming order received:', orderId);
                const response = await fetch(`/api/orders/${orderId}/confirm-received`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                console.log('Response status:', response.status);
                const data = await response.json();
                console.log('Response data:', data);
                
                if (!response.ok || !data.success) {
                    const errorMsg = data.message || data.error || 'Failed to confirm order received';
                    console.error('Error confirming order:', errorMsg);
                    throw new Error(errorMsg);
                }
                
                Swal.fire({
                    title: 'Thành công!',
                    text: 'Bạn đã xác nhận nhận hàng. Giờ bạn có thể đánh giá đơn hàng này.',
                    icon: 'success',
                    confirmButtonColor: '#10b981'
                });
                
                // Reload orders to show updated status
                loadOrders(currentStatus, currentPage);
                
            } catch (error) {
                Swal.fire({
                    title: 'Lỗi!',
                    text: error.message || 'Có lỗi xảy ra khi xác nhận đơn hàng',
                    icon: 'error',
                    confirmButtonColor: '#ef4444'
                });
            }
        }
    });
}

// Track order
function trackOrder(orderId) {
    // TODO: Implement order tracking
    showToast('Tính năng theo dõi đơn hàng đang được phát triển', 'info');
}

// Review order
function reviewOrder(orderId) {
    // Fetch order details to get concrete order items, then open the review modal
    fetch(`/api/orders/${orderId}`)
        .then(async (res) => {
            const body = await res.json().catch(() => ({}));
            if (!res.ok) {
                const msg = (body && (body.message || body.error)) || 'Không thể lấy chi tiết đơn hàng';
                throw new Error(msg);
            }
            return body;
        })
        .then((payload) => {
            // ResponseWrapper shape: { data: { ...order... } } or nested { data: { data: { ... } } }
            const wrapped = payload && payload.data ? payload.data : payload;
            const orderData = wrapped && wrapped.data ? wrapped.data : wrapped;

            // Try multiple shapes to find items
            const items =
                (orderData && orderData.items) ||
                (orderData && orderData.orderItems) ||
                (orderData && orderData.order && (orderData.order.items || orderData.order.orderItems)) ||
                [];

            if (!items || items.length === 0) {
                showToast('Không tìm thấy sản phẩm để đánh giá trong đơn này', 'info');
                return;
            }

            // Pick first item to review (UI also allows per-item review buttons)
            const it = items[0];
            const orderItemId = it.id || it.orderItemId;
            const productId = it.productId || (it.product && it.product.id);
            const productName = it.productName || (it.product && it.product.name) || 'Sản phẩm';
            const productImage = it.productImage || (it.product && it.product.image) || '/images/placeholder.jpg';

            if (!orderItemId || !productId) {
                showToast('Không đủ dữ liệu để mở form đánh giá', 'error');
                return;
            }

            // Redirect to order detail page for individual product review
            window.location.href = `/orders/${orderId}`;
        })
        .catch((err) => {
            console.error('reviewOrder error:', err);
            showToast(err.message || 'Có lỗi xảy ra khi mở form đánh giá', 'error');
        });
}

// Reorder
function reorder(orderId) {
    // Call reorder API
    fetch(`/api/orders/${orderId}/reorder`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'same-origin'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            const result = data.data;
            
            if (result.addedProducts && result.addedProducts.length > 0) {
                // Show success message for added products
                showToast(result.message, 'success');
                
                // Show warning for unavailable products
                if (result.unavailableProducts && result.unavailableProducts.length > 0) {
                    setTimeout(() => {
                        showToast('Sản phẩm không còn bán: ' + result.unavailableProducts.join(', '), 'warning');
                    }, 1500);
                }
                
                // Show warning for out of stock products
                if (result.outOfStockProducts && result.outOfStockProducts.length > 0) {
                    setTimeout(() => {
                        showToast('Sản phẩm hết hàng: ' + result.outOfStockProducts.join(', '), 'warning');
                    }, 3000);
                }
                
                // Reload cart count
                if (typeof loadCartCount === 'function') {
                    loadCartCount();
                }
                
                // Ask user if they want to go to checkout with just reordered items selected
                setTimeout(async () => {
                    try {
                        if (result.addedProductIds && result.addedProductIds.length > 0) {
                            // Store selected product IDs for checkout page to preselect
                            sessionStorage.setItem('selectedCartItems', JSON.stringify(result.addedProductIds));
                        }
                    } catch (e) {
                        console.warn('Unable to persist selectedCartItems:', e);
                    }
                    const confirmed = await showConfirm({
                        title: 'Mua lại thành công!',
                        text: 'Sản phẩm đã được thêm vào giỏ hàng. Bạn có muốn chuyển đến trang thanh toán không?',
                        icon: 'success',
                        confirmButtonText: 'Đi thanh toán',
                        cancelButtonText: 'Ở lại trang này'
                    });
                    
                    if (confirmed) {
                        window.location.href = '/checkout';
                    }
                }, 1000);
            } else {
                showToast('Không có sản phẩm nào có thể mua lại từ đơn hàng này', 'warning');
            }
        } else {
            showToast(data.message || 'Có lỗi xảy ra khi mua lại đơn hàng', 'error');
        }
    })
    .catch(error => {
        console.error('Error reordering:', error);
        showToast('Có lỗi xảy ra khi mua lại đơn hàng', 'error');
    });
}

// Show toast notification
function showToast(message, type = 'info') {
    // Create toast element
    const toast = document.createElement('div');
    toast.className = `alert alert-${type === 'success' ? 'success' : type === 'error' ? 'danger' : 'info'} position-fixed top-0 end-0 m-3`;
    toast.style.zIndex = '9999';
    toast.innerHTML = `
        <i class="bi bi-${type === 'success' ? 'check-circle' : type === 'error' ? 'x-circle' : 'info-circle'}"></i>
        ${message}
    `;
    
    document.body.appendChild(toast);
    
    // Auto remove after 3 seconds
    setTimeout(() => {
        toast.remove();
    }, 3000);
}


// View existing order review
async function viewOrderReview(orderId) {
    try {
        // Fetch order reviews
        const response = await fetch(`/api/orders/${orderId}/reviews`);
        const data = await response.json();
        
        if (data.success && data.data && data.data.length > 0) {
            // Show review modal
            showOrderReviewModal(data.data, orderId);
        } else {
            showToast('Không tìm thấy đánh giá cho đơn hàng này', 'info');
        }
    } catch (error) {
        console.error('Error loading order reviews:', error);
        showToast('Có lỗi xảy ra khi tải đánh giá', 'error');
    }
}

// Show order review modal
function showOrderReviewModal(reviews, orderId) {
    // Create modal HTML
    const modalHTML = `
        <div id="orderReviewModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div class="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                <!-- Header -->
                <div class="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 rounded-t-2xl">
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                            <div class="w-10 h-10 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                                <svg class="w-6 h-6 text-white" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                    <path d="M10.868 2.884c-.321-.772-1.415-.772-1.736 0l-1.83 4.401-4.753.381c-.833.067-1.171 1.107-.536 1.651l3.62 3.102-1.106 4.637c-.194.813.691 1.456 1.405 1.02L10 15.591l4.069 2.485c.713.436 1.598-.207 1.404-1.02l-1.106-4.637 3.62-3.102c.635-.544.297-1.584-.536-1.65l-4.752-.382-1.831-4.401Z"/>
                                </svg>
                            </div>
                            <div>
                                <h3 class="text-lg font-bold text-gray-900">Đánh giá đơn hàng #${orderId}</h3>
                                <p class="text-sm text-gray-600">Xem đánh giá và phản hồi từ shop</p>
                            </div>
                        </div>
                        <button onclick="closeOrderReviewModal()" class="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                            <svg class="w-6 h-6 text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                            </svg>
                        </button>
                    </div>
                </div>
                
                <!-- Content -->
                <div class="p-6 space-y-6">
                    ${reviews.map(review => `
                        <div class="border border-gray-200 rounded-xl p-5 space-y-4">
                            <!-- Product Info -->
                            <div class="flex items-center gap-4">
                                <img src="${review.productImage || '/images/placeholder.jpg'}" 
                                     alt="${review.productName}"
                                     class="w-16 h-16 object-cover rounded-lg"
                                     onerror="this.src='/images/placeholder.jpg'">
                                <div class="flex-1">
                                    <h4 class="font-semibold text-gray-900">${review.productName}</h4>
                                    <div class="flex items-center gap-2 mt-1">
                                        <div class="flex items-center">
                                            ${generateStars(review.rating)}
                                        </div>
                                        <span class="text-sm font-medium text-gray-700">${review.rating}/5</span>
                                        <span class="text-xs text-gray-500">${formatDate(review.createdAt)}</span>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- Review Comment -->
                            ${review.comment ? `
                                <div class="bg-blue-50 border-l-4 border-blue-400 p-4 rounded-r-lg">
                                    <div class="flex items-start gap-2">
                                        <svg class="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                            <path fill-rule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z"/>
                                        </svg>
                                        <div>
                                            <p class="text-sm font-medium text-blue-900 mb-1">Đánh giá của bạn</p>
                                            <p class="text-sm text-blue-800">${review.comment}</p>
                                        </div>
                                    </div>
                                </div>
                            ` : ''}
                            
                            <!-- Admin Response -->
                            ${review.adminResponse ? `
                                <div class="bg-green-50 border-l-4 border-green-400 p-4 rounded-r-lg">
                                    <div class="flex items-start gap-2">
                                        <svg class="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                            <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z"/>
                                        </svg>
                                        <div>
                                            <p class="text-sm font-medium text-green-900 mb-1">Phản hồi từ StarShop</p>
                                            <p class="text-sm text-green-800">${review.adminResponse}</p>
                                            ${review.adminResponseAt ? `
                                                <p class="text-xs text-green-600 mt-2">
                                                    ${review.adminResponseByName || 'Admin'} • ${formatDate(review.adminResponseAt)}
                                                </p>
                                            ` : ''}
                                        </div>
                                    </div>
                                </div>
                            ` : `
                                <div class="bg-gray-50 border-l-4 border-gray-300 p-4 rounded-r-lg">
                                    <div class="flex items-center gap-2">
                                        <svg class="w-5 h-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"/>
                                        </svg>
                                        <p class="text-sm text-gray-600">Shop chưa phản hồi đánh giá này</p>
                                    </div>
                                </div>
                            `}
                        </div>
                    `).join('')}
                </div>
                
                <!-- Footer -->
                <div class="sticky bottom-0 bg-white border-t border-gray-200 px-6 py-4 rounded-b-2xl">
                    <div class="flex justify-end">
                        <button onclick="closeOrderReviewModal()" 
                                class="px-6 py-2 bg-gray-600 hover:bg-gray-700 text-white font-medium rounded-lg transition-colors">
                            Đóng
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Add modal to DOM
    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

// Close order review modal
function closeOrderReviewModal() {
    const modal = document.getElementById('orderReviewModal');
    if (modal) {
        modal.remove();
    }
}

// Generate stars HTML
function generateStars(rating) {
    let starsHTML = '';
    for (let i = 1; i <= 5; i++) {
        if (i <= rating) {
            starsHTML += '<svg class="w-4 h-4 text-yellow-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path d="M10.868 2.884c-.321-.772-1.415-.772-1.736 0l-1.83 4.401-4.753.381c-.833.067-1.171 1.107-.536 1.651l3.62 3.102-1.106 4.637c-.194.813.691 1.456 1.405 1.02L10 15.591l4.069 2.485c.713.436 1.598-.207 1.404-1.02l-1.106-4.637 3.62-3.102c.635-.544.297-1.584-.536-1.65l-4.752-.382-1.831-4.401Z"/></svg>';
        } else {
            starsHTML += '<svg class="w-4 h-4 text-gray-300" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path d="M10.868 2.884c-.321-.772-1.415-.772-1.736 0l-1.83 4.401-4.753.381c-.833.067-1.171 1.107-.536 1.651l3.62 3.102-1.106 4.637c-.194.813.691 1.456 1.405 1.02L10 15.591l4.069 2.485c.713.436 1.598-.207 1.404-1.02l-1.106-4.637 3.62-3.102c.635-.544.297-1.584-.536-1.65l-4.752-.382-1.831-4.401Z"/></svg>';
        }
    }
    return starsHTML;
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM Content Loaded - Initializing orders page');
    
    // Setup filter tabs
    setupFilterTabs();
    
    // Star rating buttons will be set up when modal opens
    
    // Setup comment character counter
    const commentTextarea = document.getElementById('reviewComment');
    if (commentTextarea) {
        commentTextarea.addEventListener('input', function() {
            const count = this.value.length;
            document.getElementById('commentCount').textContent = `${count}/1000`;
        });
    }
    
    // Load initial orders
    console.log('Loading initial orders...');
    loadOrders('all', 0);
    
    // Expose to global scope for language switcher
    window.loadOrders = loadOrders;
    window.currentStatus = 'all';
    window.currentPage = 0;
});
