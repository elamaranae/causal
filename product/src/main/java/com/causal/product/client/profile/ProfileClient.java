package com.causal.product.client.profile;

import com.causal.product.client.profile.dto.response.ProfileShowResponse;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/profiles")
public interface ProfileClient {

    @GetExchange("/me")
    ProfileShowResponse getCurrentUserProfile();
}
