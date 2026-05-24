package com.causal.profile.service;

import com.causal.profile.config.CurrentUser;
import com.causal.profile.dto.request.ProfileCreateRequest;
import com.causal.profile.dto.request.ProfileUpdateRequest;
import com.causal.profile.dto.response.ProfileShowResponse;
import com.causal.profile.mapper.ProfileMapper;
import com.causal.profile.model.Profile;
import com.causal.profile.repository.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final CurrentUser currentUser;

    public ProfileService(ProfileRepository profileRepository, ProfileMapper profileMapper, CurrentUser currentUser) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
        this.currentUser = currentUser;
    }

    public ProfileShowResponse getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(profileMapper::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    public ProfileShowResponse getCurrentUserProfile() {
        return getProfile(currentUser.id());
    }

    public ProfileShowResponse createProfile(ProfileCreateRequest request) {
        Long userId = currentUser.id();
        if (profileRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile already exists");
        }
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        if (request.currency() != null) {
            profile.setCurrency(request.currency());
        }
        profileRepository.save(profile);
        return profileMapper.from(profile);
    }

    @Transactional
    public ProfileShowResponse updateProfile(ProfileUpdateRequest request) {
        Profile profile = profileRepository.findByUserId(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (request.firstName() != null) {
            profile.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            profile.setLastName(request.lastName());
        }
        if (request.currency() != null) {
            profile.setCurrency(request.currency());
        }
        if (request.defaultAddressId() != null) {
            profile.setDefaultAddressId(request.defaultAddressId());
        }
        return profileMapper.from(profile);
    }
}
