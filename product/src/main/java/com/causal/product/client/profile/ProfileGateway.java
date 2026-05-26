package com.causal.product.client.profile;

import com.causal.product.client.profile.dto.response.ProfileShowResponse;
import org.springframework.stereotype.Component;

@Component
public class ProfileGateway {

    private final ProfileClient client;

    public ProfileGateway(ProfileClient client) {
        this.client = client;
    }

    public ProfileShowResponse getCurrentUserProfile() {
        return client.getCurrentUserProfile();
    }
}
