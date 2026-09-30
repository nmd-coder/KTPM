/**
 * ========================================
 * STARSHOP NOTIFICATION SYSTEM
 * Centralized notification utilities
 * ========================================
 * 
 * Sử dụng SweetAlert2 cho tất cả thông báo
 * Fallback về native alert/confirm nếu SweetAlert2 chưa load
 * 
 * @author StarShop Team
 * @version 2.0
 */

/**
 * Hiển thị toast notification
 * @param {string} message - Nội dung thông báo
 * @param {string} type - Loại: 'success', 'error', 'warning', 'info'
 * @param {object} options - Tùy chọn thêm (timer, position, etc.)
 */
function showToast(message, type = 'success', options = {}) {
    // Check if SweetAlert2 is loaded
    if (typeof Swal === 'undefined') {
        console.warn('SweetAlert2 not loaded, falling back to native alert');
        alert(message);
        return;
    }

    const defaultOptions = {
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        didOpen: (toast) => {
            toast.addEventListener('mouseenter', Swal.stopTimer);
            toast.addEventListener('mouseleave', Swal.resumeTimer);
        }
    };

    const Toast = Swal.mixin({
        ...defaultOptions,
        ...options
    });

    Toast.fire({
        icon: type,
        title: message
    });
}

/**
 * Hiển thị confirmation dialog
 * @param {object} config - Configuration object
 * @param {string} config.title - Tiêu đề
 * @param {string} config.text - Nội dung
 * @param {string} config.icon - Icon: 'question', 'warning', 'info'
 * @param {string} config.confirmButtonText - Text nút xác nhận
 * @param {string} config.cancelButtonText - Text nút hủy
 * @param {boolean} config.isDanger - Nếu true, dùng màu đỏ cho confirm button
 * @returns {Promise<boolean>} - true nếu user click confirm, false nếu cancel
 */
async function showConfirm(config) {
    // Check if SweetAlert2 is loaded
    if (typeof Swal === 'undefined') {
        console.warn('SweetAlert2 not loaded, falling back to native confirm');
        return confirm(config.text || config.title);
    }

    const {
        title = 'Xác nhận',
        text = '',
        icon = 'question',
        confirmButtonText = 'Xác nhận',
        cancelButtonText = 'Hủy',
        isDanger = false
    } = config;

    const result = await Swal.fire({
        title: title,
        text: text,
        icon: icon,
        showCancelButton: true,
        confirmButtonText: confirmButtonText,
        cancelButtonText: cancelButtonText,
        confirmButtonColor: isDanger ? '#d33' : '#3085d6',
        cancelButtonColor: isDanger ? '#3085d6' : '#d33',
        reverseButtons: isDanger // Reverse buttons for danger actions
    });

    return result.isConfirmed;
}

/**
 * Hiển thị dialog với HTML content
 * @param {object} config - Configuration object
 * @param {string} config.title - Tiêu đề
 * @param {string} config.html - HTML content
 * @param {string} config.icon - Icon: 'info', 'success', 'warning', 'error'
 * @param {string} config.confirmButtonText - Text nút đóng
 */
async function showDialog(config) {
    // Check if SweetAlert2 is loaded
    if (typeof Swal === 'undefined') {
        console.warn('SweetAlert2 not loaded, falling back to native alert');
        // Strip HTML tags for native alert
        const text = config.html ? config.html.replace(/<[^>]*>/g, '') : config.title;
        alert(text);
        return;
    }

    const {
        title = '',
        html = '',
        icon = 'info',
        confirmButtonText = 'Đóng'
    } = config;

    await Swal.fire({
        title: title,
        html: html,
        icon: icon,
        confirmButtonText: confirmButtonText
    });
}

/**
 * Hiển thị loading dialog
 * @param {string} message - Nội dung loading
 */
function showLoading(message = 'Đang xử lý...') {
    if (typeof Swal === 'undefined') {
        console.warn('SweetAlert2 not loaded, cannot show loading');
        return;
    }

    Swal.fire({
        title: message,
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });
}

/**
 * Đóng loading dialog
 */
function closeLoading() {
    if (typeof Swal !== 'undefined') {
        Swal.close();
    }
}

// ========================================
// SHORTHAND FUNCTIONS (Tiện lợi hơn)
// ========================================

/**
 * Success toast
 * @param {string} message 
 */
function showSuccess(message) {
    showToast(message, 'success');
}

/**
 * Error toast
 * @param {string} message 
 */
function showError(message) {
    showToast(message, 'error');
}

/**
 * Warning toast
 * @param {string} message 
 */
function showWarning(message) {
    showToast(message, 'warning');
}

/**
 * Info toast
 * @param {string} message 
 */
function showInfo(message) {
    showToast(message, 'info');
}

/**
 * Confirmation dialog cho destructive actions (xóa, hủy, etc.)
 * @param {string} title - Tiêu đề
 * @param {string} text - Nội dung
 * @returns {Promise<boolean>}
 */
async function confirmDelete(title = 'Xác nhận xóa?', text = 'Hành động này không thể hoàn tác!') {
    return await showConfirm({
        title: title,
        text: text,
        icon: 'warning',
        confirmButtonText: 'Xóa',
        cancelButtonText: 'Hủy',
        isDanger: true
    });
}

/**
 * Confirmation dialog cho normal actions
 * @param {string} title - Tiêu đề
 * @param {string} text - Nội dung
 * @returns {Promise<boolean>}
 */
async function confirmAction(title = 'Xác nhận?', text = '') {
    return await showConfirm({
        title: title,
        text: text,
        icon: 'question',
        confirmButtonText: 'Xác nhận',
        cancelButtonText: 'Hủy',
        isDanger: false
    });
}

// ========================================
// EXPORT (nếu dùng modules)
// ========================================
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        showToast,
        showConfirm,
        showDialog,
        showLoading,
        closeLoading,
        showSuccess,
        showError,
        showWarning,
        showInfo,
        confirmDelete,
        confirmAction
    };
}

// ========================================
// GLOBAL EXPOSURE (cho browser)
// ========================================
// Expose to window for easy access
if (typeof window !== 'undefined') {
    window.Notifications = {
        showToast,
        showConfirm,
        showDialog,
        showLoading,
        closeLoading,
        showSuccess,
        showError,
        showWarning,
        showInfo,
        confirmDelete,
        confirmAction
    };
}
