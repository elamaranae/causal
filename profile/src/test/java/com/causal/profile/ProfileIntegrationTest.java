package com.causal.profile;

import com.causal.profile.repository.AddressRepository;
import com.causal.profile.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ImportTestcontainers
@ActiveProfiles("test")
class ProfileIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private AddressRepository addressRepository;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.subject("1").claim("email", "test@test.com"));
    }

    @BeforeEach
    void cleanDb() {
        profileRepository.findAll().forEach(p -> {
            p.setDefaultAddressId(null);
            profileRepository.save(p);
        });
        addressRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    void createProfile_persistsInDb() throws Exception {
        mockMvc.perform(post("/profiles/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","currency":"USD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.currency").value("USD"));

        assertTrue(profileRepository.findByUserId(1L).isPresent());
    }

    @Test
    void createProfile_duplicate_returns409() throws Exception {
        mockMvc.perform(post("/profiles/me")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/profiles/me")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jane","lastName":"Doe"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateProfile_partialUpdate() throws Exception {
        mockMvc.perform(post("/profiles/me")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","currency":"USD"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/profiles/me")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jane","currency":"EUR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void addressCrud_fullFlow() throws Exception {
        mockMvc.perform(post("/profiles/me")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/profiles/me/addresses")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Home","line1":"123 Main","city":"NYC","state":"NY","country":"US","pincode":"10001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Home"));

        mockMvc.perform(get("/profiles/me").with(jwt()))
                .andExpect(jsonPath("$.defaultAddressId").isNotEmpty());

        mockMvc.perform(post("/profiles/me/addresses")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Work","line1":"456 Office","city":"SF","state":"CA","country":"US","pincode":"94105"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/profiles/me/addresses").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        Long secondAddressId = addressRepository.findByUserId(1L).stream()
                .filter(a -> "Work".equals(a.getLabel()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(delete("/profiles/me/addresses/" + secondAddressId).with(jwt()))
                .andExpect(status().isOk());

        assertEquals(1, addressRepository.findByUserId(1L).size());
    }

    @Test
    void deleteDefaultAddress_returns400() throws Exception {
        mockMvc.perform(post("/profiles/me")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/profiles/me/addresses")
                        .with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label":"Home","line1":"123 Main","city":"NYC","state":"NY","country":"US","pincode":"10001"}
                                """))
                .andExpect(status().isOk());

        Long addressId = addressRepository.findByUserId(1L).get(0).getId();

        mockMvc.perform(delete("/profiles/me/addresses/" + addressId).with(jwt()))
                .andExpect(status().isBadRequest());
    }
}
