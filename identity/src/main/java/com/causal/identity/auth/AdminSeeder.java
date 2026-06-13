package com.causal.identity.auth;

import com.causal.identity.user.Role;
import com.causal.identity.user.RoleRepository;
import com.causal.identity.user.User;
import com.causal.identity.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@causal.dev").isPresent()) {
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("USER role not found"));

        User admin = new User();
        admin.setEmail("admin@causal.dev");
        admin.setPassword(passwordEncoder.encode("CausalAdmin123!"));
        admin.setRoles(Set.of(adminRole, userRole));
        userRepository.save(admin);
    }
}
