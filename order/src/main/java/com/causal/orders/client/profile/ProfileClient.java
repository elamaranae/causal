package com.causal.orders.client.profile;

import com.causal.orders.client.profile.dto.response.AddressShowResponse;
import com.causal.orders.client.profile.dto.response.ProfileShowResponse;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("/profiles")
public interface ProfileClient {

    @GetExchange("/me")
    ProfileShowResponse getCurrentUserProfile();

    @GetExchange("/me/addresses")
    List<AddressShowResponse> getAddresses();
}
