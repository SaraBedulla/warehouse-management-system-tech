package com.wms.controller;

import com.wms.dto.request.DeclineOrderRequest;
import com.wms.dto.response.OrderResponse;
import com.wms.enums.OrderStatus;
import com.wms.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
@Tag(name = "Orders — Manager", description = "Order management for WAREHOUSE_MANAGER role: view all, approve, decline, fulfill.")
public class ManagerOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List all orders, optionally filtered by status")
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getAllOrders(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed order information")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an order (must be AWAITING_APPROVAL → APPROVED)")
    public ResponseEntity<OrderResponse> approveOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.approveOrder(id));
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Decline an order with a reason (must be AWAITING_APPROVAL → DECLINED)")
    public ResponseEntity<OrderResponse> declineOrder(@PathVariable Long id,
                                                       @Valid @RequestBody DeclineOrderRequest request) {
        return ResponseEntity.ok(orderService.declineOrder(id, request));
    }

    @PostMapping("/{id}/fulfill")
    @Operation(summary = "Fulfill an order (must be APPROVED → FULFILLED)")
    public ResponseEntity<OrderResponse> fulfillOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.fulfillOrder(id));
    }
}
