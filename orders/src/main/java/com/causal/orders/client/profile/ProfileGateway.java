package com.causal.orders.client.profile;

import com.causal.orders.client.profile.dto.response.AddressShowResponse;
import com.causal.orders.client.profile.dto.response.ProfileShowResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProfileGateway {

    private final ProfileClient client;

    public ProfileGateway(ProfileClient client) {
        this.client = client;
    }

    public ProfileShowResponse getCurrentUserProfile() {
        return client.getCurrentUserProfile();
    }

    public List<AddressShowResponse> getAddresses() {
        return client.getAddresses();
    }
}
