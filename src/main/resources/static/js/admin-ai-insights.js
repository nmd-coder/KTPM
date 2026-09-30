/**
 * AI Insights Widget JavaScript
 * Handles fetching and rendering AI insights for admin dashboard
 */

// Global function to load AI insights
function loadAiInsights() {
    const widget = document.getElementById('aiInsightsWidget');
    const content = document.getElementById('aiWidgetContent');
    const loadingIndicator = document.getElementById('aiLoadingIndicator');
    
    if (!widget || !content) {
        return;
    }
    
    // Show loading state
    showLoadingState(content, loadingIndicator);
    
    // Fetch AI insights from API
    fetch('/admin/api/ai-insights')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success && data.data) {
                renderInsights(data.data.insights, content);
            } else if (data.fallback) {
                renderInsights(data.fallback.insights, content);
            } else {
                throw new Error('Invalid response format');
            }
        })
        .catch(error => {
            showErrorState(content, 'Không thể tải phân tích AI. Vui lòng thử lại sau.');
        })
        .finally(() => {
            hideLoadingIndicator(loadingIndicator);
        });
}

/**
 * Show loading state with skeleton
 */
function showLoadingState(content, loadingIndicator) {
    content.innerHTML = `
        <div class="insights-loading">
            <div class="loading-skeleton">
                <div class="skeleton-item"></div>
                <div class="skeleton-item"></div>
                <div class="skeleton-item"></div>
            </div>
            <p>Đang phân tích dữ liệu...</p>
        </div>
    `;
    
    if (loadingIndicator) {
        loadingIndicator.style.display = 'inline-block';
    }
}

/**
 * Hide loading indicator
 */
function hideLoadingIndicator(loadingIndicator) {
    if (loadingIndicator) {
        loadingIndicator.style.display = 'none';
    }
}

/**
 * Render insights from AI response
 */
function renderInsights(insights, content) {
    if (!insights || insights.length === 0) {
        showErrorState(content, 'Không có dữ liệu phân tích');
        return;
    }
    
    const insightsHtml = insights.map(insight => {
        const actionHtml = insight.actionLink && insight.actionText 
            ? `<a href="${insight.actionLink}" class="insight-action">${insight.actionText}</a>`
            : '';
        
        return `
            <div class="insight-item ${insight.severity}">
                <span class="insight-icon">${insight.icon}</span>
                <div class="insight-content">
                    <h4 class="insight-title">${insight.title}</h4>
                    <p class="insight-message">${insight.message}</p>
                    ${actionHtml}
                </div>
            </div>
        `;
    }).join('');
    
    content.innerHTML = `
        <div class="insights-list">
            ${insightsHtml}
        </div>
    `;
    
    // Add fade-in animation
    content.style.opacity = '0';
    content.style.transform = 'translateY(20px)';
    
    setTimeout(() => {
        content.style.transition = 'all 0.5s ease-out';
        content.style.opacity = '1';
        content.style.transform = 'translateY(0)';
    }, 100);
    
    // Bind click handlers for action links
    bindActionHandlers(content);
}

/**
 * Show error state
 */
function showErrorState(content, message) {
    content.innerHTML = `
        <div class="insights-error">
            <div class="error-icon">⚠️</div>
            <p class="error-message">${message}</p>
            <button class="retry-button" onclick="loadAiInsights()">
                <i class="fas fa-redo"></i> Thử lại
            </button>
        </div>
    `;
}

/**
 * Bind click handlers for action links
 */
function bindActionHandlers(content) {
    const actionLinks = content.querySelectorAll('.insight-action');
    actionLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // Add click tracking if needed
            
            // Optional: Add analytics tracking here
            // gtag('event', 'ai_insight_action', {
            //     'action_link': this.href,
            //     'action_text': this.textContent
            // });
        });
    });
}

/**
 * Auto-refresh insights every hour (optional)
 */
function setupAutoRefresh() {
    // Refresh every hour (3600000 ms)
    setInterval(() => {
        loadAiInsights();
    }, 3600000);
}

// Initialize auto-refresh when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    // Only setup auto-refresh if widget exists
    if (document.getElementById('aiInsightsWidget')) {
        setupAutoRefresh();
    }
});

// Export for global access
window.loadAiInsights = loadAiInsights;
