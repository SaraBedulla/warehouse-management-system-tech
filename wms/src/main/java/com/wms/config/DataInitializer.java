package com.wms.config;

import com.wms.entity.User;
import com.wms.enums.Role;
import com.wms.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.checkerframework.checker.units.qual.C;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (!userRepository.existsByUsername("admin")) {
            User user = new User();
            user.setUsername("admin");
            user.setFullName("admin123");
            user.setEmail("admin@yahoo.com");
            user.setPassword(passwordEncoder.encode("admin123"));
            user.setRole(Role.SYSTEM_ADMIN);
            userRepository.save(user);
        }
    }
}
