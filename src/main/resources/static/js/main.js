// StarShop Main JavaScript
document.addEventListener('DOMContentLoaded', function() {
    
    // Initialize AOS (Animate On Scroll) first
    initializeAOS();
    
    // Initialize all components
    initializeHeader();
    initializeSearch();
    initializeSearchToggle();
    initializeMobileMenu();
    initializeScrollEffects();
    initializeNewsletterForm();
    initializeTooltips();
    initializeAnimations();
    initializeCartActions();
    
    // Loading state management
    document.body.classList.add('loaded');
    
});

const CONFIG = {
    ANIMATION_DURATION: 300,
    TOAST_DURATION: 3000
};

// AOS (Animate On Scroll) initialization
function initializeAOS() {
    // Wait for AOS library to load (lazy loaded in main.html)
    const waitForAOS = setInterval(function() {
        if (typeof AOS !== 'undefined') {
            clearInterval(waitForAOS);
            
            // Detect device capabilities
            const isMobile = window.innerWidth < 768;
            const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
            
            // Skip AOS if user prefers reduced motion
            if (prefersReducedMotion) {
                return;
            }
            
            AOS.init({
                // Global settings - optimized for performance
                duration: isMobile ? 400 : 600,   // Faster on mobile
                easing: 'ease-out-cubic',         // Smooth natural easing
                once: true,                       // Animation happens only once (better performance)
                mirror: false,                    // Don't re-animate on scroll up
                anchorPlacement: 'top-bottom',    // When element triggers animation
                
                // Performance settings
                disable: false,                   // Don't disable, just simplify on mobile via CSS
                startEvent: 'DOMContentLoaded',   
                initClassName: 'aos-init',        
                animatedClassName: 'aos-animate', 
                useClassNames: false,             
                disableMutationObserver: isMobile, // Disable observer on mobile
                debounceDelay: isMobile ? 100 : 50, 
                throttleDelay: isMobile ? 150 : 99, 
                
                // Responsive settings
                offset: isMobile ? 50 : 100,      // Trigger earlier on mobile
                delay: 0,                         
            });
            
            // Refresh AOS on window resize (throttled)
            let resizeTimeout;
            window.addEventListener('resize', function() {
                clearTimeout(resizeTimeout);
                resizeTimeout = setTimeout(function() {
                    AOS.refresh();
                }, 250);
            });
        }
    }, 100); // Check every 100ms
    
    // Safety timeout: stop checking after 5 seconds
    setTimeout(function() {
        clearInterval(waitForAOS);
    }, 5000);
}


// Simple Header functionality - No effects
function initializeHeader() {
    const header = document.querySelector('.header-wrapper');
    
    if (!header) return;
    
    // Fix dropdown positioning issues
    fixDropdownPositioning();
}

// Fix dropdown positioning issues
function fixDropdownPositioning() {
    // Fix Bootstrap dropdown positioning for header dropdowns
    document.addEventListener('shown.bs.dropdown', function(event) {
        const dropdown = event.target.closest('.dropdown');
        if (dropdown && dropdown.closest('.header-wrapper')) {
            const menu = dropdown.querySelector('.dropdown-menu');
            if (menu) {
                // Add a class to force our CSS overrides
                menu.classList.add('header-dropdown-fixed');
                
                // Force reflow to ensure positioning works
                menu.offsetHeight;
                
                // Double-check positioning after a short delay
                setTimeout(() => {
                    if (menu.classList.contains('show')) {
                        menu.style.transform = 'none';
                        menu.style.position = 'absolute';
                        menu.style.top = '100%';
                        menu.style.right = '0';
                        menu.style.left = 'auto';
                        menu.style.zIndex = '1060';
                        menu.style.overflow = 'visible';
                    }
                }, 10);
            }
        }
    });
    
    // Also fix on hide to reset any Bootstrap positioning
    document.addEventListener('hidden.bs.dropdown', function(event) {
        const dropdown = event.target.closest('.dropdown');
        if (dropdown && dropdown.closest('.header-wrapper')) {
            const menu = dropdown.querySelector('.dropdown-menu');
            if (menu) {
                // Remove our fix class
                menu.classList.remove('header-dropdown-fixed');
            }
        }
    });
}

// Search functionality
function initializeSearch() {
    const searchInput = document.querySelector('.search-input');
    const searchSuggestions = document.getElementById('searchSuggestions');
    
    if (!searchInput || !searchSuggestions) return;
    
    let searchTimeout;
    
    // Search suggestions
    searchInput.addEventListener('input', function() {
        const query = this.value.trim();
        
        clearTimeout(searchTimeout);
        
        if (query.length >= 2) {
            searchTimeout = setTimeout(() => {
                fetchSearchSuggestions(query);
            }, 300);
        } else {
            hideSuggestions();
        }
    });
    
    // Hide suggestions when clicking outside
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.search-bar')) {
            hideSuggestions();
        }
    });
    
    // Keyboard navigation for suggestions
    searchInput.addEventListener('keydown', function(e) {
        const suggestions = searchSuggestions.querySelectorAll('.suggestion-item');
        let activeIndex = Array.from(suggestions).findIndex(item => item.classList.contains('active'));
        
        switch(e.key) {
            case 'ArrowDown':
                e.preventDefault();
                activeIndex = Math.min(activeIndex + 1, suggestions.length - 1);
                updateActiveSuggestion(suggestions, activeIndex);
                break;
                
            case 'ArrowUp':
                e.preventDefault();
                activeIndex = Math.max(activeIndex - 1, -1);
                updateActiveSuggestion(suggestions, activeIndex);
                break;
                
            case 'Enter':
                const activeSuggestion = suggestions[activeIndex];
                if (activeSuggestion) {
                    e.preventDefault();
                    window.location.href = activeSuggestion.href;
                }
                break;
                
            case 'Escape':
                hideSuggestions();
                searchInput.blur();
                break;
        }
    });
    
    function fetchSearchSuggestions(query) {
        // Mock search suggestions - replace with actual API call
        const mockSuggestions = [
            { title: 'Hoa hồng đỏ', url: '/products/hoa-hong-do', category: 'Hoa tình yêu' },
            { title: 'Hoa sinh nhật', url: '/categories/sinh-nhat', category: 'Danh mục' },
            { title: 'Bukê cưới', url: '/products/buke-cuoi', category: 'Hoa cưới' },
            { title: 'Hoa tulip', url: '/products/hoa-tulip', category: 'Hoa nhập khẩu' }
        ];
        
        const filteredSuggestions = mockSuggestions.filter(item => 
            item.title.toLowerCase().includes(query.toLowerCase())
        );
        
        displaySuggestions(filteredSuggestions);
    }
    
    function displaySuggestions(suggestions) {
        if (suggestions.length === 0) {
            hideSuggestions();
            return;
        }
        
        const html = suggestions.map(suggestion => `
            <a href="${suggestion.url}" class="suggestion-item">
                <div class="suggestion-content">
                    <svg class="suggestion-icon w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M9 3.5a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11ZM2 9a7 7 0 1 1 12.452 4.391l3.328 3.329a.75.75 0 1 1-1.06 1.06l-3.329-3.328A7 7 0 0 1 2 9Z" clip-rule="evenodd" />
                    </svg>
                    <div class="suggestion-text">
                        <div class="suggestion-title">${suggestion.title}</div>
                        <div class="suggestion-category">${suggestion.category}</div>
                    </div>
                </div>
            </a>
        `).join('');
        
        searchSuggestions.innerHTML = html;
        searchSuggestions.classList.add('show');
    }
    
    function hideSuggestions() {
        searchSuggestions.classList.remove('show');
        searchSuggestions.innerHTML = '';
    }
    
    function updateActiveSuggestion(suggestions, activeIndex) {
        suggestions.forEach((item, index) => {
            item.classList.toggle('active', index === activeIndex);
        });
        
        if (activeIndex >= 0 && suggestions[activeIndex]) {
            suggestions[activeIndex].scrollIntoView({ block: 'nearest' });
        }
    }
}

// Mobile menu functionality
function initializeMobileMenu() {
    const mobileDropdownToggles = document.querySelectorAll('.mobile-dropdown-toggle');
    
    mobileDropdownToggles.forEach(toggle => {
        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            
            const submenu = this.nextElementSibling;
            const icon = this.querySelector('.bi-chevron-down');
            
            if (submenu) {
                submenu.classList.toggle('show');
                icon.classList.toggle('rotate');
                this.classList.toggle('active');
            }
        });
    });
    
    // Close mobile menu when clicking on links
    const mobileNavLinks = document.querySelectorAll('.mobile-nav-link:not(.mobile-dropdown-toggle)');
    const offcanvas = document.getElementById('mobileMenu');
    
    mobileNavLinks.forEach(link => {
        link.addEventListener('click', function() {
            if (offcanvas) {
                const bsOffcanvas = bootstrap.Offcanvas.getInstance(offcanvas);
                if (bsOffcanvas) {
                    bsOffcanvas.hide();
                }
            }
        });
    });
}

// Scroll effects
function initializeScrollEffects() {
    // Smooth scroll for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
    
    // Parallax effect for hero sections
    const heroSections = document.querySelectorAll('.hero-section');
    
    if (heroSections.length > 0) {
        window.addEventListener('scroll', function() {
            const scrolled = window.pageYOffset;
            
            heroSections.forEach(hero => {
                const rate = scrolled * -0.5;
                hero.style.transform = `translateY(${rate}px)`;
            });
        });
    }
    
    // Fade in animation on scroll
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver(function(entries) {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('animate-in');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);
    
    document.querySelectorAll('.animate-on-scroll').forEach(el => {
        observer.observe(el);
    });
}

// Newsletter form
function initializeNewsletterForm() {
    const newsletterForms = document.querySelectorAll('.newsletter-form');
    
    newsletterForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const email = this.querySelector('input[name="email"]').value;
            const button = this.querySelector('.btn-newsletter');
            const originalHTML = button.innerHTML;
            
            // Validate email
            if (!validateEmail(email)) {
                showToast('Vui lòng nhập địa chỉ email hợp lệ', 'error');
                return;
            }
            
            // Show loading state
            button.innerHTML = '<svg class="w-5 h-5 inline-block animate-spin" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M15.312 11.424a5.5 5.5 0 0 1-9.201 2.466l-.312-.311h2.433a.75.75 0 0 0 0-1.5H3.989a.75.75 0 0 0-.75.75v4.242a.75.75 0 0 0 1.5 0v-2.43l.31.31a7 7 0 0 0 11.712-3.138.75.75 0 0 0-1.449-.39Zm1.23-3.723a.75.75 0 0 0 .219-.53V2.929a.75.75 0 0 0-1.5 0V5.36l-.31-.31A7 7 0 0 0 3.239 8.188a.75.75 0 1 0 1.448.389A5.5 5.5 0 0 1 13.89 6.11l.311.31h-2.432a.75.75 0 0 0 0 1.5h4.243a.75.75 0 0 0 .53-.219Z" clip-rule="evenodd" /></svg>';
            button.disabled = true;
            
            // Simulate API call
            setTimeout(() => {
                // Success
                showToast('Cảm ơn bạn đã đăng ký nhận tin!', 'success');
                this.reset();
                
                // Reset button
                button.innerHTML = originalHTML;
                button.disabled = false;
            }, 1500);
        });
    });
}

// Initialize tooltips
function initializeTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function(tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

// Initialize animations
function initializeAnimations() {
    // Heartbeat animation for logo
    const heartbeatElements = document.querySelectorAll('.heartbeat');
    
    heartbeatElements.forEach(element => {
        // Random delay for natural effect
        const delay = Math.random() * 2000;
        setTimeout(() => {
            element.style.animationDelay = `${delay}ms`;
        }, 100);
    });
    
    // Floating petals animation
    const petals = document.querySelectorAll('.petal');
    
    petals.forEach((petal, index) => {
        // Random properties for each petal
        const duration = 10 + Math.random() * 10; // 10-20s
        const delay = Math.random() * 5; // 0-5s delay
        const xOffset = Math.random() * 100; // Random horizontal position
        
        petal.style.animationDuration = `${duration}s`;
        petal.style.animationDelay = `${delay}s`;
        petal.style.left = `${xOffset}%`;
    });
}

// Cart actions
function initializeCartActions() {
    // Add to cart buttons
    document.addEventListener('click', function(e) {
        if (e.target.matches('.btn-add-to-cart') || e.target.closest('.btn-add-to-cart')) {
            e.preventDefault();
            
            const button = e.target.closest('.btn-add-to-cart');
            const productId = button.dataset.productId;
            
            addToCart(button);
        }
        
        // Wishlist buttons
        if (e.target.matches('.btn-wishlist') || e.target.closest('.btn-wishlist')) {
            e.preventDefault();
            
            const button = e.target.closest('.btn-wishlist');
            toggleWishlist(button);
        }
    });
}


// thêm giỏ hàng và thêm yêu thích
function addToCart(button) {
    const productId = button.dataset.productId;
    const quantity = document.getElementById('quantity')?.value || 1;
    
    if (!productId) {
        const errorMsg = typeof window.languageSwitcher !== 'undefined' && window.languageSwitcher ? 
            window.languageSwitcher.translate('add-to-cart-error') : 'Không thể thêm sản phẩm vào giỏ hàng';
        showToast(errorMsg, 'error');
        return;
    }

    const originalHTML = button.innerHTML;
    
    // Get translations if available
    const addingText = typeof window.languageSwitcher !== 'undefined' && window.languageSwitcher ? 
        window.languageSwitcher.translate('adding') : 'Đang thêm...';
    const addedText = typeof window.languageSwitcher !== 'undefined' && window.languageSwitcher ? 
        window.languageSwitcher.translate('added') : 'Đã thêm';
    
    // Show loading state
    button.innerHTML = '<svg class="w-5 h-5 inline-block" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm.75-13a.75.75 0 0 0-1.5 0v5c0 .414.336.75.75.75h4a.75.75 0 0 0 0-1.5h-3.25V5Z" clip-rule="evenodd" /></svg> ' + addingText;
    button.disabled = true;

    // Get CSRF token
    const csrfToken = getCsrfToken();
    const csrfHeaderElement = document.querySelector('meta[name="_csrf_header"]');
    const csrfHeader = csrfHeaderElement ? csrfHeaderElement.getAttribute('content') : 'X-CSRF-TOKEN';
    

    // API call to add to cart
    const headers = {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest'
    };
    
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch('/api/cart/add', {
        method: 'POST',
        headers: headers,
        credentials: 'same-origin',
        body: JSON.stringify({ 
            productId: parseInt(productId), 
            quantity: parseInt(quantity) 
        })
    })
    .then(response => {
        if (response.status === 401) {
            showToast('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng', 'warning');
            button.innerHTML = originalHTML;
            button.disabled = false;
            return;
        }
        
        if (response.status === 403) {
            showToast('Lỗi bảo mật: Vui lòng refresh trang và thử lại', 'error');
            button.innerHTML = originalHTML;
            button.disabled = false;
            return;
        }
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        return response.json();
    })
    .then(data => {
        if (!data) {
            return; // Already handled 401 case
        }
        
        // Always try to refresh cart count and show success
        fetch('/api/cart/count', { credentials: 'same-origin' })
            .then(response => response.json())
            .then(countData => {
                const totalItems = countData.data !== undefined ? countData.data : countData;
                if (typeof updateCartCount === 'function') {
                    updateCartCount(totalItems);
                }
                
                // Show success message only after cart count is updated
                const successMsg = typeof window.languageSwitcher !== 'undefined' && window.languageSwitcher ? 
                    window.languageSwitcher.translate('add-to-cart-success') : 'Đã thêm sản phẩm vào giỏ hàng';
                showToast(successMsg, 'success');
            })
            .catch(() => {
                // Still show success even if count refresh fails
                const successMsg = typeof window.languageSwitcher !== 'undefined' && window.languageSwitcher ? 
                    window.languageSwitcher.translate('add-to-cart-success') : 'Đã thêm sản phẩm vào giỏ hàng';
                showToast(successMsg, 'success');
            });
        
        // Update button state with translated text
        const addedText = typeof window.languageSwitcher !== 'undefined' && window.languageSwitcher ? 
            window.languageSwitcher.translate('added') : 'Đã thêm';
        button.innerHTML = '<svg class="w-5 h-5 inline-block" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z" clip-rule="evenodd" /></svg> ' + addedText + '!';
        
        // Reset button after delay
        setTimeout(() => {
            button.innerHTML = originalHTML;
            button.disabled = false;
        }, 2000);
    })
    .catch(error => {
        console.error('Error adding to cart:', error);
        button.innerHTML = originalHTML;
        button.disabled = false;
        showToast('Có lỗi xảy ra khi thêm sản phẩm vào giỏ hàng', 'error');
    });
}

function toggleWishlist(button) {
    const productId = button.dataset.productId;
    
    if (!productId) {
        showToast('Không thể thêm sản phẩm vào danh sách yêu thích', 'error');
        return;
    }
    
    // Disable button to prevent multiple clicks
    button.disabled = true;
    const originalHTML = button.innerHTML;
    
    // Show loading state
    button.innerHTML = '<svg class="w-5 h-5 inline-block animate-spin" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm.75-13a.75.75 0 0 0-1.5 0v5c0 .414.336.75.75.75h4a.75.75 0 0 0 0-1.5h-3.25V5Z" clip-rule="evenodd" /></svg>';
    
    // Get CSRF token
    const csrfToken = getCsrfToken();
    const csrfHeaderElement = document.querySelector('meta[name="_csrf_header"]');
    const csrfHeader = csrfHeaderElement ? csrfHeaderElement.getAttribute('content') : 'X-CSRF-TOKEN';
    
    const headers = {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest'
    };
    
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    
    // API call to toggle wishlist
    fetch('/api/wishlist/toggle', {
        method: 'POST',
        headers: headers,
        credentials: 'same-origin',
        body: JSON.stringify({ productId: parseInt(productId) })
    })
    .then(response => {
        if (response.status === 401) {
            showToast('Vui lòng đăng nhập để sử dụng tính năng yêu thích', 'warning');
            button.innerHTML = originalHTML;
            button.disabled = false;
            return;
        }
        
        if (response.status === 403) {
            showToast('Lỗi bảo mật: Vui lòng refresh trang và thử lại', 'error');
            button.innerHTML = originalHTML;
            button.disabled = false;
            return;
        }
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        return response.json();
    })
    .then(data => {
        // Early return from error handling above
        if (!data) return;
        
        // Fix: Check data.success OR (data.data exists and no error)
        if (data && (data.success || (data.data && !data.error))) {
            // Update UI based on server response
            const isInWishlist = data.data.isFavorite || data.data.isInWishlist;
            
            if (isInWishlist) {
                button.classList.add('active');
                // Heroicons: Solid heart - Màu đỏ hồng như trong home.html
                button.innerHTML = '<svg class="w-5 h-5 text-rose-500" fill="currentColor" viewBox="0 0 24 24"><path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3A5.5 5.5 0 0112 5.052 5.5 5.5 0 0116.313 3c2.973 0 5.437 2.322 5.437 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 01-4.244 3.17 15.247 15.247 0 01-.383.219l-.022.012-.007.004-.003.001a.752.752 0 01-.704 0l-.003-.001z"/></svg>';
            } else {
                button.classList.remove('active');
                // Heroicons: Outline heart - Màu xám với hover hồng như trong home.html
                button.innerHTML = '<svg class="w-5 h-5 text-gray-600 hover:text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/></svg>';
            }
            
            // ⭐ FIX: Đọc đúng tên field từ API response
            // API trả về: userWishlistCount (tổng số sản phẩm yêu thích của user)
            const wishlistCount = data.data.userWishlistCount;
            
            if (wishlistCount !== undefined && wishlistCount !== null) {
                updateWishlistCount(wishlistCount);
            } else {
                // Fallback: fetch from list API
                fetch('/api/wishlist/list', { credentials: 'same-origin' })
                    .then(response => response.json())
                    .then(countData => {
                        let count = 0;
                        if (countData && countData.success && countData.data) {
                            count = Array.isArray(countData.data) ? countData.data.length : 0;
                        }
                        updateWishlistCount(count);
                    })
                    .catch(error => {
                        console.error('Error fetching wishlist count:', error);
                    });
            }
            
            // Show success message
            if (isInWishlist) {
                showToast('Đã thêm vào danh sách yêu thích', 'success');
            } else {
                showToast('Đã xóa khỏi danh sách yêu thích', 'success');
            }
        } else {
            button.innerHTML = originalHTML;
            const errorMessage = (data && data.error) || (data && data.data && data.data.message) || 'Có lỗi xảy ra';
            showToast(errorMessage, 'error');
        }
    })
    .catch(error => {
        button.innerHTML = originalHTML;
        showToast('Có lỗi xảy ra khi thực hiện yêu cầu', 'error');
    })
    .finally(() => {
        button.disabled = false;
    });
}





// Utility functions
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}
// ========================================
// NOTIFICATION FUNCTIONS
// ========================================
// NOTE: showToast() is now provided by /js/notifications.js
// This is kept as fallback for backward compatibility
// ========================================
function showToast(message, type = 'success') {
    // Check if notifications.js is loaded
    if (typeof window.Notifications !== 'undefined') {
        window.Notifications.showToast(message, type);
        return;
    }
    
    // Fallback to inline implementation
    if (typeof Swal === 'undefined') {
        alert(message);
        return;
    }
    
    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        didOpen: (toast) => {
            toast.addEventListener('mouseenter', Swal.stopTimer);
            toast.addEventListener('mouseleave', Swal.resumeTimer);
        }
    });

    Toast.fire({
        icon: type,
        title: message
    });
}



function updateCartCount(count) {
    const cartCountElements = document.querySelectorAll('.cart-count');
    
    cartCountElements.forEach(element => {
        element.textContent = count;
        if (count > 0) {
            element.classList.remove('hidden');
            element.style.display = 'flex';
            
            // Add animation
            element.style.transform = 'scale(1.2)';
            setTimeout(() => {
                element.style.transform = 'scale(1)';
            }, 200);
        } else {
            element.classList.add('hidden');
            element.style.display = 'none';
        }
    });
}

function updateWishlistCount(count) {
    // const wishlistCountElements = document.querySelectorAll('.wishlist-count');
    // wishlistCountElements.forEach(element => {
    //     element.textContent = count;
    //     if (count > 0) {
    //         element.classList.remove('hidden');
    //         element.style.display = 'flex';
    //     } else {
    //         element.classList.add('hidden');
    //         element.style.display = 'none';
    //     }
    // });


    const wishlistCountElements = document.querySelectorAll('.wishlist-count');

    // Đảm bảo count là một số
    const newCount = Number(count);

    wishlistCountElements.forEach(element => {
        element.textContent = newCount;
        if (newCount > 0) {
            // Hiển thị số nếu lớn hơn 0
            element.classList.remove('hidden');
            element.style.display = 'flex'; // Hoặc 'inline-block' tùy theo CSS của bạn

            // Thêm hiệu ứng animation nhỏ để thu hút sự chú ý
            element.style.transform = 'scale(1.2)';
            setTimeout(() => {
                element.style.transform = 'scale(1)';
            }, 150);

        } else {
            // Ẩn số đi nếu bằng 0
            element.classList.add('hidden');
            element.style.display = 'none';
        }
    });
}

// Make functions globally accessible immediately after definition
window.updateCartCount = updateCartCount;
window.updateWishlistCount = updateWishlistCount;
window.addToCartFromMain = addToCart;

function getCsrfToken() {
    // Try to get CSRF token from cookie first (Spring Security default)
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'XSRF-TOKEN') {
            return decodeURIComponent(value);
        }
    }
    
    // Fallback to meta tag
    const token = document.querySelector('meta[name="_csrf"]');
    return token ? token.getAttribute('content') : '';
}

// CSS animations
const style = document.createElement('style');
style.textContent = `
    .spin {
        animation: spin 1s linear infinite;
    }
    
    @keyframes spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
    }
    
    .animate-on-scroll {
        opacity: 0;
        transform: translateY(30px);
        transition: all 0.6s ease-out;
    }
    
    .animate-on-scroll.animate-in {
        opacity: 1;
        transform: translateY(0);
    }
    
    .cart-count, .wishlist-count {
        transition: transform 0.2s ease-in-out;
    }
    
    .header-wrapper.scrolled {
        box-shadow: 0 2px 20px rgba(0,0,0,0.1);
        backdrop-filter: blur(10px);
    }
    
    .header-wrapper.header-hidden {
        transform: translateY(-100%);
    }
    
    .mobile-submenu {
        max-height: 0;
        overflow: hidden;
        transition: max-height 0.3s ease;
        padding-left: 1rem;
    }
    
    .mobile-submenu.show {
        max-height: 300px;
    }
    
    .mobile-dropdown-toggle .bi-chevron-down {
        transition: transform 0.3s ease;
    }
    
    .mobile-dropdown-toggle .bi-chevron-down.rotate {
        transform: rotate(180deg);
    }
    
    .suggestion-item {
        display: block;
        padding: 0.75rem 1rem;
        text-decoration: none;
        color: var(--text-primary);
        border-bottom: 1px solid var(--border-light);
        transition: background-color 0.2s ease;
    }
    
    .suggestion-item:hover,
    .suggestion-item.active {
        background-color: var(--bg-light);
        color: var(--primary);
    }
    
    .suggestion-content {
        display: flex;
        align-items: center;
    }
    
    .suggestion-icon {
        margin-right: 0.75rem;
        color: var(--text-secondary);
    }
    
    .suggestion-title {
        font-weight: 500;
        margin-bottom: 0.25rem;
    }
    
    .suggestion-category {
        font-size: 0.8rem;
        color: var(--text-secondary);
    }
    
    .search-suggestions {
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        background: white;
        border: 1px solid var(--border);
        border-top: none;
        border-radius: 0 0 8px 8px;
        box-shadow: var(--shadow-lg);
        z-index: 1000;
        max-height: 300px;
        overflow-y: auto;
        display: none;
    }
    
    .search-suggestions.show {
        display: block;
    }
    
    .toast-success .toast-header {
        background-color: var(--success-light);
        border-bottom: 1px solid var(--success);
    }
    
    .toast-error .toast-header {
        background-color: var(--danger-light);
        border-bottom: 1px solid var(--danger);
    }
    
    .toast-warning .toast-header {
        background-color: var(--warning-light);
        border-bottom: 1px solid var(--warning);
    }
    
    .toast-info .toast-header {
        background-color: var(--info-light);
        border-bottom: 1px solid var(--info);
    }
`;

document.head.appendChild(style);

// Search Toggle Functionality
function initializeSearchToggle() {
    const searchToggleBtn = document.getElementById('searchToggleBtn') || document.querySelector('.search-toggle-btn');
    const searchCollapse = document.getElementById('searchCollapse');
    
    if (!searchToggleBtn || !searchCollapse) {
        return;
    }
    
    // Add event listener as backup to onclick
    searchToggleBtn.addEventListener('click', function(e) {
        e.preventDefault();
        toggleSearch();
    });
    
    // Close search when clicking outside
    document.addEventListener('click', function(e) {
        const searchBox = document.getElementById('searchCollapse');
        const toggleBtn = document.getElementById('searchToggleBtn');
        
        if (searchBox && toggleBtn && searchBox.classList.contains('show')) {
            // Check if click is outside both search box and toggle button
            if (!searchBox.contains(e.target) && !toggleBtn.contains(e.target)) {
                hideSearch();
            }
        }
    });
    
    // Close search on escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            const searchBox = document.getElementById('searchCollapse');
            if (searchBox && searchBox.classList.contains('show')) {
                hideSearch();
            }
        }
    });
}

// Enhanced Search Toggle Function
function toggleSearch() {
    const searchBox = document.getElementById('searchCollapse');
    const toggleBtn = document.getElementById('searchToggleBtn');
    
    if (!searchBox) {
        return;
    }
    
    const isVisible = searchBox.classList.contains('show');
    
    if (isVisible) {
        // Hide search
        hideSearch();
    } else {
        // Show search
        showSearch();
    }
}

// Show Search Function
function showSearch() {
    const searchBox = document.getElementById('searchCollapse');
    const toggleBtn = document.getElementById('searchToggleBtn');
    
    if (searchBox) {
        searchBox.classList.add('show');
        
        // Focus on input after animation
        setTimeout(() => {
            const input = searchBox.querySelector('.search-input');
            if (input) {
                input.focus();
                input.select(); // Select all text if any
            }
        }, 300);
    }
    
    if (toggleBtn) {
        toggleBtn.classList.add('active');
    }
}

// Hide Search Function
function hideSearch() {
    const searchBox = document.getElementById('searchCollapse');
    const toggleBtn = document.getElementById('searchToggleBtn');
    
    if (searchBox) {
        searchBox.classList.remove('show');
        
        // Blur input
        const input = searchBox.querySelector('.search-input');
        if (input) {
            input.blur();
        }
    }
    
    if (toggleBtn) {
        toggleBtn.classList.remove('active');
    }
}

// Close search function - specifically for X button
function closeSearch() {
    hideSearch();
}

// Test function - call this from browser console to test
function testSearchToggle() {
    const searchCollapse = document.getElementById('searchCollapse');
    const searchToggleBtn = document.getElementById('searchToggleBtn');
    
    if (searchCollapse) {
        searchCollapse.classList.add('show');
    }
}

// Load initial cart count
function loadCartCount() {
    fetch('/api/cart/count', {
        method: 'GET',
        credentials: 'same-origin'
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        }
        return null;
    })
    .then(data => {
        if (data && data.success && data.data !== undefined) {
            updateCartCount(data.data);
        }
    })
    .catch(error => {
        // Silent fail for cart count
    });
}

// Load cart count on page load
document.addEventListener('DOMContentLoaded', function() {
    loadCartCount();
});

// ================================
// MoMo Payment Gateway Error Handling
// ================================

// Handle broken MoMo logo images gracefully
function handleMoMoImageErrors() {
    // Find all images from MoMo test environment
    const momoImages = document.querySelectorAll('img[src*="test-payment.momo.vn"]');
    
    momoImages.forEach(img => {
        // Add error handler for broken images
        img.addEventListener('error', function() {
            console.warn('MoMo image failed to load:', this.src);
            
            // Hide the broken image
            this.style.display = 'none';
            
            // Create fallback element
            const fallback = document.createElement('div');
            fallback.className = 'momo-fallback';
            fallback.innerHTML = '💳';
            fallback.style.cssText = `
                display: inline-flex;
                align-items: center;
                justify-content: center;
                width: 40px;
                height: 40px;
                background: linear-gradient(135deg, #ec4899, #be185d);
                border-radius: 8px;
                color: white;
                font-size: 20px;
                margin: 0 8px;
            `;
            
            // Insert fallback after the broken image
            this.parentNode.insertBefore(fallback, this.nextSibling);
        });
        
        // Add load handler for successful images
        img.addEventListener('load', function() {
            console.log('MoMo image loaded successfully:', this.src);
        });
    });
}

// Initialize MoMo error handling when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    handleMoMoImageErrors();
});

// Also handle dynamically loaded images (for payment pages)
const observer = new MutationObserver(function(mutations) {
    mutations.forEach(function(mutation) {
        if (mutation.type === 'childList') {
            mutation.addedNodes.forEach(function(node) {
                if (node.nodeType === 1) { // Element node
                    if (node.tagName === 'IMG' && node.src && node.src.includes('test-payment.momo.vn')) {
                        handleMoMoImageErrors();
                    }
                }
            });
        }
    });
});

// Start observing
observer.observe(document.body, {
    childList: true,
    subtree: true
});

