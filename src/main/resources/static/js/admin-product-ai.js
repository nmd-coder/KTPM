/**
 * AI Product Description Generator
 * Handles AI-powered product description generation for admin
 */

let currentAbortController = null;

/**
 * Extract keywords from product name and catalog
 */
function extractKeywordsFromProduct() {
    const name = document.getElementById('productName').value.trim();
    const catalogSelect = document.getElementById('productCatalog');
    const selectedOption = catalogSelect.options[catalogSelect.selectedIndex];
    const catalogName = selectedOption ? selectedOption.getAttribute('data-category-name') || '' : '';
    
    // Smart extraction logic - only extract meaningful words
    const nameWords = name.split(' ').filter(word => word.length > 2).slice(0, 2); // Only words > 2 chars, max 2 words
    const keywords = [catalogName, ...nameWords].filter(word => word.length > 2).join(', ');
    
    document.getElementById('aiKeywords').value = keywords;
    return keywords;
}

/**
 * Generate product description using AI
 */
function generateProductDescription() {
    const productName = document.getElementById('productName').value.trim();
    const catalogId = document.getElementById('productCatalog').value;
    let keywords = document.getElementById('aiKeywords').value.trim();
    
    // Auto-extract keywords if empty
    if (!keywords) {
        keywords = extractKeywordsFromProduct();
    }
    
    // Validation
    if (!productName) {
        showToast('Vui lòng nhập tên sản phẩm!', 'warning');
        document.getElementById('productName').focus();
        return;
    }
    
    // Cancel previous request
    if (currentAbortController) {
        currentAbortController.abort();
    }
    currentAbortController = new AbortController();
    
    // UI state - disable button and show loading
    const btn = document.getElementById('btnGenerateDescription');
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i>Đang viết...';
    
    // Clear existing description to show loading state
    const descriptionField = document.getElementById('productDescription');
    descriptionField.value = 'Đang tạo mô tả bằng AI...';
    
    // Prepare request data
    const formData = new URLSearchParams({
        productName: productName,
        catalogId: catalogId || '',
        keywords: keywords
    });
    
    // Prepare headers (CSRF not needed for this endpoint as per SecurityConfig)
    const headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    };
    
    // API call
    fetch('/admin/products/api/generate-description', {
        method: 'POST',
        headers: headers,
        body: formData,
        signal: currentAbortController.signal
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        return response.json();
    })
    .then(data => {
        if (data.data && !data.error) {
            // Fill description textarea
            const descriptionField = document.getElementById('productDescription');
            descriptionField.value = data.data;
            
            // Save to localStorage as backup
            localStorage.setItem('ai_description_draft', data.data);
            
            // Show success message
            showToast('Tạo mô tả thành công!', 'success');
            
            // Focus on description for review
            descriptionField.focus();
        } else {
            throw new Error(data.message || 'Không thể tạo mô tả');
        }
    })
    .catch(error => {
        if (error.name === 'AbortError') {
            return;
        }
        
        // Show user-friendly error message
        let errorMessage = 'Đã xảy ra lỗi. Vui lòng kiểm tra kết nối mạng.';
        
        if (error.message) {
            if (error.message.includes('timeout') || error.message.includes('Timeout')) {
                errorMessage = 'AI đang quá tải, vui lòng thử lại';
            } else if (error.message.includes('quota') || error.message.includes('Quota')) {
                errorMessage = 'Đã vượt giới hạn sử dụng AI hôm nay';
            } else if (error.message.includes('network') || error.message.includes('Network')) {
                errorMessage = 'Không thể kết nối với AI';
            } else if (error.message.includes('Vui lòng nhập tên sản phẩm')) {
                errorMessage = error.message;
            }
        }
        
        showToast(errorMessage, 'error');
    })
    .finally(() => {
        // Re-enable button
        btn.disabled = false;
        btn.innerHTML = originalHtml;
        currentAbortController = null;
    });
}

/**
 * Load AI description draft from localStorage
 */
function loadAiDescriptionDraft() {
    const draft = localStorage.getItem('ai_description_draft');
    const descriptionField = document.getElementById('productDescription');
    if (draft && !descriptionField.value.trim()) {
        descriptionField.value = draft;
    }
}

/**
 * Clear AI description draft from localStorage
 */
function clearAiDescriptionDraft() {
    localStorage.removeItem('ai_description_draft');
}

/**
 * Setup AI description generator
 */
function setupAiDescriptionGenerator() {
    // Attach event listener to AI button
    const aiButton = document.getElementById('btnGenerateDescription');
    if (aiButton) {
        aiButton.addEventListener('click', function(e) {
            e.preventDefault();
            generateProductDescription();
        });
    }
    
    // Auto-extract keywords when product name or catalog changes
    const productNameField = document.getElementById('productName');
    const catalogField = document.getElementById('productCatalog');
    const keywordsField = document.getElementById('aiKeywords');
    
    // Clear keywords field when product name is cleared
    if (productNameField) {
        productNameField.addEventListener('input', function() {
            const keywordsField = document.getElementById('aiKeywords');
            if (this.value.trim() === '' && keywordsField.value.trim() !== '') {
                keywordsField.value = '';
            }
        });
    }
    
    // Load draft on form open
    if (aiButton) {
        aiButton.addEventListener('click', function() {
            loadAiDescriptionDraft();
        });
    }
    
    // Clear draft when form is submitted successfully
    const productForm = document.getElementById('productForm');
    if (productForm) {
        productForm.addEventListener('submit', function() {
            clearAiDescriptionDraft();
        });
    }
    
    // AI Product Description Generator initialized
}

// Note: setupAiDescriptionGenerator() is called from index.html after DOM ready
