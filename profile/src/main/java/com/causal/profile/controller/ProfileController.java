package com.causal.profile.controller;

import com.causal.profile.dto.response.ProfileShowResponse;
import com.causal.profile.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("profiles/{userId}")
    public ProfileShowResponse getProfile(@PathVariable Long userId) {
        return profileService.getProfile(userId);
    }
}
