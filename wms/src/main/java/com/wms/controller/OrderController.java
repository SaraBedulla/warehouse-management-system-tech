package com.wms.controller;

import com.wms.dto.request.CreateOrderRequest;
import com.wms.dto.response.OrderResponse;
import com.wms.enums.OrderStatus;
import com.wms.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Orders — Client", description = "Order management for CLIENT role: create, update, submit, cancel, view own orders.")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List my orders, optionally filtered by status")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getMyOrders(userDetails.getUsername(), status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific order by ID")
    public ResponseEntity<OrderResponse> getMyOrderById(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getMyOrderById(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order items/deadline (only when CREATED or DECLINED)")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id,
                                                      @Valid @RequestBody CreateOrderRequest request,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.updateOrder(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit order for approval (status → AWAITING_APPROVAL)")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.submitOrder(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order (not allowed when FULFILLED or already CANCELED)")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.cancelOrder(id, userDetails.getUsername()));
    }
}
