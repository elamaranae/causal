package com.causal.identity.auth;

import com.causal.identity.user.User;
import com.causal.identity.user.UserRegistrationDto;
import com.causal.identity.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpirationMs;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto registrationDto, HttpServletResponse response) {
        if (userRepository.findByEmail(registrationDto.email()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use"));
        }

        User user = new User();
        user.setName(registrationDto.name());
        user.setEmail(registrationDto.email());
        user.setPassword(passwordEncoder.encode(registrationDto.password()));

        user = userRepository.save(user);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        final String jwt = jwtUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        addAuthCookies(response, jwt, refreshToken.getToken());
        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.email());
        final String jwt = jwtUtil.generateToken(userDetails);

        // Delete old refresh token if exists and create new one
        refreshTokenService.deleteByUserId(user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        addAuthCookies(response, jwt, refreshToken.getToken());
        return ResponseEntity.ok(Map.of("message", "Login successful"));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token") String requestRefreshToken, HttpServletResponse response) {
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(refreshToken -> {
                    User user = refreshToken.getUser();
                    refreshTokenService.deleteByUserId(user.getId());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
                    String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));
                    addAuthCookies(response, token, newRefreshToken.getToken());
                    return ResponseEntity.ok(Map.of("message", "Token refreshed"));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenService.deleteByUserId(user.getId());
        clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("message", "Log out successful!"));
    }

    private void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtExpirationMs / 1000));
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/auth/refresh");
        refreshCookie.setMaxAge((int) (refreshExpirationMs / 1000));
        refreshCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/auth/refresh");
        refreshCookie.setMaxAge(0);
        refreshCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshCookie);
    }
}