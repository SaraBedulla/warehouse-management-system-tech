package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.dto.request.DeclineOrderRequest;
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

@WebMvcTest(ManagerOrderController.class)
@DisplayName("ManagerOrderController")
@AutoConfigureMockMvc(addFilters = false)
class ManagerOrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean OrderService orderService;
    @MockBean JwtUtils jwtUtils;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean UserDetailsService userDetailsService;

    private OrderResponse orderWithStatus(OrderStatus status) {
        return OrderResponse.builder()
                .id(1L).orderNumber("ORD-00001")
                .clientUsername("client1").clientFullName("Alice")
                .status(status).totalAmount(new BigDecimal("50.00"))
                .items(List.of()).availableActions(List.of()).build();
    }

    @Test
    @DisplayName("GET /api/manager/orders — returns all orders for WAREHOUSE_MANAGER")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void getAllOrders_success() throws Exception {
        when(orderService.getAllOrders(null)).thenReturn(List.of(orderWithStatus(OrderStatus.AWAITING_APPROVAL)));

        mockMvc.perform(get("/api/manager/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-00001"));
    }

    @Test
    @DisplayName("GET /api/manager/orders — returns 403 for CLIENT")
    @WithMockUser(roles = "CLIENT")
    void getAllOrders_asClient_forbidden() throws Exception {
        mockMvc.perform(get("/api/manager/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/manager/orders?status=AWAITING_APPROVAL — filters correctly")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void getAllOrders_withFilter() throws Exception {
        when(orderService.getAllOrders(OrderStatus.AWAITING_APPROVAL))
                .thenReturn(List.of(orderWithStatus(OrderStatus.AWAITING_APPROVAL)));

        mockMvc.perform(get("/api/manager/orders").param("status", "AWAITING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AWAITING_APPROVAL"));
    }

    @Test
    @DisplayName("GET /api/manager/orders/{id} — returns order detail")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void getOrderById_success() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderWithStatus(OrderStatus.AWAITING_APPROVAL));

        mockMvc.perform(get("/api/manager/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/manager/orders/{id}/approve — returns APPROVED status")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void approveOrder_success() throws Exception {
        when(orderService.approveOrder(1L)).thenReturn(orderWithStatus(OrderStatus.APPROVED));

        mockMvc.perform(post("/api/manager/orders/1/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/manager/orders/{id}/approve — returns 400 when not in AWAITING_APPROVAL")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void approveOrder_wrongStatus() throws Exception {
        when(orderService.approveOrder(1L))
                .thenThrow(new BusinessException("Only orders in AWAITING_APPROVAL status can be approved"));

        mockMvc.perform(post("/api/manager/orders/1/approve").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/manager/orders/{id}/decline — returns DECLINED status with reason")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void declineOrder_success() throws Exception {
        DeclineOrderRequest req = new DeclineOrderRequest();
        req.setReason("Out of stock");

        OrderResponse declined = orderWithStatus(OrderStatus.DECLINED);
        declined.setDeclineReason("Out of stock");

        when(orderService.declineOrder(eq(1L), any())).thenReturn(declined);

        mockMvc.perform(post("/api/manager/orders/1/decline").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("Out of stock"));
    }

    @Test
    @DisplayName("POST /api/manager/orders/{id}/decline — returns 400 when reason is blank")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void declineOrder_blankReason() throws Exception {
        mockMvc.perform(post("/api/manager/orders/1/decline").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/manager/orders/{id}/fulfill — returns FULFILLED status")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void fulfillOrder_success() throws Exception {
        when(orderService.fulfillOrder(1L)).thenReturn(orderWithStatus(OrderStatus.FULFILLED));

        mockMvc.perform(post("/api/manager/orders/1/fulfill").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }
}
