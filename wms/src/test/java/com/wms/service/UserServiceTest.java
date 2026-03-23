package com.wms.service;

import com.wms.dto.request.UpdateUserRequest;
import com.wms.dto.request.UserRequest;
import com.wms.dto.response.UserResponse;
import com.wms.entity.User;
import com.wms.enums.Role;
import com.wms.exception.BusinessException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.UserRepository;
import com.wms.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L).username("john").fullName("John Doe")
                .email("john@wms.com").role(Role.CLIENT)
                .password("hashed").enabled(true).build();
    }

    // ─── createUser ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("creates user successfully when username and email are unique")
        void success() {
            UserRequest req = new UserRequest();
            req.setUsername("newuser"); req.setPassword("pass123");
            req.setFullName("New User"); req.setEmail("new@wms.com");
            req.setRole(Role.CLIENT);

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@wms.com")).thenReturn(false);
            when(passwordEncoder.encode("pass123")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0); u.setId(10L); return u;
            });

            UserResponse res = userService.createUser(req);

            assertThat(res.getUsername()).isEqualTo("newuser");
            assertThat(res.getRole()).isEqualTo(Role.CLIENT);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws BusinessException when username is already taken")
        void duplicateUsername_throws() {
            UserRequest req = new UserRequest();
            req.setUsername("john"); req.setPassword("p"); req.setFullName("X");
            req.setEmail("x@x.com"); req.setRole(Role.CLIENT);

            when(userRepository.existsByUsername("john")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already taken");
        }

        @Test
        @DisplayName("throws BusinessException when email is already registered")
        void duplicateEmail_throws() {
            UserRequest req = new UserRequest();
            req.setUsername("unique"); req.setPassword("p"); req.setFullName("X");
            req.setEmail("john@wms.com"); req.setRole(Role.CLIENT);

            when(userRepository.existsByUsername("unique")).thenReturn(false);
            when(userRepository.existsByEmail("john@wms.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already registered");
        }
    }

    // ─── getUserById ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("returns user when found")
        void found() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            UserResponse res = userService.getUserById(1L);
            assertThat(res.getUsername()).isEqualTo("john");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void notFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getAllUsers ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers returns mapped list")
    void getAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        List<UserResponse> result = userService.getAllUsers();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("john");
    }

    // ─── updateUser ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("updates fields and encodes password when provided")
        void success_withPassword() {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setFullName("John Updated"); req.setEmail("john@wms.com");
            req.setRole(Role.WAREHOUSE_MANAGER); req.setEnabled(true);
            req.setPassword("newpassword");

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.encode("newpassword")).thenReturn("newhashed");
            when(userRepository.save(any())).thenReturn(sampleUser);

            userService.updateUser(1L, req);

            verify(passwordEncoder).encode("newpassword");
            assertThat(sampleUser.getRole()).isEqualTo(Role.WAREHOUSE_MANAGER);
        }

        @Test
        @DisplayName("does not encode password when not provided")
        void success_withoutPassword() {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setFullName("John"); req.setEmail("john@wms.com");
            req.setRole(Role.CLIENT); req.setEnabled(true);
            req.setPassword(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(any())).thenReturn(sampleUser);

            userService.updateUser(1L, req);

            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("throws BusinessException when new email is already taken")
        void duplicateEmail_throws() {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setFullName("John"); req.setEmail("other@wms.com");
            req.setRole(Role.CLIENT); req.setEnabled(true);

            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userRepository.existsByEmail("other@wms.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already in use");
        }
    }

    // ─── deleteUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser removes the user")
    void deleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        userService.deleteUser(1L);
        verify(userRepository).delete(sampleUser);
    }
}
