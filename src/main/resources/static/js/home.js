// Home Page JavaScript - Simplified & Clean
document.addEventListener('DOMContentLoaded', function() {
    initializeFloatingPetals();
    safeCallName('initializeProductActions');
    safeCallName('initializeScrollAnimations');
    initializeAdPopupAlways();
});

// Floating petals - simplified
function initializeFloatingPetals() {
    const petals = document.querySelectorAll('.petal');
    
    // Add random delays to make animation more natural
    petals.forEach((petal, index) => {
        const delay = Math.random() * 15000; // 0-15 seconds
        petal.style.animationDelay = `${delay}ms`;
    });
}

// Page performance monitoring
window.addEventListener('load', function() {
    document.body.classList.add('page-loaded');
    
    // Performance metrics
    if (window.performance && window.performance.timing) {
        const loadTime = window.performance.timing.loadEventEnd - window.performance.timing.navigationStart;
    }
});

// Error handling
window.addEventListener('error', function(e) {
    // Handle JavaScript errors
});

// Ad popup logic - show on every visit
function initializeAdPopupAlways() {
    try {
        const overlay = document.getElementById('ad-popup');
        if (!overlay) return;

        const closeBtn = document.getElementById('ad-popup-close');

        const open = () => {
            overlay.classList.add('show');
            overlay.classList.remove('hidden');
            document.body.style.overflow = 'hidden';
        };
        const close = () => {
            overlay.classList.remove('show');
            overlay.classList.add('hidden');
            document.body.style.overflow = '';
        };

        // Defer a bit to avoid clashing with initial layout
        setTimeout(open, 500);

        // Fallback: if not visible yet after 1.2s, force open again
        setTimeout(() => {
            const isHidden = overlay.classList.contains('hidden');
            const computed = window.getComputedStyle(overlay);
            if (isHidden || computed.display === 'none') {
                open();
            }
        }, 1200);

        // Allow closing via the top-right close button
        [closeBtn].forEach(el => {
            if (!el) return;
            el.addEventListener('click', close);
        });

        // Also close when clicking the dim overlay area
        overlay.addEventListener('click', function(e) {
            const dialog = e.currentTarget.querySelector('.ad-dialog');
            if (!dialog) return;
            const clickInsideDialog = dialog.contains(e.target);
            if (!clickInsideDialog) {
                close();
            }
        });

        // Disable closing by clicking backdrop or pressing Esc
    } catch (err) {
        // Failed to init ad popup
    }
}

// Utility: safe wrapper for optional initializers
function safeCall(fn) {
    try { if (typeof fn === 'function') fn(); } catch (e) { /* Safe call error */ }
}

// Utility: safe call by global name without ReferenceError for undeclared identifiers
function safeCallName(functionName) {
    try {
        const maybeFn = (typeof window !== 'undefined') ? window[functionName] : undefined;
        if (typeof maybeFn === 'function') {
            maybeFn();
        }
    } catch (e) {
        // Safe call by name error
    }
}

// Product actions - simplified
// function initializeProductActions() {
//     // Quick add buttons
//     const quickAddBtns = document.querySelectorAll('.btn-quick-add');
//     quickAddBtns.forEach(btn => {
//         btn.addEventListener('click', function(e) {
//             e.preventDefault();
//             const productId = this.dataset.productId;
//             handleQuickAdd(productId, this);
//         });
//     });
// }

// Handle quick add to cart
// function handleQuickAdd(productId, button) {
//     const originalText = button.innerHTML;
    
//     // Show loading state
//     button.innerHTML = '<svg class="w-5 h-5 inline-block animate-spin" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M15.312 11.424a5.5 5.5 0 0 1-9.201 2.466l-.312-.311h2.433a.75.75 0 0 0 0-1.5H3.989a.75.75 0 0 0-.75.75v4.242a.75.75 0 0 0 1.5 0v-2.43l.31.31a7 7 0 0 0 11.712-3.138.75.75 0 0 0-1.449-.39Zm1.23-3.723a.75.75 0 0 0 .219-.53V2.929a.75.75 0 0 0-1.5 0V5.36l-.31-.31A7 7 0 0 0 3.239 8.188a.75.75 0 1 0 1.448.389A5.5 5.5 0 0 1 13.89 6.11l.311.31h-2.432a.75.75 0 0 0 0 1.5h4.243a.75.75 0 0 0 .53-.219Z" clip-rule="evenodd" /></svg> Đang thêm...';
//     button.disabled = true;
    
//     // Simulate API call
//     setTimeout(() => {
//         // Success state
//         button.innerHTML = '<svg class="w-5 h-5 inline-block" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z" clip-rule="evenodd" /></svg> Đã thêm';
//         button.style.background = 'var(--success)';
        
//         // Show toast notification
//         showToast('Đã thêm sản phẩm vào giỏ hàng!', 'success');
        
//         // Reset button after 2 seconds
//         setTimeout(() => {
//             button.innerHTML = originalText;
//             button.style.background = '';
//             button.disabled = false;
//         }, 2000);
//     }, 1000);
// }

// Scroll animations - simplified
// function initializeScrollAnimations() {
//     // Intersection Observer for fade-in animations
//     const observerOptions = {
//         threshold: 0.1,
//         rootMargin: '0px 0px -50px 0px'
//     };
    
//     const observer = new IntersectionObserver(function(entries) {
//         entries.forEach(entry => {
//             if (entry.isIntersecting) {
//                 entry.target.style.opacity = '1';
//                 entry.target.style.transform = 'translateY(0)';
//             }
//         });
//     }, observerOptions);
    
//     // Observe elements that should animate in
//     const animateElements = document.querySelectorAll('.section-header, .feature-card, .category-card, .product-card, .testimonial-card');
//     animateElements.forEach(element => {
//         element.style.opacity = '0';
//         element.style.transform = 'translateY(20px)';
//         element.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
//         observer.observe(element);
//     });
// }

// Utility functions
// function showToast(message, type = 'success') {
//     // Create toast element
//     const toast = document.createElement('div');
//     toast.className = `toast-notification toast-${type}`;
//     toast.innerHTML = `
//         <div class="toast-content">
//             <svg class="w-5 h-5 inline-block" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
//                 ${type === 'success' ? '<path fill-rule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm3.857-9.809a.75.75 0 0 0-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 1 0-1.06 1.061l2.5 2.5a.75.75 0 0 0 1.137-.089l4-5.5Z" clip-rule="evenodd" />' : '<path fill-rule="evenodd" d="M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Zm-8-5a.75.75 0 0 1 .75.75v4.5a.75.75 0 0 1-1.5 0v-4.5A.75.75 0 0 1 10 5Zm0 10a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z" clip-rule="evenodd" />'}
//             </svg>
//             <span>${message}</span>
//         </div>
//     `;
    
//     // Add to page
//     document.body.appendChild(toast);
    
//     // Show toast
//     setTimeout(() => {
//         toast.classList.add('show');
//     }, 100);
    
//     // Hide and remove toast
//     setTimeout(() => {
//         toast.classList.remove('show');
//         setTimeout(() => {
//             document.body.removeChild(toast);
//         }, 300);
//     }, 3000);
// }