package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.SecurityConfig;
import com.wms.dto.request.InventoryItemRequest;
import com.wms.dto.response.InventoryItemResponse;
import com.wms.exception.ResourceNotFoundException;
import com.wms.security.JwtAuthEntryPoint;
import com.wms.security.JwtAuthFilter;
import com.wms.security.JwtUtils;
import com.wms.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@DisplayName("InventoryController")
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean InventoryService inventoryService;
    @MockBean JwtUtils jwtUtils;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean UserDetailsService userDetailsService;

    private final InventoryItemResponse sampleResponse = InventoryItemResponse.builder()
            .id(1L).itemName("Cardboard Box").quantity(100)
            .unitPrice(new BigDecimal("1.50")).build();

    @Test
    @DisplayName("GET /api/inventory — returns 200 for CLIENT")
    @WithMockUser(roles = "CLIENT")
    void getAllItems_asClient() throws Exception {
        when(inventoryService.getAllItems()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemName").value("Cardboard Box"))
                .andExpect(jsonPath("$[0].quantity").value(100));
    }

    @Test
    @DisplayName("GET /api/inventory — returns 200 for WAREHOUSE_MANAGER")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void getAllItems_asManager() throws Exception {
        when(inventoryService.getAllItems()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/inventory — returns 401 for unauthenticated")
    void getAllItems_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/inventory/{id} — returns 200 with item data")
    @WithMockUser(roles = "CLIENT")
    void getItemById_found() throws Exception {
        when(inventoryService.getItemById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.itemName").value("Cardboard Box"));
    }

    @Test
    @DisplayName("GET /api/inventory/{id} — returns 404 when item not found")
    @WithMockUser(roles = "CLIENT")
    void getItemById_notFound() throws Exception {
        when(inventoryService.getItemById(99L)).thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/inventory/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/inventory — returns 201 for WAREHOUSE_MANAGER")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void createItem_asManager() throws Exception {
        InventoryItemRequest req = new InventoryItemRequest();
        req.setItemName("Safety Gloves"); req.setQuantity(200);
        req.setUnitPrice(new BigDecimal("4.99"));

        when(inventoryService.createItem(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/inventory").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/inventory — returns 403 for CLIENT")
    @WithMockUser(roles = "CLIENT")
    void createItem_asClient_forbidden() throws Exception {
        InventoryItemRequest req = new InventoryItemRequest();
        req.setItemName("Box"); req.setQuantity(10);
        req.setUnitPrice(BigDecimal.ONE);

        mockMvc.perform(post("/api/inventory").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/inventory/{id} — returns 200 for WAREHOUSE_MANAGER")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void updateItem_asManager() throws Exception {
        InventoryItemRequest req = new InventoryItemRequest();
        req.setItemName("Cardboard Box Updated"); req.setQuantity(150);
        req.setUnitPrice(new BigDecimal("2.00"));

        when(inventoryService.updateItem(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/inventory/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/inventory/{id} — returns 204 for WAREHOUSE_MANAGER")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void deleteItem_asManager() throws Exception {
        doNothing().when(inventoryService).deleteItem(1L);

        mockMvc.perform(delete("/api/inventory/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/inventory/{id} — returns 403 for CLIENT")
    @WithMockUser(roles = "CLIENT")
    void deleteItem_asClient_forbidden() throws Exception {
        mockMvc.perform(delete("/api/inventory/1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
