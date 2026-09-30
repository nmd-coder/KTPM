(function() {
	let selectedPaymentMethod = null;
	let cartData = null;
	let shippingFee = 0;
	let shippingCalculated = false;
	let appliedVoucher = null;
	
	// Helper to safely get translation
	function t(key, fallback) {
		if (typeof window.languageSwitcher !== 'undefined' && window.languageSwitcher && typeof window.languageSwitcher.translate === 'function') {
			return window.languageSwitcher.translate(key);
		}
		return fallback || key;
	}

	document.addEventListener('DOMContentLoaded', function() {
		loadCartData();
		loadPaymentMethods();
		const btn = document.getElementById('place-order-btn');
		if (btn) {
			btn.addEventListener('click', placeOrder);
		}
			const voucherBtn = document.getElementById('apply-voucher-btn');
			if (voucherBtn) {
				voucherBtn.addEventListener('click', applyVoucher);
			}
		const addressSelect = document.getElementById('addressSelect');
		if (addressSelect) {
			
			addressSelect.addEventListener('change', function() {
				updateAddressDetails();
				calculateShippingFee();
				updatePlaceOrderButton();
			});
			// Show initial address if default is selected
			if (addressSelect.value) {
				updateAddressDetails();
				calculateShippingFee();
			}
		}
	});

	function updateAddressDetails() {
		const select = document.getElementById('addressSelect');
		const indicator = document.getElementById('addressSelectedIndicator');
		const shippingSection = document.getElementById('shippingFeeSection');
		
		// Update address details
		
		if (select && select.value) {
			const selectedOption = select.options[select.selectedIndex];
			
			// Show green indicator
			if (indicator) {
				indicator.style.display = 'flex';
			}
			
			// Show shipping section
			if (shippingSection) {
				shippingSection.style.display = 'block';
			}
		} else {
			// Hide green indicator
			if (indicator) {
				indicator.style.display = 'none';
			}
			
			// Hide shipping section
			if (shippingSection) {
				shippingSection.style.display = 'none';
			}
			shippingFee = 0;
			shippingCalculated = false;
		}
	}

	function calculateShippingFee() {
		const select = document.getElementById('addressSelect');
		const shippingContent = document.getElementById('shippingFeeContent');
		
		// Calculate shipping fee
		
		if (!select || !select.value) {
			return;
		}
		
		// Show loading (Tailwind) if container exists
		if (shippingContent) {
			shippingContent.innerHTML = `
				<div class="flex items-center text-sm text-gray-600">
					<svg class="animate-spin h-4 w-4 text-primary" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path></svg>
					<span class="ml-2">${t('calculating-shipping-fee', 'Đang tính phí vận chuyển...')}</span>
				</div>
			`;
		}
		
		// Call shipping fee API
		const csrfToken = getCsrfToken();
		const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
		
		const requestBody = {
			addressId: parseInt(select.value),
			serviceTypeId: 2 // Default GHN service type
		};
		
		// Making shipping API request
		
		fetch('/api/shipping/fee', {
			method: 'POST',
			headers: { 
				'Content-Type': 'application/json',
				'X-Requested-With': 'XMLHttpRequest',
				[csrfHeader]: csrfToken
			},
			credentials: 'same-origin',
			body: JSON.stringify(requestBody)
		})
		.then(r => {
			return r.json();
		})
		.then(response => {
			if (response.data && response.data.success) {
				shippingFee = response.data.shippingFee || 0;
				shippingCalculated = true;
				
				if (shippingContent) {
					shippingContent.innerHTML = `
						<div class="flex items-center justify-between">
							<div>
								<strong class="text-green-600">${formatCurrency(shippingFee)}</strong>
								<br><small class="text-gray-500">${t('fast-shipping-ghn', 'Giao hàng nhanh (GHN)')}</small>
							</div>
							<svg class="h-5 w-5 text-green-600" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-7.25 9a.75.75 0 01-1.127.05l-3.25-3.5a.75.75 0 111.102-1.02l2.66 2.868 6.71-8.34a.75.75 0 011.012-.11z" clip-rule="evenodd"/></svg>
						</div>
					`;
				}
			} else {
				// Fallback to free shipping
				shippingFee = 0;
				shippingCalculated = true;
				
				if (shippingContent) {
					shippingContent.innerHTML = `
						<div class="flex items-center justify-between">
							<div>
								<strong class="text-green-600">${t('free', 'Miễn phí')}</strong>
								<br><small class="text-gray-500">${response.error || t('cannot-calculate-ghn-fee', 'Không thể tính phí GHN')}</small>
							</div>
							<span class="text-green-600">🎁</span>
						</div>
					`;
				}
			}
			
			// Update order summary with new shipping fee
			if (cartData) {
				displayOrderSummary(cartData);
			}
		})
		.catch(error => {
			// Fallback to free shipping
			shippingFee = 0;
			shippingCalculated = true;
			
			if (shippingContent) {
				shippingContent.innerHTML = `
					<div class="flex items-center justify-between">
						<div>
							<strong class="text-green-600">${t('free', 'Miễn phí')}</strong>
							<br><small class="text-gray-500">${error.message || t('cannot-calculate-shipping-fee', 'Không thể tính phí vận chuyển')}</small>
						</div>
						<span class="text-amber-600">⚠️</span>
					</div>
				`;
			}
			
			// Update order summary
			if (cartData) {
				displayOrderSummary(cartData);
			}
		});
	}

	function loadCartData() {
		fetch('/api/cart/get', { credentials: 'same-origin' })
			.then(r => r.json())
			.then(response => {
				if (response.data && response.data.success) {
					cartData = response.data.cart;
					displayCartItems(cartData);
					displayOrderSummary(cartData);
					if (cartData.items && cartData.items.length > 0) {
						document.getElementById('place-order-btn').disabled = false;
					}
				} else {
					document.getElementById('cart-items').innerHTML = '<div class="rounded-lg bg-amber-50 text-amber-800 px-4 py-3">' + t('cart-empty', 'Giỏ hàng trống') + '</div>';
					document.getElementById('order-summary').innerHTML = '<div class="rounded-lg bg-amber-50 text-amber-800 px-4 py-3">' + t('no-products-in-cart', 'Không có sản phẩm nào trong giỏ hàng') + '</div>';
				}
			})
			.catch(() => {
				document.getElementById('cart-items').innerHTML = '<div class="rounded-lg bg-red-50 text-red-700 px-4 py-3">' + t('error-loading-cart', 'Có lỗi khi tải giỏ hàng') + '</div>';
				document.getElementById('order-summary').innerHTML = '<div class="rounded-lg bg-red-50 text-red-700 px-4 py-3">' + t('error-loading-order-summary', 'Có lỗi khi tải tóm tắt đơn hàng') + '</div>';
			});
	}

	function loadPaymentMethods() {
		fetch('/api/payment/methods', { credentials: 'same-origin' })
			.then(r => r.json())
			.then(response => {
				if (response.data) displayPaymentMethods(response.data);
			})
			.catch(() => {
				document.getElementById('payment-methods').innerHTML = '<div class="rounded-lg bg-red-50 text-red-700 px-4 py-3">' + t('error-loading-payment-methods', 'Có lỗi xảy ra khi tải phương thức thanh toán') + '</div>';
			});
	}

	function displayCartItems(cart) {
		let html = '';
		if (cart.items && cart.items.length > 0) {
			// Filter items based on selected product IDs from session storage
			const selectedCartItems = sessionStorage.getItem('selectedCartItems');
			const selectedProductIds = selectedCartItems ? JSON.parse(selectedCartItems) : null;
			
			let itemsToDisplay = cart.items;
			if (selectedProductIds && selectedProductIds.length > 0) {
				itemsToDisplay = cart.items.filter(function(item) {
					const productId = item.productId || (item.product && item.product.id);
					return selectedProductIds.includes(productId);
				});
			}
			
			itemsToDisplay.forEach(function(item) {
				const imageUrl = (item.productImageUrl || item.imageUrl || (item.product && (item.product.imageUrl || item.product.thumbnail)) || item.productImage || '/images/products/default.jpg');
				const name = (item.productName || (item.product && item.product.name) || item.name || '');
				const unitPrice = getFirstNumber(
					item.unitPrice,
					item.price,
					item.productPrice,
					item.salePrice,
					item.finalPrice,
					item?.product?.price,
					item?.product?.salePrice
				);
				const quantity = getFirstNumber(item.quantity, item.qty, item.count, 1);
				const lineTotal = getFirstNumber(
					item.totalPrice,
					item.lineTotal,
					item.amount,
					item.totalAmount,
					unitPrice * quantity
				);
				html += '<div class="flex items-center gap-4 mb-3 p-4 rounded-2xl border-2 border-gray-400 bg-white shadow-sm hover:border-primary transition-all duration-200">'
					+ '<img src="' + imageUrl + '" class="rounded-xl flex-shrink-0" width="96" height="96" style="object-fit: cover;">'
					+ '<div class="flex-1 min-w-0">'
					+ '<div class="text-sm font-semibold text-gray-900 truncate">' + escapeHtml(name) + '</div>'
					+ '<div class="text-xs text-gray-500">' + formatCurrency(unitPrice) + ' × ' + quantity + '</div>'
					+ '</div>'
					+ '<div class="text-right">'
					+ '<strong class="text-gray-900 whitespace-nowrap">' + formatCurrency(lineTotal) + '</strong>'
					+ '</div>'
					+ '</div>';
			});
		} else {
			html = '<div class="rounded-lg bg-amber-50 text-amber-800 px-4 py-3">' + t('cart-empty', 'Giỏ hàng trống') + '</div>';
		}
		document.getElementById('cart-items').innerHTML = html;
	}

	function displayOrderSummary(cart) {
		let html = '';
		if (cart.items && cart.items.length > 0) {
			// Filter items based on selected product IDs
			const selectedCartItems = sessionStorage.getItem('selectedCartItems');
			const selectedProductIds = selectedCartItems ? JSON.parse(selectedCartItems) : null;
			
			let itemsToCalculate = cart.items;
			if (selectedProductIds && selectedProductIds.length > 0) {
				itemsToCalculate = cart.items.filter(function(item) {
					const productId = item.productId || (item.product && item.product.id);
					return selectedProductIds.includes(productId);
				});
			}
			
			// Always calculate subtotal from itemsToCalculate (selected items only)
			let subtotal = 0;
			try {
				subtotal = (itemsToCalculate || []).reduce(function(acc, it){
					const unitPrice = getFirstNumber(it.unitPrice, it.price, it.productPrice, it.salePrice, it.finalPrice, it?.product?.price, it?.product?.salePrice);
					const quantity = getFirstNumber(it.quantity, it.qty, it.count, 1);
					const lineTotal = getFirstNumber(it.totalPrice, it.lineTotal, it.amount, it.totalAmount, unitPrice * quantity);
					return acc + lineTotal;
				}, 0);
			} catch(_) { 
				subtotal = 0;
			}
			let discount = appliedVoucher && appliedVoucher.discount ? appliedVoucher.discount : 0;
			let shipping = shippingFee || 0;
			let total = Math.max(0, subtotal - discount) + shipping;
			
			html = '<div class="flex items-center justify-between mb-2">'
				+ '<span class="text-gray-600">' + t('temporary-total', 'Tạm tính') + ':</span>'
				+ '<span class="font-medium">' + formatCurrency(subtotal) + '</span>'
				+ '</div>'
				+ (discount > 0 ? ('<div class="flex items-center justify-between mb-2">'
				+ '<span class="text-gray-600">' + t('discount', 'Giảm giá') + ':</span>'
				+ '<span class="font-medium text-green-600">- ' + formatCurrency(discount) + '</span>'
				+ '</div>') : '')
				+ '<div class="flex items-center justify-between mb-2">'
				+ '<span class="text-gray-600">' + t('shipping-fee', 'Phí vận chuyển') + ':</span>';
			
			if (shipping > 0) {
				html += '<span class="text-primary font-medium">' + formatCurrency(shipping) + '</span>';
			} else {
				html += '<span class="text-green-600 font-medium">' + t('free', 'Miễn phí') + '</span>';
			}
			
			html += '</div>'
				+ '<div class="my-3 h-px bg-gray-100"></div>'
				+ '<div class="flex items-center justify-between mb-1">'
				+ '<span class="font-semibold text-gray-800">' + t('total', 'Tổng cộng') + ':</span>'
				+ '<span class="font-semibold text-primary">' + formatCurrency(total) + '</span>'
				+ '</div>';
		} else {
			html = '<div class="rounded-lg bg-amber-50 text-amber-800 px-4 py-3">' + t('no-products-in-cart', 'Không có sản phẩm nào trong giỏ hàng') + '</div>';
		}
		document.getElementById('order-summary').innerHTML = html;
	}

	// Helpers for robust numeric parsing and safe HTML
	function toNumber(val) {
		if (val == null) return NaN;
		if (typeof val === 'number') return val;
		if (typeof val === 'string') {
			const cleaned = val.replace(/[^0-9.-]/g, '');
			const parsed = parseFloat(cleaned);
			return isNaN(parsed) ? NaN : parsed;
		}
		return NaN;
	}

	function getFirstNumber() {
		for (let i = 0; i < arguments.length; i++) {
			const n = toNumber(arguments[i]);
			if (!isNaN(n)) return n;
		}
		return 0;
	}

	function escapeHtml(str) {
		try {
			return String(str)
				.replace(/&/g, '&amp;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;')
				.replace(/"/g, '&quot;')
				.replace(/'/g, '&#039;');
		} catch (_) { return str; }
	}

	function displayPaymentMethods(methods) {
		// Map payment method keys to translation keys
		const paymentMethodKeyMap = {
			'COD': 'payment-method-cod',
			'MOMO': 'payment-method-momo',
			'BANK_TRANSFER': 'payment-method-bank',
			'CREDIT_CARD': 'payment-method-card'
		};
		
		let html = '';
		Object.keys(methods).forEach(function(key) {
			const method = methods[key];
			const isAvailable = method.available;
			const disabledAttr = isAvailable ? '' : 'disabled';
			const containerClasses = 'group relative rounded-2xl border-2 border-gray-400 p-4 transition-all duration-200 cursor-pointer bg-white shadow-sm ' + (isAvailable ? 'hover:border-primary' : 'opacity-60 cursor-not-allowed');
			
			// Get translated payment method name
			const methodKey = paymentMethodKeyMap[key] || key.toLowerCase();
			const translatedMethodName = t(methodKey, method.displayName);
			
			// Get translated status message
			let statusKey = 'status-unknown';
			if (method.statusMessage === 'Sẵn sàng' || method.statusMessage === 'Available') {
				statusKey = 'status-available';
			} else if (method.statusMessage === 'Chưa hỗ trợ' || method.statusMessage === 'Not Supported') {
				statusKey = 'status-not-supported';
			}
			const translatedStatus = t(statusKey, method.statusMessage);
			
			html += '<label for="payment-' + key + '" class="' + containerClasses + '">'
				+ '<input class="sr-only" type="radio" name="paymentMethod" id="payment-' + key + '" value="' + key + '" ' + disabledAttr + '>'
				+ '<div class="flex items-start gap-3">'
				+ '<div class="flex-1">'
				+ '<div class="text-base font-semibold text-gray-900">' + translatedMethodName + '</div>'
				+ '<div class="text-xs text-gray-500">' + method.englishName + '</div>'
				+ '</div>'
				+ '<div class="text-right">'
				+ '<span class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ' + (isAvailable ? 'bg-green-50 text-green-700' : 'bg-amber-50 text-amber-700') + '">' + translatedStatus + '</span>'
				+ '</div>'
				+ '</div>'
				+ '<div class="mt-3 hidden group-hover:flex items-center justify-between text-xs text-gray-500">'
				+ '<span>Mô tả ngắn</span>'
				+ '<span class="text-primary">Chọn ➜</span>'
				+ '</div>'
				+ '</label>';
		});
		document.getElementById('payment-methods').innerHTML = html;
		Array.from(document.querySelectorAll('input[name="paymentMethod"]')).forEach(function(r){
			r.addEventListener('change', function(){
				selectedPaymentMethod = this.value;
				updatePlaceOrderButton();
				// Highlight selected container
				document.querySelectorAll('#payment-methods label').forEach(function(lbl){
					lbl.classList.remove('ring-2','ring-primary','bg-primary/5','border-primary');
				});
				const selectedLabel = this.closest('label');
				if (selectedLabel) {
					selectedLabel.classList.add('ring-2','ring-primary','bg-primary/5','border-primary');
				}
			});
		});
	}

	function updatePlaceOrderButton() {
		let hasItems = cartData && cartData.items && cartData.items.length > 0;
		let hasPaymentMethod = selectedPaymentMethod !== null;
		let addressSelect = document.getElementById('addressSelect');
		let hasAddress = addressSelect && addressSelect.value !== '';
		let btn = document.getElementById('place-order-btn');
		if (btn) {
			btn.disabled = !(hasItems && hasPaymentMethod && hasAddress);
		}
	}

	function placeOrder() {
		if (!selectedPaymentMethod) {
			if (typeof showToast === 'function') {
				showToast('Vui lòng chọn phương thức thanh toán', 'warning');
			}
			return;
		}
		
		const addressSelect = document.getElementById('addressSelect');
		if (!addressSelect) {
			if (typeof showToast === 'function') {
				showToast('Vui lòng thêm địa chỉ giao hàng trước khi đặt hàng', 'warning');
			}
			window.location.href = '/account/profile';
			return;
		}
		
		if (!addressSelect.value) {
			if (typeof showToast === 'function') {
				showToast('Vui lòng chọn địa chỉ giao hàng', 'warning');
			}
			return;
		}
		
		const btn = document.getElementById('place-order-btn');
		btn.disabled = true;
		btn.innerHTML = '<svg class="animate-spin -ml-1 mr-2 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path></svg>Đang xử lý...';

		// Get selected product IDs from session storage (set by cart page)
		const selectedCartItems = sessionStorage.getItem('selectedCartItems');
		const selectedProductIds = selectedCartItems ? JSON.parse(selectedCartItems) : null;
		
		const data = {
			orderRequest: {
				addressId: parseInt(addressSelect.value),
				notes: document.getElementById('orderNotes').value.trim(),
				paymentMethod: selectedPaymentMethod,
				serviceTypeId: 2, // GHN default service type
				selectedProductIds: selectedProductIds // Add selected product IDs
			},
			paymentMethod: selectedPaymentMethod
		};
		if (appliedVoucher && appliedVoucher.code) {
			data.orderRequest.voucherCode = appliedVoucher.code;
		}
		fetch('/api/orders/create-with-payment', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			credentials: 'same-origin',
			body: JSON.stringify(data)
		})
		.then(r => r.json())
		.then(response => {
			if (response.data && response.data.payment) {
				const payment = response.data.payment;
				const details = payment.paymentDetails || {};
				if (payment.success && details.payUrl) {
					window.location.href = details.payUrl;
					return;
				}
				if (payment.success) {
					if (typeof showToast === 'function') {
						showToast('🎉 Đơn hàng đã được tạo thành công!', 'success');
					}
					// Clear session storage after successful order
					sessionStorage.removeItem('selectedCartItems');
					// Reload cart count (ordered items removed from cart)
					if (typeof loadCartCount === 'function') {
						loadCartCount();
					}
                    window.location.href = '/account/orders';
					return;
				}
			}
			if (typeof showToast === 'function') {
				showToast((response.data && response.data.payment && response.data.payment.message) || 'Có lỗi xảy ra', 'error');
			}
		})
		.catch(() => {
			if (typeof showToast === 'function') {
				showToast('Có lỗi xảy ra khi đặt hàng', 'error');
			}
		})
		.finally(() => {
			btn.disabled = false;
			btn.innerHTML = 'Đặt hàng';
		});
	}

	function applyVoucher() {
		const input = document.getElementById('voucher-input');
		const message = document.getElementById('voucher-message');
		const btn = document.getElementById('apply-voucher-btn');
		const code = (input.value || '').trim().toUpperCase();
		if (!code) {
			message.textContent = 'Vui lòng nhập mã voucher';
			message.className = 'mt-2 text-sm text-amber-700';
			return;
		}
		if (!cartData || !cartData.totalAmount) {
			message.textContent = 'Không thể áp dụng voucher khi giỏ hàng trống';
			message.className = 'mt-2 text-sm text-amber-700';
			return;
		}
		btn.disabled = true;
		btn.innerHTML = '<svg class="animate-spin -ml-1 mr-2 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path></svg>Đang áp dụng...';

		// Include CSRF header for Spring Security to avoid 403
		const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
		const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
		// Compute current subtotal based on selected items (same logic as order summary)
		let orderAmountForVoucher = 0;
		try {
			const selectedCartItems = sessionStorage.getItem('selectedCartItems');
			const selectedProductIds = selectedCartItems ? JSON.parse(selectedCartItems) : null;
			const itemsToCalculate = (cartData && cartData.items) ? (
				(selectedProductIds && selectedProductIds.length > 0)
					? cartData.items.filter(function(item){
						const productId = item.productId || (item.product && item.product.id);
						return selectedProductIds.includes(productId);
					})
					: cartData.items
			) : [];
			orderAmountForVoucher = (itemsToCalculate || []).reduce(function(acc, it){
				const unitPrice = getFirstNumber(it.unitPrice, it.price, it.productPrice, it.salePrice, it.finalPrice, it?.product?.price, it?.product?.salePrice);
				const quantity = getFirstNumber(it.quantity, it.qty, it.count, 1);
				const lineTotal = getFirstNumber(it.totalPrice, it.lineTotal, it.amount, it.totalAmount, unitPrice * quantity);
				return acc + lineTotal;
			}, 0);
		} catch(_) { orderAmountForVoucher = cartData && cartData.totalAmount ? cartData.totalAmount : 0; }

		fetch('/api/vouchers/apply', {
			method: 'POST',
			headers: (function(){
				const h = { 'Content-Type': 'application/json' };
				if (csrfToken && csrfHeader) { h[csrfHeader] = csrfToken; }
				return h;
			})(),
			credentials: 'same-origin',
			body: JSON.stringify({ code, orderAmount: orderAmountForVoucher })
		})
		.then(r => r.json())
		.then(res => {
			if (res && res.success) {
				appliedVoucher = { code, discount: res.discount || 0 };
				message.textContent = 'Áp dụng voucher thành công: ' + code + ' (-' + formatCurrency(appliedVoucher.discount) + ')';
				message.className = 'mt-2 text-sm text-green-600';
				if (cartData) displayOrderSummary(cartData);
			} else {
				appliedVoucher = null;
				message.textContent = (res && res.message) || 'Voucher không khả dụng';
				message.className = 'mt-2 text-sm text-red-600';
				if (cartData) displayOrderSummary(cartData);
			}
		})
		.catch(() => {
			appliedVoucher = null;
			message.textContent = 'Có lỗi khi áp dụng voucher';
			message.className = 'mt-2 text-sm text-red-600';
			if (cartData) displayOrderSummary(cartData);
		})
		.finally(() => {
			btn.disabled = false;
			btn.innerHTML = 'Áp dụng';
		});
	}

	function formatCurrency(amount) {
		try {
			return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
		} catch (_) { return amount; }
	}
	
	// Expose functions to global scope for language switcher
	window.loadPaymentMethods = loadPaymentMethods;
	window.displayOrderSummary = displayOrderSummary;
	window.displayCartItems = displayCartItems;
	
	// Expose cartData to global scope
	Object.defineProperty(window, 'cartData', {
		get: function() { return cartData; },
		set: function(value) { cartData = value; }
	});
})();

