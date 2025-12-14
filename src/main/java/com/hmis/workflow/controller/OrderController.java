package com.hmis.workflow.controller;

import com.hmis.workflow.domain.entity.Order;
import com.hmis.workflow.domain.enums.OrderStatus;
import com.hmis.workflow.domain.enums.OrderType;
import com.hmis.workflow.dto.ApiResponse;
import com.hmis.workflow.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for order management
 * Provides endpoints for managing orders in workflow instances
 */
@RestController
@RequestMapping("/workflows/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order
     * POST /workflows/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            @RequestBody CreateOrderRequest request) {
        log.info("Creating new order: {} for workflow: {}",
                request.getOrderDescription(), request.getWorkflowInstanceId());

        Order order = Order.builder()
                .status(OrderStatus.PROPOSED)
                .orderType(request.getOrderType())
                .orderDescription(request.getOrderDescription())
                .orderCode(request.getOrderCode())
                .departmentTarget(request.getDepartmentTarget())
                .estimatedCost(request.getEstimatedCost())
                .priority(request.getPriority())
                .build();

        Order created = orderService.createOrder(order);
        OrderDTO dto = mapToDTO(created);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Order created successfully"));
    }

    /**
     * Get order by ID
     * GET /workflows/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(@PathVariable UUID id) {
        log.info("Fetching order: {}", id);

        Order order = orderService.getOrder(id);
        OrderDTO dto = mapToDTO(order);

        return ResponseEntity.ok(ApiResponse.success(dto, "Order retrieved successfully"));
    }

    /**
     * Get all orders for a workflow instance
     * GET /workflows/orders/workflow/{workflowInstanceId}
     */
    @GetMapping("/workflow/{workflowInstanceId}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByWorkflow(
            @PathVariable UUID workflowInstanceId) {
        log.info("Fetching orders for workflow: {}", workflowInstanceId);

        List<Order> orders = orderService.getOrdersByWorkflow(workflowInstanceId);
        List<OrderDTO> dtos = orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Workflow orders retrieved successfully"));
    }

    /**
     * Get all open orders
     * GET /workflows/orders/status/open
     */
    @GetMapping("/status/open")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOpenOrders() {
        log.info("Fetching all open orders");

        List<Order> orders = orderService.getOpenOrders();
        List<OrderDTO> dtos = orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Open orders retrieved successfully"));
    }

    /**
     * Authorize order
     * POST /workflows/orders/{id}/authorize
     */
    @PostMapping("/{id}/authorize")
    public ResponseEntity<ApiResponse<OrderDTO>> authorizeOrder(
            @PathVariable UUID id,
            @RequestBody AuthorizeOrderRequest request) {
        log.info("Authorizing order: {} by: {}", id, request.getAuthorizedByUser());

        Order order = orderService.authorizeOrder(id, request.getAuthorizedByUser());
        OrderDTO dto = mapToDTO(order);

        return ResponseEntity.ok(ApiResponse.success(dto, "Order authorized successfully"));
    }

    /**
     * Activate order
     * POST /workflows/orders/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<OrderDTO>> activateOrder(@PathVariable UUID id) {
        log.info("Activating order: {}", id);

        Order order = orderService.activateOrder(id);
        OrderDTO dto = mapToDTO(order);

        return ResponseEntity.ok(ApiResponse.success(dto, "Order activated successfully"));
    }

    /**
     * Start order (mark as in progress)
     * POST /workflows/orders/{id}/start
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<OrderDTO>> startOrder(@PathVariable UUID id) {
        log.info("Starting order: {}", id);

        Order order = orderService.startOrder(id);
        OrderDTO dto = mapToDTO(order);

        return ResponseEntity.ok(ApiResponse.success(dto, "Order started successfully"));
    }

    /**
     * Record order result
     * POST /workflows/orders/{id}/result
     */
    @PostMapping("/{id}/result")
    public ResponseEntity<ApiResponse<OrderDTO>> resultOrder(
            @PathVariable UUID id,
            @RequestBody ResultOrderRequest request) {
        log.info("Recording result for order: {}", id);

        Order order = orderService.resultOrder(id, request.getResult());
        OrderDTO dto = mapToDTO(order);

        return ResponseEntity.ok(ApiResponse.success(dto, "Order result recorded successfully"));
    }

    /**
     * Verify order
     * POST /workflows/orders/{id}/verify
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<OrderDTO>> verifyOrder(
            @PathVariable UUID id,
            @RequestBody VerifyOrderRequest request) {
        log.info("Verifying order: {} by: {}", id, request.getVerifiedByUser());

        Order order = orderService.verifyOrder(id, request.getVerifiedByUser());
        OrderDTO dto = mapToDTO(order);

        return ResponseEntity.ok(ApiResponse.success(dto, "Order verified successfully"));
    }

    /**
     * Close order
     * POST /workflows/orders/{id}/close
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<OrderDTO>> closeOrder(@PathVariable UUID id) {
        log.info("Closing order: {}", id);

        Order order = orderService.closeOrder(id);
        OrderDTO dto = mapToDTO(order);

        return ResponseEntity.ok(ApiResponse.success(dto, "Order closed successfully"));
    }

    /**
     * Cancel order
     * POST /workflows/orders/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable UUID id,
            @RequestBody CancelOrderRequest request) {
        log.info("Cancelling order: {} - Reason: {}", id, request.getReason());

        Order order = orderService.cancelOrder(id, request.getReason());

        // Execute compensation actions
        orderService.executePendingCompensations(id);

        OrderDTO dto = mapToDTO(order);

        return ResponseEntity.ok(ApiResponse.success(dto, "Order cancelled successfully"));
    }

    /**
     * Get orders with results
     * GET /workflows/orders/status/resulted
     */
    @GetMapping("/status/resulted")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersWithResults() {
        log.info("Fetching all orders with results");

        List<Order> orders = orderService.getOrdersWithResults();
        List<OrderDTO> dtos = orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Orders with results retrieved successfully"));
    }

    // ==================== Helper Methods ====================

    private OrderDTO mapToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .orderType(order.getOrderType())
                .orderDescription(order.getOrderDescription())
                .orderCode(order.getOrderCode())
                .departmentTarget(order.getDepartmentTarget())
                .orderedByUser(order.getOrderedByUser())
                .authorizedByUser(order.getAuthorizedByUser())
                .authorizedAt(order.getAuthorizedAt())
                .activatedAt(order.getActivatedAt())
                .resultedAt(order.getResultedAt())
                .verifiedAt(order.getVerifiedAt())
                .closedAt(order.getClosedAt())
                .cancelledAt(order.getCancelledAt())
                .result(order.getResult())
                .estimatedCost(order.getEstimatedCost())
                .actualCost(order.getActualCost())
                .priority(order.getPriority())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    // ==================== Request/Response DTOs ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class CreateOrderRequest {
        private OrderType orderType;
        private String orderDescription;
        private String orderCode;
        private String departmentTarget;
        private UUID workflowInstanceId;
        private BigDecimal estimatedCost;
        private Integer priority;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class AuthorizeOrderRequest {
        private String authorizedByUser;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class ResultOrderRequest {
        private String result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class VerifyOrderRequest {
        private String verifiedByUser;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class CancelOrderRequest {
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class OrderDTO {
        private UUID id;
        private String orderId;
        private OrderStatus status;
        private OrderType orderType;
        private String orderDescription;
        private String orderCode;
        private String departmentTarget;
        private String orderedByUser;
        private String authorizedByUser;
        private java.time.LocalDateTime authorizedAt;
        private java.time.LocalDateTime activatedAt;
        private java.time.LocalDateTime resultedAt;
        private java.time.LocalDateTime verifiedAt;
        private java.time.LocalDateTime closedAt;
        private java.time.LocalDateTime cancelledAt;
        private String result;
        private BigDecimal estimatedCost;
        private BigDecimal actualCost;
        private Integer priority;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }
}
