package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.dto.request.UserRequest;
import com.wms.dto.response.UserResponse;
import com.wms.enums.Role;
import com.wms.exception.ResourceNotFoundException;
import com.wms.security.JwtAuthEntryPoint;
import com.wms.security.JwtAuthFilter;
import com.wms.security.JwtUtils;
import com.wms.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAdminController.class)
@DisplayName("UserAdminController")
class UserAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean JwtUtils jwtUtils;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean UserDetailsService userDetailsService;

    private final UserResponse sampleUser = UserResponse.builder()
            .id(1L).username("manager1").fullName("Manager One")
            .email("m1@wms.com").role(Role.WAREHOUSE_MANAGER).enabled(true).build();

    @Test
    @DisplayName("POST /api/admin/users — returns 201 for SYSTEM_ADMIN")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createUser_success() throws Exception {
        UserRequest req = new UserRequest();
        req.setUsername("manager1"); req.setPassword("pass123");
        req.setFullName("Manager One"); req.setEmail("m1@wms.com");
        req.setRole(Role.WAREHOUSE_MANAGER);

        when(userService.createUser(any())).thenReturn(sampleUser);

        mockMvc.perform(post("/api/admin/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("manager1"))
                .andExpect(jsonPath("$.role").value("WAREHOUSE_MANAGER"));
    }

    @Test
    @DisplayName("POST /api/admin/users — returns 403 for WAREHOUSE_MANAGER")
    @WithMockUser(roles = "WAREHOUSE_MANAGER")
    void createUser_asManager_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/users — returns 403 for CLIENT")
    @WithMockUser(roles = "CLIENT")
    void createUser_asClient_forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users — returns list for SYSTEM_ADMIN")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getAllUsers_success() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleUser));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("manager1"));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} — returns 200 for SYSTEM_ADMIN")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getUserById_success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleUser);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} — returns 404 when not found")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void getUserById_notFound() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} — returns 204 for SYSTEM_ADMIN")
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void deleteUser_success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/admin/users/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/admin/users — returns 401 when unauthenticated")
    void getAllUsers_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
