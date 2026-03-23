package com.wms.security;

import com.wms.entity.User;
import com.wms.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtils")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private User user;

    // 64-char base64 secret (required for HS256)
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);

        user = User.builder()
                .id(1L).username("testuser").fullName("Test User")
                .email("test@wms.com").role(Role.CLIENT)
                .password("hashed").enabled(true).build();
    }

    @Test
    @DisplayName("generateToken returns a non-null token")
    void generateToken_returnsToken() {
        String token = jwtUtils.generateToken(user);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractUsername returns the correct username from token")
    void extractUsername() {
        String token = jwtUtils.generateToken(user);
        assertThat(jwtUtils.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("isTokenValid returns true for a freshly generated token")
    void isTokenValid_validToken() {
        String token = jwtUtils.generateToken(user);
        assertThat(jwtUtils.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for a garbage token")
    void isTokenValid_invalidToken() {
        assertThat(jwtUtils.isTokenValid("not.a.real.token")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns true when token matches user and is not expired")
    void validateToken_valid() {
        String token = jwtUtils.generateToken(user);
        assertThat(jwtUtils.validateToken(token, user)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false when username does not match")
    void validateToken_wrongUser() {
        String token = jwtUtils.generateToken(user);

        User otherUser = User.builder()
                .id(2L).username("otheruser").fullName("Other")
                .email("other@wms.com").role(Role.CLIENT)
                .password("hashed").enabled(true).build();

        assertThat(jwtUtils.validateToken(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("expired token is invalid")
    void expiredToken_isInvalid() {
        // Set expiration to -1ms so token is immediately expired
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1L);
        String token = jwtUtils.generateToken(user);
        assertThat(jwtUtils.isTokenValid(token)).isFalse();
    }
}
