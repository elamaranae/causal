package com.causal.profile.controller;

import com.causal.profile.config.SecurityConfig;
import com.causal.profile.dto.response.AddressShowResponse;
import com.causal.profile.dto.response.ProfileShowResponse;
import com.causal.profile.service.AddressService;
import com.causal.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ProfileController.class, AddressController.class})
@Import(SecurityConfig.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private AddressService addressService;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject("1").claim("email", "test@test.com"));
    }

    @Test
    void getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_authenticated_returns200() throws Exception {
        ProfileShowResponse response = new ProfileShowResponse(1L, 1L, "John", "Doe", "USD", null);
        when(profileService.getCurrentUserProfile()).thenReturn(response);

        mockMvc.perform(get("/profiles/me").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getProfile_notFound_returns404() throws Exception {
        when(profileService.getCurrentUserProfile())
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        mockMvc.perform(get("/profiles/me").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProfile_validRequest_returns200() throws Exception {
        ProfileShowResponse response = new ProfileShowResponse(1L, 1L, "John", "Doe", "USD", null);
        when(profileService.createProfile(any())).thenReturn(response);

        mockMvc.perform(post("/profiles/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","currency":"USD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void createProfile_missingFirstName_returns400() throws Exception {
        mockMvc.perform(post("/profiles/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lastName":"Doe"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProfile_duplicateProfile_returns409() throws Exception {
        when(profileService.createProfile(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Profile already exists"));

        mockMvc.perform(post("/profiles/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateProfile_returns200() throws Exception {
        ProfileShowResponse response = new ProfileShowResponse(1L, 1L, "Jane", "Doe", "EUR", null);
        when(profileService.updateProfile(any())).thenReturn(response);

        mockMvc.perform(patch("/profiles/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jane","currency":"EUR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    // Address endpoints

    @Test
    void getAddresses_returns200() throws Exception {
        AddressShowResponse addr = new AddressShowResponse(1L, 1L, "Home", "123 Main", null, "NYC", "NY", "US", "10001", null);
        when(addressService.getAddresses()).thenReturn(List.of(addr));

        mockMvc.perform(get("/profiles/me/addresses").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Home"))
                .andExpect(jsonPath("$[0].city").value("NYC"));
    }

    @Test
    void createAddress_validRequest_returns200() throws Exception {
        AddressShowResponse addr = new AddressShowResponse(1L, 1L, "Home", "123 Main", null, "NYC", "NY", "US", "10001", null);
        when(addressService.createAddress(any())).thenReturn(addr);

        mockMvc.perform(post("/profiles/me/addresses")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Home","line1":"123 Main","city":"NYC","state":"NY","country":"US","pincode":"10001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.line1").value("123 Main"));
    }

    @Test
    void createAddress_missingLine1_returns400() throws Exception {
        mockMvc.perform(post("/profiles/me/addresses")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city":"NYC","state":"NY","country":"US","pincode":"10001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAddress_returns200() throws Exception {
        mockMvc.perform(delete("/profiles/me/addresses/1").with(jwt()))
                .andExpect(status().isOk());

        verify(addressService).deleteAddress(1L);
    }

    @Test
    void deleteAddress_defaultAddress_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete default address"))
                .when(addressService).deleteAddress(1L);

        mockMvc.perform(delete("/profiles/me/addresses/1").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAddress_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"))
                .when(addressService).deleteAddress(99L);

        mockMvc.perform(delete("/profiles/me/addresses/99").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
