package com.example.demo.service;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderItemDTO;
import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.shipping.ShippingFeeResponse;
import com.example.demo.entity.*;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.entity.enums.PaymentMethod;
import com.example.demo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for managing orders
 * Handles business logic for order functionality
 */
@Service
@Transactional
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    @Autowired
    private OrderRepository orderRepository;
    
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private DeliveryUnitRepository deliveryUnitRepository;
    
    @Autowired
    private MoMoRefundService moMoRefundService;
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private ShippingService shippingService;

    @Autowired
    private OrderIdGeneratorService orderIdGeneratorService;
    
    @Autowired
    private com.example.demo.repository.ReviewRepository reviewRepository;
    
    @Autowired
    private ReviewService reviewService;
    
    /**
     * Create order from user's cart
     */
    public OrderResponse createOrderFromCart(Long userId, OrderRequest request) {
        try {
            logger.info("Creating order from cart for user: {}", userId);
            
            // Validate user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return OrderResponse.error("Người dùng không tồn tại");
            }
            
            User user = userOpt.get();
            
            // Get user's cart
            Cart cart = cartService.getOrCreateCart(userId);
            if (cart.getCartItems().isEmpty()) {
                return OrderResponse.error("Giỏ hàng trống");
            }
            
            // Validate address
            Optional<Address> addressOpt = addressRepository.findById(request.getAddressId());
            if (addressOpt.isEmpty()) {
                return OrderResponse.error("Địa chỉ không hợp lệ");
            }
            
            Address address = addressOpt.get();
            if (!address.getUser().getId().equals(userId)) {
                return OrderResponse.error("Địa chỉ không thuộc về người dùng này");
            }
            
            // Create order
            Order order = new Order(user, address, request.getPaymentMethod());
            order.setId(orderIdGeneratorService.generateOrderId());
            order.setNotes(request.getNotes());
            
            // Set delivery unit - default to GHN (ID = 2) if not provided
            Long deliveryUnitId = request.getDeliveryUnitId() != null ? request.getDeliveryUnitId() : 2L;
            logger.info("Setting delivery unit ID: {} (request provided: {})", deliveryUnitId, request.getDeliveryUnitId());
            Optional<DeliveryUnit> deliveryUnitOpt = deliveryUnitRepository.findById(deliveryUnitId);
            if (deliveryUnitOpt.isPresent()) {
                DeliveryUnit deliveryUnit = deliveryUnitOpt.get();
                order.setDeliveryUnit(deliveryUnit);
                logger.info("Successfully set delivery unit: {} (ID: {})", deliveryUnit.getName(), deliveryUnitId);
                logger.info("Order delivery unit after set: {}", order.getDeliveryUnit() != null ? order.getDeliveryUnit().getName() : "NULL");
            } else {
                logger.warn("Delivery unit with ID {} not found!", deliveryUnitId);
            }
            
            // Set voucher if provided
            if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
                Optional<Voucher> voucherOpt = voucherRepository.findByCode(request.getVoucherCode().trim());
                if (voucherOpt.isPresent()) {
                    Voucher voucher = voucherOpt.get();
                    if (voucher.isValid()) {
                        order.setVoucher(voucher);
                    } else {
                        return OrderResponse.error("Voucher không hợp lệ hoặc đã hết hạn");
                    }
                }
            }
            
            // Create order items from cart items
            // Filter cart items based on selected product IDs if provided
            List<CartItem> itemsToOrder = cart.getCartItems();
            if (request.getSelectedProductIds() != null && !request.getSelectedProductIds().isEmpty()) {
                itemsToOrder = cart.getCartItems().stream()
                    .filter(item -> request.getSelectedProductIds().contains(item.getProduct().getId()))
                    .collect(java.util.stream.Collectors.toList());
                logger.info("Processing {} selected items out of {} total cart items", 
                    itemsToOrder.size(), cart.getCartItems().size());
            }
            
            if (itemsToOrder.isEmpty()) {
                return OrderResponse.error("Không có sản phẩm nào được chọn để đặt hàng");
            }
            
            // Track ordered product IDs for cart cleanup
            List<Long> orderedProductIds = new java.util.ArrayList<>();
            
            for (CartItem cartItem : itemsToOrder) {
                Product product = cartItem.getProduct();
                
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    return OrderResponse.error("Sản phẩm '" + product.getName() + "' không đủ hàng trong kho");
                }
                
                // Create order item with current product price
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(product.getPrice());
                order.addOrderItem(orderItem);
                
                // Update product stock
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);
                
                // Track this product for removal from cart
                orderedProductIds.add(product.getId());
            }
            
            // Calculate shipping fee if address is GHN-compatible
            BigDecimal shippingFee = BigDecimal.ZERO;
            if (address.isGhnCompatible() && shippingService.isShippingServiceAvailable()) {
                try {
                    ShippingFeeResponse shippingResponse = shippingService.calculateShippingFee(
                        userId, 
                        address.getId(), 
                        cart.getCartItems(), 
                        request.getServiceTypeId()
                    );
                    
                    if (shippingResponse.success()) {
                        shippingFee = shippingResponse.shippingFee();
                        logger.info("Calculated shipping fee: {} for order", shippingFee);
                    } else {
                        logger.warn("Failed to calculate shipping fee: {}", shippingResponse.message());
                        // Continue with zero shipping fee instead of failing the order
                    }
                } catch (Exception e) {
                    logger.error("Error calculating shipping fee: {}", e.getMessage());
                    // Continue with zero shipping fee
                }
            } else {
                logger.info("Using legacy delivery unit or address not GHN-compatible");
            }
            
            // Calculate total amount with shipping fee
            order.calculateTotalAmountWithShippingFee(shippingFee);
            
            // Save order
            logger.info("Saving order with delivery unit: {}", order.getDeliveryUnit() != null ? order.getDeliveryUnit().getName() + " (ID: " + order.getDeliveryUnit().getId() + ")" : "NULL");
            order = orderRepository.save(order);
            orderRepository.flush(); // Force flush to database
            logger.info("Order saved successfully with ID: {}, delivery unit: {}", order.getId(), order.getDeliveryUnit() != null ? order.getDeliveryUnit().getName() : "NULL");
            
            // Update voucher usage count if voucher was used
            if (order.getVoucher() != null) {
                Voucher voucher = order.getVoucher();
                voucher.setUses(voucher.getUses() + 1);
                voucherRepository.save(voucher);
                logger.info("Updated voucher usage count for voucher {}: {} uses", voucher.getCode(), voucher.getUses());
            }
            
            // Remove only ordered items from cart (not the entire cart)
            if (!orderedProductIds.isEmpty()) {
                cartService.removeProductsFromCart(userId, orderedProductIds);
                logger.info("Removed {} ordered products from cart", orderedProductIds.size());
            }
            
            // Generate order number
            String orderNumber = generateOrderNumber(order.getId());
            
            logger.info("Successfully created order {} for user {}", order.getId(), userId);
            return OrderResponse.success("Đặt hàng thành công", OrderDTO.fromOrder(order), orderNumber);
            
        } catch (Exception e) {
            logger.error("Error creating order from cart for user {}: {}", userId, e.getMessage());
            return OrderResponse.error("Có lỗi xảy ra khi đặt hàng");
        }
    }
    
    /**
     * Create order directly (bypass cart)
     */
    public OrderResponse createOrderDirect(Long userId, OrderRequest request) {
        try {
            logger.info("Creating direct order for user: {}", userId);
            
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return OrderResponse.error("Danh sách sản phẩm không được để trống");
            }
            
            // Validate user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return OrderResponse.error("Người dùng không tồn tại");
            }
            
            User user = userOpt.get();
            
            // Validate address
            Optional<Address> addressOpt = addressRepository.findById(request.getAddressId());
            if (addressOpt.isEmpty()) {
                return OrderResponse.error("Địa chỉ không hợp lệ");
            }
            
            Address address = addressOpt.get();
            if (!address.getUser().getId().equals(userId)) {
                return OrderResponse.error("Địa chỉ không thuộc về người dùng này");
            }
            
            // Create order
            Order order = new Order(user, address, request.getPaymentMethod());
            order.setId(orderIdGeneratorService.generateOrderId());
            order.setNotes(request.getNotes());
            
            // Set delivery unit - default to GHN (ID = 2) if not provided
            Long deliveryUnitId = request.getDeliveryUnitId() != null ? request.getDeliveryUnitId() : 2L;
            logger.info("Setting delivery unit ID: {} (request provided: {})", deliveryUnitId, request.getDeliveryUnitId());
            Optional<DeliveryUnit> deliveryUnitOpt = deliveryUnitRepository.findById(deliveryUnitId);
            if (deliveryUnitOpt.isPresent()) {
                DeliveryUnit deliveryUnit = deliveryUnitOpt.get();
                order.setDeliveryUnit(deliveryUnit);
                logger.info("Successfully set delivery unit: {} (ID: {})", deliveryUnit.getName(), deliveryUnitId);
                logger.info("Order delivery unit after set: {}", order.getDeliveryUnit() != null ? order.getDeliveryUnit().getName() : "NULL");
            } else {
                logger.warn("Delivery unit with ID {} not found!", deliveryUnitId);
            }
            
            // Set voucher if provided
            if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
                Optional<Voucher> voucherOpt = voucherRepository.findByCode(request.getVoucherCode().trim());
                if (voucherOpt.isPresent()) {
                    Voucher voucher = voucherOpt.get();
                    if (voucher.isValid()) {
                        order.setVoucher(voucher);
                    } else {
                        return OrderResponse.error("Voucher không hợp lệ hoặc đã hết hạn");
                    }
                }
            }
            
            // Create order items
            for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
                Optional<Product> productOpt = productRepository.findById(itemRequest.getProductId());
                if (productOpt.isEmpty()) {
                    return OrderResponse.error("Sản phẩm không tồn tại");
                }
                
                Product product = productOpt.get();
                
                // Check stock availability
                if (product.getStockQuantity() < itemRequest.getQuantity()) {
                    return OrderResponse.error("Sản phẩm '" + product.getName() + "' không đủ hàng trong kho");
                }
                
                // Create order item with current product price
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setPrice(product.getPrice());
                order.addOrderItem(orderItem);
                
                // Update product stock
                product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
                productRepository.save(product);
            }
            
            // Calculate total amount
            order.calculateTotalAmount();
            
            // Save order
            order = orderRepository.save(order);
            
            // Update voucher usage count if voucher was used
            if (order.getVoucher() != null) {
                Voucher voucher = order.getVoucher();
                voucher.setUses(voucher.getUses() + 1);
                voucherRepository.save(voucher);
                logger.info("Updated voucher usage count for voucher {}: {} uses", voucher.getCode(), voucher.getUses());
            }
            
            // Generate order number
            String orderNumber = generateOrderNumber(order.getId());
            
            logger.info("Successfully created direct order {} for user {}", order.getId(), userId);
            return OrderResponse.success("Đặt hàng thành công", OrderDTO.fromOrder(order), orderNumber);
            
        } catch (Exception e) {
            logger.error("Error creating direct order for user {}: {}", userId, e.getMessage());
            return OrderResponse.error("Có lỗi xảy ra khi đặt hàng");
        }
    }
    
    /**
     * Get user's orders with pagination
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getUserOrders(Long userId, Pageable pageable) {
        try {
            Page<Order> orders = orderRepository.findByUserId(userId, pageable);
            return orders.map(order -> {
                OrderDTO dto = OrderDTO.fromOrder(order);
                // Check if order has reviews
                boolean hasReview = reviewRepository.existsByOrderItemOrderId(order.getId());
                dto.setHasReview(hasReview);
                return dto;
            });
        } catch (Exception e) {
            logger.error("Error getting orders for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Có lỗi xảy ra khi lấy danh sách đơn hàng");
        }
    }
    
    /**
     * Get user's orders by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getUserOrdersByStatus(Long userId, OrderStatus status, Pageable pageable) {
        try {
            Page<Order> orders = orderRepository.findByUserIdAndStatus(userId, status, pageable);
            return orders.map(order -> {
                OrderDTO dto = OrderDTO.fromOrder(order);
                // Check if order has reviews
                boolean hasReview = reviewRepository.existsByOrderItemOrderId(order.getId());
                dto.setHasReview(hasReview);
                return dto;
            });
        } catch (Exception e) {
            logger.error("Error getting orders for user {} with status {}: {}", userId, status, e.getMessage());
            throw new RuntimeException("Có lỗi xảy ra khi lấy danh sách đơn hàng");
        }
    }
    
    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId, Long userId) {
        try {
            logger.info("Getting order with ID: {} for user: {}", orderId, userId);
            Optional<Order> orderOpt = Optional.ofNullable(orderRepository.findOrderWithItems(orderId));
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found with ID: {}", orderId);
                return OrderResponse.error("Đơn hàng không tồn tại");
            }
            
            Order order = orderOpt.get();
            logger.info("Order found: ID={}, User ID={}, Status={}", order.getId(), order.getUser().getId(), order.getStatus());
            
            // Check if order belongs to user
            if (!order.getUser().getId().equals(userId)) {
                logger.warn("Order {} does not belong to user {}", orderId, userId);
                return OrderResponse.error("Bạn không có quyền xem đơn hàng này");
            }
            
            // Convert to DTO and add review information for each order item
            OrderDTO orderDTO = OrderDTO.fromOrder(order);
            
            // Add review information for each order item
            if (orderDTO.getOrderItems() != null) {
                for (OrderItemDTO item : orderDTO.getOrderItems()) {
                    // Check if this order item can be reviewed
                    String reviewReason = reviewService.canUserReviewOrderItemWithReason(userId, item.getId());
                    item.setCanReview(reviewReason == null);
                    item.setReviewReason(reviewReason);
                    
                    // Check if this order item has been reviewed
                    boolean reviewed = reviewRepository.existsByOrderItemId(item.getId());
                    item.setReviewed(reviewed);
                }
            }
            
            logger.info("Successfully retrieved order {} for user {} with review info", orderId, userId);
            return OrderResponse.success("Lấy thông tin đơn hàng thành công", orderDTO);
            
        } catch (Exception e) {
            logger.error("Error getting order {} for user {}: {}", orderId, userId, e.getMessage());
            return OrderResponse.error("Có lỗi xảy ra khi lấy thông tin đơn hàng");
        }
    }
    
    /**
     * Create order from cart and return Order entity (for internal use)
     */
    public Order createOrderFromCartEntity(Long userId, OrderRequest request) {
        try {
            logger.info("Creating order from cart for user: {}", userId);
            
            // Validate user
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("Người dùng không tồn tại");
            }
            
            User user = userOpt.get();
            
            // Get user's cart
            Cart cart = cartService.getOrCreateCart(userId);
            if (cart.getCartItems().isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống");
            }
            
            // Get user's address
            Address address;
            if (request.getAddressId() != null) {
                // Use specified address
                Optional<Address> addressOpt = addressRepository.findById(request.getAddressId());
                if (addressOpt.isEmpty()) {
                    throw new RuntimeException("Địa chỉ không tồn tại");
                }
                address = addressOpt.get();
                // Verify address belongs to user
                if (!address.getUser().getId().equals(userId)) {
                    throw new RuntimeException("Địa chỉ không thuộc về người dùng này");
                }
            } else {
                // Use default address
                address = user.getAddresses().stream()
                    .filter(Address::getIsDefault)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ mặc định. Vui lòng thêm địa chỉ giao hàng."));
            }
            
            // Create order
            Order order = new Order(user, address, request.getPaymentMethod());
            order.setId(orderIdGeneratorService.generateOrderId());
            order.setNotes(request.getNotes());
            
            // Set delivery unit - default to GHN (ID = 2) if not provided
            Long deliveryUnitId = request.getDeliveryUnitId() != null ? request.getDeliveryUnitId() : 2L;
            logger.info("Setting delivery unit ID: {} (request provided: {})", deliveryUnitId, request.getDeliveryUnitId());
            Optional<DeliveryUnit> deliveryUnitOpt = deliveryUnitRepository.findById(deliveryUnitId);
            if (deliveryUnitOpt.isPresent()) {
                DeliveryUnit deliveryUnit = deliveryUnitOpt.get();
                order.setDeliveryUnit(deliveryUnit);
                logger.info("Successfully set delivery unit: {} (ID: {})", deliveryUnit.getName(), deliveryUnitId);
                logger.info("Order delivery unit after set: {}", order.getDeliveryUnit() != null ? order.getDeliveryUnit().getName() : "NULL");
            } else {
                logger.warn("Delivery unit with ID {} not found!", deliveryUnitId);
            }
            
            // Set voucher if provided
            if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
                logger.info("Processing voucher code: {}", request.getVoucherCode());
                Optional<Voucher> voucherOpt = voucherRepository.findByCode(request.getVoucherCode().trim());
                if (voucherOpt.isPresent()) {
                    Voucher voucher = voucherOpt.get();
                    if (voucher.isValid()) {
                        order.setVoucher(voucher);
                        logger.info("Successfully set voucher: {} (ID: {})", voucher.getCode(), voucher.getId());
                    } else {
                        logger.warn("Voucher {} is not valid (expired, used up, or inactive)", request.getVoucherCode());
                        throw new RuntimeException("Voucher không hợp lệ hoặc đã hết hạn");
                    }
                } else {
                    logger.warn("Voucher with code {} not found", request.getVoucherCode());
                    throw new RuntimeException("Không tìm thấy voucher với mã: " + request.getVoucherCode());
                }
            }
            
            // Create order items from cart items
            // Filter cart items based on selected product IDs if provided
            List<CartItem> itemsToOrder = cart.getCartItems();
            if (request.getSelectedProductIds() != null && !request.getSelectedProductIds().isEmpty()) {
                itemsToOrder = cart.getCartItems().stream()
                    .filter(item -> request.getSelectedProductIds().contains(item.getProduct().getId()))
                    .collect(java.util.stream.Collectors.toList());
                logger.info("Processing {} selected items out of {} total cart items", 
                    itemsToOrder.size(), cart.getCartItems().size());
            }
            
            if (itemsToOrder.isEmpty()) {
                throw new RuntimeException("Không có sản phẩm nào được chọn để đặt hàng");
            }
            
            // Track ordered product IDs for cart cleanup
            List<Long> orderedProductIds = new java.util.ArrayList<>();
            
            for (CartItem cartItem : itemsToOrder) {
                Product product = cartItem.getProduct();
                
                // Check stock availability
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ hàng trong kho");
                }
                
                // Create order item with current product price
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(product.getPrice());
                order.addOrderItem(orderItem);
                
                // Update product stock
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);
                
                // Track this product for removal from cart
                orderedProductIds.add(product.getId());
            }
            
            // Calculate shipping fee if address is GHN-compatible
            BigDecimal shippingFee = BigDecimal.ZERO;
            if (address.isGhnCompatible() && shippingService.isShippingServiceAvailable()) {
                try {
                    ShippingFeeResponse shippingResponse = shippingService.calculateShippingFee(
                        userId, 
                        address.getId(), 
                        cart.getCartItems(), 
                        request.getServiceTypeId()
                    );
                    
                    if (shippingResponse.success()) {
                        shippingFee = shippingResponse.shippingFee();
                        logger.info("Calculated shipping fee: {} for order", shippingFee);
                    } else {
                        logger.warn("Failed to calculate shipping fee: {}", shippingResponse.message());
                        // Continue with zero shipping fee instead of failing the order
                    }
                } catch (Exception e) {
                    logger.error("Error calculating shipping fee: {}", e.getMessage());
                    // Continue with zero shipping fee
                }
            } else {
                logger.info("Using legacy delivery unit or address not GHN-compatible");
            }
            
            // Calculate total amount with shipping fee
            order.calculateTotalAmountWithShippingFee(shippingFee);
            
            // Save order
            Order savedOrder = orderRepository.save(order);
            
            // Update voucher usage count if voucher was used
            if (savedOrder.getVoucher() != null) {
                Voucher voucher = savedOrder.getVoucher();
                voucher.setUses(voucher.getUses() + 1);
                voucherRepository.save(voucher);
                logger.info("Updated voucher usage count for voucher {}: {} uses", voucher.getCode(), voucher.getUses());
            }
            
            // Remove only ordered items from cart (not the entire cart)
            if (!orderedProductIds.isEmpty()) {
                cartService.removeProductsFromCart(userId, orderedProductIds);
                logger.info("Removed {} ordered products from cart", orderedProductIds.size());
            }
            
            logger.info("Order created successfully with ID: {}", savedOrder.getId());
            return savedOrder;
            
        } catch (Exception e) {
            logger.error("Error creating order from cart for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Có lỗi xảy ra khi tạo đơn hàng: " + e.getMessage());
        }
    }
    
    /**
     * Get order entity by ID and user ID (for internal use)
     */
    @Transactional(readOnly = true)
    public Order getOrderEntity(String orderId, Long userId) {
        try {
            Optional<Order> orderOpt = Optional.ofNullable(orderRepository.findOrderWithItems(orderId));
            if (orderOpt.isEmpty()) {
                return null;
            }
            
            Order order = orderOpt.get();
            
            // Check if order belongs to user
            if (!order.getUser().getId().equals(userId)) {
                return null;
            }
            
            return order;
            
        } catch (Exception e) {
            logger.error("Error getting order entity {} for user {}: {}", orderId, userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Confirm order received by user (Shopee-like logic)
     * User confirms they have received the order, enabling review functionality
     */
    @Transactional
    public OrderResponse confirmOrderReceived(String orderId, Long userId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return OrderResponse.error("Đơn hàng không tồn tại");
            }
            
            Order order = orderOpt.get();
            
            // Check if order belongs to user
            if (!order.getUser().getId().equals(userId)) {
                return OrderResponse.error("Bạn không có quyền xác nhận đơn hàng này");
            }
            
            // Check if order is in COMPLETED status
            if (order.getStatus() != OrderStatus.COMPLETED) {
                return OrderResponse.error("Chỉ có thể xác nhận đơn hàng ở trạng thái Hoàn thành");
            }
            
            // Update order status to RECEIVED
            order.setStatus(OrderStatus.RECEIVED);
            order.setReceivedAt(LocalDateTime.now());
            
            orderRepository.save(order);
            
            logger.info("User {} confirmed received order {}", userId, orderId);
            return OrderResponse.success("Xác nhận đã nhận hàng thành công. Bạn có thể đánh giá đơn hàng này.", OrderDTO.fromOrder(order));
            
        } catch (Exception e) {
            logger.error("Error confirming order received {} for user {}: {}", orderId, userId, e.getMessage());
            return OrderResponse.error("Có lỗi xảy ra khi xác nhận đơn hàng");
        }
    }
    
    /**
     * Cancel order
     */
    public OrderResponse cancelOrder(String orderId, Long userId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return OrderResponse.error("Đơn hàng không tồn tại");
            }
            
            Order order = orderOpt.get();
            
            // Check if order belongs to user
            if (!order.getUser().getId().equals(userId)) {
                return OrderResponse.error("Bạn không có quyền hủy đơn hàng này");
            }
            
            // Check if order can be cancelled (only PENDING or PROCESSING orders can be cancelled)
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
                return OrderResponse.error("Chỉ có thể hủy đơn hàng ở trạng thái Chờ xử lý hoặc Đang xử lý");
            }
            
            // Check if order has been paid and needs refund
            boolean needsRefund = order.getStatus() == OrderStatus.PROCESSING && 
                                order.getPaymentMethod() == PaymentMethod.MOMO;
            
            String cancelReason = "Khách hàng hủy đơn hàng";
            
            // Process refund if needed
            if (needsRefund) {
                try {
                    logger.info("Processing refund for cancelled order: {}", orderId);
                    moMoRefundService.processRefund(order, cancelReason);
                    logger.info("Refund processed successfully for order: {}", orderId);
                } catch (Exception e) {
                    logger.error("Error processing refund for order {}: {}", orderId, e.getMessage(), e);
                    // Continue with cancellation even if refund fails
                    // Admin can handle refund manually later
                }
            }
            
            // Update order status
            order.setStatus(OrderStatus.CANCELLED);
            
            // Restore product stock
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
                productRepository.save(product);
            }
            
            orderRepository.save(order);
            
            String successMessage = needsRefund ? 
                "Hủy đơn hàng thành công. Hoàn tiền đang được xử lý." : 
                "Hủy đơn hàng thành công";
            
            logger.info("Successfully cancelled order {} for user {}", orderId, userId);
            return OrderResponse.success(successMessage, OrderDTO.fromOrder(order));
            
        } catch (Exception e) {
            logger.error("Error cancelling order {} for user {}: {}", orderId, userId, e.getMessage());
            return OrderResponse.error("Có lỗi xảy ra khi hủy đơn hàng");
        }
    }
    
    /**
     * Cancel order due to payment failure
     * This method is used by payment gateway callbacks to cancel orders and restore stock
     * when payment fails. It bypasses user ownership checks since it's triggered by payment system.
     */
    public void cancelOrderByPaymentFailure(String orderId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                logger.error("Order {} not found for payment failure cancellation", orderId);
                return;
            }
            
            Order order = orderOpt.get();
            
            // Only cancel if order is in PENDING status (waiting for payment)
            if (order.getStatus() != OrderStatus.PENDING) {
                logger.warn("Order {} is not in PENDING status, current status: {}", orderId, order.getStatus());
                return;
            }
            
            // Update order status to CANCELLED
            order.setStatus(OrderStatus.CANCELLED);
            
            // Restore product stock
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
                productRepository.save(product);
                logger.info("Restored {} units of product {} (ID: {}) due to payment failure", 
                    orderItem.getQuantity(), product.getName(), product.getId());
            }
            
            orderRepository.save(order);
            logger.info("Successfully cancelled order {} due to payment failure and restored stock", orderId);
            
        } catch (Exception e) {
            logger.error("Error cancelling order {} due to payment failure: {}", orderId, e.getMessage(), e);
        }
    }
    
    /**
     * Generate order number (legacy method - now orderId is already in correct format)
     */
    private String generateOrderNumber(String orderId) {
        // Order ID is already in DDMMYYXXX format, so just return it
        return orderId;
    }
    
    /**
     * Create order with payment processing (transactional)
     */
    @Transactional
    public Map<String, Object> createOrderWithPayment(Long userId, OrderRequest orderRequest, 
                                                     com.example.demo.entity.enums.PaymentMethod paymentMethod,
                                                     PaymentService paymentService) {
        try {
            // Create order
            Order order = createOrderFromCartEntity(userId, orderRequest);
            logger.info("Order created successfully with ID: {}", order.getId());
            
            // Process payment
            PaymentService.PaymentResult paymentResult = paymentService.processPayment(order, paymentMethod);
            
            Map<String, Object> result = new HashMap<>();
            result.put("order", OrderDTO.fromOrder(order));
            result.put("payment", paymentResult);
            result.put("success", paymentResult.isSuccess());
            
            if (!paymentResult.isSuccess()) {
                // If payment fails, the transaction will rollback automatically
                throw new RuntimeException("Payment failed: " + paymentResult.getMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error in createOrderWithPayment for user {}: {}", userId, e.getMessage(), e);
            throw e; // Re-throw to trigger rollback
        }
    }

    // ==================== ADMIN ORDER MANAGEMENT METHODS ====================

    /**
     * Get order by ID for admin
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(String orderId) {
        logger.debug("Getting order by ID: {}", orderId);
        try {
            Order order = orderRepository.findOrderWithAllDetails(orderId);
            if (order == null) {
                return null;
            }
            return OrderDTO.fromOrder(order);
        } catch (Exception e) {
            logger.error("Error getting order by ID {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to get order", e);
        }
    }
    
    /**
     * Get order entity by ID
     */
    @Transactional(readOnly = true)
    public Order getOrderEntityById(String orderId) {
        logger.debug("Getting order entity by ID: {}", orderId);
        try {
            return orderRepository.findOrderWithAllDetails(orderId);
        } catch (Exception e) {
            logger.error("Error getting order entity by ID {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to get order entity", e);
        }
    }

    /**
     * Get all orders with pagination for admin
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        logger.debug("Getting all orders with pagination: {}", pageable);
        try {
            Page<Order> orders = orderRepository.findAll(pageable);
            return orders.map(OrderDTO::fromOrder);
        } catch (Exception e) {
            logger.error("Error getting all orders: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get orders", e);
        }
    }

    /**
     * Get orders between dates with pagination
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderDTO> getOrdersBetweenDates(java.time.LocalDateTime start,
                                                                                java.time.LocalDateTime end,
                                                                                org.springframework.data.domain.Pageable pageable) {
        logger.debug("Getting orders between {} and {} with pagination {}", start, end, pageable);
        try {
            java.util.List<com.example.demo.entity.Order> list = orderRepository.findOrdersBetweenDates(start, end);
            // Manual pagination since repository returns List
            int from = (int) pageable.getOffset();
            int to = Math.min(from + pageable.getPageSize(), list.size());
            java.util.List<com.example.demo.entity.Order> pageContent = from > to ? java.util.List.of() : list.subList(from, to);
            java.util.List<OrderDTO> mapped = pageContent.stream().map(OrderDTO::fromOrder).toList();
            return new org.springframework.data.domain.PageImpl<>(mapped, pageable, list.size());
        } catch (Exception e) {
            logger.error("Error getting orders between dates: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get orders between dates", e);
        }
    }

    /**
     * Get orders by status with pagination
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        logger.debug("Getting orders by status: {} with pagination: {}", status, pageable);
        try {
            Page<Order> orders = orderRepository.findByStatus(status, pageable);
            return orders.map(OrderDTO::fromOrder);
        } catch (Exception e) {
            logger.error("Error getting orders by status {}: {}", status, e.getMessage(), e);
            throw new RuntimeException("Failed to get orders by status", e);
        }
    }

    /**
     * Search orders by order number, customer name, or email
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> searchOrders(String searchTerm, Pageable pageable) {
        logger.debug("Searching orders with term: {} and pagination: {}", searchTerm, pageable);
        try {
            Page<Order> orders = orderRepository.searchOrders(searchTerm, pageable);
            return orders.map(OrderDTO::fromOrder);
        } catch (Exception e) {
            logger.error("Error searching orders with term {}: {}", searchTerm, e.getMessage(), e);
            throw new RuntimeException("Failed to search orders", e);
        }
    }

    /**
     * Update order status (Admin function)
     */
    @Transactional
    public OrderDTO updateOrderStatus(String orderId, OrderStatus newStatus) {
        logger.debug("Updating order {} status to {}", orderId, newStatus);
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            // Validate status transition
            if (!isValidStatusTransition(order.getStatus(), newStatus)) {
                throw new RuntimeException("Invalid status transition from " + order.getStatus() + " to " + newStatus);
            }

            order.setStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());
            
            Order savedOrder = orderRepository.save(order);
            logger.info("Order {} status updated to {}", orderId, newStatus);
            
            return OrderDTO.fromOrder(savedOrder);
            
        } catch (Exception e) {
            logger.error("Error updating order status for order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get order statistics for admin dashboard
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatistics() {
        logger.debug("Getting order statistics");
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total orders
            stats.put("totalOrders", orderRepository.count());
            
            // Orders by status
            for (OrderStatus status : OrderStatus.values()) {
                stats.put(status.name().toLowerCase() + "Orders", orderRepository.countByStatus(status));
            }
            
            // Total revenue (completed orders only)
            BigDecimal totalRevenue = orderRepository.getTotalRevenue();
            stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            
            // Monthly revenue
            BigDecimal monthlyRevenue = orderRepository.getMonthlyRevenue();
            stats.put("monthlyRevenue", monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);
            
            // Recent orders count (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            List<Order> recentOrders = orderRepository.findOrdersBetweenDates(weekAgo, LocalDateTime.now());
            stats.put("recentOrdersCount", recentOrders.size());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting order statistics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get order statistics", e);
        }
    }

    /**
     * Admin cancel order with reason
     */
    @Transactional
    public OrderDTO adminCancelOrder(String orderId, String reason) {
        logger.debug("Admin cancelling order {} with reason: {}", orderId, reason);
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            // Check if order can be cancelled
            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
                throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
            }

            // Check if order has been paid and needs refund
            boolean needsRefund = (order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.SHIPPED) && 
                                order.getPaymentMethod() == PaymentMethod.MOMO;
            
            String adminReason = "Admin hủy đơn hàng: " + reason;
            
            // Process refund if needed
            if (needsRefund) {
                try {
                    logger.info("Processing refund for admin cancelled order: {}", orderId);
                    moMoRefundService.processRefund(order, adminReason);
                    logger.info("Refund processed successfully for admin cancelled order: {}", orderId);
                } catch (Exception e) {
                    logger.error("Error processing refund for admin cancelled order {}: {}", orderId, e.getMessage(), e);
                    // Continue with cancellation even if refund fails
                    // Admin can handle refund manually later
                }
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + reason : reason);
            order.setUpdatedAt(LocalDateTime.now());
            
            // Restore product stock
            restoreProductStock(order);
            
            Order savedOrder = orderRepository.save(order);
            logger.info("Order {} cancelled by admin with reason: {}", orderId, reason);
            
            return OrderDTO.fromOrder(savedOrder);
            
        } catch (Exception e) {
            logger.error("Error admin cancelling order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validate status transition
     */
    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }
        
        switch (currentStatus) {
            case PENDING:
                return newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING:
                return newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED:
                return newStatus == OrderStatus.COMPLETED;
            case COMPLETED:
            case CANCELLED:
                return false; // Final states
            default:
                return false;
        }
    }

    /**
     * Restore product stock when order is cancelled
     */
    private void restoreProductStock(Order order) {
        try {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
                logger.debug("Restored {} units of product {} (ID: {})", 
                           item.getQuantity(), product.getName(), product.getId());
            }
        } catch (Exception e) {
            logger.error("Error restoring product stock for order {}: {}", order.getId(), e.getMessage(), e);
            // Don't throw exception here as it's a side effect
        }
    }

    /**
     * Create order directly (Admin function)
     */
    @Transactional
    public OrderDTO createOrderDirect(com.example.demo.controller.AdminOrderController.CreateOrderRequest request) {
        try {
            logger.info("Creating order directly via admin for user: {}", request.getUserId());
            
            // Validate user
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                throw new RuntimeException("Người dùng không tồn tại");
            }
            User user = userOpt.get();
            
            // Get user's default address
            Address address = user.getAddresses().stream()
                .filter(Address::getIsDefault)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ mặc định. Vui lòng thêm địa chỉ giao hàng cho khách hàng."));
            
            // Create order
            com.example.demo.entity.enums.PaymentMethod paymentMethodEnum;
            try {
                paymentMethodEnum = com.example.demo.entity.enums.PaymentMethod.valueOf(request.getPaymentMethod());
            } catch (IllegalArgumentException e) {
                // Default to COD if invalid payment method
                paymentMethodEnum = com.example.demo.entity.enums.PaymentMethod.COD;
                logger.warn("Invalid payment method: {}, defaulting to COD", request.getPaymentMethod());
            }
            
            Order order = new Order(user, address, paymentMethodEnum);
            order.setId(orderIdGeneratorService.generateOrderId());
            order.setNotes(request.getNotes());
            
            // Set delivery unit - default to GHN (ID = 2)
            Long deliveryUnitId = 2L;
            Optional<DeliveryUnit> deliveryUnitOpt = deliveryUnitRepository.findById(deliveryUnitId);
            if (deliveryUnitOpt.isPresent()) {
                DeliveryUnit deliveryUnit = deliveryUnitOpt.get();
                order.setDeliveryUnit(deliveryUnit);
                logger.info("Successfully set delivery unit: {} (ID: {})", deliveryUnit.getName(), deliveryUnitId);
            } else {
                logger.warn("Delivery unit with ID {} not found!", deliveryUnitId);
            }
            
            // Set voucher if provided
            if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
                logger.info("Processing voucher code: {}", request.getVoucherCode());
                Optional<Voucher> voucherOpt = voucherRepository.findByCode(request.getVoucherCode().trim());
                if (voucherOpt.isPresent()) {
                    Voucher voucher = voucherOpt.get();
                    if (voucher.isValid()) {
                        order.setVoucher(voucher);
                        logger.info("Successfully set voucher: {} (ID: {})", voucher.getCode(), voucher.getId());
                    } else {
                        logger.warn("Voucher {} is not valid (expired, used up, or inactive)", request.getVoucherCode());
                        throw new RuntimeException("Voucher không hợp lệ hoặc đã hết hạn");
                    }
                } else {
                    logger.warn("Voucher with code {} not found", request.getVoucherCode());
                    throw new RuntimeException("Không tìm thấy voucher với mã: " + request.getVoucherCode());
                }
            }
            
            // Create order items
            for (com.example.demo.controller.AdminOrderController.OrderItemRequest itemRequest : request.getOrderItems()) {
                Optional<Product> productOpt = productRepository.findById(itemRequest.getProductId());
                if (productOpt.isEmpty()) {
                    throw new RuntimeException("Sản phẩm không tồn tại với ID: " + itemRequest.getProductId());
                }
                
                Product product = productOpt.get();
                
                // Check stock availability
                if (product.getStockQuantity() < itemRequest.getQuantity()) {
                    throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ hàng trong kho");
                }
                
                // Create order item with current product price
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setPrice(product.getPrice());
                order.addOrderItem(orderItem);
                
                // Update product stock
                product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
                productRepository.save(product);
            }
            
            // Calculate total amount (no shipping fee calculation for admin direct orders)
            order.calculateTotalAmount();
            
            // Save order
            Order savedOrder = orderRepository.save(order);
            
            // Update voucher usage count if voucher was used
            if (savedOrder.getVoucher() != null) {
                Voucher voucher = savedOrder.getVoucher();
                voucher.setUses(voucher.getUses() + 1);
                voucherRepository.save(voucher);
                logger.info("Updated voucher usage count for voucher {}: {} uses", voucher.getCode(), voucher.getUses());
            }
            
            logger.info("Successfully created direct order {} for user {}", savedOrder.getId(), request.getUserId());
            return OrderDTO.fromOrder(savedOrder);
            
        } catch (Exception e) {
            logger.error("Error creating direct order: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Reorder from previous order - add items to cart
     */
    @Transactional
    public Map<String, Object> reorderFromPreviousOrder(String orderId, Long userId) {
        logger.info("Processing reorder for order {} by user {}", orderId, userId);
        
        try {
            // Get the original order
            Order originalOrder = orderRepository.findOrderWithAllDetails(orderId);
            if (originalOrder == null) {
                return Map.of("success", false, "message", "Không tìm thấy đơn hàng");
            }
            
            // Check if order belongs to user
            if (!originalOrder.getUser().getId().equals(userId)) {
                return Map.of("success", false, "message", "Bạn không có quyền mua lại đơn hàng này");
            }
            
            // Check if order can be reordered (ONLY RECEIVED orders)
            if (originalOrder.getStatus() != OrderStatus.RECEIVED) {
                return Map.of("success", false, "message", "Chỉ có thể mua lại đơn hàng đã nhận hàng");
            }
            
            // Get user's cart
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return Map.of("success", false, "message", "Người dùng không tồn tại");
            }
            
            // Build map of existing cart items to merge quantities (do NOT clear cart)
            var cartResponse = cartService.getCart(userId);
            java.util.Map<Long, Integer> existingCartItems = new java.util.HashMap<>();
            if (cartResponse.isSuccess() && cartResponse.getCart() != null && cartResponse.getCart().getItems() != null) {
                for (var item : cartResponse.getCart().getItems()) {
                    existingCartItems.put(item.getProductId(), item.getQuantity());
                }
            }

            java.util.List<String> addedProducts = new java.util.ArrayList<>();
            java.util.List<Long> addedProductIds = new java.util.ArrayList<>();
            java.util.List<String> unavailableProducts = new java.util.ArrayList<>();
            java.util.List<String> outOfStockProducts = new java.util.ArrayList<>();
            
            // Process each order item
            for (OrderItem orderItem : originalOrder.getOrderItems()) {
                Product product = orderItem.getProduct();
                
                // Check if product is still active
                if (product.getStatus() != com.example.demo.entity.enums.ProductStatus.ACTIVE) {
                    unavailableProducts.add(product.getName());
                    continue;
                }
                
                // Check stock availability
                if (product.getStockQuantity() < orderItem.getQuantity()) {
                    outOfStockProducts.add(product.getName() + " (còn " + product.getStockQuantity() + " sản phẩm)");
                    continue;
                }
                
                // Merge with existing cart quantity if present
                if (existingCartItems.containsKey(product.getId())) {
                    int existingQuantity = existingCartItems.get(product.getId());
                    int newQuantity = existingQuantity + orderItem.getQuantity();
                    if (newQuantity > product.getStockQuantity()) {
                        outOfStockProducts.add(product.getName() + " (tổng số lượng vượt quá kho)");
                        continue;
                    }
                    var updateResponse = cartService.updateCartItem(userId, product.getId(), newQuantity);
                    if (updateResponse.isSuccess()) {
                        addedProducts.add(product.getName() + " (cập nhật số lượng)");
                        addedProductIds.add(product.getId());
                    } else {
                        outOfStockProducts.add(product.getName() + " (lỗi cập nhật)");
                    }
                } else {
                    // Add as new item
                    var addResponse = cartService.addToCart(userId, product.getId(), orderItem.getQuantity());
                    if (addResponse.isSuccess()) {
                        addedProducts.add(product.getName());
                        addedProductIds.add(product.getId());
                    } else {
                        outOfStockProducts.add(product.getName() + " (lỗi thêm vào giỏ)");
                    }
                }
            }
            
            // Prepare result message - only show success message
            String message = "";
            if (!addedProducts.isEmpty()) {
                message = "Đã thêm vào giỏ hàng: " + String.join(", ", addedProducts);
            }
            
            boolean success = !addedProducts.isEmpty();
            if (success) {
                logger.info("Successfully reordered {} products for user {} from order {}", 
                           addedProducts.size(), userId, orderId);
            } else {
                logger.warn("No products could be reordered for user {} from order {}", userId, orderId);
            }
            
            return Map.of(
                "success", success,
                "message", message.toString(),
                "addedProducts", addedProducts,
                "unavailableProducts", unavailableProducts,
                "outOfStockProducts", outOfStockProducts,
                "addedProductIds", addedProductIds,
                "redirectUrl", "/checkout"
            );
            
        } catch (Exception e) {
            logger.error("Error processing reorder for order {}: {}", orderId, e.getMessage(), e);
            return Map.of("success", false, "message", "Có lỗi xảy ra khi mua lại đơn hàng: " + e.getMessage());
        }
    }
}
