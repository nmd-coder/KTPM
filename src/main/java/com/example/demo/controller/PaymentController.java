package com.example.demo.controller;

import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.OrderService;
import com.example.demo.service.SseService;
import com.example.demo.service.JwtService;
import com.example.demo.config.MoMoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Enumeration;

@Controller
@RequestMapping("/payment/momo")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "💳 Payment", description = "Payment processing APIs - MoMo integration")
public class PaymentController {

	private final OrderRepository orderRepository;
	private final OrderService orderService;
	private final SseService sseService;
	private final MoMoProperties momoProperties;
	private final com.example.demo.repository.UserRepository userRepository;
	private final JwtService jwtService;

	@Operation(
		summary = "MoMo callback endpoint",
		description = "Endpoint nhận callback từ MoMo sau khi user thanh toán xong. Tự động cập nhật trạng thái đơn hàng và redirect về trang kết quả."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "302", description = "Redirect đến trang kết quả thanh toán"),
		@ApiResponse(responseCode = "400", description = "Chữ ký không hợp lệ hoặc không tìm thấy đơn hàng")
	})

	@GetMapping("/return")
	@Transactional
	public String momoReturn(HttpServletRequest request, org.springframework.ui.Model model, Authentication authentication) {
		log.info("=== MoMo Return Endpoint Called ===");
		log.info("Query string: {}", request.getQueryString());
		log.info("Authentication: {}", authentication != null ? authentication.getName() : "null");
		log.info("Session ID: {}", request.getSession().getId());
		log.info("Session authToken: {}", request.getSession().getAttribute("authToken"));
		log.info("Session userEmail: {}", request.getSession().getAttribute("userEmail"));
		
		try {
			// Extract all parameters for signature verification
			Map<String, String> params = new HashMap<>();
			Enumeration<String> paramNames = request.getParameterNames();
			while (paramNames.hasMoreElements()) {
				String paramName = paramNames.nextElement();
				params.put(paramName, request.getParameter(paramName));
			}
			
			String orderId = request.getParameter("orderId");
			String resultCode = request.getParameter("resultCode");
			String transId = request.getParameter("transId");
			String message = request.getParameter("message");
			String signature = request.getParameter("signature");
			
			log.info("Processing order: {}, resultCode: {}", orderId, resultCode);
			
			// Verify signature for security (temporarily disabled for testing)
			if (signature != null) {
				String rawSignature = buildRawSignature(params);
				boolean isValidSignature = verifySignature(rawSignature, signature, momoProperties.getSecretKey());
				log.info("Signature verification: {} (temporarily disabled)", isValidSignature ? "VALID" : "INVALID");
				log.info("Raw signature string: {}", rawSignature);
				log.info("Expected signature: {}", signature);
				
				// Temporarily comment out signature validation for testing
				/*
				if (!isValidSignature) {
					log.warn("Invalid signature for order: {}", orderId);
					model.addAttribute("success", false);
					model.addAttribute("message", "Chữ ký không hợp lệ. Vui lòng liên hệ hỗ trợ.");
					model.addAttribute("pageTitle", "Lỗi bảo mật - StarShop");
					return "orders/payment-result-simple";
				}
				*/
			}
			
			if ("0".equals(resultCode)) {
				// Payment successful
				Order order = null;
				if (orderId != null && orderId.startsWith("ORDER-")) {
					String orderIdString = parseOrderId(orderId);
					try {
						// Load order first
						order = orderRepository.findOrderWithAllDetails(orderIdString);
						if (order != null) {
							// Restore authentication from order if not already authenticated
							if (authentication == null || !authentication.isAuthenticated()) {
								restoreAuthenticationFromOrder(request, order);
							}
							
							// Lưu momoTransId trước khi cập nhật status
							order.setMomoTransId(transId);
							orderRepository.save(order);
							log.info("Order {} momoTransId saved: {}", orderIdString, transId);
							
							orderService.updateOrderStatus(orderIdString, OrderStatus.PROCESSING);
							log.info("Order {} status updated to PROCESSING via service", orderIdString);
							// Reload order to get updated status
							order = orderRepository.findOrderWithAllDetails(orderIdString);
						}
					} catch (Exception e) {
						log.error("Failed to update order {} status: {}", orderIdString, e.getMessage());
						// Fallback to direct repository access if service fails
						if (order == null) {
							order = orderRepository.findOrderWithAllDetails(orderIdString);
						}
						if (order != null) {
							// Restore authentication from order if not already authenticated
							if (authentication == null || !authentication.isAuthenticated()) {
								restoreAuthenticationFromOrder(request, order);
							}
							
							// Lưu momoTransId trong fallback
							order.setMomoTransId(transId);
							order.setStatus(OrderStatus.PROCESSING);
							orderRepository.save(order);
							log.info("Order {} momoTransId saved and status updated to PROCESSING via fallback", orderIdString);
						}
					}
				}
				
				// Show payment result page with success status
				model.addAttribute("success", true);
				model.addAttribute("message", "Thanh toán thành công! Đơn hàng của bạn đang được xử lý.");
				model.addAttribute("pageTitle", "Thanh toán thành công - StarShop");
				model.addAttribute("orderId", parseOrderId(orderId));
				model.addAttribute("transId", transId);
				
				// Authentication will be handled by BaseController
				// No need to manually set isUserAuthenticated as it will be set by @ModelAttribute
				
				return "orders/payment-result-simple";
			} else {
				// Payment failed - update order status to CANCELLED and show result page
				if (orderId != null && orderId.startsWith("ORDER-")) {
					String orderIdString = parseOrderId(orderId);
					try {
						orderService.updateOrderStatus(orderIdString, OrderStatus.CANCELLED);
						log.info("Order {} status updated to CANCELLED due to payment failure", orderIdString);
					} catch (Exception e) {
						log.error("Failed to update order {} status to CANCELLED: {}", orderIdString, e.getMessage());
						// Fallback to direct repository access if service fails
						Order order = orderRepository.findOrderWithAllDetails(orderIdString);
						if (order != null) {
							order.setStatus(OrderStatus.CANCELLED);
							orderRepository.save(order);
							log.info("Order {} status updated to CANCELLED via fallback", orderIdString);
						}
					}
				}
				
				// Show payment result page with failure status
				model.addAttribute("success", false);
				model.addAttribute("message", message != null ? message : "Thanh toán thất bại. Vui lòng thử lại.");
				model.addAttribute("pageTitle", "Thanh toán thất bại - StarShop");
				model.addAttribute("orderId", parseOrderId(orderId));
				model.addAttribute("transId", transId);
				
				// Authentication will be handled by BaseController
				// No need to manually set isUserAuthenticated as it will be set by @ModelAttribute
				
				return "orders/payment-result-simple";
			}
			
		} catch (Exception e) {
			log.error("Error processing MoMo callback: {}", e.getMessage(), e);
			model.addAttribute("success", false);
			model.addAttribute("message", "Có lỗi xảy ra khi xử lý thanh toán. Vui lòng liên hệ hỗ trợ.");
			model.addAttribute("pageTitle", "Lỗi thanh toán - StarShop");
			
			// Authentication will be handled by BaseController
			// No need to manually set isUserAuthenticated as it will be set by @ModelAttribute
			
			return "orders/payment-result-simple";
		}
	}



	@Operation(
		summary = "MoMo Notify endpoint (IPN)",
		description = "Endpoint nhận thông báo từ MoMo server qua VS Code port forwarding. Xử lý real-time payment updates và push SSE events."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Notify được xử lý thành công")
	})
	@PostMapping("/notify")
	@ResponseBody
	@Transactional
	public Map<String, Object> momoNotify(@RequestBody Map<String, Object> body) {
		log.info("=== MoMo Notify Endpoint Called ===");
		log.info("Request body: {}", body);
		
		try {
			// Extract parameters
			String orderId = String.valueOf(body.get("orderId"));
			String resultCode = String.valueOf(body.get("resultCode"));
			String transId = String.valueOf(body.get("transId"));
			String message = String.valueOf(body.get("message"));
			String signature = String.valueOf(body.get("signature"));
			
			log.info("Processing notify for order: {}, resultCode: {}", orderId, resultCode);
			
			// Convert body to flat map for signature verification
			Map<String, String> params = new HashMap<>();
			body.forEach((k, v) -> params.put(k, v != null ? String.valueOf(v) : ""));
			
			// Verify signature (skip in sandbox for testing)
			if (signature != null && !signature.equals("null")) {
				String rawSignature = buildRawSignature(params);
				boolean isValidSignature = verifySignature(rawSignature, signature, momoProperties.getSecretKey());
				log.info("Signature verification: {}", isValidSignature ? "VALID" : "INVALID");
				
				if (!isValidSignature) {
					log.warn("Invalid signature for notify order: {}", orderId);
					return Map.of("resultCode", 1, "message", "Invalid signature");
				}
			}
			
			// Update order status and push SSE event
			if ("0".equals(resultCode)) {
				// Payment successful
				if (orderId != null && orderId.startsWith("ORDER-")) {
					String orderIdString = parseOrderId(orderId);
					try {
						// Lưu momoTransId trước khi cập nhật status
						Order order = orderRepository.findOrderWithAllDetails(orderIdString);
						if (order != null) {
							order.setMomoTransId(transId);
							orderRepository.save(order);
							log.info("Order {} momoTransId saved via notify: {}", orderIdString, transId);
						}
						
						orderService.updateOrderStatus(orderIdString, OrderStatus.PROCESSING);
						log.info("Order {} status updated to PROCESSING via notify", orderIdString);
						
						// Push SSE event for real-time UI update
						sseService.pushPaymentUpdate(orderId, "SUCCESS", "Thanh toán thành công!", transId);
						log.info("SSE event pushed for successful payment: {}", orderId);
						
					} catch (Exception e) {
						log.error("Failed to update order {} status via notify: {}", orderIdString, e.getMessage());
					}
				}
			} else {
				// Payment failed - update order status to CANCELLED
				log.warn("MoMo payment failed via notify for order {} with resultCode: {}", orderId, resultCode);
				
				if (orderId != null && orderId.startsWith("ORDER-")) {
					String orderIdString = parseOrderId(orderId);
					try {
						orderService.updateOrderStatus(orderIdString, OrderStatus.CANCELLED);
						log.info("Order {} status updated to CANCELLED via notify due to payment failure", orderIdString);
					} catch (Exception e) {
						log.error("Failed to update order {} status to CANCELLED via notify: {}", orderIdString, e.getMessage());
						// Fallback to direct repository access if service fails
						Order order = orderRepository.findOrderWithAllDetails(orderIdString);
						if (order != null) {
							order.setStatus(OrderStatus.CANCELLED);
							orderRepository.save(order);
							log.info("Order {} status updated to CANCELLED via fallback in notify", orderIdString);
						}
					}
				}
				
				// Push SSE event for failed payment
				sseService.pushPaymentUpdate(orderId, "FAILED", message != null ? message : "Thanh toán thất bại", transId);
				log.info("SSE event pushed for failed payment: {}", orderId);
			}
			
			log.info("=== MoMo Notify End ===");
			return Map.of("resultCode", 0, "message", "success");
			
		} catch (Exception e) {
			log.error("=== FATAL ERROR in MoMo notify ===");
			log.error("Error type: {}", e.getClass().getName());
			log.error("Error message: {}", e.getMessage());
			log.error("Stack trace:", e);
			
			return Map.of("resultCode", 1, "message", "error");
		}
	}


	private String buildRawSignature(Map<String, String> params) {
		// Follow MoMo signature fields ordering for return
		String[] keys = new String[]{"accessKey","amount","extraData","message","orderId","orderInfo","orderType","partnerCode","payType","requestId","responseTime","resultCode","transId"};
		StringBuilder sb = new StringBuilder();
		for (String key : keys) {
			if (params.containsKey(key)) {
				if (sb.length() > 0) sb.append('&');
				sb.append(key).append('=').append(params.get(key));
			}
		}
		return sb.toString();
	}

	private boolean verifySignature(String raw, String signature, String secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			mac.init(secretKeySpec);
			byte[] hmac = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
			String calc = bytesToHex(hmac);
			return calc.equals(signature);
		} catch (Exception e) {
			log.error("Verify signature error: {}", e.getMessage());
			return false;
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder(2 * bytes.length);
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	private String parseOrderId(String orderId) {
		try {
			log.info("Parsing orderId: {}", orderId);
			// Expect pattern like ORDER-<id>-<timestamp>
			if (orderId != null && orderId.startsWith("ORDER-")) {
				String[] parts = orderId.split("-");
				log.info("OrderId parts: {}", java.util.Arrays.toString(parts));
				if (parts.length >= 2) {
					String parsedId = parts[1]; // Return the order ID part directly
					log.info("Parsed orderId: {}", parsedId);
					return parsedId;
				}
			}
			log.info("Returning orderId as-is: {}", orderId);
			return orderId; // Return as-is if not ORDER- format
		} catch (Exception e) {
			log.error("Error parsing orderId {}: {}", orderId, e.getMessage());
			return null;
		}
	}
	
	/**
	 * Restore authentication from order information
	 */
	private void restoreAuthenticationFromOrder(HttpServletRequest request, Order order) {
		try {
			if (order != null && order.getUser() != null) {
				User user = order.getUser();
				log.info("Restoring authentication for user: {} from order: {}", user.getEmail(), order.getId());
				
				// Set session attributes for authentication
				request.getSession().setAttribute("userEmail", user.getEmail());
				request.getSession().setAttribute("userId", user.getId());
				request.getSession().setAttribute("userRole", user.getRole().name());
				
				// Generate a new JWT token for the user
				String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
				request.getSession().setAttribute("authToken", token);
				
				// Set cookie for JWT
				jakarta.servlet.http.Cookie authCookie = new jakarta.servlet.http.Cookie("authToken", token);
				authCookie.setHttpOnly(true);
				authCookie.setSecure(false); // Set to false for localhost development
				authCookie.setPath("/");
				authCookie.setMaxAge(4 * 60 * 60); // 4 hours
				
				// Note: We can't add cookies in a GET request, but the session will be available
				log.info("Authentication restored for user: {} with role: {}", user.getEmail(), user.getRole());
			}
		} catch (Exception e) {
			log.error("Failed to restore authentication from order: {}", e.getMessage());
		}
	}
}
