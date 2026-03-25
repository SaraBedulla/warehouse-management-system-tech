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
        if (!userRepository.existsByUsername("manager")) {
            User user = new User();
            user.setUsername("manager");
            user.setFullName("Manager");
            user.setEmail("sara@yahoo.com");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRole(Role.WAREHOUSE_MANAGER);
            userRepository.save(user);
        }
    }
}
