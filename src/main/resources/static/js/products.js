/**
 * Products Page JavaScript
 * Professional interactions for product listing and detail pages
 * Following rules.mdc specifications for client-side functionality
 */

(function () {
  "use strict";

  // ================================
  // GLOBAL VARIABLES & CONFIG
  // ================================

  const CONFIG = {
    DEBOUNCE_DELAY: 300,
    ANIMATION_DURATION: 300,
    TOAST_DURATION: 3000,
    MAX_QUANTITY: 99,
    MIN_QUANTITY: 1,
  };

  let searchTimeout = null;
  let isLoading = false;

  // ================================
  // UTILITY FUNCTIONS
  // ================================

  function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }

  // Helper function to safely set class on icon (SVG or regular element)
  function setIconClass(iconElement, className) {
    if (!iconElement) return;

    try {
      if (iconElement.tagName === "SVG") {
        iconElement.setAttribute("class", className);
      } else {
        iconElement.className = className;
      }
    } catch (error) {
      // Fallback: try setAttribute for all elements
      try {
        iconElement.setAttribute("class", className);
      } catch (fallbackError) {
        // Failed to set class
      }
    }
  }

  // Helper function to safely get class from icon
  function getIconClass(iconElement) {
    if (!iconElement) return "";

    if (iconElement.tagName === "SVG") {
      return iconElement.getAttribute("class") || "";
    } else {
      return iconElement.className || "";
    }
  }

  function setLoadingState(element, loading = true) {
    if (loading) {
      element.dataset.originalContent = element.innerHTML;
      element.innerHTML =
        '<svg class="w-5 h-5 inline-block" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm.75-13a.75.75 0 0 0-1.5 0v5c0 .414.336.75.75.75h4a.75.75 0 0 0 0-1.5h-3.25V5Z" clip-rule="evenodd" /></svg> Đang xử lý...';
      element.disabled = true;
    } else {
      element.innerHTML = element.dataset.originalContent || element.innerHTML;
      element.disabled = false;
      delete element.dataset.originalContent;
    }
  }

  function formatCurrency(amount) {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(amount);
  }

  function validateQuantity(value) {
    const quantity = parseInt(value) || CONFIG.MIN_QUANTITY;
    return Math.min(
      Math.max(quantity, CONFIG.MIN_QUANTITY),
      CONFIG.MAX_QUANTITY
    );
  }

  // (Search is initialized globally in main.js)

  // ================================
  // PRODUCT GRID FUNCTIONALITY
  // ================================

  function initializeProductGrid() {
    // View toggle functionality
    initializeViewToggle();

    // Note: Product actions (wishlist, cart) are handled by main.js via event delegation
  }

  function initializeViewToggle() {
    const viewToggleButtons = document.querySelectorAll(".btn-view-toggle");
    const productsGrid = document.getElementById("productsGrid");

    if (!viewToggleButtons.length || !productsGrid) return;

    viewToggleButtons.forEach((button) => {
      button.addEventListener("click", function () {
        const view = this.dataset.view;

        // Update active state
        viewToggleButtons.forEach((btn) => btn.classList.remove("active"));
        this.classList.add("active");

        // Update grid class with animation
        if (view === "list") {
          productsGrid.style.opacity = "1";
          setTimeout(() => {
            productsGrid.classList.add("products-list-view");
            productsGrid.style.opacity = "1";
          }, 150);
        } else {
          productsGrid.style.opacity = "1";
          setTimeout(() => {
            productsGrid.classList.remove("products-list-view");
            productsGrid.style.opacity = "1";
          }, 150);
        }

        // Save preference
        localStorage.setItem("productsView", view);

        // Track analytics
        if (typeof gtag !== "undefined") {
          gtag("event", "view_toggle", {
            view_type: view,
          });
        }
      });
    });

    // Load saved view preference
    const savedView = localStorage.getItem("productsView");
    if (savedView === "list") {
      const listButton = document.querySelector('[data-view="list"]');
      if (listButton) listButton.click();
    }
  }
  // SSR renders wishlist state; no client hydration needed


  // ================================
  // PRODUCT DETAIL PAGE
  // ================================

  function initializeProductDetail() {
    initializeQuantityControls();
    initializeImageGallery();
    initializeProductTabs();
    initializeProductActions();
  }

  function initializeQuantityControls() {
    const quantityInput = document.getElementById("quantity");
    const decreaseBtn = document.querySelector(".btn-quantity-decrease");
    const increaseBtn = document.querySelector(".btn-quantity-increase");

    if (!quantityInput) return;

    // Decrease quantity
    if (decreaseBtn) {
      decreaseBtn.addEventListener("click", function () {
        const currentValue =
          parseInt(quantityInput.value) || CONFIG.MIN_QUANTITY;
        if (currentValue > CONFIG.MIN_QUANTITY) {
          quantityInput.value = currentValue - 1;
          updateQuantityDisplay();
        }
      });
    }

    // Increase quantity
    if (increaseBtn) {
      increaseBtn.addEventListener("click", function () {
        const currentValue =
          parseInt(quantityInput.value) || CONFIG.MIN_QUANTITY;
        if (currentValue < CONFIG.MAX_QUANTITY) {
          quantityInput.value = currentValue + 1;
          updateQuantityDisplay();
        }
      });
    }

    // Direct input validation
    quantityInput.addEventListener("input", function () {
      this.value = validateQuantity(this.value);
      updateQuantityDisplay();
    });

    quantityInput.addEventListener("blur", function () {
      this.value = validateQuantity(this.value);
      updateQuantityDisplay();
    });
  }

  function updateQuantityDisplay() {
    // Update any quantity-dependent displays
    const quantity = parseInt(document.getElementById("quantity")?.value) || 1;

    // Update total price if shown
    const priceElement = document.querySelector(".current-price");
    if (priceElement && priceElement.dataset.unitPrice) {
      const unitPrice = parseFloat(priceElement.dataset.unitPrice);
      const totalPrice = unitPrice * quantity;
      // Update display if needed
    }
  }

  function initializeImageGallery() {
    const mainImage = document.getElementById("mainProductImage");
    const thumbnails = document.querySelectorAll(".thumbnail-image");
    const zoomOverlay = document.getElementById("imageZoomOverlay");

    // Thumbnail click handlers
    thumbnails.forEach((thumbnail) => {
      thumbnail.addEventListener("click", function () {
        if (mainImage) {
          mainImage.src = this.src;
          mainImage.alt = this.alt;

          // Update active thumbnail
          document.querySelectorAll(".thumbnail-item").forEach((item) => {
            item.classList.remove("active");
          });
          this.closest(".thumbnail-item").classList.add("active");
        }
      });
    });

    // Image zoom functionality
    if (zoomOverlay && mainImage) {
      zoomOverlay.addEventListener("click", function () {
        openImageModal(mainImage.src, mainImage.alt);
      });
    }
  }

  function openImageModal(imageSrc, imageAlt) {
    const modal = document.createElement("div");
    modal.className = "image-modal";
    modal.innerHTML = `
            <div class="image-modal-backdrop" onclick="this.parentElement.remove()">
                <img src="${imageSrc}" alt="${imageAlt}" class="image-modal-content">
                <button class="image-modal-close" onclick="this.parentElement.parentElement.remove()">
                    <svg class="w-6 h-6" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path d="M6.28 5.22a.75.75 0 0 0-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 1 0 1.06 1.06L10 11.06l3.72 3.72a.75.75 0 1 0 1.06-1.06L11.06 10l3.72-3.72a.75.75 0 0 0-1.06-1.06L10 8.94 6.28 5.22Z" /></svg>
                </button>
            </div>
        `;

    document.body.appendChild(modal);

    // Prevent body scrolling
    document.body.style.overflow = "hidden";

    // Remove modal when clicking outside or pressing Escape
    modal.addEventListener("click", function (e) {
      if (
        e.target === modal ||
        e.target.classList.contains("image-modal-backdrop")
      ) {
        document.body.style.overflow = "";
        modal.remove();
      }
    });

    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && modal.parentElement) {
        document.body.style.overflow = "";
        modal.remove();
      }
    });
  }

  function initializeProductTabs() {
    // Enhanced tab functionality
    const tabButtons = document.querySelectorAll(".product-tabs .nav-link");

    tabButtons.forEach((button) => {
      button.addEventListener("shown.bs.tab", function (e) {
        const targetTab = e.target.getAttribute("data-bs-target");

        // Track tab views
        if (typeof gtag !== "undefined") {
          gtag("event", "view_product_tab", {
            tab_name: targetTab.replace("#", ""),
          });
        }

        // Lazy load content if needed
        if (targetTab === "#reviews") {
          loadReviews();
        }
      });
    });
  }

  function loadReviews() {
    // TODO: Implement lazy loading of reviews
  }

  // ================================
  // SORTING & FILTERING
  // ================================

  function initializeSorting() {
    const sortSelect = document.getElementById("sortSelect");
    const filterForm = document.getElementById("filterForm");

    if (!sortSelect) return;

    // AJAX sorting on change
    sortSelect.addEventListener("change", function () {
      const value = this.value;
      changeSorting(value);
    });
    
    // AJAX filtering on form submit
    if (filterForm) {
      filterForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        await applyFiltersAjax();
      });
    }
    
    // Handle browser back/forward
    window.addEventListener('popstate', function() {
      location.reload();
    });
  }

  async function changeSorting(value) {
    const form = document.getElementById('filterForm');
    if (!form) return;
    
    // Update sort value in form
    const sortSelect = document.getElementById('sortSelect');
    if (sortSelect) sortSelect.value = value;
    
    // Trigger AJAX filter
    await applyFiltersAjax();
  }

  // AJAX filtering function
  async function applyFiltersAjax() {
    const form = document.getElementById('filterForm');
    if (!form) return;

    const formData = new FormData(form);
    const params = new URLSearchParams(formData);
    
    // Show loading state
    showLoadingState();

    try {
      // Fetch products with current filters
      const response = await fetch(`/products?${params.toString()}`, {
        headers: {
          'X-Requested-With': 'XMLHttpRequest'
        }
      });

      if (!response.ok) throw new Error('Network response was not ok');

      const html = await response.text();
      
      // Parse the HTML response
      const parser = new DOMParser();
      const doc = parser.parseFromString(html, 'text/html');
      
      // Extract products grid
      const newGrid = doc.getElementById('productsGrid');
      const currentGrid = document.getElementById('productsGrid');
      
      if (newGrid && currentGrid) {
        // Smooth transition
        currentGrid.style.opacity = '0';
        
        setTimeout(() => {
          currentGrid.innerHTML = newGrid.innerHTML;
          currentGrid.style.opacity = '1';
          
          // Re-observe lazy images
          initializePerformanceOptimizations();
          
          // ✅ Refresh AOS for new products (no conflict now)
          if (typeof AOS !== 'undefined') {
            AOS.refresh();
          }
        }, 300);
      }
      
      // ✅ Update pagination - Ẩn/hiện dựa trên isPaginationEnabled
      const newPagination = doc.querySelector('.pagination-nav');
      const currentPagination = document.querySelector('.pagination-nav');
      
      if (newPagination) {
        // Backend trả về pagination → Hiển thị
        if (currentPagination) {
          currentPagination.innerHTML = newPagination.innerHTML;
          currentPagination.style.display = '';
        } else {
          // Chưa có pagination trong DOM → Thêm vào
          const productsSection = document.querySelector('.products-section');
          if (productsSection) {
            productsSection.insertAdjacentHTML('afterend', newPagination.outerHTML);
          }
        }
      } else {
        // Backend KHÔNG trả về pagination → Ẩn đi
        if (currentPagination) {
          currentPagination.style.display = 'none';
        }
      }
      
      // Update results info
      const newResultsInfo = doc.querySelector('.text-center.text-sm.text-gray-600');
      const currentResultsInfo = document.querySelector('.text-center.text-sm.text-gray-600');
      if (newResultsInfo && currentResultsInfo) {
        currentResultsInfo.innerHTML = newResultsInfo.innerHTML;
      }

      // Update URL without reload
      const newUrl = `/products?${params.toString()}`;
      history.pushState(null, '', newUrl);
      
      // ❌ Tắt thông báo khi lọc (theo yêu cầu user)
      // if (typeof showToast === 'function') {
      //   showToast('Đã cập nhật kết quả', 'success');
      // }

    } catch (error) {
      console.error('Error filtering products:', error);
      if (typeof showToast === 'function') {
        showToast('Có lỗi xảy ra khi lọc sản phẩm', 'error');
      }
      // Fallback to page reload
      form.submit();
    } finally {
      hideLoadingState();
    }
  }

  // Show skeleton loading with height preservation
  function showLoadingState() {
    const grid = document.getElementById('productsGrid');
    if (!grid) return;
    
    // Preserve current height to prevent layout shift (CLS)
    const currentHeight = grid.offsetHeight;
    if (currentHeight > 0) {
      grid.style.minHeight = currentHeight + 'px';
    }
    
    grid.classList.add('loading');
    
    // Create skeleton cards
    const skeletonHTML = Array(12).fill(0).map(() => `
      <div class="skeleton-card">
        <div class="skeleton-image"></div>
        <div class="skeleton-content">
          <div class="skeleton-title"></div>
          <div class="skeleton-text"></div>
          <div class="skeleton-text" style="width: 60%;"></div>
          <div class="skeleton-price" style="margin-top: 1rem;"></div>
        </div>
      </div>
    `).join('');
    
    grid.innerHTML = skeletonHTML;
  }

  // Hide loading state and restore natural height
  function hideLoadingState() {
    const grid = document.getElementById('productsGrid');
    if (grid) {
      grid.classList.remove('loading');
      grid.style.minHeight = ''; // Restore natural height
    }
  }

  // ================================
  // PERFORMANCE OPTIMIZATIONS
  // ================================

  function initializePerformanceOptimizations() {
    // Lazy loading for product images
    if ("IntersectionObserver" in window) {
      const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = entry.target;
            img.src = img.dataset.src || img.src;
            img.classList.remove("lazy");
            observer.unobserve(img);
          }
        });
      });

      document.querySelectorAll('img[loading="lazy"]').forEach((img) => {
        imageObserver.observe(img);
      });
    }

    // // Preload critical resources
    // const preloadLinks = [
    //     '/css/products.css',
    //     '/js/products.js'
    // ];

    // preloadLinks.forEach(href => {
    //     const link = document.createElement('link');
    //     link.rel = 'preload';
    //     link.href = href;
    //     link.as = href.endsWith('.css') ? 'style' : 'script';
    //     document.head.appendChild(link);
    // });
  }

  // ================================
  // ERROR HANDLING
  // ================================

  function initializeErrorHandling() {
    // Global error handler for AJAX requests
    window.addEventListener("unhandledrejection", function (event) {
      showToast("Đã xảy ra lỗi. Vui lòng thử lại sau.", "error");
    });

    // Network error detection
    window.addEventListener("online", function () {
      showToast("Kết nối internet đã được khôi phục");
    });

    window.addEventListener("offline", function () {
      showToast("Mất kết nối internet", "error");
    });
  }

  // ================================
  // INITIALIZATION
  // ================================

  function initialize() {
    // Check if we're on a products page
    const isProductsPage =
      document.querySelector(".products-section") ||
      document.querySelector(".product-detail-section");

    if (!isProductsPage) {
      return;
    }

    // Initialize common functionality
    initializeErrorHandling();
    initializePerformanceOptimizations();
    // Search initialized in main.js
    initializeSorting();

    // Initialize page-specific functionality
    if (document.querySelector(".products-section")) {
      // Products listing page
      initializeProductGrid();
    }

    if (document.querySelector(".product-detail-section")) {
      // Product detail page
      initializeProductDetail();
    }

    // Products page initialized successfully
  }

  // Wishlist functionality is handled by main.js via event delegation
  // No duplicate code needed here

  // ================================
  // GLOBAL FUNCTIONS (for inline handlers)
  // ================================

  // Make some functions globally accessible for inline event handlers
  window.changeSorting = changeSorting;
  
  // Note: Wishlist now uses event delegation, no need for global function
  // Note: changeCategory is now handled by form submit in index.html, not needed here

  window.buyNow = function (button) {
    const productId = button.dataset.productId;
    const quantity = document.getElementById("quantity")?.value || 1;

    // TODO: Implement buy now functionality
    // Buy now functionality

    // For now, redirect to cart
    window.location.href = "/cart";
  };

  // window.increaseQuantity = function () {
  //   const input = document.getElementById("quantity");
  //   if (input) {
  //     const currentValue = parseInt(input.value) || CONFIG.MIN_QUANTITY;
  //     if (currentValue < CONFIG.MAX_QUANTITY) {
  //       input.value = currentValue + 1;
  //       updateQuantityDisplay();
  //     }
  //   }
  // };

  // window.decreaseQuantity = function () {
  //   const input = document.getElementById("quantity");
  //   if (input) {
  //     const currentValue = parseInt(input.value) || CONFIG.MIN_QUANTITY;
  //     if (currentValue > CONFIG.MIN_QUANTITY) {
  //       input.value = currentValue - 1;
  //       updateQuantityDisplay();
  //     }
  //   }
  // };

  // ================================
  // UTILITY FUNCTIONS
  // ================================

  // Use global getCsrfToken provided by main.js

  // Use showToast from main.js if available, otherwise create a simple fallback
  // Use global showToast provided by main.js
  // ================================
  // REVIEW FUNCTIONS
  // ================================

  // Edit review
  window.editReview = function (reviewId) {
    // For now, show a message that editing is not implemented
    if (typeof showToast === 'function') {
      showToast('Tính năng chỉnh sửa đánh giá sẽ được cập nhật sớm', 'info');
    }
  };

  // Delete review
  window.deleteReview = async function (reviewId) {
    const confirmed = await showConfirm({
      title: 'Xóa đánh giá',
      text: 'Bạn có chắc chắn muốn xóa đánh giá này?',
      icon: 'warning',
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      isDanger: true
    });
    
    if (!confirmed) {
      return;
    }

    fetch(`/api/reviews/${reviewId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    .then(response => response.json())
    .then(data => {
      if (data.success) {
        if (typeof showToast === 'function') {
          showToast('Đánh giá đã được xóa thành công', 'success');
        }
        // Reload the page to update the reviews
        window.location.reload();
      } else {
        if (typeof showToast === 'function') {
          showToast(data.message || 'Có lỗi xảy ra khi xóa đánh giá', 'error');
        }
      }
    })
    .catch(error => {
      // Error deleting review
      if (typeof showToast === 'function') {
        showToast('Có lỗi xảy ra khi xóa đánh giá', 'error');
      }
    });
  };

  // Mark review as helpful
  window.markHelpful = function (reviewId) {
    // For now, show a message that helpful feature is not implemented
    if (typeof showToast === 'function') {
      showToast('Tính năng "Hữu ích" sẽ được cập nhật sớm', 'info');
    }
  };

  // ================================
  // DOM READY
  // ================================

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initialize);
  } else {
    initialize();
  }
})();
