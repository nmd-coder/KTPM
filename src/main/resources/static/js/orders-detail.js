/**
 * ========================================
 * ORDERS DETAIL PAGE JAVASCRIPT
 * Handle product review functionality
 * ========================================
 */

let currentProductReviewData = {
    orderItemId: null,
    productName: '',
    productImage: ''
};

let selectedProductRating = 0;
let uploadedFiles = [];

// Open product review modal
function openProductReviewModal(orderItemId, productName, productImage) {
    currentProductReviewData = {
        orderItemId: orderItemId,
        productName: productName,
        productImage: productImage
    };
    
    // Set product info
    document.getElementById('productReviewName').textContent = productName;
    document.getElementById('productReviewImage').src = productImage;
    document.getElementById('productReviewImage').alt = productName;
    
    // Reset form
    selectedProductRating = 0;
    document.getElementById('productReviewComment').value = '';
    uploadedFiles = [];
    clearMediaPreview();
    updateProductRatingDisplay();
    
    // Show modal
    document.getElementById('productReviewModal').classList.remove('hidden');
    document.body.style.overflow = 'hidden';
}

// Close product review modal
function closeProductReviewModal() {
    document.getElementById('productReviewModal').classList.add('hidden');
    document.body.style.overflow = 'auto';
    
    // Reset form
    selectedProductRating = 0;
    document.getElementById('productReviewComment').value = '';
    uploadedFiles = [];
    clearMediaPreview();
    updateProductRatingDisplay();
}

// Select product rating
function selectProductRating(rating) {
    selectedProductRating = rating;
    updateProductRatingDisplay();
    
    // Enable submit button
    document.getElementById('submitProductReviewBtn').disabled = false;
}

// Update product rating display
function updateProductRatingDisplay() {
    const stars = document.querySelectorAll('#productRatingStars button');
    const ratingText = document.getElementById('productRatingText');
    
    stars.forEach((star, index) => {
        const rating = index + 1;
        if (rating <= selectedProductRating) {
            star.classList.remove('text-gray-300');
            star.classList.add('text-yellow-400');
        } else {
            star.classList.remove('text-yellow-400');
            star.classList.add('text-gray-300');
        }
    });
    
    // Update rating text
    const ratingTexts = {
        0: 'Chọn số sao để đánh giá',
        1: 'Rất tệ',
        2: 'Tệ',
        3: 'Bình thường',
        4: 'Tốt',
        5: 'Rất tốt'
    };
    
    ratingText.textContent = ratingTexts[selectedProductRating] || 'Chọn số sao để đánh giá';
}

// Clear media preview
function clearMediaPreview() {
    const preview = document.getElementById('mediaPreview');
    const counter = document.getElementById('fileCounter');
    const fileCount = document.getElementById('fileCount');
    
    preview.innerHTML = '';
    preview.classList.add('hidden');
    counter.classList.add('hidden');
    fileCount.textContent = '0';
}

// Handle file upload
function handleFileUpload(files) {
    const validFiles = [];
    const maxSize = 10 * 1024 * 1024; // 10MB
    const maxFiles = 5; // Tối đa 5 file
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'video/mp4', 'video/quicktime'];

    // Kiểm tra tổng số file
    if (uploadedFiles.length + files.length > maxFiles) {
        showToast(`Tối đa chỉ được upload ${maxFiles} file. Hiện tại đã có ${uploadedFiles.length} file.`, 'error');
        return;
    }

    for (let file of files) {
        if (file.size > maxSize) {
            showToast(`File ${file.name} quá lớn. Kích thước tối đa 10MB`, 'error');
            continue;
        }

        if (!allowedTypes.includes(file.type)) {
            showToast(`File ${file.name} không được hỗ trợ. Chỉ chấp nhận ảnh (JPG, PNG, GIF) và video (MP4, MOV)`, 'error');
            continue;
        }

        validFiles.push(file);
    }

    if (validFiles.length > 0) {
        uploadedFiles = [...uploadedFiles, ...validFiles];
        updateMediaPreview();
    }
}

// Update media preview
function updateMediaPreview() {
    const preview = document.getElementById('mediaPreview');
    const counter = document.getElementById('fileCounter');
    const fileCount = document.getElementById('fileCount');
    
    preview.innerHTML = '';
    fileCount.textContent = uploadedFiles.length;

    if (uploadedFiles.length === 0) {
        preview.classList.add('hidden');
        counter.classList.add('hidden');
        return;
    }

    preview.classList.remove('hidden');
    counter.classList.remove('hidden');

    uploadedFiles.forEach((file, index) => {
        const previewItem = document.createElement('div');
        previewItem.className = 'relative group';
        
        const isVideo = file.type.startsWith('video/');
        
        previewItem.innerHTML = `
            <div class="relative">
                ${isVideo ? 
                    `<video class="w-full h-24 object-cover rounded-lg" controls>
                        <source src="${URL.createObjectURL(file)}" type="${file.type}">
                    </video>` :
                    `<img src="${URL.createObjectURL(file)}" alt="${file.name}" class="w-full h-24 object-cover rounded-lg">`
                }
                <button type="button" 
                        onclick="removeFile(${index})"
                        class="absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-6 h-6 flex items-center justify-center text-xs hover:bg-red-600 transition-colors">
                    ×
                </button>
            </div>
            <p class="text-xs text-gray-600 mt-1 truncate" title="${file.name}">${file.name}</p>
        `;
        
        preview.appendChild(previewItem);
    });
}

// Remove file from upload list
function removeFile(index) {
    uploadedFiles.splice(index, 1);
    updateMediaPreview();
}

// Submit product review
async function submitProductReview() {
    if (selectedProductRating === 0) {
        showToast('Vui lòng chọn số sao đánh giá', 'error');
        return;
    }
    
    const comment = document.getElementById('productReviewComment').value.trim();
    
    try {
        showLoading('Đang gửi đánh giá...');
        
        // Get CSRF token
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        
        // Create FormData for file upload
        const formData = new FormData();
        formData.append('rating', selectedProductRating);
        formData.append('comment', comment);
        
        // Add uploaded files
        uploadedFiles.forEach((file, index) => {
            formData.append('files', file);
        });
        
        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        
        // Submit review for single product
        const response = await fetch(`/api/reviews/order-item/${currentProductReviewData.orderItemId}`, {
            method: 'POST',
            headers: headers,
            body: formData
        });
        
        const data = await response.json();
        closeLoading();
        
        if (response.ok && data && data.success) {
            showToast('Đánh giá sản phẩm thành công!', 'success');
            
            // Close modal
            closeProductReviewModal();
            
            // Reload page to update UI
            window.location.reload();
        } else {
            showToast(data.message || 'Có lỗi xảy ra khi gửi đánh giá', 'error');
        }
    } catch (error) {
        console.error('Error submitting product review:', error);
        closeLoading();
        showToast('Có lỗi xảy ra khi gửi đánh giá', 'error');
    }
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    // Product rating stars
    document.querySelectorAll('#productRatingStars button').forEach(button => {
        button.addEventListener('click', function() {
            const rating = parseInt(this.getAttribute('data-rating'));
            selectProductRating(rating);
        });
    });

    // File upload
    const mediaUpload = document.getElementById('mediaUpload');
    const uploadArea = document.getElementById('uploadArea');

    if (mediaUpload) {
        mediaUpload.addEventListener('change', function(e) {
            handleFileUpload(Array.from(e.target.files));
        });
    }

    if (uploadArea) {
        // Drag and drop
        uploadArea.addEventListener('dragover', function(e) {
            e.preventDefault();
            uploadArea.classList.add('border-pink-400', 'bg-pink-50');
        });

        uploadArea.addEventListener('dragleave', function(e) {
            e.preventDefault();
            uploadArea.classList.remove('border-pink-400', 'bg-pink-50');
        });

        uploadArea.addEventListener('drop', function(e) {
            e.preventDefault();
            uploadArea.classList.remove('border-pink-400', 'bg-pink-50');
            handleFileUpload(Array.from(e.dataTransfer.files));
        });

        // Click to upload
        uploadArea.addEventListener('click', function() {
            mediaUpload.click();
        });
    }
    
    // Close modal buttons
    document.getElementById('closeProductModalBtn').addEventListener('click', closeProductReviewModal);
    document.getElementById('productModalBackdrop').addEventListener('click', closeProductReviewModal);
    
    // Submit button
    document.getElementById('submitProductReviewBtn').addEventListener('click', submitProductReview);
    
    // Close on Escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeProductReviewModal();
        }
    });
});
