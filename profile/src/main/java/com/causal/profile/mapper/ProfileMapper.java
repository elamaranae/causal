package com.causal.profile.mapper;

import com.causal.profile.dto.response.ProfileShowResponse;
import com.causal.profile.model.Profile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    ProfileShowResponse from(Profile profile);
}
