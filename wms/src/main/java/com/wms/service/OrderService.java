package com.wms.service;

import com.wms.dto.request.CreateOrderRequest;
import com.wms.dto.request.DeclineOrderRequest;
import com.wms.dto.response.OrderResponse;
import com.wms.enums.OrderStatus;
import com.wms.enums.Role;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderService {
    // CLIENT
    OrderResponse createOrder(CreateOrderRequest request, String username);
    OrderResponse updateOrder(Long id, CreateOrderRequest request, String username);
    OrderResponse submitOrder(Long id, String username);
    OrderResponse cancelOrder(Long id, String username);
    List<OrderResponse> getMyOrders(String username, OrderStatus status);

    @Transactional(readOnly = true)
    OrderResponse getMyOrderById(Long id, String username);

    // WAREHOUSE MANAGER
    List<OrderResponse> getAllOrders(OrderStatus status);

    @Transactional(readOnly = true)
    OrderResponse getOrderById(Long id);

    OrderResponse approveOrder(Long id);
    OrderResponse declineOrder(Long id, DeclineOrderRequest request);
    OrderResponse fulfillOrder(Long id);

//    // SHARED — role-aware detail view used by both CLIENT and WAREHOUSE_MANAGER
//    OrderResponse getOrderDetail(Long id, String callerUsername, Role callerRole);
}
