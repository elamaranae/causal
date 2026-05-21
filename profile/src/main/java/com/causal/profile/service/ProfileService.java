package com.causal.profile.service;

import com.causal.profile.dto.response.ProfileShowResponse;
import com.causal.profile.mapper.ProfileMapper;
import com.causal.profile.repository.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    public ProfileService(ProfileRepository profileRepository, ProfileMapper profileMapper) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
    }

    public ProfileShowResponse getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(profileMapper::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Profile not found for userId: " + userId));
    }
}
