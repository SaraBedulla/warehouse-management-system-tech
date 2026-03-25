package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.dto.request.CreateOrderRequest;
import com.wms.dto.response.OrderResponse;
import com.wms.enums.OrderStatus;
import com.wms.exception.BusinessException;
import com.wms.security.JwtAuthEntryPoint;
import com.wms.security.JwtAuthFilter;
import com.wms.security.JwtUtils;
import com.wms.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController")
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean OrderService orderService;
    @MockBean JwtUtils jwtUtils;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean UserDetailsService userDetailsService;

    private final OrderResponse sampleOrder = OrderResponse.builder()
            .id(1L).orderNumber("ORD-00001")
            .clientUsername("client1").clientFullName("Alice")
            .status(OrderStatus.CREATED)
            .totalAmount(new BigDecimal("50.00"))
            .items(List.of()).availableActions(List.of()).build();

    @Test
    @DisplayName("POST /api/orders — returns 201 for CLIENT")
    @WithMockUser(username = "ana", roles = "CLIENT")
    void createOrder_success() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setItems(List.of());

        when(orderService.createOrder(any(), eq("ana"))).thenReturn(sampleOrder);

        mockMvc.perform(post("/api/orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-00001"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("POST /api/orders — returns 403 for WAREHOUSE_MANAGER")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void createOrder_asManager_forbidden() throws Exception {
        mockMvc.perform(post("/api/orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/orders — returns list for CLIENT")
    @WithMockUser(username = "ana", roles = "CLIENT")
    void getMyOrders() throws Exception {
        when(orderService.getMyOrders("ana", null)).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-00001"));
    }

    @Test
    @DisplayName("GET /api/orders?status=CREATED — returns filtered list")
    @WithMockUser(username = "client1", roles = "CLIENT")
    void getMyOrders_withStatusFilter() throws Exception {
        when(orderService.getMyOrders("client1", OrderStatus.CREATED)).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders").param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} — returns 200 for owner")
    @WithMockUser(username = "client1", roles = "CLIENT")
    void getMyOrderById() throws Exception {
        when(orderService.getMyOrderById(1L, "client1")).thenReturn(sampleOrder);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/orders/{id}/submit — returns 200 on success")
    @WithMockUser(username = "client1", roles = "CLIENT")
    void submitOrder_success() throws Exception {
        OrderResponse submitted = OrderResponse.builder()
                .id(1L).orderNumber("ORD-00001").status(OrderStatus.AWAITING_APPROVAL)
                .totalAmount(BigDecimal.ZERO).items(List.of()).availableActions(List.of()).build();

        when(orderService.submitOrder(1L, "client1")).thenReturn(submitted);

        mockMvc.perform(post("/api/orders/1/submit").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    @DisplayName("POST /api/orders/{id}/submit — returns 400 when order has no items")
    @WithMockUser(username = "client1", roles = "CLIENT")
    void submitOrder_noItems() throws Exception {
        when(orderService.submitOrder(1L, "client1"))
                .thenThrow(new BusinessException("Cannot submit an order with no items"));

        mockMvc.perform(post("/api/orders/1/submit").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot submit an order with no items"));
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel — returns 200 on success")
    @WithMockUser(username = "client1", roles = "CLIENT")
    void cancelOrder_success() throws Exception {
        OrderResponse canceled = OrderResponse.builder()
                .id(1L).orderNumber("ORD-00001").status(OrderStatus.CANCELED)
                .totalAmount(BigDecimal.ZERO).items(List.of()).availableActions(List.of()).build();

        when(orderService.cancelOrder(1L, "client1")).thenReturn(canceled);

        mockMvc.perform(post("/api/orders/1/cancel").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("GET /api/orders — returns 401 for unauthenticated")
    void getMyOrders_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }
}
