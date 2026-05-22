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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.HexFormat;
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

        final String jwt = jwtUtil.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        addAuthCookies(response, jwt, refreshToken.getToken());
        return ResponseEntity.ok(Map.of("accessToken", jwt, "refreshToken", refreshToken.getToken()));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        final String jwt = jwtUtil.generateToken(user);

        // Delete old refresh token if exists and create new one
        refreshTokenService.deleteByUserId(user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        addAuthCookies(response, jwt, refreshToken.getToken());
        return ResponseEntity.ok(Map.of("accessToken", jwt, "refreshToken", refreshToken.getToken()));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response) {
        String requestRefreshToken = cookieRefreshToken;
        if (requestRefreshToken == null && body != null) {
            requestRefreshToken = body.get("refreshToken");
        }
        if (requestRefreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token is required"));
        }

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(refreshToken -> {
                    User user = refreshToken.getUser();
                    refreshTokenService.deleteByUserId(user.getId());
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
                    String token = jwtUtil.generateToken(user);
                    addAuthCookies(response, token, newRefreshToken.getToken());
                    return ResponseEntity.ok(Map.of("accessToken", token, "refreshToken", newRefreshToken.getToken()));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @GetMapping("/internal/token-exchange")
    public ResponseEntity<?> tokenExchange(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer causal_")) {
            return ResponseEntity.status(401).build();
        }

        String apiToken = authHeader.substring(7);
        String hash = TokenHasher.hash(apiToken);
        User user = userRepository.findByApiTokenHash(hash).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        String jwt = jwtUtil.generateToken(user);
        return ResponseEntity.ok()
                .header("X-Forwarded-Authorization", "Bearer " + jwt)
                .build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        User user = getAuthenticatedUser();

        refreshTokenService.deleteByUserId(user.getId());
        clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("message", "Log out successful!"));
    }

    private void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addCookie(buildCookie("access_token", accessToken, "/", (int) (jwtExpirationMs / 1000)));
        response.addCookie(buildCookie("refresh_token", refreshToken, "/auth/refresh", (int) (refreshExpirationMs / 1000)));
        response.addCookie(buildCsrfCookie());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addCookie(buildCookie("access_token", "", "/", 0));
        response.addCookie(buildCookie("refresh_token", "", "/auth/refresh", 0));

        Cookie csrfCookie = new Cookie("csrf_token", "");
        csrfCookie.setHttpOnly(false);
        csrfCookie.setSecure(cookieSecure);
        csrfCookie.setPath("/");
        csrfCookie.setMaxAge(0);
        csrfCookie.setAttribute("SameSite", "Lax");
        response.addCookie(csrfCookie);
    }

    private Cookie buildCsrfCookie() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        String csrfToken = HexFormat.of().formatHex(randomBytes);

        Cookie cookie = new Cookie("csrf_token", csrfToken);
        cookie.setHttpOnly(false);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpirationMs / 1000));
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Cookie buildCookie(String name, String value, String path, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
