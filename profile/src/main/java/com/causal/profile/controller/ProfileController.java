package com.causal.profile.controller;

import com.causal.profile.dto.request.ProfileCreateRequest;
import com.causal.profile.dto.request.ProfileUpdateRequest;
import com.causal.profile.dto.response.ProfileShowResponse;
import com.causal.profile.service.ProfileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profiles/me")
    public ProfileShowResponse getCurrentUserProfile() {
        return profileService.getCurrentUserProfile();
    }

    @GetMapping("/internal/profiles/{userId}")
    public ProfileShowResponse getProfile(@PathVariable Long userId) {
        return profileService.getProfile(userId);
    }

    @PostMapping("/profiles/me")
    public ProfileShowResponse createProfile(@Validated @RequestBody ProfileCreateRequest request) {
        return profileService.createProfile(request);
    }

    @PatchMapping("/profiles/me")
    public ProfileShowResponse updateProfile(@Validated @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(request);
    }
}
